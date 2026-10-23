package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class HandGestureControllerTest {
    private class Harness {
        val controller = HandGestureController()
        var time = 1_000L
        var epoch = 1L
        var aspect = 1f
        var target: String? = "model"
        fun frame(vararg hands: TrackedHand, delta: Long = 50L, stable: Boolean = true): List<HandGestureEvent> {
            time += delta
            return controller.process(HandTrackingFrame(time, time, epoch, hands.toList(), stable, aspect), time) { _, _ -> target }
        }
        fun arm(vararg hands: TrackedHand) { repeat(7) { frame(*hands) } }
        fun pinch(vararg hands: TrackedHand): List<HandGestureEvent> = (1..3).flatMap { frame(*hands) }
        fun start(x: Float = .45f): HandGestureEvent.Start {
            arm(hand(x = x)); return pinch(hand(x = x, ratio = .1f)).filterIsInstance<HandGestureEvent.Start>().single()
        }
    }
    companion object {
        /** A labelled synthetic 21-landmark hand, not a recorded/provider result. */
        private fun hand(id: String = "left-1", x: Float = .45f, y: Float = .4f, ratio: Float = .8f, open: Boolean = false,
                         confidence: Float = .95f, size: Float = .2f): TrackedHand {
            val p = MutableList(21) { HandLandmark(x, y + size * .75f) }
            p[0] = HandLandmark(x, y + size * 1.75f)
            val base = listOf(5 to -.4f, 9 to -.1f, 13 to .15f, 17 to .4f)
            for ((i, offset) in base) {
                p[i] = HandLandmark(x + offset * size, y + size * .75f)
                p[i + 1] = HandLandmark(x + offset * size, y + size * .40f)
                p[i + 2] = HandLandmark(x + offset * size, y + size * .50f)
                p[i + 3] = HandLandmark(x + offset * size, if (open) y - size * .15f else y + size * .65f)
            }
            p[8] = HandLandmark(x, y)
            p[4] = HandLandmark(if (open) x - size * 1.25f else x + size * ratio, if (open) y + size * .60f else y)
            return TrackedHand(id, if (id.startsWith("right")) Handedness.RIGHT else Handedness.LEFT, confidence, p)
        }
    }

    @Test fun startupPinchCannotSelectAndAVisibleNeutralHandArmsOnce() {
        val h = Harness()
        repeat(30) { assertTrue(h.frame(hand(ratio = .1f)).none { it is HandGestureEvent.Start }) }
        h.arm(hand())
        val selected = h.pinch(hand(ratio = .1f)).filterIsInstance<HandGestureEvent.Start>()
        assertEquals(1, selected.size)
        repeat(20) { assertTrue(h.frame(hand(ratio = .1f)).none { it is HandGestureEvent.Start }) }
    }

    @Test fun aTargetIsLockedAtPinchOnsetAndReleaseCommitsOnlyOnce() {
        val h = Harness(); assertEquals("model", h.start().targetId)
        h.target = "different"
        val moved = h.frame(hand(x = .52f, ratio = .1f)).filterIsInstance<HandGestureEvent.Move>().single()
        assertEquals("model", moved.targetId)
        assertTrue(moved.x in .45f.. .52f); assertTrue(abs(moved.deltaX) <= .075f)
        val release = (1..4).flatMap { h.frame(hand(x = .52f)) }
        assertEquals(1, release.filterIsInstance<HandGestureEvent.End>().size)
        assertTrue(release.filterIsInstance<HandGestureEvent.End>().single().dragged)
        assertTrue(release.none { it is HandGestureEvent.Cancel })
        repeat(10) { assertTrue(h.frame(hand()).none { it is HandGestureEvent.End || it is HandGestureEvent.Start }) }
    }

    @Test fun aPinchReleaseWithoutMovementIsAnExplicitTapCandidate() {
        val h = Harness(); h.start()
        val ended = (1..4).flatMap { h.frame(hand()) }.filterIsInstance<HandGestureEvent.End>().single()
        assertFalse(ended.dragged)
    }

    @Test fun pinchingEmptySpaceCannotAcquireATargetWhileStillHeld() {
        val h = Harness(); h.target = null; h.arm(hand())
        assertTrue(h.pinch(hand(ratio = .1f)).none { it is HandGestureEvent.Start })
        h.target = "model"
        repeat(10) { assertTrue(h.frame(hand(ratio = .1f)).none { it is HandGestureEvent.Start }) }
        h.arm(hand())
        assertEquals(1, h.pinch(hand(ratio = .1f)).filterIsInstance<HandGestureEvent.Start>().size)
    }

    @Test fun pinchHysteresisAndDwellRejectThresholdJitter() {
        val h = Harness(); h.arm(hand())
        repeat(10) {
            assertTrue(h.frame(hand(ratio = .20f)).none { it is HandGestureEvent.Start })
            assertTrue(h.frame(hand(ratio = .42f)).none { it is HandGestureEvent.Start })
        }
        assertEquals(1, h.pinch(hand(ratio = .15f)).filterIsInstance<HandGestureEvent.Start>().size)
        repeat(8) { assertTrue(h.frame(hand(ratio = .43f)).none { it is HandGestureEvent.End }) }
        repeat(5) {
            assertTrue(h.frame(hand(ratio = .75f)).none { it is HandGestureEvent.End })
            assertTrue(h.frame(hand(ratio = .35f)).none { it is HandGestureEvent.End })
        }
    }

    @Test fun staleCaptureCannotActEvenIfTheProviderJustReturnedIt() {
        val h = Harness(); h.start(); h.time += 300
        val events = h.controller.process(HandTrackingFrame(h.time - 300, h.time, h.epoch, listOf(hand(ratio = .1f))), h.time) { _, _ -> "model" }
        assertEquals(HandCancelReason.STALE_FRAME, events.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(h.controller.tick(h.time + 100).isEmpty())
        assertTrue(h.pinch(hand(ratio = .1f)).none { it is HandGestureEvent.Start })
    }

    @Test fun missingResultsTimeOutAndReplayedFramesCannotFinishAPinch() {
        val h = Harness(); h.arm(hand()); h.frame(hand(ratio = .1f))
        val duplicate = HandTrackingFrame(h.time, h.time, h.epoch, listOf(hand(ratio = .1f)))
        repeat(6) { assertTrue(h.controller.process(duplicate, h.time + 20) { _, _ -> "model" }.isEmpty()) }
        h.time += 20
        assertTrue(h.frame(hand(ratio = .1f), delta = 10).none { it is HandGestureEvent.Start })
        h.frame(hand(ratio = .1f), delta = 50)
        assertEquals(HandCancelReason.STALE_FRAME, h.controller.tick(h.time + 251).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
    }

    @Test fun orientationLossAmbiguityAndConfidenceChangesCancelAndRequireRearming() {
        val h = Harness(); h.start(); h.epoch++
        assertEquals(HandCancelReason.ORIENTATION_CHANGED, h.frame(hand(ratio = .1f)).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(h.pinch(hand(ratio = .1f)).none { it is HandGestureEvent.Start })
        h.start()
        assertEquals(HandCancelReason.IDENTITY_AMBIGUOUS, h.frame(hand(ratio = .1f), stable = false).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        h.start()
        assertEquals(HandCancelReason.LOW_CONFIDENCE, h.frame(hand(ratio = .1f, confidence = .3f)).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        h.start()
        assertEquals(HandCancelReason.LOST_TRACKING, h.frame().filterIsInstance<HandGestureEvent.Cancel>().single().reason)
    }

    @Test fun aDifferentIdOrSuddenSameIdWristJumpCannotTakeOverAGrab() {
        val h = Harness(); h.start()
        assertEquals(HandCancelReason.IDENTITY_CHANGED, h.frame(hand(id = "right-2", ratio = .1f)).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        h.start(x = .25f)
        assertEquals(HandCancelReason.IDENTITY_CHANGED, h.frame(hand(x = .65f, ratio = .1f)).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
    }

    @Test fun openPalmIsDeliberateCancellationAndDoesNotGenerateAnEnd() {
        val h = Harness(); h.start()
        val events = (1..5).flatMap { h.frame(hand(open = true)) }
        assertEquals(HandCancelReason.OPEN_PALM, events.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(events.none { it is HandGestureEvent.End })
        assertTrue(h.pinch(hand(ratio = .1f)).none { it is HandGestureEvent.Start })
    }

    @Test fun aSecondHandTransformsTheLockedObjectAndReleaseReturnsToPrimaryDrag() {
        val h = Harness()
        h.arm(hand(x = .25f), hand(id = "right-2", x = .70f))
        val start = h.pinch(hand(x = .25f, ratio = .1f), hand(id = "right-2", x = .70f)).filterIsInstance<HandGestureEvent.Start>().single()
        h.target = "unrelated"
        val transformStart = h.pinch(hand(x = .25f, ratio = .1f), hand(id = "right-2", x = .70f, ratio = .1f)).filterIsInstance<HandGestureEvent.TransformStart>().single()
        assertEquals(start.targetId, transformStart.targetId)
        var transformed = emptyList<HandGestureEvent>()
        repeat(12) { transformed = h.frame(hand(x = .16f, ratio = .1f), hand(id = "right-2", x = .83f, y = .35f, ratio = .1f)) }
        val move = transformed.filterIsInstance<HandGestureEvent.TransformMove>().single()
        assertEquals("model", move.targetId); assertTrue(move.scale > 1f); assertTrue(move.rotationRadians < 0f)
        val release = (1..4).flatMap { h.frame(hand(x = .16f, ratio = .1f), hand(id = "right-2", x = .83f, y = .35f)) }
        assertEquals(1, release.filterIsInstance<HandGestureEvent.TransformEnd>().size)
        assertTrue(release.none { it is HandGestureEvent.End || it is HandGestureEvent.Cancel })
        assertEquals(1, h.frame(hand(x = .20f, ratio = .1f), hand(id = "right-2", x = .83f, y = .35f)).filterIsInstance<HandGestureEvent.Move>().size)
    }

    @Test fun twoHandTransformValuesAreFiniteBoundedAndCancelledOnInterruption() {
        val h = Harness()
        h.arm(hand(x = .45f), hand(id = "right-2", x = .56f))
        h.pinch(hand(x = .45f, ratio = .1f), hand(id = "right-2", x = .56f, ratio = .1f))
        val values = mutableListOf<HandGestureEvent.TransformMove>()
        repeat(20) { values += h.frame(hand(x = .20f, ratio = .1f), hand(id = "right-2", x = .80f, ratio = .1f)).filterIsInstance<HandGestureEvent.TransformMove>() }
        assertTrue(values.isNotEmpty())
        assertTrue(values.all { it.scale.isFinite() && it.scale in .5f..2f && it.rotationRadians.isFinite() && abs(it.rotationRadians) <= HandGestureController.MAX_ROTATION && it.centreX in 0f..1f && it.centreY in 0f..1f })
        assertEquals(2f, values.last().scale, .02f)
        val cancelled = h.controller.interrupt()
        assertTrue(cancelled.filterIsInstance<HandGestureEvent.TransformEnd>().single().cancelled)
        assertEquals(HandCancelReason.INTERRUPTED, cancelled.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(h.controller.interrupt().isEmpty())
    }

    @Test fun malformedInputsCannotEmitActionsAndSnapshotsDoNotRetainMutableLists() {
        val h = Harness(); h.start()
        val malformed = TrackedHand("left-1", Handedness.LEFT, .9f, listOf(HandLandmark(Float.NaN, .4f)))
        assertEquals(HandCancelReason.INVALID_FRAME, h.frame(malformed).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        val points = hand().landmarks.toMutableList()
        val snapshot = TrackedHand("copy", Handedness.LEFT, .9f, points)
        val hands = mutableListOf(snapshot)
        val frame = HandTrackingFrame(0, 0, 1, hands)
        points.clear(); hands.clear()
        assertEquals(21, snapshot.landmarks.size); assertEquals(1, frame.hands.size)
    }

    @Test fun palmNormalizationKeepsPinchThresholdsIndependentOfHandSize() {
        for (size in listOf(.08f, .15f, .3f)) {
            val h = Harness(); h.arm(hand(size = size))
            assertEquals(1, h.pinch(hand(size = size, ratio = .10f)).filterIsInstance<HandGestureEvent.Start>().size)
        }
    }

    @Test fun clockRollbackAndInvalidFutureResultsCancelWithoutAnAction() {
        val h = Harness(); h.start()
        val rollback = h.controller.tick(h.time - 1)
        assertEquals(HandCancelReason.CLOCK_ROLLBACK, rollback.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        h.time += 50; h.start()
        val future = HandTrackingFrame(h.time + 5, h.time + 5, h.epoch, listOf(hand()))
        val events = h.controller.process(future, h.time) { _, _ -> "model" }
        assertEquals(HandCancelReason.INVALID_FRAME, events.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(events.none { it is HandGestureEvent.End || it is HandGestureEvent.Start })
    }

    @Test fun displayAspectCorrectionPreservesTheSamePhysicalPinchAndGeometryChangesCancel() {
        for (aspect in listOf(.5f, 1f, 2.2f)) {
            val h = Harness(); h.aspect = aspect
            fun mapped(ratio: Float): TrackedHand {
                val input = hand(x = .5f, size = .15f, ratio = ratio)
                return TrackedHand(input.id, input.handedness, input.confidence,
                    input.landmarks.map { it.copy(x = .5f + (it.x - .5f) / aspect) })
            }
            h.arm(mapped(.8f))
            assertEquals(1, h.pinch(mapped(.1f)).filterIsInstance<HandGestureEvent.Start>().size)
            h.aspect = if (aspect == .5f) 1f else .5f
            assertEquals(HandCancelReason.ORIENTATION_CHANGED,
                h.frame(mapped(.1f)).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        }
    }

    @Test fun handednessCannotSwitchUnderAnExistingStableId() {
        val h = Harness(); h.start()
        val input = hand(ratio = .1f)
        val swapped = TrackedHand(input.id, Handedness.RIGHT, input.confidence, input.landmarks)
        assertEquals(HandCancelReason.IDENTITY_CHANGED,
            h.frame(swapped).filterIsInstance<HandGestureEvent.Cancel>().single().reason)
    }

    @Test fun lossOfEitherTransformHandCancelsTheWholeUncommittedTransform() {
        val h = Harness()
        h.arm(hand(x = .25f), hand(id = "right-2", x = .70f))
        h.pinch(hand(x = .25f, ratio = .1f), hand(id = "right-2", x = .70f, ratio = .1f))
        val events = h.frame(hand(x = .25f, ratio = .1f))
        assertTrue(events.filterIsInstance<HandGestureEvent.TransformEnd>().single().cancelled)
        assertEquals(HandCancelReason.IDENTITY_CHANGED, events.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(events.none { it is HandGestureEvent.End || it is HandGestureEvent.Start })
    }


    @Test fun confidenceAboveOneIsMalformedAndCannotArmOrContinueAGrab() {
        val h = Harness()
        repeat(8) {
            assertTrue(h.frame(hand(confidence = 1.1f)).none { it is HandGestureEvent.Start })
        }
        assertTrue(h.pinch(hand(ratio = .1f)).none { it is HandGestureEvent.Start })
        h.start()
        val rejected = h.frame(hand(ratio = .1f, confidence = 1.0001f))
        assertEquals(HandCancelReason.INVALID_FRAME,
            rejected.filterIsInstance<HandGestureEvent.Cancel>().single().reason)
        assertTrue(rejected.none { it is HandGestureEvent.Start || it is HandGestureEvent.End })
    }

    @Test fun releaseOpeningCannotMoveTheLastHeldPosition() {
        val h = Harness(); h.start()
        var heldX = .45f
        repeat(8) { h.frame(hand(x=.50f, ratio=.1f)).filterIsInstance<HandGestureEvent.Move>().lastOrNull()?.let{heldX=it.x} }
        val released=(1..4).flatMap { h.frame(hand(x=.62f)) }.filterIsInstance<HandGestureEvent.End>().single()
        assertEquals("Opening the fingers must commit the last held position, not drag the object",heldX,released.x,.0001f)
    }

    @Test fun targetIsLatchedAtTheFirstDeliberatePinchSample() {
        val h = Harness();h.arm(hand());h.target="original"
        h.frame(hand(ratio=.1f))
        h.target="neighbour"
        val events=h.pinch(hand(ratio=.1f))
        assertEquals("original",events.filterIsInstance<HandGestureEvent.Start>().single().targetId)
    }

    @Test fun turningBothHandsPastHalfATurnNeverReversesDirection() {
        val h=Harness()
        fun pair(degrees:Double,pinched:Boolean):Array<TrackedHand> {
            val a=Math.toRadians(degrees);val dx=(kotlin.math.cos(a)*.15).toFloat();val dy=(kotlin.math.sin(a)*.15).toFloat()
            return arrayOf(hand(x=.5f-dx,y=.35f-dy,ratio=if(pinched).1f else .8f),hand(id="right-2",x=.5f+dx,y=.35f+dy,ratio=if(pinched).1f else .8f))
        }
        h.arm(*pair(0.0,false));h.pinch(*pair(0.0,true))
        var rotation=0f
        for(angle in 5..220 step 5) {
            val moves=h.frame(*pair(angle.toDouble(),true)).filterIsInstance<HandGestureEvent.TransformMove>()
            assertEquals(1,moves.size)
            assertTrue("Continuous turn reversed at $angle degrees",moves.single().rotationRadians>=rotation-.001f)
            rotation=moves.single().rotationRadians
        }
        assertTrue(rotation>Math.PI)
    }

    @Test fun gripStartsAtThePointThatSelectedTheModelBeforeFingerClosureDrift() {
        val h=Harness();h.arm(hand(x=.45f));h.frame(hand(x=.45f,ratio=.1f))
        val events=h.pinch(hand(x=.53f,ratio=.1f))
        val start=events.filterIsInstance<HandGestureEvent.Start>().single()
        assertEquals(.45f,start.x,.0001f)
        assertTrue(events.filterIsInstance<HandGestureEvent.Move>().any{it.x>start.x})
    }

}
