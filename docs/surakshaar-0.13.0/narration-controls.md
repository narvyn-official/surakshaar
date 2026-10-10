# Narration controls — 10 October 2026

One shared, device-local narration controller now serves Main, Fire room missions, procedure demonstrations, skill practice, learning scenarios and the anatomy/first-aid hosts. Ordinary training screens expose one **Audio** entry instead of repeated large pause/replay/mute blocks. The audio panel has four 48 dp targets: play/pause, replay, automatic voice, and speed. Text labels for assistive technology and a visible status accompany the icons. Slow, clear, normal and faster rates persist on the device.

## Behaviour implemented

- A new authored instruction cancels the previous one. Re-rendering unchanged text does not restart it.
- Playback is split into bounded sentences. Continue repeats the interrupted sentence, not the whole passage; replay starts at the beginning.
- Unique utterance tickets ignore completion/error callbacks from superseded audio. Only one shared narrator may speak in the app.
- Backgrounding, loss of audio focus, headphone disconnection and closing the host stop playback. Returning does not unexpectedly resume an interrupted sentence.
- Offline, installed voices must match the selected language. Cloud voices and voices needing a download are excluded. Missing translation and missing voice data are separate visible states.
- An English-only lesson never automatically plays English while Hindi is selected. The user may explicitly choose **Listen in English** for that host session. A later Hindi-authored instruction clears that override.
- Automatic speech does not compete with touch-exploration screen readers. Explicit playback remains available.
- Fire learning dialogs wait for their own window focus before reading, so the host's focus-loss handler cannot immediately cancel newly requested dialog speech.
- No microphone permission, speech recognition, cloud transcription or audio upload was added. “Dictation” was interpreted as spoken instructions in the current learning workflow.

## Integration

`TrainingNarrator(host) { uiIsHindi }` preserves existing callers. Use `show(text, "en-IN")` for explicitly English-authored text, or `showLocalized(en, hi, sat)` for available authored versions. `languageProvider` can supply another genuine selection. `showControls()` opens the compact audio dialog; `compactControls()` is available for an existing learning panel. Continue calling `pause()`/`close()` on the host lifecycle; the narrator also observes its host lifecycle defensively.

The app's actual language selector currently stores only English/Hindi through `preferences.hi`. The Santali row remains a review-pending notice, not a working language selection. The narrator supports strict `sat` selection but this change does not claim Santali content or voice availability.

## Evidence and limits

- **7 JVM tests passed:** stale callbacks after step replacement, sentence continuation, repeated render suppression, language change cancellation, empty content, bounded multilingual/Unicode chunks, and strict deterministic offline voice selection.
- Main Kotlin compilation passed after integration. On the API 36 emulator, a final **five-test batch passed**: native audio language/48 dp controls/mute/speed/Back, recall without an unrecorded hint, closing audio before leaving the mission, automatic Hindi selection/mute without evidence changes, and clearing stale narration for an untranslated lesson.
- A separate **landscape 200% text test passed** after visual inspection exposed a clipped Voice settings control. Voice settings now lives in the native dialog footer; both it and Done remain visible. The portrait and landscape screenshots were inspected. These six final checks used app SHA-256 `95c0956d8585339ee813004aa27d05b8a8b343ea299d930e5b9e5a942199b186`; later casualty/fire builds have separate evidence. Logs and screenshots are in `artifacts/surakshaar-0.13.0/validation/narration/`.
- The first final landscape run hit an Espresso focus error after dismissing a nested speed dialog; the preserved failing log is not counted as a pass. The test now sends Android's actual system Back event and verifies the audio dialog closes while the lesson remains open.
- Actual sound output, device-specific pronunciation, interruptions during a call, installed offline Hindi voices, TalkBack and Bluetooth/headset behaviour still require physical-device checks. Emulator UI checks are not auditory validation.
- New clinical translations remain draft or absent; speech does not constitute clinical approval.

## Primary references checked

- [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech) — asynchronous initialization, voice selection, bounded input and engine cleanup.
- [Android TextToSpeech.Engine](https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine) — installed versus network voice features.
- [Android UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener) — per-utterance callbacks may arrive on multiple threads.
- [Android audio focus](https://developer.android.com/media/optimize/audio-focus) — speech audio attributes, focus request result, interruption handling and Android 15+ foreground restriction.

Applicable rulebook checks: A11Y-04/06/07/08 (labels, transcript/text, scaling, stop controls), NAT-04/05 (native semantics and lifecycle), STA-01/MIC-01 (loading, missing voice, pause, completion, retry), PRI-01 (no recording/upload), and GOV-04 (separate tested state from device/audio claims).
