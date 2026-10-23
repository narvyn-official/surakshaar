package com.narvyn.suraksha

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.cos
import kotlin.math.sin

/** Metres in the surface anchor's X/Z plane; Y-up yaw in degrees. */
internal data class HandModelPose(val x:Float=0f,val z:Float=0f,val scale:Float=1f,val yaw:Float=0f) {
    fun bounded() = if(listOf(x,z,scale,yaw).all{it.isFinite()} && scale>0f)
        copy(x=x.coerceIn(-.75f,.75f),z=z.coerceIn(-.75f,.75f),scale=scale.coerceIn(.3f,1.5f),yaw=angle(yaw))
        else HandModelPose()
    companion object {fun angle(value:Float):Float {val v=value%360;return if(v>180)v-360 else if(v< -180)v+360 else v}}
}

internal object HandModelTransforms {
    /** A similarity transform about the actual grip, using the effective bounded scale. */
    fun twoHand(base:HandModelPose,pivotX:Float,pivotZ:Float,centreX:Float,centreZ:Float,ratio:Float,degrees:Float):HandModelPose {
        if(!listOf(pivotX,pivotZ,centreX,centreZ,ratio,degrees).all{it.isFinite()} || ratio<=0f)return base
        val b=base.bounded();val nextScale=(b.scale*ratio).coerceIn(.3f,1.5f);val effective=nextScale/b.scale
        val angle=Math.toRadians(degrees.toDouble());val c=cos(angle).toFloat();val s=sin(angle).toFloat()
        val dx=(b.x-pivotX)*effective;val dz=(b.z-pivotZ)*effective
        return HandModelPose(centreX+c*dx+s*dz,centreZ-s*dx+c*dz,nextScale,b.yaw+degrees).bounded()
    }
}

/** Visual interpolation only; the input transaction retains the authoritative bounded target. */
internal class HandModelMotion(initial:HandModelPose=HandModelPose()) {
    var visible=initial.bounded();private set
    fun snap(value:HandModelPose){visible=value.bounded()}
    fun advance(target:HandModelPose,seconds:Float):HandModelPose {
        if(!seconds.isFinite()||seconds<=0f)return visible
        val next=target.bounded();val alpha=1f-exp(-seconds.coerceAtMost(.05f)/.035f)
        visible=HandModelPose(visible.x+(next.x-visible.x)*alpha,visible.z+(next.z-visible.z)*alpha,
            exp(ln(visible.scale)+(ln(next.scale)-ln(visible.scale))*alpha),
            visible.yaw+HandModelPose.angle(next.yaw-visible.yaw)*alpha).bounded()
        return visible
    }
}
