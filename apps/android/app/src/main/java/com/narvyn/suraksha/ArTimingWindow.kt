package com.narvyn.suraksha

import kotlin.math.ceil

/** Fixed memory window. ARCore update cost and distinct camera cadence are separate metrics. */
internal class ArTimingWindow(private val capacity:Int=300) {
    init { require(capacity>0) }
    private val updates=LongArray(capacity)
    private val intervals=LongArray(capacity)
    private var updateCount=0
    private var intervalCount=0
    private var lastFrame=0L
    fun record(updateNs:Long,cameraTimestamp:Long) {
        require(updateNs>=0)
        updates[updateCount%capacity]=updateNs;updateCount++
        if(cameraTimestamp>lastFrame) {
            if(lastFrame>0){intervals[intervalCount%capacity]=cameraTimestamp-lastFrame;intervalCount++}
            lastFrame=cameraTimestamp
        }
    }
    data class Percentiles(val samples:Int,val p50Ms:Double,val p95Ms:Double,val p99Ms:Double)
    private fun percentiles(values:LongArray,count:Int):Percentiles {
        val n=minOf(count,capacity);val sorted=values.take(n).sorted()
        fun p(q:Double)=if(n==0)0.0 else sorted[(ceil(q*n).toInt()-1).coerceAtLeast(0)]/1_000_000.0
        return Percentiles(n,p(.5),p(.95),p(.99))
    }
    fun update()=percentiles(updates,updateCount)
    fun cameraInterval()=percentiles(intervals,intervalCount)
    fun clear(){updateCount=0;intervalCount=0;lastFrame=0}
}
