package com.narvyn.suraksha

import android.opengl.GLES20 as GL
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Static VBOs and bounded material lighting; geometry and picking share the source mesh. */
internal class AnatomyRenderer(private val data:AnatomyData,private val equipment:List<AnatomyPart> = emptyList()) {
    private var program=0
    private var position=0;private var normal=0;private var matrix=0;private var model=0;private var normalMatrix=0
    private var colour=0;private var surface=0;private var eye=0;private var selection=0
    private val buffers=HashMap<String,Int>()
    private var uploadScratch:java.nio.FloatBuffer?=null
    private val materials=(data.parts+equipment).associate{it.id to AnatomyMaterials.forPart(it)}
    private val partMvp=FloatArray(16);private val partModel=FloatArray(16)
    private val inverseModel=FloatArray(16);private val normals=FloatArray(16)

    /** Called for a new EGL context. Rebuilding in the same context requires release() first. */
    fun create(){
        // Lost EGL contexts own their old names. Never delete stale names in a new context.
        buffers.clear();program=0
        var vertex=0;var fragment=0
        try {
            vertex=shader(GL.GL_VERTEX_SHADER,VERTEX_SHADER)
            fragment=shader(GL.GL_FRAGMENT_SHADER,FRAGMENT_SHADER)
            program=GL.glCreateProgram();check(program!=0){"Could not create anatomy shader"}
            GL.glAttachShader(program,vertex);GL.glAttachShader(program,fragment);GL.glLinkProgram(program)
            val linked=IntArray(1);GL.glGetProgramiv(program,GL.GL_LINK_STATUS,linked,0)
            check(linked[0]!=0){GL.glGetProgramInfoLog(program)}
            position=GL.glGetAttribLocation(program,"p");normal=GL.glGetAttribLocation(program,"n")
            matrix=GL.glGetUniformLocation(program,"mvp");model=GL.glGetUniformLocation(program,"model")
            normalMatrix=GL.glGetUniformLocation(program,"normalMatrix");colour=GL.glGetUniformLocation(program,"colour")
            surface=GL.glGetUniformLocation(program,"surface");eye=GL.glGetUniformLocation(program,"eye")
            selection=GL.glGetUniformLocation(program,"selection")
            // Upload only displayed layers. Reuse one bounded staging buffer rather than
            // allocate a direct buffer for every structure during camera startup.
        }catch(error:Throwable){release();throw error}
        finally {
            if(vertex!=0)GL.glDeleteShader(vertex)
            if(fragment!=0)GL.glDeleteShader(fragment)
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER,0)
        }
    }

    /** GL owner only, before its EGL context is retired. Safe after partial creation. */
    fun release(){
        if(buffers.isNotEmpty()){
            val names=buffers.values.toIntArray();GL.glDeleteBuffers(names.size,names,0);buffers.clear()
        }
        if(program!=0){GL.glDeleteProgram(program);program=0}
        uploadScratch=null
    }

    private fun buffer(part:AnatomyPart):Int = buffers[part.id] ?: run {
        val ids=IntArray(1);GL.glGenBuffers(1,ids,0);check(ids[0]!=0){"Could not create anatomy buffer"}
        try {
            val scratch=uploadScratch?.takeIf{it.capacity()>=part.vertices.size}
                ?: ByteBuffer.allocateDirect(part.vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().also{uploadScratch=it}
            scratch.clear();scratch.put(part.vertices);scratch.flip()
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER,ids[0]);GL.glBufferData(GL.GL_ARRAY_BUFFER,part.vertices.size*4,scratch,GL.GL_STATIC_DRAW)
            check(GL.glGetError()==GL.GL_NO_ERROR){"Could not upload anatomy geometry"}
            buffers[part.id]=ids[0];ids[0]
        } catch(error:Throwable){GL.glDeleteBuffers(1,ids,0);throw error}
    }

    fun draw(mvp:FloatArray,transform:FloatArray,parts:List<AnatomyPart>,selected:String?,alpha:Float=1f,
             skinTone:Int=AnatomyMaterials.DEFAULT_SKIN_TONE,partTransforms:Map<String,FloatArray> = emptyMap(),cameraPosition:FloatArray?=null){
        if(program==0)return
        GL.glEnable(GL.GL_DEPTH_TEST);GL.glDisable(GL.GL_CULL_FACE);GL.glUseProgram(program)
        GL.glUniform3f(eye,cameraPosition?.get(0)?:0f,cameraPosition?.get(1)?:.9f,cameraPosition?.get(2)?:4f)
        GL.glDepthMask(alpha>=1f)
        if(alpha<1f){GL.glEnable(GL.GL_BLEND);GL.glBlendFunc(GL.GL_SRC_ALPHA,GL.GL_ONE_MINUS_SRC_ALPHA)}else GL.glDisable(GL.GL_BLEND)
        GL.glEnableVertexAttribArray(position);GL.glEnableVertexAttribArray(normal)
        val skin=AnatomyMaterials.skinTones.getOrNull(skinTone)?:AnatomyMaterials.skinTones[AnatomyMaterials.DEFAULT_SKIN_TONE]
        uploadMatrices(mvp,transform)
        var baseMatricesActive=true
        parts.forEach{part->
            val local=partTransforms[part.id]
            if(local!=null){
                Matrix.multiplyMM(partMvp,0,mvp,0,local,0);Matrix.multiplyMM(partModel,0,transform,0,local,0)
                uploadMatrices(partMvp,partModel);baseMatricesActive=false
            }else if(!baseMatricesActive){uploadMatrices(mvp,transform);baseMatricesActive=true}
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER,buffer(part));GL.glVertexAttribPointer(position,3,GL.GL_FLOAT,false,24,0);GL.glVertexAttribPointer(normal,3,GL.GL_FLOAT,false,24,12)
            val c=if(part.layer=="skin")skin.colour else part.colour
            GL.glUniform4f(colour,c[0],c[1],c[2],alpha)
            val material=materials.getValue(part.id)
            GL.glUniform4f(surface,material.roughness,material.specular,material.softness,material.variation)
            GL.glUniform1f(selection,if(part.id==selected)1f else 0f)
            GL.glDrawArrays(GL.GL_TRIANGLES,0,part.vertices.size/6)
        }
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER,0);GL.glDisableVertexAttribArray(position);GL.glDisableVertexAttribArray(normal);GL.glDisable(GL.GL_BLEND);GL.glDepthMask(true)
    }

    private fun uploadMatrices(mvp:FloatArray,transform:FloatArray){
        if(Matrix.invertM(inverseModel,0,transform,0))Matrix.transposeM(normals,0,inverseModel,0)else Matrix.setIdentityM(normals,0)
        GL.glUniformMatrix4fv(matrix,1,false,mvp,0);GL.glUniformMatrix4fv(model,1,false,transform,0);GL.glUniformMatrix4fv(normalMatrix,1,false,normals,0)
    }

    private fun shader(type:Int,source:String):Int {
        val id=GL.glCreateShader(type);check(id!=0){"Could not allocate anatomy shader"}
        try {
            GL.glShaderSource(id,source);GL.glCompileShader(id)
            val compiled=IntArray(1);GL.glGetShaderiv(id,GL.GL_COMPILE_STATUS,compiled,0)
            check(compiled[0]!=0){GL.glGetShaderInfoLog(id)}
            return id
        }catch(error:Throwable){GL.glDeleteShader(id);throw error}
    }

    companion object {
        private const val VERTEX_SHADER="""
            attribute vec3 p; attribute vec3 n;
            uniform mat4 mvp; uniform mat4 model; uniform mat4 normalMatrix;
            varying vec3 worldNormal; varying vec3 worldPosition; varying vec3 sourcePosition;
            void main(){
                worldNormal=normalize(mat3(normalMatrix)*n);
                worldPosition=(model*vec4(p,1.0)).xyz;
                sourcePosition=p;
                gl_Position=mvp*vec4(p,1.0);
            }
        """
        // Analytic lighting: no environment maps, added textures or extra render passes.
        // Fine variation is cosmetic, not histology, muscle-fibre direction or an injury cue.
        private const val FRAGMENT_SHADER="""
            precision mediump float;
            varying vec3 worldNormal; varying vec3 worldPosition; varying vec3 sourcePosition;
            uniform vec4 colour; uniform vec4 surface; uniform vec3 eye; uniform float selection;
            void main(){
                vec3 n=normalize(worldNormal); if(!gl_FrontFacing)n=-n;
                vec3 view=normalize(eye-worldPosition);
                vec3 key=normalize(vec3(-0.45,0.72,0.85));
                vec3 fill=normalize(vec3(0.72,0.30,-0.45));
                float ndl=dot(n,key);
                float diffuse=max(ndl,0.0);
                float soft=max((ndl+0.38)/1.38,0.0);
                float hemisphere=0.26+0.10*(n.y*0.5+0.5);
                float light=hemisphere+0.67*mix(diffuse,soft,surface.z)+0.18*max(dot(n,fill),0.0);
                float shine=pow(max(dot(n,normalize(key+view)),0.0),mix(76.0,10.0,surface.x));
                float fleck=sin(sourcePosition.x*173.0+sin(sourcePosition.y*197.0))*sin(sourcePosition.z*157.0+sourcePosition.y*89.0);
                vec3 base=pow(max(colour.rgb,vec3(0.001)),vec3(2.2));
                vec3 lit=base*light*(1.0+surface.w*fleck)+vec3(0.93,0.96,1.0)*shine*surface.y*0.22;
                float edge=pow(1.0-max(dot(n,view),0.0),3.0);
                lit+=base*edge*0.07;
                vec3 result=pow(max(lit,vec3(0.0)),vec3(0.454545));
                result=mix(result,vec3(0.20,0.54,0.88),selection*(0.13+0.24*edge));
                gl_FragColor=vec4(result,colour.a);
            }
        """
    }
}
