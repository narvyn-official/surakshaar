package com.narvyn.suraksha

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Primitive storage avoids one boxed object per float while authoring detailed meshes. */
internal class GeometryFloats {
    private var values=FloatArray(8192)
    var size=0;private set
    fun add(value:Float){
        if(size==values.size){require(size<8_000_000){"Training mesh exceeds its allocation budget"};values=values.copyOf((size*2).coerceAtMost(8_000_000))}
        values[size++]=value
    }
    fun addAll(items:FloatArray){for(value in items)add(value)}
    fun toFloatArray()=values.copyOf(size)
    fun toBuffer():FloatBuffer=ByteBuffer.allocateDirect(size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(values,0,size);position(0)}
}
