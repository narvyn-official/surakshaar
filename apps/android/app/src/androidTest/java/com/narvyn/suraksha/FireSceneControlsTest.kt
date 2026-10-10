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
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import org.hamcrest.Matchers.`is`
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Rendering and gesture regression only. Camera/anchor alignment needs a physical ARCore device. */
@RunWith(AndroidJUnit4::class)
class FireSceneControlsTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private val landscape get()=InstrumentationRegistry.getArguments().getString("orientation")=="landscape"
    private val suffix get()=if(landscape)"-landscape"else""
    private fun views(v:View):List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList())
    private fun field(owner:Any,name:String):Any?=owner.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(owner)
    private fun waitFor(message:String,check:()->Boolean){repeat(100){if(check())return;Thread.sleep(80)};error(message)}
    private fun capture(name:String){
        ins.waitForIdleSync();Thread.sleep(300)
        val image=ins.uiAutomation.takeScreenshot()?:error("Screenshot missing")
        File(context.getExternalFilesDir("validation"),"$name$suffix.png").outputStream().use{assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it))};image.recycle()
    }
    private fun checkZoomControls(scene:RoomMissionView,factor:Float){
        val controls=views(scene).filterIsInstance<Button>().filter{it.tag in setOf("room-zoom-in","room-zoom-out","room-zoom-reset")}
        assertEquals(3,controls.size)
        val occupied=mutableListOf<Rect>()
        for(button in controls){
            val bounds=Rect()
            assertTrue("${button.tag} must be visible",button.getGlobalVisibleRect(bounds))
            val minimum=(48*scene.resources.displayMetrics.density+.5f).toInt()
            assertTrue("${button.tag} keeps a 48dp touch target",button.width>=minimum&&button.height>=minimum)
            assertTrue("${button.tag} must not be clipped",bounds.width()>=button.width*.98f&&bounds.height()>=button.height*.98f)
            assertTrue("Zoom controls must not overlap",occupied.none{Rect.intersects(it,bounds)})
            occupied.add(bounds)
            assertFalse("Every control needs an accessible name",button.contentDescription.isNullOrBlank())
            if(button.text.isNotEmpty()){
                val layout=button.layout
                assertNotNull("Control text must be laid out",layout)
                assertTrue("${button.tag} text height must fit",layout.height<=button.height-button.compoundPaddingTop-button.compoundPaddingBottom)
                assertTrue("${button.tag} text must stay on one visible line",layout.lineCount==1&&layout.getEllipsisCount(0)==0)
                assertTrue("${button.tag} text width must fit",layout.getLineWidth(0)<=button.width-button.compoundPaddingLeft-button.compoundPaddingRight+1f)
            }
        }
        val reset=controls.single{it.tag=="room-zoom-reset"}
        val label=if(factor==1f)"1×"else String.format(java.util.Locale.ROOT,"%.1f×",factor)
        assertTrue("Reset accessibility must retain the factor",reset.contentDescription.contains(label))
        if(scene.resources.configuration.fontScale>=1.6f){assertTrue(reset.text.isEmpty());assertNotNull("Large-text reset uses a visible icon",reset.foreground)}
        else assertEquals(label,reset.text.toString())
    }
    private fun checkCompactWorkspace(host:RoomMissionActivity,scene:RoomMissionView){
        if(field(host,"compactLandscape")!=true)return
        val density=scene.resources.displayMetrics.density
        val sceneBounds=Rect();assertTrue(scene.getGlobalVisibleRect(sceneBounds))
        val root=scene.parent as View
        val contentWidth=root.width-root.paddingLeft-root.paddingRight
        assertTrue("Compact practice gives the scene the full available width",sceneBounds.width()>=contentWidth*.98f)
        assertTrue("Compact practice retains a useful model height",sceneBounds.height()>=144*density)
        if(RoomMissionChoices.isEquipmentInspection((field(host,"mission") as RoomMission).phase)){
            val area=field(scene,"inspectionModelArea") as Pair<*,*>
            assertTrue("Equipment has at least half the scene width",(area.first as Int)>=scene.width*.5f)
            assertTrue("The actual model area remains tall enough to inspect",(area.second as Int)>=144*density)
        }
        val prompt=field(host,"prompt") as TextView
        assertFalse("Full instructions use details instead of a competing permanent column",prompt.isAttachedToWindow)
        val toolbar=views(root).single{it.tag=="room-tools"}
        assertTrue("Compact controls occupy one bounded row",toolbar.height<=57*density)
        val occupied=mutableListOf<Rect>()
        for(tag in listOf("room-step-details","room-speak","room-help","room-mission-control")){
            val control=views(toolbar).single{it.tag==tag}
            if(control.visibility!=View.VISIBLE)continue
            val bounds=Rect();assertTrue("$tag is fully visible",control.getGlobalVisibleRect(bounds))
            assertTrue("$tag has a 48dp touch target",control.width>=48*density-1&&control.height>=48*density-1)
            assertTrue("$tag is not clipped",bounds.width()>=control.width*.98f&&bounds.height()>=control.height*.98f)
            assertTrue("Compact controls do not overlap",occupied.none{Rect.intersects(it,bounds)});occupied.add(bounds)
            assertTrue("$tag can be reached without a gesture",control.isClickable&&control.isFocusable)
            if(tag!="room-step-details")assertFalse("$tag has an accessible name",control.contentDescription.isNullOrBlank())
        }
    }
    private fun rendered(scene:View,previous:Long?=null):Long {
        var colours=0
        repeat(25){
            val done=CountDownLatch(1);val result=AtomicInteger(-1);val count=AtomicInteger();val fingerprint=AtomicLong()
            ins.runOnMainSync{
                val surface=views(scene).filterIsInstance<GLSurfaceView>().single()
                if(surface.width==0||surface.height==0){done.countDown();return@runOnMainSync}
                val image=Bitmap.createBitmap(surface.width,surface.height,Bitmap.Config.ARGB_8888)
                PixelCopy.request(surface,image,{code->
                    result.set(code)
                    if(code==PixelCopy.SUCCESS){val colours=HashSet<Int>();var hash=1125899906842597L
                        for(x in 0 until image.width step 6)for(y in 0 until image.height step 6){val colour=image.getPixel(x,y);colours.add(colour);hash=hash*31+colour}
                        count.set(colours.size);fingerprint.set(hash)}
                    image.recycle();done.countDown()
                },Handler(Looper.getMainLooper()))
            }
            assertTrue(done.await(2,TimeUnit.SECONDS));colours=count.get()
            if(result.get()==PixelCopy.SUCCESS&&colours>12&&fingerprint.get()!=previous)return fingerprint.get()
            Thread.sleep(100)
        }
        fail("A rendered model is required; colour sample count=$colours")
        error("Unreachable")
    }
    @Test fun scenePickerShowsAllAuthoredContextsAndRestoresSelectedModel(){
        val hi=Store(context).use{it.hi}
        fun t(en:String,hindi:String)=if(hi)hindi else en
        ActivityScenario.launch<EquipmentActivity>(Intent(context,EquipmentActivity::class.java).putExtra("moduleId","fire")).use{s->
            var previous:Long?=null
            for(case in FireResponseLearning.cases){
                onView(withContentDescription(t("Choose a Fire scene","आग का दृश्य चुनें"))).perform(click())
                onView(withText(case.title.local(hi))).perform(revealOnPage(),click())
                lateinit var decor:View
                s.onActivity{a->
                    decor=a.window.decorView
                    assertEquals("context:${case.id}",field(a,"fireSceneId"))
                    val status=views(decor).filterIsInstance<TextView>().single{it.tag=="equipment-view-state"}
                    assertTrue(status.text.contains(case.title.local(hi)))
                }
                previous=rendered(decor,previous);capture("fire-context-${case.id}")
            }
            onView(withContentDescription(t("Equipment view controls","उपकरण दृश्य नियंत्रण"))).perform(click())
            onView(withContentDescription(t("Zoom in","ज़ूम बढ़ाएँ"))).perform(click())
            var distance=0f
            s.onActivity{distance=field(it,"distance") as Float;assertTrue(distance<1.25f)}
            s.recreate()
            s.onActivity{assertEquals("context:sand",field(it,"fireSceneId"));assertEquals(distance,field(it,"distance") as Float,0f)}
            onView(withContentDescription(t("Reset equipment view","उपकरण दृश्य रीसेट करें"))).perform(click())
            s.onActivity{assertEquals("Reset view must preserve the selected case","context:sand",field(it,"fireSceneId"));assertEquals(1.25f,field(it,"distance") as Float,0f)}
            capture("fire-context-reset")
        }
    }
    @Test fun roomZoomKeepsPhaseAndEvidenceWhilePinchCancelsTheActionPointer(){
        assumeTrue("Isolated authored fixtures are emulator only",Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val ids=mutableSetOf<String>();var worker="";var oldHi=false
        Store(context).use{worker=it.workerId;oldHi=it.hi;it.hi=false}
        try{
            for(target in listOf("CONDITION_CHECK","DAMAGED_UNIT","ROUTE_RECHECK")){
                val scenarioId=if(target=="CONDITION_CHECK")FireRoomScenarioCatalog.DEFAULT_ID else FireRoomScenarioCatalog.DAMAGED_ID
                val mission=RoomMission("fire",false,scenarioId)
                mission.act("alarm",1);mission.act("select-clear-exit",2);mission.act("select-suitable-extinguisher",3);mission.act("confirm-label-match",4)
                if(target=="ROUTE_RECHECK")assertTrue(mission.act("reject-damaged-unit",5))
                assertEquals(target,mission.phase)
                val id=UUID.randomUUID().toString();ids.add(id)
                val saved=mission.toJson().toString()
                RoomMissionStore(context,worker).use{it.save(JSONObject().put("id",id).put("workerId",worker).put("module","fire").put("mode","screen").put("updatedAt",100L).put("mission",mission.toJson()).put("coaching",RoomCoaching("fire").toJson()))}
                ActivityScenario.launch<RoomMissionActivity>(Intent(context,RoomMissionActivity::class.java).putExtra("moduleId","fire").putExtra("camera",false).putExtra("resumeId",id).putExtra("fireScenarioId",scenarioId)).use{s->
                    if(landscape){
                        s.onActivity{it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
                        waitFor("Landscape configuration must settle"){var ready=false;s.onActivity{ready=it.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE};ready}
                    }
                    waitFor("Continue briefing"){
                        ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Continue")?.firstOrNull{it.isClickable&&it.isVisibleToUser&&it.text?.toString()=="Continue"}?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true
                    }
                    lateinit var scene:RoomMissionView
                    waitFor("Room must render before magnification"){var ready=false;s.onActivity{a->scene=views(a.window.decorView).filterIsInstance<RoomMissionView>().single();ready=scene.ready};ready}
                    s.onActivity{a->checkZoomControls(scene,1f);checkCompactWorkspace(a,scene)}
                    var compactDetails:String?=null
                    s.onActivity{a->
                        if(field(a,"compactLandscape")==true){
                            compactDetails=(field(a,"prompt") as TextView).text.toString()+"\n\n"+(field(a,"status") as TextView).text.toString()
                        }
                    }
                    compactDetails?.let{details->
                        onView(withTagValue(`is`("room-step-details" as Any))).perform(click())
                        waitFor("Current step dialog must be open"){
                            ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Current step and status")?.any{it.isVisibleToUser}==true
                        }
                        onView(withTagValue(`is`("paged-dialog-message" as Any))).inRoot(isDialog()).check(matches(withText(details)))
                        capture("fire-view-$target-details")
                        onView(withText("Close")).inRoot(isDialog()).perform(click())
                        waitFor("Closing details restores the scene"){var ready=false;s.onActivity{ready=scene.ready};ready}
                        s.onActivity{a->assertEquals("Reading instructions cannot change the lesson record",saved,(field(a,"mission") as RoomMission).toJson().toString())}
                    }
                    rendered(scene);capture("fire-view-$target-fit")
                    var oldImage:RoomMissionView.Image?=null
                    s.onActivity{oldImage=field(scene,"image") as RoomMissionView.Image;views(scene).filterIsInstance<Button>().single{it.tag=="room-zoom-in"}.performClick();assertFalse("Old picks are invalid immediately after view change",scene.ready)}
                    waitFor("Magnified frame"){var ready=false;s.onActivity{ready=scene.ready&&(field(scene,"image") as RoomMissionView.Image).magnification>1f};ready}
                    s.onActivity{a->
                        val image=field(scene,"image") as RoomMissionView.Image
                        assertEquals(target,image.phase);assertEquals(1.2f,image.magnification,.001f);assertTrue(image.viewSerial>oldImage!!.viewSerial)
                        checkZoomControls(scene,image.magnification)
                        assertEquals(saved,(field(a,"mission") as RoomMission).toJson().toString())
                        if(target=="ROUTE_RECHECK"){
                            val before=oldImage!!.targets.getValue("base");val after=image.targets.getValue("base")
                            assertEquals(scene.width/2f+1.2f*(before.x-scene.width/2f),after.x,.2f)
                            assertEquals(scene.height/2f+1.2f*(before.y-scene.height/2f),after.y,.2f)
                        }
                    }
                    capture("fire-view-$target-zoom")
                    s.onActivity{
                        views(scene).filterIsInstance<Button>().single{it.tag=="room-zoom-out"}.performClick()
                    }
                    waitFor("Zoom out restores fit"){var done=false;s.onActivity{done=scene.ready&&(field(scene,"image") as RoomMissionView.Image).magnification==1f};done}
                    s.onActivity{
                        val overlay=field(scene,"overlay") as View
                        val start=SystemClock.uptimeMillis()
                        val configuration=ViewConfiguration.get(scene.context)
                        val minSpan=configuration.scaledMinimumScalingSpan.toFloat()
                        val slop=configuration.scaledTouchSlop.toFloat()
                        // ScaleGestureDetector thresholds are physical-density dependent. The old
                        // 40→240px span never qualified as a pinch on this high-density emulator.
                        val startRadius=scene.width*.08f
                        val beginRadius=maxOf(minSpan/2+slop*2,startRadius+slop*2+1f)
                        val endRadius=scene.width*.44f
                        assertTrue("Scene must accommodate a real pinch: width=${scene.width}, minSpan=$minSpan, slop=$slop",endRadius>beginRadius+slop)
                        File(context.getExternalFilesDir("validation"),"fire-view-$target-pinch$suffix.json").writeText(JSONObject()
                            .put("width",scene.width).put("height",scene.height).put("minimumScalingSpan",minSpan)
                            .put("touchSlop",slop).put("startSpan",startRadius*2).put("beginSpan",beginRadius*2).put("endSpan",endRadius*2).toString(2))
                        fun event(action:Int,count:Int,span:Float,offset:Long){
                            val props=Array(count){i->MotionEvent.PointerProperties().apply{this.id=i;toolType=MotionEvent.TOOL_TYPE_FINGER}}
                            val coords=Array(count){i->MotionEvent.PointerCoords().apply{x=scene.width*.5f+(if(i==0)-span else span);y=scene.height*.5f;pressure=1f;size=1f}}
                            val e=MotionEvent.obtain(start,start+offset,action,count,props,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
                            try{overlay.dispatchTouchEvent(e)}finally{e.recycle()}
                        }
                        event(MotionEvent.ACTION_DOWN,1,startRadius,0)
                        event(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,startRadius,10)
                        event(MotionEvent.ACTION_MOVE,2,beginRadius,35)
                        event(MotionEvent.ACTION_MOVE,2,endRadius,70)
                        event(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,endRadius,90)
                        event(MotionEvent.ACTION_UP,1,endRadius,100)
                        assertNull("Pinch must not leave a procedure pointer",field(scene,"down"))
                        assertNull("Pinch must not retain an action target",field(scene,"gestureTarget"))
                        assertTrue("Pinch changes the view",(field(scene,"magnification") as InspectionMagnification.Snapshot).factor>1f)
                        views(scene).filterIsInstance<Button>().single{it.tag=="room-zoom-reset"}.performClick()
                    }
                    waitFor("Reset frame"){var done=false;s.onActivity{done=scene.ready&&(field(scene,"image") as RoomMissionView.Image).magnification==1f};done}
                    s.onActivity{checkZoomControls(scene,1f)}
                    s.onActivity{a->assertEquals("View controls cannot advance or grade the mission",saved,(field(a,"mission") as RoomMission).toJson().toString())}
                    capture("fire-view-$target-reset")
                }
                RoomMissionStore(context,worker).use{assertEquals("Viewing must not fabricate persisted actions",saved,it.record(id)!!.getJSONObject("mission").toString())}
            }
        }finally{
            RoomMissionStore(context,worker).use{store->ids.forEach{store.writableDatabase.delete("room_missions","id=? AND worker=?",arrayOf(it,worker))}}
            Store(context).use{if(it.workerId==worker)it.hi=oldHi}
        }
    }
}
