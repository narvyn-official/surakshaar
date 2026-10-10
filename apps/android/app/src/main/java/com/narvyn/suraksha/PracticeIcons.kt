package com.narvyn.suraksha

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.Drawable
import android.widget.Button

/** Original small line icons; visible labels remain the primary meaning and TalkBack name. */
internal enum class PracticeIcon { VIEW, AUDIO, PAUSE, PLAY, INFO, TARGET, RESET, BACK }
internal class PracticeIconDrawable(private val icon:PracticeIcon,private val colour:Int):Drawable(){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=1.8f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    override fun draw(c:Canvas){c.save();c.translate(bounds.left.toFloat(),bounds.top.toFloat());c.scale(bounds.width()/24f,bounds.height()/24f);p.color=colour
        when(icon){
            PracticeIcon.VIEW->{val path=Path().apply{moveTo(2f,12f);quadTo(12f,1f,22f,12f);quadTo(12f,23f,2f,12f)};c.drawPath(path,p);c.drawCircle(12f,12f,3f,p)}
            PracticeIcon.AUDIO->{val path=Path().apply{moveTo(3f,9f);lineTo(7f,9f);lineTo(12f,5f);lineTo(12f,19f);lineTo(7f,15f);lineTo(3f,15f);close()};c.drawPath(path,p);c.drawArc(9f,4f,22f,20f,-65f,130f,false,p);c.drawArc(11f,8f,17f,16f,-60f,120f,false,p)}
            PracticeIcon.PAUSE->{c.drawLine(8f,5f,8f,19f,p);c.drawLine(16f,5f,16f,19f,p)}
            PracticeIcon.PLAY->{val path=Path().apply{moveTo(7f,4f);lineTo(20f,12f);lineTo(7f,20f);close()};c.drawPath(path,p)}
            PracticeIcon.INFO->{c.drawCircle(12f,12f,9f,p);c.drawLine(12f,11f,12f,17f,p);c.drawPoint(12f,7f,p)}
            PracticeIcon.TARGET->{c.drawCircle(12f,12f,7f,p);c.drawCircle(12f,12f,2f,p);c.drawLine(12f,1f,12f,5f,p);c.drawLine(12f,19f,12f,23f,p);c.drawLine(1f,12f,5f,12f,p);c.drawLine(19f,12f,23f,12f,p)}
            PracticeIcon.RESET->{c.drawArc(5f,5f,21f,21f,-80f,285f,false,p);c.drawLine(4f,4f,4f,10f,p);c.drawLine(4f,10f,10f,10f,p)}
            PracticeIcon.BACK->{c.drawLine(16f,5f,9f,12f,p);c.drawLine(9f,12f,16f,19f,p)}
        };c.restore()
    }
    override fun setAlpha(a:Int){p.alpha=a}
    override fun setColorFilter(f:ColorFilter?){p.colorFilter=f}
    @Deprecated("Drawable opacity is unused") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
internal fun Activity.practiceTool(label:String,icon:PracticeIcon,description:String=label,action:()->Unit):Button =
    action(label,false,ActionRole.NEUTRAL,action).apply{
        textSize=12f;contentDescription=description;minHeight=dp(48);minimumHeight=dp(48)
        setPadding(dp(7),dp(6),dp(7),dp(6))
        setPracticeIcon(this,icon,label.isBlank())
    }

internal fun Activity.setPracticeIcon(button:Button,icon:PracticeIcon,iconOnly:Boolean=false){
    if(iconOnly||resources.configuration.fontScale>=1.6f){
        button.text="";button.setCompoundDrawablesRelative(null,null,null,null)
        button.tooltipText=button.contentDescription
        button.foreground=object:Drawable(){
            private val symbol=PracticeIconDrawable(icon,Palette.ink)
            override fun draw(c:Canvas){val size=dp(24);val x=bounds.centerX()-size/2;val y=bounds.centerY()-size/2;symbol.setBounds(x,y,x+size,y+size);symbol.draw(c)}
            override fun setAlpha(a:Int){symbol.alpha=a}
            override fun setColorFilter(f:ColorFilter?){symbol.colorFilter=f}
            @Deprecated("Drawable opacity") override fun getOpacity()=PixelFormat.TRANSLUCENT
        }
    }else{
        button.foreground=null
        val symbol=PracticeIconDrawable(icon,Palette.ink).apply{setBounds(0,0,dp(18),dp(18))}
        button.setCompoundDrawablesRelative(symbol,null,null,null);button.compoundDrawablePadding=dp(5)
    }
}
