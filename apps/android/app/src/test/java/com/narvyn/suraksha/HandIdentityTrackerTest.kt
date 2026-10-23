package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HandIdentityTrackerTest {
    private fun hand(x:Float,y:Float,label:Handedness=Handedness.RIGHT)=TrackedHand("candidate",label,.95f,List(21){HandLandmark(x,y)})
    @Test fun resultOrderCannotSwapHandIdentity() {
        val tracker=HandIdentityTracker()
        val a=tracker.associate(listOf(hand(.2f,.4f,Handedness.LEFT),hand(.8f,.4f)),1).first
        val b=tracker.associate(listOf(hand(.79f,.41f),hand(.21f,.41f,Handedness.LEFT)),1)
        assertTrue(b.second);assertEquals(a[0].id,b.first[1].id);assertEquals(a[1].id,b.first[0].id)
    }
    @Test fun overlapFailsClosedAndEpochNeverReusesIdentifiers() {
        val tracker=HandIdentityTracker()
        val first=tracker.associate(listOf(hand(.5f,.5f)),1).first.single().id
        assertFalse(tracker.associate(listOf(hand(.5f,.5f),hand(.51f,.5f,Handedness.LEFT)),1).second)
        assertNotEquals(first,tracker.associate(listOf(hand(.5f,.5f)),2).first.single().id)
    }
    @Test fun distinctSecondHandCanJoinWithoutCancellingHeldPrimary() {
        val tracker=HandIdentityTracker()
        val primary=tracker.associate(listOf(hand(.2f,.4f,Handedness.LEFT)),1).first.single().id
        val joined=tracker.associate(listOf(hand(.21f,.41f,Handedness.LEFT),hand(.8f,.4f)),1)
        assertTrue(joined.second);assertEquals(primary,joined.first.first().id)
        assertNotEquals(primary,joined.first.last().id)
    }
    @Test fun largeJumpHandednessFlipAndReappearanceRequireNeutralAgain() {
        val tracker=HandIdentityTracker()
        tracker.associate(listOf(hand(.2f,.5f)),1)
        assertFalse(tracker.associate(listOf(hand(.8f,.5f)),1).second)
        assertFalse(tracker.associate(listOf(hand(.8f,.5f,Handedness.LEFT)),1).second)
        tracker.associate(emptyList(),1)
        val a=tracker.associate(listOf(hand(.8f,.5f)),1).first.single().id
        tracker.associate(emptyList(),1)
        assertNotEquals(a,tracker.associate(listOf(hand(.8f,.5f)),1).first.single().id)
    }
}
