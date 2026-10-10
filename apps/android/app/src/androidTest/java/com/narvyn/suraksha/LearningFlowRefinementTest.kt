package com.narvyn.suraksha

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
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
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicReference

/** Synthetic emulator-only navigation checks. Camera activities are intercepted, never simulated as real AR. */
@RunWith(AndroidJUnit4::class)
class LearningFlowRefinementTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = ins.targetContext
    private lateinit var original: String
    private lateinit var worker: String
    private lateinit var previous: String
    private var oldHindi = false
    private var oldFontScale: String? = null
    private val fontScale get() = InstrumentationRegistry.getArguments().getString("fontScale")
    private fun shell(command: String) = ins.uiAutomation.executeShellCommand(command).use {
        FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim()
    }
    private fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup)
        (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun tap(tag: String) = onView(withTagValue(equalTo<Any>(tag))).perform(revealOnPage(), click())
    private fun course(id: String) = Intent(context, LaunchCourseActivity::class.java).putExtra("courseId", id)
    private fun capture(name: String) {
        ins.waitForIdleSync(); ins.uiAutomation.waitForIdle(100, 2000)
        val bitmap = ins.uiAutomation.takeScreenshot() ?: error("No rendered screenshot")
        File(context.getExternalFilesDir("validation"), "learning-flow-$name-${fontScale ?: "1.0"}.png")
            .outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
    private fun captureLaunch(action: () -> Unit): Intent {
        val captured = AtomicReference<Intent?>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.component?.className?.startsWith("com.narvyn.suraksha.") != true) return null
                captured.set(Intent(intent)); return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        ins.addMonitor(monitor)
        try { action(); ins.waitForIdleSync(); return captured.get() ?: error("No activity launched") }
        finally { ins.removeMonitor(monitor) }
    }
    @Before fun isolate() {
        assumeTrue("Never change the learner's physical phone", Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        fontScale?.let { require(it in setOf("1.3", "2.0")); oldFontScale = shell("settings get system font_scale"); shell("settings put system font_scale $it") }
        Store(context).use {
            original = it.workerId; oldHindi = it.hi
            previous = JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString()
            worker = it.createProfile("Synthetic direct learning flow", "Unspecified"); it.switchProfile(worker); it.hi = false
        }
    }
    @After fun restore() {
        oldFontScale?.let { if (it == "null") shell("settings delete system font_scale") else shell("settings put system font_scale $it") }
        if (!::worker.isInitialized) return
        Store(context).use { it.switchProfile(original); it.hi = oldHindi }
        Store(context).use {
            assertEquals(previous, JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString())
            it.writableDatabase.delete("worker_profiles", "id=?", arrayOf(worker))
        }
        for (db in listOf("launch-learning.db", "learning-revision.db")) SkillPracticeStore(context, worker, db).use {
            it.writableDatabase.delete("practice", "worker=?", arrayOf(worker))
        }
        context.getSharedPreferences("learning-profile", 0).edit().remove("context-$worker").commit()
    }

    @Test fun choosingLessonOpensItImmediatelyAndCourseFitsNormalPortrait() {
        val catalog = LearningCatalog(context)
        val target = catalog.scenario("heat-illness")
        ActivityScenario.launch<LaunchCourseActivity>(course("first-aid")).use { scenario ->
            scenario.onActivity { activity ->
                val all = views(activity.window.decorView)
                assertTrue(all.none { it is android.widget.ScrollView })
                assertTrue(all.filterIsInstance<Button>().none { it.contentDescription == "Previous lesson" || it.contentDescription == "Next lesson" })
                if (fontScale == null) assertEquals("Course starts in one screen", 1, all.filterIsInstance<PagedPanel>().single().pageCount)
            }
            capture("first-aid-course")
            tap("choose-lesson")
            val launched = captureLaunch { onView(withText(target.getString("title"))).perform(revealOnPage(), click()) }
            assertEquals(LearningScenarioActivity::class.java.name, launched.component?.className)
            assertEquals(target.getString("id"), launched.getStringExtra("scenarioId"))
            LearningProgress(context, worker).use { assertTrue("Choosing only must not manufacture a practice result", it.records().isEmpty()) }
        }
    }

    @Test fun courseRecommendsSavedUnfinishedLessonAndPreservesItsEvents() {
        val catalog = LearningCatalog(context)
        val practice = LearningAttempt.create(catalog.scenario("heat-illness"), catalog.version, worker)
        practice.answer(practice.current.getJSONArray("choices").objects().first { it.getBoolean("correct") }.getString("id"))
        LearningProgress(context, worker).use { it.save(practice) }
        val before = practice.data.toString()
        ActivityScenario.launch<LaunchCourseActivity>(course("first-aid")).use { scenario ->
            tap("launch-heat-illness") // Choose the saved attempt; its pending feedback must remain intact.
            tap("learning-resume-continue")
            onView(withText("Decision saved")).perform(revealOnPage()).check(matches(isDisplayed()))
            pressBack(); ins.waitForIdleSync()
            scenario.recreate()
            val launched = captureLaunch { tap("launch-heat-illness") }
            assertEquals("heat-illness", launched.getStringExtra("scenarioId"))
            assertFalse(launched.getBooleanExtra("newPractice", false))
            capture("resume-lesson")
        }
        LearningProgress(context, worker).use { assertEquals(before, it.records().single().toString()) }
    }

    @Test fun schoolContextKeepsOnlyAwarenessLessonsAndBodyExploration() {
        context.getSharedPreferences("learning-profile", 0).edit().putString("context-$worker", "School").commit()
        val catalog = LearningCatalog(context)
        ActivityScenario.launch<LaunchCourseActivity>(course("first-aid")).use { scenario ->
            scenario.onActivity { activity ->
                val all = views(activity.window.decorView)
                assertNull(all.firstOrNull { it.tag == "first-aid-bay" })
                assertNotNull(all.firstOrNull { it.tag == "explore-body" })
            }
            capture("school-course")
            tap("choose-lesson")
            val adult = catalog.scenario("adult-collapse").getString("title")
            onView(withText(adult)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
            val awareness = catalog.scenario("school-injury-help")
            val launched = captureLaunch { onView(withText(awareness.getString("title"))).perform(revealOnPage(), click()) }
            assertEquals(awareness.getString("id"), launched.getStringExtra("scenarioId"))
        }
    }

    @Test fun fireModeAndScenarioLaunchWithoutASecondPicker() {
        ActivityScenario.launch<LaunchCourseActivity>(course("fire")).use {
            capture("fire-course")
            for (camera in listOf(false, true)) {
                tap(if (camera) "fire-ar-practice" else "fire-3d-practice")
                val fire = FireRoomScenarioCatalog.all.first()
                val launched = captureLaunch { onView(withText(fire.title(false))).perform(revealOnPage(), click()) }
                assertEquals(RoomMissionActivity::class.java.name, launched.component?.className)
                assertEquals(fire.id, launched.getStringExtra("fireScenarioId"))
                assertEquals(camera, launched.getBooleanExtra("camera", !camera))
            }
        }
    }

    @Test fun lessonStillRecordsEveryDecisionAndContinueThenOffersNextLesson() {
        val catalog = LearningCatalog(context)
        val lesson = catalog.scenario("escape-first")
        assertTrue(lesson.getJSONArray("steps").objects().none { it.optBoolean("requiresObservation") })
        ActivityScenario.launch<LearningScenarioActivity>(Intent(context, LearningScenarioActivity::class.java).putExtra("scenarioId", "escape-first")).use {
            lesson.getJSONArray("steps").objects().forEachIndexed { index, step ->
                if (index == 0) {
                    val wrong = step.getJSONArray("choices").objects().first { !it.getBoolean("correct") }
                    tap("learning-choice-${wrong.getString("id")}")
                }
                val correct = step.getJSONArray("choices").objects().single { it.getBoolean("correct") }
                tap("learning-choice-${correct.getString("id")}")
                onView(withText("Decision saved")).perform(revealOnPage()).check(matches(isDisplayed()))
                tap("learning-next")
            }
            onView(withText("Practice complete")).perform(revealOnPage()).check(matches(isDisplayed()))
            capture("completion")
            val record = LearningProgress(context, worker).use { it.records().single() }
            val saved = LearningAttempt.restore(record, lesson, catalog.version, worker)
            assertTrue(saved.finished)
            assertEquals(saved.steps.size + 1, saved.events.objects().count { event -> event.optString("kind") == "decision" })
            assertEquals(saved.steps.size, saved.events.objects().count { event -> event.optString("kind") == "continue" })
            tap("learning-completion-tools")
            onView(withText("Try without guidance")).perform(revealOnPage()).check(matches(isDisplayed()))
            onView(withText("Back")).perform(click())
            val launched = captureLaunch { tap("learning-next-lesson") }
            assertEquals(LearningScenarioActivity::class.java.name, launched.component?.className)
            assertNotEquals("escape-first", launched.getStringExtra("scenarioId"))
            LearningProgress(context, worker).use { assertEquals(record.toString(), it.records().single().toString()) }
        }
    }
}
