package com.narvyn.suraksha

import android.graphics.Rect
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Tests the actual compact dock and scene together, without changing any learner records. */
@RunWith(AndroidJUnit4::class)
class AnatomyViewControlsTest {
    private fun all(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else emptyList()
    private fun tap(name: String) = onView(withContentDescription(name)).perform(click())
    private fun capture(name: String) {
        val ins=InstrumentationRegistry.getInstrumentation();ins.waitForIdleSync();Thread.sleep(250)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        val config=ins.targetContext.resources.configuration
        File(ins.targetContext.getExternalFilesDir("validation"),"dock-$name-${config.orientation}-${config.fontScale}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
    }
    @Test fun repeatedEntryReleasesTheRenderingThreadAfterPause() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        fun glThreads()=Thread.getAllStackTraces().keys.count{it.isAlive&&it.name.startsWith("GLThread")}
        val baseline=glThreads()
        repeat(5){
            ActivityScenario.launch(AnatomyActivity::class.java).use { activity ->
                var loaded=false
                repeat(100){activity.onActivity{host->loaded=all(host.window.decorView).any{it is AnatomyScene}};if(!loaded)Thread.sleep(100)}
                assertTrue(loaded)
                // Exercise the user's real finish path (onPause precedes onDestroy). The API36
                // test invoker can stall its EmptyActivity handshake when closing from CREATED.
                activity.onActivity { it.finish() }
                repeat(50){if(activity.state!=androidx.lifecycle.Lifecycle.State.DESTROYED)Thread.sleep(100)}
                assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED,activity.state)
            }
            repeat(50){if(glThreads()>baseline)Thread.sleep(100)}
            assertTrue("Closing a paused body view must end its GL thread",glThreads()<=baseline)
        }
    }
    @Test fun compactControlsKeepModelVisibleAndBackClosesOnlyTheDock() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        ActivityScenario.launch(AnatomyActivity::class.java).use { activity ->
            val landscape = InstrumentationRegistry.getArguments().getString("orientation") == "landscape"
            if (landscape) {
                activity.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                var rotated = false
                repeat(50) {
                    activity.onActivity { rotated = it.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
                    if (!rotated) Thread.sleep(100)
                }
                assertTrue("Landscape verification must actually rotate the activity", rotated)
            }
            var scene: AnatomyScene? = null
            repeat(100) {
                activity.onActivity { host -> scene = all(host.window.decorView).filterIsInstance<AnatomyScene>().firstOrNull() }
                if (scene == null) Thread.sleep(100)
            }
            assertNotNull("Anatomy must load before controls can open", scene)
            val original = scene!!
            onView(withText("View")).perform(click())
            activity.onActivity { host ->
                val dock = all(host.window.decorView).filterIsInstance<AnatomyViewDock>().single()
                val bounds = Rect(); assertTrue(original.getGlobalVisibleRect(bounds))
                assertTrue("Controls must leave a useful model viewport", bounds.height() >= host.dp(120))
                assertTrue("Model viewport remains interactive", original.isShown)
                all(dock).filterIsInstance<Button>().forEach { button ->
                    val visible = Rect(); assertTrue("${button.contentDescription} must be visible", button.getGlobalVisibleRect(visible))
                    assertTrue("${button.contentDescription} must not be clipped", visible.height() >= button.height * .9f)
                    assertTrue("Buttons keep native touch height", button.height >= host.dp(48))
                }
                assertEquals(1, all(dock).filterIsInstance<SeekBar>().size)
            }
            capture("angle")
            val before = original.controls.distance
            tap("Zoom out")
            assertTrue("Zoom changes the live scene", original.controls.distance > before)
            tap("Back")
            assertEquals(180f, original.controls.yaw, .01f)
            tap("Move controls")
            capture("move")
            tap("Move view left")
            assertTrue(original.controls.panX < 0f)
            activity.onActivity { original.change { it.copy(isolated = true, selected = "FMA7485", hidden = setOf("FMA7197")) } }
            tap("Angle controls")
            tap("Recenter and reset view")
            assertEquals("FMA7485", original.controls.selected)
            assertTrue("Reset camera preserves exploration state", original.controls.isolated)
            assertTrue(original.controls.hidden.contains("FMA7197"))
            pressBack()
            activity.onActivity { host ->
                assertFalse("Back must not exit anatomy while controls are open", host.isFinishing)
                assertTrue(all(host.window.decorView).none { it is AnatomyViewDock })
                assertSame(original, all(host.window.decorView).filterIsInstance<AnatomyScene>().single())
            }
            onView(withText("View")).perform(click())
            tap("Look controls")
            capture("look")
            tap("Skin tone: Deep brown")
            assertEquals(4, original.controls.skinTone)
            tap("Close view controls")
            activity.recreate()
            var restored: AnatomyScene? = null
            repeat(100) {
                activity.onActivity { restored = all(it.window.decorView).filterIsInstance<AnatomyScene>().firstOrNull() }
                if (restored == null) Thread.sleep(100)
            }
            assertEquals("Appearance survives recreation", 4, restored!!.controls.skinTone)
            assertTrue(restored!!.controls.isolated)
        }
    }
}
