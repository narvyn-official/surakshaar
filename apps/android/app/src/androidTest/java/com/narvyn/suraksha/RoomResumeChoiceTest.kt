package com.narvyn.suraksha

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.util.UUID

/** Synthetic saved journals only. No camera evidence or production learner records are changed. */
@RunWith(AndroidJUnit4::class)
class RoomResumeChoiceTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private fun field(a:RoomMissionActivity,key:String)=RoomMissionActivity::class.java.getDeclaredField(key).apply{isAccessible=true}.get(a)
    private fun shell(command:String)=ins.uiAutomation.executeShellCommand(command).use{FileInputStream(it.fileDescriptor).bufferedReader().readText().trim()}
    private fun tap(text:String){
        repeat(60){
            val root=ins.uiAutomation.rootInActiveWindow
            val window=android.graphics.Rect();root?.getBoundsInScreen(window)
            val node=root?.findAccessibilityNodeInfosByText(text)?.firstOrNull{
                val bounds=android.graphics.Rect();it.getBoundsInScreen(bounds)
                it.text?.toString().equals(text,true)&&it.isClickable&&it.isEnabled&&it.isVisibleToUser&&window.contains(bounds)&&bounds.height()>=context.dp(48)
            }
            if(node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){ins.waitForIdleSync();return}
            Thread.sleep(100)
        }
        error("Missing action: $text")
    }
    private fun fixture(run:(RoomMissionStore,JSONObject,MutableSet<String>)->Unit){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val scale=InstrumentationRegistry.getArguments().getString("fontScale")
        val oldScale=scale?.let{require(it=="2.0");shell("settings get system font_scale")}
        Store(context).use{owner->
            val oldHi=owner.hi;owner.hi=false
            val ids=mutableSetOf<String>();val id=UUID.randomUUID().toString();ids.add(id)
            val mission=RoomMission("fire",false,FireRoomScenarioCatalog.DAMAGED_ID)
            assertTrue(mission.act("alarm",1));assertTrue(mission.act("select-clear-exit",2))
            val help=RoomCoaching("fire",true);assertTrue(help.requestCue(mission.phase,3))
            val record=JSONObject().put("id",id).put("workerId",owner.workerId).put("module","fire").put("mode","screen")
                .put("updatedAt",100L).put("mission",mission.toJson()).put("coaching",help.toJson())
            RoomMissionStore(context,owner.workerId).use{db->
                db.save(record)
                try {scale?.let{shell("settings put system font_scale $it")};run(db,record,ids)} finally {
                    ids.forEach{db.writableDatabase.delete("room_missions","id=? AND worker=?",arrayOf(it,owner.workerId))}
                    owner.hi=oldHi
                    oldScale?.let{shell(if(it=="null")"settings delete system font_scale"else"settings put system font_scale $it")}
                }
            }
        }
    }
    private fun launch(id:String):ActivityScenario<RoomMissionActivity>{
        val activity=ActivityScenario.launch<RoomMissionActivity>(Intent(context,RoomMissionActivity::class.java)
            .putExtra("moduleId","fire").putExtra("camera",false).putExtra("resumeId",id).putExtra("recall",true)
            .putExtra("fireScenarioId",FireRoomScenarioCatalog.DAMAGED_ID))
        if(InstrumentationRegistry.getArguments().getString("orientation")=="landscape"){
            activity.onActivity{it.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            var ready=false
            repeat(50){activity.onActivity{ready=it.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE};if(!ready)Thread.sleep(100)}
            assertTrue("Landscape must be active",ready)
        }
        return activity
    }
    private fun capture(name:String){
        ins.waitForIdleSync();Thread.sleep(200)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("No screenshot")
        File(context.getExternalFilesDir("validation"),"resume-$name-${context.resources.configuration.orientation}-${context.resources.configuration.fontScale}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Test fun continueRetainsIdentityDecisionsAndHintsAcrossRecreation()=fixture{db,before,_->
        launch(before.getString("id")).use{a->
            a.recreate();capture("fire-choice")
            assertEquals(before.toString(),db.record(before.getString("id")).toString())
            tap("Continue")
            a.onActivity{host->
                assertEquals(before.getString("id"),field(host,"id"))
                val mission=field(host,"mission")as RoomMission
                assertEquals("EQUIPMENT",mission.phase)
                assertEquals(before.getJSONObject("mission").getJSONArray("events").toString(),mission.toJson().getJSONArray("events").toString())
                assertEquals(1,(field(host,"coaching")as RoomCoaching).cueCount)
                assertEquals(false,field(host,"resumeChoice"))
            }
        }
    }
    @Test fun restartCreatesStepOneWithoutChangingEarlierRecord()=fixture{db,before,ids->
        launch(before.getString("id")).use{a->
            tap("Start from step 1")
            var fresh=""
            a.onActivity{host->
                fresh=field(host,"id")as String;ids.add(fresh)
                assertNotEquals(before.getString("id"),fresh)
                val mission=field(host,"mission")as RoomMission
                assertEquals("ALARM",mission.phase);assertTrue(mission.events.isEmpty())
                assertEquals(FireRoomScenarioCatalog.DAMAGED_ID,mission.fireScenarioId)
                assertTrue((field(host,"coaching")as RoomCoaching).recall)
                assertEquals(0,(field(host,"coaching")as RoomCoaching).cueCount)
                assertEquals(false,field(host,"camera"))
            }
            assertEquals(before.toString(),db.record(before.getString("id")).toString())
            assertNotNull(db.record(fresh))
            a.recreate()
            a.onActivity{assertEquals(fresh,field(it,"id"));assertEquals("ALARM",(field(it,"mission")as RoomMission).phase);assertEquals(false,field(it,"resumeChoice"))}
        }
    }
    @Test fun leavingChoiceDoesNotWriteToSavedJournal()=fixture{db,before,_->
        launch(before.getString("id")).use{tap("Back")}
        assertEquals(before.toString(),db.record(before.getString("id")).toString())
        // This isolated launch has no parent screen; exiting it is the expected Back result.
        launch(before.getString("id")).use{activity->
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Continue"))
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            androidx.test.espresso.Espresso.pressBackUnconditionally()
            repeat(50){if(activity.state!=androidx.lifecycle.Lifecycle.State.DESTROYED)Thread.sleep(100)}
            assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED,activity.state)
        }
        assertEquals(before.toString(),db.record(before.getString("id")).toString())
    }
}
