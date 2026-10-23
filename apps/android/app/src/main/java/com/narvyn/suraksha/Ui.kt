package com.narvyn.suraksha

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.InsetDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.*

object Palette {
    // Cool tactile neutrals with a restrained blue-teal action accent.
    val canvas=Color.rgb(231,236,239); val surface=Color.rgb(237,242,244); val ink=Color.rgb(32,52,61)
    val muted=Color.rgb(83,100,111); val blue=Color.rgb(33,91,107); val soft=Color.rgb(220,232,237)
    val line=Color.rgb(180,194,202); val controlLine=Color.rgb(111,130,142); val disabled=Color.rgb(222,228,232)
    val success=Color.rgb(33,102,79); val successBg=Color.rgb(226,238,231)
    val danger=Color.rgb(159,62,63); val dangerBg=Color.rgb(243,229,229); val amber=Color.rgb(128,91,29); val amberBg=Color.rgb(241,233,216)
    val teal=Color.rgb(31,105,95); val tealBg=Color.rgb(222,236,232)
    val fire=Color.rgb(180,77,38)
    val violet=Color.rgb(106,82,141); val violetBg=Color.rgb(235,229,242)
}
/** Roles describe the action, never the correctness of an unanswered choice. */
enum class ActionRole { PRIMARY, LEARN, CAMERA, REVIEW, NEUTRAL, DANGER }
fun Context.dp(n: Int)= (n*resources.displayMetrics.density).toInt()
fun Context.shape(colour: Int, radius: Int=16, stroke: Int?=null)=GradientDrawable().apply { setColor(colour);cornerRadius=dp(radius).toFloat();stroke?.let{setStroke(dp(1),it)} }
fun Context.label(value: String,size: Float=16f,colour: Int=Palette.ink,bold: Boolean=false)=TextView(this).apply {
    text=value;textSize=size;setTextColor(colour);typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
    setLineSpacing(dp(2).toFloat(),1f);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES
}
fun Context.column(padding: Int=0)=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(padding),dp(padding),dp(padding),dp(padding)) }
fun LinearLayout.add(view: View,top: Int=0,bottom: Int=0) { addView(view,LinearLayout.LayoutParams(-1,-2).apply { topMargin=context.dp(top);bottomMargin=context.dp(bottom) }) }
fun Context.card(colour: Int=Palette.surface)=column(18).apply {
    background=softSurface(colour,18);elevation=0f
}
fun Button.actionRole(role: ActionRole)=apply {
    val (fill,ink)=when(role) {
        ActionRole.PRIMARY -> Palette.blue to Color.WHITE
        ActionRole.LEARN -> Palette.soft to Palette.blue
        ActionRole.CAMERA -> Palette.tealBg to Palette.teal
        ActionRole.REVIEW -> Palette.amberBg to Palette.amber
        ActionRole.NEUTRAL -> Palette.surface to Palette.ink
        ActionRole.DANGER -> Palette.dangerBg to Palette.danger
    }
    setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),intArrayOf()),intArrayOf(Palette.muted,ink)))
    // Colour is supplementary to the existing label. Focus/selection also have a visible edge.
    val edge=if(role==ActionRole.PRIMARY)Palette.blue else Palette.controlLine
    val face=context.softSurface(fill,16,edge,interactive=true)
    backgroundTintList=null
    background=RippleDrawable(
        ColorStateList.valueOf(if(role==ActionRole.PRIMARY)0x32ffffff else 0x1821343d),
        face,
        InsetDrawable(context.shape(Color.WHITE,16),context.dp(4)),
    )
    background.state=drawableState
    refreshDrawableState()
}
fun Context.action(text: String,primary: Boolean=true,role: ActionRole=if(primary)ActionRole.PRIMARY else ActionRole.LEARN,onClick: ()->Unit)=Button(this).apply {
    stateListAnimator=null;elevation=0f;this.text=text;isAllCaps=false;textSize=15f;typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
    actionRole(role)
    minHeight=dp(48);minimumHeight=dp(48);minWidth=dp(48);minimumWidth=dp(48);setPadding(dp(16),dp(10),dp(16),dp(10));setOnClickListener{performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);onClick()}
}
fun Activity.notice(title: String,message: String) { android.app.AlertDialog.Builder(this).setTitle(title).setView(paged(column(12).apply{add(label(message,16f))},getSharedPreferences("preferences",Context.MODE_PRIVATE).getBoolean("hi",false))).setPositiveButton("OK",null).show() }

/** Explicit structure for screen-reader heading navigation; bold text alone has no heading semantics. */
fun TextView.asHeading()=apply { isAccessibilityHeading=true }

