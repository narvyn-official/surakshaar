package com.narvyn.suraksha

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real native renderer captures. No AR tracking or clinical validation is inferred from these. */
@RunWith(AndroidJUnit4::class)
class AnatomyClothingFlowTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun waitFor(test:()->Boolean){repeat(300){if(test())return;Thread.sleep(100)};error("Model did not finish loading")}
    private fun capture(name:String,body:AnatomyScene?=null):Int{
        ins.waitForIdleSync();Thread.sleep(700)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        File(ins.targetContext.getExternalFilesDir("validation"),"clothing-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        // Count the garment's blue fabric only inside the GL viewport, excluding app controls.
        // Every anatomical source layer uses warm colours, so this checks actual rendered output.
        var fabric=0
        if(body!=null){
            val at=IntArray(2);var width=0;var height=0
            ins.runOnMainSync{body.getLocationOnScreen(at);width=body.width;height=body.height}
            for(y in maxOf(0,at[1]) until minOf(bitmap.height,at[1]+height) step 2)
                for(x in maxOf(0,at[0]) until minOf(bitmap.width,at[0]+width) step 2){
                    val pixel=bitmap.getPixel(x,y);val r=Color.red(pixel);val g=Color.green(pixel);val b=Color.blue(pixel)
                    if(r<g*.78f&&g<b*.9f&&g-r>8&&b-g>6)fabric++
                }
        }
        bitmap.recycle()
        return fabric
    }
    @Test fun detailedBodyIsCoveredFromEveryInspectionAngleAndRetainsUsefulLayers(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val context=ins.targetContext;val prefs=context.getSharedPreferences("anatomy-quality",0)
        val hadChoice=prefs.contains("detailed");val old=prefs.getBoolean("detailed",false)
        // Exercise the shipped first-use default, retaining the prepared offline pack as app content.
        prefs.edit().remove("detailed").commit()
        try {
            ActivityScenario.launch(AnatomyActivity::class.java).use{activity->
                var scene:AnatomyScene?=null
                waitFor{activity.onActivity{scene=views(it.window.decorView).filterIsInstance<AnatomyScene>().firstOrNull()};scene!=null}
                val body=scene!!
                assertEquals("Detailed",body.data.manifest.getString("loadedQuality"))
                assertTrue(body.data.manifest.getInt("triangleCount")>900_000)
                for(part in body.data.parts.filter{it.layer=="skin"})for(i in part.vertices.indices step 18)
                    assertFalse("Protected skin reached the runtime",AnatomyClothing.coveredTriangle(part.vertices,i))
                val angles=listOf("front" to (0f to 0f),"back" to (180f to 0f),"side" to (90f to 0f),
                    "low" to (0f to -65f),"high" to (180f to 65f))
                for((name,angle)in angles){
                    activity.onActivity{body.layer("skin");body.change{it.copy(yaw=angle.first,pitch=angle.second)}}
                    assertTrue("Skin loses visible shorts at $name",capture(name,body)>25)
                }
                for(layer in listOf("skeleton","muscles","organs")){
                    activity.onActivity{body.layer(layer);body.change{it.copy(yaw=25f,pitch=0f,skinOverlay=false)}}
                    val bare=capture("$layer-bare",body)
                    assertTrue("Shorts remain visible on $layer without skin: $bare pixels",bare<8)
                    activity.onActivity{body.change{it.copy(skinOverlay=true)}}
                    assertTrue("Skin overlay loses its opaque coverage on $layer",capture("$layer-skin-overlay",body)>bare+25)
                    activity.onActivity{body.change{it.copy(hidden=body.data.parts.filter{p->p.layer=="skin"}.map{p->p.id}.toSet())}}
                    assertTrue("Hidden skin leaves floating shorts on $layer",capture("$layer-hidden-skin",body)<8)
                    val selected=body.data.parts.first{it.layer==layer}.id
                    activity.onActivity{body.change{it.copy(hidden=emptySet(),selected=selected,isolated=true,skinOverlay=true)}}
                    assertTrue("Isolated $layer leaves floating shorts",capture("$layer-isolated",body)<8)
                }
                // The same scene owner and source transform draw the lying and side-rolled casualty.
                for(roll in listOf(0f,90f)){
                    activity.onActivity{body.change{it.copy(layer="skin",selected=null,isolated=false,hidden=emptySet(),supine=true,bodyRoll=roll,skinOverlay=false,yaw=25f,pitch=0f)};body.reset()}
                    assertTrue("Lying skin loses its garment at roll $roll",capture("lying-${roll.toInt()}",body)>25)
                }
                activity.onActivity{it.finish()}
            }
        }finally{prefs.edit().apply{if(hadChoice)putBoolean("detailed",old)else remove("detailed")}.commit()}
    }
}
