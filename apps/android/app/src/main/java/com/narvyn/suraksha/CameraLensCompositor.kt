package com.narvyn.suraksha

import android.opengl.GLES20 as G
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Cheap second pass: same monoscopic camera rays, calibrated separately for each viewer lens. */
internal class CameraLensCompositor {
    private var program=0
    private var position=0;private var texture=0;private var scale=0;private var offset=0
    private val quad=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f));position(0)}
    fun create(){
        fun shader(type:Int,source:String):Int{val id=G.glCreateShader(type);G.glShaderSource(id,source);G.glCompileShader(id);val ok=IntArray(1);G.glGetShaderiv(id,G.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){val error=G.glGetShaderInfoLog(id);G.glDeleteShader(id);error(error)};return id}
        val vertex=shader(G.GL_VERTEX_SHADER,"attribute vec2 p;uniform vec2 scale;uniform vec2 offset;varying vec2 uv;void main(){uv=(p*.5+.5)*scale+offset;gl_Position=vec4(p,0.,1.);}")
        val fragment=shader(G.GL_FRAGMENT_SHADER,"precision mediump float;uniform sampler2D image;varying vec2 uv;void main(){if(uv.x<0.||uv.x>1.||uv.y<0.||uv.y>1.)gl_FragColor=vec4(0.,0.,0.,1.);else gl_FragColor=texture2D(image,uv);}")
        program=G.glCreateProgram()
        try{G.glAttachShader(program,vertex);G.glAttachShader(program,fragment);G.glLinkProgram(program);val ok=IntArray(1);G.glGetProgramiv(program,G.GL_LINK_STATUS,ok,0);check(ok[0]!=0){G.glGetProgramInfoLog(program)}}
        catch(e:Throwable){release();throw e}finally{G.glDeleteShader(vertex);G.glDeleteShader(fragment)}
        position=G.glGetAttribLocation(program,"p");texture=G.glGetUniformLocation(program,"image");scale=G.glGetUniformLocation(program,"scale");offset=G.glGetUniformLocation(program,"offset")
    }
    fun draw(cameraSceneTexture:Int,mapping:CameraLensMapping){
        G.glDisable(G.GL_DEPTH_TEST);G.glDisable(G.GL_BLEND);G.glDisable(G.GL_CULL_FACE);G.glBindBuffer(G.GL_ARRAY_BUFFER,0);G.glUseProgram(program)
        quad.position(0);G.glVertexAttribPointer(position,2,G.GL_FLOAT,false,0,quad);G.glEnableVertexAttribArray(position)
        G.glActiveTexture(G.GL_TEXTURE0);G.glBindTexture(G.GL_TEXTURE_2D,cameraSceneTexture);G.glUniform1i(texture,0)
        G.glUniform2f(scale,mapping.scaleX,mapping.scaleY);G.glUniform2f(offset,mapping.offsetX,mapping.offsetY)
        G.glDrawArrays(G.GL_TRIANGLE_STRIP,0,4);G.glDisableVertexAttribArray(position)
    }
    fun release(){if(program!=0)G.glDeleteProgram(program);program=0}
}
