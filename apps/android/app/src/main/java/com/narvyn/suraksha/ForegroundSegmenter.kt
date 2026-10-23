package com.narvyn.suraksha

import android.content.Context
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import java.nio.ByteOrder
import java.security.MessageDigest

/** Owned by the existing single inference worker. No camera, network or frame persistence. */
internal class ForegroundSegmenter(context:Context):AutoCloseable {
    private val segmenter:ImageSegmenter
    init {
        val hash=MessageDigest.getInstance("SHA-256");var bytes=0
        context.assets.open(MODEL_ASSET).use{input->val buffer=ByteArray(16384);while(true){val n=input.read(buffer);if(n<0)break;bytes+=n;require(bytes<=300_000);hash.update(buffer,0,n)}}
        require(hash.digest().joinToString(""){"%02x".format(it)}==MODEL_SHA256)
        segmenter=ImageSegmenter.createFromOptions(context,ImageSegmenter.ImageSegmenterOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).setDelegate(Delegate.CPU).build())
            .setRunningMode(RunningMode.VIDEO).setOutputConfidenceMasks(true).setOutputCategoryMask(false).build())
    }
    fun segment(image:MPImage,timestamp:Long,mapping:FloatArray,captureTime:Long,generation:Long):ForegroundMask {
        val result=segmenter.segmentForVideo(image,timestamp)
        val masks=result.confidenceMasks().orElseThrow{IllegalStateException("Missing foreground probabilities")}
        try {
            // The pinned single-channel model returns person probability. Tasks versions can
            // expand binary output into background/person channels; person is then channel 1.
            require(masks.size in 1..2)
            val mask=masks[if(masks.size==2)1 else 0]
            require(mask.width in 1..512&&mask.height in 1..512){"Foreground output exceeds the bounded input size"}
            val buffer=ByteBufferExtractor.extract(mask).duplicate().order(ByteOrder.nativeOrder())
            require(buffer.remaining()==mask.width*mask.height*4)
            val values=FloatArray(mask.width*mask.height);buffer.asFloatBuffer().get(values)
            return ForegroundMask(mask.width,mask.height,ForegroundMask.probabilities(values),mapping,captureTime,generation)
        } finally { masks.forEach{it.close()} }
    }
    override fun close(){segmenter.close()}
    companion object {
        const val MODEL_ASSET="models/foreground/selfie_segmenter.tflite"
        const val MODEL_SHA256="191ac9529ae506ee0beefa6b2c945a172dab9d07d1e802a290a4e4038226658b"
    }
}
