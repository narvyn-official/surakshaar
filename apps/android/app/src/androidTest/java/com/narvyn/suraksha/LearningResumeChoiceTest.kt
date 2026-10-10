package com.narvyn.suraksha

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.equalTo
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Synthetic local records only. A fresh attempt must never replace or erase earlier evidence. */
@RunWith(AndroidJUnit4::class)
class LearningResumeChoiceTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = ins.targetContext
    private lateinit var original: String
    private lateinit var worker: String
    private lateinit var previous: String
    private var oldHindi = false
    private fun tap(tag: String) = onView(withTagValue(equalTo<Any>(tag))).perform(revealOnPage(), click())
    private fun field(host: Any, name: String): Any? = host.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(host)
    private fun all(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()
    private fun capture(name: String) {
        ins.waitForIdleSync()
        // ActivityScenario can report idle before SurfaceFlinger replaces the white launch frame.
        // Wait for the visible blue choice button, rather than publishing a blank screenshot.
        repeat(30) {
            val image = ins.uiAutomation.takeScreenshot() ?: error("Screenshot unavailable")
            var blue = 0
            for (y in image.height / 4 until image.height * 9 / 10 step 8) for (x in 0 until image.width step 8) {
                val pixel = image.getPixel(x, y)
                if (android.graphics.Color.red(pixel) < 90 && android.graphics.Color.green(pixel) < 140 && android.graphics.Color.blue(pixel) > 170) blue++
            }
            if (blue > 80) {
                File(context.getExternalFilesDir("validation"), "resume-choice-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
                image.recycle(); return
            }
            image.recycle(); Thread.sleep(100)
        }
        error("Resume choice was not rendered")
    }
    @Before fun isolate() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        Store(context).use {
            original = it.workerId; oldHindi = it.hi
            previous = JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString()
            worker = it.createProfile("Synthetic resume choices", "Unspecified"); it.switchProfile(worker); it.hi = false
        }
    }
    @After fun restore() {
        if (!::worker.isInitialized) return
        Store(context).use { it.switchProfile(original); it.hi = oldHindi }
        Store(context).use {
            assertEquals(previous, JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString())
            it.writableDatabase.delete("worker_profiles", "id=?", arrayOf(worker))
        }
        for (db in listOf("launch-learning.db", "learning-revision.db", "first-aid-bay.db")) SkillPracticeStore(context, worker, db).use {
            it.writableDatabase.delete("practice", "worker=?", arrayOf(worker))
        }
    }
    private fun savedLesson(withDecision: Boolean = true): LearningAttempt {
        val catalog = LearningCatalog(context)
        val attempt = LearningAttempt.create(catalog.scenario("escape-first"), catalog.version, worker)
        if (withDecision) {
            attempt.answer(attempt.current.getJSONArray("choices").objects().first { it.getBoolean("correct") }.getString("id"))
            attempt.advance()
        }
        LearningProgress(context, worker).use { it.save(attempt) }
        return attempt
    }
    private fun lessonIntent(record: LearningAttempt) = Intent(context, LearningScenarioActivity::class.java)
        .putExtra("scenarioId", record.scenario.getString("id")).putExtra("attemptId", record.data.getString("id"))
    private fun lessonRecord(id: String) = LearningProgress(context, worker).use { p -> p.records().single { it.getString("id") == id }.toString() }
    private fun savedBay(withDecision: Boolean = true): FirstAidBay {
        val bay = FirstAidBay.createScenario(worker, LearningCatalog(context).version, "bleeding-injury")
        if (withDecision) {
            bay.act(bay.current.actions.first { it.correct }.id, "3d", true)
            bay.pause()
        }
        SkillPracticeStore(context, worker, "first-aid-bay.db").use { it.save(bay.data) }
        return bay
    }
    private fun bayIntent(bay: FirstAidBay) = Intent(context, FirstAidBayActivity::class.java).putExtra("attemptId", bay.data.getString("id"))
    private fun bayRecord(id: String) = SkillPracticeStore(context, worker, "first-aid-bay.db").use { p -> p.records().single { it.getString("id") == id }.toString() }

    @Test fun lessonContinueAndRotationKeepTheExactSavedAttempt() {
        val old = savedLesson(); val before = old.data.toString()
        savedLesson(false) // A newer empty attempt must not replace the explicitly selected older one.
        ActivityScenario.launch<LearningScenarioActivity>(lessonIntent(old)).use { scenario ->
            tap("learning-resume-continue")
            scenario.onActivity { assertEquals(old.data.getString("id"), (field(it, "attempt") as LearningAttempt).data.getString("id")) }
            scenario.recreate()
            scenario.onActivity {
                val restored = field(it, "attempt") as LearningAttempt
                assertEquals(old.data.getString("id"), restored.data.getString("id")); assertEquals(old.index, restored.index)
                assertFalse(field(it, "resumeChoicePending") as Boolean)
            }
        }
        assertEquals(before, lessonRecord(old.data.getString("id")))
    }

    @Test fun lessonRestartCreatesNewIdAtStepOneAndKeepsOriginalBytes() {
        val old = savedLesson(); val before = old.data.toString()
        ActivityScenario.launch<LearningScenarioActivity>(lessonIntent(old)).use { scenario ->
            scenario.recreate() // The pending choice itself also survives rotation.
            onView(withText("Earlier attempt stays saved.")).check(matches(isDisplayed()))
            capture("lesson")
            tap("learning-resume-restart")
            var freshId = ""
            scenario.onActivity {
                val fresh = field(it, "attempt") as LearningAttempt
                freshId = fresh.data.getString("id"); assertNotEquals(old.data.getString("id"), freshId)
                assertEquals(0, fresh.index); assertEquals(0, fresh.events.length())
            }
            scenario.recreate()
            scenario.onActivity { assertEquals(freshId, (field(it, "attempt") as LearningAttempt).data.getString("id")) }
            LearningProgress(context, worker).use { assertEquals(2, it.records().size) }
        }
        assertEquals(before, lessonRecord(old.data.getString("id")))
    }

    @Test fun activeLessonCanRestartAndCancellingSavedChoiceLeavesHistoryUnchanged() {
        val old = savedLesson(); val before = old.data.toString()
        ActivityScenario.launch<LearningScenarioActivity>(lessonIntent(old)).use { scenario -> scenario.onActivity { it.finish() } }
        assertEquals(before, lessonRecord(old.data.getString("id")))
        ActivityScenario.launch<LearningScenarioActivity>(lessonIntent(old)).use { scenario ->
            tap("learning-resume-continue"); tap("learning-tools")
            onView(withText("Start from step 1")).perform(revealOnPage(), click())
            scenario.onActivity {
                val fresh = field(it, "attempt") as LearningAttempt
                assertEquals(0, fresh.index); assertNotEquals(old.data.getString("id"), fresh.data.getString("id"))
            }
        }
        assertEquals(before, lessonRecord(old.data.getString("id")))
    }

    @Test fun unstartedLessonHasNoRedundantResumeChoice() {
        val old = savedLesson(false)
        ActivityScenario.launch<LearningScenarioActivity>(lessonIntent(old)).use { scenario ->
            scenario.onActivity {
                assertFalse(field(it, "resumeChoicePending") as Boolean)
                assertTrue(all(it.window.decorView).none { view -> view.tag == "learning-resume-continue" })
                assertEquals(old.data.getString("id"), (field(it, "attempt") as LearningAttempt).data.getString("id"))
            }
        }
    }

    @Test fun firstAidContinueUsesSameRecordAndChosenMode() {
        val old = savedBay()
        ActivityScenario.launch<FirstAidBayActivity>(bayIntent(old)).use { scenario ->
            capture("first-aid")
            tap("bay-resume-continue")
            scenario.onActivity {
                val resumed = field(it, "attempt") as FirstAidBay
                assertEquals(old.data.getString("id"), resumed.data.getString("id")); assertEquals(old.index, resumed.index)
                assertFalse(field(it, "camera") as Boolean); assertFalse(field(it, "resumeChoicePending") as Boolean)
            }
        }
        val record = JSONObject(bayRecord(old.data.getString("id")))
        assertEquals(old.events.getJSONObject(0).toString(), record.getJSONArray("answers").getJSONObject(0).toString())
        assertEquals(old.index, record.getInt("index"))
    }

    @Test fun firstAidRestartKeepsScenarioAndOriginalBytes() {
        val old = savedBay(); val before = old.data.toString()
        ActivityScenario.launch<FirstAidBayActivity>(bayIntent(old)).use { scenario ->
            tap("bay-resume-restart")
            scenario.onActivity {
                val fresh = field(it, "attempt") as FirstAidBay
                assertNotEquals(old.data.getString("id"), fresh.data.getString("id"))
                assertEquals(old.scenarioId, fresh.scenarioId); assertEquals(0, fresh.index); assertEquals(0, fresh.events.length())
                assertFalse(field(it, "camera") as Boolean)
            }
            SkillPracticeStore(context, worker, "first-aid-bay.db").use { assertEquals(2, it.records().size) }
        }
        assertEquals(before, bayRecord(old.data.getString("id")))
    }

    @Test fun unstartedFirstAidAndCancelledSavedChoiceDoNotChangeHistory() {
        val fresh = savedBay(false); val freshBefore = fresh.data.toString()
        ActivityScenario.launch<FirstAidBayActivity>(bayIntent(fresh)).use { scenario ->
            scenario.onActivity {
                assertFalse(field(it, "resumeChoicePending") as Boolean)
                assertTrue(all(it.window.decorView).filterIsInstance<Button>().none { button -> button.tag == "bay-resume-continue" })
            }
        }
        assertEquals(freshBefore, bayRecord(fresh.data.getString("id")))
        val old = savedBay(); val before = old.data.toString()
        ActivityScenario.launch<FirstAidBayActivity>(bayIntent(old)).use { scenario ->
            scenario.onActivity { assertTrue(field(it, "resumeChoicePending") as Boolean); it.finish() }
        }
        assertEquals(before, bayRecord(old.data.getString("id")))
    }
}
