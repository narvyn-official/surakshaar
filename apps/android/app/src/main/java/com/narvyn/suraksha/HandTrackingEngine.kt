package com.narvyn.suraksha

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.SystemClock
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal enum class HandTrackingState { DISABLED, STARTING, SEARCHING, TRACKING, PAUSED, UNAVAILABLE }

internal data class HandTrackingSnapshot(
    val state:HandTrackingState,
    val message:String,
    val frame:HandTrackingFrame?=null,
    val processingMs:Long=0,
    val sourceTimestampNs:Long=0,
    val sessionEpoch:Long=0,
    val foreground:ForegroundMask?=null,
    val foregroundUnavailable:Boolean=false
) {
    fun freshAt(nowMs:Long):HandTrackingSnapshot {
        val sample=frame ?: return this
        return if(nowMs-sample.captureTimeMs !in 0..250L || nowMs-sample.receivedTimeMs !in 0..250L)
            copy(state=HandTrackingState.SEARCHING,message="Hand signal delayed. Use touch controls.",frame=null,foreground=null)
        else if(foreground?.freshAt(nowMs)==false)copy(foreground=null)else this
    }
}

/**
 * Optional hand input consumes the existing ARCore camera; it never opens a second camera.
 * The GL owner only copies <=480px YUV and captures the current frame's affine transform.
 * Conversion, model loading and synchronous VIDEO inference belong to one background worker.
 * No camera image, landmark or frame is logged, persisted or sent by this application.
 * MediaPipe's separately disclosed vendor usage/performance metrics are not image uploads.
 */
internal class HandTrackingEngine(context:Context):AutoCloseable {
    private val app=context.applicationContext
    private val executor=Executors.newSingleThreadExecutor { task->Thread(task,"Suraksha hand inference").apply{isDaemon=true} }
    private val busy=AtomicBoolean(false)
    private val lock=Any()
    private var enabled=false
    private var closed=false
    private var generation=generations.incrementAndGet()
    private var geometry:Geometry?=null
    private val pacer=HandInferencePacer()
    private var lastImageNs=0L
    private var paused=false
    private val snapshot=AtomicReference(HandTrackingSnapshot(HandTrackingState.DISABLED,"Hand controls are off. Touch controls remain available."))
    // These fields are worker-owned, including destruction.
    private var landmarker:HandLandmarker?=null
    private var segmenter:ForegroundSegmenter?=null
    private var segmentationFailed=false
    private val segmentationSchedule=ForegroundInferenceSchedule()
    private var lastModelTimestamp=Long.MIN_VALUE
    private val identities=HandIdentityTracker()

    fun setEnabled(value:Boolean) {
        synchronized(lock) {
            if(closed || enabled==value)return
            enabled=value;generation=generations.incrementAndGet();geometry=null;paused=false
            pacer.reset();lastImageNs=0
            snapshot.set(if(value)HandTrackingSnapshot(HandTrackingState.STARTING,"Starting on-device hand controls. Keep touch controls available.")
                else HandTrackingSnapshot(HandTrackingState.DISABLED,"Hand controls are off. Touch controls remain available."))
        }
        if(!value)queueRelease()
    }

