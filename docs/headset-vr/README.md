# Headset learning

SurakshaAR now has a native Cardboard mode alongside handheld AR. The phone renders a separate view for each eye through Google Cardboard's calibrated lens compositor. The current authored Fire Safety and First Aid scenarios can be completed, paused, restarted and resumed without taking the phone out to use ordinary touch menus.

## Start a lesson

1. Open **Home → Headset VR** and choose a lesson. Accounts and profile selection happen before inserting the phone.
2. Scan the viewer manufacturer's Cardboard QR code. The **Cardboard v1** option is only for Google's original v1 viewer, not a universal lens preset.
3. Choose button selection or gaze dwell, confirm a clear seated area, and accept the English lesson language. Handle Android's first-use fullscreen confirmation here, before insertion.
4. Start the lesson and insert the phone in landscape. Look at **Begin lesson** and press the viewer button. If the button does not respond, look at **Use gaze selection** until its ring fills.

A short viewer-button press selects the highlighted command. Holding it for at least 1.1 seconds recentres the view. Enter, Space, DPAD centre and controller A are also supported. Dwell selection requires 1.6 seconds of continuous head direction; it is not eye tracking. Selection is suspended during a button hold, an interruption, or a content change. Look away from controls before dwelling on a new page.

**Pause** opens the stereo menu. **Replay** repeats the current safe instruction; wrong-answer distractors are never narrated as directions. **Viewer and model controls** changes input, rotates/enlarges a model or resets it. Enlarging models does not change the headset's field of view. Android Back first opens Pause; Exit returns to setup. Resume and Start from step 1 preserve earlier records as separate attempts.

## Learning and evidence

- All 20 authored scenarios / 94 steps use the existing lesson and spatial catalogues. Step-specific First Aid props, equipment and sources remain tied to those authored steps. Content is still explicitly labelled as draft where it was draft before this change.
- Required anatomy observations use mesh intersection on an enlarged original anatomical structure, alongside a translucent clothed body for context. Selecting a text button alone does not award observation evidence.
- Incorrect decisions require Retry; correct decisions require Next. Required observations gate progression.
- VR history is stored locally in a separate profile- and content-scoped journal. It is marked **headset decision practice**, **not physical competence**, and is not automatically eligible for practical certification or uploaded through the existing assessment pipeline.
- School profiles retain awareness-only access in the chooser and at direct Activity entry. The current headset lessons are English; setup offers explicit English consent rather than speaking untranslated content as Hindi or Santali.
- Installed Android offline voices are used. Headset mute is independent of the handheld automatic-voice preference. Missing voice support leaves all instructions readable.

## Implementation

`HeadsetActivity` owns preflight. `VrTrainingActivity` owns the in-viewer workflow and narration. `VrSceneView` owns GL, head pose, input targets and stereo rendering; `CardboardBridge` is its narrow JNI boundary. `VrLearningSession` owns validated lesson transitions; `VrLearningStore` owns append-only SQLite persistence. `VrPanels` paints text and its corresponding visible command bounds together. `VrAssets` loads a bounded, hash-verified balanced anatomical subset without changing handheld model quality.

One head view is predicted at boot-clock time +30 ms. Each eye uses the SDK's eye-from-head transform and viewer projection. Rendering goes to a bounded side-by-side framebuffer, then the real SDK lens meshes composite to the display. UI texture painting runs outside the GL frame loop, geometry buffers are cached, and EGL/native resources are recreated after interruption. Severe thermal status hides the teaching model, pauses decisions and blocks Resume until Android reports recovery.

Native code is built for arm64-v8a, armeabi-v7a and x86_64. SDK source, hashes, Apache-2.0 license, protobuf generation and the bounded lens-solver patch are documented in [the Cardboard module](../../apps/android/cardboard-sdk/README.md).

## Boundaries and device acceptance

Cardboard supplies orientation tracking with a neck model. This mode does not claim physical room positioning, tracked hands, a real casualty diagnosis or verified hands-on skills. Handheld AR retains its separate ARCore anchoring workflow.

The public build requires real motion sensors and a saved valid viewer profile. Synthetic head direction is gated to debug builds on emulator hardware and visibly labelled **EMULATOR PREVIEW**; it is absent from the release path. Initial QR pairing can need Google Play Services barcode assets. Already-paired lessons, models and local records do not require a collection provider or a live account server.

Before public headset release, test the actual phone and viewer together: correct manufacturer QR, camera permission/scan cancellation, eye alignment and readable text, movement/recentering in both landscape rotations, repeated physical button presses, dwell fallback, focus interruption, offline resume, sustained frame rate and thermal behaviour. Comfort, audible voice quality and optical/head-motion performance cannot be certified from an emulator. Existing clinical draft labels and supervised-practice requirements remain applicable.

## Build and verification

Use the existing Gradle wrapper with Android Studio's JBR, SDK 36, NDK 27.1.12297006 and CMake 3.22.1. Preserve the approved `SURAKSHA_API_ORIGIN` when building a public account-enabled APK. Signing is handled by the existing project-local release script; signing secrets are not part of this document or the deliverables.

Run `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` and `lintDebug`; then the headset setup, stereo flow and journal instrumentation tests on a separate emulator. `sh scripts/test-cardboard-native.sh` checks native pose/lens invariants and `python3 scripts/verify-cardboard-source.py` verifies pinned sources. The dated [validation report](VALIDATION-2026-10-22.md) records actual results and remaining physical checks.
