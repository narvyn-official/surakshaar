package com.narvyn.suraksha

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real touch events through the completed GL projection, never fabricated AR evidence. */
@RunWith(AndroidJUnit4::class)
class FirstAidDirectPropFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private fun field(value:Any,name:String)=value.javaClass.getDeclaredField(name).apply{isAccessible=true}.get(value)
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun waitFor(message:String,test:()->Boolean){repeat(200){if(test())return;Thread.sleep(100)};error(message)}
    private fun bay(a:ActivityScenario<FirstAidBayActivity>):FirstAidBay{lateinit var b:FirstAidBay;a.onActivity{b=field(it,"attempt") as FirstAidBay};return b}
    private fun scene(a:ActivityScenario<FirstAidBayActivity>):AnatomyScene{lateinit var s:AnatomyScene;a.onActivity{s=field(it,"scene") as AnatomyScene};return s}
    private fun snapshot(name:String){
        val b=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        File(context.getExternalFilesDir("validation"),"first-aid-direct-$name.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
    }
    private fun tapProp(a:ActivityScenario<FirstAidBayActivity>,id:String){
        val s=scene(a);val part=FirstAidTeachingProps.parts.single{it.id==id}
        waitFor("View change did not draw"){field(s,"lastControls")==s.controls}
        lateinit var surface:GLSurfaceView;val at=IntArray(2);val projection=FloatArray(16);val done=CountDownLatch(1)
        a.onActivity{surface=views(s).filterIsInstance<GLSurfaceView>().single();surface.getLocationOnScreen(at)
            surface.queueEvent{(field(s,"teachingMvp") as FloatArray).copyInto(projection);done.countDown()}}
        assertTrue(done.await(3,TimeUnit.SECONDS))
        // The tap control is the exposed end of its blue lever; the sleeve centre is a static surface.
        val px=if(id=="teaching:tap-control")part.bounds[3]-.009f else (part.bounds[0]+part.bounds[3])/2
        val source=floatArrayOf(px,(part.bounds[1]+part.bounds[4])/2,(part.bounds[2]+part.bounds[5])/2,1f)
        val clip=FloatArray(4);Matrix.multiplyMV(clip,0,projection,0,source,0)
        assertTrue(clip[3]>0f)
        val x=at[0]+(clip[0]/clip[3]+1)*.5f*surface.width;val y=at[1]+(1-clip[1]/clip[3])*.5f*surface.height
        assertTrue(x>at[0]&&x<at[0]+surface.width);assertTrue(y>at[1]&&y<at[1]+surface.height)
        a.onActivity{host->for(button in views(host.window.decorView).filterIsInstance<Button>().filter{it.isShown}){
            val r=Rect();button.getGlobalVisibleRect(r);assertFalse("Projected prop is behind a control",r.contains(x.toInt(),y.toInt()))}}
        val now=SystemClock.uptimeMillis()
        for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
            val event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,x,y,0).apply{this.source=InputDevice.SOURCE_TOUCHSCREEN}
            try{ins.sendPointerSync(event)}finally{event.recycle()};Thread.sleep(70)
        }
        ins.waitForIdleSync();Thread.sleep(250)
    }
    @Test fun visibleTapAdvancesOnceAndSleeveCannotBeChosenThroughTheBody(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        lateinit var original:String;lateinit var worker:String;lateinit var before:String;var oldHindi=false
        Store(context).use{original=it.workerId;oldHindi=it.hi;before=JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()
            worker=it.createProfile("Synthetic direct prop checks","Unspecified");it.switchProfile(worker);it.hi=false}
        try {
            val fixture=FirstAidBay.createScenario(worker,LearningCatalog(context).version,"thermal-burn-response")
            while(fixture.current.id!="cool")fixture.act(fixture.current.actions.single{it.correct}.id,"3d",false)
            SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.save(fixture.data)}
            ActivityScenario.launch<FirstAidBayActivity>(Intent(context,FirstAidBayActivity::class.java).putExtra("attemptId",fixture.data.getString("id"))).use{a->
                onView(withText("Continue")).perform(click())
                waitFor("Body scene did not load"){var ready=false;a.onActivity{ready=(field(it,"scene") as? AnatomyScene)?.renderedTeachingKey=="thermal-burn-response/cool"};ready}
                onView(withContentDescription("Inspect this step in detail")).perform(click())
                val old=bay(a).index;tapProp(a,"teaching:tap-control")
                waitFor("Direct tap did not advance cooling"){bay(a).index==old+1}
                assertEquals("cool-running-water",bay(a).events.objects().last{it.optString("kind")=="action"}.optString("actionId"))
                assertEquals("3d",bay(a).events.objects().last{it.optString("kind")=="action"}.optString("mode"))
                tapProp(a,"teaching:tap-control");assertEquals("Feedback must reject duplicate taps",old+1,bay(a).index)
                onView(withText("Next step")).perform(click());assertEquals("clothing",bay(a).current.id)
                val s=scene(a)
                a.onActivity{s.change{it.copy(yaw=-90f,pitch=44f,panX=0f,panY=0f,distance=2.2f)}}
                tapProp(a,"teaching:sleeve");assertEquals("Occluded sleeve must not advance",old+1,bay(a).index);snapshot("sleeve-occluded")
                a.onActivity{s.focusTeachingStep()}
                tapProp(a,"teaching:sleeve")
                waitFor("Visible sleeve did not advance"){bay(a).index==old+2};snapshot("sleeve-visible")
            }
        }finally{
            Store(context).use{it.switchProfile(original);it.hi=oldHindi}
            var preserved=false
            try{Store(context).use{preserved=before==JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString()}}
            finally{Store(context).use{it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))};SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.writableDatabase.delete("practice","worker=?",arrayOf(worker))};context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()}
            assertTrue("Original assessment/certificate data changed",preserved)
        }
    }
}
