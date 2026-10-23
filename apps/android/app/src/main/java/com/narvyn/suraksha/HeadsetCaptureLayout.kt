package com.narvyn.suraksha

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Camera capture and optical-eye viewport are independent. Do not crop two hands to half a phone. */
internal object HeadsetCaptureLayout {
    fun sourceWidth(viewHeight:Int,imageWidth:Int,imageHeight:Int):Int {
        require(viewHeight in 1..16384&&imageWidth in 1..16384&&imageHeight in 1..16384)
        val aspect=max(imageWidth,imageHeight).toFloat()/min(imageWidth,imageHeight)
        require(aspect in 1f..3f)
        return (viewHeight*aspect).roundToInt().coerceAtMost(16384)
    }
}
