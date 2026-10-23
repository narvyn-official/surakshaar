package com.narvyn.suraksha

/** Optional person matting must never hold up publication of hand input. Worker-owned, no queue. */
internal class ForegroundInferenceSchedule {
    private var last:Long?=null
    fun reset(){last=null}
    fun shouldRun(nowMs:Long,handAgeMs:Long,hasHands:Boolean):Boolean {
        if(nowMs<0||handAgeMs !in 0..70)return false
        val previous=last
        if(previous!=null&&nowMs>=previous&&nowMs-previous<(if(hasHands)120L else 240L))return false
        last=nowMs;return true
    }
}
