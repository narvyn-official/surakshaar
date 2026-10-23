package com.narvyn.suraksha

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/** Official, explicitly labelled static fixtures: this does not claim live camera or phone testing. */
@RunWith(AndroidJUnit4::class)
class HandTrackingEngineTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val app get()=instrumentation.targetContext.applicationContext
    private fun detector()=HandLandmarker.createFromOptions(app,HandLandmarker.HandLandmarkerOptions.builder()
        .setBaseOptions(BaseOptions.builder().setDelegate(Delegate.CPU).setModelAssetPath(HandTrackingEngine.MODEL_ASSET).build())
        .setRunningMode(RunningMode.IMAGE).setNumHands(2).setMinHandDetectionConfidence(.65f)
        .setMinHandPresenceConfidence(.65f).setMinTrackingConfidence(.65f).build())
    @Test fun bundledOfficialModelMatchesPinnedDigest() {
        val hash=MessageDigest.getInstance("SHA-256")
        app.assets.open(HandTrackingEngine.MODEL_ASSET).use{input->val buffer=ByteArray(16384);while(true){val n=input.read(buffer);if(n<0)break;hash.update(buffer,0,n)}}
        assertEquals(HandTrackingEngine.MODEL_SHA256,hash.digest().joinToString(""){"%02x".format(it)})
    }
    @Test fun officialStaticHandFixturesRunRealOfflineInference() {
        detector().use { detector ->
            listOf("fixture-thumb-up.jpg" to 1,"fixture-two-right-hands.jpg" to 2).forEach { (name,expected)->
                val bitmap=instrumentation.context.assets.open("hand-tracking/$name").use{BitmapFactory.decodeStream(it)}
                assertNotNull("Missing labelled fixture $name",bitmap)
                try {
                    BitmapImageBuilder(bitmap).build().use{image->
                        val result=detector.detect(image)
                        assertEquals("Official static fixture $name",expected,result.landmarks().size)
                        result.landmarks().forEach{hand->assertEquals(21,hand.size);assertTrue(hand.all{p->p.x().isFinite()&&p.y().isFinite()&&p.z().isFinite()})}
                    }
                } finally {bitmap.recycle()}
            }
        }
    }
    @Test fun emptyStaticImageProducesNoHands() {
        val bitmap=Bitmap.createBitmap(480,360,Bitmap.Config.ARGB_8888).apply{eraseColor(0xff809080.toInt())}
        try{detector().use{detector->BitmapImageBuilder(bitmap).build().use{image->assertTrue(detector.detect(image).landmarks().isEmpty())}}}
        finally{bitmap.recycle()}
    }
    @Test fun disabledAndClosedEngineNeverExposeActionableInput() {
        val engine=HandTrackingEngine(app)
        assertEquals(HandTrackingState.DISABLED,engine.latestSnapshot().state)
        engine.setEnabled(true)
        assertEquals(HandTrackingState.STARTING,engine.latestSnapshot().state)
        assertNull(engine.latestSnapshot().frame)
        engine.setEnabled(false)
        assertEquals(HandTrackingState.DISABLED,engine.latestSnapshot().state)
        engine.close();engine.close();engine.setEnabled(true)
        assertEquals(HandTrackingState.DISABLED,engine.latestSnapshot().state)
        assertNull(engine.latestSnapshot().frame)
    }
}
