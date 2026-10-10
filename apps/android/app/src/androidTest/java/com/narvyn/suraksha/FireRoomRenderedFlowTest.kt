package com.narvyn.suraksha

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import org.hamcrest.Matchers.equalTo
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Screen-mode rendering/replay evidence only. Never a real-camera or physical competence test. */
@RunWith(AndroidJUnit4::class)
class FireRoomRenderedFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private val requestedFont get()=InstrumentationRegistry.getArguments().getString("fontScale")
    private val landscape get()=InstrumentationRegistry.getArguments().getString("orientation")=="landscape"
    private val suffix get()=if(requestedFont!=null||landscape)"-${if(landscape)"landscape" else "portrait"}-${requestedFont?:"1.0"}" else ""
    private fun shell(command:String)=ins.uiAutomation.executeShellCommand(command).use{FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim()}
    private fun views(v:View):List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList())
    private fun mission(a:RoomMissionActivity)=RoomMissionActivity::class.java.getDeclaredField("mission").apply{isAccessible=true}.get(a) as RoomMission
    private fun field(owner:Any,name:String):Any?=owner.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(owner)
    private fun waitUntil(message:String,check:()->Boolean){repeat(100){if(check())return;Thread.sleep(80)};error(message)}
    private fun rendered(scene:RoomMissionView) {
        var lastCode=-1;var lastColours=0
        repeat(30){
            val done=CountDownLatch(1);val code=AtomicInteger(-1);val colours=AtomicInteger()
            ins.runOnMainSync{
                val surface=views(scene).filterIsInstance<GLSurfaceView>().single()
                if(surface.width<=0||surface.height<=0){done.countDown();return@runOnMainSync}
                val bitmap=Bitmap.createBitmap(surface.width,surface.height,Bitmap.Config.ARGB_8888)
                PixelCopy.request(surface,bitmap,{result->
                    code.set(result)
                    if(result==PixelCopy.SUCCESS){val sample=HashSet<Int>();for(x in 0 until bitmap.width step 8)for(y in 0 until bitmap.height step 8)sample.add(bitmap.getPixel(x,y));colours.set(sample.size)}
                    bitmap.recycle();done.countDown()
                },Handler(Looper.getMainLooper()))
            }
            assertTrue("GL copy must finish",done.await(2,TimeUnit.SECONDS));lastCode=code.get();lastColours=colours.get()
            if(lastCode==PixelCopy.SUCCESS&&lastColours>=8)return
            Thread.sleep(100)
        }
        fail("Rendered Fire equipment/room required: PixelCopy=$lastCode, colours=$lastColours")
    }
    private fun capture(name:String){
        ins.waitForIdleSync();Thread.sleep(250)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        File(context.getExternalFilesDir("validation"),"$name.png").outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle()
    }

    /** Test native focus/labels and real hit mapping without applying test answers to the fixture. */
    private fun checkInspectionDock(s:ActivityScenario<RoomMissionActivity>,scene:RoomMissionView,phase:String){
        val choices=RoomMissionChoices.forPhase(phase)
        lateinit var original:(String)->Unit
        val received=AtomicReference<String?>()
        s.onActivity{original=scene.onAction;scene.onAction={received.set(it)}}
        try {
            for(choice in choices){
                val action=choice.action
                onView(withTagValue(equalTo<Any>("inspection:$action"))).perform(revealOnPage())
                waitUntil("Inspection frame must be ready before the next real tap"){var ready=false;s.onActivity{ready=scene.ready};ready}
                capture("fire-$phase-$action$suffix")
                val bounds=Rect()
                s.onActivity{host->
                    val button=views(scene).filterIsInstance<Button>().single{it.tag=="inspection:$action"}
                    val panel=views(scene).filterIsInstance<PagedPanel>().single{it.tag=="inspection-panel"}
                    val visible=button.getGlobalVisibleRect(bounds)
                    @Suppress("UNCHECKED_CAST")
                    val area=field(scene,"inspectionModelArea") as Pair<Int,Int>
                    val geometry=JSONObject().put("phase",phase).put("action",action).put("fontScale",host.resources.configuration.fontScale)
                        .put("density",host.resources.displayMetrics.density).put("sceneWidth",scene.width).put("sceneHeight",scene.height)
                        .put("modelWidth",area.first).put("modelHeight",area.second).put("page",panel.pageIndex).put("pageCount",panel.pageCount)
                        .put("panelWidth",panel.width).put("panelHeight",panel.height).put("panelLeft",panel.left).put("panelTop",panel.top)
                        .put("sceneLayoutRequested",scene.isLayoutRequested).put("panelLayoutRequested",panel.isLayoutRequested)
                        .put("contentLayoutRequested",panel.content?.isLayoutRequested).put("buttonLayoutRequested",button.isLayoutRequested)
                        .put("visible",bounds.toShortString()).put("buttonWidth",button.width).put("buttonHeight",button.height)
                    File(context.getExternalFilesDir("validation"),"fire-$phase-$action-layout$suffix.json").writeText(geometry.toString(2))
                    assertTrue("A useful Fire inspection model area is required: $geometry",area.first>=host.dp(120)&&area.second>=host.dp(96))
                    val beside=area.first<scene.width
                    assertTrue("The panel must match the actual remaining model rectangle: $geometry",if(beside)
                        panel.left==area.first&&panel.top==0&&panel.width+area.first==scene.width&&panel.height==scene.height
                        else panel.top==area.second&&panel.left==0&&panel.width==scene.width&&panel.height+area.second==scene.height)
                    assertTrue("$action must be at least 48dp: $geometry",button.width>=host.dp(48)&&button.height>=host.dp(48))
                    assertTrue("$action must be fully visible through explicit pages: $geometry",visible&&bounds.width()>=button.width*.95f&&bounds.height()>=button.height*.95f)
                    assertEquals(choice.en,button.text.toString());assertEquals(choice.en,button.contentDescription?.toString())
                    assertTrue("Choice must be focusable and enabled",button.isFocusable&&button.isEnabled)
                    val layout=button.layout
                    assertTrue("Choice caption must not be clipped: $geometry",layout!=null&&layout.height<=button.height-button.compoundPaddingTop-button.compoundPaddingBottom)
                    assertTrue("Choice caption must not be ellipsised",(0 until layout.lineCount).all{layout.getEllipsisCount(it)==0})
                    val node=button.createAccessibilityNodeInfo()
                    assertEquals("android.widget.Button",node.className.toString())
                    assertTrue("Choice must expose a native click action",node.isClickable&&node.isEnabled&&node.isVisibleToUser)
                    for(other in views(panel).filterIsInstance<Button>().filter{it!==button&&it.isShown}){
                        val otherBounds=Rect()
                        if(other.getGlobalVisibleRect(otherBounds))assertFalse("Inspection controls must not overlap",Rect.intersects(bounds,otherBounds))
                    }
                }
                received.set(null)
                val down=SystemClock.uptimeMillis()
                fun send(kind:Int){
                    val event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),kind,bounds.exactCenterX(),bounds.exactCenterY(),0).apply{source=InputDevice.SOURCE_TOUCHSCREEN}
                    try{ins.sendPointerSync(event)}finally{event.recycle()}
                }
                send(MotionEvent.ACTION_DOWN);Thread.sleep(120);send(MotionEvent.ACTION_UP)
                waitUntil("Tap did not emit inspection choice $action"){received.get()!=null}
                assertEquals("Dock caption and touch action must agree",action,received.get())
            }
        }finally{s.onActivity{scene.onAction=original}}
    }

    @Test fun savedInspectionDefectAndChangedRouteRenderWithoutFabricatingActions(){
        assumeTrue("Synthetic rendering fixtures run only on the emulator",Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val created=mutableSetOf<String>();var originalHindi=false;var worker=""
        val oldFont=requestedFont?.let{require(it in setOf("1.3","2.0"));shell("settings get system font_scale")}
        Store(context).use{originalHindi=it.hi;worker=it.workerId;it.hi=false}
        try {
            requestedFont?.let{shell("settings put system font_scale $it")}
            for(target in listOf("CONDITION_CHECK","DAMAGED_UNIT","ROUTE_RECHECK")) {
                val scenarioId=if(target=="CONDITION_CHECK")FireRoomScenarioCatalog.DEFAULT_ID else FireRoomScenarioCatalog.DAMAGED_ID
                val engine=RoomMission("fire",false,scenarioId)
                engine.act("alarm",1);engine.act("select-clear-exit",2);engine.act("select-suitable-extinguisher",3);engine.act("confirm-label-match",4)
                if(target=="ROUTE_RECHECK")assertTrue(engine.act("reject-damaged-unit",5))
                assertEquals(target,engine.phase)
                val id=UUID.randomUUID().toString();created.add(id)
                val coaching=RoomCoaching("fire").toJson()
                val before=JSONObject().put("id",id).put("workerId",worker).put("module","fire").put("mode","screen")
                    .put("updatedAt",100L).put("mission",engine.toJson()).put("coaching",coaching)
                RoomMissionStore(context,worker).use{it.save(before)}
                val intent=Intent(context,RoomMissionActivity::class.java).putExtra("moduleId","fire").putExtra("camera",false)
                    .putExtra("resumeId",id).putExtra("fireScenarioId",scenarioId)
                ActivityScenario.launch<RoomMissionActivity>(intent).use{s->
                    if(landscape){
                        s.onActivity{it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
                        waitUntil("Landscape configuration did not settle"){var ready=false;s.onActivity{ready=it.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE};ready}
                    }
                    waitUntil("Saved practice briefing must be acknowledged"){
                        val node=ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Start in a clear area")?.firstOrNull{it.isVisibleToUser}
                        node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true
                    }
                    lateinit var scene:RoomMissionView
                    try {
                        waitUntil("Saved Fire phase must be ready"){
                            var ready=false
                            s.onActivity{a->scene=views(a.window.decorView).filterIsInstance<RoomMissionView>().single();ready=scene.ready&&mission(a).phase==target}
                            ready
                        }
                    }catch(error:IllegalStateException){
                        capture("fire-$target-not-ready$suffix")
                        s.onActivity{
                            val panel=views(scene).filterIsInstance<PagedPanel>().single{it.tag=="inspection-panel"}
                            val geometry=JSONObject().put("ready",scene.ready).put("sceneWidth",scene.width).put("sceneHeight",scene.height)
                                .put("panelWidth",panel.width).put("panelHeight",panel.height).put("panelDesiredWidth",panel.layoutParams.width).put("panelDesiredHeight",panel.layoutParams.height)
                                .put("pageCount",panel.pageCount).put("sceneLayoutRequested",scene.isLayoutRequested).put("panelLayoutRequested",panel.isLayoutRequested).put("contentLayoutRequested",panel.content?.isLayoutRequested)
                                .put("image",field(scene,"image").toString()).put("modelArea",field(scene,"inspectionModelArea").toString())
                            File(context.getExternalFilesDir("validation"),"fire-$target-not-ready$suffix.json").writeText(geometry.toString(2))
                        }
                        throw error
                    }
                    rendered(scene)
                    s.onActivity{a->
                        val restored=mission(a)
                        val actualId=RoomMissionActivity::class.java.getDeclaredField("id").apply{isAccessible=true}.get(a) as String
                        created.add(actualId);assertEquals(id,actualId)
                        assertEquals(scenarioId,restored.fireScenarioId)
                        assertEquals(engine.fireEquipmentCondition,restored.fireEquipmentCondition)
                        assertEquals(engine.routeChanged,restored.routeChanged);assertEquals(engine.effectiveExitStation,restored.effectiveExitStation)
                        assertEquals(engine.toJson().toString(),restored.toJson().toString())
                        val prompt=RoomMissionActivity::class.java.getDeclaredField("prompt").apply{isAccessible=true}.get(a) as TextView
                        assertEquals(RoomMissionChoices.instruction(target,false),prompt.text.toString())
                        val image=RoomMissionView::class.java.getDeclaredField("image").apply{isAccessible=true}.get(scene) as RoomMissionView.Image
                        assertEquals(target,image.phase)
                        // Magnified screen inspections intentionally frame the equipment, not the remote exit.
                        if(!RoomMissionChoices.isEquipmentInspection(target))
                            for(choice in RoomMissionChoices.forPhase(target))assertNotNull("Projected target ${choice.action}",image.targets[choice.action])
                    }
                    capture("fire-$target-screen$suffix")
                    if(RoomMissionChoices.isEquipmentInspection(target))checkInspectionDock(s,scene,target)
                    s.onActivity{assertEquals("Touch callback checks must not mutate the saved engine",engine.toJson().toString(),mission(it).toJson().toString())}
                }
                RoomMissionStore(context,worker).use{store->
                    val after=store.record(id)!!
                    assertEquals(worker,after.getString("workerId"));assertEquals("screen",after.getString("mode"))
                    // Acknowledging a fresh safe setup refreshes updatedAt; viewing does not add learning evidence.
                    assertEquals(before.getJSONObject("mission").toString(),after.getJSONObject("mission").toString())
                    assertEquals(coaching.toString(),after.getJSONObject("coaching").toString())
                }
            }
        } finally {
            oldFont?.let{if(it=="null")shell("settings delete system font_scale")else shell("settings put system font_scale $it")}
            RoomMissionStore(context,worker).use{store->for(id in created)store.writableDatabase.delete("room_missions","id=? AND worker=?",arrayOf(id,worker))}
            Store(context).use{if(it.workerId==worker)it.hi=originalHindi}
        }
    }
}
