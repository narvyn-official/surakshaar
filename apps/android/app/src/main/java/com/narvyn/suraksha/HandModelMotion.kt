package com.narvyn.suraksha

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.cos
import kotlin.math.sin

/** Anchor-local metres; positive Y lifts above the surface. Y-up yaw in degrees. */
internal data class HandModelPose(val x:Float=0f,val z:Float=0f,val scale:Float=1f,val yaw:Float=0f,val y:Float=0f) {
    fun bounded() = if(listOf(x,z,scale,yaw,y).all{it.isFinite()} && scale>0f)
        copy(x=x.coerceIn(-.75f,.75f),z=z.coerceIn(-.75f,.75f),scale=scale.coerceIn(.3f,1.5f),yaw=angle(yaw),y=y.coerceIn(0f,1f))
        else HandModelPose()
    companion object {fun angle(value:Float):Float {val v=value%360;return if(v>180)v-360 else if(v< -180)v+360 else v}}
}

internal object HandModelTransforms {
    fun twoHand3d(base:HandModelPose,pivot:FloatArray,centre:FloatArray,ratio:Float,degrees:Float):HandModelPose {
        if(pivot.size!=3||centre.size!=3||pivot.any{!it.isFinite()}||centre.any{!it.isFinite()}||!ratio.isFinite()||ratio<=0f||!degrees.isFinite())return base
        val next=twoHand(base,pivot[0],pivot[2],centre[0],centre[2],ratio,degrees)
        val effective=next.scale/base.bounded().scale
        return next.copy(y=centre[1]+(base.y-pivot[1])*effective).bounded()
    }
    /** A similarity transform about the actual grip, using the effective bounded scale. */
    fun twoHand(base:HandModelPose,pivotX:Float,pivotZ:Float,centreX:Float,centreZ:Float,ratio:Float,degrees:Float):HandModelPose {
        if(!listOf(pivotX,pivotZ,centreX,centreZ,ratio,degrees).all{it.isFinite()} || ratio<=0f)return base
        val b=base.bounded();val nextScale=(b.scale*ratio).coerceIn(.3f,1.5f);val effective=nextScale/b.scale
        val angle=Math.toRadians(degrees.toDouble());val c=cos(angle).toFloat();val s=sin(angle).toFloat()
        val dx=(b.x-pivotX)*effective;val dz=(b.z-pivotZ)*effective
        return HandModelPose(centreX+c*dx+s*dz,centreZ-s*dx+c*dz,nextScale,b.yaw+degrees,b.y).bounded()
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
            visible.yaw+HandModelPose.angle(next.yaw-visible.yaw)*alpha,visible.y+(next.y-visible.y)*alpha).bounded()
        return visible
    }
}
