package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HandResultQualityTest {
    private val points=List(21){HandLandmark(.4f,.5f)}
    @Test fun uncertainLeftRightClassificationDoesNotDiscardAnAcceptedLandmarkDetection() {
        val hand=HandResultMapping.acceptedHand("hand","Left",.51f,points)
        assertNotNull(hand);assertEquals(Handedness.UNKNOWN,hand!!.handedness)
    }
    @Test fun handednessProbabilityIsNeverPresentedAsLandmarkTrackingConfidence() {
        val hand=HandResultMapping.acceptedHand("hand","Right",.97f,points)!!
        assertEquals(Handedness.RIGHT,hand.handedness)
        assertEquals(HandGestureController.MIN_CONFIDENCE,hand.confidence,0f)
    }
    @Test fun malformedClassifierResultsCannotCreateAHand() {
        for(value in listOf(Float.NaN,Float.POSITIVE_INFINITY,-.1f,1.1f))
            assertNull(HandResultMapping.acceptedHand("hand","Left",value,points))
    }
}
