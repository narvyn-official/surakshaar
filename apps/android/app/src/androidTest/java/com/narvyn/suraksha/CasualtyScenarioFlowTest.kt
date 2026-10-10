package com.narvyn.suraksha

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real native controls and rendered 3D touch path; no AR or physical treatment claims. */
@RunWith(AndroidJUnit4::class)
class CasualtyScenarioFlowTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = ins.targetContext
    private lateinit var original: String
    private lateinit var worker: String
    private lateinit var evidenceBefore: String
    private var oldHindi = false
    private var oldFont: String? = null
    private val requestedFont get() = InstrumentationRegistry.getArguments().getString("fontScale")
    private val landscape get() = InstrumentationRegistry.getArguments().getString("orientation") == "landscape"
    private fun field(value: Any, name: String): Any? = value.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(value)
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    private fun shell(command: String) = ins.uiAutomation.executeShellCommand(command).use { FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim() }
    private fun waitUntil(message: String, test: () -> Boolean) { repeat(150) { if (test()) return; Thread.sleep(100) }; error(message) }
    private fun tap(text: String) {
        val root = ins.uiAutomation.rootInActiveWindow
        val shown = root?.findAccessibilityNodeInfosByText(text)?.any { it.text?.toString() == text && it.isVisibleToUser } == true
        if (!shown && text in setOf("View", "Audio", "Cue", "Pause", "Resume")) {
            onView(withContentDescription(if (text in setOf("Pause", "Resume")) "$text practice" else text)).perform(click())
            return
        }
        if (!shown && text != "Choose the next action" && root?.findAccessibilityNodeInfosByText("Choose the next action")?.any { it.isVisibleToUser } == true)
            onView(withText("Choose the next action")).perform(revealOnPage(), click())
        onView(withText(text)).perform(revealOnPage(), click())
    }
    private fun back() { shell("input keyevent 4"); ins.waitForIdleSync(); Thread.sleep(200) }
    private fun bay(scenario: ActivityScenario<FirstAidBayActivity>): FirstAidBay {
        lateinit var result: FirstAidBay; scenario.onActivity { result = field(it, "attempt") as FirstAidBay }; return result
    }
    private fun scene(scenario: ActivityScenario<FirstAidBayActivity>): AnatomyScene {
        lateinit var result: AnatomyScene; scenario.onActivity { result = field(it, "scene") as AnatomyScene }; return result
    }
    private fun launch(id: String): ActivityScenario<FirstAidBayActivity> {
        val result = ActivityScenario.launch<FirstAidBayActivity>(Intent(context, FirstAidBayActivity::class.java).putExtra("scenarioId", id))
        if (landscape) {
            result.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            waitUntil("Landscape configuration did not settle") { var ready = false; result.onActivity { ready = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }; ready }
        }
        return result
    }
    private fun enter(scenario: ActivityScenario<FirstAidBayActivity>) {
        tap("Use 3D practice")
        waitUntil("Body scene did not become interactive") { var ready = false; scenario.onActivity { ready = (field(it, "scene") as? AnatomyScene)?.interactionReady() == true }; ready }
        rendered(scene(scenario))
    }
    private fun finish(scenario: ActivityScenario<FirstAidBayActivity>) {
        scenario.onActivity { it.finish() }; waitUntil("Activity did not finish") { scenario.state == Lifecycle.State.DESTROYED }; scenario.close()
    }
    private fun screenshot(name: String) {
        ins.waitForIdleSync(); Thread.sleep(250)
        val image = ins.uiAutomation.takeScreenshot() ?: error("Could not capture emulator")
        File(context.getExternalFilesDir("validation"), "$name.png").outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }; image.recycle()
    }
    private fun rendered(scene: AnatomyScene) {
        var lastCode = -1; var lastColours = 0
        repeat(30) {
            val done = CountDownLatch(1); val code = AtomicInteger(-1); val colours = AtomicInteger(0)
            ins.runOnMainSync {
                val surface = views(scene).filterIsInstance<GLSurfaceView>().single()
                if (surface.width <= 0 || surface.height <= 0) { done.countDown(); return@runOnMainSync }
                val bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(surface, bitmap, { value ->
                    code.set(value)
                    if (value == PixelCopy.SUCCESS) {
                        val unique = HashSet<Int>()
                        for (x in 0 until bitmap.width step 8) for (y in 0 until bitmap.height step 8) unique.add(bitmap.getPixel(x, y))
                        colours.set(unique.size)
                    }
                    bitmap.recycle(); done.countDown()
                }, Handler(Looper.getMainLooper()))
            }
            assertTrue(done.await(2, TimeUnit.SECONDS)); lastCode = code.get(); lastColours = colours.get()
            if (lastCode == PixelCopy.SUCCESS && lastColours >= 8) return
            Thread.sleep(100)
        }
        fail("Body not rendered: PixelCopy=$lastCode sampledColours=$lastColours")
    }
    /** Project the authored source-space cue with the renderer's actual completed-frame matrix. */
    private fun touchCue(scenario: ActivityScenario<FirstAidBayActivity>, duration: Long = 80, move: Boolean = false, enterLate: Boolean = false, leaveAndReturn: Boolean = false) {
        val scene = scene(scenario); rendered(scene); Thread.sleep(100)
        lateinit var surface: GLSurfaceView; lateinit var region: String
        val location = IntArray(2); val matrix = FloatArray(16); val copied = CountDownLatch(1)
        scenario.onActivity {
            surface = views(scene).filterIsInstance<GLSurfaceView>().single(); surface.getLocationOnScreen(location)
            region = requireNotNull(scene.controls.cueRegion)
            surface.queueEvent { (field(scene, "mvp") as FloatArray).copyInto(matrix); copied.countDown() }
        }
        assertTrue("Could not read the rendered frame matrix", copied.await(2, TimeUnit.SECONDS))
        val centre = CasualtyMarkers.centres.getValue(region)
        val clip = FloatArray(4); Matrix.multiplyMV(clip, 0, matrix, 0, floatArrayOf(centre[0], centre[1], centre[2], 1f), 0)
        assertTrue("Cue must be in front of the camera", clip[3] > 0f)
        val x = (clip[0] / clip[3] + 1) * .5f * surface.width
        val y = (1 - clip[1] / clip[3]) * .5f * surface.height
        assertTrue("Cue x=$x is outside the actual viewport", x in 1f..(surface.width - 1f))
        assertTrue("Cue y=$y is outside the actual viewport", y in 1f..(surface.height - 1f))
        val outside = FloatArray(4)
        Matrix.multiplyMV(outside, 0, matrix, 0, floatArrayOf(centre[0] + .10f, centre[1], centre[2], 1f), 0)
        val outsideX = (outside[0] / outside[3] + 1) * .5f * surface.width
        val outsideY = (1 - outside[1] / outside[3]) * .5f * surface.height
        if (leaveAndReturn) assertTrue("Test must leave the cue within the general motion tolerance", kotlin.math.hypot(outsideX - x, outsideY - y) < context.dp(24))
        val down = SystemClock.uptimeMillis()
        fun event(action: Int, dx: Float = 0f, atOutside: Boolean = false) {
            val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, (if (atOutside) outsideX else x) + location[0] + dx, (if (atOutside) outsideY else y) + location[1], 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
            try { ins.sendPointerSync(e) } finally { e.recycle() }
        }
        event(MotionEvent.ACTION_DOWN, atOutside = enterLate)
        if (move) { Thread.sleep(100); event(MotionEvent.ACTION_MOVE, context.dp(36).toFloat()) }
        if (leaveAndReturn) {
            Thread.sleep(800); event(MotionEvent.ACTION_MOVE, atOutside = true)
            Thread.sleep(800); event(MotionEvent.ACTION_MOVE); Thread.sleep(100)
        } else Thread.sleep(duration)
        if (enterLate) { event(MotionEvent.ACTION_MOVE); Thread.sleep(100) }
        // Software rendering may start its first hold frame later than ACTION_DOWN.
        // Wait for the actual displayed ring, within a strict bound; never inject completion.
        if (duration >= 1500 && !move && !enterLate && !leaveAndReturn) {
            val samples = JSONArray()
            var complete = false
            for (sampleIndex in 0 until 40) {
                val progress = (field(scene, "holdProgress") as java.util.concurrent.atomic.AtomicReference<*>).get()
                val fraction = progress?.let { field(it, "fraction") as Float } ?: -1f
                val age = progress?.let { SystemClock.elapsedRealtime() - (field(it, "at") as Long) } ?: -1L
                samples.put(JSONObject().put("fraction", fraction).put("ageMs", age))
                if (fraction >= 1f && age in 0..150) { complete = true; break }
                Thread.sleep(100)
            }
            File(context.getExternalFilesDir("validation"), "casualty-hold-diagnostics.json").writeText(
                JSONObject().put("region", region).put("x", x).put("y", y).put("completeBeforeRelease", complete).put("samples", samples).toString(2))
        }
        event(MotionEvent.ACTION_UP, if (move) context.dp(36).toFloat() else 0f)
        ins.waitForIdleSync(); Thread.sleep(200)
    }
    private fun answerCorrect(scenario: ActivityScenario<FirstAidBayActivity>) {
        val current = bay(scenario); val expectedIndex = current.index + 1
        tap(current.current.actions.first { it.correct }.label)
        assertEquals(expectedIndex, bay(scenario).index)
        tap(if (bay(scenario).finished) "Review practice" else "Next step")
    }
    private fun assertReachable(host: FirstAidBayActivity, dock: Boolean) {
        val scene = field(host, "scene") as AnatomyScene
        assertTrue("Casualty viewport collapsed: ${scene.width}x${scene.height}", scene.height >= host.dp(96) && scene.width >= host.dp(120))
        for (button in views(host.window.decorView).filterIsInstance<Button>().filter { it.isShown }) {
            val rect = Rect(); assertTrue("${button.text} not visible", button.getGlobalVisibleRect(rect))
            assertTrue("${button.text} too small", button.height >= host.dp(48))
            assertTrue("${button.text} vertically clipped", rect.height() >= button.height * .95f)
            assertTrue("${button.text} horizontally clipped", rect.width() >= button.width * .95f)
            val layout = button.layout
            assertTrue("${button.text} label clipped", button.text.isBlank() || layout == null || layout.height <= button.height - button.compoundPaddingTop - button.compoundPaddingBottom)
        }
        val labels = if (dock && host.resources.configuration.screenHeightDp < 480) emptyList() else if (dock) listOf("stage", "status") else listOf("stage", "cue", "status")
        for (name in labels) {
            val text = field(host, name) as TextView; val rect = Rect()
            assertTrue("$name hidden", text.getGlobalVisibleRect(rect))
            assertTrue("$name clipped", rect.height() >= text.height * .95f)
            assertTrue("$name lines clipped", text.layout == null || text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom)
        }
    }
    @Before fun isolate() {
        assumeTrue("Never run synthetic fixtures on a learner's phone", Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        requestedFont?.let { require(it in setOf("1.3", "2.0")); oldFont = shell("settings get system font_scale"); shell("settings put system font_scale $it") }
        Store(context).use {
            original = it.workerId; oldHindi = it.hi
            evidenceBefore = JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString()
            worker = it.createProfile("Synthetic casualty validation", "Unspecified"); it.switchProfile(worker); it.hi = false
        }
    }
    @After fun restore() {
        oldFont?.let { if (it == "null") shell("settings delete system font_scale") else shell("settings put system font_scale $it") }
        if (!::worker.isInitialized) return
        Store(context).use { it.switchProfile(original); it.hi = oldHindi }
        Store(context).use {
            assertEquals(evidenceBefore, JSONObject().put("attempts", JSONArray(it.attempts())).put("credentials", JSONArray(it.credentials())).toString())
            it.writableDatabase.delete("worker_profiles", "id=?", arrayOf(worker))
        }
        SkillPracticeStore(context, worker, "first-aid-bay.db").use { it.writableDatabase.delete("practice", "worker=?", arrayOf(worker)) }
        context.getSharedPreferences("learning-profile", 0).edit().remove("context-$worker").commit()
    }

    @Test fun bleedingUsesRealCueTouchAndHoldPreservesMistakesAndResumesTheSameAttempt() {
        val scenario = launch("bleeding-injury")
        try {
            enter(scenario)
            val id = bay(scenario).data.getString("id")
            tap(bay(scenario).current.actions.first { !it.correct }.label)
            assertEquals(0, bay(scenario).index); assertEquals("incorrect", bay(scenario).events.objects().last().getString("reason"))
            tap("Try again"); answerCorrect(scenario)
            assertEquals("observe", bay(scenario).current.id)
            assertEquals("RIGHT_FOREARM", scene(scenario).controls.cueRegion); assertEquals("bleeding", scene(scenario).controls.casualtyDetail)
            val beforeControls = bay(scenario).events.toString(); val beforeDistance = scene(scenario).controls.distance
            tap("View"); onView(withContentDescription("Zoom in")).perform(click())
            assertTrue(scene(scenario).controls.distance < beforeDistance)
            tap("Move"); onView(withContentDescription("Move view left")).perform(click())
            assertTrue(scene(scenario).controls.panX < 0f)
            tap("Angle"); onView(withContentDescription("Recenter and reset view")).perform(click())
            back(); scenario.onActivity { assertFalse(it.isFinishing); assertNull(field(it, "dock")) }
            assertEquals("View controls cannot fabricate learning actions", beforeControls, bay(scenario).events.toString())
            touchCue(scenario)
            assertEquals(2, bay(scenario).index); assertEquals("3d", bay(scenario).events.objects().last().getString("mode"))
            tap("Next step")
            tap("Pause"); scenario.recreate(); enter(scenario)
            assertEquals(id, bay(scenario).data.getString("id")); assertEquals(2, bay(scenario).index)
            assertTrue(bay(scenario).events.objects().any { it.optString("reason") == "incorrect" })
            assertTrue(bay(scenario).events.objects().any { it.getString("kind") == "pause" })
            assertTrue(bay(scenario).events.objects().any { it.getString("kind") == "resume" })
            while (bay(scenario).current.id != "pressure") answerCorrect(scenario)
            assertTrue(scene(scenario).controls.cueHold)
            val beforeHold = bay(scenario).events.toString(); val beforeIndex = bay(scenario).index
            touchCue(scenario, 100); assertEquals("A short tap is not the hold gesture", beforeHold, bay(scenario).events.toString())
            touchCue(scenario, 1600, move = true); assertEquals("Moving cancels a hold", beforeHold, bay(scenario).events.toString())
            touchCue(scenario, 1700, enterLate = true); assertEquals("Time spent away from the wound cannot satisfy the hold", beforeHold, bay(scenario).events.toString())
            touchCue(scenario, 1700, leaveAndReturn = true); assertEquals("Leaving the wound resets dwell even inside the movement tolerance", beforeHold, bay(scenario).events.toString())
            screenshot("casualty-pressure-before-hold")
            touchCue(scenario, 1700); assertEquals(beforeIndex + 1, bay(scenario).index)
            assertEquals("apply-pressure", bay(scenario).events.objects().last().getString("actionId"))
            tap("Next step")
            while (!bay(scenario).finished) answerCorrect(scenario)
            screenshot("casualty-bleeding-complete")
            assertEquals("3d", bay(scenario).presentation); assertFalse(bay(scenario).data.getBoolean("certifiable"))
            assertEquals("not-assessed", bay(scenario).data.getString("practicalCompetence"))
            assertEquals(bay(scenario).steps.size, bay(scenario).events.objects().count { it.optBoolean("advanced") })
        } finally { finish(scenario) }
    }

    @Test fun explicitSituationsKeepSeparateRecordsAndUseDistinctRenderedCues() {
        val catalog = LearningCatalog(context)
        val previous = FirstAidBay.createScenario(worker, catalog.version, "bleeding-injury")
        previous.act("ready-space", "3d", false); previous.pause()
        val previousBytes = previous.data.toString()
        SkillPracticeStore(context, worker, "first-aid-bay.db").use { it.save(previous.data) }
        for ((id, detail) in listOf("thermal-burn-response" to "scald", "suspected-limb-injury" to "limb", "unresponsive-normal-breathing" to null)) {
            val scenario = launch(id)
            try {
                enter(scenario)
                assertEquals(id, bay(scenario).scenarioId); assertNotEquals(previous.data.getString("id"), bay(scenario).data.getString("id"))
                answerCorrect(scenario)
                assertEquals(detail, scene(scenario).controls.casualtyDetail)
                // Normal-breathing starts with a response decision, not a spatial inspection.
                val expectedRegion = when (id) {
                    "thermal-burn-response" -> "LEFT_FOREARM"
                    "suspected-limb-injury" -> "RIGHT_LOWER_LEG"
                    else -> null
                }
                assertEquals(expectedRegion, scene(scenario).controls.cueRegion)
                rendered(scene(scenario)); screenshot("casualty-$id-observe")
                while (!bay(scenario).finished) {
                    if (bay(scenario).currentVisual.pose == FirstAidScenarioCatalog.Pose.RECOVERY_SIDE) {
                        assertEquals(90f, scene(scenario).controls.bodyRoll, .001f); rendered(scene(scenario)); screenshot("casualty-side-lying-illustration")
                    }
                    answerCorrect(scenario)
                }
                assertFalse(bay(scenario).data.getBoolean("certifiable")); assertEquals("3d", bay(scenario).presentation)
            } finally { finish(scenario) }
        }
        SkillPracticeStore(context, worker, "first-aid-bay.db").use { records ->
            assertEquals("An explicit new situation must not mutate the unrelated saved attempt", previousBytes, records.records().single { it.getString("id") == previous.data.getString("id") }.toString())
            assertEquals(3, records.records().count { it.optBoolean("finished") })
        }
    }

    @Test fun casualtyAndViewControlsFitTheViewportAndBackPreservesProgress() {
        val scenario = launch("bleeding-injury")
        val suffix = "${if (landscape) "landscape" else "portrait"}-${requestedFont ?: "1.0"}"
        try {
            enter(scenario); answerCorrect(scenario); rendered(scene(scenario))
            screenshot("casualty-layout-$suffix")
            scenario.onActivity { assertReachable(it, false) }
            val index = bay(scenario).index; val id = bay(scenario).data.getString("id"); val events = bay(scenario).events.toString()
            tap("View"); screenshot("casualty-controls-$suffix")
            scenario.onActivity { assertReachable(it, true) }
            tap("Move"); screenshot("casualty-move-$suffix"); scenario.onActivity { assertReachable(it, true) }
            back(); scenario.onActivity { assertNull(field(it, "dock")); assertFalse(it.isFinishing) }
            assertEquals(events, bay(scenario).events.toString())
            back(); scenario.onActivity { assertFalse(it.isFinishing); assertFalse(field(it, "practising") as Boolean) }
            assertTrue(bay(scenario).paused); enter(scenario)
            assertEquals(index, bay(scenario).index); assertEquals(id, bay(scenario).data.getString("id"))
        } finally { finish(scenario) }
    }
}
