package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HandDragPlaneTest {
    @Test fun aModelAtEyeLevelCanBeGrabbedAndMoved() {
        val eye=floatArrayOf(0f,1.4f,0f);val ray=floatArrayOf(0f,0f,-1f);val hit=floatArrayOf(0f,1.4f,-1f)
        val plane=HandDragPlane.atGrip(hit,ray)
        assertArrayEquals(hit,plane.intersect(eye,ray),.0001f)
        val moved=plane.intersect(eye,floatArrayOf(.1f,.1f,-.9899495f))
        assertNotNull(moved);assertTrue(moved!![0]>.05f);assertTrue(moved[2]< -1f)
    }
    @Test fun aTableModelAboveTheCameraDoesNotRequireARayDownToTheFloor() {
        val eye=floatArrayOf(0f,1.4f,0f);val ray=floatArrayOf(0f,.6f,-.8f);val hit=floatArrayOf(0f,2f,-.8f)
        val plane=HandDragPlane.atGrip(hit,ray)
        assertArrayEquals(hit,plane.intersect(eye,ray),.0001f)
    }
    @Test fun lookingDownKeepsTheOrdinaryHorizontalSurfaceMapping() {
        val eye=floatArrayOf(0f,1.5f,0f);val hit=floatArrayOf(0f,0f,0f)
        val plane=HandDragPlane.atGrip(hit,floatArrayOf(0f,-1f,0f))
        assertArrayEquals(hit,plane.intersect(eye,floatArrayOf(0f,-1f,0f)),.0001f)
    }
    @Test fun behindCameraFarAndMalformedRaysCannotMoveTheObject() {
        val eye=floatArrayOf(0f,1.4f,0f);val ray=floatArrayOf(0f,0f,-1f)
        val plane=HandDragPlane.atGrip(floatArrayOf(0f,1.4f,-1f),ray)
        assertNull(plane.intersect(eye,floatArrayOf(0f,0f,1f)))
        assertNull(plane.intersect(eye,floatArrayOf(Float.NaN,0f,-1f)))
        assertNull(plane.intersect(floatArrayOf(0f,1.4f,10f),ray))
    }
}
