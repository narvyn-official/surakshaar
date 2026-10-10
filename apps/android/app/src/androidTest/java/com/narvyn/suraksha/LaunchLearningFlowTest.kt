package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Emulator-only, synthetic profiles. No camera claims, account setup or destructive database reset. */
@RunWith(AndroidJUnit4::class)
class LaunchLearningFlowTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = ins.targetContext
    private lateinit var original: String
    private lateinit var worker: String
    private lateinit var legacyBefore: String
    private var oldHindi = false
    private var oldFontScale: String? = null
    private val requestedFontScale get() = InstrumentationRegistry.getArguments().getString("fontScale")
    private fun field(value: Any, name: String): Any? = value.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(value)
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun tap(text: String) {
        val root = ins.uiAutomation.rootInActiveWindow
        val alreadyShown = root?.findAccessibilityNodeInfosByText(text)?.any { it.text?.toString() == text && it.isVisibleToUser } == true
        val chooser = "Choose the next action"
        if (!alreadyShown && text != chooser && root?.findAccessibilityNodeInfosByText(chooser)?.any { it.isVisibleToUser } == true)
            onView(withText(chooser)).perform(revealOnPage(), click())
        onView(withText(text)).perform(revealOnPage(), click())
    }
    private fun waitUntil(message: String, condition: () -> Boolean) {
        repeat(100) { if (condition()) return; Thread.sleep(100) }
        error(message)
    }
    /** Dirty rendering can finish in one frame; verify the actual GL pixels instead of a frame pump. */
    private fun waitForRenderedModel(scene: AnatomyScene) {
        var lastCode = -1; var lastColours = 0
        repeat(30) {
            val finished = CountDownLatch(1); val result = AtomicInteger(-1); val colours = AtomicInteger(0)
            ins.runOnMainSync {
                val surface = views(scene).filterIsInstance<GLSurfaceView>().single()
                if (surface.width <= 0 || surface.height <= 0) { finished.countDown(); return@runOnMainSync }
                val bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(surface, bitmap, { code ->
                    result.set(code)
                    if (code == PixelCopy.SUCCESS) {
                        val samples = HashSet<Int>()
                        for (x in 0 until bitmap.width step 8) for (y in 0 until bitmap.height step 8) samples.add(bitmap.getPixel(x, y))
                        colours.set(samples.size)
                    }
                    bitmap.recycle(); finished.countDown()
                }, Handler(Looper.getMainLooper()))
            }
            assertTrue("GL pixel copy did not finish", finished.await(2, TimeUnit.SECONDS))
            lastCode = result.get(); lastColours = colours.get()
            if (lastCode == PixelCopy.SUCCESS && lastColours >= 8) return
            Thread.sleep(100)
        }
        fail("The model viewport must contain rendered geometry: PixelCopy=$lastCode, sampled colours=$lastColours")
    }
    private fun shell(command: String) = ins.uiAutomation.executeShellCommand(command).use { FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim() }
    private fun screenshot(name: String) {
        ins.waitForIdleSync()
        // Allow the latest UI/SurfaceView frame to reach the compositor after a transition.
        Thread.sleep(250)
        val image = ins.uiAutomation.takeScreenshot() ?: error("Could not capture emulator rendering")
        val output = File(context.getExternalFilesDir("validation"), "$name.png")
        output.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }; image.recycle()
    }
    @Before fun isolatedEmulatorProfile() {
        assumeTrue("Synthetic UI tests must never run on the learner's physical phone", Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        requestedFontScale?.let { scale ->
            require(scale in setOf("1.3", "2.0"))
            oldFontScale = shell("settings get system font_scale")
            shell("settings put system font_scale $scale")
        }
        Store(context).use {
            original = it.workerId; oldHindi = it.hi
            legacyBefore = JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString()
            worker = it.createProfile("Synthetic launch validation", "Unspecified"); it.switchProfile(worker); it.hi = false
        }
    }
    @After fun restoreProfileWithoutTouchingExistingEvidence() {
        oldFontScale?.let { if (it == "null") shell("settings delete system font_scale") else shell("settings put system font_scale $it") }
        if (!::worker.isInitialized) return
        Store(context).use { it.switchProfile(original); it.hi = oldHindi }
        Store(context).use {
            assertEquals(legacyBefore, JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString())
            it.writableDatabase.delete("worker_profiles", "id=?", arrayOf(worker))
        }
        for (db in listOf("launch-learning.db", "first-aid-bay.db")) SkillPracticeStore(context, worker, db).use {
            it.writableDatabase.delete("practice", "worker=?", arrayOf(worker))
        }
        context.getSharedPreferences("learning-profile", 0).edit().remove("context-$worker").commit()
    }

    @Test fun packagedAnatomyRendersAndControlsRemainReachableWithLargeText() {
        val pack = AnatomyData.load(context)
        val manifest = JSONObject(context.assets.open("anatomy/manifest.json").bufferedReader().use { it.readText() })
        assertEquals(manifest.getJSONArray("parts").length(), pack.parts.size)
        assertTrue(pack.parts.map { it.id }.containsAll(listOf("FMA7485", "FMA7309", "FMA7310", "FMA7394", "FMA50801")))
        assertTrue(pack.manifest.getJSONArray("parts").objects().all { it.getString("file").endsWith(".meshpack") })
        assertEquals(setOf("skin", "skeleton", "muscles", "organs"), pack.parts.map { it.layer }.toSet())
        val previousScale = shell("settings get system font_scale")
        try {
            shell("settings put system font_scale ${requestedFontScale ?: "1.3"}")
            ActivityScenario.launch(AnatomyActivity::class.java).use { scenario ->
                waitUntil("Pack did not reach the native viewport") { var ready = false; scenario.onActivity { ready = field(it, "scene") != null }; ready }
                val rendered = AtomicReference<AnatomyScene>()
                scenario.onActivity { rendered.set(field(it, "scene") as AnatomyScene) }
                waitForRenderedModel(rendered.get())
                tap("Parts")
                onView(withHint("Find a structure")).perform(replaceText("FMA7485")).check(matches(withText("FMA7485"))).perform(closeSoftKeyboard())
                tap(AnatomyKnowledge.info("FMA7485")!!.title)
                onView(withContentDescription("About selected part")).perform(click()); tap("Show alone")
                val picked = AtomicReference<String>()
                scenario.onActivity { activity ->
                    val scene = field(activity, "scene") as AnatomyScene
                    val prior = scene.onPicked
                    scene.onPicked = { part, mode -> picked.set(part.id); prior(part, mode) }
                }
                // The GL touch path must intersect actual visible geometry, not merely select a list item.
                onView(isAssignableFrom(AnatomyScene::class.java)).perform(click())
                waitUntil("Touch did not select the focused visible sternum mesh") { picked.get() == "FMA7485" }
                scenario.onActivity { activity ->
                    val scene = field(activity, "scene") as AnatomyScene
                    assertEquals("FMA7485", scene.controls.selected)
                    for (text in listOf("Layers", "Parts", "View", "AR")) {
                        val button = views(activity.window.decorView).filterIsInstance<Button>().single { it.text.toString() == text }
                        val visible = Rect(); assertTrue("$text not visible at large text", button.getGlobalVisibleRect(visible))
                        assertTrue("$text clipped", visible.height() >= button.height * .9f)
                    }
                }
                screenshot("anatomy-sternum-${requestedFontScale ?: "1.3"}-text")
                tap("View"); onView(withContentDescription("Zoom out")).perform(click()); tap("Done")
                tap("Info"); pressBack()
                scenario.onActivity { assertFalse(it.isFinishing) }
            }
        } finally { if (previousScale == "null") shell("settings delete system font_scale") else shell("settings put system font_scale $previousScale") }
    }

    @Test fun firstAidBayCompletesIn3dAndPreservesMistakesPauseAndOldRecords() {
        val catalog = LearningCatalog(context)
        val old = JSONObject().put("id", UUID.randomUUID().toString()).put("workerId", worker)
            .put("scenarioId", FirstAidBay.SCENARIO_ID).put("contentVersion", "older-fixture")
            .put("answers", JSONArray()).put("updatedAt", 1L).put("index", 0).put("finished", false)
        SkillPracticeStore(context, worker, "first-aid-bay.db").use { it.save(old) }
        val parts = AnatomyData.load(context).parts.associateBy { it.id }
        ActivityScenario.launch<FirstAidBayActivity>(Intent(context,FirstAidBayActivity::class.java).putExtra("legacyBay",true)).use { scenario ->
            tap("Use 3D practice")
            var readiness = "No scene state observed"
            try { waitUntil("Practice bay did not enable its controls") { var ready = false; scenario.onActivity {
                val scene = field(it, "scene") as? AnatomyScene
                readiness = "interactionReady=${scene?.interactionReady()}, frames=${scene?.let { s -> field(s, "frames") }}, actions=" +
                    views(it.window.decorView).filterIsInstance<Button>().filter { b -> b.tag == "bay-action" }.joinToString { b -> "${b.text}:${b.isEnabled}" }
                ready = scene?.interactionReady() == true && (field(it, "model") as? AnatomyData)?.parts?.isNotEmpty() == true &&
                    views(it.window.decorView).filterIsInstance<Button>().any { b -> b.text.toString() in setOf("Start without checking the area", "Choose the next action") && b.isEnabled }
            }; ready } } catch (error:IllegalStateException) { throw IllegalStateException("${error.message}: $readiness",error) }
            val rendered = AtomicReference<AnatomyScene>()
            scenario.onActivity { rendered.set(field(it, "scene") as AnatomyScene) }
            waitForRenderedModel(rendered.get())
            tap("Start without checking the area"); tap("Try again")
            for (step in FirstAidBay.steps) {
                var current: FirstAidBay? = null; scenario.onActivity { current = field(it, "attempt") as FirstAidBay }
                assertEquals(step.id, current!!.current.id)
                if (step.id in setOf("buddy", "aed_power")) {
                    scenario.onActivity { activity ->
                        val scene = field(activity, "scene") as AnatomyScene
                        assertTrue("Practice viewport collapsed at ${step.id}", scene.height >= activity.dp(96))
                        for (button in views(activity.window.decorView).filterIsInstance<Button>().filter { it.isShown }) {
                            val rect = Rect(); assertTrue("${button.text} not reachable", button.getGlobalVisibleRect(rect))
                            assertTrue("${button.text} clipped", rect.height() >= button.height * .9f)
                        }
                    }
                    screenshot("first-aid-bay-${step.id}-${requestedFontScale ?: "default"}-text")
                }
                when {
                    step.partId != null -> { tap("Find structure by name"); tap(parts.getValue(step.partId).name) }
                    step.id == "aed_power" -> tap("Power button alternative · 3D")
                    else -> tap(step.actions.first { it.correct }.label)
                }
                tap(if (step == FirstAidBay.steps.last()) "Review practice" else "Next step")
                if (step.id == "buddy") {
                    tap("Pause"); scenario.recreate(); tap("Use 3D practice")
                    waitUntil("Bay did not resume") { var ready = false; scenario.onActivity { ready = (field(it, "scene") as? AnatomyScene)?.interactionReady() == true }; ready }
                    scenario.onActivity { assertEquals("chest", (field(it, "attempt") as FirstAidBay).current.id) }
                }
            }
            screenshot("first-aid-bay-complete")
        }
        val saved = SkillPracticeStore(context, worker, "first-aid-bay.db").use { it.records() }
        assertEquals(old.toString(), saved.single { it.getString("id") == old.getString("id") }.toString())
        val bay = FirstAidBay.restore(saved.single { it.getString("contentVersion") == catalog.version }, worker, catalog.version)
        assertTrue(bay.finished); assertEquals("3d", bay.presentation); assertFalse(bay.data.getBoolean("certifiable"))
        assertTrue(bay.events.objects().any { it.optString("reason") == "incorrect" })
        assertTrue(bay.events.objects().any { it.getString("kind") == "pause" })
        assertTrue(bay.events.objects().any { it.getString("kind") == "resume" })
        assertEquals(12, bay.events.objects().count { it.optBoolean("advanced") })
    }

    @Test fun cancelledObservationDoesNotAdvanceAndSelectionReturnsOnlyExplorationEvidence() {
        val catalog = LearningCatalog(context)
        val attempt = LearningAttempt.create(catalog.scenario("adult-collapse"), catalog.version, worker)
        while (!attempt.current.optBoolean("requiresObservation")) {
            attempt.answer(attempt.current.getJSONArray("choices").objects().first { it.getBoolean("correct") }.getString("id"))
            attempt.advance()
        }
        LearningProgress(context, worker).use { it.save(attempt) }
        val index = attempt.index; val priorEvents = attempt.events.length()
        val part = AnatomyData.load(context).parts.single { it.id == attempt.current.getString("part") }
        ActivityScenario.launch<LearningScenarioActivity>(Intent(context, LearningScenarioActivity::class.java).putExtra("scenarioId", "adult-collapse")).use { scenario ->
            tap("Continue")
            tap("Find in body")
            waitUntil("Anatomy child did not load") { ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Parts")?.any { it.isVisibleToUser } == true }
            pressBack()
            scenario.onActivity { assertEquals(priorEvents, (field(it, "attempt") as LearningAttempt).events.length()) }
            tap("Find in body")
            waitUntil("Anatomy child did not reload") { ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Parts")?.any { it.isVisibleToUser } == true }
            tap("Parts")
            onView(withHint("Find a structure")).perform(replaceText(part.id)).check(matches(withText(part.id))).perform(closeSoftKeyboard())
            tap(AnatomyKnowledge.info(part.id)!!.title); tap("Use this structure")
            scenario.onActivity {
                val current = field(it, "attempt") as LearningAttempt
                assertEquals(index, current.index)
                val event = current.events.objects().last()
                assertEquals("observation", event.getString("kind")); assertEquals("3d", event.getString("mode"))
                assertEquals(part.id, event.getString("partId")); assertFalse(current.finished)
            }
            screenshot("anatomy-observation-return")
        }
    }
}
