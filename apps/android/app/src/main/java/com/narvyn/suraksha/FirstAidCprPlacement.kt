package com.narvyn.suraksha

/** Model-specific teaching landmark, calibrated once when an anatomy pack is loaded. */
internal object FirstAidCprPlacement {
    const val STERNUM = "FMA7485"
    // The mesh authoring origin is only a reference; loaded sternum/skin determine placement.
    const val AUTHOR_X = 0f
    const val AUTHOR_Y = 1.2825f
    const val AUTHOR_Z = .113f
    const val HEEL_CLEARANCE = .003f
    const val MOTION_DEPTH = .003f
    data class Calibration(val surface:FloatArray,val translation:FloatArray)

    fun forParts(parts:List<AnatomyPart>):Calibration? {
        val sternum=parts.firstOrNull{it.id==STERNUM}?:return null
        val skin=parts.firstOrNull{it.id=="FMA7163"}?:return null
        val b=sternum.bounds
        if(b.size!=6||b.any{!it.isFinite()}||b[4]<=b[1])return null
        val x=(b[0]+b[3])/2f
        // Centre of the lower half, above the inferior tip; source Y points superiorly.
        val y=b[1]+(b[4]-b[1])*.25f
        val origin=floatArrayOf(x,y,skin.bounds[5]+.05f)
        val distance=AnatomyPicking.distance(skin.vertices,skin.bounds,origin,floatArrayOf(0f,0f,-1f))?:return null
        val z=origin[2]-distance
        return Calibration(floatArrayOf(x,y,z),floatArrayOf(x-AUTHOR_X,y-AUTHOR_Y,z-AUTHOR_Z))
    }
    fun usesLandmark(id:String)=id=="teaching:cpr"||id=="teaching:chest-landmark"
}
