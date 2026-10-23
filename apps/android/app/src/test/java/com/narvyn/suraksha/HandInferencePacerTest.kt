package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HandInferencePacerTest {
    @Test fun warmFastInferenceCanOfferTwentyFramesPerSecondWithoutAQueue() {
        val p=HandInferencePacer();assertTrue(p.offer(1000));p.completed(20)
        assertFalse(p.offer(1049));assertTrue(p.offer(1050));assertFalse(p.offer(1050))
    }
    @Test fun slowInferenceAutomaticallyLeavesCpuHeadroom() {
        val p=HandInferencePacer();p.completed(100)
        assertTrue(p.intervalMs>=140)
        assertTrue(p.offer(1000));assertFalse(p.offer(1100));assertTrue(p.offer(1160))
    }
    @Test fun cadenceRecoversWhenTheDeviceBecomesFaster() {
        val p=HandInferencePacer();repeat(5){p.completed(100)}
        repeat(30){p.completed(15)}
        assertEquals(50L,p.intervalMs)
        p.reset();assertTrue(p.offer(500))
        assertFalse(p.offer(499));assertFalse(p.offer(-1))
    }
}
