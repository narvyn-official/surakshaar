package com.narvyn.suraksha

import kotlin.math.abs

/** An inference matte, not measured depth. Coordinates use the exact camera crop/rotation. */
internal class ForegroundMask(
    val width:Int, val height:Int, val alpha:ByteArray,
    val viewToMask:FloatArray, val captureTimeMs:Long, val generation:Long
) {
    init {
        require(width in 1..512 && height in 1..512 && alpha.size==width*height)
        require(viewToMask.size==6 && viewToMask.all{it.isFinite()})
        require(abs(viewToMask[2]*viewToMask[5]-viewToMask[3]*viewToMask[4])>.00001f)
    }
    fun freshAt(nowMs:Long)=nowMs-captureTimeMs in 0..MAX_AGE_MS
    /** A missed thin hand switches to transparent assistance; no polygon impersonates its edge. */
    fun covers(hands:List<TrackedHand>):Boolean=hands.isNotEmpty()&&hands.all{hand->
        if(hand.landmarks.size!=21)return@all false
        fun covered(point:HandLandmark):Boolean{
            val x=viewToMask[0]+viewToMask[2]*point.x+viewToMask[4]*point.y
            val y=viewToMask[1]+viewToMask[3]*point.x+viewToMask[5]*point.y
            return if(!x.isFinite()||!y.isFinite()||x !in 0f..1f||y !in 0f..1f)false
            else (alpha[(y*(height-1)).toInt()*width+(x*(width-1)).toInt()].toInt() and 255)>=204
        }
        hand.landmarks.count(::covered)>=18&&intArrayOf(0,4,5,8,9,17).all{covered(hand.landmarks[it])}
    }
    companion object {
        const val MAX_AGE_MS=180L
        /** Invert ARCore's affine mapping, undo crop, then rotate into the model input. */
        fun mapping(imageToView:HandImageToView,turns:Int,sourceWidth:Int,sourceHeight:Int,
                    left:Int,top:Int,cropWidth:Int,cropHeight:Int):FloatArray {
            require(sourceWidth>0&&sourceHeight>0&&cropWidth>0&&cropHeight>0)
            require(left>=0&&top>=0&&left+cropWidth<=sourceWidth&&top+cropHeight<=sourceHeight)
            val a=imageToView; val det=a.xx*a.yy-a.xy*a.yx
            fun point(vx:Float,vy:Float):Pair<Float,Float>{
                val dx=vx-a.originX;val dy=vy-a.originY
                val rx=(a.yy*dx-a.yx*dy)/det;val ry=(-a.xy*dx+a.xx*dy)/det
                val x=(rx*sourceWidth-left)/cropWidth;val y=(ry*sourceHeight-top)/cropHeight
                return when(Math.floorMod(turns,4)){0->x to y;1->(1f-y) to x;2->(1f-x) to (1f-y);else->y to (1f-x)}
            }
            val o=point(0f,0f);val x=point(1f,0f);val y=point(0f,1f)
            return floatArrayOf(o.first,o.second,x.first-o.first,x.second-o.second,y.first-o.first,y.second-o.second)
        }
        /** Reject NaN/corruption rather than turning malformed probabilities into an opaque screen. */
        fun probabilities(values:FloatArray):ByteArray {
            require(values.isNotEmpty()&&values.size<=512*512)
            require(values.all{it.isFinite()&&it in 0f..1f})
            return ByteArray(values.size){i->(values[i]*255+.5f).toInt().toByte()}
        }
    }
}
