package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class HeadsetArRefinementTest {
    @Test fun headsetCaptureUsesTheCameraAspectInsteadOfCroppingToOneEye(){
        val source=HeadsetCaptureLayout.sourceWidth(1080,640,480)
        assertEquals(1440,source)
        assertTrue("Two hands need the captured horizontal field, not one physical eye viewport",source>2400/2)
        assertEquals(1920,HeadsetCaptureLayout.sourceWidth(1080,1920,1080))
        try{HeadsetCaptureLayout.sourceWidth(1080,0,480);fail("Invalid camera geometry must fail closed")}catch(_:IllegalArgumentException){}
    }
    private fun projection(x:Float=1f,y:Float=1f,ox:Float=0f,oy:Float=0f)=FloatArray(16).also{
        it[0]=x;it[5]=y;it[8]=ox;it[9]=oy;it[10]=-1.01f;it[11]=-1f;it[14]=-.1f
    }
    @Test fun liftIsBoundedInterpolatedAndCancelledWithTheWholePose(){
        val target=HandModelPose(y=.8f)
        val motion=HandModelMotion();val first=motion.advance(target,.01f)
        assertTrue(first.y in .01f.. .79f)
        motion.snap(HandModelPose());assertEquals(0f,motion.visible.y,0f)
        assertEquals(1f,HandModelPose(y=9f).bounded().y,0f)
        assertEquals(0f,HandModelPose(y=-1f).bounded().y,0f)
    }
    @Test fun scaleAndYawKeepTheLiftedGripAtTheNewMidpoint(){
        val base=HandModelPose(x=.2f,scale=.5f,y=.2f)
        val result=HandModelTransforms.twoHand3d(base,floatArrayOf(.1f,.3f,0f),floatArrayOf(.1f,.5f,.1f),2f,90f)
        assertEquals(.3f,result.y,.0001f)
        assertEquals(.1f,result.x,.0001f);assertEquals(-.1f,result.z,.0001f)
        assertEquals(base,HandModelTransforms.twoHand3d(base,floatArrayOf(Float.NaN,0f,0f),floatArrayOf(0f,0f,0f),1f,0f))
    }
    @Test fun lensMappingPreservesTheOpticalRayForAsymmetricEyeProjections(){
        val source=projection(2f,3f,.1f,-.2f)
        val eye=projection(1.5f,2f,-.15f,.1f)
        val map=CameraLensMapping.between(source,eye)
        // camera axis projects at -projection[8,9]. Each eye must sample that same ray.
        val centre=map.sourceUv((1f-eye[8])/2,(1f-eye[9])/2)
        assertEquals((1f-source[8])/2,centre.first,.00001f)
        assertEquals((1f-source[9])/2,centre.second,.00001f)
        for(x in listOf(.2f,.5f,.8f)){
            val sampled=map.sourceUv(x,.5f)
            val ray=(2*x-1+eye[8])/eye[0]
            assertEquals((source[0]*ray-source[8]+1)/2,sampled.first,.00001f)
        }
    }
    @Test fun noClampingOrStretchingOutsideCameraFovAndInvalidOpticsRejected(){
        val map=CameraLensMapping.between(projection(3f,3f),projection())
        assertTrue(map.sourceUv(1f,1f).first>1f)
        try{CameraLensMapping.between(projection(Float.NaN),projection());fail("NaN must fail closed")}catch(_:IllegalArgumentException){}
        try{CameraLensMapping.between(projection(),projection(0f));fail("Zero projection must fail closed")}catch(_:IllegalArgumentException){}
    }
    @Test fun segmentationCannotDelayEveryHandSampleOrRunOutsideItsBudget(){
        val schedule=ForegroundInferenceSchedule()
        assertTrue(schedule.shouldRun(1000,15,true))
        assertFalse(schedule.shouldRun(1050,15,true))
        assertTrue(schedule.shouldRun(1120,15,true))
        assertFalse(schedule.shouldRun(1500,85,true))
        assertFalse(schedule.shouldRun(1510,250,true))
        schedule.reset();assertTrue(schedule.shouldRun(1510,5,true))
    }
    @Test fun handRevealIsLocalAndRequiresFreshStableConfidentFullLandmarks(){
        val points=List(21){i->HandLandmark(.4f+(i%4)*.018f,.4f+(i/4)*.02f)}
        fun frame(at:Long=1000,confidence:Float=.9f,stable:Boolean=true)=HandTrackingFrame(at,at,1,listOf(TrackedHand("one",Handedness.RIGHT,confidence,points)),stable)
        assertEquals(1,HandCameraReveal.regions(frame(),1050).size)
        assertTrue(HandCameraReveal.regions(frame(),1201).isEmpty())
        assertTrue(HandCameraReveal.regions(frame(),999).isEmpty())
        assertTrue(HandCameraReveal.regions(frame(confidence=.4f),1050).isEmpty())
        assertTrue(HandCameraReveal.regions(frame(stable=false),1050).isEmpty())
        val mask=HandCameraReveal.rasterize(HandCameraReveal.regions(frame(),1050),160,160,1f)
        assertTrue((mask[(.44f*160).toInt()*160+(.43f*160).toInt()].toInt() and 255)>240)
        assertEquals(0,mask[0].toInt());assertEquals(0,mask.last().toInt())
        assertTrue(mask.count{it.toInt()!=0}<mask.size/4)
    }
}
