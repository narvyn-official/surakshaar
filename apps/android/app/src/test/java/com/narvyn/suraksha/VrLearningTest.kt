package com.narvyn.suraksha

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VrLearningTest {
    private fun scenario(observation:Boolean=false)=JSONObject().put("id","practice").put("course","first-aid")
        .put("reviewStatus","draft").put("steps",JSONArray((1..2).map{i->
            JSONObject().put("id","step-$i").put("part",if(observation)"FMA-part"else JSONObject.NULL)
                .put("requiresObservation",observation).put("choices",JSONArray(listOf(
                    JSONObject().put("id","safe-$i").put("text","Safe response").put("correct",true),
                    JSONObject().put("id","unsafe-$i").put("text","Unsafe response").put("correct",false)
                )))
        }))
    private fun session(observation:Boolean=false)=VrLearningSession.create(scenario(observation),"vr-test","learner-one",99)
    private fun restore(s:VrLearningSession)=VrLearningSession.restore(JSONObject(s.data.toString()),s.scenario,"vr-test","learner-one")

    @Test fun incorrectDecisionNeedsExplicitRetryAndNeverAdvancesTheStep(){
        val s=session()
        assertFalse(s.choose("unsafe-1",100))
        assertTrue(s.awaitingRetry);assertFalse(s.awaitingContinue);assertFalse(s.lastCorrect);assertEquals(0,s.index)
        val rejected=s.data.toString()
        assertThrows(IllegalStateException::class.java){s.choose("safe-1",101)}
        assertThrows(IllegalStateException::class.java){s.next(101)}
        assertEquals(rejected,s.data.toString())
        s.retry(102);assertFalse(s.awaitingRetry)
        assertTrue(s.choose("safe-1",103));assertTrue(s.awaitingContinue);assertEquals(0,s.index)
        s.next(104);assertEquals(1,s.index)
        val decisions=s.events.objects().filter{it.getString("kind")=="decision"}
        assertEquals(listOf(false,true),decisions.map{it.getBoolean("correct")})
        assertEquals(listOf("decision","retry","decision","continue"),s.events.objects().map{it.getString("kind")})
    }

    @Test fun correctFeedbackCannotBeSkippedOrChosenTwice(){
        val s=session()
        assertThrows(IllegalStateException::class.java){s.next(100)}
        assertThrows(IllegalStateException::class.java){s.retry(100)}
        assertThrows(IllegalArgumentException::class.java){s.choose("injected-choice",100)}
        assertEquals(0,s.events.length())
        assertTrue(s.choose("safe-1",101))
        assertThrows(IllegalStateException::class.java){s.choose("safe-1",102)}
        assertThrows(IllegalStateException::class.java){s.retry(102)}
        s.next(103);assertFalse(s.awaitingContinue);assertFalse(s.lastCorrect)
        s.choose("safe-2",104);s.next(105)
        assertTrue(s.finished);assertTrue(s.data.getBoolean("finished"))
        val completed=s.data.toString()
        assertThrows(IllegalStateException::class.java){s.choose("safe-2",106)}
        assertThrows(IllegalStateException::class.java){s.next(106)}
        assertThrows(IllegalStateException::class.java){s.observe("FMA-part",106)}
        assertEquals(completed,s.data.toString())
    }

    @Test fun headsetObservationIsExplicitAndOnlySatisfiesTheCurrentStep(){
        val s=session(true)
        s.choose("safe-1",100)
        assertThrows(IllegalStateException::class.java){s.next(101)}
        assertThrows(IllegalArgumentException::class.java){s.observe("another-part",101)}
        s.observe("FMA-part",102)
        val once=s.data.toString();s.observe("FMA-part",103)
        assertEquals("Repeated gaze callbacks cannot manufacture repeated evidence",once,s.data.toString())
        s.next(104);s.choose("safe-2",105)
        assertThrows(IllegalStateException::class.java){s.next(106)}
        s.observe("FMA-part",107);s.next(108)
        assertTrue(s.finished)
        assertEquals(2,s.events.objects().count{it.getString("kind")=="observation"})
        assertTrue(s.events.objects().all{it.getString("presentation")=="headset-vr"})
        assertEquals("headset-decision-practice",s.data.getString("evidenceType"))
        assertFalse(s.data.getBoolean("certifiable"))
        assertEquals("not-assessed",s.data.getString("practicalCompetence"))
    }

    @Test fun interruptedWrongAndCorrectFeedbackRestoreWithoutChangingTheJournal(){
        val s=session();s.choose("unsafe-1",100)
        val wrong=restore(s);assertTrue(wrong.awaitingRetry);assertEquals(s.data.toString(),wrong.data.toString())
        wrong.retry(101);wrong.choose("safe-1",102)
        val correct=restore(wrong)
        assertTrue(correct.awaitingContinue);assertTrue(correct.lastCorrect);assertEquals(0,correct.index)
        assertEquals(wrong.data.toString(),correct.data.toString())
        correct.next(103);correct.choose("safe-2",104);correct.next(105)
        assertTrue(restore(correct).finished)
    }

    @Test fun choicesStayStableAcrossFramesAndProcessRestoration(){
        val s=session();val order=s.choices().map{it.getString("id")}
        repeat(12){assertEquals(order,s.choices().map{it.getString("id")})}
        assertEquals(order,restore(s).choices().map{it.getString("id")})
        assertEquals(setOf("safe-1","unsafe-1"),order.toSet())
    }

    @Test fun replayRejectsAnotherOwnerVersionScenarioAndForgedProgress(){
        val s=session();s.choose("unsafe-1",100)
        fun reject(change:(JSONObject)->Unit){val bad=JSONObject(s.data.toString());change(bad)
            assertThrows(IllegalArgumentException::class.java){VrLearningSession.restore(bad,s.scenario,"vr-test","learner-one")}}
        assertThrows(IllegalArgumentException::class.java){VrLearningSession.restore(s.data,s.scenario,"other-version","learner-one")}
        assertThrows(IllegalArgumentException::class.java){VrLearningSession.restore(s.data,s.scenario,"vr-test","learner-two")}
        reject{it.put("index",1)};reject{it.put("finished",true)}
        reject{it.put("awaitingContinue",true)};reject{it.put("awaitingRetry",false)}
        reject{it.put("lastCorrect",true)};reject{it.put("certifiable",true)}
        reject{it.put("scenarioId","different")};reject{it.put("presentation","ar-exploration")}
        reject{it.getJSONArray("answers").getJSONObject(0).put("correct",true)}
        reject{it.getJSONArray("answers").getJSONObject(0).put("presentation","ar-exploration")}
        reject{it.getJSONArray("answers").getJSONObject(0).put("kind","credential")}
        reject{it.put("updatedAt",98)}
        val rewritten=JSONObject(s.scenario.toString()).apply{getJSONArray("steps").getJSONObject(0).getJSONArray("choices").getJSONObject(1).put("correct",true)}
        assertThrows(IllegalArgumentException::class.java){VrLearningSession.restore(s.data,rewritten,"vr-test","learner-one")}
    }

    @Test fun unknownPayloadFieldsCannotStoreTrackingOrPrivateInstitutionData(){
        val s=session();s.choose("safe-1",100)
        val position=JSONObject(s.data.toString()).put("headPose",JSONArray(listOf(1,2,3)))
        assertThrows(IllegalArgumentException::class.java){VrLearningSession.restore(position,s.scenario,"vr-test","learner-one")}
        val frame=JSONObject(s.data.toString()).apply{getJSONArray("answers").getJSONObject(0).put("cameraFrame","private")}
        assertThrows(IllegalArgumentException::class.java){VrLearningSession.restore(frame,s.scenario,"vr-test","learner-one")}
    }

    @Test fun recordClockCanMoveBackwardsButExplicitEventTimesCannot(){
        val future=System.currentTimeMillis()+3_600_000
        val s=VrLearningSession.create(scenario(),"vr-test","learner-one",future)
        s.choose("unsafe-1");s.retry();s.choose("safe-1");s.next()
        assertTrue(s.events.objects().all{it.getLong("at")>=future})
        val before=s.data.toString()
        assertThrows(IllegalArgumentException::class.java){s.choose("safe-2",future-1)}
        assertEquals(before,s.data.toString());restore(s)
    }

    @Test fun historyAppendGuardRejectsRewritingTruncationAndAnotherRecord(){
        val s=session();s.choose("unsafe-1",100);val old=JSONObject(s.data.toString())
        s.retry(101);s.choose("safe-1",102)
        VrLearningHistory.requireAppendOnly(old,s.data)
        VrLearningHistory.requireAppendOnly(s.data,JSONObject(s.data.toString()))
        val edited=JSONObject(s.data.toString()).apply{getJSONArray("answers").getJSONObject(0).put("choiceId","safe-1")}
        assertThrows(IllegalArgumentException::class.java){VrLearningHistory.requireAppendOnly(old,edited)}
        assertThrows(IllegalArgumentException::class.java){VrLearningHistory.requireAppendOnly(s.data,old)}
        val another=VrLearningSession.create(s.scenario,"vr-test","learner-one",99)
        assertThrows(IllegalArgumentException::class.java){VrLearningHistory.requireAppendOnly(old,another.data)}
    }

    @Test fun everyBundledLearningScenarioHasAReachableHeadsetDecisionFinish(){
        val content=JSONObject(File("../../../content/learning/scenarios.json").readText())
        for(scenario in content.getJSONArray("scenarios").objects()){
            val s=VrLearningSession.create(scenario,content.getString("version"),"learner-one",99)
            var now=100L
            while(!s.finished){
                if(s.current.optBoolean("requiresObservation"))s.observe(s.current.getString("part"),now++)
                s.choose(s.current.getJSONArray("choices").objects().single{it.getBoolean("correct")}.getString("id"),now++)
                s.next(now++)
            }
            assertEquals(s.steps.size,s.events.objects().count{it.getString("kind")=="decision"})
            VrLearningSession.restore(JSONObject(s.data.toString()),scenario,content.getString("version"),"learner-one")
        }
    }
}
