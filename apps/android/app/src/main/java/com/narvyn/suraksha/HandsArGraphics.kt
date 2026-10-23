package com.narvyn.suraksha

import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20 as G
import android.opengl.GLUtils
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** GL-thread-owned camera, HUD and hand marks. The camera uses ARCore's actual display transform. */
internal class HandsArGraphics {
    private val quad = floats(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f))
    private val uv = floats(floatArrayOf(0f,0f,1f,0f,0f,1f,1f,1f))
    private val cameraUv = floats(FloatArray(8))
    private var cameraProgram=0; private var flatProgram=0; private var textureProgram=0
    var cameraTexture=0; private set
    private var hudTexture=0
    private val markBuffer=floats(FloatArray(84))
    private val markData=FloatArray(84)
    private val edges=arrayOf(0 to 1,1 to 2,2 to 3,3 to 4,0 to 5,5 to 6,6 to 7,7 to 8,5 to 9,9 to 10,10 to 11,11 to 12,9 to 13,13 to 14,14 to 15,15 to 16,13 to 17,17 to 18,18 to 19,19 to 20,0 to 17)
    fun create() {
        cameraProgram=program("attribute vec2 p;attribute vec2 u;varying vec2 v;void main(){v=u;gl_Position=vec4(p,0.,1.);}","#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES t;varying vec2 v;void main(){gl_FragColor=texture2D(t,v);}")
        textureProgram=program("attribute vec2 p;attribute vec2 u;varying vec2 v;void main(){v=vec2(u.x,1.-u.y);gl_Position=vec4(p,0.,1.);}","precision mediump float;uniform sampler2D t;varying vec2 v;void main(){gl_FragColor=texture2D(t,v);}")
        flatProgram=program("attribute vec2 p;uniform float size;void main(){gl_Position=vec4(p,0.,1.);gl_PointSize=size;}","precision mediump float;uniform vec4 c;void main(){gl_FragColor=c;}")
        cameraTexture=texture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    }
    private fun texture(target:Int):Int {
        val name=IntArray(1); G.glGenTextures(1,name,0); check(name[0]!=0)
        G.glBindTexture(target,name[0]); for(p in intArrayOf(G.GL_TEXTURE_MIN_FILTER,G.GL_TEXTURE_MAG_FILTER))G.glTexParameteri(target,p,G.GL_LINEAR)
        for(p in intArrayOf(G.GL_TEXTURE_WRAP_S,G.GL_TEXTURE_WRAP_T))G.glTexParameteri(target,p,G.GL_CLAMP_TO_EDGE)
        return name[0]
    }
    fun camera(frame:Frame) {
        quad.position(0);cameraUv.position(0)
        frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,quad,Coordinates2d.TEXTURE_NORMALIZED,cameraUv)
        drawTexture(cameraProgram,cameraTexture,GLES11Ext.GL_TEXTURE_EXTERNAL_OES,cameraUv)
    }
    fun hud(bitmap:Bitmap) {
        if(hudTexture==0)hudTexture=texture(G.GL_TEXTURE_2D)
        G.glBindTexture(G.GL_TEXTURE_2D,hudTexture);GLUtils.texImage2D(G.GL_TEXTURE_2D,0,bitmap,0)
    }
    fun hud() {
        if(hudTexture==0)return
        G.glEnable(G.GL_BLEND);G.glBlendFunc(G.GL_SRC_ALPHA,G.GL_ONE_MINUS_SRC_ALPHA)
        drawTexture(textureProgram,hudTexture,G.GL_TEXTURE_2D,uv);G.glDisable(G.GL_BLEND)
    }
    private fun drawTexture(program:Int,name:Int,target:Int,tex:FloatBuffer) {
        G.glDisable(G.GL_DEPTH_TEST);G.glDepthMask(false);G.glBindBuffer(G.GL_ARRAY_BUFFER,0);G.glUseProgram(program)
        val p=G.glGetAttribLocation(program,"p");val u=G.glGetAttribLocation(program,"u")
        quad.position(0);tex.position(0);G.glActiveTexture(G.GL_TEXTURE0);G.glBindTexture(target,name);G.glUniform1i(G.glGetUniformLocation(program,"t"),0)
        G.glVertexAttribPointer(p,2,G.GL_FLOAT,false,0,quad);G.glVertexAttribPointer(u,2,G.GL_FLOAT,false,0,tex);G.glEnableVertexAttribArray(p);G.glEnableVertexAttribArray(u)
        G.glDrawArrays(G.GL_TRIANGLE_STRIP,0,4);G.glDisableVertexAttribArray(p);G.glDisableVertexAttribArray(u);G.glDepthMask(true)
    }
    fun marks(points:List<HandGestureEvent.Cursor>,hands:List<TrackedHand>,width:Int,height:Int) {
        G.glDisable(G.GL_DEPTH_TEST);G.glBindBuffer(G.GL_ARRAY_BUFFER,0);G.glUseProgram(flatProgram)
        val location=G.glGetAttribLocation(flatProgram,"p"); G.glEnableVertexAttribArray(location)
        for(hand in hands) {
            if(hand.landmarks.size!=21)continue
            for((i,e)in edges.withIndex()){val a=hand.landmarks[e.first];val b=hand.landmarks[e.second];markData[i*4]=a.x*2-1;markData[i*4+1]=1-a.y*2;markData[i*4+2]=b.x*2-1;markData[i*4+3]=1-b.y*2}
            markBuffer.clear();markBuffer.put(markData,0,edges.size*4);markBuffer.position(0);G.glVertexAttribPointer(location,2,G.GL_FLOAT,false,0,markBuffer)
            G.glUniform4f(G.glGetUniformLocation(flatProgram,"c"),.12f,.88f,.74f,1f);G.glLineWidth(2f);G.glDrawArrays(G.GL_LINES,0,edges.size*2)
        }
        for(point in points){
            markBuffer.clear();markBuffer.put(point.x*2-1);markBuffer.put(1-point.y*2);markBuffer.position(0);G.glVertexAttribPointer(location,2,G.GL_FLOAT,false,0,markBuffer)
            val c=if(point.pinched)floatArrayOf(1f,.56f,.20f)else if(point.armed)floatArrayOf(.12f,.95f,.79f)else floatArrayOf(.94f,.96f,.98f)
            G.glUniform4f(G.glGetUniformLocation(flatProgram,"c"),c[0],c[1],c[2],1f);G.glUniform1f(G.glGetUniformLocation(flatProgram,"size"),(minOf(width,height)*.022f).coerceIn(8f,24f));G.glDrawArrays(G.GL_POINTS,0,1)
        }
        G.glDisableVertexAttribArray(location)
    }
    fun release(){for(p in intArrayOf(cameraProgram,flatProgram,textureProgram))if(p!=0)G.glDeleteProgram(p);G.glDeleteTextures(2,intArrayOf(cameraTexture,hudTexture),0);cameraProgram=0;flatProgram=0;textureProgram=0;cameraTexture=0;hudTexture=0}
    private fun program(vertex:String,fragment:String):Int {
        fun shader(type:Int,text:String):Int{val s=G.glCreateShader(type);G.glShaderSource(s,text);G.glCompileShader(s);val ok=IntArray(1);G.glGetShaderiv(s,G.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){val msg=G.glGetShaderInfoLog(s);G.glDeleteShader(s);error(msg)};return s}
        val v=shader(G.GL_VERTEX_SHADER,vertex);val f=shader(G.GL_FRAGMENT_SHADER,fragment);val p=G.glCreateProgram()
        try {G.glAttachShader(p,v);G.glAttachShader(p,f);G.glLinkProgram(p);val ok=IntArray(1);G.glGetProgramiv(p,G.GL_LINK_STATUS,ok,0);check(ok[0]!=0){G.glGetProgramInfoLog(p)};return p}
        catch(e:Throwable){G.glDeleteProgram(p);throw e}finally{G.glDeleteShader(v);G.glDeleteShader(f)}
    }
    private fun floats(values:FloatArray)=ByteBuffer.allocateDirect(values.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(values);position(0)}
}
