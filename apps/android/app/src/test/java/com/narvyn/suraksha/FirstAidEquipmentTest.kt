package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class FirstAidEquipmentTest {
    @Test fun powerControlIsTheNearestVisibleSurfaceFromTheFront() {
        val origin=floatArrayOf(.54f,1.02f,1f)
        val ray=floatArrayOf(0f,0f,-1f)
        val nearest=FirstAidEquipment.parts.mapNotNull { p -> AnatomyPicking.distance(p.vertices,p.bounds,origin,ray)?.let { p.id to it } }.minByOrNull{it.second}
        assertEquals(FirstAidEquipment.POWER,nearest?.first)
    }
    @Test fun equipmentDoesNotOverlapTheCentralCasualtyLandmarks() {
        val ray=floatArrayOf(0f,0f,-1f)
        assertTrue(FirstAidEquipment.parts.all { AnatomyPicking.distance(it.vertices,it.bounds,floatArrayOf(0f,1.25f,1f),ray)==null })
        assertTrue(FirstAidEquipment.parts.flatMap{it.vertices.toList()}.all{it.isFinite()})
    }
}
