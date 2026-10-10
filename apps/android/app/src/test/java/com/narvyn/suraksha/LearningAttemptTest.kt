package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LearningAttemptTest {
    private fun scenario(): JSONObject = JSONObject().put("id", "sequence").put("course", "first-aid").put("reviewStatus", "draft")
        .put("steps", JSONArray((1..2).map { i ->
            JSONObject().put("id", "step-$i").put("choices", JSONArray(listOf(
                JSONObject().put("id", "safe-$i").put("correct", true),
                JSONObject().put("id", "unsafe-$i").put("correct", false)
            )))
        }))
    private fun attempt() = LearningAttempt.create(scenario(), "test-version", "worker-one", now = 99L)
    private fun decisions(attempt: LearningAttempt) = attempt.events.objects().filter { it.getString("kind") == "decision" }

    @Test fun wrongChoiceRemainsInEvidenceAfterAReattempt() {
        val attempt = attempt()
        assertFalse(attempt.answer("unsafe-1", 100))
        assertEquals(0, attempt.index)
        assertFalse(attempt.awaitingContinue)
        assertTrue(attempt.answer("safe-1", 101))
        assertTrue(attempt.awaitingContinue)
        attempt.advance(102)
        assertEquals(1, attempt.index)
        assertEquals(2, decisions(attempt).size)
        assertFalse(decisions(attempt)[0].getBoolean("correct"))
        assertTrue(decisions(attempt)[1].getBoolean("correct"))
        assertEquals("unsafe-1", decisions(attempt)[0].getString("choiceId"))
    }

    @Test fun cannotSkipStagesOrSubmitDuplicateAnswerWhileWaiting() {
        val attempt = attempt()
        assertThrows(IllegalStateException::class.java) { attempt.advance(100) }
        assertThrows(IllegalStateException::class.java) { attempt.answer("injected-option", 100) }
        assertEquals(0, attempt.events.length())
        assertTrue(attempt.answer("safe-1", 101))
        assertThrows(IllegalStateException::class.java) { attempt.answer("safe-1", 102) }
        assertEquals(1, attempt.events.length())
    }

    @Test fun hintsAreRecordedAndResetForTheNextStage() {
        val attempt = attempt()
        attempt.hint(100); attempt.hint(101)
        attempt.answer("safe-1", 102)
        assertEquals(2, decisions(attempt).last().getInt("hints"))
        attempt.advance(103)
        attempt.answer("safe-2", 104)
        assertEquals(0, decisions(attempt).last().getInt("hints"))
    }

    @Test fun completionCannotBeReansweredAndTeachBackDoesNotRewriteDecisions() {
        val attempt = attempt()
        assertThrows(IllegalStateException::class.java) { attempt.explain("too early", 99) }
        attempt.answer("safe-1", 100); attempt.advance(101)
        attempt.answer("safe-2", 102); attempt.advance(103)
        assertTrue(attempt.finished)
        assertTrue(attempt.data.getBoolean("finished"))
        assertThrows(IllegalStateException::class.java) { attempt.answer("safe-2", 104) }
        assertThrows(IllegalStateException::class.java) { attempt.hint(104) }
        val original = attempt.events.objects().map { it.toString() }
        attempt.explain("x".repeat(1200), 105)
        assertEquals(original, attempt.events.objects().take(original.size).map { it.toString() })
        assertEquals(1000, attempt.events.objects().last().getString("text").length)
        assertEquals("formative-decision-practice", attempt.data.getString("evidenceType"))
        assertFalse(attempt.data.optBoolean("certifiable"))
    }

    @Test fun optionOrderIsStableWithinARecordedStage() {
        val attempt = attempt()
        val first = attempt.choices().map { it.getString("id") }
        repeat(8) { assertEquals(first, attempt.choices().map { it.getString("id") }) }
        assertEquals(setOf("safe-1", "unsafe-1"), first.toSet())
    }

    @Test fun interruptedAttemptRestoresWithoutErasingWrongChoicesOrHints() {
        val original = attempt()
        original.hint(100)
        original.answer("unsafe-1", 101)
        original.answer("safe-1", 102)
        val text = original.data.toString()
        val restored = LearningAttempt.restore(JSONObject(text), original.scenario, "test-version", "worker-one")
        assertTrue(restored.awaitingContinue)
        assertEquals(0, restored.index)
        assertEquals(text, restored.data.toString())
        restored.advance(103)
        assertEquals(1, restored.index)
        assertFalse(decisions(restored).first().getBoolean("correct"))
    }

    @Test fun restoreRejectsWrongOwnerVersionEditedCorrectnessAndProgress() {
        val original = attempt()
        original.answer("unsafe-1", 100)
        val text = original.data.toString()
        assertThrows(IllegalArgumentException::class.java) { LearningAttempt.restore(JSONObject(text), original.scenario, "test-version", "worker-two") }
        assertThrows(IllegalArgumentException::class.java) { LearningAttempt.restore(JSONObject(text), original.scenario, "different-version", "worker-one") }
        val correctness = JSONObject(text).apply { getJSONArray("answers").getJSONObject(0).put("correct", true) }
        assertThrows(IllegalArgumentException::class.java) { LearningAttempt.restore(correctness, original.scenario, "test-version", "worker-one") }
        assertThrows(IllegalArgumentException::class.java) { LearningAttempt.restore(JSONObject(text).put("index", 1), original.scenario, "test-version", "worker-one") }
        assertThrows(IllegalArgumentException::class.java) { LearningAttempt.restore(JSONObject(text).put("updatedAt", 98L), original.scenario, "test-version", "worker-one") }
    }

    @Test fun clockRegressionCannotAppendAnEvent() {
        val attempt = attempt()
        attempt.hint(101)
        val original = attempt.data.toString()
        assertThrows(IllegalArgumentException::class.java) { attempt.answer("safe-1", 100) }
        assertEquals(original, attempt.data.toString())
    }

    @Test fun deviceClockMovingBackwardsDoesNotBreakDefaultUiActions() {
        val future = System.currentTimeMillis() + 3_600_000L
        val attempt = LearningAttempt.create(scenario(), "test-version", "worker-one", now = future)
        attempt.hint()
        attempt.answer("safe-1")
        attempt.advance()
        attempt.answer("safe-2")
        attempt.advance()
        attempt.explain("I checked before acting.")
        assertTrue(attempt.finished)
        assertTrue(attempt.events.objects().all { it.getLong("at") >= future })
        LearningAttempt.restore(JSONObject(attempt.data.toString()), attempt.scenario, "test-version", "worker-one")
    }

    @Test fun modelObservationRequiresCurrentPartAndExplicitMode() {
        val scene = scenario().apply { getJSONArray("steps").getJSONObject(0).put("part", "FMA-part") }
        val attempt = LearningAttempt.create(scene, "test-version", "worker-one", now = 99L)
        assertThrows(IllegalArgumentException::class.java) { attempt.observe("different-part", "3d", 100) }
        assertThrows(IllegalArgumentException::class.java) { attempt.observe("FMA-part", "practical-skill", 100) }
        attempt.observe("FMA-part", "3d", 100)
        assertEquals("3d", attempt.events.getJSONObject(0).getString("mode"))
        assertEquals(0, attempt.index)
        assertFalse(attempt.finished)
        LearningAttempt.restore(JSONObject(attempt.data.toString()), scene, "test-version", "worker-one")
    }

    @Test fun requiredObservationCannotBeSkippedOrBorrowedFromAnotherStage() {
        val scene = scenario().apply {
            getJSONArray("steps").objects().forEach { it.put("part", "FMA-part").put("requiresObservation", true) }
        }
        val attempt = LearningAttempt.create(scene, "test-version", "worker-one", now = 99L)
        attempt.answer("safe-1", 100)
        assertThrows(IllegalStateException::class.java) { attempt.advance(101) }
        assertEquals(0, attempt.index)
        attempt.observe("FMA-part", "3d", 102)
        attempt.advance(103)
        attempt.answer("safe-2", 104)
        // The same part viewed for the previous objective cannot satisfy this objective.
        assertThrows(IllegalStateException::class.java) { attempt.advance(105) }
        attempt.observe("FMA-part", "ar-exploration", 106)
        attempt.advance(107)
        assertTrue(attempt.finished)
        LearningAttempt.restore(JSONObject(attempt.data.toString()), scene, "test-version", "worker-one")
    }

    @Test fun everyAuthoredScenarioHasAReachableFinishWithItsOriginalVersion() {
        val content = JSONObject(File("../../../content/learning/scenarios.json").readText())
        for (scene in content.getJSONArray("scenarios").objects()) {
            val attempt = LearningAttempt.create(scene, content.getString("version"), "worker-one", now = 99L)
            var now = 100L
            while (!attempt.finished) {
                if (attempt.current.optBoolean("requiresObservation")) {
                    attempt.observe(attempt.current.getString("part"), "3d", now++)
                }
                val correct = attempt.current.getJSONArray("choices").objects().first { it.getBoolean("correct") }
                assertTrue(attempt.answer(correct.getString("id"), now++))
                attempt.advance(now++)
            }
            assertEquals(scene.getJSONArray("steps").length(), decisions(attempt).size)
            assertEquals(content.getString("version"), attempt.data.getString("contentVersion"))
            assertFalse(attempt.data.optBoolean("certifiable"))
        }
    }
}
