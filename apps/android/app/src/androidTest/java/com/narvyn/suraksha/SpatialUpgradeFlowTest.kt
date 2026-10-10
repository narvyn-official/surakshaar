package com.narvyn.suraksha
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SpatialUpgradeFlowTest {
 private val ins get()=InstrumentationRegistry.getInstrumentation()
 private val context get()=ins.targetContext
 private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{all(v.getChildAt(it))}else emptyList()
 private fun capture(name:String){File(context.getExternalFilesDir(null),"spatial-$name.png").outputStream().use{ins.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)}}
 private fun awaitEquipment(activity:ActivityScenario<EquipmentActivity>){
  var ready=false
  for(i in 0..100){activity.onActivity{a->ready=a.hasWindowFocus()&&EquipmentActivity::class.java.getDeclaredField("rendered").apply{isAccessible=true}.getBoolean(a)};if(ready)return;Thread.sleep(100)}
  assertTrue("Equipment window must be rendered and focused",ready)
 }
 private fun pixels(surface:SurfaceView){
  ins.runOnMainSync{(surface as? android.opengl.GLSurfaceView)?.requestRender()};ins.waitForIdleSync();Thread.sleep(600)
  val bounds=android.graphics.Rect();ins.runOnMainSync{assertTrue(surface.getGlobalVisibleRect(bounds))}
  val screen=ins.uiAutomation.takeScreenshot();val colors=mutableSetOf<Int>()
  for(y in bounds.top.coerceAtLeast(0) until bounds.bottom.coerceAtMost(screen.height) step 5)for(x in bounds.left.coerceAtLeast(0) until bounds.right.coerceAtMost(screen.width) step 5)colors.add(screen.getPixel(x,y))
  screen.recycle();assertTrue("Visible GPU surface must contain actual geometry: ${colors.size} colours",colors.size>20)
 }

 @Test fun liveBodyScenesShareVerifiedGeometryWithoutSharingLearnerState(){
  val first=AnatomyData.load(context);val second=AnatomyData.load(context)
  assertSame(first.parts,second.parts);assertSame(first.parts.first().vertices,second.parts.first().vertices)
  assertEquals(first.manifest.getString("loadedQuality"),second.manifest.getString("loadedQuality"))
 }
 @Test fun eachCourseOpensItsStepSceneAndReturningDoesNotCreateAnAnswer(){
  val profile=Store(context).use{it.workerId};val prefs=context.getSharedPreferences("learning-profile",0);val key="context-$profile";val previous=prefs.getString(key,null);prefs.edit().putString(key,"Independent learning").commit()
  try {
   val before=LearningProgress(context,profile).records().map{it.toString()}
   val catalog=LearningCatalog(context)
   for(scenario in catalog.scenarios){val step=scenario.getJSONArray("steps").getJSONObject(0)
    ActivityScenario.launch<LearningSpatialActivity>(Intent(context,LearningSpatialActivity::class.java).putExtra("scenarioId",scenario.getString("id")).putExtra("stepId",step.getString("id"))).use{activity->
     var surface:SurfaceView?=null
     for(i in 0..100){activity.onActivity{host->surface=all(host.window.decorView).filterIsInstance<SurfaceView>().firstOrNull()};if(surface!=null&&surface!!.width>0)break;Thread.sleep(100)}
     assertNotNull(scenario.getString("id"),surface);Thread.sleep(450);pixels(surface!!)
     if(scenario.getString("id") in setOf("adult-collapse","extinguisher-check","thermal-burn","sprain-support"))capture(scenario.getString("id"))
     activity.recreate();Thread.sleep(450)
    }
   }
   assertEquals(before,LearningProgress(context,profile).records().map{it.toString()})
  }finally{if(previous==null)prefs.edit().remove(key).commit()else prefs.edit().putString(key,previous).commit()}
 }
 @Test fun detailedNativeModelsRenderAndViewStateSurvivesRecreation(){
  for(module in listOf("fire","gas","machinery","ppe","emergency"))ActivityScenario.launch<EquipmentActivity>(Intent(context,EquipmentActivity::class.java).putExtra("moduleId",module)).use{activity->
   var ready=false;for(i in 0..80){activity.onActivity{a->ready=EquipmentActivity::class.java.getDeclaredField("rendered").apply{isAccessible=true}.getBoolean(a)};if(ready)break;Thread.sleep(100)};assertTrue("$module model must be ready",ready);var surface:SurfaceView?=null;activity.onActivity{surface=all(it.window.decorView).filterIsInstance<SurfaceView>().first()};pixels(surface!!);capture(module)
   activity.recreate();ready=false;for(i in 0..80){activity.onActivity{a->ready=EquipmentActivity::class.java.getDeclaredField("rendered").apply{isAccessible=true}.getBoolean(a)};if(ready)break;Thread.sleep(100)};assertTrue("$module model must reload after recreation",ready);activity.onActivity{surface=all(it.window.decorView).filterIsInstance<SurfaceView>().first()};pixels(surface!!)
  }
 }
 @Test fun textileCloseUpFocusesTheVisibleUnderside(){
  ActivityScenario.launch<EquipmentActivity>(Intent(context,EquipmentActivity::class.java).putExtra("moduleId","ppe")).use{activity->
   awaitEquipment(activity)
   activity.onActivity{a->all(a.window.decorView).filterIsInstance<Button>().first{it.contentDescription?.toString() in setOf("Inspect equipment parts","उपकरण के पुर्ज़े जानें")}.performClick()}
   val name=SpatialCatalog(context).parts("ppe").first{it.getString("id")=="webbing"}.getString("name")
   androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(name)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(revealOnPage(),androidx.test.espresso.action.ViewActions.click())
   androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Inspect close-up")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click())
   awaitEquipment(activity);var surface:SurfaceView?=null
   activity.onActivity{a->surface=all(a.window.decorView).filterIsInstance<SurfaceView>().first();assertEquals(180f,EquipmentActivity::class.java.getDeclaredField("yaw").apply{isAccessible=true}.getFloat(a),.01f);assertEquals(25.5f,EquipmentActivity::class.java.getDeclaredField("pitch").apply{isAccessible=true}.getFloat(a),.01f)}
   pixels(surface!!);capture("ppe-webbing")
  }
 }
 @Test fun closeUpInspectorHasTinyComponentsAndKeepsSelectionOnRotation(){
  ActivityScenario.launch<EquipmentActivity>(Intent(context,EquipmentActivity::class.java).putExtra("moduleId","fire")).use{activity->
   awaitEquipment(activity)
   for(name in listOf("Inspection tag & tie","Valve-neck thread","Split hose reinforcement")){
    activity.onActivity{a->all(a.window.decorView).filterIsInstance<Button>().first{it.contentDescription?.toString() in setOf("Inspect equipment parts","उपकरण के पुर्ज़े जानें")}.performClick()}
    androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(name)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(revealOnPage(),androidx.test.espresso.action.ViewActions.click())
    androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Inspect close-up")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click())
    Thread.sleep(450);capture(name.substringBefore(' ').lowercase());var distance=0f
    activity.onActivity{a->distance=EquipmentActivity::class.java.getDeclaredField("distance").apply{isAccessible=true}.getFloat(a);assertTrue(distance<=.25f)}
    activity.recreate();awaitEquipment(activity);activity.onActivity{a->assertEquals(distance,EquipmentActivity::class.java.getDeclaredField("distance").apply{isAccessible=true}.getFloat(a),.0001f)}
   }
  }
 }
}
