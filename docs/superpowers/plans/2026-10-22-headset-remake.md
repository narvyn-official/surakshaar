# SurakshaAR headset remake implementation plan

Goal: a learner can choose a current Fire Safety or First Aid lesson, calibrate a Cardboard viewer, insert the phone and finish/resume an offline stereoscopic lesson using its button and head direction.

Architecture: retain Android/Kotlin, the authored catalogue, trusted account workflows and existing handheld AR. Add a native Cardboard runtime with one GL owner, shared original equipment/anatomy geometry, spatial lesson controls and a separately attributed local VR journal. Rebuild the mobile entry journey around concise Continue / Learn / Explore / Progress tasks and explicit Headset VR entry.

Stack: existing native Views/OpenGL ES, Google Cardboard SDK v1.35.0 (pinned source with license/provenance), NDK/JNI, SQLite and installed offline Android speech. No new cloud provider. User hardware: Cardboard-style phone viewer with a button.

## Constraints and acceptance

- School context retains awareness-only eligibility in setup and direct VR Activity entry.
- QR viewer calibration precedes use; Cardboard v1 defaults require explicit selection. A missing gyroscope/native renderer yields a useful handheld route, never an uncalibrated headset claim.
- Each eye uses SDK eye pose/projection and distortion correction; head tracking owns camera pose. No simulated walking, camera auto-orbit or changes to the real-world AR driver.
- Button selection and optional dwell share one target command. Pausing, focus loss, stale frames, leaving a target and content changes cancel pending selection. Back opens the stereo pause menu before exiting.
- Every menu/instruction/action needed after insertion is visible in both eye views. Text has bounded pages and predictable next/back; no native single-screen popup inside the viewer.
- VR records are profile/content scoped, replay validated and distinguishable from AR/physical assessment. No existing learner records/certificates or authored answers change.
- Content is the current English draft; preflight explicitly accepts English when needed. No silently substituted speech or invented translations.
- Existing light identity: navy/teal with functional warm Fire accents; 48 dp controls, native focus/labels and short-screen/large-text recovery. No prohibited decorative symbols.
- No head-pose/camera upload, provider activation, deployment or publication in this implementation request.

## Work packages

1. Native runtime — `CardboardBridge.kt`, C++ bridge/CMake and pinned SDK preparation; verify compiled ABIs, profile persistence, per-eye matrices and distortion path. Missing/invalid state fails closed to setup.
2. Lesson domain — `VrLearning.kt`, `VrLearningStore.kt`; test wrong/retry/correct/next, observation gates, resume, tampering, stale content and cross-profile isolation. Keep original curriculum hashes.
3. Mobile shell/setup — `MainActivity.kt`, `HeadsetActivity.kt`; concise home/catalog/explore, preflight viewer/sensor/input/language, discoverable cancel and history. Test real setup navigation, unsupported sensor state and school filtering.
4. Headset journey — `VrTrainingActivity.kt`, `VrSceneView.kt`, `VrPanels.kt`, gaze/comfort helpers; render two corrected eyes, original models, step cues, choices, feedback, pause/replay/recenter, explicit exit and resumable local records. Unit test ray/plane, dwell reset/debounce and bounded text; emulator-test actual rendered stereo, button input, pause/focus and recovery.
5. Delivery — build/unit/lint, relevant existing learner regressions, normal/short/large-text rendered inspection, APK signature/secret scan, content hash comparison and a dated report with measured results/untested hardware limits. Keep unrelated dirty work.

## Review focus

### Added scope: real-time hand interaction

The user confirmed an uncovered rear camera and requested live surroundings with hand gestures. Add an opt-in, on-device hand landmark pipeline consuming the existing ARCore CPU stream; no second camera owner. A separate exploration workspace supports pointing, intentional pinch/release, bounded grab/drag and two-hand scale/rotation. It cannot classify arbitrary physical objects or award assessment/certification evidence. Camera frames and landmarks are transient; disclose the inference SDK's usage/performance telemetry before enabling it.

Headset live AR presents the same single-camera image to both eyes through the calibrated Cardboard compositor. Label this experimental monoscopic passthrough, require a seated clear space, and do not claim stereo depth, reliable occlusion or safe room navigation. Use ARCore as the sole camera/pose owner; Cardboard supplies lens correction only in this mode. Preserve separate VR lessons and existing handheld workflows.

Acceptance: bundled verified model and pinned SDK; bounded asynchronous inference; correct image/display mapping; stale/lost/ambiguous hands cancel interaction; neutral rearming prevents startup grabs; targets lock during gestures; transform bounds and touch alternatives; permission/unsupported/error/recovery states; Back and pause release resources; School rules and current-profile fences; emulator fixtures explicitly labelled, with physical hand accuracy/latency untested at the user's request.

Viewer mismatch, missing gyro, app interruption mid-dwell, profile changes during loading, and text too long for a stereo panel must have useful recovery. Emulator images establish rendering and flow, not optical comfort, physical motion tracking, thermal endurance or clinical approval. Physical viewer acceptance is recorded separately if a supported device is available.
