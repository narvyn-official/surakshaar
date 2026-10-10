package com.narvyn.suraksha

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.`is`
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Only synthetic emulator profiles; never connect accounts, call services or alter older histories. */
@RunWith(AndroidJUnit4::class)
class LearningRevisionFlowTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var worker:String
    private lateinit var original:String
    private lateinit var prior:String
    private var hindi=false
    @Before fun isolate(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        Store(context).use { store ->
            original=store.workerId;hindi=store.hi
            prior=JSONObject().put("attempts",JSONArray(store.attempts())).put("credentials",JSONArray(store.credentials())).toString()
            worker=store.createProfile("Synthetic objective revision","Unspecified");store.switchProfile(worker);store.hi=false
        }
    }
    @After fun restore(){
        if(!::worker.isInitialized)return
        Store(context).use { it.switchProfile(original);it.hi=hindi }
        Store(context).use {
            assertEquals(prior,JSONObject().put("attempts",JSONArray(it.attempts())).put("credentials",JSONArray(it.credentials())).toString())
            it.writableDatabase.delete("worker_profiles","id=?",arrayOf(worker))
        }
        for(db in listOf("launch-learning.db","learning-revision.db"))SkillPracticeStore(context,worker,db).use {
            it.writableDatabase.delete("practice","worker=?",arrayOf(worker))
        }
        context.getSharedPreferences("learning-profile",0).edit().remove("context-$worker").commit()
    }
    private fun weak(id:String):LearningAttempt {
        val c=LearningCatalog(context);val a=LearningAttempt.create(c.scenario(id),c.version,worker)
        a.answer(a.current.getJSONArray("choices").objects().first{!it.getBoolean("correct")}.getString("id"))
        LearningProgress(context,worker).use{it.save(a)};return a
    }
    private fun revisionIntent()=Intent(context,LearningScenarioActivity::class.java).putExtra("revision",true).putExtra("courseId","first-aid")
    @Test fun revisionAnswersPersistAcrossRecreationWithoutEditingOriginalAttempt(){
        val a=weak("heat-illness");val originalRecord=a.data.toString()
        ActivityScenario.launch<LearningScenarioActivity>(revisionIntent()).use { activity ->
            onView(withText("Targeted revision")).check(matches(isDisplayed()))
            onView(withTagValue(`is`("revision-choice-early-0" as Any))).perform(revealOnPage(),click())
            onView(withText("Decision revisited")).check(matches(isDisplayed()))
            activity.recreate()
            onView(withText("Decision revisited")).check(matches(isDisplayed()))
            LearningProgress(context,worker).use { p ->
                assertEquals(originalRecord,p.records().single().toString())
                val r=p.revisionRecords().single();assertTrue(r.getBoolean("finished"))
                assertEquals("formative-objective-revision",r.getString("evidenceType"))
                assertTrue(p.weakObjectives(LearningCatalog(context)).isEmpty())
            }
            // This test launches revision as the only activity. Back should finish it;
            // Espresso.pressBack() incorrectly treats that expected exit as a failure.
            pressBackUnconditionally()
            repeat(50) { if (activity.state != androidx.lifecycle.Lifecycle.State.DESTROYED) Thread.sleep(100) }
            assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, activity.state)
        }
    }
    @Test fun schoolRevisionOnlyUsesAwarenessObjectives(){
        weak("heat-illness");weak("school-injury-help")
        context.getSharedPreferences("learning-profile",0).edit().putString("context-$worker","School").commit()
        ActivityScenario.launch<LearningScenarioActivity>(revisionIntent()).use {
            onView(withText("School: recognise injury and get help")).check(matches(isDisplayed()))
            onView(withTagValue(`is`("revision-choice-space-0" as Any))).perform(revealOnPage(),click())
            onView(withText("Next revision")).perform(revealOnPage(),click())
            onView(withText("No tricky decisions waiting")).check(matches(isDisplayed()))
            LearningProgress(context,worker).use { p ->
                assertEquals(1,p.weakObjectives(LearningCatalog(context),awarenessOnly=false).size)
                assertTrue(p.weakObjectives(LearningCatalog(context),awarenessOnly=true).isEmpty())
            }
        }
    }
    @Test fun historicalReplayUsesArchivedWordingAndKeepsOriginalVersion(){
        val c=LearningCatalog(context);val version="learning-2026.10.10-draft1"
        val scene=c.scenarioForVersion("adult-collapse",version)!!
        val a=LearningAttempt.create(scene,version,worker)
        val choice=a.current.getJSONArray("choices").objects().first{it.getBoolean("correct")};a.answer(choice.getString("id"))
        LearningProgress(context,worker).use{it.save(a)}
        ActivityScenario.launch<LearningScenarioActivity>(Intent(context,LearningScenarioActivity::class.java).putExtra("showRecords",true)).use {
            onView(withText("An adult collapses · in progress")).perform(revealOnPage(),click())
            onView(withText("$version · original events")).check(matches(isDisplayed()))
            onView(withText("Pause at the scene\n${choice.getString("text")}\nAppropriate decision")).perform(revealOnPage()).check(matches(isDisplayed()))
        }
    }
}
