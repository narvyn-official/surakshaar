package com.narvyn.suraksha

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LearningRevisionTest {
    private val version="revision-test-v2"
    private val worker="learner-one"
    private fun scene()=JSONObject().put("id","safe-response").put("course","first-aid").put("reviewStatus","draft")
        .put("audience","adult").put("steps",JSONArray(listOf("safe","handover").map { id ->
            JSONObject().put("id",id).put("safetyCritical",id=="safe").put("choices",JSONArray(listOf(
                JSONObject().put("id","$id-correct").put("correct",true),JSONObject().put("id","$id-wrong").put("correct",false))))
        }))
    private fun attempt(s:JSONObject=scene(),v:String=version,w:String=worker,at:Long=100)=LearningAttempt.create(s,v,w,now=at)
    private fun targets(records:List<JSONObject>,checks:List<JSONObject> = emptyList(),s:JSONObject=scene())=
        LearningRevisionPlan.targets(records,checks,listOf(s),version,worker)

    @Test fun queueUsesRecordedMissesAndHintsWithoutMutatingOriginal(){
        val a=attempt();a.answer("safe-wrong",101);a.answer("safe-correct",102);a.advance(103);a.hint(104)
        val original=a.data.toString();val queue=targets(listOf(a.data))
        assertEquals(listOf("safe","handover"),queue.map{it.step.getString("id")})
        assertEquals(1,queue[0].misses);assertEquals(1,queue[1].hints);assertEquals(original,a.data.toString())
    }
    @Test fun oldVersionsOtherPeopleAndTamperedEvidenceDoNotDriveCurrentRevision(){
        val old=attempt(v="revision-test-v1");old.answer("safe-wrong",101)
        val other=attempt(w="learner-two");other.answer("safe-wrong",101)
        val tampered=attempt();tampered.answer("safe-wrong",101)
        tampered.events.getJSONObject(0).put("correct",true)
        assertTrue(targets(listOf(old.data,other.data,tampered.data)).isEmpty())
    }
    @Test fun laterCleanAttemptResolvesOnlyTheObjectiveActuallyRevisited(){
        val a=attempt();a.answer("safe-wrong",101);a.answer("safe-correct",102);a.advance(103);a.hint(104)
        val fresh=attempt(at=200);fresh.answer("safe-correct",201)
        assertEquals(listOf("handover"),targets(listOf(a.data,fresh.data)).map{it.step.getString("id")})
    }
    @Test fun targetedRetryPreservesMissAndIsNotACleanIndependentCheck(){
        val s=scene();val r=LearningRevision.create(s,s.getJSONArray("steps").getJSONObject(0),version,worker,100)
        assertFalse(r.answer("safe-wrong",101));assertTrue(r.answer("safe-correct",102));assertFalse(r.clean)
        assertEquals(2,r.events.length());assertFalse(r.events.getJSONObject(0).getBoolean("correct"))
        assertThrows(IllegalStateException::class.java){r.answer("safe-correct",103)}
        assertEquals("formative-objective-revision",r.data.getString("evidenceType"))
        assertFalse(r.data.optBoolean("certifiable"))
        val replay=LearningRevision.restore(JSONObject(r.data.toString()),s,r.step,version,worker)
        assertTrue(replay.passed);assertFalse(replay.clean)
    }
    @Test fun cleanRevisionMustFollowTheOriginalProblemAndBeTheLatestCheck(){
        val a=attempt();a.answer("safe-wrong",101)
        val step=a.current
        val old=LearningRevision.create(a.scenario,step,version,worker,50);old.answer("safe-correct",51)
        assertEquals(1,targets(listOf(a.data),listOf(old.data)).size)
        val fresh=LearningRevision.create(a.scenario,step,version,worker,200);fresh.answer("safe-correct",201)
        assertTrue(targets(listOf(a.data),listOf(fresh.data)).isEmpty())
        val later=LearningRevision.create(a.scenario,step,version,worker,300);later.answer("safe-wrong",301)
        assertEquals(1,targets(listOf(a.data),listOf(fresh.data,later.data)).size)
    }
    @Test fun guidedRevisionDoesNotClearWeakQueue(){
        val a=attempt();a.hint(101)
        val r=LearningRevision.create(a.scenario,a.current,version,worker,200);r.hint(201);r.answer("safe-correct",202)
        assertFalse(r.clean);assertEquals(1,targets(listOf(a.data),listOf(r.data)).size)
    }
    @Test fun revisionRejectsIdentityVersionObjectiveAndChangedCorrectness(){
        val a=attempt();val r=LearningRevision.create(a.scenario,a.current,version,worker,200);r.answer("safe-wrong",201)
        assertThrows(IllegalArgumentException::class.java){LearningRevision.restore(r.data,a.scenario,a.current,version,"another-worker")}
        assertThrows(IllegalArgumentException::class.java){LearningRevision.restore(r.data,a.scenario,a.current,"older-version",worker)}
        assertThrows(IllegalArgumentException::class.java){LearningRevision.restore(r.data,a.scenario,a.steps[1],version,worker)}
        val changed=JSONObject(r.data.toString());changed.getJSONArray("answers").getJSONObject(0).put("correct",true)
        assertThrows(IllegalArgumentException::class.java){LearningRevision.restore(changed,a.scenario,a.current,version,worker)}
    }
    @Test fun clockRollbackUnknownChoicesAndEarlyReflectionDoNotAppend(){
        val a=attempt();val r=LearningRevision.create(a.scenario,a.current,version,worker,Long.MAX_VALUE-100)
        val original=r.data.toString()
        assertThrows(IllegalArgumentException::class.java){r.answer("safe-correct",100)}
        assertThrows(IllegalStateException::class.java){r.answer("unknown",Long.MAX_VALUE-99)}
        assertThrows(IllegalStateException::class.java){r.reflect("too early",Long.MAX_VALUE-99)}
        assertEquals(original,r.data.toString());r.answer("safe-correct");r.reflect("x".repeat(1200))
        assertEquals(1000,r.events.getJSONObject(1).getString("text").length)
        LearningRevision.restore(r.data,a.scenario,a.current,version,worker)
    }
    @Test fun lateReflectionCannotMakeAnOldDecisionOverrideNewEvidence(){
        val old=attempt();old.answer("safe-wrong",101);old.answer("safe-correct",102);old.advance(103)
        old.answer("handover-correct",104);old.advance(105)
        val fresh=attempt(at=200);fresh.answer("safe-correct",201)
        old.explain("A later reflection about an earlier practice",300)
        assertTrue(targets(listOf(old.data,fresh.data)).isEmpty())
        val check=LearningRevision.create(old.scenario,old.steps[0],version,worker,110);check.answer("safe-correct",111)
        val newMiss=attempt(at=200);newMiss.answer("safe-wrong",201)
        check.reflect("Later reflection does not repeat the check",400)
        assertEquals(1,targets(listOf(newMiss.data),listOf(check.data)).size)
    }
    @Test fun sameTimestampAmbiguityKeepsTheWeakObjectiveVisible(){
        val wrong=attempt();wrong.answer("safe-wrong",101)
        val right=attempt();right.answer("safe-correct",101)
        assertEquals(1,targets(listOf(right.data,wrong.data)).size)
        assertEquals(1,targets(listOf(wrong.data,right.data)).size)
    }
    @Test fun archivedDraftOneRemainsReadableAndReplayableAsItsOwnVersion(){
        val archiveFile=File("../../../content/learning/archive/learning-2026.10.10-draft1.json")
        assertEquals("ebaae0b6cbfae944c231344c041838e2d840ce50da7b65a32c779903717c857f", java.security.MessageDigest.getInstance("SHA-256").digest(archiveFile.readBytes()).joinToString(""){"%02x".format(it)})
        val archive=JSONObject(archiveFile.readText())
        val current=JSONObject(File("../../../content/learning/scenarios.json").readText())
        assertNotEquals(archive.getString("version"),current.getString("version"))
        assertEquals(12,archive.getJSONArray("scenarios").length())
        val s=archive.getJSONArray("scenarios").getJSONObject(0)
        val original=attempt(s,archive.getString("version"));val choice=original.current.getJSONArray("choices").objects().first{it.getBoolean("correct")}
        original.answer(choice.getString("id"),101)
        LearningAttempt.restore(original.data,s,archive.getString("version"),worker)
        assertThrows(IllegalArgumentException::class.java){LearningAttempt.restore(original.data,s,current.getString("version"),worker)}
        assertTrue(targets(listOf(original.data),s=s).isEmpty())
    }
}
