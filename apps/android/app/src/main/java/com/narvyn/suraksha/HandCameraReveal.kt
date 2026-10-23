package com.narvyn.suraksha

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Conservative LOCAL camera reveal around fresh tracked fingers and palms. This is visibility
 * assistance, not segmentation, an anatomical hand mesh or measured depth. Only current camera
 * pixels are drawn through it. A small surrounding region may also cover virtual geometry.
 */
internal object HandCameraReveal {
    const val MAX_AGE_MS=160L
    private val fingers=arrayOf(intArrayOf(0,1,2,3,4),intArrayOf(5,6,7,8),intArrayOf(9,10,11,12),intArrayOf(13,14,15,16),intArrayOf(17,18,19,20))
    private val palm=intArrayOf(0,1,5,9,13,17)
    fun regions(frame:HandTrackingFrame?,now:Long):List<TrackedHand> {
        if(frame==null||!frame.identityStable||now-frame.captureTimeMs !in 0..MAX_AGE_MS||frame.receivedTimeMs !in frame.captureTimeMs..now||frame.hands.size>2||frame.hands.map{it.id}.distinct().size!=frame.hands.size)return emptyList()
        return frame.hands.filter{it.confidence.isFinite()&&it.confidence in HandGestureController.MIN_CONFIDENCE..1f&&it.landmarks.size==21&&it.landmarks.all{p->p.x.isFinite()&&p.y.isFinite()&&p.x in 0f..1f&&p.y in 0f..1f}}
    }
    /** Bounded reusable low-resolution map in display coordinates; no platform bitmap allocation. */
    fun rasterize(hands:List<TrackedHand>,width:Int,height:Int,aspect:Float,out:ByteArray=ByteArray(width*height)):ByteArray {
        require(width in 1..512&&height in 1..512&&out.size==width*height&&aspect.isFinite()&&aspect in .2f..5f)
        out.fill(0)
        fun capsule(a:HandLandmark,b:HandLandmark,radius:Float){
            val ax=a.x*aspect;val ay=a.y;val bx=b.x*aspect;val by=b.y
            val left=((min(ax,bx)-radius)*width/aspect).toInt().coerceIn(0,width-1)
            val right=((max(ax,bx)+radius)*width/aspect+1).toInt().coerceIn(0,width-1)
            val top=((min(ay,by)-radius)*height).toInt().coerceIn(0,height-1)
            val bottom=((max(ay,by)+radius)*height+1).toInt().coerceIn(0,height-1)
            val dx=bx-ax;val dy=by-ay;val length=dx*dx+dy*dy
            val feather=1.5f/height
            for(y in top..bottom)for(x in left..right){
                val px=(x+.5f)*aspect/width-ax;val py=(y+.5f)/height-ay
                val t=if(length>1e-9f)((px*dx+py*dy)/length).coerceIn(0f,1f)else 0f
                val d=sqrt((px-dx*t)*(px-dx*t)+(py-dy*t)*(py-dy*t))
                val alpha=((radius-d)/feather*255).toInt().coerceIn(0,255)
                val index=y*width+x;if(alpha>(out[index].toInt() and 255))out[index]=alpha.toByte()
            }
        }
        for(hand in hands.take(2)){
            val p=hand.landmarks;if(p.size!=21)continue
            val size=max(hypot((p[5].x-p[17].x)*aspect,p[5].y-p[17].y),hypot((p[0].x-p[9].x)*aspect,p[0].y-p[9].y))
            if(!size.isFinite()||size<.025f||size>.8f)continue
            val radius=max(.009f,size*.16f)
            for(finger in fingers)for(i in 0 until finger.lastIndex)capsule(p[finger[i]],p[finger[i+1]],radius)
            // Fill the palm using closely spaced cross-sections. Adjacent finger gaps remain open.
            val centre=HandLandmark(palm.map{p[it].x}.average().toFloat(),palm.map{p[it].y}.average().toFloat())
            for(i in palm.indices){
                val a=p[palm[i]];val b=p[palm[(i+1)%palm.size]]
                val steps=(hypot((a.x-b.x)*aspect,a.y-b.y)*height).toInt().coerceIn(1,128)
                for(n in 0..steps){val t=n.toFloat()/steps;capsule(centre,HandLandmark(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t),radius*.75f)}
            }
        }
        return out
    }
}
