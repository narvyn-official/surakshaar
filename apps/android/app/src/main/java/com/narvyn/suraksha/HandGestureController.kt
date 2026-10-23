package com.narvyn.suraksha

import java.util.Collections
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot

/** Display-normalized coordinates after camera rotation, crop and mirror mapping. z is not metres. */
internal data class HandLandmark(val x: Float, val y: Float, val z: Float = 0f)
internal enum class Handedness { LEFT, RIGHT, UNKNOWN }

/** A local inference snapshot. IDs must be stable across frames, never inferred from list position. */
internal class TrackedHand(
    val id: String,
    val handedness: Handedness,
    val confidence: Float,
    landmarks: List<HandLandmark>,
) {
    val landmarks: List<HandLandmark> = Collections.unmodifiableList(landmarks.toList())
}

/** Both timestamps use elapsed realtime milliseconds. Never persist or upload this transient input. */
internal class HandTrackingFrame(
    val captureTimeMs: Long,
    val receivedTimeMs: Long,
    val orientationEpoch: Long,
    hands: List<TrackedHand>,
    val identityStable: Boolean = true,
    val viewAspectRatio: Float = 1f,
) {
    val hands: List<TrackedHand> = Collections.unmodifiableList(hands.toList())
}

internal enum class HandCancelReason {
    LOST_TRACKING, STALE_FRAME, LOW_CONFIDENCE, IDENTITY_AMBIGUOUS, IDENTITY_CHANGED,
    ORIENTATION_CHANGED, INTERRUPTED, OPEN_PALM, INVALID_FRAME, CLOCK_ROLLBACK, TARGET_REMOVED,
}

/** Pointer events only: adapters hit-test eligible scene targets and retain ordinary touch controls. */
internal sealed class HandGestureEvent {
    data class Cursor(val handId: String, val x: Float, val y: Float, val armed: Boolean, val pinched: Boolean) : HandGestureEvent()
    data class Start(val targetId: String, val handId: String, val x: Float, val y: Float) : HandGestureEvent()
    /** Coordinates and deltas remain display-normalized; the scene owns ray/plane mapping. */
    data class Move(val targetId: String, val handId: String, val x: Float, val y: Float, val deltaX: Float, val deltaY: Float) : HandGestureEvent()
    data class End(val targetId: String, val handId: String, val x: Float, val y: Float, val dragged: Boolean) : HandGestureEvent()
    data class Cancel(val targetId: String, val reason: HandCancelReason) : HandGestureEvent()
    data class TransformStart(val targetId: String, val firstHandId: String, val secondHandId: String, val centreX:Float, val centreY:Float) : HandGestureEvent()
    /** Cumulative relative values since TransformStart, never absolute model transforms. */
    data class TransformMove(val targetId: String, val scale: Float, val rotationRadians: Float, val centreX: Float, val centreY: Float) : HandGestureEvent()
    data class TransformEnd(val targetId: String, val cancelled: Boolean) : HandGestureEvent()
}

/**
 * One owner calls this controller in timestamp order. Pinch must follow a visible neutral hand;
 * an already pinched startup hand cannot select. Selection locks at pinch onset, and is never
 * retargeted during a drag. Distances are palm-normalized, not device pixel thresholds.
 * These deliberately limited gestures are not hand pose, body diagnosis or practical competency.
 */
internal class HandGestureController {
    private class HandState(val id: String) {
        var armed = false
        var neutralSince: Long? = null
        var pinchSince: Long? = null
        var releaseSince: Long? = null
        var palmSince: Long? = null
        var pinched = false
        var pinchTarget: String? = null
        var lastX: Float? = null
        var lastY: Float? = null
        var wrist: HandLandmark? = null
        var handedness: Handedness = Handedness.UNKNOWN
        val pointer = HandPointerFilter()
    }
    private data class Sample(val hand: TrackedHand, val point: HandLandmark, val ratio: Float, val openPalm: Boolean)
    private data class Active(val target: String, val primary: String, var x: Float, var y: Float, val startX: Float = x, val startY: Float = y, var dragged: Boolean = false)
    private data class Transform(
        val secondary: String, val distance: Float, var lastAngle: Float, var rawRotation: Float = 0f,
        var scale: Float = 1f, var rotation: Float = 0f, var centreX: Float, var centreY: Float,
    )
    private val states = mutableMapOf<String, HandState>()
    private var active: Active? = null
    private var transform: Transform? = null
    private var epoch: Long? = null
    private var lastCapture: Long? = null
    private var lastNow: Long? = null
    private var viewAspect = 1f

