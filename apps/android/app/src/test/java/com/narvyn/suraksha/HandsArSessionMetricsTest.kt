package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HandsArSessionMetricsTest {
    @Test fun timingIsBoundedAndSuspendedTimeIsExcluded(){
        val stats=HandsArSessionMetrics()
        repeat(200){stats.render(it*16L,true)}
        assertEquals(120,stats.frameSamples)
        assertEquals(16L,stats.frameP95Ms)
        stats.suspend();stats.render(80000,true)
        assertEquals(16L,stats.frameP95Ms)
    }
    @Test fun noCameraIdentifiersOrDuplicateInferenceInflateTiming(){
        val stats=HandsArSessionMetrics()
        val frame=HandTrackingFrame(1000,1020,1,emptyList())
        stats.hand(frame,1030);stats.hand(frame,1040)
        assertEquals(1,stats.handSamples);assertEquals(30L,stats.handAgeP95Ms)
        stats.hand(HandTrackingFrame(900,910,1,emptyList()),1300)
        assertEquals(1,stats.handSamples)
    }
    @Test fun onlyCommittedHandTransformsCountAsManipulationChecks(){
        val stats=HandsArSessionMetrics()
        stats.selected()
        assertEquals(1,stats.selections);assertFalse(stats.moved);assertFalse(stats.resized)
        // Cancel does not call committed. Selection alone cannot claim any completed action.
        stats.committed(HandModelPose(),HandModelPose(x=.1f,y=.2f,scale=1.3f,yaw=25f))
        assertTrue(stats.moved);assertTrue(stats.lifted);assertTrue(stats.resized);assertTrue(stats.rotated)
        assertFalse(stats.summary().contains("passed",ignoreCase=true))
        assertTrue(stats.summary().contains("not measured depth"))
    }
}
