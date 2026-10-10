package com.narvyn.suraksha

import org.junit.Assert.*
import org.junit.Test

class NarrationPlaybackTest {
    @Test fun latestInstructionCancelsOldCallbacksAndNeverReplaysTheOldStep() {
        val state = NarrationPlayback()
        state.set("Check the scene. Ask for help.", "en-IN")
        val old = state.begin()!!
        state.set("Leave the area.", "en-IN")
        val next = state.begin()!!
        assertFalse(state.complete(old.id)); assertTrue(state.owns(next.id)); assertEquals(0, state.index)
        assertTrue(state.complete(next.id)); assertTrue(state.finished); assertFalse(state.playing)
    }
    @Test fun pauseResumeRepeatsOnlyTheInterruptedPhrase() {
        val state = NarrationPlayback(); state.set("First sentence.\n\nSecond sentence.\n\nThird sentence.", "en")
        assertTrue(state.complete(state.begin()!!.id))
        val interrupted = state.begin()!!; assertEquals("Second sentence.", interrupted.text)
        state.pause(); assertTrue(state.paused); assertFalse(state.complete(interrupted.id))
        assertEquals("Second sentence.", state.begin()!!.text)
        assertEquals("First sentence.", state.begin(restart = true)!!.text)
    }
    @Test fun repeatedRenderDoesNotResetPlaybackOrAutomaticSuppression() {
        val state = NarrationPlayback(); assertTrue(state.set("Keep clear.", "hi-IN"))
        state.begin(); assertFalse(state.set("Keep clear.", "hi")); assertTrue(state.attempted)
        state.pause(); assertFalse(state.set("Keep clear.", "hi")); assertTrue(state.attempted)
        state.retryAutomatic(); assertFalse(state.attempted)
    }
    @Test fun changedLanguageInvalidatesCallbacksEvenWhenTextMatches() {
        val state = NarrationPlayback(); state.set("AED", "en")
        val old = state.begin()!!; assertTrue(state.set("AED", "hi")); assertFalse(state.complete(old.id))
        assertFalse(state.attempted)
    }
    @Test fun emptyContentNeverCreatesASpeechRequest() {
        val state = NarrationPlayback(); state.set(" \n ", "en"); assertNull(state.begin())
    }
    @Test fun paragraphsAndLongTextSplitWithinEngineLimitWithoutSplittingSurrogatePairs() {
        val chunks = NarrationPlayback.split("First.\nदूसरा वाक्य। अगला। " + "word ".repeat(250) + "😀".repeat(500))
        assertEquals("First.", chunks[0]); assertEquals("दूसरा वाक्य।", chunks[1])
        assertTrue(chunks.all { it.length <= 420 && !Character.isHighSurrogate(it.last()) && !Character.isLowSurrogate(it.first()) })
    }
    @Test fun relatedSentencesShareProsodyButParagraphsRemainSeparate() {
        assertEquals(listOf("Check the scene. Ask for help.", "Take your time. Pause when needed."),
            NarrationPlayback.phrases("Check the scene. Ask for help.\n\nTake your time. Pause when needed."))
    }
    @Test fun speechFormattingKeepsNumbersWarningsAndAuthoredMeaning() {
        val source = "CPR · Adult. .\nUse 5.5 cm as the example. Never practise compressions on a healthy person."
        val phrases = NarrationPlayback.phrases(source)
        assertEquals("CPR. Adult. Use 5.5 cm as the example. Never practise compressions on a healthy person.", phrases.joinToString(" "))
        assertEquals(listOf("धीरे बोलें। फिर सुनें।"), NarrationPlayback.phrases("धीरे बोलें।. फिर सुनें।"))
    }
    @Test fun boundedPhrasesKeepAllTextAndUnicodeIntact() {
        val source = "Check the scene. " + "word ".repeat(170) + "😀".repeat(160)
        val phrases = NarrationPlayback.phrases(source, 120)
        assertTrue(phrases.size > 2)
        assertTrue(phrases.all { it.length <= 120 && !Character.isHighSurrogate(it.last()) && !Character.isLowSurrogate(it.first()) })
        assertEquals(source.filterNot { it.isWhitespace() }, phrases.joinToString("").filterNot { it.isWhitespace() })
    }
    @Test fun matchingInstalledOfflineVoiceWinsAndRegionThenQualityAreDeterministic() {
        val voices = listOf(
            NarrationVoicePolicy.Candidate("cloud", "hi-IN", false, true, 500),
            NarrationVoicePolicy.Candidate("missing", "hi-IN", true, false, 500),
            NarrationVoicePolicy.Candidate("english", "en-IN", true, true, 500),
            NarrationVoicePolicy.Candidate("regional", "hi-IN", true, true, 200),
            NarrationVoicePolicy.Candidate("other-region", "hi", true, true, 400))
        assertEquals("regional", NarrationVoicePolicy.select("hi-IN", voices))
        assertNull(NarrationVoicePolicy.select("sat-IN", voices))
    }
    @Test fun explicitPreferenceNeverEnablesCloudMissingOrWrongLanguageVoice() {
        val voices = listOf(
            NarrationVoicePolicy.Candidate("default", "en-IN", true, true, 400),
            NarrationVoicePolicy.Candidate("preferred", "en-IN", true, true, 200),
            NarrationVoicePolicy.Candidate("cloud", "en-IN", false, true, 500),
            NarrationVoicePolicy.Candidate("missing", "en-IN", true, false, 500),
            NarrationVoicePolicy.Candidate("hindi", "hi-IN", true, true, 500))
        assertEquals("preferred", NarrationVoicePolicy.select("en-IN", voices, "preferred"))
        for (unavailable in listOf("cloud", "missing", "hindi", "removed"))
            assertEquals("default", NarrationVoicePolicy.select("en-IN", voices, unavailable))
    }
    @Test fun voiceQualityThenLatencyDetermineAutomaticChoiceWithinTheLocale() {
        val voices = listOf(
            NarrationVoicePolicy.Candidate("slow", "en-IN", true, true, 400, 500),
            NarrationVoicePolicy.Candidate("quick", "en-IN", true, true, 400, 100),
            NarrationVoicePolicy.Candidate("basic", "en-IN", true, true, 200, 50))
        assertEquals("quick", NarrationVoicePolicy.select("en-IN", voices))
        assertEquals(listOf("quick", "slow", "basic"), NarrationVoicePolicy.ranked("en-IN", voices).map { it.name })
    }
}
