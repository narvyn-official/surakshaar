package com.narvyn.suraksha

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EquipmentExplorerFlowTest {
    private fun all(view:View):List<View> = listOf(view)+if(view is ViewGroup)(0 until view.childCount).flatMap{all(view.getChildAt(it))}else emptyList()
    private fun value(host:EquipmentActivity,name:String)=EquipmentActivity::class.java.getDeclaredField(name).apply{isAccessible=true}.getFloat(host)
    @Test fun focusRotationAndBackKeepTheModelVisibleWithoutPagingItAway(){
        val ins=InstrumentationRegistry.getInstrumentation()
        val hi=Store(ins.targetContext).use{it.hi}
        fun text(en:String,hindi:String)=if(hi)hindi else en
        val landscape=InstrumentationRegistry.getArguments().getString("orientation")=="landscape"
        ActivityScenario.launch<EquipmentActivity>(Intent(ins.targetContext,EquipmentActivity::class.java).putExtra("moduleId","fire")).use{scenario->
            if(landscape){
                scenario.onActivity{it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
                var ready=false
                repeat(50){scenario.onActivity{ready=it.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE};if(!ready)Thread.sleep(100)}
                assertTrue(ready)
            }
            onView(withContentDescription(text("Equipment view controls","उपकरण दृश्य नियंत्रण"))).perform(click())
            scenario.onActivity{host->
                val scene=all(host.window.decorView).filterIsInstance<GLSurfaceView>().single()
                val bounds=Rect();assertTrue(scene.getGlobalVisibleRect(bounds))
                assertTrue("Open controls must retain model width",bounds.width()>=host.dp(120))
                assertTrue("Open controls must retain model height",bounds.height()>=host.dp(96))
                var parent=scene.parent
                while(parent is View){assertFalse("The model must never sit inside content pages",parent is PagedPanel);parent=parent.parent}
                all(host.window.decorView).filterIsInstance<Button>().filter{it.isShown}.forEach{
                    assertTrue("${it.contentDescription} keeps touch height",it.height>=host.dp(48))
                }
            }
            onView(withContentDescription(text("Label","लेबल"))).perform(click())
            scenario.onActivity{assertEquals(.256f,value(it,"focusY"),.001f);assertEquals(.46f,value(it,"distance"),.001f)}
            capture("label",landscape)
            onView(withContentDescription(text("Gauge","गेज"))).perform(click())
            scenario.onActivity{assertEquals(.455f,value(it,"focusY"),.001f);assertEquals(.38f,value(it,"distance"),.001f)}
            capture("gauge",landscape)
            onView(withContentDescription(text("Rotate right","दाएँ घुमाएँ"))).perform(click())
            scenario.onActivity{assertEquals(15f,value(it,"yaw"),.001f)}
            pressBack()
            scenario.onActivity{host->assertFalse(host.isFinishing);assertTrue(all(host.window.decorView).none{it.tag=="equipment-view-dock"})}
            onView(withContentDescription(text("Reset equipment view","उपकरण दृश्य रीसेट करें"))).perform(click())
            scenario.onActivity{assertEquals(0f,value(it,"yaw"),.001f);assertEquals(1.25f,value(it,"distance"),.001f)}
            capture("whole",landscape)
        }
    }
    private fun capture(name:String,landscape:Boolean){
        val ins=InstrumentationRegistry.getInstrumentation();ins.waitForIdleSync();Thread.sleep(300)
        val bitmap=ins.uiAutomation.takeScreenshot()?:error("No emulator screenshot")
        File(ins.targetContext.getExternalFilesDir("validation"),"equipment-$name-${if(landscape)"landscape"else"portrait"}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