    /** Cancel once, then require neutral hands again. Also call on focus loss and session changes. */
    fun interrupt(reason: HandCancelReason = HandCancelReason.INTERRUPTED): List<HandGestureEvent> {
        val events = mutableListOf<HandGestureEvent>()
        active?.let {
            if (transform != null) events += HandGestureEvent.TransformEnd(it.target, true)
            events += HandGestureEvent.Cancel(it.target, reason)
        }
        active = null; transform = null; states.clear()
        return events
    }

    /** The render/input owner calls tick even when the provider stops returning results. */
    fun tick(nowMs: Long): List<HandGestureEvent> {
        val reason = when {
            nowMs < 0 || (lastNow != null && nowMs < lastNow!!) -> HandCancelReason.CLOCK_ROLLBACK
            lastCapture != null && nowMs - lastCapture!! > CAPTURE_FRESH_MS -> HandCancelReason.STALE_FRAME
            else -> null
        }
        lastNow = nowMs
        return if (reason == null) emptyList() else interrupt(reason)
    }

    fun process(frame: HandTrackingFrame, nowMs: Long, targetAt: (Float, Float) -> String?): List<HandGestureEvent> {
        if (nowMs < 0 || (lastNow != null && nowMs < lastNow!!)) {
            lastNow = nowMs
            return interrupt(HandCancelReason.CLOCK_ROLLBACK)
        }
        lastNow = nowMs
        val previousEpoch = epoch
        if (previousEpoch != null && previousEpoch != frame.orientationEpoch) {
            epoch = frame.orientationEpoch; lastCapture = null
            return interrupt(HandCancelReason.ORIENTATION_CHANGED)
        }
        epoch = frame.orientationEpoch
        if (!frame.viewAspectRatio.isFinite() || frame.viewAspectRatio !in .2f..5f || frame.captureTimeMs < 0 || frame.receivedTimeMs < frame.captureTimeMs || frame.receivedTimeMs > nowMs || frame.captureTimeMs > nowMs) {
            return interrupt(HandCancelReason.INVALID_FRAME)
        }
        if (nowMs - frame.captureTimeMs > CAPTURE_FRESH_MS || nowMs - frame.receivedTimeMs > RECEIPT_FRESH_MS) {
            return interrupt(HandCancelReason.STALE_FRAME)
        }
        // Replayed or out-of-order inference cannot finish a dwell/pinch or release a held object.
        if (lastCapture != null && frame.captureTimeMs <= lastCapture!!) return emptyList()
        if (lastCapture != null && abs(viewAspect - frame.viewAspectRatio) > .01f) {
            viewAspect = frame.viewAspectRatio; lastCapture = null
            return interrupt(HandCancelReason.ORIENTATION_CHANGED)
        }
        viewAspect = frame.viewAspectRatio
        val gap = lastCapture?.let { frame.captureTimeMs - it } ?: 0L
        lastCapture = frame.captureTimeMs
        if (gap > CAPTURE_FRESH_MS) return interrupt(HandCancelReason.STALE_FRAME)
        if (!frame.identityStable || frame.hands.size > 2 || frame.hands.map { it.id }.distinct().size != frame.hands.size) {
            return interrupt(HandCancelReason.IDENTITY_AMBIGUOUS)
        }
        if (frame.hands.isEmpty()) return interrupt(HandCancelReason.LOST_TRACKING)
        if (frame.hands.any { it.confidence < MIN_CONFIDENCE || !it.confidence.isFinite() }) {
            return interrupt(HandCancelReason.LOW_CONFIDENCE)
        }
        if (frame.hands.any { !valid(it) }) return interrupt(HandCancelReason.INVALID_FRAME)
        val ids = frame.hands.map { it.id }.toSet()
        val heldIds = listOfNotNull(active?.primary, transform?.secondary)
        if (heldIds.any { it !in ids }) return interrupt(HandCancelReason.IDENTITY_CHANGED)
        states.keys.retainAll(ids)
        val samples = frame.hands.sortedBy { it.id }.map { sample(it) }
        // Identity labels alone are insufficient if a different hand suddenly occupies that label.
        if (samples.any { s ->
                val prior = states[s.hand.id]
                (prior?.wrist?.let { distance(it, s.hand.landmarks[0]) > MAX_WRIST_JUMP } == true) ||
                    (prior != null && prior.handedness != Handedness.UNKNOWN && s.hand.handedness != Handedness.UNKNOWN && prior.handedness != s.hand.handedness)
            }) {
            return interrupt(HandCancelReason.IDENTITY_CHANGED)
        }
        val events = mutableListOf<HandGestureEvent>()
        val began = mutableListOf<String>()
        val ended = mutableListOf<String>()
        var openPalmCancel = false
        for (sample in samples) {
            val state = states.getOrPut(sample.hand.id) { HandState(sample.hand.id) }
            // Opening a pinch moves the index tip even when the hand itself is still. Freeze
            // the held point throughout release confirmation; never commit that release drift.
            if (!state.pinched || sample.ratio < PINCH_OFF) {
                val point = state.pointer.sample(sample.point.x, sample.point.y, frame.captureTimeMs, viewAspect)
                state.lastX = point.first; state.lastY = point.second
            }
            state.wrist = sample.hand.landmarks[0]; state.handedness = sample.hand.handedness
            if (!state.pinched) {
                if (sample.ratio >= PINCH_OFF) {
                    state.pinchSince = null; state.pinchTarget = null
                    if (state.neutralSince == null) state.neutralSince = frame.captureTimeMs
                    if (frame.captureTimeMs - state.neutralSince!! >= NEUTRAL_MS) state.armed = true
                } else {
                    state.neutralSince = null
                    if (state.armed && sample.ratio <= PINCH_ON) {
                        if (state.pinchSince == null) {
                            state.pinchSince = frame.captureTimeMs
                            state.pinchTarget = targetAt(state.lastX!!, state.lastY!!)?.takeIf { it.isNotBlank() && it.length <= 160 }
                        }
                        if (frame.captureTimeMs - state.pinchSince!! >= PINCH_MS) {
                            state.pinched = true; state.armed = false; state.pinchSince = null
                            state.releaseSince = null; state.palmSince = null; began += state.id
                        }
                    } else { state.pinchSince = null; state.pinchTarget = null }
                }
            } else if (sample.ratio >= PINCH_OFF) {
                if (state.releaseSince == null) state.releaseSince = frame.captureTimeMs
                if (sample.openPalm && state.id in heldIds) {
                    if (state.palmSince == null) state.palmSince = frame.captureTimeMs
                    if (frame.captureTimeMs - state.palmSince!! >= OPEN_PALM_MS && state.id in heldIds) openPalmCancel = true
                } else {
                    state.palmSince = null
                    if (frame.captureTimeMs - state.releaseSince!! >= RELEASE_MS) {
                        state.pinched = false; state.armed = false; state.neutralSince = frame.captureTimeMs
                        state.releaseSince = null; ended += state.id
                    }
                }
            } else {
                state.releaseSince = null; state.palmSince = null
            }
            events += HandGestureEvent.Cursor(state.id, state.lastX!!, state.lastY!!, state.armed, state.pinched)
        }
        if (openPalmCancel) return events + interrupt(HandCancelReason.OPEN_PALM)
        active?.let { a ->
            if (a.primary in ended) {
                if (transform != null) events += HandGestureEvent.TransformEnd(a.target, false)
                val p = states.getValue(a.primary)
                val endX = p.lastX!!; val endY = p.lastY!!
                events += HandGestureEvent.End(a.target, a.primary, endX, endY, a.dragged || hypot((endX - a.startX) * viewAspect, endY - a.startY) > .025f)
                active = null; transform = null
                // Another already-held pinch must not silently become the next selection.
                states.values.forEach { it.armed = false; it.neutralSince = null }
            } else if (transform?.secondary in ended) {
                events += HandGestureEvent.TransformEnd(a.target, false); transform = null
            }
        }
        if (active == null) {
            for (id in began) {
                val state = states.getValue(id)
                val target = state.pinchTarget
                if (target != null) {
                    active = Active(target, id, state.lastX!!, state.lastY!!)
                    events += HandGestureEvent.Start(target, id, state.lastX!!, state.lastY!!)
                    break
                }
            }
        }
        active?.let { a ->
            val primary = states.getValue(a.primary)
            val other = began.firstOrNull { it != a.primary && states[it]?.pinched == true }
            if (transform == null && other != null) {
                val secondary = states.getValue(other)
                val distance = hypot((primary.lastX!! - secondary.lastX!!) * viewAspect, primary.lastY!! - secondary.lastY!!)
                if (distance >= MIN_TRANSFORM_SPAN) {
                    a.dragged = true
                    transform = Transform(other, distance, angle(primary, secondary), centreX = (primary.lastX!! + secondary.lastX!!) / 2, centreY = (primary.lastY!! + secondary.lastY!!) / 2)
                    events += HandGestureEvent.TransformStart(a.target, a.primary, other, transform!!.centreX, transform!!.centreY)
                }
            }
            val previousX = a.x; val previousY = a.y
            a.x = primary.lastX!!; a.y = primary.lastY!!
            if (hypot((a.x - a.startX) * viewAspect, a.y - a.startY) > .025f) a.dragged = true
            if (transform == null) {
                if (a.x != previousX || a.y != previousY) events += HandGestureEvent.Move(a.target, a.primary, a.x, a.y, a.x - previousX, a.y - previousY)
            } else {
                val t = transform!!; val secondary = states.getValue(t.secondary)
                val span = hypot((primary.lastX!! - secondary.lastX!!) * viewAspect, primary.lastY!! - secondary.lastY!!)
                val rawScale = (span / t.distance).coerceIn(MIN_SCALE, MAX_SCALE)
                val currentAngle = angle(primary, secondary)
                t.rawRotation = (t.rawRotation + wrapAngle(currentAngle - t.lastAngle)).coerceIn(-MAX_ROTATION, MAX_ROTATION)
                t.lastAngle = currentAngle
                val rawAngle = t.rawRotation
                val alpha=(1f-exp(-gap.coerceAtLeast(1)/55f)).coerceIn(.01f,.95f)
                t.scale = (t.scale+(rawScale-t.scale)*alpha).coerceIn(MIN_SCALE, MAX_SCALE)
                t.rotation = (t.rotation+(rawAngle-t.rotation)*alpha).coerceIn(-MAX_ROTATION, MAX_ROTATION)
                t.centreX = (primary.lastX!! + secondary.lastX!!) / 2; t.centreY = (primary.lastY!! + secondary.lastY!!) / 2
                events += HandGestureEvent.TransformMove(a.target, t.scale, t.rotation, t.centreX, t.centreY)
            }
        }
        return events
    }

