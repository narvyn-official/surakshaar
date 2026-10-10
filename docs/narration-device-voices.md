# On-device narration

SurakshaAR uses Android's installed text-to-speech engine. It does not send lesson text to a new speech service. Text instructions remain available when speech is unavailable.

## Interaction

- **Audio** contains Play/Pause, Replay, automatic narration and speed.
- **Voice settings** replaces the contents of the same dialog, preserving the practice screen's existing modal pause. Compare installed voices with the previous/next controls and **Preview voice**. The selection is saved for this engine and language; **Automatic** restores automatic selection.
- Preview uses a short, authored English or Hindi sample. It pauses the lesson without changing its text, position, attempt or progress. Back returns to Audio; closing the panel stops a playing preview.
- **Android voices** opens the device's speech settings. On return, the app reloads the speech engine so newly installed voices or a changed default engine can be used.
- New installations use normal rate and neutral pitch (`1.0`). Existing user speed preferences remain unchanged. Users can still slow the narration.

## Selection and playback policy

Only voices marked as installed and not requiring a network are eligible. The selected language must match. Automatic selection orders eligible voices by exact locale, engine-reported quality, then lower engine-reported latency. A saved choice is accepted only while it still meets the same language and offline requirements; otherwise the app falls back to an eligible voice.

Related sentences are spoken together in bounded phrases (280 characters maximum) so the engine retains their punctuation and prosody. Paragraph boundaries remain separate; long text prefers clause or word boundaries. Display separators and duplicated sentence punctuation are normalized without deleting warnings or changing facts. Pause resumes at the current phrase boundary. A new instruction or replay flushes old audio; continuation uses `QUEUE_ADD`. The explicit `read()` path no longer starts automatically and immediately starts again.

TalkBack continues to suppress automatic narration. Audio-focus loss, disconnecting headphones, leaving the activity and destruction stop playback. English-only content does not silently substitute for a selected Hindi or Santali lesson; the existing explicit English opt-in remains available.

The caller supplies concise authored narration: the current cue, question or explanation. Button labels, especially incorrect assessment choices, must not be appended as spoken instructions. The speech layer does not remove safety warnings on its own.

## Verification scope

Focused automated checks are in `NarrationPlaybackTest` and `NarrationControlsTest`. They cover stale callback rejection, pause/replay, repeated-render suppression, bounded Unicode-safe phrasing, preserved numerical/safety text, offline/installed/language filtering, saved-choice fallback, voice ranking and the settings/preview control flow. The UI test conditionally invokes preview only when the emulator reports an installed voice; it does not claim audible output. Validation screenshots are written to the app's external `validation` directory.

Build and emulator execution are recorded by the coordinating implementation task. This note by itself does not establish a passing run, screen-reader conformance or human listening approval. The following need listening on the target phone: Hindi and English pronunciation, pace, sentence transitions, interruptions and whether the selected voice sounds natural. Voice quality ultimately depends on the installed engine and voice assets. Engine-reported quality is ranking metadata, not evidence of a human-like voice. Santali speech requires an installed compatible voice and authored Santali content; no unreviewed sample is synthesized for it.

## Official Android references

Checked 10 October 2026:

- [Voice](https://developer.android.com/reference/android/speech/tts/Voice): locale, quality, latency and network requirement metadata.
- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech): voice selection, normal rate/pitch, queue modes, asynchronous playback and shutdown.
- [TextToSpeech.Engine](https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine): `KEY_FEATURE_NOT_INSTALLED` and installed-voice handling.
