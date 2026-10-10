package com.narvyn.suraksha

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

/** A bounded, offline pack manager. Preparation is explicit and pauses when this screen leaves foreground. */
class OfflineContentActivity : Activity() {
    private lateinit var packs: AnatomyPackStore
    private val executor = Executors.newSingleThreadExecutor()
    private val back by lazy { AppBackNavigation(this) { finish() } }
    @Volatile private var cancelled = false
    private var working = false
    private var foreground = false
    private var message = ""
    private var percent = 0
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); packs = AnatomyPackStore(this); back.enabled(true); render()
    }
    private fun size(bytes: Long) = "%.1f MB".format(bytes / 1_000_000.0)
    private fun render() {
        val root = column().apply { setBackgroundColor(Palette.canvas) }
        val header = LinearLayout(this)
        header.addView(action("‹", false, ActionRole.NEUTRAL) { finish() }.apply { contentDescription = "Back to learning" }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(label("Offline content", 20f, Palette.ink, true).asHeading(), LinearLayout.LayoutParams(0, -2, 1f))
        root.add(header, bottom = 14)
        val body = column()
        body.add(label("Ready without internet", 18f, Palette.ink, true), bottom = 8)
        body.add(label("Fire and First Aid lessons, balanced anatomy and practice records work on this phone. The core pack stays available when optional detail is removed.", 15f, Palette.muted), bottom = 20)
        val descriptor = packs.catalogue
        body.add(label("Detailed body", 18f, Palette.ink, true), bottom = 8)
        if (descriptor == null) {
            body.add(label("No optional pack is included in this build. Balanced anatomy is available.", 15f, Palette.muted))
        } else {
            body.add(label(descriptor.optString("description", "Higher-resolution surfaces for closer anatomy exploration."), 15f), bottom = 10)
            body.add(label("Included archive ${size(descriptor.optLong("bytes"))} · installed storage about ${size(descriptor.optLong("unpackedBytes"))}\nPrepared locally; no network download or account is required.", 13f, Palette.muted), bottom = 14)
            if (working) {
                progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = percent }
                status = label(message.ifBlank { "Preparing…" }, 15f, Palette.blue)
                body.add(status, bottom = 10); body.add(progress, bottom = 14)
                body.add(action("Pause preparation", false) { cancelled = true; message = "Pausing safely…"; status.text = message })
            } else if (packs.installed()) {
                body.add(label("Installed · ${size(packs.installedBytes())}", 14f, Palette.teal, true), bottom = 10)
                body.add(action(if (packs.detailedSelected()) "Use balanced models" else "Use detailed models") {
                    packs.selectDetailed(!packs.detailedSelected()); message = "Applies when you next open a body view."; render()
                }, bottom = 8)
                body.add(action("Remove optional pack", false, ActionRole.NEUTRAL) {
                    runCatching { packs.remove() }.fold({ message = "Optional models removed. Lessons and learning records are unchanged." }, { message = it.message ?: "Removal failed. Try again." }); render()
                })
            } else {
                body.add(action(if (packs.paused()) "Resume preparation" else "Install detailed body") { install() })
                if (packs.paused()) body.add(action("Discard partial files", false, ActionRole.NEUTRAL) {
                    runCatching { packs.remove() }.fold({ message = "Partial files removed." }, { message = it.message ?: "Could not remove partial files." }); render()
                }, top = 8)
            }
        }
        if (message.isNotBlank() && !working) body.add(label(message, 14f, Palette.muted), top = 14)
        body.add(label("Balanced models use less memory and are recommended for longer AR practice. Optional detail changes geometry quality; it does not change lesson outcomes or anatomy review status.", 13f, Palette.muted), top = 20)
        root.addView(paged(body, false), LinearLayout.LayoutParams(-1, 0, 1f))
        root.add(action("Explore the body", false) { startActivity(Intent(this, AnatomyActivity::class.java)) }, top = 12)
        setContentView(root); trainingInsets(root)
    }
    private fun install() {
        if (working) return
        cancelled = false; working = true; percent = 0; message = "Checking the included archive"; render()
        executor.execute {
            var lastPosted = 0L
            val result = runCatching { packs.install({ cancelled || Thread.currentThread().isInterrupted }) { text, amount ->
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastPosted > 80 || amount == 100) {
                    lastPosted = now
                    runOnUiThread { if (!isDestroyed && working) { message = text; percent = amount; if (foreground && ::status.isInitialized) { status.text = "$text · $amount%"; progress.progress = amount } } }
                }
            } }
            runOnUiThread {
                if (!isDestroyed) {
                    working = false
                    message = when (val error = result.exceptionOrNull()) {
                        null -> "Detailed models are ready. Open the body to use them."
                        is CancellationException -> "Preparation paused. Verified files are kept for resuming."
                        else -> error.message ?: "Preparation failed. Balanced models remain available."
                    }
                    if (foreground) render()
                }
            }
        }
    }
    override fun onResume() { super.onResume(); foreground = true; if (::packs.isInitialized) render() }
    override fun onPause() { foreground = false; cancelled = true; super.onPause() }
    override fun onDestroy() { cancelled = true; executor.shutdownNow(); back.close(); super.onDestroy() }
}
