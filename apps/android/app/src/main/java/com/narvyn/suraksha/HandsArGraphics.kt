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
    private var cameraProgram=0; private var flatProgram=0; private var textureProgram=0;private var foregroundProgram=0
    var cameraTexture=0; private set
    private var hudTexture=0
    private var hudAllocated=false
    private var hudWidth=0;private var hudHeight=0
    private var maskTexture=0;private var maskWidth=0;private var maskHeight=0
    private var handTexture=0;private var handWidth=0;private var handHeight=0
    private var uploadedHand:ForegroundMask?=null
    private var handBytes=ByteBuffer.allocateDirect(1)
    private var uploadedMask:ForegroundMask?=null
    private var maskBytes=ByteBuffer.allocateDirect(1)
    private val maskMatrix=FloatArray(9)
    private val markBuffer=floats(FloatArray(84))
    private val markData=FloatArray(84)
    private val edges=arrayOf(0 to 1,1 to 2,2 to 3,3 to 4,0 to 5,5 to 6,6 to 7,7 to 8,5 to 9,9 to 10,10 to 11,11 to 12,9 to 13,13 to 14,14 to 15,15 to 16,13 to 17,17 to 18,18 to 19,19 to 20,0 to 17)
    fun create() {
        cameraProgram=program("attribute vec2 p;attribute vec2 u;varying vec2 v;void main(){v=u;gl_Position=vec4(p,0.,1.);}","#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES t;varying vec2 v;void main(){gl_FragColor=texture2D(t,v);}")
        textureProgram=program("attribute vec2 p;attribute vec2 u;varying vec2 v;void main(){v=vec2(u.x,1.-u.y);gl_Position=vec4(p,0.,1.);}","precision mediump float;uniform sampler2D t;varying vec2 v;void main(){gl_FragColor=texture2D(t,v);}")
        flatProgram=program("attribute vec2 p;uniform float size;void main(){gl_Position=vec4(p,0.,1.);gl_PointSize=size;}","precision mediump float;uniform vec4 c;void main(){gl_FragColor=c;}")
        cameraTexture=texture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        foregroundProgram=program("attribute vec2 p;attribute vec2 u;uniform mat3 maskMap;varying vec2 v;varying vec2 m;varying vec2 h;void main(){v=u;h=vec2(p.x*.5+.5,.5-p.y*.5);m=(maskMap*vec3(h,1.)).xy;gl_Position=vec4(p,0.,1.);}","#extension GL_OES_EGL_image_external : require\nprecision mediump float;uniform samplerExternalOES t;uniform sampler2D matte;uniform sampler2D handMatte;uniform float handOn;uniform float assist;varying vec2 v;varying vec2 m;varying vec2 h;void main(){float inside=step(0.,m.x)*step(m.x,1.)*step(0.,m.y)*step(m.y,1.);float coverage=smoothstep(.40,.80,texture2D(matte,m).r)*inside;float hand=texture2D(handMatte,h).r*handOn;gl_FragColor=vec4(texture2D(t,v).rgb,max(max(coverage,hand),assist));}")
        maskTexture=texture(G.GL_TEXTURE_2D)
        G.glPixelStorei(G.GL_UNPACK_ALIGNMENT,1)
        G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_LUMINANCE,1,1,0,G.GL_LUMINANCE,G.GL_UNSIGNED_BYTE,maskBytes)
        handTexture=texture(G.GL_TEXTURE_2D)
        G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_LUMINANCE,1,1,0,G.GL_LUMINANCE,G.GL_UNSIGNED_BYTE,handBytes)
    }
    private fun texture(target:Int):Int {
        val name=IntArray(1); G.glGenTextures(1,name,0); check(name[0]!=0)
        G.glBindTexture(target,name[0]); for(p in intArrayOf(G.GL_TEXTURE_MIN_FILTER,G.GL_TEXTURE_MAG_FILTER))G.glTexParameteri(target,p,G.GL_LINEAR)
        for(p in intArrayOf(G.GL_TEXTURE_WRAP_S,G.GL_TEXTURE_WRAP_T))G.glTexParameteri(target,p,G.GL_CLAMP_TO_EDGE)
        return name[0]
    }
    fun camera(frame:Frame,draw:Boolean=true) {
        quad.position(0);cameraUv.position(0)
        frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,quad,Coordinates2d.TEXTURE_NORMALIZED,cameraUv)
        if(draw)drawTexture(cameraProgram,cameraTexture,GLES11Ext.GL_TEXTURE_EXTERNAL_OES,cameraUv)
    }
    /** Camera pixels are composited AFTER geometry and BEFORE controls. A matte is never depth. */
    fun foreground(mask:ForegroundMask?,seeThrough:Boolean=false,handReveal:ForegroundMask?=null) {
        if(mask==null&&!seeThrough&&handReveal==null)return
        if(mask!=null&&uploadedMask!==mask){
            val size=mask.alpha.size
            if(maskBytes.capacity()<size)maskBytes=ByteBuffer.allocateDirect(size)
            maskBytes.clear();maskBytes.put(mask.alpha);maskBytes.flip()
            G.glActiveTexture(G.GL_TEXTURE1);G.glBindTexture(G.GL_TEXTURE_2D,maskTexture);G.glPixelStorei(G.GL_UNPACK_ALIGNMENT,1)
            if(maskWidth!=mask.width||maskHeight!=mask.height){
                G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_LUMINANCE,mask.width,mask.height,0,G.GL_LUMINANCE,G.GL_UNSIGNED_BYTE,maskBytes)
                maskWidth=mask.width;maskHeight=mask.height
            }else G.glTexSubImage2D(G.GL_TEXTURE_2D,0,0,0,mask.width,mask.height,G.GL_LUMINANCE,G.GL_UNSIGNED_BYTE,maskBytes)
            uploadedMask=mask
        }
        if(handReveal!=null&&uploadedHand!==handReveal){
            val size=handReveal.alpha.size
            if(handBytes.capacity()<size)handBytes=ByteBuffer.allocateDirect(size)
            handBytes.clear();handBytes.put(handReveal.alpha);handBytes.flip()
            G.glActiveTexture(G.GL_TEXTURE2);G.glBindTexture(G.GL_TEXTURE_2D,handTexture);G.glPixelStorei(G.GL_UNPACK_ALIGNMENT,1)
            if(handWidth!=handReveal.width||handHeight!=handReveal.height){
                G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_LUMINANCE,handReveal.width,handReveal.height,0,G.GL_LUMINANCE,G.GL_UNSIGNED_BYTE,handBytes)
                handWidth=handReveal.width;handHeight=handReveal.height
            }else G.glTexSubImage2D(G.GL_TEXTURE_2D,0,0,0,handReveal.width,handReveal.height,G.GL_LUMINANCE,G.GL_UNSIGNED_BYTE,handBytes)
            uploadedHand=handReveal
        }
        val a=mask?.viewToMask
        maskMatrix.fill(0f);maskMatrix[0]=a?.get(2)?:1f;maskMatrix[1]=a?.get(3)?:0f
        maskMatrix[3]=a?.get(4)?:0f;maskMatrix[4]=a?.get(5)?:1f
        maskMatrix[6]=a?.get(0)?:0f;maskMatrix[7]=a?.get(1)?:0f;maskMatrix[8]=1f
        G.glDisable(G.GL_DEPTH_TEST);G.glDepthMask(false);G.glBindBuffer(G.GL_ARRAY_BUFFER,0);G.glUseProgram(foregroundProgram)
        val p=G.glGetAttribLocation(foregroundProgram,"p");val u=G.glGetAttribLocation(foregroundProgram,"u")
        quad.position(0);cameraUv.position(0)
        G.glVertexAttribPointer(p,2,G.GL_FLOAT,false,0,quad);G.glVertexAttribPointer(u,2,G.GL_FLOAT,false,0,cameraUv)
        G.glEnableVertexAttribArray(p);G.glEnableVertexAttribArray(u)
        G.glActiveTexture(G.GL_TEXTURE0);G.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,cameraTexture);G.glUniform1i(G.glGetUniformLocation(foregroundProgram,"t"),0)
        // With no fresh matte bind a zero-coverage map, never a retired person's silhouette.
        if(mask==null){maskMatrix[6]=-2f;maskMatrix[7]=-2f}
        G.glActiveTexture(G.GL_TEXTURE1);G.glBindTexture(G.GL_TEXTURE_2D,maskTexture);G.glUniform1i(G.glGetUniformLocation(foregroundProgram,"matte"),1)
        G.glUniformMatrix3fv(G.glGetUniformLocation(foregroundProgram,"maskMap"),1,false,maskMatrix,0)
        G.glActiveTexture(G.GL_TEXTURE2);G.glBindTexture(G.GL_TEXTURE_2D,handTexture);G.glUniform1i(G.glGetUniformLocation(foregroundProgram,"handMatte"),2)
        G.glUniform1f(G.glGetUniformLocation(foregroundProgram,"handOn"),if(handReveal!=null)1f else 0f)
        G.glUniform1f(G.glGetUniformLocation(foregroundProgram,"assist"),if(seeThrough).55f else 0f)
        G.glEnable(G.GL_BLEND);G.glBlendFunc(G.GL_SRC_ALPHA,G.GL_ONE_MINUS_SRC_ALPHA);G.glDrawArrays(G.GL_TRIANGLE_STRIP,0,4)
        G.glDisable(G.GL_BLEND);G.glDisableVertexAttribArray(p);G.glDisableVertexAttribArray(u);G.glDepthMask(true);G.glActiveTexture(G.GL_TEXTURE0)
    }
    fun hud(bitmap:Bitmap) {
        if(hudTexture==0)hudTexture=texture(G.GL_TEXTURE_2D)
        G.glBindTexture(G.GL_TEXTURE_2D,hudTexture)
        if(!hudAllocated||hudWidth!=bitmap.width||hudHeight!=bitmap.height){GLUtils.texImage2D(G.GL_TEXTURE_2D,0,bitmap,0);hudAllocated=true;hudWidth=bitmap.width;hudHeight=bitmap.height}
        else GLUtils.texSubImage2D(G.GL_TEXTURE_2D,0,0,0,bitmap)
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
    fun marks(points:List<HandGestureEvent.Cursor>,hands:List<TrackedHand>,width:Int,height:Int,showSkeleton:Boolean=false) {
        G.glDisable(G.GL_DEPTH_TEST);G.glBindBuffer(G.GL_ARRAY_BUFFER,0);G.glUseProgram(flatProgram)
        val location=G.glGetAttribLocation(flatProgram,"p"); G.glEnableVertexAttribArray(location)
        for(hand in if(showSkeleton)hands else emptyList()) {
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
    fun release(){for(p in intArrayOf(cameraProgram,flatProgram,textureProgram,foregroundProgram))if(p!=0)G.glDeleteProgram(p);G.glDeleteTextures(4,intArrayOf(cameraTexture,hudTexture,maskTexture,handTexture),0);cameraProgram=0;flatProgram=0;textureProgram=0;foregroundProgram=0;cameraTexture=0;hudTexture=0;maskTexture=0;handTexture=0;hudAllocated=false;uploadedMask=null;uploadedHand=null;maskWidth=0;maskHeight=0;handWidth=0;handHeight=0}
    private fun program(vertex:String,fragment:String):Int {
        fun shader(type:Int,text:String):Int{val s=G.glCreateShader(type);G.glShaderSource(s,text);G.glCompileShader(s);val ok=IntArray(1);G.glGetShaderiv(s,G.GL_COMPILE_STATUS,ok,0);if(ok[0]==0){val msg=G.glGetShaderInfoLog(s);G.glDeleteShader(s);error(msg)};return s}
        val v=shader(G.GL_VERTEX_SHADER,vertex);val f=shader(G.GL_FRAGMENT_SHADER,fragment);val p=G.glCreateProgram()
        try {G.glAttachShader(p,v);G.glAttachShader(p,f);G.glLinkProgram(p);val ok=IntArray(1);G.glGetProgramiv(p,G.GL_LINK_STATUS,ok,0);check(ok[0]!=0){G.glGetProgramInfoLog(p)};return p}
        catch(e:Throwable){G.glDeleteProgram(p);throw e}finally{G.glDeleteShader(v);G.glDeleteShader(f)}
    }
    private fun floats(values:FloatArray)=ByteBuffer.allocateDirect(values.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{put(values);position(0)}
}
