package com.narvyn.suraksha

import android.app.Activity
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.exp
import kotlin.math.ln

/** Compact non-modal controls. The model remains visible and interactive above this dock. */
internal class AnatomyViewDock(
    private val host: Activity,
    private val scene: AnatomyScene,
    private val onClose: () -> Unit,
    private val onRescan: () -> Unit,
    private val allowPlacementEdits:Boolean=true,
) : LinearLayout(host) {
    private var tab = 0
    private val shortScreen = host.resources.configuration.screenHeightDp < 480
    private var slider: SeekBar? = null
    private var value: TextView? = null
    init { orientation = VERTICAL; setPadding(host.dp(8), host.dp(6), host.dp(8), host.dp(4)); background = host.shape(Palette.surface, 14, Palette.line); render() }

    private fun button(text: String, description: String = text, selected: Boolean = false, action: () -> Unit): Button =
        host.action(text, false, ActionRole.NEUTRAL, action).apply {
            contentDescription = description; textSize = 12f; minHeight = host.dp(48); minimumHeight = host.dp(48)
            setPadding(host.dp(5), host.dp(5), host.dp(5), host.dp(5)); isSelected = selected
            if (selected) { setTextColor(Palette.blue); background = host.shape(Palette.soft, 9, Palette.blue) }
        }
    private fun row() = LinearLayout(host).apply { gravity = Gravity.CENTER_VERTICAL; isBaselineAligned = false }
    private fun LinearLayout.equal(view: android.view.View) = addView(view, LayoutParams(0, -2, 1f).apply { marginEnd = host.dp(3) })
    private fun heading(text: String) = host.label(text, 12f, Palette.muted).apply { gravity = Gravity.CENTER; minimumWidth = host.dp(37) }
    private fun render() {
        removeAllViews(); slider = null; value = null
        val tabs = row()
        listOf(if (scene.camera) "Place" else "Angle", "Move", "Look").forEachIndexed { index, title ->
            tabs.equal(button(title, "$title controls", tab == index) { tab = index; render() })
        }
        tabs.equal(button("Done", "Close view controls") { onClose() }.actionRole(ActionRole.PRIMARY)); addView(tabs)
        when (tab) { 0 -> if (scene.camera) placement() else angle(); 1 -> movement(); else -> appearance() }
        sync()
    }
    private fun angle() {
        val presets = row()
        listOf("Front" to 0f, "Back" to 180f, "Side" to 90f).forEach { (name, yaw) ->
            presets.equal(button(name) { scene.change { it.copy(yaw = yaw, pitch = 0f) } })
        }; addView(presets)
        val zoom = row(); zoom.addView(button("−", "Zoom out") { scene.change { it.copy(distance = (it.distance / .82f).coerceAtMost(5f)) } }, LayoutParams(host.dp(48), -2))
        slider = SeekBar(host).apply {
            max = 1000; contentDescription = "Model zoom"
            progressTintList = android.content.res.ColorStateList.valueOf(Palette.blue)
            thumbTintList = android.content.res.ColorStateList.valueOf(Palette.blue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, user: Boolean) { if (user) scene.change { it.copy(distance = (.3 * exp((1 - progress / 1000.0) * ln(5 / .3))).toFloat()) } }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }
        zoom.addView(slider, LayoutParams(0, host.dp(48), 1f))
        zoom.addView(button("+", "Zoom in") { scene.change { it.copy(distance = (it.distance * .82f).coerceAtLeast(.3f)) } }, LayoutParams(host.dp(48), -2))
        if(shortScreen)zoom.addView(button("Fit", "Recenter and reset view") { scene.reset(); sync() }, LayoutParams(host.dp(52), -2))
        addView(zoom)
        if(!shortScreen){
            val bottom = row(); bottom.equal(button("Reset view", "Recenter and reset view") { scene.reset(); sync() })
            bottom.equal(host.label("Drag · pinch · two-finger move", 11f, Palette.muted).apply { gravity = Gravity.CENTER; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }); addView(bottom)
        }
    }
    private fun placement() {
        val modes = row()
        if(allowPlacementEdits){
            modes.equal(button(if (scene.controls.edit) "Lock" else "Edit", if (scene.controls.edit) "Lock placement" else "Edit scale and rotation") { scene.change { it.copy(edit = !it.edit) }; render() }.apply { isEnabled = scene.placed })
            modes.equal(button("Move body") { scene.move(); onClose() }.apply { isEnabled = scene.placed })
        }
        modes.equal(button("Rescan", "Retry camera and rescan") { onClose(); onRescan() }); addView(modes)
        val sizes = row()
        listOf("40%" to .4f, "100%" to 1f).forEach { (title, scale) ->
            sizes.equal(button(title, "$title of source size") { scene.change { it.copy(scale = scale) } }.apply { isEnabled = !scene.placed || scene.controls.edit })
        }
        value = host.label("", 12f, Palette.muted).apply { gravity = Gravity.CENTER }; sizes.equal(value!!); addView(sizes)
        addView(host.label(if(allowPlacementEdits)"The phone controls the AR viewpoint. Unlock only to change the model."else "Use the phone to look around. Choose size before placement; rescan to move the casualty.", 12f, Palette.muted))
    }
    private fun movement() {
        if (!scene.camera) {
            val pan = row(); pan.addView(heading("Pan"))
            listOf(Triple("←", "Move view left", -1), Triple("↑", "Move view up", 2), Triple("↓", "Move view down", -2), Triple("→", "Move view right", 1)).forEach { (symbol, name, direction) ->
                pan.equal(button(symbol, name) { scene.change { c -> if (direction == -1 || direction == 1) c.copy(panX = (c.panX + direction * .15f).coerceIn(-2f, 2f)) else c.copy(panY = (c.panY + direction / 2 * .15f).coerceIn(-2f, 2f)) } })
            }; addView(pan)
        }
        val rotate = row(); rotate.addView(heading("Turn"))
        rotate.equal(button("−", "Rotate left") { scene.change { it.copy(yaw = it.yaw - 15f) } }.apply { isEnabled = !scene.camera || !scene.placed || scene.controls.edit })
        rotate.equal(button("+", "Rotate right") { scene.change { it.copy(yaw = it.yaw + 15f) } }.apply { isEnabled = !scene.camera || !scene.placed || scene.controls.edit })
        if (!scene.camera) {
            rotate.addView(heading("Tilt"))
            rotate.equal(button("−", "Tilt up") { scene.change { it.copy(pitch = (it.pitch - 15f).coerceAtLeast(-65f)) } })
            rotate.equal(button("+", "Tilt down") { scene.change { it.copy(pitch = (it.pitch + 15f).coerceAtMost(65f)) } })
        }; addView(rotate)
        if (scene.camera) addView(host.label(if(!allowPlacementEdits) "Move the phone to look around. Rescan to choose another placement." else if (scene.controls.edit) "Turn the body, then return to Place and lock it." else "Open Place → Edit to unlock rotation.", 12f, Palette.muted))
        else if(!shortScreen)addView(button("Recenter") { scene.reset(); sync() })
    }
    private fun appearance() {
        val tones = row()
        AnatomyMaterials.skinTones.forEach { tone ->
            tones.equal(button("●", "Skin tone: ${tone.label}", scene.controls.skinTone == tone.id) { scene.change { it.copy(skinTone = tone.id) }; render() }.apply {
                setTextColor(android.graphics.Color.rgb((tone.colour[0] * 255).toInt(), (tone.colour[1] * 255).toInt(), (tone.colour[2] * 255).toInt())); textSize = 24f
            })
        }; addView(tones)
        addView(button(if (scene.controls.skinOverlay) "Hide skin guide" else "Show skin guide") { scene.change { it.copy(skinOverlay = !it.skinOverlay) }; render() })
        if(!shortScreen)addView(host.label("Appearance only. Skin colour is never used to judge a condition.", 12f, Palette.muted))
    }
    fun sync() {
        value?.text = "${(scene.controls.scale * 100).toInt()}% size"
        slider?.let {
            if (!it.isPressed) it.progress = ((1 - ln(scene.controls.distance.coerceIn(.3f, 5f) / .3) / ln(5 / .3)) * 1000).toInt().coerceIn(0, 1000)
            if (android.os.Build.VERSION.SDK_INT >= 30) it.stateDescription = "%.1f times zoom".format(2.6f / scene.controls.distance)
        }
    }
}
