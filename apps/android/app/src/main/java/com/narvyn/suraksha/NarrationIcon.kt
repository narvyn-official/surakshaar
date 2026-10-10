package com.narvyn.suraksha

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/** Small original audio symbols, paired with explicit accessibility labels. */
internal class NarrationIcon(private val kind: String, color: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun draw(canvas: Canvas) {
        val save = canvas.save(); canvas.translate(bounds.left.toFloat(), bounds.top.toFloat()); canvas.scale(bounds.width()/24f, bounds.height()/24f)
        fun path(vararg xy: Float) { val p = Path(); p.moveTo(xy[0], xy[1]); for (i in 2 until xy.size step 2) p.lineTo(xy[i], xy[i+1]); canvas.drawPath(p, paint) }
        when (kind) {
            "play" -> path(7f, 4f, 20f, 12f, 7f, 20f, 7f, 4f)
            "pause" -> { canvas.drawLine(8f, 5f, 8f, 19f, paint); canvas.drawLine(16f, 5f, 16f, 19f, paint) }
            "replay" -> { canvas.drawArc(RectF(4f,4f,21f,21f),-120f,300f,false,paint); path(5f,2f,5f,9f,12f,9f) }
            else -> {
                path(3f,9f,7f,9f,12f,5f,12f,19f,7f,15f,3f,15f,3f,9f)
                if (kind == "muted") { path(17f,9f,22f,15f); path(22f,9f,17f,15f) }
                else { canvas.drawArc(RectF(10f,6f,21f,18f),-65f,130f,false,paint); canvas.drawArc(RectF(11f,3f,25f,21f),-55f,110f,false,paint) }
            }
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
    @Deprecated("Drawable opacity") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