    /** Must be called while [frame] is current, on the one ARCore Session.update owner. */
    fun offer(frame:Frame,displayRotation:Int,width:Int,height:Int,trackingReady:Boolean,epoch:Long) {
        val now=SystemClock.elapsedRealtime()
        val key=Geometry(epoch,displayRotation,width,height)
        val token:Long
        synchronized(lock) {
            if(closed||!enabled||snapshot.get().state==HandTrackingState.UNAVAILABLE)return
            if(!trackingReady || width<=0 || height<=0) {
                if(!paused){generation=generations.incrementAndGet();paused=true;geometry=null}
                snapshot.set(HandTrackingSnapshot(HandTrackingState.PAUSED,"AR tracking paused. Hold the phone steady; use touch controls.",sessionEpoch=epoch))
                return
            }
            if(geometry!=key||paused) {
                generation=generations.incrementAndGet();geometry=key;paused=false;lastImageNs=0
                snapshot.set(HandTrackingSnapshot(HandTrackingState.SEARCHING,"Show an open hand in the camera view.",sessionEpoch=epoch))
            }
            token=generation
            if(busy.get() || !pacer.offer(now))return
            if(!busy.compareAndSet(false,true))return
        }
        var copy:HandYuvCopy?=null
        try {
            // Do not retain Frame or Android Image beyond this call: a newer frame invalidates mapping.
            val basis=FloatArray(6)
            frame.transformCoordinates2d(Coordinates2d.IMAGE_NORMALIZED,floatArrayOf(0f,0f,1f,0f,0f,1f),Coordinates2d.VIEW_NORMALIZED,basis)
            val mapping=HandImageToView.fromBasis(basis)
            val captureAt=SystemClock.elapsedRealtime()
            val sourceNs:Long
            val captureTime:Long
            frame.acquireCameraImage().use { image ->
                require(image.format==ImageFormat.YUV_420_888)
                sourceNs=image.timestamp
                synchronized(lock) {
                    if(sourceNs<=lastImageNs){busy.set(false);return}
                    lastImageNs=sourceNs
                }
                // AndroidCameraTimestamp shares the Android Image clock; Frame.timestamp need not.
                val cameraLagNs=frame.androidCameraTimestamp-sourceNs
                require(cameraLagNs in 0..MAX_CAMERA_LAG_NS) { "Camera frame is stale" }
                captureTime=maxOf(0,captureAt-cameraLagNs/1_000_000)
                val crop=image.cropRect
                copy=HandYuvCopy.copy(image.width,image.height,crop.left,crop.top,crop.right,crop.bottom,
                    image.planes.map { HandYuvPlane(it.buffer,it.rowStride,it.pixelStride) })
            }
            val owned=checkNotNull(copy)
            executor.execute {
                try { infer(owned,mapping,sourceNs,captureTime,key,token) }
                finally { owned.erase();busy.set(false) }
            }
        } catch(_:NotYetAvailableException) {
            copy?.erase();busy.set(false)
        } catch(_:RejectedExecutionException) {
            copy?.erase();busy.set(false)
        } catch(_:Exception) {
            copy?.erase();busy.set(false)
            publish(token,key,HandTrackingSnapshot(HandTrackingState.SEARCHING,"Camera frame unavailable. Use touch controls while the camera recovers.",sessionEpoch=epoch))
        }
    }

    fun latestSnapshot(nowMs:Long=SystemClock.elapsedRealtime()):HandTrackingSnapshot {
        return snapshot.get().freshAt(nowMs)
    }

    private fun infer(copy:HandYuvCopy,mapping:HandImageToView,sourceNs:Long,captureTime:Long,key:Geometry,token:Long) {
        if(!isCurrent(token,key))return
        var bitmap:Bitmap?=null
        try {
            val detector=landmarker ?: createLandmarker().also{landmarker=it}
            if(!isCurrent(token,key))return
            val started=SystemClock.elapsedRealtime()
            if(started-captureTime>MAX_RESULT_AGE_MS)return
            val turns=mapping.clockwiseQuarterTurns()
            val pixels=copy.rotatedArgb(turns)
            bitmap=Bitmap.createBitmap(pixels.third,pixels.first,pixels.second,Bitmap.Config.ARGB_8888)
            pixels.third.fill(0)
            val modelTimestamp=maxOf(captureTime,lastModelTimestamp+1)
            lastModelTimestamp=modelTimestamp
            BitmapImageBuilder(bitmap).build().use { image->
                val result=detector.detectForVideo(image,modelTimestamp)
                val received=SystemClock.elapsedRealtime()
                if(!isCurrent(token,key)||received-captureTime>MAX_RESULT_AGE_MS)return@use
                val candidates=mapResult(result,copy,mapping,turns)
                val associated=identities.associate(candidates,token,key.width.toFloat()/key.height)
                val handFrame=HandTrackingFrame(captureTime,received,token,associated.first,associated.second,key.width.toFloat()/key.height)
                val previousMatte=snapshot.get().foreground?.takeIf{it.generation==token&&it.freshAt(received)}
                val hands=HandTrackingSnapshot(if(associated.first.isEmpty())HandTrackingState.SEARCHING else HandTrackingState.TRACKING,
                    if(associated.first.isEmpty())"Show an open hand in the camera view." else "Hand controls active.",handFrame,received-started,sourceNs,key.epoch,previousMatte,segmentationFailed)
                // Input reaches the renderer immediately. Optional person matting cannot add its
                // processing time to this hand result's publication latency.
                publish(token,key,hands)
                if(!segmentationFailed&&segmentationSchedule.shouldRun(received,received-captureTime,associated.first.isNotEmpty())){
                    try {
                        val foreground=segmenter?:ForegroundSegmenter(app).also{segmenter=it}
                        val viewToMask=ForegroundMask.mapping(mapping,turns,copy.sourceWidth,copy.sourceHeight,copy.left,copy.top,copy.cropWidth,copy.cropHeight)
                        val matte=foreground.segment(image,modelTimestamp,viewToMask,captureTime,token)
                        val completed=SystemClock.elapsedRealtime()
                        publish(token,key,hands.copy(foreground=matte.takeIf{it.freshAt(completed)}))
                    }catch(_:Exception){segmentationFailed=true;runCatching{segmenter?.close()};segmenter=null}
                    catch(_:LinkageError){segmentationFailed=true;runCatching{segmenter?.close()};segmenter=null}
                }
            }
            val received=SystemClock.elapsedRealtime()
            synchronized(lock){if(isCurrent(token,key))pacer.completed(received-started)}
            if(snapshot.get().frame?.captureTimeMs!=captureTime&&received-captureTime>MAX_RESULT_AGE_MS) {
                publish(token,key,HandTrackingSnapshot(HandTrackingState.SEARCHING,"Hand signal delayed. Use touch controls.",processingMs=received-started,sessionEpoch=key.epoch))
            }
        } catch(_:LinkageError) {
            publish(token,key,unavailable(key))
            releaseModel()
        } catch(_:Exception) {
            publish(token,key,unavailable(key))
            releaseModel()
        } finally { bitmap?.recycle() }
    }

