package com.narvyn.suraksha

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningWorkspaceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun views(v:View):List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList())
    private fun shot(name:String){val i=InstrumentationRegistry.getInstrumentation();java.io.File(context.getExternalFilesDir(null),"workspace-011-$name.png").outputStream().use{i.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
    private fun tag(value:String)=withTagValue(equalTo<Any>(value))
    private fun clickTag(value:String){onView(tag(value)).perform(revealOnPage(),click())}
    private fun tap(text:String){onView(allOf(withText(text),isAssignableFrom(Button::class.java))).perform(revealOnPage(),click())}
    private fun isolated(block:(Store,String)->Unit){Store(context).use{owner->
        val prior=owner.workerId;val oldHi=owner.hi;val id=owner.createProfile("Workspace test","Mining");owner.switchProfile(id)
        try{Store(context).use{learner->learner.hi=false;block(learner,id)}}finally{
            owner.switchProfile(prior);owner.hi=oldHi
            SkillPracticeStore(context,id).use{it.writableDatabase.delete("practice","worker=?",arrayOf(id))}
            ProcedureStore(context,id).use{it.writableDatabase.delete("procedures","worker=?",arrayOf(id))}
            owner.writableDatabase.delete("worker_profiles","id=?",arrayOf(id))
        }
    }}
    @Test fun learnerHasThreeTabsAndAuthorisedWorkspaceDoesNotAppearForSignedOutUser()=isolated{_,_->
        AdminClient(context).clear()
        ActivityScenario.launch(MainActivity::class.java).use{s->
            s.onActivity{a->assertEquals(setOf("main-nav-home","main-nav-courses","main-nav-records"),views(a.window.decorView).mapNotNull{it.tag as? String}.filter{it.startsWith("main-nav-")}.toSet())}
            shot("home");clickTag("main-nav-courses");shot("courses");tap("Earlier industrial courses");clickTag("legacy-fire");clickTag("main-course-actions")
            onView(withText("Read or listen to lessons")).inRoot(isDialog()).perform(revealOnPage(),click())
            clickTag("main-lesson-next");s.recreate();clickTag("main-nav-records");tap("Earlier records & credentials");tap("Objective skill passport");pressBack()
            clickTag("main-profile")
            onView(isRoot()).inRoot(isDialog()).check{v,e->if(e!=null)throw e;assertFalse(views(v).filterIsInstance<TextView>().any{it.text.toString()=="Manage training"})};pressBack()
        }
    }
    @Test fun savedTransferFeedbackAndCriticalStopSurviveRecreationWithoutChangingKnowledge()=isolated{learner,id->
        val before=learner.attempts().map{it.toString()}
        val catalog=JSONObject(context.assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()})
        ActivityScenario.launch<SkillPracticeActivity>(Intent(context,SkillPracticeActivity::class.java).putExtra("moduleId","fire")).use{s->
            clickTag("skill-scenario-fire-electrical");clickTag("skill-choice-check");s.recreate();shot("transfer-feedback")
            tap("Continue challenge");clickTag("skill-choice-unsafe");s.recreate();shot("transfer-stop")
            val saved=SkillPracticeStore(context,id).use{it.records().first()};assertTrue(saved.getBoolean("finished"));assertFalse(saved.getBoolean("passed"));assertEquals(2,saved.getJSONArray("answers").length());assertFalse(saved.getBoolean("certifiable"));SkillPracticeSession.restore(saved,catalog,id)
        }
        assertEquals(before,learner.attempts().map{it.toString()})
        SkillPracticeStore(context,"another-worker").use{assertTrue(it.records().isEmpty())}
    }
    @Test fun replayDisplaysOriginalDivergenceAndNeverRewritesEvidence()=isolated{_,id->
        val session=ProcedureSession.create("gas",id,false);assertTrue(session.choose(session.step.id+"-unsafe","screen",session.data.getLong("updatedAt")+1));assertTrue(session.done)
        ProcedureStore(context,id).use{it.save(session.data)};val original=session.data.toString()
        ActivityScenario.launch<DecisionReplayActivity>(Intent(context,DecisionReplayActivity::class.java).putExtra("procedureId",session.data.getString("id"))).use{s->
            s.onActivity{a->assertTrue(views(a.window.decorView).filterIsInstance<TextView>().any{it.text.toString()=="Sequence diverged here"})};s.recreate();shot("replay")
        }
        assertEquals(original,ProcedureStore(context,id).use{it.records().single().toString()})
    }
    @Test fun untranslatedDraftDoesNotReplayThePreviousHindiCompletionNotice()=isolated{learner,id->
        learner.hi=true
        val catalog=JSONObject(context.assets.open("transfer-scenarios.json").bufferedReader().use{it.readText()})
        val attempt=SkillPracticeSession.create(catalog,"fire-electrical",id)
        while(!attempt.done)attempt.choose(attempt.current.getJSONArray("choices").objects().single{it.getBoolean("correct")}.getString("id"),attempt.data.getLong("updatedAt")+1)
        SkillPracticeStore(context,id).use{it.save(attempt.data)}
        ActivityScenario.launch<SkillPracticeActivity>(Intent(context,SkillPracticeActivity::class.java).putExtra("attemptId",attempt.data.getString("id"))).use{s->
            tap("दूसरी व्यवस्था में अभ्यास करें");clickTag("skill-scenario-fire-electrical")
            fun assertEnglishDraftIsNotHindiPlayback(){s.onActivity{a->
                val narrator=SkillPracticeActivity::class.java.getDeclaredField("narrator").apply{isAccessible=true}.get(a) as TrainingNarrator
                assertTrue(narrator.statusText().contains("हिन्दी आवाज़ उपलब्ध नहीं"));assertFalse(narrator.isSpeaking)
                assertNotNull(views(a.window.decorView).filterIsInstance<Button>().singleOrNull{it.text.toString()=="आवाज़"})
            }}
            assertEnglishDraftIsNotHindiPlayback()
            s.recreate();assertEnglishDraftIsNotHindiPlayback()
        }
    }
    @Test fun progressHistoryCanReplayAnOlderRecordAfterANewAttemptStarts()=isolated{_,id->
        val old=ProcedureSession.create("gas",id,false);old.choose(old.step.id+"-unsafe","screen",old.data.getLong("updatedAt")+1)
        val new=ProcedureSession.create("gas",id,true)
        ProcedureStore(context,id).use{it.save(old.data);it.save(new.data)}
        ActivityScenario.launch(MainActivity::class.java).use{s->
            clickTag("main-nav-records");tap("Earlier records & credentials");clickTag("main-practice-history")
            onView(tag("history-procedure-${old.data.getString("id")}")).inRoot(isDialog()).perform(revealOnPage(),click())
            tap("Return to progress")
            s.onActivity{a->assertNotNull(views(a.window.decorView).singleOrNull{it.tag=="main-page-legacy-records"})}
        }
        assertEquals(old.data.toString(),ProcedureStore(context,id).use{it.records().single{r->r.getString("id")==old.data.getString("id")}.toString()})
    }
    @Test fun offlineQuestionSearchSurvivesRecreationAndShowsDraftStatus()=isolated{_,_->
        ActivityScenario.launch(QuestionGuideActivity::class.java).use{s->
            onView(tag("question-guide-query")).perform(revealOnPage(),replaceText("Can the phone measure gas?"),closeSoftKeyboard());s.recreate()
            s.onActivity{a->assertEquals("Can the phone measure gas?",views(a.window.decorView).filterIsInstance<android.widget.EditText>().single().text.toString())}
            tap("Search offline guide");tap("Can the phone measure gas?")
            onView(isRoot()).inRoot(isDialog()).check{v,e->if(e!=null)throw e;assertTrue(views(v).filterIsInstance<TextView>().any{it.text.toString().contains("COMPETENT REVIEW REQUIRED")})};shot("question-reference");pressBack()
        }
    }
}
