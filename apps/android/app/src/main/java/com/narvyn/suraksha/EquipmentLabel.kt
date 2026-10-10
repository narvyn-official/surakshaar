package com.narvyn.suraksha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

/** Original bilingual training decal. No manufacturer, certification or real pressure claim. */
internal class EquipmentLabel {
    private var program=0
    private var texture=0
    private var position=0
    private var uv=0
    private var transform=0
    private var lighting=0
    private val vertices=ByteBuffer.allocateDirect(32*6*5*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        fun vertex(section:Int,top:Boolean){
            val u=section/32f
            val angle=(u-.5f)*1.38f
            put(sin(angle)*.0862f);put(if(top).323f else .187f);put(cos(angle)*.0862f)
            put(u);put(if(top)0f else 1f)
        }
        repeat(32){i->vertex(i,false);vertex(i+1,false);vertex(i+1,true);vertex(i,false);vertex(i+1,true);vertex(i,true)}
        position(0)
    }
    fun contextCreated(){program=0;texture=0}
    private fun create(){
        fun shader(type:Int,text:String):Int {
            val id=GLES20.glCreateShader(type);GLES20.glShaderSource(id,text);GLES20.glCompileShader(id)
            val ok=IntArray(1);GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0)
            check(ok[0]!=0){GLES20.glGetShaderInfoLog(id)};return id
        }
        val vertex=shader(GLES20.GL_VERTEX_SHADER,"uniform mat4 mvp;attribute vec3 position;attribute vec2 uv;varying mediump vec2 tex;void main(){tex=uv;gl_Position=mvp*vec4(position,1.0);}")
        val fragment=shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float;uniform sampler2D decal;uniform vec4 light;varying mediump vec2 tex;void main(){vec4 c=texture2D(decal,tex);gl_FragColor=vec4(c.rgb*clamp(light.rgb*light.a,vec3(.65),vec3(1.15)),1.0);}")
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vertex);GLES20.glAttachShader(program,fragment);GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertex);GLES20.glDeleteShader(fragment)
        val ok=IntArray(1);GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);check(ok[0]!=0){GLES20.glGetProgramInfoLog(program)}
        position=GLES20.glGetAttribLocation(program,"position");uv=GLES20.glGetAttribLocation(program,"uv")
        transform=GLES20.glGetUniformLocation(program,"mvp");lighting=GLES20.glGetUniformLocation(program,"light")
        val names=IntArray(1);GLES20.glGenTextures(1,names,0);texture=names[0];check(texture!=0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
        val bitmap=bitmap()
        try{GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)}finally{bitmap.recycle()}
    }
    fun draw(mvp:FloatArray,light:FloatArray){
        if(program==0)create()
        GLES20.glUseProgram(program);GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"decal"),0)
        GLES20.glUniformMatrix4fv(transform,1,false,mvp,0);GLES20.glUniform4fv(lighting,1,light,0)
        vertices.position(0);GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,20,vertices)
        vertices.position(3);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,20,vertices)
        GLES20.glEnableVertexAttribArray(position);GLES20.glEnableVertexAttribArray(uv)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices.capacity()/5)
        GLES20.glDisableVertexAttribArray(position);GLES20.glDisableVertexAttribArray(uv);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
    }
    private fun bitmap():Bitmap=Bitmap.createBitmap(1024,1024,Bitmap.Config.ARGB_8888).also { bitmap->
        val c=Canvas(bitmap);val p=Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(Color.rgb(246,244,235))
        p.color=Color.rgb(157,31,39);c.drawRect(0f,0f,1024f,174f,p)
        fun text(value:String,y:Float,size:Float,bold:Boolean=true,colour:Int=Color.rgb(27,38,48)){
            p.color=colour;p.textSize=size;p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
            c.drawText(value,512f,y,p)
        }
        text("TRAINING UNIT",82f,62f,colour=Color.WHITE)
        text("प्रशिक्षण उपकरण",144f,46f,colour=Color.WHITE)
        text("PAPER / WOOD",256f,69f)
        text("कागज़ / लकड़ी",323f,51f)
        // Paper and wood symbols reinforce the authored material cue without claiming a class.
        p.color=Color.rgb(31,104,78);c.drawRoundRect(74f,374f,476f,634f,22f,22f,p);c.drawRoundRect(548f,374f,950f,634f,22f,22f,p)
        p.color=Color.WHITE;p.style=Paint.Style.STROKE;p.strokeWidth=10f
        c.drawRect(170f,418f,354f,588f,p)
        for(y in listOf(458f,500f,542f))c.drawLine(200f,y,324f,y,p)
        c.drawOval(606f,470f,690f,566f,p);c.drawLine(648f,470f,850f,426f,p);c.drawLine(648f,566f,850f,522f,p);c.drawArc(806f,426f,894f,522f,-90f,180f,false,p)
        p.style=Paint.Style.FILL
        text("SIMULATED LABEL",717f,47f)
        text("काल्पनिक लेबल",775f,42f,false)
        p.color=Color.rgb(162,170,174);c.drawRect(74f,823f,950f,826f,p)
        text("Read the real unit's instructions",886f,38f,false)
        text("असली उपकरण के निर्देश पढ़ें",944f,37f,false)
    }
}
