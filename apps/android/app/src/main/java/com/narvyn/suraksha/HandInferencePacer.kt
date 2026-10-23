package com.narvyn.suraksha

/** Worker-cost-aware cadence. The engine separately enforces one frame in flight. */
internal class HandInferencePacer {
    private var last=Long.MIN_VALUE
    private var cost=0f
    val intervalMs:Long get()=kotlin.math.ceil(maxOf(50f,cost*1.5f).coerceAtMost(160f)).toLong()
    fun reset(){last=Long.MIN_VALUE;cost=0f}
    fun offer(now:Long):Boolean {
        if(now<0L || (last!=Long.MIN_VALUE&&now-last<intervalMs))return false
        last=now;return true
    }
    fun completed(durationMs:Long){if(durationMs in 0..10_000L)cost=if(cost==0f)durationMs.toFloat() else cost*.75f+durationMs*.25f}
}