    private fun mapResult(result:HandLandmarkerResult,copy:HandYuvCopy,mapping:HandImageToView,turns:Int):List<TrackedHand> {
        if(result.landmarks().size>2)return emptyList()
        return result.landmarks().mapIndexedNotNull { index,landmarks ->
            if(landmarks.size!=21)return@mapIndexedNotNull null
            val points=HandResultMapping.landmarks(landmarks.map{HandLandmark(it.x(),it.y(),it.z())},mapping,turns,
                copy.sourceWidth,copy.sourceHeight,copy.left,copy.top,copy.cropWidth,copy.cropHeight) ?: return@mapIndexedNotNull null
            val category=result.handedness().getOrNull(index)?.maxByOrNull{it.score()}
            HandResultMapping.acceptedHand("candidate-$index",category?.categoryName(),category?.score(),points)
        }
    }

    private fun createLandmarker():HandLandmarker {
        val digest=MessageDigest.getInstance("SHA-256")
        var size=0L
        app.assets.open(MODEL_ASSET).use { input->
            val bytes=ByteArray(16384)
            while(true){val n=input.read(bytes);if(n<0)break;size+=n;require(size<=8_000_000);digest.update(bytes,0,n)}
        }
        require(digest.digest().joinToString(""){"%02x".format(it)}==MODEL_SHA256) { "Hand model integrity check failed" }
        val options=HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).setDelegate(Delegate.CPU).build())
            .setRunningMode(RunningMode.VIDEO).setNumHands(2)
            .setMinHandDetectionConfidence(.65f).setMinHandPresenceConfidence(.65f).setMinTrackingConfidence(.65f).build()
        return HandLandmarker.createFromOptions(app,options)
    }
    private fun unavailable(key:Geometry)=HandTrackingSnapshot(HandTrackingState.UNAVAILABLE,"Hand controls unavailable on this device. Use touch controls, or switch hand controls off and on to retry.",sessionEpoch=key.epoch)
    private fun isCurrent(token:Long,key:Geometry)=synchronized(lock){!closed&&enabled&&!paused&&generation==token&&geometry==key}
    private fun publish(token:Long,key:Geometry,value:HandTrackingSnapshot) { synchronized(lock){if(!closed&&enabled&&!paused&&generation==token&&geometry==key)snapshot.set(value)} }
    private fun queueRelease(){try{executor.execute{releaseModel()}}catch(_:RejectedExecutionException){}}
    private fun releaseModel(){try{landmarker?.close()}catch(_:Exception){}finally{landmarker=null;lastModelTimestamp=Long.MIN_VALUE;runCatching{segmenter?.close()};segmenter=null;segmentationFailed=false;segmentationSchedule.reset()}}
    override fun close() {
        synchronized(lock){if(closed)return;closed=true;enabled=false;generation=generations.incrementAndGet();snapshot.set(HandTrackingSnapshot(HandTrackingState.DISABLED,"Hand controls are off."))}
        queueRelease();executor.shutdown()
    }
    private data class Geometry(val epoch:Long,val rotation:Int,val width:Int,val height:Int)
    companion object {
        const val MODEL_ASSET="models/hand-tracking/hand_landmarker.task"
        const val MODEL_SHA256="fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1"
        private const val MAX_RESULT_AGE_MS=250L
        private const val MAX_CAMERA_LAG_NS=100_000_000L
        private val generations=AtomicLong()
    }
}