    private fun valid(hand: TrackedHand): Boolean = hand.id.isNotBlank() && hand.id.length <= 100 && hand.confidence <= 1f &&
        hand.landmarks.size == 21 && hand.landmarks.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() && it.x in 0f..1f && it.y in 0f..1f && abs(it.z) <= 4f } &&
        palmSize(hand.landmarks) >= .025f
    private fun sample(hand: TrackedHand): Sample {
        val p = hand.landmarks; val palm = palmSize(p)
        val extended = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18).all { (tip, pip) -> distance(p[0], p[tip]) > distance(p[0], p[pip]) * 1.16f }
        val thumbExtended = distance(p[4], p[17]) > palm * 1.35f
        return Sample(hand, p[8], distance(p[4], p[8]) / palm, extended && thumbExtended)
    }
    private fun palmSize(p: List<HandLandmark>) = maxOf(distance(p[5], p[17]), distance(p[0], p[9]))
    private fun distance(a: HandLandmark, b: HandLandmark) = hypot((a.x - b.x) * viewAspect, a.y - b.y)
    private fun angle(a: HandState, b: HandState) = atan2(b.lastY!! - a.lastY!!, (b.lastX!! - a.lastX!!) * viewAspect)
    private fun wrapAngle(value: Float): Float {
        var v = value
        while (v > PI) v -= (2 * PI).toFloat()
        while (v < -PI) v += (2 * PI).toFloat()
        return v
    }
    companion object {
        const val CAPTURE_FRESH_MS = 250L
        const val RECEIPT_FRESH_MS = 350L
        const val MIN_CONFIDENCE = .65f
        const val MIN_SCALE = .5f
        const val MAX_SCALE = 2f
        val MAX_ROTATION = (PI * 4).toFloat()
        private const val PINCH_ON = .30f
        private const val PINCH_OFF = .52f
        private const val NEUTRAL_MS = 120L
        private const val PINCH_MS = 60L
        private const val RELEASE_MS = 90L
        private const val OPEN_PALM_MS = 150L
        private const val MAX_WRIST_JUMP = .30f
        private const val MIN_TRANSFORM_SPAN = .08f
    }
}
