package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class HandCameraMappingTest {
    private val identity=HandImageToView(0f,0f,1f,0f,0f,1f)
    private fun planes(width:Int,height:Int,y:Int=235)=listOf(
        HandYuvPlane(ByteBuffer.wrap(ByteArray(width*height){y.toByte()}),width,1),
        HandYuvPlane(ByteBuffer.wrap(ByteArray(((width+1)/2)*((height+1)/2)){128.toByte()}),(width+1)/2,1),
        HandYuvPlane(ByteBuffer.wrap(ByteArray(((width+1)/2)*((height+1)/2)){128.toByte()}),(width+1)/2,1))

    @Test fun basisCapturesRotationAndCropWithoutMirroring() {
        val portrait=HandImageToView.fromBasis(floatArrayOf(1.2f,-.1f,1.2f,1.1f,-.2f,-.1f))
        assertEquals(1,portrait.clockwiseQuarterTurns())
        val point=portrait.map(.5f,.5f)
        assertEquals(.5f,point.first,.00001f);assertEquals(.5f,point.second,.00001f)
        assertEquals(2,HandImageToView(1f,1f,-1f,0f,0f,-1f).clockwiseQuarterTurns())
        assertEquals(3,HandImageToView(0f,1f,0f,-1f,1f,0f).clockwiseQuarterTurns())
    }
    @Test fun allQuarterTurnsUndoRotationExactly() {
        val x=.2f;val y=.7f
        val rotated=listOf(x to y,(1-y) to x,(1-x) to (1-y),y to (1-x))
        rotated.forEachIndexed{turn,p->val raw=HandImageToView.unrotate(p.first,p.second,turn);assertEquals(x,raw.first,.00001f);assertEquals(y,raw.second,.00001f)}
    }
    @Test fun copyHonoursIndependentPlanePositionsPaddingAndPixelStrides() {
        val y=ByteBuffer.allocate(15).apply{position(2);put(2,16);put(3,235.toByte());put(7,81);put(8,145.toByte())}
        val u=ByteBuffer.allocate(5).apply{position(1);put(1,128.toByte())}
        val v=ByteBuffer.allocate(7).apply{position(3);put(3,128.toByte())}
        val copy=HandYuvCopy.copy(2,2,0,0,2,2,listOf(HandYuvPlane(y,5,1),HandYuvPlane(u,3,2),HandYuvPlane(v,3,2)))
        val argb=copy.rotatedArgb(0).third
        assertEquals(0xff000000.toInt(),argb[0]);assertEquals(0xffffffff.toInt(),argb[1])
        assertEquals(2,y.position());assertEquals(1,u.position());assertEquals(3,v.position())
    }
    @Test fun oddCropsAndAllRotationsStayInsideBoundedCopy() {
        val copy=HandYuvCopy.copy(7,5,1,1,7,5,planes(7,5))
        assertEquals(6,copy.width);assertEquals(4,copy.height)
        val corner=copy.rawNormalized(0f,0f)
        assertEquals(1f/7,corner.first,.00001f);assertEquals(.2f,corner.second,.00001f)
        (0..3).forEach{turn->val out=copy.rotatedArgb(turn);assertEquals(24,out.third.size);assertEquals(if(turn%2==0)6 else 4,out.first)}
    }
    @Test fun largeImageDownsamplesBeforeReleasingCameraBuffer() {
        val copy=HandYuvCopy.copy(1920,1080,0,0,1920,1080,planes(1920,1080))
        assertEquals(480,copy.width);assertEquals(270,copy.height)
        assertTrue(copy.rotatedArgb(1).third.size<=480*480)
    }
    @Test fun truncatedPlanesInvalidStrideAndOversizeAreRejected() {
        assertThrows(IllegalArgumentException::class.java){HandYuvCopy.copy(4,4,0,0,4,4,planes(2,2))}
        assertThrows(IllegalArgumentException::class.java){HandYuvCopy.copy(4,4,0,0,4,4,planes(4,4).map{HandYuvPlane(it.buffer,1,it.pixelStride)})}
        assertThrows(IllegalArgumentException::class.java){HandYuvCopy.copy(9000,2,0,0,9000,2,planes(1,1))}
        assertThrows(IllegalArgumentException::class.java){HandYuvCopy.copy(4,4,0,0,5,4,planes(4,4))}
    }
    @Test fun malformedLandmarksAndOffscreenHandsNeverBecomeVisibleTargets() {
        val valid=List(21){HandLandmark(.5f,.5f,0f)}
        assertEquals(21,HandResultMapping.landmarks(valid,identity,0,100,100)?.size)
        assertNull(HandResultMapping.landmarks(valid.drop(1),identity,0,100,100))
        assertNull(HandResultMapping.landmarks(valid.toMutableList().apply{this[8]=HandLandmark(Float.NaN,.5f)},identity,0,100,100))
        assertNull(HandResultMapping.landmarks(valid.toMutableList().apply{this[8]=HandLandmark(1.1f,.5f)},identity,0,100,100))
        assertNull(HandResultMapping.landmarks(valid,HandImageToView(1f,0f,1f,0f,0f,1f),0,100,100))
    }
    @Test fun cropRotationMappingUsesOriginalCameraSpace() {
        val mapped=HandResultMapping.landmarks(List(21){HandLandmark(.25f,.75f)},identity,1,100,200,20,40,40,80)!![0]
        assertEquals(.5f,mapped.x,.00001f);assertEquals(.5f,mapped.y,.00001f)
    }
    @Test fun invalidAffineTransformsAreRejected() {
        assertThrows(IllegalArgumentException::class.java){HandImageToView(0f,0f,0f,0f,0f,0f)}
        assertThrows(IllegalArgumentException::class.java){HandImageToView(Float.NaN,0f,1f,0f,0f,1f)}
    }
    @Test fun staleAndFutureSnapshotsRemoveActionableLandmarks() {
        val frame=HandTrackingFrame(1000,1050,1,emptyList())
        val snap=HandTrackingSnapshot(HandTrackingState.TRACKING,"active",frame)
        assertSame(frame,snap.freshAt(1250).frame)
        assertNull(snap.freshAt(1251).frame)
        assertNull(snap.freshAt(999).frame)
    }
}