/** Small original vector symbols: no font glyph dependencies, network images or missing icon fonts. */
class AppIcon(private val kind:String,private val ink:Int):Drawable() {
    private val pen=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=ink;style=Paint.Style.STROKE;strokeWidth=1.7f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND }
    override fun draw(canvas:Canvas) {
        val checkpoint=canvas.save();canvas.translate(bounds.left.toFloat(),bounds.top.toFloat());canvas.scale(bounds.width()/24f,bounds.height()/24f)
        fun line(x:Float,y:Float,x2:Float,y2:Float)=canvas.drawLine(x,y,x2,y2,pen)
        fun path(vararg points:Float) { val p=Path();p.moveTo(points[0],points[1]);for(i in 2 until points.size step 2)p.lineTo(points[i],points[i+1]);canvas.drawPath(p,pen) }
        when(kind) {
            "learn" -> { canvas.drawRoundRect(RectF(3f,4f,21f,20f),2f,2f,pen);line(12f,4f,12f,20f);line(6f,8f,9f,8f);line(15f,8f,18f,8f);line(6f,12f,9f,12f);line(15f,12f,18f,12f) }
            "records","shield" -> { path(12f,2.5f,20f,5.5f,19f,14f,16f,18f,12f,21f,8f,18f,5f,14f,4f,5.5f,12f,2.5f);path(8f,11.5f,11f,14.5f,16f,9.5f) }
            "help" -> { canvas.drawCircle(12f,12f,9f,pen);canvas.drawCircle(12f,12f,4f,pen);line(6f,6f,9f,9f);line(15f,15f,18f,18f);line(6f,18f,9f,15f);line(15f,9f,18f,6f) }
            "user" -> { canvas.drawCircle(12f,8f,4f,pen);canvas.drawArc(RectF(4f,13f,20f,26f),180f,180f,false,pen) }
            "fire" -> { val p=Path();p.moveTo(13f,2f);p.cubicTo(5f,7f,8f,10f,5f,11f);p.cubicTo(1f,22f,20f,25f,20f,14f);p.cubicTo(20f,10f,17f,8f,17f,8f);p.lineTo(15f,13f);p.cubicTo(10f,10f,14f,6f,13f,2f);canvas.drawPath(p,pen) }
            "gas" -> { canvas.drawRoundRect(RectF(5f,3f,19f,22f),3f,3f,pen);canvas.drawRoundRect(RectF(8f,6f,16f,12f),1f,1f,pen);canvas.drawCircle(9f,16f,1f,pen);canvas.drawCircle(15f,16f,1f,pen);line(9f,20f,15f,20f) }
            "machinery" -> { canvas.drawRoundRect(RectF(3f,8f,21f,21f),2f,2f,pen);canvas.drawCircle(10f,14f,3f,pen);path(6f,8f,6f,3f,15f,3f,15f,8f);line(17f,12f,18f,12f);line(17f,16f,18f,16f) }
            "ppe" -> { canvas.drawArc(RectF(4f,5f,20f,21f),180f,180f,false,pen);path(3f,13f,3f,17f,21f,17f,21f,13f);line(12f,4f,12f,11f) }
            "first-aid" -> { canvas.drawRoundRect(RectF(4f,5f,20f,21f),3f,3f,pen);path(9f,5f,9f,2f,15f,2f,15f,5f);path(6f,13f,9f,13f,11f,9f,14f,17f,16f,13f,18f,13f) }
            "emergency" -> { canvas.drawRoundRect(RectF(4f,5f,20f,20f),3f,3f,pen);line(9f,2f,15f,2f);line(12f,9f,12f,16f);line(8.5f,12.5f,15.5f,12.5f) }
            "camera" -> { canvas.drawRoundRect(RectF(2f,6f,22f,21f),3f,3f,pen);canvas.drawCircle(12f,13f,4f,pen);path(7f,6f,9f,3f,15f,3f,17f,6f) }
            "admin" -> { canvas.drawRoundRect(RectF(3f,3f,21f,21f),3f,3f,pen);line(3f,9f,21f,9f);line(10f,9f,10f,21f);line(14f,13f,17f,13f);line(14f,17f,17f,17f) }
            "down" -> path(6f,9f,12f,15f,18f,9f)
            else -> path(9f,5f,16f,12f,9f,19f)
        }
        canvas.restoreToCount(checkpoint)
    }
    override fun setAlpha(alpha:Int) { pen.alpha=alpha;invalidateSelf() }
    override fun setColorFilter(filter:android.graphics.ColorFilter?) { pen.colorFilter=filter;invalidateSelf() }
    @Suppress("DEPRECATION") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
}
fun Context.appIcon(kind:String,ink:Int=Palette.teal,size:Int=24)=AppIcon(kind,ink).apply { setBounds(0,0,dp(size),dp(size)) }
