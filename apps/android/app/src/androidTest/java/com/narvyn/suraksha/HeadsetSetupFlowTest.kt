package com.narvyn.suraksha

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Native setup checks only. No physical lens fit, audible voice or head tracking is inferred. */
@RunWith(AndroidJUnit4::class)
class HeadsetSetupFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private lateinit var original:String
    private lateinit var worker:String
    private var hindi=false
    private fun views(view:View):List<View> = listOf(view)+if(view is ViewGroup)(0 until view.childCount).flatMap{views(view.getChildAt(it))}else emptyList()
    private fun tagged(name:String)=onView(withTagValue(`is`(name as Any)))
    private fun capture(name:String){
        ins.waitForIdleSync()
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        File(context.getExternalFilesDir("validation"),"headset-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun waitFor(test:()->Boolean){repeat(100){if(test())return;Thread.sleep(100)};error("Setup did not settle")}
    @Before fun isolate(){
        assumeTrue(Build.HARDWARE in setOf("ranchu","goldfish"))
        Store(context).use{original=it.workerId;hindi=it.hi;worker=it.createProfile("Synthetic headset setup","Unspecified");it.switchProfile(worker);it.hi=false}
    }
    @After fun restore(){
        if(!::worker.isInitialized)return
        context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()
        Store(context).use{it.switchProfile(original);it.hi=hindi;it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))}
    }
    private fun landscape(activity:ActivityScenario<HeadsetActivity>){
        if(InstrumentationRegistry.getArguments().getString("orientation")=="landscape"){
            activity.onActivity{it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            waitFor{var done=false;activity.onActivity{done=it.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE};done}
        }
    }
    @Test fun schoolChooserRejectsAdultDeepLinkAndBackReturnsOneStep(){
        context.getSharedPreferences("learning-profile",0).edit().putString("context-$worker","School").commit()
        val intent=Intent(context,HeadsetActivity::class.java).putExtra("scenarioId","adult-collapse").putExtra("preview",true)
        ActivityScenario.launch<HeadsetActivity>(intent).use{activity->
            landscape(activity)
            activity.onActivity{host->
                val all=views(host.window.decorView)
                assertTrue(all.none{it is ScrollView})
                assertTrue("Adult deep link must not open readiness",all.none{it.tag=="headset-start"})
                val shown=all.mapNotNull{(it.tag as? String)?.removePrefix("headset-lesson-")?.takeIf{tag->it.tag=="headset-lesson-$tag"}}
                val catalog=LearningCatalog(host)
                for(id in shown)assertEquals("awareness",catalog.scenario(id).getString("audience"))
            }
            tagged("headset-course-first-aid").perform(revealOnPage(),click())
            tagged("headset-lesson-school-help").perform(revealOnPage(),click())
            activity.onActivity{host->
                val all=views(host.window.decorView)
                val start=all.single{it.tag=="headset-start"} as Button
                assertFalse("Clear-space acknowledgement cannot be skipped",start.isEnabled)
                assertEquals("Start in English",start.text.toString())
                assertNotNull(all.singleOrNull{it.tag=="headset-clear-space"})
                assertFalse((all.single{it.tag=="headset-clear-space"} as CheckBox).isChecked)
                val bounds=Rect();assertTrue("Start action stays visible",start.getGlobalVisibleRect(bounds));assertTrue(start.height>=host.dp(48))
            }
            capture("setup-school")
            activity.recreate()
            activity.onActivity{host->assertNotNull(views(host.window.decorView).singleOrNull{it.tag=="headset-start"})}
            tagged("headset-back").perform(click())
            activity.onActivity{host->assertFalse(host.isFinishing);assertTrue(views(host.window.decorView).none{it.tag=="headset-start"})}
            capture("chooser-school")
        }
    }
    @Test fun homeHasAHeadsetEntryAndKeepsHandheldCoursesAvailable(){
        ActivityScenario.launch(MainActivity::class.java).use{activity->
            activity.onActivity{host->
                val all=views(host.window.decorView)
                assertNotNull(all.singleOrNull{it.tag=="main-headset"})
                assertNotNull(all.singleOrNull{it.tag=="main-handheld"})
                assertTrue("Home cannot repeat the course catalogue",all.none{(it.tag as? String)?.startsWith("course-")==true})
            }
            capture("home")
            tagged("main-handheld").perform(revealOnPage(),click())
            activity.onActivity{host->
                val tags=views(host.window.decorView).map{it.tag}
                assertTrue(tags.containsAll(listOf("course-fire","course-first-aid","main-learning-headset","main-learning-library")))
            }
            capture("courses")
        }
    }
}
