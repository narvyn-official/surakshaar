# Live hand interaction validation — 22 October 2026

## Deliverable

Signed **0.20.0-dev**, version code **38**, package `com.narvyn.suraksha`. Local APK: `artifacts/surakshaar-0.20.0-dev/surakshaar-0.20.0-dev-release.apk`, **96,316,896 bytes**. SHA-256: `53ff5e7ee29b46fe6437d4cdee738db79329ea4bb09bac3e03edb6d85e216274`.

This is a development preview. The user explicitly requested emulator checks only. Physical live-hand recognition, sustained camera latency and headset comfort are not verified. No deployment or GitHub publication was performed.

## Implemented scope

- Opt-in live AR workspace linked from Home, Explore, headset setup, body exploration and step inspection.
- Real MediaPipe on-device hand landmark inference consuming ARCore's current camera image; one camera owner, one bounded inference worker, bundled verified model, no cloud inference dependency.
- Visible hand landmarks/cursors, deliberate pinch/release, locked object selection, placement-plane dragging, bounded two-hand resize/rotation, open-palm cancellation, touch and viewer-button alternatives.
- Extinguisher, AED, clothed body, verified linked anatomical parts and step-specific First Aid teaching props. Part descriptions use anatomy knowledge; this mode does not identify arbitrary physical objects or create medical skill evidence.
- Experimental headset passthrough uses the same camera image for both eyes with actual Cardboard lens correction. It requires an uncovered rear camera, calibration, landscape and a clear seated space. It is monoscopic, without true stereo camera depth or reliable hand occlusion.
- Current-profile and School-context fences, explicit consent before camera/inference, permission recovery, pause/focus/heat cancellation, Back navigation, and no automatic camera restart after Activity recreation.
- Existing authored catalogues, assessment answers and certification pipelines remain unchanged. Four catalogue/curriculum hashes match the pre-change baseline.

## Verification evidence

Paths below are relative to `artifacts/hand-interaction/` unless specified otherwise.

The latest results cover **20 distinct selected Android tests across the recorded runs**, plus three large-text/layout variants. `selected-android-results.json` identifies those tests and explicitly excludes a full Android-suite claim. Public headset release is **not accepted** pending physical camera/hand, optical, input and sustained-performance validation; the local software preview is the deliverable.

| Check | Status and evidence |
|---|---|
| Debug app/test build, JVM tests, lint | **Pass**: `build-permission-recovery.txt`; **368 JVM tests, 0 failures/errors/skips**. Lint has zero errors and advisory/pre-existing warnings. |
| Gesture domain | **Pass**: 19 synthetic-landmark tests include neutral arming, pinch hysteresis, tap/drag distinction, target lock, stale/duplicate/lost input, confidence/identity/geometry changes, open-palm rollback and bounded two-hand transforms. `gesture-domain-standalone.txt` and Gradle results. These are not camera accuracy tests. |
| Camera mapping and identity | **Pass**: 14 JVM tests cover rotated/cropped mappings, YUV plane strides/bounds/colour, bounded copies, stable identities and a distinct second hand joining. Included in the 368 total. |
| Genuine offline model inference | **Pass**: 4 Android checks verified the bundled model hash, actual CPU inference on officially sourced one/two-hand static fixtures, an empty image, and disabled/closed engine state. Airplane mode was enabled. Fixtures are labelled and confined to the test APK. This does not prove live ARCore-image inference on a phone. |
| Preflight and isolation | **Pass across initial/recovery runs**: explicit consent, uncovered-camera and viewer requirements, recreation without camera startup, School adult-illustration rejection and closure after learner switching. The learner-switch test initially expected RESUMED even though the app correctly finished; it now uses real task navigation. |
| First-use camera permission | **Pass**: `first-camera-permission.txt`. A real Android one-time permission grant now continues the user's requested session without a second Start tap. Manual inspection exposed callback/resume ordering; the fix retains the request only within the current Activity, rechecks consent/profile/viewer and clears it on Back. Activity recreation does not automatically start the camera. The emulator then displayed a recoverable AR startup error, not a tracked camera view. |
| Related headset journeys and local journal | **Pass across selected runs**: Fire/collapse/scald via synthetic gaze, required actual mesh observation, optional anatomy without fake evidence, repeat-gaze prevention, input mode toggle, profile isolation and corrupt-record retention. Initial 17-test run had 15 passes and 2 test-harness failures; both corrected sequences passed in `recovery-checks.txt`. |
| Simulated button input | **Pass**: 2 checks in `simulated-button-acceptance.txt` cover Fire wrong/retry/pause/resume/completion and held/cancelled/orphan/unfocused input rejection. This is injected Android touch/key input, not a physical Cardboard-button test. |
| Large text / landscape | **Pass**: 3 checks in `final-large-text-layouts.txt` exercise 200% text, native Home/course entry, School headset setup and hand-workspace camera/viewer acknowledgements in landscape. Rendered inspection exposed a clipped Back label and split navigation words; the final UI uses a labelled Back icon and a two-row navigation layout for large portrait text. Screenshots are under `final-large-text-screenshots/`. |
| Signed release and packaging | **Pass**: `release-permission-recovery.txt`; existing signing identity retained; ZIP alignment verified by the release script. `../surakshaar-0.20.0-dev/artifact-verification.json` records 15 native libraries, required arm64/x86_64 16 KiB LOAD alignment, packaged notices and correct model hash. No static test fixtures or TinyFish credential prefix were found in the release APK. This is a scoped inspection, not an exhaustive secret/security audit. |
| Native Cardboard | **Pass**: pose/lens host invariants and 130 pinned SDK source/generated hashes, recorded in the companion [headset report](../headset-vr/VALIDATION-2026-10-22.md). |

