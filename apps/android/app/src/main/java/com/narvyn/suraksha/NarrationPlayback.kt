package com.narvyn.suraksha

import java.util.Locale

/** Main-thread speech queue. A paused utterance resumes at its short phrase boundary. */
internal class NarrationPlayback {
    data class Ticket(val id: String, val text: String)
    var text = ""; private set
    var language = "en"; private set
    var sentences: List<String> = emptyList(); private set
    var index = 0; private set
    var attempted = false; private set
    var playing = false; private set
    var paused = false; private set
    var finished = false; private set
    private var serial = 0L
    private var ticket: String? = null
    fun set(value: String, languageTag: String): Boolean {
        val clean = value.trim()
        val lang = language(languageTag)
        if (clean == text && lang == language) return false
        cancel()
        text = clean; language = lang; sentences = phrases(clean); index = 0
        attempted = false; paused = false; finished = false
        return true
    }
    fun begin(restart: Boolean = false): Ticket? {
        if (sentences.isEmpty()) return null
        if (restart || finished) index = 0
        attempted = true; playing = true; paused = false; finished = false
        return Ticket("narration-${++serial}", sentences[index]).also { ticket = it.id }
    }
    fun complete(id: String): Boolean {
        if (!playing || id != ticket) return false
        ticket = null; index++
        if (index >= sentences.size) { playing = false; finished = true }
        return true
    }
    fun owns(id: String?) = id != null && id == ticket && playing
    fun cancel() { ticket = null; playing = false; serial++ }
    fun pause() { cancel(); attempted = true; paused = text.isNotBlank() && !finished }
    fun retryAutomatic() { attempted = false }
    companion object {
        fun language(tag: String) = Locale.forLanguageTag(tag).language.lowercase(Locale.ROOT)
        /** Let the engine voice connected sentences together instead of resetting after each label.
         * Paragraphs still form pauses. Only display punctuation is cleaned; facts and safety text
         * are never removed or rewritten by the speech layer.
         */
        fun phrases(value: String, maximum: Int = 280): List<String> {
            require(maximum >= 16)
            return value.trim().split(Regex("\\n\\s*\\n")).flatMap { paragraph ->
                val clean = paragraph
                    .replace(Regex("(?m)^\\s*[•●]\\s*"), "")
                    .replace(Regex("\\s+[·|]\\s+"), ". ")
                    .replace(Regex("([.!?।])\\s*[.।]+(?=\\s|$)"), "$1")
                val result = mutableListOf<String>()
                var phrase = ""
                for (sentence in split(clean, maximum)) {
                    if (phrase.isNotEmpty() && phrase.length + sentence.length + 1 > maximum) {
                        result += phrase; phrase = ""
                    }
                    phrase += (if (phrase.isEmpty()) "" else " ") + sentence
                }
                if (phrase.isNotEmpty()) result += phrase
                result
            }
        }
        fun split(value: String, maximum: Int = 420): List<String> {
            require(maximum >= 16)
            val result = mutableListOf<String>()
            for (sentence in value.trim().split(Regex("(?<=[.!?।])\\s+|\\n+"))) {
                var remaining = sentence.trim()
                while (remaining.length > maximum) {
                    // Prefer a clause boundary for long sentences, then a word boundary.
                    var boundary = remaining.take(maximum).indexOfLast { it in ",;:।" }
                        .takeIf { it >= maximum / 2 }?.plus(1)
                        ?: remaining.lastIndexOf(' ', maximum).takeIf { it >= maximum / 2 } ?: maximum
                    if (boundary > 0 && Character.isHighSurrogate(remaining[boundary - 1])) boundary--
                    result += remaining.substring(0, boundary).trim()
                    remaining = remaining.substring(boundary).trim()
                }
                if (remaining.isNotEmpty()) result += remaining
            }
            return result
        }
    }
}

/** Voice selection is intentionally strict: no network voice or silent language substitution. */
internal object NarrationVoicePolicy {
    data class Candidate(val name: String, val languageTag: String, val offline: Boolean, val installed: Boolean,
        val quality: Int = 0, val latency: Int = 0)
    fun ranked(languageTag: String, candidates: List<Candidate>): List<Candidate> = candidates
        .filter { it.offline && it.installed && NarrationPlayback.language(it.languageTag) == NarrationPlayback.language(languageTag) }
        .sortedWith(compareByDescending<Candidate> { it.languageTag.equals(languageTag, true) }
            .thenByDescending { it.quality }.thenBy { it.latency }.thenBy { it.name })
    fun select(languageTag: String, candidates: List<Candidate>, preferredName: String? = null): String? {
        val available = ranked(languageTag, candidates)
        return available.firstOrNull { it.name == preferredName }?.name ?: available.firstOrNull()?.name
    }
}
