package com.narvyn.suraksha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FirstAidScenarioCatalogTest {
    private fun create(id:String)=FirstAidBay.createScenario("learner","bay-content",id,100)
    private fun restore(bay:FirstAidBay)=FirstAidBay.restore(JSONObject(bay.data.toString()),"learner","bay-content")
    private fun correct(bay:FirstAidBay)=bay.current.actions.single{it.correct}.id

    @Test fun staticModelDescriptionsDoNotClaimAnimatedClinicalSignsOrMeshDeformation(){
        val limb=FirstAidScenarioCatalog.find("suspected-limb-injury")!!.steps.first{it.id=="red-flags"}
        assertTrue(limb.cue.contains("Authored report"));assertTrue(limb.cue.contains("not demonstrated by the mesh"))
        assertFalse(limb.cue.contains("looks deformed"))
        val seizure=FirstAidScenarioCatalog.find("seizure-response")!!
        assertTrue(seizure.steps.first{it.id=="observe"}.visual!!.label.contains("not animated"))
        val recovery=FirstAidScenarioCatalog.find("unresponsive-normal-breathing")!!
        assertTrue(recovery.steps.first{it.id=="breathing"}.cue.contains("not animated or measured"))
        assertTrue(recovery.steps.first{it.id=="monitor"}.cue.contains("does not animate breathing"))
        for(id in listOf("fainting-response-bay","unresponsive-normal-breathing")) {
            val s=FirstAidScenarioCatalog.find(id)!!
            val last=s.steps.last().visual!!
            assertEquals(FirstAidScenarioCatalog.Response.UNRESPONSIVE,last.response)
            assertTrue(last.label.contains("unresponsive"));assertTrue(last.label.contains("abnormal gasps"))
        }
    }

    @Test fun eightExplicitAdultScenariosHaveSourcesBilingualDraftCopyAndSpatialObservations(){
        val all=FirstAidScenarioCatalog.all
        assertEquals(8,all.size);assertEquals(8,all.map{it.id}.toSet().size)
        assertNotNull(FirstAidScenarioCatalog.find(FirstAidScenarioCatalog.DEFAULT_ID))
        assertNull(FirstAidScenarioCatalog.find("every-medical-condition"))
        for(s in all){
            assertEquals("adult",s.casualtyAge);assertEquals("draft",s.reviewStatus)
            assertEquals("authored-draft-unreviewed",s.localeStatus["hi"])
            assertTrue(s.title(true).any{it in '\u0900'..'\u097F'});assertTrue(s.summary(true).any{it in '\u0900'..'\u097F'})
            assertTrue(s.fingerprint.matches(Regex("[a-f0-9]{64}")))
            assertEquals(s.steps.size,s.steps.map{it.id}.toSet().size)
            assertEquals(s.steps.flatMap{it.actions}.size,s.steps.flatMap{it.actions}.map{it.id}.toSet().size)
            assertTrue(s.steps.any{st->st.actions.any{it.spatial&&it.id.startsWith("inspect:")}})
            for(step in s.steps){
                assertNull(step.partId);assertFalse(step.instructionalPause)
                assertNotNull(step.visual);assertTrue(step.source.startsWith("https://"))
                assertEquals(1,step.actions.count{it.correct})
                assertTrue(step.title(true).any{it in '\u0900'..'\u097F'})
                assertTrue(step.cue(true).any{it in '\u0900'..'\u097F'})
                for(a in step.actions){assertTrue(a.label(true).any{it in '\u0900'..'\u097F'});assertTrue(a.feedback(true).any{it in '\u0900'..'\u097F'})}
            }
        }
    }

    @Test fun allScenariosCompleteRestoreAndRetainWrongChoicesWithoutPhysicalCompetenceClaims(){
        for(s in FirstAidScenarioCatalog.all){
            var bay=create(s.id);var now=101L
            while(!bay.finished){
                val index=bay.index
                bay.current.actions.firstOrNull{!it.correct}?.let { wrong ->
                    val result=bay.act(wrong.id,"3d",false,now++)
                    assertEquals("incorrect",result.reason);assertFalse(result.advanced);assertEquals(index,bay.index)
                    assertTrue(result.message(true).any{it in '\u0900'..'\u097F'})
                }
                assertTrue(bay.act(correct(bay),"3d",false,now++).advanced)
                bay=restore(bay)
            }
            assertEquals(s.steps.size,bay.index);assertEquals("3d",bay.presentation)
            assertFalse(bay.data.getBoolean("certifiable"));assertEquals("not-assessed",bay.data.getString("practicalCompetence"))
            assertTrue(bay.events.objects().any{it.optString("reason")=="incorrect"})
            assertEquals(s.fingerprint,bay.data.getString("scenarioFingerprint"))
        }
    }

    @Test fun trackingLossCannotAdvanceAnyNewScenarioIncludingNonSpatialDecisions(){
        for(s in FirstAidScenarioCatalog.all){
            val bay=create(s.id);var now=101L
            while(!bay.finished){
                val prior=bay.index
                assertEquals("tracking-unavailable",bay.act(correct(bay),"ar",false,now++).reason)
                assertEquals(prior,bay.index)
                assertTrue(bay.act(correct(bay),"ar",true,now++).advanced)
            }
            assertEquals(s.steps.size,bay.events.objects().count{it.optString("reason")=="tracking-unavailable"})
            restore(bay)
        }
    }

    @Test fun scenarioDefinitionOwnerAndVersionArePinned(){
        val bay=create(FirstAidScenarioCatalog.DEFAULT_ID);bay.act(correct(bay),"3d",false,101)
        val text=bay.data.toString()
        for((key,value) in listOf("scenarioId" to "bleeding-injury","scenarioDefinitionVersion" to 2,"scenarioFingerprint" to "f".repeat(64),"workerId" to "another","contentVersion" to "changed","certifiable" to true)){
            assertThrows(IllegalArgumentException::class.java){FirstAidBay.restore(JSONObject(text).put(key,value),"learner","bay-content")}
        }
        assertThrows(IllegalArgumentException::class.java){FirstAidBay.createScenario("learner","bay-content","unknown",100)}
    }

    @Test fun oldVersionOneRecordsKeepTheirOriginalTwelveStageDefinition(){
        val legacy=FirstAidBay.create("learner","bay-content",100)
        assertEquals(FirstAidBay.SCENARIO_ID,legacy.scenarioId)
        assertEquals(12,legacy.steps.size);assertEquals(FirstAidBay.steps,legacy.steps)
        assertFalse(legacy.data.has("scenarioFingerprint"));assertFalse(legacy.data.has("scenarioDefinitionVersion"))
        legacy.act("ready-space","3d",false,101)
        assertEquals("response",legacy.current.id)
        assertEquals("select:FMA50801",correct(legacy))
        val snapshot=legacy.data.toString();val restored=restore(legacy)
        assertEquals(snapshot,restored.data.toString());assertEquals(legacy.steps,restored.steps)
    }

    @Test fun urgentNewResponseGetsHelpBeforeBreathingObservationAndNeverRequiresAnatomy(){
        for(id in listOf("adult-collapse-response","unresponsive-normal-breathing")){
            val bay=create(id)
            assertTrue(bay.steps.indexOfFirst{it.actions.any{a->a.id.startsWith("call-")}} < bay.steps.indexOfFirst{it.id=="breathing"})
            assertTrue(bay.steps.none{it.partId!=null||it.actions.any{a->a.id.startsWith("select:")}})
        }
        val recovery=FirstAidScenarioCatalog.find("unresponsive-normal-breathing")!!
        assertTrue(recovery.steps.first{it.id=="position"}.cue.contains("no trauma"))
        assertEquals(FirstAidScenarioCatalog.Pose.RECOVERY_SIDE,recovery.steps.first{it.id=="monitor"}.visual!!.pose)
        val changed=recovery.steps.first{it.id=="change"}
        assertEquals(FirstAidScenarioCatalog.Breathing.ABNORMAL,changed.visual!!.breathing)
        assertEquals("change-to-cpr",changed.actions.single{it.correct}.id)
    }

    @Test fun dangerousSpecificActionsAreRejectedForTheAuthoredCases(){
        val forbidden=mapOf("bleeding-injury" to "pull-glass","thermal-burn-response" to "ice-remedy",
            "suspected-limb-injury" to "straighten-limb","seizure-response" to "spoon-mouth",
            "fainting-response-bay" to "give-drink","heat-illness-response" to "force-water")
        for((id,action) in forbidden){
            val bay=create(id);var now=101L
            while(bay.current.actions.none{it.id==action})bay.act(correct(bay),"3d",false,now++)
            val before=bay.index;val result=bay.act(action,"3d",false,now++)
            assertEquals("incorrect",result.reason);assertFalse(result.advanced);assertEquals(before,bay.index)
            restore(bay)
        }
    }

    @Test fun pauseStopMixedModeAndClockCorrectionStillWorkForNewDefinitions(){
        val bay=create("bleeding-injury")
        bay.pause(101);assertEquals("paused",bay.act(correct(bay),"3d",false,102).reason)
        bay.resume(103);bay.act(correct(bay),"3d",false,104);bay.act(correct(bay),"ar",true,105)
        assertEquals("mixed",bay.presentation)
        assertThrows(IllegalArgumentException::class.java){bay.act(correct(bay),"ar",true,100)}
        bay.stop(106);assertTrue(restore(bay).stopped)
        val text=bay.data.toString();assertFalse(bay.act("ready-space","3d",false,107).accepted);assertEquals(text,bay.data.toString())
    }
}
