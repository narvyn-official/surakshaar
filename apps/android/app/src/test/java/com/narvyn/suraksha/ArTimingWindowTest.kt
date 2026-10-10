package com.narvyn.suraksha
import org.junit.Assert.*
import org.junit.Test
class ArTimingWindowTest {
    @Test fun nearestRankAndFixedWindow(){val m=ArTimingWindow(100);for(i in 1..200)m.record(i*1_000_000L,i*33_000_000L);assertEquals(100,m.update().samples);assertEquals(150.0,m.update().p50Ms,0.0);assertEquals(195.0,m.update().p95Ms,0.0);assertEquals(199.0,m.update().p99Ms,0.0);assertEquals(33.0,m.cameraInterval().p95Ms,0.0)}
    @Test fun duplicatesAndResumeDoNotInventCadence(){val m=ArTimingWindow();m.record(1,100);m.record(2,100);assertEquals(0,m.cameraInterval().samples);m.record(3,200);assertEquals(1,m.cameraInterval().samples);m.clear();m.record(3,1000);assertEquals(0,m.cameraInterval().samples);assertEquals(1,m.update().samples)}
}
