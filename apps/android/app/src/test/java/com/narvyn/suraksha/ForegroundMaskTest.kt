package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class ForegroundMaskTest {
    @Test fun cameraCropRotationRoundTripsEveryQuarterTurn(){
        val mappings=listOf(HandImageToView(0f,0f,1f,0f,0f,1f),HandImageToView(1.1f,-.2f,0f,1.4f,-1.2f,0f),HandImageToView(.9f,1.1f,-.8f,0f,0f,-1.2f))
        for(mapping in mappings)for(turn in 0..3){
            val transform=ForegroundMask.mapping(mapping,turn,640,480,16,24,608,432)
            for((x,y)in listOf(.2f to .3f,.5f to .7f,.8f to .1f)){
                val raw=HandImageToView.unrotate(x,y,turn)
                val view=mapping.map((16+raw.first*608)/640,(24+raw.second*432)/480)
                val mx=transform[0]+transform[2]*view.first+transform[4]*view.second
                val my=transform[1]+transform[3]*view.first+transform[5]*view.second
                assertEquals(x,mx,.00001f);assertEquals(y,my,.00001f)
            }
        }
    }
    @Test fun staleAndFutureMaskCannotHideCurrentCamera(){
        val mask=ForegroundMask(1,1,byteArrayOf(-1),floatArrayOf(0f,0f,1f,0f,0f,1f),1000,1)
        assertFalse(mask.freshAt(999));assertTrue(mask.freshAt(1000));assertTrue(mask.freshAt(1180));assertFalse(mask.freshAt(1181))
    }
    @Test fun probabilitiesPreserveCoverage(){assertArrayEquals(byteArrayOf(0,128.toByte(),255.toByte()),ForegroundMask.probabilities(floatArrayOf(0f,.5f,1f)))}
    @Test fun missingThinFingersRequestVisibilityAssistance(){
        val map=floatArrayOf(0f,0f,1f,0f,0f,1f)
        val hands=listOf(TrackedHand("hand",Handedness.LEFT,.9f,List(21){HandLandmark(.5f,.5f)}))
        assertTrue(ForegroundMask(1,1,byteArrayOf(-1),map,0,1).covers(hands))
        assertFalse(ForegroundMask(1,1,byteArrayOf(100),map,0,1).covers(hands))
        assertFalse(ForegroundMask(1,1,byteArrayOf(-1),map,0,1).covers(emptyList()))
    }
    @Test fun malformedProbabilitiesAndDimensionsAreRejected(){
        for(value in listOf(Float.NaN,Float.POSITIVE_INFINITY,-.1f,1.01f))assertTrue(runCatching{ForegroundMask.probabilities(floatArrayOf(value))}.isFailure)
        assertTrue(runCatching{ForegroundMask(513,1,ByteArray(513),floatArrayOf(0f,0f,1f,0f,0f,1f),0,0)}.isFailure)
    }
    @Test fun coveredPalmCannotConcealMissingPinchFingertips(){
        val hand=TrackedHand("pinch",Handedness.LEFT,.9f,List(21){HandLandmark(it/20f,0f)})
        val alpha=ByteArray(21){255.toByte()}.apply{this[4]=0;this[7]=0;this[8]=0}
        assertFalse(ForegroundMask(21,1,alpha,floatArrayOf(0f,0f,1f,0f,0f,1f),0,1).covers(listOf(hand)))
    }
}
