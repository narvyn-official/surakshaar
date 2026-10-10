package com.narvyn.suraksha

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.io.FileInputStream
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AnatomyDescriptionFlowTest {
    private var oldDetailed:Boolean?=null
    private var qualitySet=false
    @Before fun useDetailedGeometry(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val prefs=InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("anatomy-quality",0)
        oldDetailed=if(prefs.contains("detailed"))prefs.getBoolean("detailed",false)else null
        qualitySet=true;prefs.edit().remove("detailed").commit()
    }
    @After fun restoreGeometryPreference(){
        if(!qualitySet)return
        val prefs=InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("anatomy-quality",0)
        prefs.edit().apply{oldDetailed?.let{putBoolean("detailed",it)}?:remove("detailed")}.commit()
    }

    private var previousScale:String?=null
    private fun shell(command:String)=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use{FileInputStream(it.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim()}
    @Before fun configure(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        InstrumentationRegistry.getArguments().getString("fontScale")?.let{require(it in setOf("1.3","2.0"));previousScale=shell("settings get system font_scale");shell("settings put system font_scale $it")}
    }
    @After fun restore(){previousScale?.let{shell(if(it=="null")"settings delete system font_scale"else"settings put system font_scale $it")}}

    private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
    private fun loaded(activity:ActivityScenario<AnatomyActivity>):AnatomyScene {
        repeat(150){
            var scene:AnatomyScene?=null
            activity.onActivity{scene=views(it.window.decorView).filterIsInstance<AnatomyScene>().firstOrNull()}
            if(scene!=null){assertEquals("Detailed",scene!!.data.manifest.getString("loadedQuality"));return scene!!}
            Thread.sleep(100)
        }
        error("Body model did not load")
    }
    private fun waitForRenderedModel(scene: AnatomyScene) {
        var lastCode = -1; var lastColours = 0
        repeat(30) {
            val finished = CountDownLatch(1); val result = AtomicInteger(-1); val colours = AtomicInteger(0)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val surface = views(scene).filterIsInstance<GLSurfaceView>().single()
                if (surface.width <= 0 || surface.height <= 0) { finished.countDown(); return@runOnMainSync }
                val bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(surface, bitmap, { code ->
                    result.set(code)
                    if (code == PixelCopy.SUCCESS) {
                        val samples = HashSet<Int>()
                        for (x in 0 until bitmap.width step 8) for (y in 0 until bitmap.height step 8) samples.add(bitmap.getPixel(x, y))
                        colours.set(samples.size)
                    }
                    bitmap.recycle(); finished.countDown()
                }, Handler(Looper.getMainLooper()))
            }
            assertTrue("GL pixel copy did not finish", finished.await(2, TimeUnit.SECONDS))
            lastCode = result.get(); lastColours = colours.get()
            if (lastCode == PixelCopy.SUCCESS && lastColours >= 8) return
            Thread.sleep(100)
        }
        fail("The model viewport must contain rendered geometry: PixelCopy=$lastCode, sampled colours=$lastColours")
    }
    private fun screenshot(name:String){
        val ins=InstrumentationRegistry.getInstrumentation();ins.waitForIdleSync();Thread.sleep(700)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("No screenshot")
        File(ins.targetContext.getExternalFilesDir("validation"),"learning-$name-${ins.targetContext.resources.configuration.fontScale}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    @Test fun cancelledLoadingKeepsVerifiedDetailedPackAvailable(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=AnatomyPackStore(context)
        assertNull(store.prepareDefaultQuality{false})
        assertTrue(store.detailedSelected())
        val result=java.util.concurrent.atomic.AtomicReference<Throwable?>()
        val worker=Thread{
            Thread.currentThread().interrupt()
            try{AnatomyData.load(context)}catch(error:Throwable){result.set(error)}
        }
        worker.start();worker.join(10_000)
        assertFalse("Cancelled model load must finish promptly",worker.isAlive)
        assertTrue(result.get() is java.util.concurrent.CancellationException)
        assertTrue("Cancellation is not pack corruption",store.installed())
        assertTrue("Keep the selected quality after cancellation",store.detailedSelected())
    }
    @Test fun selectionShowsAnatomyAndClearsWhenLayerChanges(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("preferences",0);val wasHi=prefs.getBoolean("hi",false)
        prefs.edit().putBoolean("hi",false).commit()
        try { ActivityScenario.launch(AnatomyActivity::class.java).use{activity->
            val scene=loaded(activity)
            activity.onActivity{scene.select("FMA7088",true)}
            onView(withTagValue(org.hamcrest.Matchers.`is`("anatomy-part-name" as Any))).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText(AnatomyKnowledge.info("FMA7088")!!.title)))
            activity.onActivity{host->
                val text=views(host.window.decorView).filterIsInstance<TextView>().single{it.tag=="anatomy-part-description"}
                assertEquals(AnatomyKnowledge.info("FMA7088")!!.description,text.text.toString())
                val bounds=Rect();assertTrue(scene.getGlobalVisibleRect(bounds));assertTrue(bounds.height()>=host.dp(120))
            }
            waitForRenderedModel(scene)
            screenshot("heart-selected")
            onView(withContentDescription("About selected part")).perform(click())
            onView(withText("Where it is")).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()))
            onView(withText(AnatomyKnowledge.info("FMA7088")!!.function)).perform(revealOnPage()).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()))
            screenshot("heart-description")
            onView(withText("Close")).perform(click())
            activity.onActivity{host->
                scene.layer("skeleton")
                assertNull(scene.controls.selected)
                val all=views(host.window.decorView)
                assertEquals("Tap a body part",all.filterIsInstance<TextView>().single{it.tag=="anatomy-part-name"}.text.toString())
                assertFalse(all.filterIsInstance<Button>().single{it.tag=="anatomy-details"}.isEnabled)
                scene.select("FMA7485",true)
            }
            activity.recreate()
            val restored=loaded(activity)
            activity.onActivity{host->
                assertSame("Recreation reuses verified geometry instead of duplicating the detailed pack",scene.data,restored.data)
                assertEquals("FMA7485",restored.controls.selected)
                assertEquals(AnatomyKnowledge.info("FMA7485")!!.description,views(host.window.decorView).filterIsInstance<TextView>().single{it.tag=="anatomy-part-description"}.text.toString())
            }
        }}finally{prefs.edit().putBoolean("hi",wasHi).commit()}
    }
    @Test fun hindiDescriptionMatchesSelectionAndObservationCannotUseHiddenPart(){
        assumeTrue(Build.HARDWARE in listOf("ranchu","goldfish")||Build.MODEL.contains("sdk_gphone"))
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("preferences",0);val wasHi=prefs.getBoolean("hi",false)
        prefs.edit().putBoolean("hi",true).commit()
        try{ActivityScenario.launch<AnatomyActivity>(Intent(context,AnatomyActivity::class.java).putExtra("observation",true).putExtra("partId","FMA7088")).use{activity->
            val scene=loaded(activity)
            activity.onActivity{host->
                scene.select("FMA7088")
                val all=views(host.window.decorView)
                assertEquals(AnatomyKnowledge.info("FMA7088","hi")!!.description,all.filterIsInstance<TextView>().single{it.tag=="anatomy-part-description"}.text.toString())
                val confirm=all.filterIsInstance<Button>().single{it.text.toString()=="Use this structure"}
                assertTrue(confirm.isEnabled)
                scene.layer("skeleton")
                assertFalse("Changing layer must clear observation eligibility",confirm.isEnabled)
                scene.select("FMA7485")
            }
            activity.recreate()
            val restored=loaded(activity)
            assertEquals("Recreation preserves exploration instead of re-focusing intent target","FMA7485",restored.controls.selected)
            waitForRenderedModel(restored)
            screenshot("hindi-sternum")
        }}finally{prefs.edit().putBoolean("hi",wasHi).commit()}
    }
}
