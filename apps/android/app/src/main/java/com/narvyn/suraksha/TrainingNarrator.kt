package com.narvyn.suraksha

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import java.util.Locale

/** Device speech only. The authored language is explicit; untranslated lessons never impersonate it. */
class TrainingNarrator(private val host: Activity, private val hi: () -> Boolean) : AutoCloseable {
    private val queue = NarrationPlayback()
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var initialised = false
    private var active = false
    private var closed = false
    private var pendingManual = false
    private var explicitLanguage: String? = null
    private var problem: String? = null
    private var lastNotice: String? = null
    private val observers = mutableSetOf<() -> Unit>()
    private val prefs = host.getSharedPreferences("room-voice", Context.MODE_PRIVATE)
    private val appPrefs = host.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val manager = host.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ if (it < 0) pause() }, handler).build()
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }
    private val lifecycle = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) { if (activity === host) { active = true; automatic() } }
        override fun onActivityPaused(activity: Activity) { if (activity === host) { active = false; pause() } }
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) { if (activity === host) close() }
    }
    /** Override only when the host provides a real language selector. Content uses show(..., tag). */
    var languageProvider: () -> String = { if (appPrefs.getBoolean("hi", false)) "hi-IN" else "en-IN" }
    val automaticEnabled get() = prefs.getBoolean("automatic", true)
    val speechRate get() = prefs.getFloat("rate", .9f).takeIf { it in .65f..1.25f } ?: .9f
    val isSpeaking get() = queue.playing

    init {
        host.application.registerActivityLifecycleCallbacks(lifecycle)
        ContextCompat.registerReceiver(host, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        // Post even when an engine invokes its initialization callback synchronously.
        engine = TextToSpeech(host.applicationContext) { result -> handler.post {
            if (closed) return@post
            initialised = true; ready = result == TextToSpeech.SUCCESS
            engine?.setAudioAttributes(attributes)
            engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { handler.post { if (queue.owns(id)) changed() } }
                override fun onDone(id: String?) { handler.post {
                    if (!closed && id != null && queue.complete(id)) {
                        if (queue.finished) { releaseFocus(); changed() } else speakNext()
                    }
                } }
                override fun onStop(id: String?, interrupted: Boolean) { handler.post { if (queue.owns(id)) pause() } }
                @Deprecated("Legacy engine callback") override fun onError(id: String?) = failed(id)
                override fun onError(id: String?, errorCode: Int) = failed(id)
                private fun failed(id: String?) { handler.post { if (queue.owns(id)) fail(t(
                    "Voice could not play. Read the instruction or retry.", "आवाज़ नहीं चली। निर्देश पढ़ें या फिर प्रयास करें।"), true) } }
            })
            if (pendingManual) { pendingManual = false; play() } else automatic()
            changed()
        } }
    }

    /** Legacy callers pass already-localized text; new authored lessons should pass their language. */
    fun show(value: String, contentLanguageTag: String = if (hi()) "hi-IN" else "en-IN") {
        if (closed) return
        if (queue.set(value, contentLanguageTag)) {
            engine?.stop(); releaseFocus(); if (queue.language != "en") explicitLanguage = null; problem = null; pendingManual = false
        }
        automatic(); changed()
    }
    fun showLocalized(en: String, hi: String? = null, sat: String? = null) {
        when (NarrationPlayback.language(languageProvider())) {
            "hi" -> if (!hi.isNullOrBlank()) show(hi, "hi-IN") else show(en, "en-IN")
            "sat" -> if (!sat.isNullOrBlank()) show(sat, "sat-IN") else show(en, "en-IN")
            else -> show(en, "en-IN")
        }
    }
    fun clear() { pause(); queue.set("", "en"); explicitLanguage = null; problem = null; changed() }
    fun automatic() {
        if (closed || !active || !host.hasWindowFocus() || host.isFinishing || queue.text.isBlank() ||
            queue.attempted || !automaticEnabled || screenReaderActive()) return
        if (translationMissing()) { val message = translationMessage(); fail(message, false); announceOnce(message); return }
        if (!ready && !initialised) return
        play(report = false)
    }
    fun replay(report: Boolean = true) { play(restart = true, report = report) }
    fun resume() { play() }
    /** Explicit user/page request, suitable for dialog content. The caller owns whether it is automatic. */
    fun read(value: String, contentLanguageTag: String = if (hi()) "hi-IN" else "en-IN"): String? {
        show(value, contentLanguageTag); play(restart = true, report = false)
        return problem
    }
    fun setAutomatic(enabled: Boolean) {
        prefs.edit().putBoolean("automatic", enabled).apply()
        pause()
        if (enabled) { queue.retryAutomatic(); automatic() }
        changed()
    }
    fun setRate(value: Float) {
        val clamped = value.coerceIn(.65f, 1.25f)
        prefs.edit().putFloat("rate", clamped).apply()
        // Re-speak only the current sentence; settings never restart the full lesson.
        val wasPlaying = queue.playing
        if (wasPlaying) pause()
        engine?.setSpeechRate(clamped)
        if (wasPlaying) play()
        changed()
    }
    private fun play(restart: Boolean = false, report: Boolean = true) {
        if (closed || host.isFinishing || host.isDestroyed || !active || queue.text.isBlank()) return
        if (translationMissing()) { fail(translationMessage(), report); return }
        if (!ready) {
            if (!initialised) { pendingManual = true; problem = t("Preparing voice…", "आवाज़ तैयार हो रही है…"); changed() }
            else fail(t("Android speech is unavailable. Text remains available.", "Android आवाज़ उपलब्ध नहीं है। पाठ उपलब्ध है।"), report)
            return
        }
        val tts = engine ?: return
        val selected = explicitLanguage ?: languageProvider()
        val voices = runCatching { tts.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val name = NarrationVoicePolicy.select(selected, voices.map {
            NarrationVoicePolicy.Candidate(it.name, it.locale.toLanguageTag(), !it.isNetworkConnectionRequired,
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty(), it.quality)
        })
        val voice = voices.firstOrNull { it.name == name }
        if (voice == null) { fail(t("${languageName(selected)} offline voice is not installed. Text remains available.",
            "${languageName(selected)} की ऑफ़लाइन आवाज़ इंस्टॉल नहीं है। पाठ उपलब्ध है।"), report); return }
        if (runCatching { tts.setVoice(voice) }.getOrDefault(TextToSpeech.ERROR) != TextToSpeech.SUCCESS) {
            fail(t("The offline voice could not load. Try again or read the text.", "ऑफ़लाइन आवाज़ लोड नहीं हुई। फिर कोशिश करें या पाठ पढ़ें।"), report); return
        }
        current.get()?.takeIf { it !== this }?.pause()
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            fail(t("Audio is busy. Try again when the other audio has stopped.", "आवाज़ अभी व्यस्त है। दूसरी आवाज़ रुकने पर फिर कोशिश करें।"), report); return
        }
        current = WeakReference(this); problem = null; pendingManual = false
        tts.setSpeechRate(speechRate)
        speak(queue.begin(restart)); changed()
    }
    private fun speakNext() { if (active && !closed) speak(queue.begin()) else pause() }
    private fun speak(ticket: NarrationPlayback.Ticket?) {
        if (ticket == null) { releaseFocus(); return }
        if (engine?.speak(ticket.text, TextToSpeech.QUEUE_FLUSH, null, ticket.id) != TextToSpeech.SUCCESS)
            fail(t("Voice could not start. Text remains available.", "आवाज़ शुरू नहीं हुई। पाठ उपलब्ध है।"), true)
    }
    private fun fail(message: String, report: Boolean) {
        pause(); problem = message; changed()
        if (report) announceOnce(message)
    }
    private fun announceOnce(message: String) { if (lastNotice != message) { lastNotice = message; Toast.makeText(host, message, Toast.LENGTH_LONG).show() } }
    private fun translationMissing() = explicitLanguage == null && NarrationPlayback.language(languageProvider()) != queue.language
    private fun translationMessage() = t("This lesson has English audio only.",
        "इस पाठ की ${languageName(languageProvider())} आवाज़ उपलब्ध नहीं है।")
    private fun languageName(tag: String) = when (NarrationPlayback.language(tag)) { "hi" -> "हिन्दी"; "sat" -> "Santali"; "en" -> "English"; else -> Locale.forLanguageTag(tag).displayLanguage }
    private fun screenReaderActive() = host.getSystemService(AccessibilityManager::class.java).isTouchExplorationEnabled
    private fun t(en: String, hindi: String) = if (appPrefs.getBoolean("hi", false)) hindi else en
    private fun releaseFocus() { manager.abandonAudioFocusRequest(focus); if (current.get() === this) current.clear() }
    fun pause() { pendingManual = false; queue.pause(); engine?.stop(); releaseFocus(); changed() }
    private fun changed() { observers.toList().forEach { it() } }

    fun statusText(): String = problem ?: when {
        queue.text.isBlank() -> t("Select a step to hear its instruction", "निर्देश सुनने के लिए कोई चरण चुनें")
        translationMissing() -> translationMessage()
        queue.playing -> t("${languageName(explicitLanguage ?: languageProvider())} · speaking ${queue.index + 1}/${queue.sentences.size}", "${languageName(explicitLanguage ?: languageProvider())} · बोल रहा है ${queue.index + 1}/${queue.sentences.size}")
        queue.paused -> t("Paused · continue from this sentence", "रुका है · इसी वाक्य से जारी करें")
        queue.finished -> t("Instruction finished", "निर्देश पूरा हुआ")
        screenReaderActive() -> t("Automatic voice paused for the screen reader", "स्क्रीन रीडर के लिए अपने आप बोलना रुका है")
        !automaticEnabled -> t("Automatic voice off", "अपने आप बोलना बंद है")
        else -> t("${languageName(languageProvider())} · automatic instructions", "${languageName(languageProvider())} · अपने आप निर्देश")
    }

    /** Four 48 dp targets and a short live status, with no page or long control list. */
    fun compactControls(): LinearLayout {
        val root = host.column(0)
        val row = LinearLayout(host).apply { gravity = Gravity.CENTER_VERTICAL }
        val play = iconButton("play", t("Play instruction", "निर्देश सुनें")) { if (queue.playing) pause() else resume() }
        val replay = iconButton("replay", t("Replay instruction", "निर्देश फिर सुनें")) { replay() }
        val automatic = iconButton("speaker", t("Automatic voice", "अपने आप आवाज़")) { setAutomatic(!automaticEnabled) }
        val speed = host.action("", false, ActionRole.NEUTRAL) { showSpeed() }.apply { tag = "narration-speed"; setPadding(0, 0, 0, 0); textSize = 14f }
        listOf(play, replay, automatic, speed).forEachIndexed { i, button -> row.addView(button, LinearLayout.LayoutParams(0, host.dp(48), 1f).apply { if (i > 0) marginStart = host.dp(6) }) }
        val status = host.label("", 12f, Palette.muted).apply { tag = "narration-status"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        root.add(row); root.add(status, top = 6)
        val update = {
            play.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            play.setCompoundDrawables(NarrationIcon(if (queue.playing) "pause" else "play", Palette.ink).apply { setBounds(0, 0, host.dp(22), host.dp(22)) }, null, null, null)
            play.contentDescription = if (queue.playing) t("Pause instruction", "निर्देश रोकें") else t("Play instruction", "निर्देश सुनें")
            play.isEnabled = queue.text.isNotBlank(); replay.isEnabled = queue.text.isNotBlank()
            automatic.setCompoundDrawables(NarrationIcon(if (automaticEnabled) "speaker" else "muted", Palette.ink).apply { setBounds(0, 0, host.dp(22), host.dp(22)) }, null, null, null)
            automatic.contentDescription = if (automaticEnabled) t("Automatic voice on. Tap to mute.", "अपने आप आवाज़ चालू है। बंद करने के लिए दबाएँ।") else t("Automatic voice off. Tap to enable.", "अपने आप आवाज़ बंद है। चालू करने के लिए दबाएँ।")
            automatic.isSelected = automaticEnabled
            speed.text = String.format(Locale.ROOT, "%.2f", speechRate).trimEnd('0').trimEnd('.') + "×"
            speed.contentDescription = t("Speech speed ${speed.text}. Change speed.", "आवाज़ की गति ${speed.text}। गति बदलें।")
            status.text = statusText()
            Unit
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { observers += update; update() }
            override fun onViewDetachedFromWindow(view: View) { observers -= update }
        })
        update(); return root
    }
    fun controls(): LinearLayout = host.column(12).apply {
        add(compactControls())
        if (translationMissing() && queue.language == "en") add(host.action(t("Listen in English", "अंग्रेज़ी में सुनें"), false, ActionRole.LEARN) {
            explicitLanguage = "en-IN"; problem = null; replay()
        }, top = 10)
    }
    private fun voiceSettings() {
        pause()
        val intent = Intent("com.android.settings.TTS_SETTINGS")
        if (runCatching { host.startActivity(intent) }.isFailure) host.notice(t("Voice settings", "आवाज़ सेटिंग"),
            t("Open Android Settings and search for Text-to-speech. Install an offline voice for your language.", "Android सेटिंग में Text-to-speech खोजें। अपनी भाषा की ऑफ़लाइन आवाज़ इंस्टॉल करें।"))
    }
    fun showControls(): AlertDialog = AlertDialog.Builder(host).setTitle(t("Audio", "आवाज़"))
        .setView(controls()).setNeutralButton(t("Voice settings", "आवाज़ सेटिंग")) { _, _ -> voiceSettings() }
        .setPositiveButton(t("Done", "पूरा हुआ"), null).show()
    private fun showSpeed() {
        val rates = floatArrayOf(.75f, .9f, 1f, 1.15f)
        val labels = if (appPrefs.getBoolean("hi", false)) arrayOf("धीरे · 0.75×", "स्पष्ट · 0.9×", "सामान्य · 1×", "तेज़ · 1.15×")
            else arrayOf("Slow · 0.75×", "Clear · 0.9×", "Normal · 1×", "Faster · 1.15×")
        AlertDialog.Builder(host).setTitle(t("Speech speed", "आवाज़ की गति"))
            .setSingleChoiceItems(labels, rates.indexOfFirst { kotlin.math.abs(it - speechRate) < .01f }) { dialog, which -> setRate(rates[which]); dialog.dismiss() }
            .setNegativeButton(t("Cancel", "रद्द करें"), null).show()
    }
    private fun iconButton(icon: String, description: String, action: () -> Unit): Button = host.action("", false, ActionRole.NEUTRAL, action).apply {
        tag = "narration-$icon"; contentDescription = description; setPadding(host.dp(12), 0, host.dp(12), 0)
        addOnLayoutChangeListener { view, left, _, right, _, _, _, _, _ ->
            val inset = ((right - left - host.dp(22)) / 2).coerceAtLeast(0)
            if (view.paddingLeft != inset || view.paddingRight != inset) view.setPadding(inset, 0, inset, 0)
        }
        setCompoundDrawables(NarrationIcon(icon, Palette.ink).apply { setBounds(0, 0, host.dp(22), host.dp(22)) }, null, null, null)
    }
    override fun close() {
        if (closed) return
        closed = true; pause(); ready = false; engine?.shutdown(); engine = null
        host.application.unregisterActivityLifecycleCallbacks(lifecycle); host.unregisterReceiver(noisy)
        observers.clear(); handler.removeCallbacksAndMessages(null)
    }
    companion object { private var current = WeakReference<TrainingNarrator>(null) }
}
