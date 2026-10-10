package com.narvyn.suraksha

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FirstAidCprPlacementTest {
    private fun content(path:String):File=generateSequence(File(System.getProperty("user.dir"))){it.parentFile}
        .map{File(it,"content/$path")}.first{it.isFile}
    private fun parts(manifest:String,read:(String)->ByteArray):List<AnatomyPart>{
        val list=JSONObject(manifest).getJSONArray("parts")
        return (0 until list.length()).map{list.getJSONObject(it)}.filter{it.getString("id") in setOf("FMA7163",FirstAidCprPlacement.STERNUM)}.map{p->
            val raw=GZIPInputStream(read(p.getString("file")).inputStream()).use{it.readBytes()}
            val buffer=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);buffer.position(4)
            val vertices=FloatArray(buffer.int*6);buffer.asFloatBuffer().get(vertices)
            fun values(key:String)=p.getJSONArray(key).let{a->FloatArray(a.length()){a.getDouble(it).toFloat()}}
            AnatomyPart(p.getString("id"),p.getString("name"),p.getString("layer"),"",values("bounds"),values("colour"),vertices)
        }
    }
    private fun bothPacks():List<List<AnatomyPart>>{
        val manifest=content("anatomy/manifest.json")
        val balanced=parts(manifest.readText()){File(manifest.parentFile.parentFile,it).readBytes()}
        val detailed=ZipFile(content("anatomy/optional/body-detail.zip")).use{z->parts(z.getInputStream(z.getEntry("manifest.json")).bufferedReader().readText()){z.getInputStream(z.getEntry(it)).use{stream->stream.readBytes()}}}
        return listOf(balanced,detailed)
    }
    @Test fun heelIsOnActualSkinOverLowerHalfOfSternumInBothShippedPacks(){
        for(parts in bothPacks()){
            val placement=FirstAidCprPlacement.forParts(parts)!!;val p=placement.surface
            val sternum=parts.first{it.id==FirstAidCprPlacement.STERNUM};val skin=parts.first{it.id=="FMA7163"}
            assertTrue(p[0] in sternum.bounds[0]..sternum.bounds[3])
            assertTrue(p[1]>sternum.bounds[1]+.02f&&p[1]<(sternum.bounds[1]+sternum.bounds[4])/2)
            val skinHit=AnatomyPicking.distance(skin.vertices,skin.bounds,floatArrayOf(p[0],p[1],p[2]+.01f),floatArrayOf(0f,0f,-1f))!!
            assertEquals("Contact must reach the actual triangle, not a guessed chest height",.01f,skinHit,.00001f)
            assertTrue(p[2] in .10f.. .13f)
            val hand=FirstAidTeachingProps.parts.first{it.id=="teaching:cpr"}
            assertEquals(FirstAidCprPlacement.HEEL_CLEARANCE,hand.bounds[2]+placement.translation[2]-p[2],.00001f)
            assertTrue("Hands must stay away from the neck",hand.bounds[4]+placement.translation[1]<(sternum.bounds[1]+sternum.bounds[4])/2)
            assertTrue("Thick floating palms obscure the face in a foreshortened view",hand.bounds[5]+placement.translation[2]-p[2]<.052f)
            val guide=FirstAidSceneGuide.forStep("adult-collapse-response","begin_cpr")!!
            for(t in listOf(0f,.08f,.2f,.5f,1f,3f)){
                val motion=FirstAidTeachingProps.offset(hand.id,t,guide)
                val heel=hand.bounds[2]+placement.translation[2]+motion[2]
                assertTrue("Illustrated heel must not float or penetrate the chest",heel>=p[2]-.00001f&&heel<=p[2]+.00301f)
            }
        }
    }
    @Test fun legacyChestLandmarkLeavesTheSternumSelectable(){
        val guide=FirstAidSceneGuide.forStep(FirstAidBay.SCENARIO_ID,"chest")!!
        assertEquals(setOf("chest-landmark"),guide.props)
        val ring=FirstAidTeachingProps.visible(guide).single()
        val origin=floatArrayOf(FirstAidCprPlacement.AUTHOR_X,FirstAidCprPlacement.AUTHOR_Y,.5f)
        assertNull("The landmark centre must not occlude a sternum selection",AnatomyPicking.distance(ring.vertices,ring.bounds,origin,floatArrayOf(0f,0f,-1f)))
    }
    @Test fun missingLandmarksDoNotInventAContactPoint(){
        assertNull(FirstAidCprPlacement.forParts(emptyList()))
        val parts=bothPacks().first()
        assertNull(FirstAidCprPlacement.forParts(parts.filter{it.id!=FirstAidCprPlacement.STERNUM}))
        assertNull(FirstAidCprPlacement.forParts(parts.filter{it.id!="FMA7163"}))
    }
}
