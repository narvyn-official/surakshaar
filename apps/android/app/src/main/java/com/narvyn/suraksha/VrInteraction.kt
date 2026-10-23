package com.narvyn.suraksha

import kotlin.math.abs
import kotlin.math.sqrt

/** Head-direction selection, not eye tracking. Values are local and never persisted. */
internal object VrInteraction {
    data class Target(val id:String,val x:Float,val y:Float,val z:Float,val width:Float,val height:Float) {
        init { require(listOf(x,y,z,width,height).all{it.isFinite()}&&width>0&&height>0) }
    }
    data class Ray(val origin:FloatArray,val direction:FloatArray)
    fun intersect(ray:Ray,target:Target):Float? {
        if(ray.origin.size!=3||ray.direction.size!=3||(ray.origin+ray.direction).any{!it.isFinite()})return null
        if(abs(ray.direction[2])<.00001f)return null
        val t=(target.z-ray.origin[2])/ray.direction[2]
        if(t<=.05f)return null
        val x=ray.origin[0]+t*ray.direction[0];val y=ray.origin[1]+t*ray.direction[1]
        return t.takeIf{abs(x-target.x)<=target.width/2&&abs(y-target.y)<=target.height/2}
    }
    fun nearest(ray:Ray,targets:List<Target>)=targets.mapNotNull{t->intersect(ray,t)?.let{t to it}}.minByOrNull{it.second}?.first
    fun normalised(x:Float,y:Float,z:Float):FloatArray {
        val length=sqrt(x*x+y*y+z*z)
        require(length.isFinite()&&length>.00001f)
        return floatArrayOf(x/length,y/length,z/length)
    }
}

/** A consumed target must be left before it can fire again. Time gaps never complete a dwell. */
internal class VrGazeGate(private val dwellMs:Long=1600,private val freshnessMs:Long=250) {
    init { require(dwellMs in 1000..5000&&freshnessMs in 50..500) }
    private var target:String?=null
    private var since=0L;private var at=0L;private var consumed=false;private var awaitExit=false
    val focused get()=target
    var fraction=0f;private set
    fun reset(waitForExit:Boolean=false){target=null;since=0;at=0;consumed=false;fraction=0f;awaitExit=waitForExit}
    fun frame(id:String?,now:Long,enabled:Boolean,dwell:Boolean):String? {
        if(!enabled||id==null){reset();return null}
        if(awaitExit){target=id;at=now;fraction=0f;return null}
        if(id!=target||now<at||now-at>freshnessMs){target=id;since=now;consumed=false}
        at=now
        fraction=if(dwell&&!consumed)((now-since).toFloat()/dwellMs).coerceIn(0f,1f)else 0f
        if(dwell&&fraction>=1&&!consumed){consumed=true;return id}
        return null
    }
    fun trigger(now:Long):String? {
        if(target==null||consumed||now-at !in 0..freshnessMs)return null
        consumed=true;return target
    }
}

/** Bounded text pages retain all words; no safety sentence is silently ellipsized in the viewer. */
internal object VrTextPages {
    fun split(text:String,limit:Int=240):List<String> {
        require(limit in 80..500)
        var rest=text.trim();val pages=mutableListOf<String>()
        while(rest.isNotEmpty()){
            var n=minOf(limit,rest.length)
            if(n<rest.length){
                if(Character.isHighSurrogate(rest[n-1]))n--
                val word=rest.lastIndexOf(' ',n-1)
                if(word>=limit/3)n=word
            }
            pages+=rest.substring(0,n).trim();rest=rest.substring(n).trimStart()
        }
        return pages.ifEmpty{listOf("")}
    }
}
