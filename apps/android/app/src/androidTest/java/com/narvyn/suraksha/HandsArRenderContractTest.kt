package com.narvyn.suraksha

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Actual scene GL resources/HUD without a camera. Explicit generated renderer-contract fixture. */
@RunWith(AndroidJUnit4::class)
class HandsArRenderContractTest {
    private val ins get()=InstrumentationRegistry.getInstrumentation()
    private fun field(value:Any,name:String)=value.javaClass.getDeclaredField(name).apply{isAccessible=true}
    @Test fun cameraAndLensTargetsAndOptionalPanelsBuildWithoutOpeningACamera(){
        ActivityScenario.launch(HandsArActivity::class.java).use{activity->
            lateinit var scene:HandsArSceneView
            activity.onActivity{host->CardboardBridge.initialise(host);CardboardBridge.useDefaultViewer();scene=HandsArSceneView(host,true)}
            try{CameraLensGpuTest().gpu{
                try{
                    scene.onSurfaceCreated(null,null);scene.onSurfaceChanged(null,2400,1080)
                    assertFalse(field(scene,"failed").getBoolean(scene))
                    assertTrue(field(scene,"fbo").getInt(scene)>0);assertTrue(field(scene,"lensFbo").getInt(scene)>0)
                    assertEquals(1440,field(scene,"logicalWidth").getInt(scene))
                    val paint=scene.javaClass.getDeclaredMethod("paintHud").apply{isAccessible=true}
                    for(panel in listOf(0,1,3,4,5)){
                        field(scene,"panel").setInt(scene,panel);field(scene,"hudKey").set(scene,"")
                        paint.invoke(scene)
                        val bitmap=field(scene,"hud").get(scene) as Bitmap
                        assertEquals(800,bitmap.width);assertEquals(600,bitmap.height)
                        File(ins.targetContext.getExternalFilesDir("validation"),"generated-ar-hud-$panel.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                        assertTrue(field(scene,"detailPages").getInt(scene)>=1)
                    }
                    assertNull("Renderer fixture must never start the ARCore camera",field(scene,"driver").get(scene))
                }finally{scene.javaClass.getDeclaredMethod("destroyStereo").apply{isAccessible=true}.invoke(scene)}
            }}finally{ins.runOnMainSync{scene.close()}}
        }
    }
}
