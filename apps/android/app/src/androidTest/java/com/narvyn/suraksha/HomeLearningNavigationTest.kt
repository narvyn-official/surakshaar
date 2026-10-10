package com.narvyn.suraksha

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.equalTo
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Synthetic native navigation fixtures; no account mutation, camera use or simulated certification. */
@RunWith(AndroidJUnit4::class)
class HomeLearningNavigationTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=ins.targetContext
    private lateinit var worker:String
    private lateinit var original:String
    private var originalHindi=false
    private val extraProfiles=mutableListOf<String>()
    private var oldFont:String?=null
    private val font get()=InstrumentationRegistry.getArguments().getString("fontScale")
    private val landscape get()=InstrumentationRegistry.getArguments().getString("orientation")=="landscape"
    private val suffix get()="${if(landscape)"landscape"else"portrait"}-${font?:"1.0"}"
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun shell(command:String)=ins.uiAutomation.executeShellCommand(command).use{FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim()}
    private fun tap(tag:String){onView(withTagValue(equalTo<Any>(tag))).perform(revealOnPage(),click())}
    private fun back(){shell("input keyevent 4");ins.waitForIdleSync()}
    private fun waitUntil(message:String,test:()->Boolean){repeat(80){if(test())return;Thread.sleep(80)};error(message)}
    private fun capture(name:String){ins.waitForIdleSync();ins.uiAutomation.waitForIdle(100,2000);val image=ins.uiAutomation.takeScreenshot()?:error("No screenshot");File(context.getExternalFilesDir("validation"),"home-$name-$suffix.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()}
    private fun launch():ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java).also{s->
        if(landscape){s.onActivity{it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE};waitUntil("Landscape did not settle"){var ready=false;s.onActivity{ready=it.resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE};ready}}
    }
    private fun page(s:ActivityScenario<MainActivity>,expected:String){s.onActivity{assertNotNull(views(it.window.decorView).singleOrNull{v->v.tag=="main-page-$expected"})}}
    private fun reachable(s:ActivityScenario<MainActivity>,tag:String){
        onView(withTagValue(equalTo<Any>(tag))).perform(revealOnPage())
        s.onActivity{a->val button=views(a.window.decorView).single{it.tag==tag} as Button;val rect=Rect()
            assertTrue("$tag must be reachable",button.getGlobalVisibleRect(rect)&&rect.width()>=button.width*.95f&&rect.height()>=button.height*.95f)
            assertTrue("$tag must remain 48dp",button.width>=a.dp(48)&&button.height>=a.dp(48))
            assertTrue("$tag caption clipped",button.layout.height<=button.height-button.compoundPaddingTop-button.compoundPaddingBottom)
            assertTrue("$tag caption ellipsised",(0 until button.layout.lineCount).all{button.layout.getEllipsisCount(it)==0})
        }
    }
    @Before fun isolate(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        font?.let{require(it in setOf("1.3","2.0"));oldFont=shell("settings get system font_scale");shell("settings put system font_scale $it")}
        Store(context).use{original=it.workerId;originalHindi=it.hi;worker=it.createProfile("Synthetic Home learner","Unspecified");it.switchProfile(worker);it.hi=false}
    }
    @After fun restore(){
        oldFont?.let{if(it=="null")shell("settings delete system font_scale")else shell("settings put system font_scale $it")}
        if(!::worker.isInitialized)return
        Store(context).use{it.switchProfile(original);it.hi=originalHindi;for(id in extraProfiles+worker)it.writableDatabase.delete("worker_profiles","id=?",arrayOf(id))}
        for(db in listOf("launch-learning.db","first-aid-bay.db"))SkillPracticeStore(context,worker,db).use{it.writableDatabase.delete("practice","worker=?",arrayOf(worker))}
        RoomMissionStore(context,worker).use{it.writableDatabase.delete("room_missions","worker=?",arrayOf(worker))}
        context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()
    }
    @Test fun homeAndLearnHaveDifferentJobsInBothLanguagesAndBackRetainsTheCatalog(){
        for(hindi in listOf(false,true)){
            Store(context).use{it.hi=hindi}
            launch().use{s->
                s.onActivity{a->val all=views(a.window.decorView)
                    assertTrue("Home must not repeat course cards",all.none{(it.tag as? String)?.startsWith("course-")==true})
                    assertNotNull(all.singleOrNull{it.tag=="main-home-progress"})
                    assertNotNull(all.singleOrNull{it.tag=="main-home-account-status"})
                    assertTrue(all.none{it is ScrollView})
                    val panel=all.filterIsInstance<PagedPanel>().single()
                    if(panel.pageCount>1){
                        panel.nextPage();assertEquals(1,panel.pageIndex)
                        @Suppress("DEPRECATION") a.onBackPressed()
                        assertFalse("Legacy Android Back must keep paged Home open",a.isFinishing);assertEquals(0,panel.pageIndex)
                    }
                    if(font==null&&!landscape)assertEquals("Default Home must fit one screen",1,all.filterIsInstance<PagedPanel>().single().pageCount)
                    if(landscape){
                        assertTrue("Short Home needs at most two explicit pages",all.filterIsInstance<PagedPanel>().single().pageCount<=2)
                        val primary=all.single{it.tag=="main-browse-courses"};val bounds=Rect();assertTrue("Home starts with a usable primary action",primary.getGlobalVisibleRect(bounds)&&bounds.height()>=primary.height*.95f)
                    }
                }
                for(tag in listOf("main-browse-courses","main-home-progress","main-home-account","main-nav-home","main-nav-courses","main-nav-records"))reachable(s,tag)
                tap("main-nav-home");capture(if(hindi)"empty-hi"else"empty-en")
                tap("main-browse-courses");page(s,"courses")
                s.onActivity{a->val all=views(a.window.decorView)
                    assertEquals(setOf("course-fire","course-first-aid"),all.mapNotNull{it.tag as? String}.filter{it.startsWith("course-")}.toSet())
                    assertTrue(all.single{it.tag=="main-nav-courses"}.isSelected)
                    if(landscape){
                        assertTrue("Short Learn needs at most two explicit pages",all.filterIsInstance<PagedPanel>().single().pageCount<=2)
                        for(tag in listOf("course-fire","course-first-aid")){val primary=all.single{it.tag==tag};val bounds=Rect();assertTrue("$tag must be available immediately",primary.getGlobalVisibleRect(bounds)&&bounds.height()>=primary.height*.95f)}
                    }
                    assertEquals(if(hindi)"पाठ्यक्रम सूची"else"Course catalog",all.single{it.tag=="main-page-courses"}.accessibilityPaneTitle)
                }
                for(tag in listOf("course-fire","course-first-aid","main-learning-library"))reachable(s,tag)
                tap("main-nav-courses");capture(if(hindi)"catalog-hi"else"catalog-en")
                tap("main-learning-library");page(s,"library");tap("legacy-fire");page(s,"module")
                tap("main-back");page(s,"library");tap("main-back");page(s,"courses")
                s.recreate();page(s,"courses");back();page(s,"home")
            }
        }
    }
    @Test fun resumeUsesTheNewestValidPracticeAndSchoolContextCannotResumeAdultPractice(){
        val catalog=LearningCatalog(context)
        val decision=LearningAttempt.create(catalog.scenario("escape-first"),catalog.version,worker,now=1000)
        LearningProgress(context,worker).use{it.save(decision)}
        val bay=FirstAidBay.createScenario(worker,catalog.version,"bleeding-injury",2000)
        SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.save(bay.data);it.save(FirstAidBay.createScenario(worker,"uninstalled-content","bleeding-injury",9000).data)}
        val engine=RoomMission("fire",false,FireRoomScenarioCatalog.DEFAULT_ID);engine.act("alarm",1)
        val room=JSONObject().put("id",UUID.randomUUID().toString()).put("workerId",worker).put("module","fire").put("mode","screen").put("updatedAt",3000L)
            .put("mission",engine.toJson()).put("coaching",RoomCoaching("fire",true).toJson())
        RoomMissionStore(context,worker).use{it.save(room)}
        val captured=AtomicReference<Intent?>()
        val monitor=object:Instrumentation.ActivityMonitor(){override fun onStartActivity(intent:Intent):Instrumentation.ActivityResult?{
            if(intent.component?.className?.let { it in setOf(RoomMissionActivity::class.java.name,FirstAidBayActivity::class.java.name,LearningScenarioActivity::class.java.name) } == true){
                captured.set(Intent(intent));return Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null)
            };return null
        }}
        ins.addMonitor(monitor)
        try{launch().use{s->
            capture("resume-fire");tap("main-continue")
            assertEquals(RoomMissionActivity::class.java.name,captured.get()?.component?.className)
            assertEquals(room.getString("id"),captured.get()?.getStringExtra("resumeId"));assertFalse(captured.get()!!.getBooleanExtra("camera",true));assertTrue(captured.get()!!.getBooleanExtra("recall",false))
            assertEquals(FireRoomScenarioCatalog.DEFAULT_ID,captured.get()?.getStringExtra("fireScenarioId"))
            assertTrue(bay.pause(4000));SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.save(bay.data)};s.recreate();captured.set(null)
            tap("main-continue");assertEquals(FirstAidBayActivity::class.java.name,captured.get()?.component?.className);assertEquals(bay.data.getString("id"),captured.get()?.getStringExtra("attemptId"))
            context.getSharedPreferences("learning-profile",0).edit().putString("context-$worker","School").commit();s.recreate();captured.set(null)
            tap("main-continue");assertEquals(LearningScenarioActivity::class.java.name,captured.get()?.component?.className);assertEquals("escape-first",captured.get()?.getStringExtra("scenarioId"))
            capture("school-resume")
        }}finally{ins.removeMonitor(monitor)}
        assertEquals(room.toString(),RoomMissionStore(context,worker).use{it.record(room.getString("id"))!!.toString()})
        assertEquals(decision.data.toString(),LearningProgress(context,worker).use{it.records().single().toString()})
        assertEquals(bay.data.toString(),SkillPracticeStore(context,worker,"first-aid-bay.db").use{it.records().single{r->r.getString("id")==bay.data.getString("id")}.toString()})
    }
    @Test fun recentProgressIsProfileScopedAndAccountEntryHasOneExplicitDestination(){
        val catalog=LearningCatalog(context);val done=LearningAttempt.create(catalog.scenario("escape-first"),catalog.version,worker,now=1000)
        while(!done.finished){done.answer(done.current.getJSONArray("choices").objects().single{it.getBoolean("correct")}.getString("id"),done.data.getLong("updatedAt")+1);done.advance(done.data.getLong("updatedAt")+1)}
        LearningProgress(context,worker).use{it.save(done)}
        val captured=AtomicReference<Intent?>();val monitor=object:Instrumentation.ActivityMonitor(){override fun onStartActivity(intent:Intent):Instrumentation.ActivityResult?{
            if(intent.component?.className==LearnerCentreActivity::class.java.name){captured.set(Intent(intent));return Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null)};return null
        }};ins.addMonitor(monitor)
        try{launch().use{s->
            s.onActivity{a->val all=views(a.window.decorView);assertNotNull(all.singleOrNull{it.tag=="main-home-recent"});assertTrue((all.single{it.tag=="main-home-progress"} as Button).text.contains("1 completed"));assertNull(all.singleOrNull{it.tag=="main-continue"})}
            if(landscape)s.onActivity{a->val all=views(a.window.decorView);val panel=all.filterIsInstance<PagedPanel>().single();assertEquals("Completed Home stays on one short-screen page",1,panel.pageCount)
                for(tag in listOf("main-browse-courses","main-home-progress","main-home-account")){val action=all.single{it.tag==tag};val bounds=Rect();assertTrue("Completed Home must not clip $tag",action.getGlobalVisibleRect(bounds)&&bounds.height()>=action.height*.95f)}
            }
            capture("completed");tap("main-home-account");assertEquals("account",captured.get()?.getStringExtra(LearnerCentreActivity.EXTRA_DESTINATION))
            tap("main-nav-courses");tap("main-nav-home");s.recreate()
            s.onActivity{a->assertNotNull(views(a.window.decorView).singleOrNull{it.tag=="main-home-recent"})}
            // Another isolated learner must never receive this learner's summary.
            Store(context).use{val other=it.createProfile("Other synthetic learner","Unspecified");extraProfiles.add(other);it.switchProfile(other)};s.recreate()
            s.onActivity{a->assertTrue(views(a.window.decorView).filterIsInstance<android.widget.TextView>().none{it.tag=="main-home-learner"&&it.text.toString()=="Synthetic Home learner"})}
            s.onActivity{a->assertNull(views(a.window.decorView).singleOrNull{it.tag=="main-home-recent"})}
        }}finally{ins.removeMonitor(monitor);Store(context).use{it.switchProfile(worker)}}
        assertEquals(done.data.toString(),LearningProgress(context,worker).use{it.records().single().toString()})
    }
}
