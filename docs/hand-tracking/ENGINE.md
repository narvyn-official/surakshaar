# On-device hand landmark engine

The optional hand-input engine shares the existing ARCore camera. It does not create a CameraX/Camera2 session or acquire camera ownership. Enable it only after the app's hand-controls explanation and permission flow; retain ordinary touch controls and an explicit way to turn it off.

`HandTrackingEngine(applicationContext)` exposes `setEnabled(Boolean)`, `offer(Frame, displayRotation, width, height, trackingReady, sessionEpoch)`, `latestSnapshot()` and `close()`. The GL/session owner calls `offer` while that ARCore frame is current. `HandTrackingSnapshot.frame` contains the shared `HandTrackingFrame` types used by `HandGestureController`; X and Y are normalized to the actual mono camera viewport. The same mono view may be shown to both eyes in a viewer, but this does not create stereo camera depth. Z remains the model's relative estimate, not metres or a measured distance to a real object.

## Resource and lifecycle limits

- One inference worker and one frame in flight, with a configured maximum of 20 offered frames per second, paced down using observed inference cost; no inference or full-image RGB conversion on GL/UI threads.
- Copy at most 480 × 480 YUV samples, observing crop, each plane's position, limit, row stride and pixel stride. The Android `Image` closes before asynchronous work starts. The copied YUV arrays and temporary ARGB array are erased after use; the Bitmap and MPImage close/recycle. Images and landmarks are never logged, stored, uploaded or attached to learning evidence by this application.
- CPU VIDEO mode, at most two hands, 21 landmarks per hand; no extra cloud provider. Temporal model tracking avoids repeating full palm detection on every frame.
- Capture `IMAGE_NORMALIZED → VIEW_NORMALIZED` affine coordinates on the current frame. Derive the quarter turn from that transform, physically rotate the bounded inference image, unrotate model points, restore the original crop, then apply the captured mapping. An asynchronous worker never calls `Frame.transformCoordinates2d` on an old frame.
- Use Android camera timestamp to reject images over 100 ms behind the corresponding frame, and elapsed realtime timestamps to reject results older than 250 ms. Display geometry, orientation, session, tracking loss, disable and close invalidate old generations immediately. A paused/unavailable/stale snapshot exposes no actionable hand frame.
- Conservative wrist/handedness association preserves transient IDs when output list order changes. Crossing, overlap, discontinuity and ambiguous association cancel gesture ownership; these IDs are not biometric identities. Full/partial offscreen hands and malformed coordinates fail closed rather than being clamped onto a visible target.
- Disabling immediately clears actionable input. Native detector destruction is queued on its worker after any running invocation, so UI/session shutdown does not wait on inference. Native load/model failure gives a visible unavailable state and leaves touch available; switch the option off and on to retry.

## Pinned dependency and provenance

Verified on 22 October 2026 against Google's [Maven metadata](https://dl.google.com/dl/android/maven2/com/google/mediapipe/tasks-vision/maven-metadata.xml): `com.google.mediapipe:tasks-vision:1.1.1` and transitive `tasks-core:1.1.1`. The optional LiteRT-LM dependency is not required by this engine. No new AI text/speech provider is used.

The model is Google's [Hand Landmarker float16 version 1 bundle](https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task), SHA-256 `fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1`, 7,819,105 bytes. The engine verifies the bundled model digest before creating the task. Its linked [official model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20Hand%20Tracking%20(Lite_Full)%20with%20Fairness%20Oct%202021.pdf), page 2, declares Apache License 2.0. SDK/model license and SDK bundled third-party notices are packaged in `content/licenses/mediapipe/`. Exact download hashes and native inspection are in `UPSTREAM.json`.

All four native MediaPipe AAR ABI libraries inspected from Google's current `tasks-core:1.1.1` have 16,384-byte LOAD alignment. This includes arm64-v8a, x86_64 and armeabi-v7a; no device-specific runtime claim follows from ELF alignment alone. The final merged APK must also be checked because the app includes other native libraries. ABI/runtime failures are caught as an unavailable hand-input state; no fallback replaces the live camera with fixtures.

## Privacy disclosure

Google's [MediaPipe Tasks privacy notice](https://developers.google.com/edge/mediapipe/solutions/tasks) states that input data is processed on device and not sent to Google, while API usage/performance metrics may be sent to Google. Inspection of the pinned AAR confirms a real `TasksStatsProtoLogger` and Android Data Transport dependencies. There is no supported public telemetry opt-out in `HandLandmarkerOptions` or `BaseOptions` inspected for this release. The app must disclose this distinction before enabling the option, especially for school/institution use. Do not describe this dependency as having zero vendor network traffic. Offline landmark inference does not need a downloaded runtime model or a network request; the model is bundled.

## Verification boundaries

Deterministic unit tests cover all four rotations, crop/affine projection, independent padded/interleaved plane strides, odd crop dimensions, bounded downsampling, truncated/invalid buffers, malformed or offscreen landmarks, stale/future snapshots and transient identity continuity/ambiguity. `HandTrackingEngineTest` uses explicitly labelled official Google static hand fixtures bundled only in `androidTest` assets, plus an empty generated image, to exercise genuine on-device model inference. Fixture images are not product assets or live camera results; source/license evidence is in `UPSTREAM.json`.

Build/test results belong in the task's verification report after the commands run. Static fixture inference cannot verify a person's hands through a headset, physical phone latency, lighting, skin-tone coverage, gloves, camera-to-eye alignment, world-space occlusion or safe navigation. User-requested emulator checks must not be reported as physical hand-tracking acceptance. The model card identifies limitations for gloves/occlusion, low light and motion blur. Hand gestures are optional UI input for virtual teaching objects, not a measurement of first-aid technique or a medical/safety decision.

## 23 October input follow-up

`HandInferencePacer` bounds offers to a minimum 50 ms interval, backs off with observed worker cost, and keeps the existing one-frame-in-flight rule. `HandPointerFilter` uses aspect-corrected, filtered velocity to vary its low-pass cutoff, plus a time-based motion bound. Neither adds a queued frame backlog or predicts past observed input. Freshness and identity cancellation remain in force.

The official [result contract](https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android#handle_and_display_results), rechecked on 23 October, distinguishes handedness classification from landmarks. The previous adapter incorrectly reused the left/right category score as landmark tracking confidence. The corrected adapter treats uncertain handedness as UNKNOWN, validates coordinates and the category range, and relies on the model's configured presence/detection/tracking logic. Its internal admission-floor value is an adapter marker, **not** a measured landmark-confidence probability. No confidence percentage is shown or stored as learner evidence.

The [23 October validation](../hand-interaction/VALIDATION-2026-10-23.md) separates deterministic tests, static fixture inference, generated GPU routing fixtures and the still-blocked physical phone/viewer checks.
