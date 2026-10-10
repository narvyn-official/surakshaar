package com.narvyn.suraksha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FirstAidBayTest {
    private fun bay() = FirstAidBay.create("learner", "learning-test", 99)
    private fun restore(bay: FirstAidBay) = FirstAidBay.restore(JSONObject(bay.data.toString()), "learner", "learning-test")
    private fun action(bay: FirstAidBay) = bay.current.actions.single { it.correct }.id

    @Test fun everyStageCompletesInOrderAndRestoresWithOriginalEvidence() {
        var bay = bay()
        var now = 100L
        while (!bay.finished) {
            assertTrue(bay.act(action(bay), "ar", true, now++).advanced)
            bay = restore(bay)
        }
        assertEquals(12, bay.index)
        assertEquals(12, bay.events.length())
        assertEquals("ar", bay.presentation)
        assertFalse(bay.data.getBoolean("certifiable"))
        assertEquals("not-assessed", bay.data.getString("practicalCompetence"))
        val original = bay.data.toString()
        assertFalse(bay.act("ready-space", "ar", true, now).accepted)
        assertEquals(original, bay.data.toString())
    }

    @Test fun staleTrackingCannotAdvanceAnyArStage() {
        val bay = bay()
        var now = 100L
        while (!bay.finished) {
            val index = bay.index
            val feedback = bay.act(action(bay), "ar", false, now++)
            assertFalse(feedback.advanced)
            assertEquals("tracking-unavailable", feedback.reason)
            assertEquals(index, bay.index)
            assertTrue(bay.act(action(bay), "ar", true, now++).advanced)
        }
        assertEquals(24, bay.events.length())
        assertEquals(12, bay.events.objects().count { it.getString("reason") == "tracking-unavailable" })
        restore(bay)
    }

    @Test fun wrongPartAndFutureActionAreKeptWithoutSkippingTheCurrentStage() {
        val bay = bay()
        bay.act("ready-space", "3d", false, 100)
        assertFalse(bay.act("select:FMA7485", "3d", false, 101).advanced)
        assertEquals("response", bay.current.id)
        assertFalse(bay.act("handover-observations", "3d", false, 102).advanced)
        assertEquals("response", bay.current.id)
        assertTrue(bay.act("select:FMA50801", "3d", false, 103).advanced)
        assertEquals(4, bay.events.length())
        assertEquals(2, bay.events.objects().count { it.getString("reason") == "unknown-action" })
        restore(bay)
    }

    @Test fun explicitIncorrectActionDoesNotAdvanceAndHasAuthoredFeedback() {
        val bay = bay()
        val feedback = bay.act("ignore-space", "3d", false, 100)
        assertEquals("incorrect", feedback.reason)
        assertFalse(feedback.advanced)
        assertTrue(feedback.message.contains("practice space"))
        assertEquals(0, bay.index)
        bay.act("ready-space", "3d", false, 101)
        assertEquals("incorrect", bay.events.getJSONObject(0).getString("reason"))
    }

    @Test fun threeDAndMixedModesAreNeverPresentedAsPureAr() {
        val bay = bay()
        assertEquals("unstarted", bay.presentation)
        bay.act("ready-space", "ar", false, 100)
        assertEquals("unstarted", bay.presentation)
        bay.act("ready-space", "3d", false, 101)
        assertEquals("3d", bay.presentation)
        bay.act("select:FMA50801", "ar", true, 102)
        assertEquals("mixed", bay.presentation)
        restore(bay)
    }

    @Test fun pauseSurvivesRestoreAndBlocksInteractionUntilResumed() {
        var bay = bay()
        assertTrue(bay.pause(100))
        assertFalse(bay.pause(101))
        bay = restore(bay)
        assertTrue(bay.paused)
        assertEquals("paused", bay.act("ready-space", "3d", false, 102).reason)
        assertEquals(0, bay.index)
        assertTrue(bay.resume(103))
        assertTrue(bay.act("ready-space", "3d", false, 104).advanced)
        restore(bay)
    }

    @Test fun stopIsTerminalAndRetryUsesANewRecord() {
        val original = bay()
        original.act("ready-space", "3d", false, 100)
        assertTrue(original.stop(101))
        assertTrue(restore(original).stopped)
        val text = original.data.toString()
        assertFalse(original.resume(102))
        assertFalse(original.act("select:FMA50801", "3d", false, 103).accepted)
        assertEquals(text, original.data.toString())
        val retry = bay()
        assertNotEquals(original.data.getString("id"), retry.data.getString("id"))
        assertEquals(0, retry.index)
    }

    @Test fun alteredOwnerVersionProgressAndResultFlagsAreRejected() {
        val bay = bay()
        bay.act("ready-space", "3d", false, 100)
        val record = bay.data.toString()
        assertThrows(IllegalArgumentException::class.java) { FirstAidBay.restore(JSONObject(record), "other", "learning-test") }
        assertThrows(IllegalArgumentException::class.java) { FirstAidBay.restore(JSONObject(record), "learner", "new-version") }
        for ((key, value) in listOf("index" to 2, "finished" to true, "certifiable" to true, "passed" to true, "score" to 100, "reviewStatus" to "approved", "practicalCompetence" to "passed")) {
            assertThrows(IllegalArgumentException::class.java) { FirstAidBay.restore(JSONObject(record).put(key, value), "learner", "learning-test") }
        }
    }

    @Test fun fabricatedTrackingOrCompletionInRecordedEventIsRejected() {
        val bay = bay()
        bay.act("ready-space", "ar", false, 100)
        for ((key, value) in listOf("advanced" to true, "reason" to "advanced", "validTracking" to true)) {
            val record = JSONObject(bay.data.toString())
            record.getJSONArray("answers").getJSONObject(0).put(key, value)
            assertThrows(IllegalArgumentException::class.java) { FirstAidBay.restore(record, "learner", "learning-test") }
        }
    }

    @Test fun duplicateEventIdsAndRegressingTimeAreRejected() {
        val bay = bay()
        bay.act("ignore-space", "3d", false, 100)
        val original = bay.data.toString()
        assertThrows(IllegalArgumentException::class.java) { bay.act("ready-space", "3d", false, 98) }
        assertEquals(original, bay.data.toString())
        bay.act("ready-space", "3d", false, 101)
        val record = JSONObject(bay.data.toString())
        record.getJSONArray("answers").getJSONObject(1).put("id", record.getJSONArray("answers").getJSONObject(0).getString("id"))
        assertThrows(IllegalArgumentException::class.java) { FirstAidBay.restore(record, "learner", "learning-test") }
    }

    @Test fun defaultUiActionsRecoverAfterDeviceClockMovesBackwards() {
        val future = System.currentTimeMillis() + 3_600_000L
        val bay = FirstAidBay.create("learner", "learning-test", future)
        bay.pause()
        bay.resume()
        assertTrue(bay.act("ready-space", "3d", false).advanced)
        bay.stop()
        assertTrue(bay.stopped)
        assertTrue(bay.events.objects().all { it.getLong("at") >= future })
        restore(bay)
    }

    @Test fun schemaHasStableUniqueStagesActionsAndOnlyPromisedModelParts() {
        assertEquals(FirstAidBay.steps.size, FirstAidBay.steps.map { it.id }.toSet().size)
        val actions = FirstAidBay.steps.flatMap { it.actions }.map { it.id }
        assertEquals(actions.size, actions.toSet().size)
        assertEquals(setOf("FMA50801", "FMA7394", "FMA7485"), FirstAidBay.steps.mapNotNull { it.partId }.toSet())
        for (step in FirstAidBay.steps) {
            assertEquals(1, step.actions.count { it.correct })
            assertTrue(step.source.startsWith("https://"))
            step.partId?.let { assertTrue(step.actions.single { a -> a.correct }.spatial) }
        }
    }
}
