package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class InspectionMagnificationTest {
    private fun identity()=FloatArray(16){if(it%5==0)1f else 0f}
    private fun transform(m:FloatArray,p:FloatArray)=FloatArray(4){row->(0..3).sumOf{col->(m[col*4+row]*p[col]).toDouble()}.toFloat()}
    @Test fun finiteBoundsAndResetInvalidateOldSnapshotsOnlyWhenViewActuallyChanges() {
        val original=InspectionMagnification.Snapshot()
        assertSame(original,original.withFactor(Float.NaN))
        assertSame(original,original.withFactor(Float.POSITIVE_INFINITY))
        assertSame(original,original.withFactor(-1f))
        assertSame(original,original.withFactor(1f))
        val enlarged=original.withFactor(100f)
        assertEquals(2.5f,enlarged.factor,0f);assertEquals(1L,enlarged.serial)
        val reset=enlarged.withFactor(1f)
        assertEquals(1f,reset.factor,0f);assertEquals(2L,reset.serial)
    }
    @Test fun cameraCropAndProjectionCoincideAtEveryCornerAndCentre() {
        for(zoom in listOf(1f,1.2f,2f,2.5f)){
            val projection=identity();InspectionMagnification.project(projection,zoom)
            val cropped=InspectionMagnification.cameraCorners(zoom)
            val full=InspectionMagnification.cameraCorners(1f)
            for(i in 0..3){
                val p=transform(projection,floatArrayOf(cropped[i*2],cropped[i*2+1],.3f,1f))
                assertEquals(full[i*2],p[0],.00001f);assertEquals(full[i*2+1],p[1],.00001f)
                assertEquals(.3f,p[2],0f);assertEquals(1f,p[3],0f)
            }
            assertArrayEquals(floatArrayOf(0f,0f,.3f,1f),transform(projection,floatArrayOf(0f,0f,.3f,1f)),.00001f)
        }
    }
    @Test fun offCentreViewportZoomPreservesItsCentreAndDepth() {
        val original=floatArrayOf(2f,0f,0f,0f,0f,3f,0f,0f,.1f,.2f,-1.1f,-1f,0f,0f,-.2f,0f)
        val zoomed=original.copyOf();InspectionMagnification.project(zoomed,2f,-.4f,.3f)
        val point=floatArrayOf(.2f,.1f,-3f,1f)
        val before=transform(original,point);val after=transform(zoomed,point)
        assertEquals(-.4f+2*(before[0]/before[3]+.4f),after[0]/after[3],.00001f)
        assertEquals(.3f+2*(before[1]/before[3]-.3f),after[1]/after[3],.00001f)
        assertEquals(before[2],after[2],0f);assertEquals(before[3],after[3],0f)
        val reset=original.copyOf();InspectionMagnification.project(reset,1f)
        assertArrayEquals(original,reset,0f)
    }
}
