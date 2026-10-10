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
import android.speech.tts.Voice
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
    private var pendingRestart = false
    private var explicitLanguage: String? = null
    private var problem: String? = null
    private var lastNotice: String? = null
    private var previewId: String? = null
    private var previewSerial = 0L
    private var engineGeneration = 0L
    private var reloadEngineOnResume = false
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
        override fun onActivityResumed(activity: Activity) { if (activity === host) {
            active = true
            if (reloadEngineOnResume) {
                reloadEngineOnResume = false; ready = false; initialised = false
                engine?.shutdown(); engine = null; initialiseEngine()
            } else automatic()
        } }
        override fun onActivityPaused(activity: Activity) { if (activity === host) { active = false; pause() } }
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) { if (activity === host) close() }
    }
    /** Override only when the host provides a real language selector. Content uses show(..., tag). */
    var languageProvider: () -> String = { if (appPrefs.getBoolean("hi", false)) "hi-IN" else "en-IN" }
    val automaticEnabled get() = prefs.getBoolean("automatic", true)
    val speechRate get() = prefs.getFloat("rate", 1f).takeIf { it in .65f..1.25f } ?: 1f
    val isSpeaking get() = queue.playing || previewId != null

    init {
        host.application.registerActivityLifecycleCallbacks(lifecycle)
        ContextCompat.registerReceiver(host, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        initialiseEngine()
    }
    private fun initialiseEngine() {
        val generation = ++engineGeneration
        // Post even when an engine invokes its initialization callback synchronously.
        engine = TextToSpeech(host.applicationContext) { result -> handler.post {
            if (closed || generation != engineGeneration) return@post
            initialised = true; ready = result == TextToSpeech.SUCCESS
            engine?.setAudioAttributes(attributes)
            engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) { handler.post { if (queue.owns(id)) changed() } }
                override fun onDone(id: String?) { handler.post {
                    if (!closed && id != null && id == previewId) {
                        previewId = null; releaseFocus(); changed(); return@post
                    }
                    if (!closed && id != null && queue.complete(id)) {
                        if (queue.finished) { releaseFocus(); changed() } else speakNext()
                    }
                } }
                override fun onStop(id: String?, interrupted: Boolean) { handler.post { if (queue.owns(id) || id != null && id == previewId) pause() } }
                @Deprecated("Legacy engine callback") override fun onError(id: String?) = failed(id)
                override fun onError(id: String?, errorCode: Int) = failed(id)
                private fun failed(id: String?) { handler.post { if (queue.owns(id) || id != null && id == previewId) fail(t(
                    "Voice could not play. Read the instruction or retry.", "आवाज़ नहीं चली। निर्देश पढ़ें या फिर प्रयास करें।"), true) } }
            })
            if (pendingManual) { pendingManual = false; play(restart = pendingRestart) } else automatic()
            changed()
        } }
    }

    /** Legacy callers pass already-localized text; new authored lessons should pass their language. */
    fun show(value: String, contentLanguageTag: String = if (hi()) "hi-IN" else "en-IN") {
        if (closed) return
        setInstruction(value, contentLanguageTag)
        automatic(); changed()
    }
    /** Dialogs own their focus and automatic-play timing; loading text must not start it twice. */
    fun prepare(value: String, contentLanguageTag: String) {
        if (closed) return
        setInstruction(value, contentLanguageTag); changed()
    }
    private fun setInstruction(value: String, contentLanguageTag: String) {
        if (!queue.set(value, contentLanguageTag)) return
        previewId = null; engine?.stop(); releaseFocus()
        if (queue.language != "en") explicitLanguage = null
        problem = null; pendingManual = false
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
        if (closed) return null
        // Do not auto-start through show() and then immediately flush the same instruction.
        setInstruction(value, contentLanguageTag); play(restart = true, report = false)
        return problem
    }
    fun setAutomatic(enabled: Boolean) {
        prefs.edit().putBoolean("automatic", enabled).apply()
        pause()
        if (enabled) { queue.retryAutomatic(); automatic() }
        changed()
    }
    fun setRate(value: Float) {
        val clamped = if (value.isFinite()) value.coerceIn(.65f, 1.25f) else 1f
        prefs.edit().putFloat("rate", clamped).apply()
        // Re-speak only the current short phrase; settings never restart the full lesson.
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
            if (!initialised) { pendingManual = true; pendingRestart = restart; problem = t("Preparing voice…", "आवाज़ तैयार हो रही है…"); changed() }
            else fail(t("Android speech is unavailable. Text remains available.", "Android आवाज़ उपलब्ध नहीं है। पाठ उपलब्ध है।"), report)
            return
        }
        val tts = engine ?: return
        val selected = explicitLanguage ?: languageProvider()
        val voice = chooseVoice(selected)
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
        previewId = null; tts.setSpeechRate(speechRate); tts.setPitch(1f)
        speak(queue.begin(restart), flush = true); changed()
    }
    private fun speakNext() { if (active && !closed) speak(queue.begin(), flush = false) else pause() }
    private fun speak(ticket: NarrationPlayback.Ticket?, flush: Boolean) {
        if (ticket == null) { releaseFocus(); return }
        if (engine?.speak(ticket.text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, ticket.id) != TextToSpeech.SUCCESS)
            fail(t("Voice could not start. Text remains available.", "आवाज़ शुरू नहीं हुई। पाठ उपलब्ध है।"), true)
    }
    private fun voicePreferenceKey(tag: String) = "voice:${engine?.defaultEngine.orEmpty()}:${Locale.forLanguageTag(tag).toLanguageTag()}"
    private fun offlineVoices(tag: String): List<Voice> {
        val voices = runCatching { engine?.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val byName = voices.associateBy { it.name }
        return NarrationVoicePolicy.ranked(tag, voices.map {
            NarrationVoicePolicy.Candidate(it.name, it.locale.toLanguageTag(), !it.isNetworkConnectionRequired,
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty(), it.quality, it.latency)
        }).mapNotNull { byName[it.name] }
    }
    private fun chooseVoice(tag: String, preferredName: String? = prefs.getString(voicePreferenceKey(tag), null)): Voice? {
        val voices = offlineVoices(tag)
        return voices.firstOrNull { it.name == preferredName } ?: voices.firstOrNull()
    }
    /** Preview is separate from the lesson queue: it cannot change the step or consume narration. */
    private fun previewVoice(tag: String, preferredName: String?) {
        pause()
        if (closed || !active || host.isFinishing || host.isDestroyed) return
        val tts = engine
        if (!ready || tts == null) {
            problem = if (initialised) t("Android speech is unavailable.", "Android आवाज़ उपलब्ध नहीं है।")
                else t("Preparing voice… Try again shortly.", "आवाज़ तैयार हो रही है… थोड़ी देर बाद फिर कोशिश करें।")
            changed(); return
        }
        val voice = chooseVoice(tag, preferredName)
        if (voice == null || runCatching { tts.setVoice(voice) }.getOrDefault(TextToSpeech.ERROR) != TextToSpeech.SUCCESS) {
            problem = t("Install an offline ${languageName(tag)} voice in Android settings.", "Android सेटिंग में ${languageName(tag)} की ऑफ़लाइन आवाज़ इंस्टॉल करें।")
            changed(); return
        }
        // Authored samples only, never a translated or fabricated version of the lesson.
        val sample = when (NarrationPlayback.language(tag)) {
            "hi" -> "नमस्ते। आराम से सीखें, एक चरण पर ध्यान दें। आप आवाज़ को रोककर फिर से सुन सकते हैं।"
            "en" -> "Hello. Take your time and focus on one step. You can pause these instructions and listen again."
            else -> { problem = t("A voice preview is not available in this language.", "इस भाषा में आवाज़ का नमूना उपलब्ध नहीं है।"); changed(); return }
        }
        current.get()?.takeIf { it !== this }?.pause()
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            problem = t("Audio is busy. Try again shortly.", "आवाज़ अभी व्यस्त है। थोड़ी देर बाद फिर कोशिश करें।"); changed(); return
        }
        current = WeakReference(this); problem = null
        tts.setSpeechRate(speechRate); tts.setPitch(1f)
        val id = "preview-${++previewSerial}"; previewId = id
        if (tts.speak(sample, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS)
            fail(t("Voice could not start. Try another installed voice.", "आवाज़ शुरू नहीं हुई। दूसरी इंस्टॉल की हुई आवाज़ चुनें।"), false)
        changed()
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
    fun pause() { pendingManual = false; previewId = null; queue.pause(); engine?.stop(); releaseFocus(); changed() }
    private fun changed() { observers.toList().forEach { it() } }

    fun statusText(): String = problem ?: when {
        queue.text.isBlank() -> t("Select a step to hear its instruction", "निर्देश सुनने के लिए कोई चरण चुनें")
        translationMissing() -> translationMessage()
        queue.playing -> t("${languageName(explicitLanguage ?: languageProvider())} · speaking", "${languageName(explicitLanguage ?: languageProvider())} · बोल रहा है")
        queue.paused -> t("Paused · continue here", "रुका है · यहीं से जारी करें")
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
    private fun openAndroidVoiceSettings() {
        pause()
        val intent = Intent("com.android.settings.TTS_SETTINGS")
        reloadEngineOnResume = true
        if (runCatching { host.startActivity(intent) }.isFailure) {
            reloadEngineOnResume = false
            host.notice(t("Voice settings", "आवाज़ सेटिंग"),
                t("Open Android Settings and search for Text-to-speech. Install an offline voice for your language.", "Android सेटिंग में Text-to-speech खोजें। अपनी भाषा की ऑफ़लाइन आवाज़ इंस्टॉल करें।"))
        }
    }
    fun showControls(): AlertDialog {
        val content = host.column(0)
        val dialog = AlertDialog.Builder(host).setTitle(t("Audio", "आवाज़"))
            .setView(content).setNeutralButton(t("Voice settings", "आवाज़ सेटिंग"), null)
            .setPositiveButton(t("Done", "पूरा हुआ"), null).create()
        fun audioPanel() {
            content.removeAllViews(); content.add(controls()); dialog.setTitle(t("Audio", "आवाज़"))
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                text = t("Voice settings", "आवाज़ सेटिंग")
                setOnClickListener { voicePanel(dialog, content, ::audioPanel) }
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                text = t("Done", "पूरा हुआ"); setOnClickListener { dialog.dismiss() }
            }
        }
        dialog.show(); audioPanel(); return dialog
    }
    /** Replace the audio panel in place, keeping the host's modal pause and dismissal ownership. */
    private fun voicePanel(dialog: AlertDialog, content: LinearLayout, back: () -> Unit) {
        pause(); problem = null
        val selected = explicitLanguage ?: languageProvider()
        val root = host.column(12)
        val row = LinearLayout(host).apply { gravity = Gravity.CENTER_VERTICAL }
        var chosen = prefs.getString(voicePreferenceKey(selected), null)
        val caption = host.label("", 14f, Palette.ink, true).apply {
            gravity = Gravity.CENTER; tag = "narration-voice-choice"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val status = host.label("", 12f, Palette.muted).apply {
            tag = "narration-preview-status"; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        lateinit var update: () -> Unit
        fun change(direction: Int) {
            pause(); problem = null
            val choices = listOf<String?>(null) + offlineVoices(selected).map { it.name }
            val index = choices.indexOf(chosen).coerceAtLeast(0)
            chosen = choices[(index + direction + choices.size) % choices.size]
            prefs.edit().apply { if (chosen == null) remove(voicePreferenceKey(selected)) else putString(voicePreferenceKey(selected), chosen) }.apply()
            update()
        }
        val previous = host.action("‹", false, ActionRole.NEUTRAL) { change(-1) }.apply {
            tag = "narration-voice-previous"; contentDescription = t("Previous voice", "पिछली आवाज़"); setPadding(0, 0, 0, 0)
        }
        val next = host.action("›", false, ActionRole.NEUTRAL) { change(1) }.apply {
            tag = "narration-voice-next"; contentDescription = t("Next voice", "अगली आवाज़"); setPadding(0, 0, 0, 0)
        }
        row.addView(previous, LinearLayout.LayoutParams(host.dp(48), host.dp(48)))
        row.addView(caption, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = host.dp(8); marginEnd = host.dp(8) })
        row.addView(next, LinearLayout.LayoutParams(host.dp(48), host.dp(48)))
        val preview = host.action(t("Preview voice", "आवाज़ सुनें"), false, ActionRole.LEARN) {
            if (previewId != null) pause() else previewVoice(selected, chosen)
        }.apply { tag = "narration-voice-preview" }
        root.add(row); root.add(status, top = 6); root.add(preview, top = 8)
        update = {
            val voices = offlineVoices(selected)
            val index = voices.indexOfFirst { it.name == chosen }
            caption.text = if (index >= 0) t("Voice ${index + 1} of ${voices.size}\n${languageName(selected)}", "आवाज़ ${index + 1} / ${voices.size}\n${languageName(selected)}")
                else t("Automatic\n${languageName(selected)}", "अपने आप चुनें\n${languageName(selected)}")
            previous.isEnabled = voices.size > 1; next.isEnabled = voices.size > 1
            preview.text = if (previewId != null) t("Stop preview", "नमूना रोकें") else t("Preview voice", "आवाज़ सुनें")
            preview.isEnabled = ready && voices.isNotEmpty() && NarrationPlayback.language(selected) in listOf("en", "hi")
            status.text = problem ?: when {
                previewId != null -> t("Playing a sample · lesson stays paused", "नमूना चल रहा है · पाठ रुका रहेगा")
                !initialised -> t("Preparing device voices…", "फ़ोन की आवाज़ें तैयार हो रही हैं…")
                voices.isEmpty() -> t("No offline voice. Open Android settings.", "ऑफ़लाइन आवाज़ नहीं है। Android सेटिंग खोलें।")
                else -> t("Offline voices · saved for this language", "ऑफ़लाइन आवाज़ें · इस भाषा के लिए सहेजी गई")
            }
            Unit
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { observers += update; update() }
            override fun onViewDetachedFromWindow(view: View) { observers -= update; if (previewId != null) pause() }
        })
        content.removeAllViews(); content.add(root); dialog.setTitle(t("Voice", "आवाज़ चुनें"))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
            text = t("Android voices", "Android आवाज़ें"); setOnClickListener { openAndroidVoiceSettings() }
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            text = t("Back", "वापस"); setOnClickListener { pause(); back() }
        }
        update()
    }
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