## Boundaries and untested areas

The engine/gesture logic, actual static-image inference, Android preflight and selected stereo lessons were tested. The complete **live ARCore camera → hand inference → anchored object manipulation** loop has not been accepted on physical hardware. Accuracy across lighting, gloves, skin tones, occlusion and rapid movement, sustained frame rate, thermal endurance and real controller mechanics remain unverified. Do not infer these from static fixtures or synthetic gaze.

The ARM64 Android 36 test AVD could not start the AR camera: ARCore's installer reported a failed service binding and a runtime exception (`emulator-ar-startup.txt`); the app returned to its explicit recovery screen. This is not counted as successful live AR. Google's current [ARCore emulator guide](https://developers.google.com/ar/develop/java/emulator) and [supported-device list](https://developers.google.com/ar/devices) specify x86/x86_64 AVDs for ARCore testing. The supported physical phone/viewer remains the acceptance route for this Mac's setup.

The hand workspace controls virtual models and its own commands. It does not recognise every object visible in the room, track metric hand depth, detect hazards, diagnose injuries, assess CPR compression or issue certification. Current experimental workspace controls are English; reviewed new Hindi/Santali hand-mode translations are not claimed. Existing lesson review/draft labels and supervised practice requirements remain applicable.

Images and landmarks are transient in this application. MediaPipe's vendor usage/performance metrics are separately disclosed before enabling it; no supported public SDK telemetry opt-out was found. No learner identities, assessments or institution documents are supplied to the hand model by this feature. See [engine provenance](../hand-tracking/ENGINE.md) for current official documentation, exact versions and notices.

## Changed file groups

- Inference: `HandTrackingEngine.kt`, `HandCameraMapping.kt`, `HandIdentityTracker.kt`, pinned `tasks-vision:1.1.1`, bundled model and MediaPipe license/notice assets.
- Gesture domain: `HandGestureController.kt` and tests.
- Workspace: `HandsArActivity.kt`, `HandsArSceneView.kt`, `HandsArGraphics.kt`, non-exported manifest entry.
- Entries/layout: `MainActivity.kt`, `HeadsetActivity.kt`, `AnatomyActivity.kt`, `LearningSpatialActivity.kt`.
- Verification: hand engine/domain/mapping/identity/preflight tests, official test-only image fixtures, scoped APK inspection and reports.

Kotlin implementation paths are under `apps/android/app/src/main/java/com/narvyn/suraksha/`. The substantial pre-existing dirty tree was preserved; no broad reset, staging or commit was performed. Backend authentication, email, recurring collection and deployment configuration were not changed.
