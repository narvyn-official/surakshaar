package com.narvyn.suraksha

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.PI

/** Display-space pointer filtering. It never predicts past the observed hand. */
internal class HandPointerFilter {
    private var at:Long?=null
    private var rawX=0f;private var rawY=0f
    private var x=0f;private var y=0f
    private var vx=0f;private var vy=0f
    fun sample(nextX:Float,nextY:Float,time:Long,aspect:Float):Pair<Float,Float> {
        val previous=at
        if(previous==null){at=time;rawX=nextX;rawY=nextY;x=nextX;y=nextY;return x to y}
        if(time<=previous)return x to y
        val seconds=((time-previous)/1000f).coerceIn(.001f,.25f)
        val derivativeAlpha=1f-exp((-2*PI*seconds).toFloat())
        vx+=(((nextX-rawX)*aspect/seconds)-vx)*derivativeAlpha
        vy+=(((nextY-rawY)/seconds)-vy)*derivativeAlpha
        val cutoff=1.6f+7f*hypot(vx,vy)
        val alpha=1f-exp((-2*PI*cutoff*seconds).toFloat())
        val step=1.5f*seconds
        x+=((nextX-x)*alpha).coerceIn(-step/aspect,step/aspect)
        y+=((nextY-y)*alpha).coerceIn(-step,step)
        rawX=nextX;rawY=nextY;at=time
        return x to y
    }
}
