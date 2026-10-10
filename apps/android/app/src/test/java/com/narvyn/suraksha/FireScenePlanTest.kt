package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import java.nio.FloatBuffer

class FireScenePlanTest {
    @Test fun everyCurrentJourneyPhaseHasAnExplicitPresentation() {
        val current=RoomMission.allPhases("fire")
        assertEquals(current,FireScenePlan.supportedPhases)
        for(scenario in FireRoomScenarioCatalog.all)for(phase in RoomMission.phases("fire",scenario.id)) {
            val plan=FireScenePlan.forPhase(phase,scenario.equipmentCondition)
            assertEquals(phase,plan.phase)
            assertTrue(plan.focusY in .2f..5f)
        }
    }
    @Test fun damagedAndExplosionVariantsCannotDepictDischarge() {
        for(phase in FireScenePlan.supportedPhases){
            val damaged=FireScenePlan.forPhase(phase,"damaged",held=true)
            assertTrue("fire-damaged" in damaged.equipmentFlags)
            assertFalse(damaged.equipmentFlags.any{it in setOf("fire-pin","fire-aim","fire-squeeze")})
            val explosion=FireScenePlan.forPhase(phase,explosionRisk=true,held=true)
            assertFalse("fire-squeeze" in explosion.equipmentFlags)
            assertTrue(explosion.smoke)
        }
    }
    @Test fun sceneTransitionsShowDifferentAuthoredObjectsWithoutFireOutClaim() {
        assertTrue(FireScenePlan.forPhase("ALARM").showAlarm)
        assertFalse(FireScenePlan.forPhase("PIN").showAlarm)
        assertTrue(FireScenePlan.forPhase("EQUIPMENT").showUnconfirmedUnit)
        assertFalse(FireScenePlan.forPhase("LABEL_CHECK").showUnconfirmedUnit)
        assertFalse(FireScenePlan.forPhase("SWEEP",held=false).equipmentFlags.contains("fire-squeeze"))
        assertTrue(FireScenePlan.forPhase("SWEEP",held=true).equipmentFlags.contains("fire-squeeze"))
        assertFalse(FireScenePlan.forPhase("EVACUATE").equipmentFlags.contains("fire-pin"))
        val changed=FireScenePlan.forPhase("ROUTE_RECHECK",routeChanged=true)
        assertEquals("blocked-exit",changed.exitKind);assertFalse(changed.showEquipment);assertTrue(changed.smoke)
        val finish=FireScenePlan.forPhase("COMPLETE",routeChanged=true)
        assertEquals("assembly",finish.exitKind);assertTrue(finish.showRollCall);assertTrue(finish.showHandover);assertTrue(finish.smoke)
    }
    @Test fun contextualModelsMapExactlyToExistingCasesAndDoNotInventResponseLessons() {
        assertEquals(FireResponseLearning.cases.map{it.id}.toSet(),FireScenePlan.contextProps.keys)
        assertEquals(7,FireScenePlan.contextProps.values.toSet().size)
        assertTrue(FireScenePlan.inspections.all{it.title!=it.titleHi&&it.detail!=it.detailHi&&it.detailHi.any{c->c in '\u0900'..'\u097f'}})
        for(id in listOf("cooking","metal","sand","liquid")){
            val view=FireScenePlan.viewForCase(id)
            assertTrue("$id must reveal distinguishing top details",view.pitch in 20f..40f)
            assertTrue("$id starts in a modest three-quarter view",view.yaw in -30f..-10f)
        }
        assertEquals(FireScenePlan.ViewPreset(),FireScenePlan.viewForCase(null))
    }
    @Test fun newProceduralMeshesAreFiniteCachedAndBounded() {
        val equipment=WorldEquipment()
        val method=WorldEquipment::class.java.getDeclaredMethod("mesh",String::class.java).apply{isAccessible=true}
        for(kind in FireScenePlan.propKinds){
            val mesh=method.invoke(equipment,kind) as FloatBuffer
            assertSame("Mesh must be cached rather than rebuilt per frame",mesh,method.invoke(equipment,kind))
            assertEquals(0,mesh.capacity()%36)
            assertTrue("Bounded original mesh: $kind",mesh.capacity()/36 in 1..25000)
            for(i in 0 until mesh.capacity() step 12){
                for(j in 0..11)assertTrue("$kind has nonfinite vertex data",mesh.get(i+j).isFinite())
                for(j in 0..2)assertTrue("$kind exceeds the training prop envelope",kotlin.math.abs(mesh.get(i+j))<=1.2f)
                val n=(3..5).sumOf{mesh.get(i+it).toDouble()*mesh.get(i+it)}
                assertEquals("$kind normals must be unit length",1.0,n,.002)
            }
        }
    }
}
