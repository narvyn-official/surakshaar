package com.narvyn.suraksha

import android.graphics.Bitmap
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
    private fun capture(name:String){
        ins.waitForIdleSync();Thread.sleep(700)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("Screenshot unavailable")
        File(ins.targetContext.getExternalFilesDir("validation"),"clothing-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
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
                    capture(name)
                }
                for(layer in listOf("skeleton","muscles","organs")){
                    activity.onActivity{body.layer(layer);body.change{it.copy(yaw=25f,pitch=0f,skinOverlay=true)}}
                    capture(layer)
                }
                // The same scene owner and source transform draw the lying and side-rolled casualty.
                for(roll in listOf(0f,90f)){
                    activity.onActivity{body.change{it.copy(layer="skin",supine=true,bodyRoll=roll,skinOverlay=false,yaw=25f,pitch=0f)};body.reset()}
                    capture("lying-${roll.toInt()}")
                }
                activity.onActivity{it.finish()}
            }
        }finally{prefs.edit().apply{if(hadChoice)putBoolean("detailed",old)else remove("detailed")}.commit()}
    }
}
