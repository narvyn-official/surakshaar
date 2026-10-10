package com.narvyn.suraksha

/** Serialises session replacement and response application. Ordinary cookie
 * refreshes do not change identity; explicit sign-in, sign-out and origin changes do. */
internal class SessionRequestGuard {
    @Volatile private var generation=0L
    fun capture()=generation
    @Synchronized fun <T> snapshot(expected:Long?=null,read:()->T):T{
        if(expected!=null&&expected!=generation)throw Replaced()
        return read()
    }
    class Replaced:Exception()
    @Synchronized fun replace(change:()->Unit){generation++;change()}
    @Synchronized fun apply(expected:Long,change:()->Unit):Boolean {
        if(expected!=generation)return false
        change();return true
    }
    fun isCurrent(expected:Long)=expected==generation
}
