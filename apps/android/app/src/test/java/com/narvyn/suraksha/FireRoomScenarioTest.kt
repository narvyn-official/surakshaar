package com.narvyn.suraksha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FireRoomScenarioTest {
    private fun scenario(id:String=FireRoomScenarioCatalog.DEFAULT_ID, explosion:Boolean=false) = RoomMission("fire",explosion,id)
    private fun setup(m:RoomMission) {
        assertTrue(m.act("alarm",1)); assertTrue(m.act("select-clear-exit",2))
        assertTrue(m.act("select-suitable-extinguisher",3))
        assertEquals("LABEL_CHECK",m.phase)
    }
    private fun replay(m:RoomMission):RoomMission = RoomMission.restore(JSONObject(m.toJson().toString())).also {
        assertEquals(m.toJson().toString(),it.toJson().toString())
    }
    private fun finishExit(m:RoomMission, now:Long) {
        assertEquals("ROUTE_RECHECK",m.phase)
        assertTrue(m.routeChanged); assertEquals(0,m.effectiveExitStation)
        assertFalse(m.act("follow-original-route",now));assertFalse(m.act("follow-clear-route",now+1))
        assertFalse(m.act("enter-smoke",now+2));assertEquals("ROUTE_RECHECK",m.phase)
        assertTrue(m.act("follow-alternate-route",now+3));assertEquals("ASSEMBLY",m.phase)
        assertTrue(m.act("reach-assembly-point",now+4));assertFalse(m.act("reenter-search",now+5))
        assertTrue(m.act("report-missing-worker",now+6));assertEquals("FIRE_HANDOVER",m.phase)
        assertFalse(m.act("claim-fire-safe",now+7));assertFalse(m.act("return-for-equipment",now+8))
        assertFalse(m.completed);assertTrue(m.act("report-fire-handover",now+9));assertTrue(m.completed)
        assertFalse(m.toJson().getJSONObject("result").getBoolean("certifiable"));replay(m)
    }

    @Test fun goodUnitRequiresLabelPressureAndHoseEvidenceBeforePinThenChangedRoute() {
        val m=scenario();assertFalse(m.routeChanged);assertEquals(2,m.effectiveExitStation)
        assertEquals("good",m.fireEquipmentCondition);setup(m)
        assertFalse(m.act("use-colour-only",4));assertFalse(m.act("pin-drag",5))
        assertTrue(m.act("confirm-label-match",6));assertEquals("CONDITION_CHECK",m.phase);replay(m)
        assertFalse(m.act("pin-drag",7));assertFalse(m.act("ignore-low-pressure",8))
        assertTrue(m.act("confirm-pressure-range",9));assertEquals("HOSE_CHECK",m.phase);replay(m)
        assertFalse(m.act("pin-drag",10));assertFalse(m.act("ignore-damaged-hose",11))
        assertTrue(m.act("confirm-hose-intact",12));assertEquals("PIN",m.phase);replay(m)
        assertTrue(m.act("pin-drag",13));for(t in 100L..400L step 100)m.aim(0f,0f,false,t,true)
        assertEquals("SWEEP",m.phase)
        for(i in 0..15)m.aim(-1f+2f*i/15f,0f,true,500L+i*100,true)
        assertEquals("WITHDRAW",m.phase)
        assertFalse(m.act("withdraw",2100));assertFalse(m.act("continue-discharge",2101))
        assertTrue(m.act("release",2102));assertTrue(m.act("withdraw",2103));replay(m)
        finishExit(m,2200)
        val evidence=m.toJson().getJSONObject("scenario")
        assertTrue(evidence.getBoolean("labelChecked"));assertTrue(evidence.getBoolean("pressureChecked"))
        assertTrue(evidence.getBoolean("hoseChecked"));assertFalse(evidence.getBoolean("damagedUnitRejected"))
        assertTrue(evidence.getBoolean("alternateExitSelected"));assertEquals(2,m.measurements.size)
    }

    @Test fun damagedUnitMustBeRefusedCannotBorrowGoodInspectionOrReachDischarge() {
        val m=scenario(FireRoomScenarioCatalog.DAMAGED_ID);assertEquals("damaged",m.fireEquipmentCondition);setup(m)
        assertTrue(m.act("confirm-label-match",4));assertEquals("DAMAGED_UNIT",m.phase)
        for((i,action) in listOf("use-damaged-unit","confirm-pressure-range","confirm-hose-intact","pin-drag","follow-alternate-route").withIndex()) {
            assertFalse(m.act(action,5L+i));assertEquals("DAMAGED_UNIT",m.phase)
        }
        assertTrue(m.act("reject-damaged-unit",20));assertTrue(m.evacuationOnly);replay(m)
        finishExit(m,30)
        assertTrue(m.measurements.isEmpty());assertFalse(m.events.any {it.accepted&&it.id=="pin-drag"})
        val evidence=m.toJson().getJSONObject("scenario")
        assertTrue(evidence.getBoolean("damagedUnitRejected"));assertFalse(evidence.getBoolean("pressureChecked"))
        assertFalse(evidence.getBoolean("hoseChecked"));assertEquals("scenario-authored",evidence.getString("conditionSource"))
    }

    @Test fun evacuationRemainsAvailableAtEveryInspectionWithoutFalselyRecordingAnInspection() {
        for(target in listOf("EQUIPMENT","LABEL_CHECK","CONDITION_CHECK","HOSE_CHECK","DAMAGED_UNIT")) {
            val m=scenario(if(target=="DAMAGED_UNIT")FireRoomScenarioCatalog.DAMAGED_ID else FireRoomScenarioCatalog.DEFAULT_ID)
            m.act("alarm",1);m.act("select-clear-exit",2)
            if(target!="EQUIPMENT")m.act("select-suitable-extinguisher",3)
            if(target in setOf("CONDITION_CHECK","HOSE_CHECK","DAMAGED_UNIT"))m.act("confirm-label-match",4)
            if(target=="HOSE_CHECK")m.act("confirm-pressure-range",5)
            assertEquals(target,m.phase);assertTrue(m.act("choose-evacuation",10));replay(m)
            assertTrue(m.evacuationOnly);assertTrue(m.measurements.isEmpty());finishExit(m,20)
            assertFalse(m.toJson().getJSONObject("scenario").getBoolean("hoseChecked"))
            assertFalse(m.toJson().getJSONObject("scenario").getBoolean("damagedUnitRejected"))
        }
    }

    @Test fun announcedExplosionBlocksNewEquipmentJourneyBeforeInspections() {
        for(s in FireRoomScenarioCatalog.all) {
            val m=scenario(s.id,true);m.act("alarm",1);m.act("select-clear-exit",2)
            assertFalse(m.act("select-suitable-extinguisher",3));assertEquals("unsafe-control",m.events.last().reason)
            assertFalse(m.act("confirm-label-match",4));assertFalse(m.act("pin-drag",5))
            assertTrue(m.act("choose-evacuation",6));finishExit(m,20)
            assertFalse(m.toJson().getJSONObject("scenario").getBoolean("labelChecked"));assertTrue(m.measurements.isEmpty())
        }
    }

    @Test fun newScenarioMetadataAndOutcomeCannotBeAlteredDuringRestore() {
        val m=scenario();setup(m);m.act("confirm-label-match",4)
        val changes=listOf<(JSONObject)->Unit>(
            {it.getJSONObject("scenario").put("fireScenarioId",FireRoomScenarioCatalog.DAMAGED_ID)},
            {it.getJSONObject("scenario").put("fireDefinitionVersion",2)},
            {it.getJSONObject("scenario").put("equipmentCondition","damaged")},
            {it.getJSONObject("scenario").put("pressureChecked",true)},
            {it.getJSONObject("scenario").put("effectiveExitStation",0)},
            {it.getJSONObject("scenario").put("routeChanged",true)},
            {it.getJSONObject("scenario").put("realAuthorisationGranted",true)},
            {it.getJSONObject("scenario").remove("fireScenarioId")},
            {it.getJSONObject("result").put("certifiable",true)},
            {it.getJSONObject("scenario").put("fireScenarioId","unknown")}
        )
        for(change in changes) {
            val json=JSONObject(m.toJson().toString());change(json)
            assertThrows(IllegalArgumentException::class.java){RoomMission.restore(json)}
        }
        assertThrows(IllegalArgumentException::class.java){RoomMission("gas",false,FireRoomScenarioCatalog.DEFAULT_ID)}
        assertThrows(IllegalArgumentException::class.java){RoomMission("fire",false,"unknown")}
    }

    @Test fun newPhasesHaveBilingualChoicesConsequencesWhyAndSourceWithoutChangingLegacy() {
        val phases=setOf("LABEL_CHECK","CONDITION_CHECK","HOSE_CHECK","DAMAGED_UNIT","ROUTE_RECHECK","FIRE_HANDOVER")
        for(phase in phases) {
            val choices=RoomMissionChoices.forPhase(phase)
            assertEquals(3,choices.size);assertEquals(choices.size,choices.map{it.action}.toSet().size)
            choices.forEach { assertTrue(it.station in 0..2);assertTrue(it.hi.any{c->c in '\u0900'..'\u097f'});assertNotEquals(it.en,it.hi) }
            assertNotEquals(RoomMissionChoices.instruction(phase,false),RoomMissionChoices.instruction(phase,true))
            assertTrue(RoomMissionChoices.instruction(phase,true)!!.any{it in '\u0900'..'\u097f'})
            val lesson=RoomLearning.forPhase(phase);assertNotEquals(lesson.why.en,lesson.why.hi)
            assertTrue(RoomLearning.sources(phase).all{it.startsWith("https://")});assertTrue(RoomLearning.sources(phase).isNotEmpty())
            assertTrue(RoomLearning.doNot(phase).hi.isNotBlank())
        }
        assertEquals(2,FireRoomScenarioCatalog.all.size)
        FireRoomScenarioCatalog.all.forEach { assertEquals("draft",it.reviewStatus);assertNotEquals(it.title(false),it.title(true));assertEquals(1,it.definitionVersion) }
        val legacy=RoomMission("fire");legacy.act("alarm",1);legacy.act("select-clear-exit",2)
        legacy.act("select-suitable-extinguisher",3);assertEquals("PIN",legacy.phase)
        val json=legacy.toJson();assertEquals(1,json.getJSONObject("scenario").getInt("catalogVersion"))
        assertFalse(json.getJSONObject("scenario").has("fireScenarioId"));replay(legacy)
        assertEquals("EVACUATE",RoomMission("fire").apply{act("alarm",1);act("select-clear-exit",2);act("choose-evacuation",3)}.phase)
    }

    @Test fun forkCueAndReviewKeepNewPhaseEvidenceWithoutChangingLegacyCoachingFormat() {
        val m=scenario();setup(m);val fork=m.fork()
        assertEquals(m.toJson().toString(),fork.toJson().toString());assertEquals(m.fireScenarioId,fork.fireScenarioId)
        val coaching=RoomCoaching("fire",true)
        assertTrue(coaching.requestCue("LABEL_CHECK",1));assertFalse(RoomCoaching("gas",true).requestCue("LABEL_CHECK",1))
        fork.act("use-colour-only",4);fork.act("confirm-label-match",5)
        assertEquals("LABEL_CHECK",m.phase);assertEquals("CONDITION_CHECK",fork.phase)
        val review=RoomLearning.review(fork,coaching).single{it.phase=="LABEL_CHECK"}
        assertEquals(1,review.corrections);assertEquals(1,review.hints);assertTrue(review.needsPractice)
        assertEquals(coaching.toJson().toString(),RoomCoaching.restore(coaching.toJson(),"fire").toJson().toString())
        replay(fork)
    }
}
