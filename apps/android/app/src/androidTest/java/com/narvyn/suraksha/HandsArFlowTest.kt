package com.narvyn.suraksha

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ScrollView
import androidx.lifecycle.Lifecycle
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

/** Preflight tests only. No synthetic gesture is represented as live camera recognition. */
@RunWith(AndroidJUnit4::class)
class HandsArFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private lateinit var original:String
    private lateinit var worker:String
    private val profiles=mutableListOf<String>()
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun tagged(name:String)=onView(withTagValue(`is`(name as Any)))
    private fun field(host:Any,name:String)=host.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(host)
    private fun click(name:String){tagged(name).perform(revealOnPage(),click())}
    private fun capture(name:String){
        ins.waitForIdleSync()
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        File(context.getExternalFilesDir("validation"),"hands-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Before fun isolate(){
        assumeTrue(Build.HARDWARE in setOf("ranchu","goldfish"))
        Store(context).use{original=it.workerId;worker=it.createProfile("Synthetic hands preflight","Unspecified");profiles+=worker;it.switchProfile(worker)}
    }
    @After fun restore(){
        if(!::worker.isInitialized)return
        for(id in profiles)context.getSharedPreferences("learning-profile",0).edit().remove("context-$id").commit()
        Store(context).use{it.switchProfile(original);for(id in profiles)it.writableDatabase.delete("worker_profiles","id=?",arrayOf(id))}
    }
    @Test fun handheldConsentBackAndRecreationDoNotStartTheCamera(){
        ActivityScenario.launch(HandsArActivity::class.java).use{a->
            a.onActivity{assertNull(field(it,"scene"));assertTrue(views(it.window.decorView).none{v->v is ScrollView})}
            capture("entry")
            a.onActivity{host->
                val all=views(host.window.decorView)
                assertFalse((all.single{it.tag=="hands-ar-start"} as Button).isEnabled)
                for(id in listOf("hands-ar-space","hands-ar-privacy"))assertFalse((all.single{it.tag==id} as CheckBox).isChecked)
            }
            click("hands-ar-privacy")
            a.onActivity{assertFalse((views(it.window.decorView).single{v->v.tag=="hands-ar-start"} as Button).isEnabled)}
            click("hands-ar-space")
            a.onActivity{assertTrue((views(it.window.decorView).single{v->v.tag=="hands-ar-start"} as Button).isEnabled);assertNull(field(it,"scene"))}
            capture("handheld-ready");a.recreate()
            a.onActivity{assertNull("Recreation must not open a camera",field(it,"scene"));assertEquals(1,field(it,"page"))}
            click("hands-ar-back")
            repeat(60){if(a.state!=Lifecycle.State.DESTROYED)Thread.sleep(50)}
            assertEquals(Lifecycle.State.DESTROYED,a.state)
        }
    }
    @Test fun headsetRequiresUncoveredCameraAndExplicitViewerReadiness(){
        val intent=Intent(context,HandsArActivity::class.java).putExtra("headset",true)
        ActivityScenario.launch<HandsArActivity>(intent).use{a->
            a.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            var landscape=false
            repeat(50){if(!landscape){a.onActivity{host->landscape=host.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE};if(!landscape)Thread.sleep(100)}}
            assertTrue("Headset preflight must be landscape",landscape)
            // Configuration can report landscape before Android finishes the rotation transition.
            // Wait for the real window to settle before sending its first pointer event.
            ins.waitForIdleSync();ins.uiAutomation.waitForIdle(500,3000)
            a.onActivity{CardboardBridge.initialise(it);CardboardBridge.useDefaultViewer()}
            click("hands-ar-space");click("hands-ar-privacy")
            a.onActivity{host->
                val all=views(host.window.decorView)
                assertFalse((all.single{it.tag=="hands-ar-camera"} as CheckBox).isChecked)
                assertFalse((all.single{it.tag=="hands-ar-start"} as Button).isEnabled)
            }
            click("hands-ar-camera")
            capture("headset-before-start-check")
            a.onActivity{host->
                val flags=listOf("page","headset","space","privacy","rearCamera","profileContext").associateWith{field(host,it)}
                assertTrue("Ready flags: $flags; orientation=${host.resources.configuration.orientation}; viewer=${CardboardBridge.hasViewerProfile()}",(views(host.window.decorView).single{v->v.tag=="hands-ar-start"} as Button).isEnabled);assertNull(field(host,"scene"))
            }
            capture("headset-ready")
        }
    }
    @Test fun headsetStartWorksWithSystemAutoRotateOff(){
        val resolver=context.contentResolver
        val auto=android.provider.Settings.System.getString(resolver,"accelerometer_rotation")
        val rotation=android.provider.Settings.System.getString(resolver,"user_rotation")
        fun setting(key:String,value:String?){
            val command=if(value==null)"settings delete system $key" else "settings put system $key $value"
            ins.uiAutomation.executeShellCommand(command).use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()}
        }
        try{
            setting("accelerometer_rotation","0");setting("user_rotation","0")
            val intent=Intent(context,HandsArActivity::class.java).putExtra("headset",true)
            ActivityScenario.launch<HandsArActivity>(intent).use{a->
                var landscape=false
                repeat(60){if(!landscape){a.onActivity{landscape=it.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE};if(!landscape)Thread.sleep(100)}}
                capture("rotation-locked-preflight")
                assertTrue("Cardboard must enter landscape even when system auto-rotate is off",landscape)
                ins.waitForIdleSync();ins.uiAutomation.waitForIdle(500,3000)
                a.onActivity{CardboardBridge.initialise(it);CardboardBridge.useDefaultViewer()}
                click("hands-ar-space");click("hands-ar-privacy");click("hands-ar-camera")
                a.onActivity{host->
                    assertTrue("All headset prerequisites are satisfied; Start must be usable",(views(host.window.decorView).single{it.tag=="hands-ar-start"} as Button).isEnabled)
                    assertNull("Orientation and consent alone must not open the camera",field(host,"scene"))
                    if(host.resources.configuration.fontScale<=1f)assertEquals("Ordinary headset setup should fit one screen",1,views(host.window.decorView).filterIsInstance<PagedPanel>().first().pageCount)
                }
                capture("rotation-locked-ready")
                click("hands-ar-start")
                var attempted=false
                repeat(100){if(!attempted){a.onActivity{attempted=field(it,"permissionStartPending")==true||field(it,"scene")!=null||field(it,"error")!=null};if(!attempted)Thread.sleep(50)}}
                assertTrue("A Start tap must request the camera or attempt AR startup",attempted)
            }
        }finally{setting("accelerometer_rotation",auto);setting("user_rotation",rotation)}
    }
    @Test fun missingSafetyCheckIsExplainedBesideTheStartButton(){
        val wasHindi=Store(context).use{val old=it.hi;it.hi=false;old}
        try{
            for(hindi in listOf(false,true)){
            Store(context).use{it.hi=hindi}
            ActivityScenario.launch(HandsArActivity::class.java).use{a->
                click("hands-ar-privacy")
                a.onActivity{host->
                    val all=views(host.window.decorView)
                    val status=all.single{it.tag=="hands-ar-viewer-status"} as android.widget.TextView
                    val button=all.single{it.tag=="hands-ar-start"} as Button
                    assertTrue("Explain the remaining clear-space check in the selected language",status.text.contains(if(hindi)"अभ्यास की जगह"else"practice space"))
                    assertSame("Readiness help must stay beside Start outside the paged content",button.parent,status.parent)
                    assertFalse(button.isEnabled)
                    assertNull(field(host,"scene"))
                }
                capture(if(hindi)"hindi-readiness"else"english-readiness")
            }
            }
        }finally{Store(context).use{it.hi=wasHindi}}
    }
    @Test fun schoolDeepEntryCannotOpenAdultTreatmentIllustrations(){
        context.getSharedPreferences("learning-profile",0).edit().putString("context-$worker","School").commit()
        val steps=SpatialCatalog(context).data.getJSONObject("steps")
        val adult=steps.keys().asSequence().map{steps.getJSONObject(it)}.first{it.optString("audience")=="adult"&&it.optString("teachingKey").isNotEmpty()}
        val intent=Intent(context,HandsArActivity::class.java).putExtra("modelId","body").putExtra("teachingKey",adult.getString("teachingKey"))
        ActivityScenario.launch<HandsArActivity>(intent).use{a->
            a.onActivity{assertNull(field(it,"teachingKey"));assertNull(field(it,"scene"));assertNotNull(field(it,"entryNote"))}
            capture("school-entry")
        }
    }
    @Test fun authoredStepEntryKeepsItsSceneAndRejectsSchoolTreatment(){
        val intent=Intent(context,HandsArActivity::class.java).putExtra("scenarioId","thermal-burn").putExtra("stepId","cool")
        ActivityScenario.launch<HandsArActivity>(intent).use{a->
            a.onActivity{host->val lesson=field(host,"lesson") as HandsLessonSession
                assertEquals("thermal-burn-response/cool",lesson.current.teachingKey);assertEquals("body",field(host,"modelId"));assertNull(field(host,"scene"))}
            a.recreate()
            a.onActivity{host->assertEquals("cool",(field(host,"lesson") as HandsLessonSession).current.stepId);assertNull(field(host,"scene"))}
        }
        context.getSharedPreferences("learning-profile",0).edit().putString("context-$worker","School").commit()
        ActivityScenario.launch<HandsArActivity>(intent).use{a->a.onActivity{host->
            assertNull(field(host,"lesson"));assertNull(field(host,"scene"));assertNotNull(views(host.window.decorView).singleOrNull{it.tag=="hands-ar-unavailable"})
        }}
    }
    @Test fun untranslatedHandsLessonDeclaresItsLanguageBeforeStarting(){
        val oldHindi=Store(context).use{val old=it.hi;it.hi=true;old}
        try{
            val intent=Intent(context,HandsArActivity::class.java).putExtra("scenarioId","thermal-burn").putExtra("stepId","cool")
            ActivityScenario.launch<HandsArActivity>(intent).use{a->
                a.onActivity{host->val all=views(host.window.decorView)
                    assertNotNull(all.singleOrNull{it.tag=="hands-ar-content-language"})
                    assertTrue((all.single{it.tag=="hands-ar-start"} as Button).text.contains("अंग्रेज़ी"))
                    assertNull(field(host,"scene"))
                }
            }
        }finally{Store(context).use{it.hi=oldHindi}}
    }
    @Test fun changingLearnerWhileInBackgroundClosesTheOldWorkspace(){
        ActivityScenario.launch(HandsArActivity::class.java).use{a->
            a.moveToState(Lifecycle.State.CREATED)
            Store(context).use{val next=it.createProfile("Synthetic replacement learner","Unspecified");profiles+=next;it.switchProfile(next)}
            // Resume through Android's real task navigation. ActivityScenario.moveToState(RESUMED)
            // expects RESUMED even when the app correctly finishes the stale learner's Activity.
            context.startActivity(Intent(context,HandsArActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            repeat(100){if(a.state!=Lifecycle.State.DESTROYED)Thread.sleep(100)}
            assertEquals(Lifecycle.State.DESTROYED,a.state)
        }
    }
    @Test fun firstCameraGrantContinuesTheRequestedSessionWithoutAnotherStartTap(){
        assertEquals("Run this check with camera permission revoked on the isolated test emulator",android.content.pm.PackageManager.PERMISSION_DENIED,context.checkSelfPermission(android.Manifest.permission.CAMERA))
        val automation=ins.uiAutomation
        val info=automation.serviceInfo
        info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo=info
        ActivityScenario.launch(HandsArActivity::class.java).use{a->
            click("hands-ar-privacy");click("hands-ar-space");click("hands-ar-start")
            var allowed=false
            repeat(100){if(!allowed){
                val root=automation.rootInActiveWindow
                val node=root?.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/permission_allow_one_time_button")?.firstOrNull()
                if(node!=null)allowed=node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                if(!allowed)Thread.sleep(50)
            }}
            assertTrue("Android camera permission dialog was not handled",allowed)
            var attempted=false
            repeat(200){if(!attempted){a.onActivity{attempted=field(it,"scene")!=null||field(it,"error")!=null};if(!attempted)Thread.sleep(50)}}
            assertTrue("Granting permission must continue the already-requested session",attempted)
            // An emulator may reject ARCore. This check accepts the explicit recovery UI, but
            // never calls that a successfully tracked camera or successful hand recognition.
            capture("permission-continuation")
        }
    }
}
