package com.narvyn.suraksha

import kotlin.math.abs
import kotlin.math.sqrt

/** A well-conditioned grip constraint; the scene projects movement onto the anchor surface. */
internal class HandDragPlane private constructor(private val point:FloatArray,private val normal:FloatArray) {
    fun intersect(origin:FloatArray,direction:FloatArray):FloatArray? {
        if(origin.size!=3||direction.size!=3||(origin+direction).any{!it.isFinite()})return null
        val denominator=(0..2).sumOf{(normal[it]*direction[it]).toDouble()}.toFloat()
        if(abs(denominator)<.05f)return null
        val distance=(0..2).sumOf{((point[it]-origin[it])*normal[it]).toDouble()}.toFloat()/denominator
        if(!distance.isFinite()||distance !in .05f..4f)return null
        return FloatArray(3){origin[it]+direction[it]*distance}
    }
    companion object {
        fun atGrip(point:FloatArray,direction:FloatArray):HandDragPlane {
            require(point.size==3&&direction.size==3&&(point+direction).all{it.isFinite()})
            // Blend surface-up with the direction toward the camera. Floor-looking input
            // retains its horizontal constraint; eye-level rays no longer run parallel to it.
            var n=floatArrayOf(-direction[0],1f-direction[1],-direction[2])
            var length=sqrt(n.sumOf{(it*it).toDouble()}).toFloat()
            if(length<.05f){n=FloatArray(3){-direction[it]};length=sqrt(n.sumOf{(it*it).toDouble()}).toFloat()}
            require(length>.0001f)
            return HandDragPlane(point.copyOf(),FloatArray(3){n[it]/length})
        }
    }
}
