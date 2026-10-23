package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class HandPointerFilterTest {
    @Test fun steadyHandsHaveLessJitterWithoutDeadeningDeliberateMovement() {
        val filter=HandPointerFilter();filter.sample(.4f,.4f,1000,1f)
        var time=1000L
        repeat(40){time+=50;val p=filter.sample(.4f+if(it%2==0).004f else -.004f,.4f,time,1f)
            if(it>15)assertTrue("Steady-hand pointer jitter",abs(p.first-.4f)<.0014f)}
        var raw=.4f;var point=.4f
        repeat(10){time+=50;raw+=.025f;point=filter.sample(raw,.4f,time,1f).first}
        assertTrue("Intentional movement must not trail by more than 1.2% of view height",raw-point<.012f)
    }
    @Test fun replayCannotMoveThePointerAndDisplayAspectPreservesTheSameMotion() {
        val narrow=HandPointerFilter();val wide=HandPointerFilter()
        for(i in 0..12){
            val x=.2f+i*.02f;val a=narrow.sample(x,.3f,1000+i*50L,1f)
            val b=wide.sample(x/2,.3f,1000+i*50L,2f)
            assertEquals(a.first,b.first*2,.0001f)
        }
        val before=narrow.sample(.44f,.3f,1600,1f)
        assertEquals(before,narrow.sample(.9f,.3f,1500,1f))
    }
}
