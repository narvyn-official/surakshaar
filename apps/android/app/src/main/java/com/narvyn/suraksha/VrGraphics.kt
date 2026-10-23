package com.narvyn.suraksha

import android.graphics.Bitmap
import android.opengl.GLES20 as G
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Small stereo scene helper; the GLSurfaceView owns every GL object and its lifetime. */
internal class VrGraphics {
    private var program=0;private var buffer=0
    private var p=0;private var uv=0;private var mvp=0;private var tint=0;private var mode=0;private var progress=0
    private val model=FloatArray(16);private val matrix=FloatArray(16)
    fun create(){
        fun shader(type:Int,source:String):Int {val s=G.glCreateShader(type);G.glShaderSource(s,source);G.glCompileShader(s);val ok=IntArray(1);G.glGetShaderiv(s,G.GL_COMPILE_STATUS,ok,0);check(ok[0]!=0){G.glGetShaderInfoLog(s)};return s}
        val v=shader(G.GL_VERTEX_SHADER,"attribute vec3 p; attribute vec2 uv; uniform mat4 mvp; varying vec2 t; void main(){t=uv;gl_Position=mvp*vec4(p,1.0);}")
        val f=shader(G.GL_FRAGMENT_SHADER,"""
            precision mediump float; varying vec2 t; uniform sampler2D image; uniform vec4 tint; uniform float mode; uniform float progress;
            void main(){
                if(mode<0.5){gl_FragColor=tint;}
                else if(mode<1.5){gl_FragColor=texture2D(image,t)*tint;}
                else {
                    vec2 q=t-vec2(.5); float r=length(q); float ring=step(.34,r)*(1.0-step(.46,r));
                    float a=mod(atan(q.x,-q.y)+6.283185,6.283185)/6.283185;
                    vec3 c=mix(vec3(.96),vec3(.08,.86,.74),step(a,progress));
                    gl_FragColor=vec4(c,max(ring,(1.0-step(.085,r)))*tint.a);
                }
            }
        """.trimIndent())
        program=G.glCreateProgram();G.glAttachShader(program,v);G.glAttachShader(program,f);G.glLinkProgram(program);G.glDeleteShader(v);G.glDeleteShader(f)
        val ok=IntArray(1);G.glGetProgramiv(program,G.GL_LINK_STATUS,ok,0);check(ok[0]!=0){G.glGetProgramInfoLog(program)}
        p=G.glGetAttribLocation(program,"p");uv=G.glGetAttribLocation(program,"uv");mvp=G.glGetUniformLocation(program,"mvp");tint=G.glGetUniformLocation(program,"tint");mode=G.glGetUniformLocation(program,"mode");progress=G.glGetUniformLocation(program,"progress")
        val vertices=floatArrayOf(-.5f,.5f,0f,0f,0f, -.5f,-.5f,0f,0f,1f, .5f,.5f,0f,1f,0f, .5f,-.5f,0f,1f,1f)
        val ids=IntArray(1);G.glGenBuffers(1,ids,0);buffer=ids[0];G.glBindBuffer(G.GL_ARRAY_BUFFER,buffer)
        G.glBufferData(G.GL_ARRAY_BUFFER,vertices.size*4,ByteBuffer.allocateDirect(vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(vertices);position(0)},G.GL_STATIC_DRAW);G.glBindBuffer(G.GL_ARRAY_BUFFER,0)
    }
    fun texture(bitmap:Bitmap,old:Int=0):Int {
        val id=if(old!=0)old else IntArray(1).also{G.glGenTextures(1,it,0)}[0]
        check(id!=0);G.glActiveTexture(G.GL_TEXTURE0);G.glBindTexture(G.GL_TEXTURE_2D,id)
        G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_MIN_FILTER,G.GL_LINEAR);G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_MAG_FILTER,G.GL_LINEAR)
        G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_WRAP_S,G.GL_CLAMP_TO_EDGE);G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_WRAP_T,G.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(G.GL_TEXTURE_2D,0,bitmap,0);bitmap.recycle();return id
    }
    fun plane(vp:FloatArray,x:Float,y:Float,z:Float,width:Float,height:Float,texture:Int=0,colour:FloatArray=floatArrayOf(1f,1f,1f,1f),tilt:Float=0f,ringProgress:Float?=null){
        Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);if(tilt!=0f)Matrix.rotateM(model,0,tilt,1f,0f,0f);Matrix.scaleM(model,0,width,height,1f)
        Matrix.multiplyMM(matrix,0,vp,0,model,0);quad(matrix,texture,colour,ringProgress)
    }
    private fun quad(matrix:FloatArray,texture:Int,colour:FloatArray,ring:Float?){
        G.glUseProgram(program);G.glActiveTexture(G.GL_TEXTURE0);G.glBindTexture(G.GL_TEXTURE_2D,texture)
        G.glBindBuffer(G.GL_ARRAY_BUFFER,buffer);G.glEnableVertexAttribArray(p);G.glEnableVertexAttribArray(uv)
        G.glVertexAttribPointer(p,3,G.GL_FLOAT,false,20,0);G.glVertexAttribPointer(uv,2,G.GL_FLOAT,false,20,12)
        G.glUniformMatrix4fv(mvp,1,false,matrix,0);G.glUniform4fv(tint,1,colour,0);G.glUniform1f(mode,if(ring!=null)2f else if(texture!=0)1f else 0f);G.glUniform1f(progress,ring?:0f)
        G.glEnable(G.GL_BLEND);G.glBlendFunc(G.GL_SRC_ALPHA,G.GL_ONE_MINUS_SRC_ALPHA);G.glDrawArrays(G.GL_TRIANGLE_STRIP,0,4)
        G.glDisableVertexAttribArray(p);G.glDisableVertexAttribArray(uv);G.glBindBuffer(G.GL_ARRAY_BUFFER,0);G.glDisable(G.GL_BLEND)
    }
    fun release(){if(buffer!=0)G.glDeleteBuffers(1,intArrayOf(buffer),0);if(program!=0)G.glDeleteProgram(program);buffer=0;program=0}
}
