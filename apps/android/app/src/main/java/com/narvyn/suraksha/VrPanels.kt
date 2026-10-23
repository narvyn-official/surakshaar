package com.narvyn.suraksha

import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

internal data class VrAction(val id:String,val label:String)
internal data class VrPage(
    val revision:Long,val title:String,val eyebrow:String,val body:String,val actions:List<VrAction>,
    val footer:String="",val progress:Float=0f,val scene:VrSceneSpec?=null,val visualPaused:Boolean=false,
    val toolbar:List<VrAction> = listOf(VrAction("pause","Pause"),VrAction("replay","Replay"),VrAction("recenter","Recenter"))
)
internal data class VrSceneSpec(val model:String,val teachingKey:String?=null,val part:String?=null,val title:String="")

/** A semantic command and its visible bounds are generated together. No hidden gaze buttons. */
internal object VrPanelPainter {
    const val WIDTH=960;const val HEIGHT=1280
    data class Hit(val id:String,val rect:RectF)
    data class Painted(val bitmap:Bitmap,val hits:List<Hit>)
    private val ink=0xff18344c.toInt();private val teal=0xff0f766e.toInt()
    private fun text(canvas:Canvas,value:String,size:Float,x:Float,y:Float,width:Int,bold:Boolean=false,colour:Int=ink):Float {
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{color=colour;textSize=size;typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)}
        val layout=StaticLayout.Builder.obtain(value,0,value.length,paint,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(5f,1f).build()
        canvas.save();canvas.translate(x,y);layout.draw(canvas);canvas.restore();return layout.height.toFloat()
    }
    fun paint(page:VrPage):Painted {
        require(page.actions.size<=4)
        val bitmap=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888);val c=Canvas(bitmap)
        val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=0xfff7fbfc.toInt();c.drawRoundRect(0f,0f,WIDTH.toFloat(),HEIGHT.toFloat(),32f,32f,p)
        p.color=teal;c.drawRoundRect(38f,38f,38f+884f*page.progress.coerceIn(0f,1f),46f,4f,4f,p)
        var y=78f
        y+=text(c,page.eyebrow,25f,48f,y,864,colour=teal)+18
        y+=text(c,page.title,40f,48f,y,864,true)+22
        val bodyHeight=text(c,page.body,30f,48f,y,864);y+=bodyHeight+28
        val hits=mutableListOf<Hit>()
        val bottom=if(page.footer.isEmpty())1228f else 1175f
        val actionStart=maxOf(y,bottom-page.actions.size*148f)
        check(actionStart+page.actions.size*148f<=bottom+1f){"Headset page exceeds its text budget"}
        page.actions.forEachIndexed{i,action->
            val r=RectF(40f,actionStart+i*148f,920f,actionStart+i*148f+130f)
            p.color=if(action.id=="next"||action.id=="resume"||action.id=="restart-confirm")teal else 0xffe3eef2.toInt()
            c.drawRoundRect(r,20f,20f,p)
            val label=action.label
            val tp=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{color=if(p.color==teal)Color.WHITE else ink;textSize=30f;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)}
            val l=StaticLayout.Builder.obtain(label,0,label.length,tp,824).setIncludePad(false).setLineSpacing(3f,1f).build()
            check(l.height<=116){"Headset action label exceeds its text budget"}
            c.save();c.translate(68f,r.top+(130-l.height)/2);l.draw(c);c.restore()
            hits+=Hit(action.id,r)
        }
        if(page.footer.isNotEmpty())text(c,page.footer,23f,48f,1200f,864,colour=0xff4b667b.toInt())
        return Painted(bitmap,hits)
    }
    fun toolbar(actions:List<VrAction>):Painted {
        val bitmap=Bitmap.createBitmap(1200,180,Bitmap.Config.ARGB_8888);val c=Canvas(bitmap);val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val hits=mutableListOf<Hit>();val width=1200f/actions.size
        actions.forEachIndexed{i,a->
            val r=RectF(i*width+8,8f,(i+1)*width-8,172f)
            p.color=0xff163c50.toInt();c.drawRoundRect(r,22f,22f,p)
            val tp=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=40f;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)}
            val l=StaticLayout.Builder.obtain(a.label,0,a.label.length,tp,(width-44).toInt()).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build()
            c.save();c.translate(r.left+14,r.top+(r.height()-l.height)/2);l.draw(c);c.restore();hits+=Hit(a.id,r)
        }
        return Painted(bitmap,hits)
    }
}
