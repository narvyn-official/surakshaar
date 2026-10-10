package com.narvyn.suraksha

/** View-only magnification. Never changes a model matrix, anchor pose, or distance evidence. */
object InspectionMagnification {
    const val MIN = 1f
    const val MAX = 2.5f
    const val STEP = 1.2f
    data class Snapshot(val factor:Float = MIN, val serial:Long = 0) {
        fun withFactor(value:Float):Snapshot {
            if(!value.isFinite() || value<=0f)return this
            val next=value.coerceIn(MIN,MAX)
            return if(next==factor)this else Snapshot(next,serial+1)
        }
    }
    /** Column-major projection: enlarge clip x/y about an optional viewport centre. */
    fun project(matrix:FloatArray,factor:Float,centreX:Float=0f,centreY:Float=0f) {
        require(matrix.size==16 && factor.isFinite() && factor in MIN..MAX)
        require(centreX.isFinite() && centreY.isFinite())
        for(column in 0..3){
            val i=column*4
            matrix[i]=matrix[i]*factor+matrix[i+3]*centreX*(1f-factor)
            matrix[i+1]=matrix[i+1]*factor+matrix[i+3]*centreY*(1f-factor)
        }
    }
    /** ARCore converts these cropped display coordinates to texture UVs for the same zoom. */
    fun cameraCorners(factor:Float):FloatArray {
        require(factor.isFinite() && factor in MIN..MAX)
        val edge=1f/factor
        return floatArrayOf(-edge,-edge,edge,-edge,-edge,edge,edge,edge)
    }
}
