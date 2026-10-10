package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class AnatomyClothingTest {
    private fun triangle(y:Float,x:Float=0f)=floatArrayOf(x-.01f,y-.01f,.10f,0f,0f,1f,
        x+.01f,y-.01f,.10f,0f,0f,1f,x,y+.01f,.10f,0f,0f,1f)
    @Test fun coveredExternalSkinIsRemovedButAirwayChestLimbsAndHandsRemain(){
        val skin=AnatomyPart("skin","Skin","skin","",floatArrayOf(-.3f,0f,-.15f,.3f,1.72f,.15f),floatArrayOf(.5f,.3f,.2f),
            triangle(.85f)+triangle(1.30f)+triangle(1.64f)+triangle(.35f)+triangle(.85f,.28f)+triangle(1.025f,.20f)+triangle(1.07f,.18f))
        val result=AnatomyClothing.protectedSkin(skin)
        assertEquals(6*18,result.vertices.size)
        for(i in result.vertices.indices step 18)assertFalse(AnatomyClothing.coveredTriangle(result.vertices,i))
        assertArrayEquals(skin.bounds,result.bounds,0f)
        assertSame("Clothing must not alter an organ's source geometry",skin.vertices,AnatomyClothing.protectedSkin(skin.copy(layer="organs")).vertices)
    }
    @Test fun shortsBlockRaysFromEverySideAndBelowOrAbove(){
        val centre=floatArrayOf(0f,.88f,0f)
        for(elevation in listOf(-90,-60,-30,0,30,60,90))for(azimuth in 0 until 360 step 15){
            val el=Math.toRadians(elevation.toDouble());val az=Math.toRadians(azimuth.toDouble())
            val d=floatArrayOf((cos(el)*sin(az)).toFloat(),sin(el).toFloat(),(cos(el)*cos(az)).toFloat())
            val origin=FloatArray(3){centre[it]+2*d[it]};val ray=FloatArray(3){-d[it]}
            assertTrue("Uncovered angle $azimuth/$elevation",AnatomyClothing.parts.any{AnatomyPicking.distance(it.vertices,it.bounds,origin,ray)!=null})
        }
    }
    @Test fun clothesHaveBoundedFiniteGeometryAndDoNotCoverCoreLearningLandmarks(){
        val parts=AnatomyClothing.parts
        assertEquals(parts.size,parts.map{it.id}.toSet().size)
        assertTrue(parts.sumOf{it.vertices.size}/18<4000)
        for(p in parts){
            assertEquals("clothing",p.layer);assertTrue(p.vertices.all{it.isFinite()});assertEquals(0,p.vertices.size%18)
            for(i in p.vertices.indices step 6){val n=sqrt(p.vertices[i+3].pow(2)+p.vertices[i+4].pow(2)+p.vertices[i+5].pow(2));assertEquals(1f,n,.0001f)}
        }
        for(region in listOf("HEAD","CHEST","RIGHT_FOREARM","LEFT_FOREARM","RIGHT_LOWER_LEG")){
            val c=CasualtyMarkers.centres.getValue(region);val origin=floatArrayOf(c[0],c[1],1f)
            assertFalse("Clothing hides $region",parts.any{AnatomyPicking.distance(it.vertices,it.bounds,origin,floatArrayOf(0f,0f,-1f))!=null})
        }
    }
    @Test fun posedShortsAndSkinUseTheSameSourceCoordinates(){
        for(roll in listOf(0f,90f,-90f)){
            val p=CasualtyGeometry.point(floatArrayOf(0f,.88f,.16f),roll,.35f)
            assertTrue(p.all{it.isFinite()})
            val bounds=CasualtyGeometry.bounds(AnatomyClothing.parts.first().bounds,roll,.35f)
            assertTrue((0..2).all{p[it] in bounds[it]-.03f..bounds[it+3]+.03f})
        }
    }
}
