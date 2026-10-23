package com.narvyn.suraksha

/** Same camera ray in an asymmetric calibrated eye frustum. No eye translation or invented depth. */
internal data class CameraLensMapping(val scaleX:Float,val scaleY:Float,val offsetX:Float,val offsetY:Float) {
    /** GL texture convention: origin at the bottom left. Outside coordinates must render black. */
    fun sourceUv(x:Float,y:Float)=Pair(x*scaleX+offsetX,y*scaleY+offsetY)
    companion object {
        fun between(camera:FloatArray,eye:FloatArray):CameraLensMapping {
            for(p in listOf(camera,eye))require(p.size==16&&p.all{it.isFinite()}&&p[0]>.001f&&p[5]>.001f&&kotlin.math.abs(p[11]+1f)<.001f)
            val x=camera[0]/eye[0];val y=camera[5]/eye[5]
            return CameraLensMapping(x,y,(1f-x+x*eye[8]-camera[8])/2,(1f-y+y*eye[9]-camera[9])/2)
        }
    }
}
