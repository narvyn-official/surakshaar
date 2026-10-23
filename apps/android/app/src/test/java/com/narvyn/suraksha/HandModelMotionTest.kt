package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HandModelMotionTest {
    @Test fun movingBothHandsTogetherMovesTheObjectWithoutChangingItsScale() {
        val result=HandModelTransforms.twoHand(HandModelPose(.1f,.1f),.2f,.2f,.4f,.1f,1f,0f)
        assertEquals(.3f,result.x,.0001f);assertEquals(0f,result.z,.0001f);assertEquals(1f,result.scale,0f)
    }
    @Test fun scalingAndTurningPreservesTheOffCentreGripPivot() {
        val result=HandModelTransforms.twoHand(HandModelPose(.2f,0f,.5f),.1f,0f,.1f,.1f,2f,90f)
        assertEquals(.1f,result.x,.0001f);assertEquals(-.1f,result.z,.0001f)
        assertEquals(1f,result.scale,.0001f);assertEquals(90f,result.yaw,.0001f)
    }
    @Test fun clampedScaleUsesItsActualRatioForThePivot() {
        val result=HandModelTransforms.twoHand(HandModelPose(.2f,0f),.1f,0f,.1f,0f,8f,0f)
        assertEquals(1.5f,result.scale,.0001f);assertEquals(.25f,result.x,.0001f)
    }
    @Test fun invalidTransformDoesNotReplaceTheCurrentPlacement() {
        val before=HandModelPose(.2f,.1f,.7f,20f)
        for(bad in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f,0f))
            assertEquals(before,HandModelTransforms.twoHand(before,.1f,.2f,.2f,.2f,bad,0f))
    }
    @Test fun renderInterpolationIsContinuousAndFrameRateIndependent() {
        val start=HandModelPose();val target=HandModelPose(.5f,.4f,1.4f,90f)
        val slow=HandModelMotion(start);val fast=HandModelMotion(start)
        val first=fast.advance(target,.01f)
        assertTrue(first.x>0f&&first.x<target.x)
        repeat(9){fast.advance(target,.01f)};repeat(2){slow.advance(target,.05f)}
        assertEquals(slow.visible.x,fast.visible.x,.0001f)
        assertEquals(slow.visible.scale,fast.visible.scale,.0001f)
        assertEquals(slow.visible.yaw,fast.visible.yaw,.0001f)
        assertTrue(fast.visible.x>.45f)
    }
    @Test fun rotationTakesTheShortPathAcrossTheAngleBoundary() {
        val motion=HandModelMotion(HandModelPose(yaw=179f))
        val next=motion.advance(HandModelPose(yaw=-179f),.01f)
        assertTrue(next.yaw>179f||next.yaw< -179f)
    }
    @Test fun cancellationSnapsImmediatelyAndBadTimeDoesNotMoveTheModel() {
        val motion=HandModelMotion(HandModelPose(.1f,.2f))
        val before=motion.visible
        assertEquals(before,motion.advance(HandModelPose(.5f,.5f),Float.NaN))
        motion.snap(HandModelPose(.3f,.3f,.6f,20f));assertEquals(HandModelPose(.3f,.3f,.6f,20f),motion.visible)
    }
}
