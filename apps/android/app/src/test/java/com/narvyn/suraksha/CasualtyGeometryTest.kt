package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class CasualtyGeometryTest {
    private val body=floatArrayOf(-.33f,0f,-.15f,.33f,1.72f,.15f)
    @Test fun lyingModelLiesAboveFloorAndPreservesLength(){
        for(roll in listOf(0f,90f,-90f)){
            val b=CasualtyGeometry.bounds(body,roll,CasualtyGeometry.lift(body,roll))
            assertEquals(.02f,b[1],.0001f);assertEquals(1.72f,b[5]-b[2],.0001f)
            assertTrue(b.all{it.isFinite()})
        }
    }
    @Test fun anatomicalSidesStayConsistentWhenLying(){
        val right=CasualtyGeometry.point(floatArrayOf(-.26f,1f,.08f),0f,.17f)
        assertTrue(right[0]<0);assertEquals(-1f,right[2],.0001f)
        val side=CasualtyGeometry.point(floatArrayOf(-.26f,1f,.08f),90f,.35f)
        assertEquals(.08f,side[0],.0001f);assertEquals(.61f,side[1],.0001f)
    }
    @org.junit.Test fun cueCentreIsTouchableButNearbyBodyIsNotACue() {
        val c=CasualtyMarkers.centres.getValue("RIGHT_FOREARM")
        org.junit.Assert.assertTrue(CasualtyMarkers.hit("RIGHT_FOREARM",floatArrayOf(c[0],c[1],c[2]+1f),floatArrayOf(0f,0f,-1f)))
        org.junit.Assert.assertFalse(CasualtyMarkers.hit("RIGHT_FOREARM",floatArrayOf(c[0]+.2f,c[1],c[2]+1f),floatArrayOf(0f,0f,-1f)))
        org.junit.Assert.assertFalse(CasualtyMarkers.hit("RIGHT_FOREARM",floatArrayOf(c[0],c[1],c[2]+1f),floatArrayOf(0f,0f,1f)))
    }
}
