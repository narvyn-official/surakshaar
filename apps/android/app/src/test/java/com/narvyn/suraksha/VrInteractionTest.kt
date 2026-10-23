package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class VrInteractionTest {
    @Test fun gazeUsesWorldRayAndNearestVisibleCommandNotScreenHalfCoordinates(){
        val near=VrInteraction.Target("near",0f,0f,-2f,1f,1f);val far=near.copy(id="far",z=-4f)
        val ray=VrInteraction.Ray(floatArrayOf(0f,0f,0f),floatArrayOf(0f,0f,-1f))
        assertEquals("near",VrInteraction.nearest(ray,listOf(far,near))?.id)
        assertNull(VrInteraction.nearest(ray.copy(direction=floatArrayOf(0f,0f,1f)),listOf(near)))
        assertNull(VrInteraction.nearest(ray.copy(direction=floatArrayOf(1f,0f,0f)),listOf(near)))
        assertNull(VrInteraction.nearest(ray.copy(origin=floatArrayOf(1f,0f,0f)),listOf(near)))
        assertNull(VrInteraction.nearest(ray.copy(origin=floatArrayOf(Float.NaN,0f,0f)),listOf(near)))
    }
    @Test fun aButtonUsesAFreshTargetOnceAndCannotConsumeAnOldFrame(){
        val g=VrGazeGate();g.frame("water",1000,true,false)
        assertEquals("water",g.trigger(1040));assertNull(g.trigger(1060))
        g.frame(null,1070,true,false);g.frame("water",1080,true,false)
        assertNull(g.trigger(1400));g.reset();assertNull(g.trigger(1401))
    }
    @Test fun dwellRequiresContinuousFramesAndLeavingResetsItsProgress(){
        val g=VrGazeGate(1600);assertNull(g.frame("water",0,true,true))
        for(t in 100L..1500L step 100)assertNull(g.frame("water",t,true,true))
        assertEquals("water",g.frame("water",1600,true,true))
        assertNull(g.frame("water",1700,true,true));assertNull(g.trigger(1701))
        g.frame(null,1800,true,true);g.frame("water",1900,true,true)
        assertNull(g.frame("water",2200,true,true));assertEquals(0f,g.fraction,0f)
    }
    @Test fun pauseAndClockRollbackCannotFinishASelection(){
        val g=VrGazeGate(1000)
        for(t in 0L..900L step 100)g.frame("exit",t,true,true)
        g.frame("exit",1000,false,true);assertNull(g.trigger(1001))
        g.frame("exit",1100,true,true);assertNull(g.frame("exit",1000,true,true));assertEquals(0f,g.fraction,0f)
        g.reset();assertNull(g.trigger(1001))
    }
    @Test fun aNewPageRequiresLookingAwayBeforeDwellCanStart(){
        val g=VrGazeGate(1000);g.reset(waitForExit=true)
        for(t in 0L..3000L step 100)assertNull(g.frame("new-choice",t,true,true))
        assertEquals(0f,g.fraction,0f)
        g.frame(null,3100,true,true)
        for(t in 3200L..4100L step 100)assertNull(g.frame("new-choice",t,true,true))
        assertEquals("new-choice",g.frame("new-choice",4200,true,true))
    }
    @Test fun textPaginationRetainsSafetyWordsAndUnicodeWithoutTruncating(){
        val text=("Never use water on this equipment. Do not remove stuck material. हिन्दी 👍 ").repeat(12).trim()
        val pages=VrTextPages.split(text,140)
        assertTrue(pages.size>1);assertTrue(pages.all{it.length<=140})
        assertEquals(text.split(Regex("\\s+")),pages.joinToString(" ").split(Regex("\\s+")))
        assertTrue(pages.none{it.endsWith('\uD83D')||it.startsWith('\uDC4D')})
        assertEquals(listOf(""),VrTextPages.split(""))
    }
}
