package com.narvyn.suraksha

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable

/** Original native viewer symbol; no external asset or icon font. */
internal class HeadsetIcon(private val ink:Int):Drawable(){
    private val pen=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=ink;style=Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    override fun draw(canvas:Canvas){
        val mark=canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat());canvas.scale(bounds.width()/24f,bounds.height()/24f)
        val outline=Path().apply{moveTo(4f,6f);lineTo(20f,6f);quadTo(22f,6f,22f,9f);lineTo(21f,17f);quadTo(20.5f,19f,18.5f,19f);lineTo(15f,19f);lineTo(12f,16f);lineTo(9f,19f);lineTo(5.5f,19f);quadTo(3.5f,19f,3f,17f);lineTo(2f,9f);quadTo(2f,6f,4f,6f)}
        canvas.drawPath(outline,pen);canvas.drawOval(RectF(5f,9f,10f,14f),pen);canvas.drawOval(RectF(14f,9f,19f,14f),pen)
        canvas.drawLine(6f,3f,18f,3f,pen);canvas.restoreToCount(mark)
    }
    override fun setAlpha(alpha:Int){pen.alpha=alpha;invalidateSelf()}
    override fun setColorFilter(filter:ColorFilter?){pen.colorFilter=filter;invalidateSelf()}
    @Deprecated("Drawable opacity") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
internal fun Context.headsetIcon(ink:Int=Palette.teal,size:Int=24)=HeadsetIcon(ink).apply{setBounds(0,0,dp(size),dp(size))}

/** Native keyboard focus stays visible even on the shell's quiet tonal controls. */
internal fun Context.headsetSurface(fill:Int,border:Int=Palette.line,radius:Int=16):Drawable {
    val focused=shape(fill,radius,Palette.teal).apply{setStroke(dp(2),Palette.teal)}
    val states=StateListDrawable().apply{
        addState(intArrayOf(android.R.attr.state_focused),focused)
        addState(intArrayOf(),shape(fill,radius,border))
    }
    return RippleDrawable(ColorStateList.valueOf(0x200f766e),states,shape(android.graphics.Color.WHITE,radius))
}
