package com.narvyn.suraksha

import kotlin.math.abs
import kotlin.math.sqrt

/** GL-owner-only aggregate counters. No images, landmarks, identity, disk writes or uploads. */
internal class HandsArSessionMetrics {
    private val intervals=ArrayDeque<Long>()
    private val ages=ArrayDeque<Long>()
    private var lastFrame:Long?=null
    private var lastHand:Pair<Long,Long>?=null
    var selections=0;private set
    var moved=false;private set
    var lifted=false;private set
    var resized=false;private set
    var rotated=false;private set
    var twoHandsSeen=false;private set
    val frameSamples get()=intervals.size
    val handSamples get()=ages.size
    val frameP95Ms get()=percentile(intervals,.95f)
    val handAgeP95Ms get()=percentile(ages,.95f)
    private fun percentile(values:ArrayDeque<Long>,p:Float):Long=if(values.isEmpty())0 else values.sorted()[kotlin.math.ceil(values.size*p).toInt().coerceIn(1,values.size)-1]
    private fun append(values:ArrayDeque<Long>,value:Long){if(values.size==120)values.removeFirst();values.addLast(value)}
    fun render(atMs:Long,tracking:Boolean){
        if(!tracking){lastFrame=null;return}
        lastFrame?.let{if(atMs>it)append(intervals,(atMs-it).coerceAtMost(10000))};lastFrame=atMs
    }
    fun suspend(){lastFrame=null}
    fun hand(frame:HandTrackingFrame?,now:Long){
        if(frame==null||!frame.identityStable||now-frame.captureTimeMs !in 0..HandGestureController.CAPTURE_FRESH_MS||frame.receivedTimeMs !in frame.captureTimeMs..now)return
        val id=frame.orientationEpoch to frame.captureTimeMs
        if(lastHand==id)return
        lastHand=id;append(ages,now-frame.captureTimeMs)
        if(frame.hands.size==2&&frame.hands.all{it.confidence>=HandGestureController.MIN_CONFIDENCE})twoHandsSeen=true
    }
    fun selected(){if(selections<Int.MAX_VALUE)selections++}
    fun committed(before:HandModelPose,after:HandModelPose){
        val a=before.bounded();val b=after.bounded()
        if(sqrt((b.x-a.x)*(b.x-a.x)+(b.z-a.z)*(b.z-a.z))>=.03f)moved=true
        if(abs(b.y-a.y)>=.03f)lifted=true
        if(abs(b.scale/a.scale-1f)>=.1f)resized=true
        if(abs(HandModelPose.angle(b.yaw-a.yaw))>=10f)rotated=true
    }
    fun summary():String {
        fun observed(value:Boolean)=if(value)"observed"else"not yet observed"
        val timing=if(intervals.isEmpty())"Waiting for tracked frames"else"Render interval p50 / p95: ${percentile(intervals,.5f)} / $frameP95Ms ms"
        val input=if(ages.isEmpty())"Waiting for fresh hand results"else"Hand result age p95: $handAgeP95Ms ms ($handSamples samples)"
        return "Session check\n\n$timing\n$input\n\nHand selections: $selections\nTwo hands: ${observed(twoHandsSeen)}\nMove: ${observed(moved)}\nLift: ${observed(lifted)}\nResize: ${observed(resized)}\nRotate: ${observed(rotated)}\n\nTimings are app measurements, not display latency. Actions count only committed hand movements. Check real hand visibility and lens comfort yourself; these counters cannot measure accuracy. Local hand reveal is assistance, not measured depth. Nothing is saved or uploaded."
    }
}
