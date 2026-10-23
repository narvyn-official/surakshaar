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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.RootMatchers.isDialog
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
        VrLearningStore(context,worker).use{it.writableDatabase.delete("headset_practice","worker=?",arrayOf(worker))}
        Store(context).use{it.switchProfile(original);it.hi=hindi;it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))}
    }
    @Test fun separateVrActivityIsNotAnInstalledDestination(){
        val component=android.content.ComponentName(context,"com.narvyn.suraksha.VrTrainingActivity")
        try{context.packageManager.getActivityInfo(component,0);fail("Retired VR mode must not be installed")}
        catch(_:android.content.pm.PackageManager.NameNotFoundException){}
    }
    @Test fun headsetEntryUsesCameraPreflightAndKeepsTheCameraClosed(){
        val intent=Intent(context,HandsArActivity::class.java).putExtra("headset",true)
        ActivityScenario.launch<HandsArActivity>(intent).use{activity->
            activity.onActivity{host->
                val all=views(host.window.decorView)
                assertTrue(all.none{it is ScrollView})
                assertNotNull(all.singleOrNull{it.tag=="hands-ar-headset"})
                assertNotNull(all.singleOrNull{it.tag=="hands-ar-camera"})
                assertFalse((all.single{it.tag=="hands-ar-start"} as Button).isEnabled)
                val scene=host.javaClass.getDeclaredField("scene").apply{isAccessible=true}.get(host)
                assertNull("Opening a headset route must not open the camera",scene)
            }
            capture("camera-preflight")
        }
    }
    @Test fun earlierVrRecordRemainsReadableWithoutResumingOrChangingEvidence(){
        val catalog=LearningCatalog(context)
        val record=VrLearningSession.create(catalog.scenario("escape-first"),catalog.version,worker)
        VrLearningStore(context,worker).use{it.save(record)}
        val before=VrLearningStore(context,worker).use{it.record(record.data.getString("id")).toString()}
        val evidence=Store(context).use{it.attempts().toString()+it.credentials().toString()}
        ActivityScenario.launch(MainActivity::class.java).use{activity->
            tagged("main-nav-records").perform(click())
            tagged("main-headset-records").perform(revealOnPage(),click())
            tagged("headset-record-${record.data.getString("id")}").perform(revealOnPage(),click())
            activity.onActivity{host->assertFalse(host.isFinishing)}
            capture("earlier-record-read-only")
            onView(withText(org.hamcrest.Matchers.containsString("VR mode has been retired"))).inRoot(isDialog()).check(matches(isDisplayed()))
        }
        assertEquals(before,VrLearningStore(context,worker).use{it.record(record.data.getString("id")).toString()})
        assertEquals(evidence,Store(context).use{it.attempts().toString()+it.credentials().toString()})
    }
    @Test fun homeLeadsToCoursesAndExploreOwnsTheViewingModes(){
        ActivityScenario.launch(MainActivity::class.java).use{activity->
            activity.onActivity{host->
                val all=views(host.window.decorView)
                assertNotNull(all.singleOrNull{it.tag=="main-browse-courses"})
                assertTrue("Home should not repeat Explore controls",all.none{it.tag in setOf("main-headset","main-handheld","main-hands-ar")})
                assertTrue("Home cannot repeat the course catalogue",all.none{(it.tag as? String)?.startsWith("course-")==true})
            }
            capture("home")
            tagged("main-nav-explore").perform(click())
            activity.onActivity{host->assertTrue(views(host.window.decorView).map{it.tag}.containsAll(listOf("studio-hands-ar","studio-headset","studio-anatomy")))}
            val monitor=ins.addMonitor(HandsArActivity::class.java.name,null,false)
            try{
                tagged("studio-headset").perform(revealOnPage(),click())
                val opened=ins.waitForMonitorWithTimeout(monitor,5000)
                assertTrue("Explore must open the camera-AR preflight directly",opened is HandsArActivity)
                ins.runOnMainSync{
                    val scene=opened.javaClass.getDeclaredField("scene").apply{isAccessible=true}.get(opened)
                    assertNull("Route entry cannot open the camera",scene)
                    assertTrue(opened.intent.getBooleanExtra("headset",false))
                    opened.finish()
                }
            }finally{ins.removeMonitor(monitor)}
            ins.waitForIdleSync()
            tagged("main-nav-courses").perform(click())
            activity.onActivity{host->
                val tags=views(host.window.decorView).map{it.tag}
                assertTrue(tags.containsAll(listOf("course-fire","course-first-aid","main-learning-headset","main-learning-library")))
            }
            capture("courses")
        }
    }
}
