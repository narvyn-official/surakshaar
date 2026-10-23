package com.narvyn.suraksha

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlin.math.roundToInt

/**
 * Original soft surface, with an explicit edge so shadows are never the only control boundary.
 * The software blur is baked once into a bounded shared patch; normal hardware drawing uses
 * nine cached source/destination rectangles. There is no per-frame blur, bitmap or path creation.
 * Shadows stay inside the drawable bounds, avoiding parent clipping and larger touch targets.
 */
internal class SoftSurfaceDrawable(
    context: Context,
    private val fill: Int = Palette.surface,
    radiusDp: Int = 16,
    private val edge: Int = Palette.line,
    private val interactive: Boolean = false,
    private val inset: Boolean = false,
) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val radius = (radiusDp.coerceIn(4, 32) * density).coerceAtLeast(1f)
    private val insetPx = (4f * density).roundToInt().coerceAtLeast(1)
    private val face = RectF()
    private val focusBounds = RectF()
    private val destinations = Array(9) { Rect() }
    private val bitmapPen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val facePen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePen = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = density.coerceAtLeast(1f) }
    private val focusPen = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density }
    private var patch: Patch? = null
    private var depth = if (inset) Depth.INSET else Depth.RAISED
    private var faceColor = fill
    private var edgeColor = edge
    private var focused = false
    private var opacity = 255

    init { updateAppearance(intArrayOf(android.R.attr.state_enabled)) }

    override fun isStateful() = interactive
    override fun onStateChange(state: IntArray): Boolean = updateAppearance(state)

    private fun updateAppearance(state: IntArray): Boolean {
        val enabled = !interactive || android.R.attr.state_enabled in state
        val selected = interactive && (android.R.attr.state_selected in state || android.R.attr.state_checked in state)
        val pressed = interactive && enabled && android.R.attr.state_pressed in state
        val nextFocus = interactive && enabled && android.R.attr.state_focused in state
        val nextDepth = when { !enabled -> Depth.FLAT; pressed || selected || inset -> Depth.INSET; else -> Depth.RAISED }
        val nextColor = when {
            !enabled -> Palette.disabled
            pressed -> blend(fill, Palette.ink, .065f)
            selected -> blend(fill, Palette.blue, .08f)
            else -> fill
        }
        val nextEdge = when { !enabled -> Palette.line; selected -> Palette.blue; else -> edge }
        if (patch != null && nextDepth == depth && nextColor == faceColor && nextEdge == edgeColor && focused == nextFocus) return false
        depth = nextDepth; faceColor = nextColor; edgeColor = nextEdge; focused = nextFocus
        val key = Key(radius.roundToInt(), insetPx, (density * 100).roundToInt(), faceColor, edgeColor, depth)
        patch = synchronized(cache) {
            cache.get(key) ?: try {
                createPatch(key).also { cache.put(key, it) }
            } catch (_: OutOfMemoryError) {
                // Retain readable solid fill, border and focus if decorative allocation fails.
                null
            }
        }
        updateGeometry()
        invalidateSelf()
        return true
    }

    override fun onBoundsChange(bounds: Rect) { updateGeometry() }

    private fun updateGeometry() {
        val width = bounds.width().coerceAtLeast(0)
        val height = bounds.height().coerceAtLeast(0)
        val pad = minOf(insetPx.toFloat(), width / 4f, height / 4f)
        face.set(bounds.left + pad, bounds.top + pad, bounds.right - pad, bounds.bottom - pad)
        focusBounds.set(face); focusBounds.inset(-density, -density)
        val cap = patch?.cap ?: return
        val dx = minOf(cap, width / 2)
        val dy = minOf(cap, height / 2)
        var i = 0
        for (row in 0..2) for (column in 0..2) {
            val left = when (column) { 0 -> bounds.left; 1 -> bounds.left + dx; else -> bounds.right - dx }
            val right = when (column) { 0 -> bounds.left + dx; 1 -> bounds.right - dx; else -> bounds.right }
            val top = when (row) { 0 -> bounds.top; 1 -> bounds.top + dy; else -> bounds.bottom - dy }
            val bottom = when (row) { 0 -> bounds.top + dy; 1 -> bounds.bottom - dy; else -> bounds.bottom }
            destinations[i++].set(left, top, right, bottom)
        }
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val cached = patch
        if (cached != null) {
            for (i in 0..8) if (!destinations[i].isEmpty) canvas.drawBitmap(cached.bitmap, cached.sources[i], destinations[i], bitmapPen)
        } else {
            facePen.color = faceColor; edgePen.color = edgeColor
            facePen.alpha=opacity;edgePen.alpha=opacity
            canvas.drawRoundRect(face, radius, radius, facePen)
            canvas.drawRoundRect(face, radius, radius, edgePen)
        }
        if (focused) {
            focusPen.color = Palette.ink
            focusPen.alpha=opacity
            canvas.drawRoundRect(focusBounds, radius + density, radius + density, focusPen)
        }
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(face.left.roundToInt(), face.top.roundToInt(), face.right.roundToInt(), face.bottom.roundToInt(), radius)
        outline.alpha = opacity / 255f
    }
    override fun setAlpha(alpha: Int) {
        opacity = alpha.coerceIn(0, 255)
        bitmapPen.alpha = opacity; facePen.alpha = opacity; edgePen.alpha = opacity; focusPen.alpha = opacity
        invalidateSelf()
    }
    override fun setColorFilter(filter: ColorFilter?) {
        bitmapPen.colorFilter = filter; facePen.colorFilter = filter; edgePen.colorFilter = filter; focusPen.colorFilter = filter
        invalidateSelf()
    }
    @Suppress("DEPRECATION") override fun getOpacity() = PixelFormat.TRANSLUCENT

    private enum class Depth { RAISED, INSET, FLAT }
    private data class Key(val radius: Int, val pad: Int, val density: Int, val fill: Int, val edge: Int, val depth: Depth)
    private data class Patch(val bitmap: Bitmap, val cap: Int, val sources: Array<Rect>)

    companion object {
        /** Process-local bounded cache, never disk storage; evictions are not recycled while views use them. */
        private val cache = object : LruCache<Key, Patch>(4 * 1024 * 1024) {
            override fun sizeOf(key: Key, value: Patch) = value.bitmap.allocationByteCount
        }
        private fun blend(a: Int, b: Int, amount: Float): Int = Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * amount).roundToInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * amount).roundToInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * amount).roundToInt(),
        )
        private fun createPatch(key: Key): Patch {
            val cap = key.pad + key.radius + 1
            val side = cap * 2 + 1
            val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val radius = key.radius.toFloat()
            val density = key.density / 100f
            val face = RectF(key.pad.toFloat(), key.pad.toFloat(), (side - key.pad).toFloat(), (side - key.pad).toFloat())
            val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = key.fill }
            val blur = 4f * density
            val offset = 2f * density
            when (key.depth) {
                Depth.RAISED -> {
                    pen.setShadowLayer(blur, offset, offset, 0x41576d7a)
                    canvas.drawRoundRect(face, radius, radius, pen)
                    pen.setShadowLayer(blur, -offset, -offset, 0xe6ffffff.toInt())
                    canvas.drawRoundRect(face, radius, radius, pen)
                    pen.clearShadowLayer(); canvas.drawRoundRect(face, radius, radius, pen)
                }
                Depth.INSET -> {
                    canvas.drawRoundRect(face, radius, radius, pen)
                    val inner = Path().apply { addRoundRect(face, radius, radius, Path.Direction.CW) }
                    val outside = Path().apply {
                        fillType = Path.FillType.EVEN_ODD
                        addRect(-blur * 2, -blur * 2, side + blur * 2, side + blur * 2, Path.Direction.CW)
                        addRoundRect(face, radius, radius, Path.Direction.CW)
                    }
                    val save = canvas.save(); canvas.clipPath(inner)
                    pen.setShadowLayer(blur, offset, offset, 0x55526572)
                    canvas.drawPath(outside, pen)
                    pen.setShadowLayer(blur, -offset, -offset, 0xc4ffffff.toInt())
                    canvas.drawPath(outside, pen)
                    pen.clearShadowLayer(); canvas.restoreToCount(save)
                }
                Depth.FLAT -> canvas.drawRoundRect(face, radius, radius, pen)
            }
            pen.clearShadowLayer(); pen.color = key.edge; pen.style = Paint.Style.STROKE
            pen.strokeWidth = density.coerceAtLeast(1f)
            canvas.drawRoundRect(face, radius, radius, pen)
            val sources = Array(9) { Rect() }
            var i = 0
            for (row in 0..2) for (column in 0..2) {
                sources[i++].set(
                    when (column) { 0 -> 0; 1 -> cap; else -> cap + 1 },
                    when (row) { 0 -> 0; 1 -> cap; else -> cap + 1 },
                    when (column) { 0 -> cap; 1 -> cap + 1; else -> side },
                    when (row) { 0 -> cap; 1 -> cap + 1; else -> side },
                )
            }
            return Patch(bitmap, cap, sources)
        }
    }
}

/** Shared tactile treatment; the native view retains focus, selection and accessibility semantics. */
internal fun Context.softSurface(
    fill: Int = Palette.surface,
    radius: Int = 16,
    edge: Int = Palette.line,
    interactive: Boolean = false,
    inset: Boolean = false,
): Drawable = SoftSurfaceDrawable(this, fill, radius, edge, interactive, inset)
