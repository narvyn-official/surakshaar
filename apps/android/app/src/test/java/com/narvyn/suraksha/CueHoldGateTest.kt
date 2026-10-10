package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class CueHoldGateTest {
    @Test fun emptySpaceNeverAccumulatesAndFreshIntersectionStartsItsOwnDwell(){
        val gate=CueHoldGate()
        for(t in 0L..2000L step 100L)assertEquals(0f,gate.sample(1,false,t),0f)
        assertEquals(0f,gate.sample(1,true,2100),0f)
        for(t in 2200L..3500L step 100L)assertTrue(gate.sample(1,true,t)<1f)
        assertEquals(1f,gate.sample(1,true,3600),0f)
    }
    @Test fun lossOfTargetFrameGapAndNewGestureCannotInheritTime(){
        val gate=CueHoldGate()
        for(t in 0L..1400L step 100L)gate.sample(1,true,t)
        assertEquals(0f,gate.sample(1,false,1500),0f)
        assertEquals(0f,gate.sample(1,true,1600),0f)
        assertEquals(0f,gate.sample(1,true,1900),0f)
        assertTrue(gate.sample(1,true,2000)>0f)
        assertEquals(0f,gate.sample(2,true,2100),0f)
        assertEquals(0f,gate.sample(2,true,2000),0f)
        gate.reset();assertEquals(0f,gate.sample(2,true,2100),0f)
    }
}
