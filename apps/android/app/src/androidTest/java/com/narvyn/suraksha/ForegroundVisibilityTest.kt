package com.narvyn.suraksha

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.EGL14 as E
import android.opengl.GLES20 as G
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.framework.image.BitmapImageBuilder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Labelled upstream photos and generated GPU colours; never live hand/phone evidence. */
@RunWith(AndroidJUnit4::class)
class ForegroundVisibilityTest {
    private val ins=InstrumentationRegistry.getInstrumentation()
    private val app get()=ins.targetContext.applicationContext
    @Test fun pinnedModelRunsOfflineAndMatchesOfficialPortraitMatte(){
        val digest=MessageDigest.getInstance("SHA-256")
        app.assets.open(ForegroundSegmenter.MODEL_ASSET).use{input->val bytes=ByteArray(8192);while(true){val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
        assertEquals(ForegroundSegmenter.MODEL_SHA256,digest.digest().joinToString(""){"%02x".format(it)})
        val original=ins.context.assets.open("foreground/official-portrait.jpg").use{BitmapFactory.decodeStream(it)}
        // Match the production camera worker's 480 px bound; the returned mask follows input size.
        val factor=480f/maxOf(original.width,original.height)
        val photo=Bitmap.createScaledBitmap(original,(original.width*factor).toInt(),(original.height*factor).toInt(),true)
        if(photo!==original)original.recycle()
        val expected=ins.context.assets.open("foreground/official-portrait-expected.jpg").use{BitmapFactory.decodeStream(it)}
        try{ForegroundSegmenter(app).use{model->BitmapImageBuilder(photo).build().use{image->
            val actual=model.segment(image,1,floatArrayOf(0f,0f,1f,0f,0f,1f),0,1)
            var intersection=0;var union=0
            for(y in 0 until actual.height)for(x in 0 until actual.width){
                val found=(actual.alpha[y*actual.width+x].toInt() and 255)>127
                val wanted=Color.red(expected.getPixel(x*expected.width/actual.width,y*expected.height/actual.height))>127
                if(found&&wanted)intersection++;if(found||wanted)union++
            }
            val iou=intersection.toFloat()/union.coerceAtLeast(1)
            assertTrue("Official portrait matte IoU $iou",iou>.90f)
        }}}finally{photo.recycle();expected.recycle()}
    }
    @Test fun cameraForegroundCompositesOverModelAndStaleMaskIsNotReused(){
        val display=E.eglGetDisplay(E.EGL_DEFAULT_DISPLAY);assertNotEquals(E.EGL_NO_DISPLAY,display)
        assertTrue(E.eglInitialize(display,IntArray(2),0,IntArray(2),1))
        val configs=arrayOfNulls<android.opengl.EGLConfig>(1);val count=IntArray(1)
        assertTrue(E.eglChooseConfig(display,intArrayOf(E.EGL_RENDERABLE_TYPE,E.EGL_OPENGL_ES2_BIT,E.EGL_SURFACE_TYPE,E.EGL_PBUFFER_BIT,E.EGL_RED_SIZE,8,E.EGL_GREEN_SIZE,8,E.EGL_BLUE_SIZE,8,E.EGL_NONE),0,configs,0,1,count,0));assertTrue(count[0]>0)
        val context=E.eglCreateContext(display,configs[0],E.EGL_NO_CONTEXT,intArrayOf(E.EGL_CONTEXT_CLIENT_VERSION,2,E.EGL_NONE),0)
        val output=E.eglCreatePbufferSurface(display,configs[0],intArrayOf(E.EGL_WIDTH,32,E.EGL_HEIGHT,32,E.EGL_NONE),0)
        assertTrue(E.eglMakeCurrent(display,output,output,context))
        var texture:SurfaceTexture?=null;var producer:Surface?=null;var graphics:HandsArGraphics?=null
        try{
            val renderer=HandsArGraphics().also{it.create()};graphics=renderer
            val camera=SurfaceTexture(renderer.cameraTexture).also{it.setDefaultBufferSize(32,32)};texture=camera
            val ready=CountDownLatch(1);camera.setOnFrameAvailableListener({ready.countDown()},android.os.Handler(android.os.Looper.getMainLooper()))
            val surface=Surface(camera);producer=surface
            val canvas=surface.lockCanvas(null);canvas.drawColor(Color.RED);surface.unlockCanvasAndPost(canvas)
            assertTrue("Generated camera texture did not arrive",ready.await(5,TimeUnit.SECONDS));camera.updateTexImage()
            val coordinates=renderer.javaClass.getDeclaredField("cameraUv").apply{isAccessible=true}.get(renderer) as FloatBuffer
            coordinates.position(0);coordinates.put(floatArrayOf(0f,0f,1f,0f,0f,1f,1f,1f));coordinates.position(0)
            G.glViewport(0,0,32,32)
            fun blueModel(){G.glDisable(G.GL_SCISSOR_TEST);G.glClearColor(0f,0f,1f,1f);G.glClear(G.GL_COLOR_BUFFER_BIT)}
            fun sample(x:Int,y:Int=16):IntArray{val bytes=ByteBuffer.allocateDirect(4);G.glReadPixels(x,y,1,1,G.GL_RGBA,G.GL_UNSIGNED_BYTE,bytes);return IntArray(3){bytes.get(it).toInt() and 255}}
            val mask=ForegroundMask(2,2,byteArrayOf(-1,0,-1,0),floatArrayOf(0f,0f,1f,0f,0f,1f),0,1)
            blueModel();renderer.foreground(mask)
            val hand=sample(4);val model=sample(28)
            assertTrue("Camera must cover geometry at foreground: ${hand.toList()}",hand[0]>220&&hand[2]<20)
            assertTrue("Unmasked model must remain: ${model.toList()}",model[2]>220&&model[0]<20)
            blueModel();renderer.foreground(null)
            assertTrue("Absent matte must not reuse old coverage",sample(4)[2]>220)
            renderer.foreground(null,seeThrough=true)
            val assisted=sample(4);assertTrue("See-through must retain both camera and model: ${assisted.toList()}",assisted[0] in 128..152&&assisted[2] in 103..128)
            val corner=ForegroundMask(2,2,byteArrayOf(-1,0,0,0),floatArrayOf(0f,0f,1f,0f,0f,1f),0,1)
            blueModel();renderer.foreground(corner)
            assertTrue("Top-left image mask must map to top-left display",sample(4,28)[0]>220)
            assertTrue("Bottom-left must not use top-left coverage",sample(4,4)[2]>220)
            val rotated=ForegroundMask(2,2,corner.alpha,floatArrayOf(1f,0f,0f,1f,-1f,0f),0,1)
            blueModel();renderer.foreground(rotated)
            assertTrue("Affine rotation must move coverage to bottom-left",sample(4,4)[0]>220)
            assertTrue("Affine rotation must clear the old corner",sample(4,28)[2]>220)
            blueModel();renderer.foreground(null,handReveal=mask)
            assertTrue("Local hand reveal restores actual camera colour over geometry",sample(4)[0]>220)
            assertTrue("Local reveal must leave unrelated geometry solid",sample(28)[2]>220)
            blueModel();renderer.foreground(null)
            assertTrue("Expired local hand region must not be reused",sample(4)[2]>220)
            assertEquals(G.GL_NO_ERROR,G.glGetError())
        }finally{
            producer?.release();texture?.release();graphics?.release()
            E.eglMakeCurrent(display,E.EGL_NO_SURFACE,E.EGL_NO_SURFACE,E.EGL_NO_CONTEXT);E.eglDestroySurface(display,output);E.eglDestroyContext(display,context);E.eglTerminate(display)
        }
    }
}
