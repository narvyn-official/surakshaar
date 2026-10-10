package com.narvyn.suraksha

/** Consecutive fresh cue intersections; duration is a UI gesture, never clinical pressure. GL owner only. */
internal class CueHoldGate(private val durationMs:Long=1500,private val maximumGapMs:Long=150){
    private var serial=-1L;private var start=-1L;private var previous=-1L
    fun reset(){serial=-1;start=-1;previous=-1}
    fun sample(id:Long,inside:Boolean,at:Long):Float{
        if(id!=serial||!inside||at<previous||previous>=0&&at-previous>maximumGapMs){start=-1;previous=-1}
        serial=id
        if(!inside)return 0f
        if(start<0)start=at
        previous=at
        return ((at-start).toFloat()/durationMs).coerceIn(0f,1f)
    }
}
