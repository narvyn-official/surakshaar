package com.narvyn.suraksha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TactileSurfaceTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun pressSelectionFocusAndDisableRemainVisuallyDistinct(){
        val surface=SoftSurfaceDrawable(context,interactive=true)
        surface.setBounds(0,0,360,160)
        fun render(vararg state:Int):IntArray{
            surface.state=state
            val bitmap=Bitmap.createBitmap(360,160,Bitmap.Config.ARGB_8888)
            surface.draw(Canvas(bitmap));return IntArray(360*160).also{bitmap.getPixels(it,0,360,0,0,360,160);bitmap.recycle()}
        }
        val normal=render(android.R.attr.state_enabled)
        for(state in listOf(android.R.attr.state_pressed,android.R.attr.state_selected,android.R.attr.state_focused))
            assertFalse("Interaction state $state must be visible",normal.contentEquals(render(android.R.attr.state_enabled,state)))
        assertFalse("Disabled state must be distinct",normal.contentEquals(render()))
        surface.alpha=0;val transparent=render(android.R.attr.state_enabled,android.R.attr.state_focused)
        assertTrue("Drawable opacity must apply to cached pixels",transparent.all{Color.alpha(it)==0})
    }
    @Test fun nativeTextAndControlEdgesRetainContrastWithoutShadow(){
        fun luminance(c:Int):Double=doubleArrayOf(.2126,.7152,.0722).mapIndexed{i,w->
            val channel=(when(i){0->Color.red(c);1->Color.green(c);else->Color.blue(c)})/255.0
            w*if(channel<=.04045)channel/12.92 else Math.pow((channel+.055)/1.055,2.4)
        }.sum()
        fun contrast(a:Int,b:Int):Double{val x=luminance(a);val y=luminance(b);return (maxOf(x,y)+.05)/(minOf(x,y)+.05)}
        for((ink,fill)in listOf(Palette.ink to Palette.surface,Palette.muted to Palette.canvas,Color.WHITE to Palette.blue,Palette.teal to Palette.tealBg,Palette.amber to Palette.amberBg,Palette.danger to Palette.dangerBg))
            assertTrue("Text contrast must remain readable",contrast(ink,fill)>=4.5)
        assertTrue("Control edge survives loss of shadow",contrast(Palette.controlLine,Palette.surface)>=3.0)
    }
}
