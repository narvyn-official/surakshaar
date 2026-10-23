package com.narvyn.suraksha

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.roundToInt

/** Affine raw IMAGE_NORMALIZED -> VIEW_NORMALIZED, captured on the current ARCore Frame. */
internal data class HandImageToView(val originX:Float,val originY:Float,val xx:Float,val xy:Float,val yx:Float,val yy:Float) {
    init {
        require(listOf(originX,originY,xx,xy,yx,yy).all { it.isFinite() })
        require(abs(xx*yy-xy*yx)>0.00001f) { "Singular camera mapping" }
    }
    fun map(x:Float,y:Float)=Pair(originX+xx*x+yx*y,originY+xy*x+yy*y)
    /** Camera orientation is derived from the camera/display transform, not a guessed sensor angle. */
    fun clockwiseQuarterTurns():Int = if(abs(xx)>abs(xy)) { if(xx>0)0 else 2 } else { if(xy>0)1 else 3 }
    companion object {
        fun fromBasis(values:FloatArray):HandImageToView {
            require(values.size==6)
            return HandImageToView(values[0],values[1],values[2]-values[0],values[3]-values[1],values[4]-values[0],values[5]-values[1])
        }
        /** Undo the physically rotated bitmap before applying ARCore's raw-image transform. */
        fun unrotate(x:Float,y:Float,quarterTurns:Int):Pair<Float,Float> = when(Math.floorMod(quarterTurns,4)) {
            0 -> x to y
            1 -> y to (1f-x)
            2 -> (1f-x) to (1f-y)
            else -> (1f-y) to x
        }
    }
}

internal data class HandYuvPlane(val buffer:ByteBuffer,val rowStride:Int,val pixelStride:Int)

/** The only retained camera data: bounded, downsampled YUV, independent of the closed Android Image. */
internal class HandYuvCopy private constructor(
    val width:Int,val height:Int,val sourceWidth:Int,val sourceHeight:Int,
    val left:Int,val top:Int,val cropWidth:Int,val cropHeight:Int,
    private val y:ByteArray,private val u:ByteArray,private val v:ByteArray
) {
    fun rawNormalized(x:Float,y:Float)=((left+x*cropWidth)/sourceWidth) to ((top+y*cropHeight)/sourceHeight)

    /** Runs on the inference worker; no per-pixel color conversion is performed on the GL thread. */
    fun rotatedArgb(quarterTurns:Int):Triple<Int,Int,IntArray> {
        val turns=Math.floorMod(quarterTurns,4)
        val outWidth=if(turns%2==0)width else height
        val outHeight=if(turns%2==0)height else width
        val out=IntArray(width*height)
        for(sy in 0 until height) for(sx in 0 until width) {
            val i=sy*width+sx
            val c=maxOf(0,(y[i].toInt() and 255)-16)
            val d=(u[i].toInt() and 255)-128; val e=(v[i].toInt() and 255)-128
            val r=((298*c+409*e+128) shr 8).coerceIn(0,255)
            val g=((298*c-100*d-208*e+128) shr 8).coerceIn(0,255)
            val b=((298*c+516*d+128) shr 8).coerceIn(0,255)
            val dx:Int; val dy:Int
            when(turns) {
                0->{dx=sx;dy=sy};1->{dx=height-1-sy;dy=sx}
                2->{dx=width-1-sx;dy=height-1-sy};else->{dx=sy;dy=width-1-sx}
            }
            out[dy*outWidth+dx]=(255 shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Triple(outWidth,outHeight,out)
    }
    fun erase(){y.fill(0);u.fill(0);v.fill(0)}

    companion object {
        const val MAX_EDGE=480
        /** Nearest-neighbour sampling respects each plane's independent position/limit/strides. */
        fun copy(width:Int,height:Int,left:Int,top:Int,right:Int,bottom:Int,planes:List<HandYuvPlane>):HandYuvCopy {
            require(width in 1..8192 && height in 1..8192)
            require(left>=0 && top>=0 && right<=width && bottom<=height && right>left && bottom>top)
            require(planes.size==3)
            val cropWidth=right-left;val cropHeight=bottom-top
            val scale=minOf(1f,MAX_EDGE.toFloat()/maxOf(cropWidth,cropHeight))
            val outWidth=maxOf(1,(cropWidth*scale).roundToInt());val outHeight=maxOf(1,(cropHeight*scale).roundToInt())
            require(outWidth*outHeight<=MAX_EDGE*MAX_EDGE)
            val views=planes.map { p ->
                require(p.rowStride in 1..65536 && p.pixelStride in 1..8)
                p.buffer.duplicate()
            }
            // Validate final accessed byte before allocating/copying, including odd image dimensions.
            planes.forEachIndexed { index,p ->
                val rowWidth=if(index==0)width else (width+1)/2
                require(p.rowStride.toLong()>=(rowWidth-1L)*p.pixelStride+1) { "Invalid YUV row stride" }
                val maxX=if(index==0)right-1 else (right-1)/2
                val maxY=if(index==0)bottom-1 else (bottom-1)/2
                val last=views[index].position().toLong()+maxY.toLong()*p.rowStride+maxX.toLong()*p.pixelStride
                require(last<views[index].limit()) { "Truncated YUV plane" }
            }
            val channels=List(3){ByteArray(outWidth*outHeight)}
            for(oy in 0 until outHeight) for(ox in 0 until outWidth) {
                val sx=left+minOf(cropWidth-1,((ox+.5f)*cropWidth/outWidth).toInt())
                val sy=top+minOf(cropHeight-1,((oy+.5f)*cropHeight/outHeight).toInt())
                val at=oy*outWidth+ox
                planes.forEachIndexed { channel,p ->
                    val px=if(channel==0)sx else sx/2;val py=if(channel==0)sy else sy/2
                    channels[channel][at]=views[channel].get(views[channel].position()+py*p.rowStride+px*p.pixelStride)
                }
            }
            return HandYuvCopy(outWidth,outHeight,width,height,left,top,cropWidth,cropHeight,channels[0],channels[1],channels[2])
        }
    }
}

/** Reject malformed/provider-outside-view results rather than clipping an unseen hand onto targets. */
internal object HandResultMapping {
    fun landmarks(input:List<HandLandmark>,mapping:HandImageToView,quarterTurns:Int,
                  sourceWidth:Int,sourceHeight:Int,left:Int=0,top:Int=0,cropWidth:Int=sourceWidth,cropHeight:Int=sourceHeight):List<HandLandmark>? {
        if(input.size!=21 || sourceWidth<=0 || sourceHeight<=0)return null
        if(input.any{!it.x.isFinite()||!it.y.isFinite()||!it.z.isFinite()||it.x !in 0f..1f||it.y !in 0f..1f})return null
        val points=input.map { point ->
            val raw=HandImageToView.unrotate(point.x,point.y,quarterTurns)
            val view=mapping.map((left+raw.first*cropWidth)/sourceWidth,(top+raw.second*cropHeight)/sourceHeight)
            HandLandmark(view.first,view.second,point.z)
        }
        return points.takeIf{it.all{p->p.x.isFinite()&&p.y.isFinite()&&p.x in 0f..1f&&p.y in 0f..1f}}
    }
}
