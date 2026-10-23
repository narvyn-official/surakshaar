package com.narvyn.suraksha

import android.opengl.EGL14 as E
import android.opengl.GLES20 as G
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer

/** Generated colour texture and real Cardboard optics on emulator. Not phone/headset evidence. */
@RunWith(AndroidJUnit4::class)
class CameraLensGpuTest {
    internal fun gpu(block:()->Unit){
        val display=E.eglGetDisplay(E.EGL_DEFAULT_DISPLAY)
        assertTrue(E.eglInitialize(display,IntArray(2),0,IntArray(2),1))
        val configs=arrayOfNulls<android.opengl.EGLConfig>(1);val count=IntArray(1)
        assertTrue(E.eglChooseConfig(display,intArrayOf(E.EGL_RENDERABLE_TYPE,E.EGL_OPENGL_ES2_BIT,E.EGL_SURFACE_TYPE,E.EGL_PBUFFER_BIT,E.EGL_RED_SIZE,8,E.EGL_GREEN_SIZE,8,E.EGL_BLUE_SIZE,8,E.EGL_NONE),0,configs,0,1,count,0))
        val context=E.eglCreateContext(display,configs[0],E.EGL_NO_CONTEXT,intArrayOf(E.EGL_CONTEXT_CLIENT_VERSION,2,E.EGL_NONE),0)
        val output=E.eglCreatePbufferSurface(display,configs[0],intArrayOf(E.EGL_WIDTH,320,E.EGL_HEIGHT,160,E.EGL_NONE),0)
        assertTrue(E.eglMakeCurrent(display,output,output,context))
        try{block();assertEquals(G.GL_NO_ERROR,G.glGetError())}finally{E.eglMakeCurrent(display,E.EGL_NO_SURFACE,E.EGL_NO_SURFACE,E.EGL_NO_CONTEXT);E.eglDestroySurface(display,output);E.eglDestroyContext(display,context);E.eglTerminate(display)}
    }
    private fun texture():Int{
        val name=IntArray(1);G.glGenTextures(1,name,0);G.glBindTexture(G.GL_TEXTURE_2D,name[0])
        for(p in intArrayOf(G.GL_TEXTURE_MIN_FILTER,G.GL_TEXTURE_MAG_FILTER))G.glTexParameteri(G.GL_TEXTURE_2D,p,G.GL_NEAREST)
        for(p in intArrayOf(G.GL_TEXTURE_WRAP_S,G.GL_TEXTURE_WRAP_T))G.glTexParameteri(G.GL_TEXTURE_2D,p,G.GL_CLAMP_TO_EDGE)
        val image=ByteBuffer.allocateDirect(64*64*4)
        for(y in 0..63)for(x in 0..63){image.put((x*4).toByte());image.put((y*4).toByte());image.put(80);image.put(-1)}
        image.flip();G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_RGBA,64,64,0,G.GL_RGBA,G.GL_UNSIGNED_BYTE,image)
        return name[0]
    }
    private fun sample(x:Int,y:Int):IntArray{val bytes=ByteBuffer.allocateDirect(4);G.glReadPixels(x,y,1,1,G.GL_RGBA,G.GL_UNSIGNED_BYTE,bytes);return IntArray(3){bytes.get(it).toInt() and 255}}
    @Test fun cameraRayReprojectionKeepsOrientationAndBlacksOutsideCapturedFov()=gpu{
        val renderer=CameraLensCompositor().also{it.create()};val texture=texture()
        try{
            G.glViewport(0,0,160,160);renderer.draw(texture,CameraLensMapping(1f,1f,0f,0f))
            val bottom=sample(40,40);val top=sample(40,120)
            assertTrue(bottom[1] in 56..72);assertTrue("FBO Y must not be flipped",top[1] in 184..200)
            renderer.draw(texture,CameraLensMapping(2f,2f,-.5f,-.5f))
            assertTrue("Camera FOV edge must be black, never stretched",sample(4,80).all{it<3})
            assertTrue(sample(80,80)[0] in 120..136)
            renderer.draw(texture,CameraLensMapping(1f,1f,.2f,0f))
            assertTrue("Asymmetric lens samples its matching camera ray",sample(80,80)[0] in 172..188)
        }finally{renderer.release();G.glDeleteTextures(1,intArrayOf(texture),0)}
    }
    @Test fun realViewerProfileProjectsBothEyesAndUsesTheLensDistortionRenderer(){
        ActivityScenario.launch(HandsArActivity::class.java).use{activity->
            activity.onActivity{CardboardBridge.initialise(it);CardboardBridge.useDefaultViewer()}
            gpu{
                val renderer=CameraLensCompositor().also{it.create()};val image=texture();val viewer=CardboardBridge.create(320,160)
                val framebuffers=IntArray(1);val atlas=IntArray(1)
                G.glGenTextures(1,atlas,0);G.glBindTexture(G.GL_TEXTURE_2D,atlas[0]);G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_MIN_FILTER,G.GL_LINEAR);G.glTexParameteri(G.GL_TEXTURE_2D,G.GL_TEXTURE_MAG_FILTER,G.GL_LINEAR)
                G.glTexImage2D(G.GL_TEXTURE_2D,0,G.GL_RGBA,320,160,0,G.GL_RGBA,G.GL_UNSIGNED_BYTE,null)
                G.glGenFramebuffers(1,framebuffers,0);G.glBindFramebuffer(G.GL_FRAMEBUFFER,framebuffers[0]);G.glFramebufferTexture2D(G.GL_FRAMEBUFFER,G.GL_COLOR_ATTACHMENT0,G.GL_TEXTURE_2D,atlas[0],0)
                assertEquals(G.GL_FRAMEBUFFER_COMPLETE,G.glCheckFramebufferStatus(G.GL_FRAMEBUFFER))
                try{
                    val camera=FloatArray(16).also{it[0]=1f;it[5]=1f;it[10]=-1.01f;it[11]=-1f;it[14]=-.1f}
                    for(eye in 0..1){
                        val projection=FloatArray(16);CardboardBridge.projection(viewer,eye,.05f,30f,projection)
                        assertTrue(projection[0]>0&&projection[5]>0)
                        G.glViewport(eye*160,0,160,160);renderer.draw(image,CameraLensMapping.between(camera,projection))
                    }
                    CardboardBridge.composite(viewer,320,160,atlas[0])
                    val bytes=ByteBuffer.allocateDirect(320*160*4);G.glReadPixels(0,0,320,160,G.GL_RGBA,G.GL_UNSIGNED_BYTE,bytes)
                    for(eye in 0..1){
                        val coloured=(0 until 160).sumOf{y->(eye*160 until (eye+1)*160).count{x->(bytes.get((y*320+x)*4+2).toInt() and 255)>40}}
                        assertTrue("Real Cardboard lens $eye must display the camera image: $coloured pixels",coloured>500)
                    }
                }finally{CardboardBridge.destroy(viewer);renderer.release();G.glDeleteFramebuffers(1,framebuffers,0);G.glDeleteTextures(2,intArrayOf(image,atlas[0]),0)}
            }
        }
    }
}
