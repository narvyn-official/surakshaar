package com.narvyn.suraksha

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic records on emulator only. Captures are rendered 3D; they prove neither AR nor treatment skill. */
@RunWith(AndroidJUnit4::class)
class FirstAidTeachingSceneFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private lateinit var original:String
    private lateinit var worker:String
    private lateinit var before:String
    private var oldHindi=false
    private fun field(value:Any,name:String)=value.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(value)
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun waitFor(message:String,test:()->Boolean){repeat(300){if(test())return;Thread.sleep(100)};error(message)}
    @Before fun isolate(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        Store(context).use{
            original=it.workerId;oldHindi=it.hi
            before=JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()
            worker=it.createProfile("Synthetic first aid visual checks","Unspecified");it.switchProfile(worker);it.hi=false
        }
    }
    @After fun restore(){
        if(!::worker.isInitialized)return
        Store(context).use{it.switchProfile(original);it.hi=oldHindi}
        var preserved=false
        try {
            // Store deliberately pins its owner at construction. Reopen after switching profiles.
            Store(context).use{preserved=before==JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()}
        }finally{
            Store(context).use{it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))}
            SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.writableDatabase.delete("practice","worker=?",arrayOf(worker))}
            context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()
        }
        assertTrue("Synthetic scene checks changed the original profile's assessments or credentials",preserved)
    }
    private fun launch(id:String,stepId:String):ActivityScenario<FirstAidBayActivity>{
        val bay=FirstAidBay.createScenario(worker,LearningCatalog(context).version,id)
        while(bay.current.id!=stepId)assertTrue(bay.act(bay.current.actions.single{it.correct}.id,"3d",false).advanced)
        SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.save(bay.data)}
        val activity=ActivityScenario.launch<FirstAidBayActivity>(Intent(context,FirstAidBayActivity::class.java).putExtra("attemptId",bay.data.getString("id")))
        if(InstrumentationRegistry.getArguments().getString("orientation")=="landscape"){
            activity.onActivity{it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            waitFor("Landscape did not settle"){var ready=false;activity.onActivity{ready=it.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE};ready}
        }
        onView(withText("Continue")).perform(click())
        waitFor("Scene did not load"){var ready=false;activity.onActivity{ready=(field(it,"scene") as? AnatomyScene)?.interactionReady()==true};ready}
        return activity
    }
    private fun scene(activity:ActivityScenario<FirstAidBayActivity>):AnatomyScene{lateinit var s:AnatomyScene;activity.onActivity{s=field(it,"scene") as AnatomyScene};return s}
    private fun bay(activity:ActivityScenario<FirstAidBayActivity>):FirstAidBay{lateinit var b:FirstAidBay;activity.onActivity{b=field(it,"attempt") as FirstAidBay};return b}
    private fun capture(activity:ActivityScenario<FirstAidBayActivity>,id:String,step:String,label:String=step):Set<String>{
        val s=scene(activity);val expected=FirstAidSceneGuide.forStep(id,step)!!
        waitFor("Teaching props did not draw: ${expected.key}"){s.renderedTeachingKey==expected.key&&s.renderedTeachingParts.isNotEmpty()}
        assertEquals(FirstAidTeachingProps.visible(expected).map{it.id}.toSet(),s.renderedTeachingParts.toSet())
        var box=Rect();activity.onActivity{
            assertTrue(s.width>=it.dp(120)&&s.height>=it.dp(96));s.getGlobalVisibleRect(box)
            val root=Rect();it.window.decorView.getGlobalVisibleRect(root)
            for(button in views(it.window.decorView).filterIsInstance<Button>().filter{v->v.isShown&&v.tag in setOf("bay-demo-toggle","bay-demo-replay","bay-demo-detail")}){
                val r=Rect();assertTrue(button.getGlobalVisibleRect(r));assertTrue(root.contains(r));assertTrue(button.width>=it.dp(48));assertTrue(button.height>=it.dp(48));assertTrue("Demo labels must not wrap",button.text.isNullOrEmpty())
            }
        }
        ins.waitForIdleSync();Thread.sleep(180)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("No screenshot")
        val colours=HashSet<Int>()
        for(y in maxOf(0,box.top) until minOf(bitmap.height,box.bottom) step 6)for(x in maxOf(0,box.left) until minOf(bitmap.width,box.right) step 6)colours.add(bitmap.getPixel(x,y))
        assertTrue("GL viewport is blank",colours.size>20)
        File(context.getExternalFilesDir("validation"),"first-aid-$id-$label.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        assertEquals(step,bay(activity).current.id)
        return s.renderedTeachingParts.toSet()
    }
    private fun tapChoice(text:String){
        if(ins.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Choose the next action")?.any{it.isVisibleToUser}==true)
            onView(withText("Choose the next action")).perform(click())
        onView(withText(text)).perform(revealOnPage(),click())
    }
    private fun next(activity:ActivityScenario<FirstAidBayActivity>){
        val b=bay(activity);val answer=b.current.actions.single{it.correct};val index=b.index
        tapChoice(answer.label)
        assertEquals(index+1,bay(activity).index)
        onView(withText("Next step")).perform(click())
    }
    private fun assertRenderedCprContact(s:AnatomyScene){
        val placement=FirstAidCprPlacement.forParts(s.data.parts)!!
        val latch=CountDownLatch(1);var error:Throwable?=null
        (field(s,"surface") as GLSurfaceView).queueEvent{
            try{
                val body=field(s,"model") as FloatArray;val apparatus=field(s,"teachingModel") as FloatArray
                assertArrayEquals("Supine body and apparatus must use the same floor frame",body,apparatus,.00001f)
                @Suppress("UNCHECKED_CAST") val local=(field(s,"teachingTransforms") as Map<String,FloatArray>).getValue("teaching:cpr")
                val hand=FloatArray(16);Matrix.multiplyMM(hand,0,apparatus,0,local,0)
                val contact=FloatArray(4);val heel=FloatArray(4)
                Matrix.multiplyMV(contact,0,body,0,floatArrayOf(placement.surface[0],placement.surface[1],placement.surface[2],1f),0)
                Matrix.multiplyMV(heel,0,hand,0,floatArrayOf(FirstAidCprPlacement.AUTHOR_X,FirstAidCprPlacement.AUTHOR_Y,FirstAidCprPlacement.AUTHOR_Z+FirstAidCprPlacement.HEEL_CLEARANCE,1f),0)
                for(i in 0..2)assertEquals("Rendered heel drift at world axis $i",contact[i],heel[i],.0031f)
            }catch(t:Throwable){error=t}finally{latch.countDown()}
        }
        assertTrue("Renderer did not return the contact frame",latch.await(5,TimeUnit.SECONDS));error?.let{throw it}
    }
    private fun assertCprDetailFraming(s:AnatomyScene){
        waitFor("CPR detail camera did not render"){field(s,"lastControls")==s.controls}
        val latch=CountDownLatch(1);var error:Throwable?=null
        (field(s,"surface") as GLSurfaceView).queueEvent{
            try{
                val anatomy=field(s,"mvp") as FloatArray
                val apparatus=field(s,"teachingMvp") as FloatArray
                @Suppress("UNCHECKED_CAST") val local=(field(s,"teachingTransforms") as Map<String,FloatArray>).getValue("teaching:cpr")
                val handMatrix=FloatArray(16);Matrix.multiplyMM(handMatrix,0,apparatus,0,local,0)
                fun inside(matrix:FloatArray,point:FloatArray,margin:Float,label:String){
                    val clip=FloatArray(4);Matrix.multiplyMV(clip,0,matrix,0,floatArrayOf(point[0],point[1],point[2],1f),0)
                    assertTrue("$label is behind the camera",clip[3]>.02f)
                    val x=clip[0]/clip[3];val y=clip[1]/clip[3];val z=clip[2]/clip[3]
                    assertTrue("$label clipped horizontally: $x",x in -margin..margin)
                    assertTrue("$label clipped vertically: $y",y in -margin..margin)
                    assertTrue("$label outside camera depth",z in -1f..1f)
                }
                val hand=FirstAidTeachingProps.parts.first{it.id=="teaching:cpr"}
                for(i in hand.vertices.indices step 6)inside(handMatrix,hand.vertices.copyOfRange(i,i+3),.80f,"CPR hand")
                val contact=FirstAidCprPlacement.forParts(s.data.parts)!!.surface
                val sternum=s.data.parts.first{it.id==FirstAidCprPlacement.STERNUM}.bounds
                for(x in listOf(contact[0]-.18f,contact[0]+.18f))for(y in listOf(sternum[1]-.08f,sternum[4]+.06f))
                    for(z in listOf(contact[2]-.04f,contact[2]+.05f))inside(anatomy,floatArrayOf(x,y,z),.90f,"Chest context")
            }catch(t:Throwable){error=t}finally{latch.countDown()}
        }
        assertTrue("Renderer did not return the framing projection",latch.await(5,TimeUnit.SECONDS));error?.let{throw it}
    }
    @Test fun actualScenesChangeAcrossAllEightSituationsWithoutChangingTheAnswers(){
        launch("thermal-burn-response","cool").use{a->
            val initial=scene(a).controls.distance
            onView(withContentDescription("Inspect this step in detail")).perform(click())
            waitFor("Detail framing was not applied after the viewport became ready"){scene(a).controls.distance<initial&&scene(a).renderedTeachingKey=="thermal-burn-response/cool"}
            val cool=capture(a,"thermal-burn-response","cool")
            val s=scene(a);val before=bay(a).data.toString();val yaw=s.controls.yaw;val scale=s.controls.scale
            onView(withContentDescription("Pause visual demonstration")).perform(click());assertTrue(s.controls.teachingPaused)
            val at=field(s,"teachingElapsed") as Float;Thread.sleep(200);assertEquals(at,field(s,"teachingElapsed") as Float,.001f)
            onView(withContentDescription("Replay visual demonstration")).perform(click());assertFalse(s.controls.teachingPaused)
            assertEquals(yaw,s.controls.yaw,0f);assertEquals(scale,s.controls.scale,0f);assertEquals(before,bay(a).data.toString())
            next(a);onView(withContentDescription("Inspect this step in detail")).perform(click());val attached=capture(a,"thermal-burn-response","clothing");assertNotEquals(cool,attached)
            next(a);onView(withContentDescription("Inspect this step in detail")).perform(click());val covered=capture(a,"thermal-burn-response","cover-help");assertNotEquals(attached,covered)
        }
        launch("bleeding-injury","pressure").use{a->
            val pressure=capture(a,"bleeding-injury","pressure");next(a)
            val embedded=capture(a,"bleeding-injury","embedded");assertNotEquals(pressure,embedded)
            val old=bay(a).index
            tapChoice("Pull the glass out");assertEquals(old,bay(a).index)
        }
        launch("unresponsive-normal-breathing","response").use{a->
            capture(a,"unresponsive-normal-breathing","response");next(a);capture(a,"unresponsive-normal-breathing","breathing")
            next(a);capture(a,"unresponsive-normal-breathing","position");next(a);capture(a,"unresponsive-normal-breathing","monitor")
        }
        for((id,step)in listOf("adult-collapse-response" to "begin_cpr","suspected-limb-injury" to "support","seizure-response" to "protect","fainting-response-bay" to "support","heat-illness-response" to "cool")){
            launch(id,step).use{a->
                capture(a,id,step)
                if(id=="adult-collapse-response"){
                    assertRenderedCprContact(scene(a))
                    onView(withContentDescription("Inspect this step in detail")).perform(click())
                    capture(a,id,step,"$step-detail")
                    assertCprDetailFraming(scene(a))
                }
            }
        }
    }
    @Test fun detailTappedBeforeTheFirstViewportIsAppliedAndDoesNotLeakToAnotherStep(){
        launch("thermal-burn-response","cool").use{a->
            lateinit var early:AnatomyScene
            var created=false
            try{
                a.onActivity{host->
                    val data=(field(host,"scene") as AnatomyScene).data
                    early=AnatomyScene(host,data,false,bay=true)
                    created=true
                    early.change{it.copy(supine=true,layer="skin",teachingKey="thermal-burn-response/cool")}
                    early.focusTeachingStep()
                    assertEquals("thermal-burn-response/cool",field(early,"pendingTeachingFocus"))
                    early.change{it.copy(teachingKey="thermal-burn-response/clothing")}
                    assertNull("A queued focus must not cross a lesson step",field(early,"pendingTeachingFocus"))
                    early.focusTeachingStep()
                    assertEquals("thermal-burn-response/clothing",field(early,"pendingTeachingFocus"))
                    (host.window.decorView as ViewGroup).addView(early,FrameLayout.LayoutParams(-1,-1))
                    early.resume()
                }
                waitFor("Pre-viewport Detail intent was lost"){
                    var applied=false
                    a.onActivity{applied=field(early,"pendingTeachingFocus")==null&&early.controls.distance<2.6f&&early.renderedTeachingKey=="thermal-burn-response/clothing"}
                    applied
                }
            }finally{
                if(created)a.onActivity{early.close();(early.parent as? ViewGroup)?.removeView(early)}
            }
        }
    }
}
