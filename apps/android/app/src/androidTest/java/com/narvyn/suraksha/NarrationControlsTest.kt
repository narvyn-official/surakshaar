package com.narvyn.suraksha

import android.app.AlertDialog
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Rect
import java.io.File
import java.io.FileInputStream
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Controls/state test only. It does not claim the emulator produced audible, correct pronunciation. */
@RunWith(AndroidJUnit4::class)
class NarrationControlsTest {
    @Test fun audioControlsKeepLanguageHonestRememberSpeedAndCloseWithoutLeavingLesson() {
        assumeTrue(Build.HARDWARE in listOf("ranchu", "goldfish") || Build.MODEL.contains("sdk_gphone"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val voice = context.getSharedPreferences("room-voice", 0)
        val language = context.getSharedPreferences("preferences", 0)
        val hadAutomatic = voice.contains("automatic"); val oldAutomatic = voice.getBoolean("automatic", true)
        val hadRate = voice.contains("rate"); val oldRate = voice.getFloat("rate", .9f)
        val oldHi = language.getBoolean("hi", false)
        voice.edit().putBoolean("automatic", false).commit(); language.edit().putBoolean("hi", true).commit()
        try {
            ActivityScenario.launch(DemonstrationActivity::class.java).use { activity ->
                if (InstrumentationRegistry.getArguments().getString("orientation") == "landscape") {
                    activity.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                    var landscape = false
                    repeat(50) { activity.onActivity { landscape = it.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }; if (!landscape) Thread.sleep(100) }
                    assertTrue(landscape)
                }
                lateinit var narrator: TrainingNarrator
                lateinit var dialog: AlertDialog
                activity.onActivity { host ->
                    val field = DemonstrationActivity::class.java.getDeclaredField("narrator").apply { isAccessible = true }
                    narrator = field.get(host) as TrainingNarrator
                    narrator.show("This practice has English text only.", "en-IN")
                    dialog = narrator.showControls()
                }
                onView(withTagValue(`is`("narration-status" as Any))).inRoot(isDialog()).check(matches(withText(containsString("हिन्दी आवाज़ उपलब्ध नहीं"))))
                onView(withText("अंग्रेज़ी में सुनें")).check(matches(isDisplayed()))
                for (tag in listOf("narration-play", "narration-replay", "narration-speaker", "narration-speed")) {
                    onView(withTagValue(`is`(tag as Any))).inRoot(isDialog()).check { view, missing ->
                        if (missing != null) throw missing
                        assertTrue(view is Button); assertTrue(view.isShown)
                        assertTrue("Audio targets remain 48 dp high", view.height >= context.dp(48))
                        val bounds=Rect();assertTrue(view.getGlobalVisibleRect(bounds))
                        assertTrue("Audio targets must not be clipped",bounds.height()>=view.height*.95f)
                        assertFalse(view.contentDescription.isNullOrBlank())
                    }
                }
                for (label in listOf("अंग्रेज़ी में सुनें", "आवाज़ सेटिंग", "पूरा हुआ")) {
                    onView(withText(label)).inRoot(isDialog()).check { view, missing ->
                        if (missing != null) throw missing
                        val bounds=Rect();assertTrue(view.getGlobalVisibleRect(bounds))
                        assertTrue("$label must be visible without scrolling", bounds.height() >= view.height * .95f)
                    }
                }
                val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No screenshot")
                val config=context.resources.configuration
                File(context.getExternalFilesDir("validation"),"narration-${config.orientation}-${config.fontScale}.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()
                onView(withTagValue(`is`("narration-speaker" as Any))).inRoot(isDialog()).perform(click())
                assertTrue(voice.getBoolean("automatic", false))
                onView(withTagValue(`is`("narration-speaker" as Any))).inRoot(isDialog()).perform(click())
                assertFalse(voice.getBoolean("automatic", true))
                onView(withTagValue(`is`("narration-play" as Any))).inRoot(isDialog()).perform(click())
                activity.onActivity { assertFalse("An English-only lesson must not silently use another language", narrator.isSpeaking) }
                onView(withTagValue(`is`("narration-speed" as Any))).inRoot(isDialog()).perform(click())
                onView(withText("धीरे · 0.75×")).perform(click())
                assertEquals(.75f, voice.getFloat("rate", 0f), .001f)
                // Target the actual focused dialog. Espresso's generic Back may select the
                // unfocused activity after dismissing a nested speed dialog on API 36.
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4")
                    .use { FileInputStream(it.fileDescriptor).readBytes() }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                repeat(30) { var showing=false; activity.onActivity { showing=dialog.isShowing }; if(showing)Thread.sleep(100) }
                activity.onActivity { host -> assertFalse(host.isFinishing); assertFalse(dialog.isShowing); host.finish() }
                repeat(50) { if (activity.state != androidx.lifecycle.Lifecycle.State.DESTROYED) Thread.sleep(100) }
                assertFalse(narrator.isSpeaking)
            }
        } finally {
            val edit = voice.edit()
            if (hadAutomatic) edit.putBoolean("automatic", oldAutomatic) else edit.remove("automatic")
            if (hadRate) edit.putFloat("rate", oldRate) else edit.remove("rate")
            edit.commit(); language.edit().putBoolean("hi", oldHi).commit()
        }
    }
}
