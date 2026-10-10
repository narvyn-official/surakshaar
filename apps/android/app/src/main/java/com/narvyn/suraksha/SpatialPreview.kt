package com.narvyn.suraksha
import android.content.Context
import android.graphics.*
import android.view.View
/** Original lightweight cover artwork. Activation opens the actual rotatable equipment model. */
class SpatialPreview(context:Context):View(context){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override fun onDraw(c:Canvas){super.onDraw(c);val k=height/164f;c.save();c.scale(k,k);val w=width/k
  p.color=Color.rgb(220,236,245);p.style=Paint.Style.FILL;c.drawRoundRect(RectF(0f,0f,w,164f),12f,12f,p)
  p.color=Color.rgb(189,212,226);p.strokeWidth=.7f
  for(i in 0..15){val x=i*w/15;c.drawLine(w/2+(x-w/2)*.25f,95f,x,164f,p)}
  for(y in listOf(98f,112f,133f,160f))c.drawLine(0f,y,w,y,p)
  val x=w*.69f;c.save();c.translate(x,8f);c.rotate(-10f,0f,80f)
  p.color=Color.rgb(161,188,204);c.drawOval(RectF(-37f,137f,43f,145f),p)
  p.shader=LinearGradient(-24f,0f,24f,0f,intArrayOf(0xffa53326.toInt(),0xffe26346.toInt(),0xffb73a29.toInt()),null,Shader.TileMode.CLAMP);c.drawRoundRect(RectF(-23f,43f,23f,138f),15f,15f,p);p.shader=null
  p.color=0xfffaf0d5.toInt();c.drawRoundRect(RectF(-19f,73f,19f,109f),2f,2f,p)
  p.color=0xff314c5d.toInt();c.drawRect(-5f,28f,5f,46f,p);c.drawRoundRect(RectF(-7f,23f,29f,29f),2f,2f,p)
  p.style=Paint.Style.STROKE;p.strokeWidth=4f;c.drawPath(Path().apply{moveTo(-4f,34f);cubicTo(-35f,25f,-36f,66f,-31f,117f)},p);p.style=Paint.Style.FILL
  p.color=0xff507b83.toInt();c.drawCircle(7f,37f,6f,p);c.restore()
  p.color=Palette.ink;p.textSize=11f;p.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);c.drawText("SPATIAL STUDIO",18f,28f,p)
  p.textSize=20f;c.drawText("Look closer.",18f,72f,p)
  p.color=Palette.muted;p.textSize=11f;p.typeface=Typeface.create("sans-serif",Typeface.NORMAL);c.drawText("Explore your equipment",18f,94f,p)
  p.color=Palette.blue;p.textSize=11f;c.drawText("Open 3D explorer  ›",18f,137f,p);c.restore()
 }
}
