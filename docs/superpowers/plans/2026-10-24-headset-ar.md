# Camera-only Cardboard AR implementation plan

**Goal:** One camera-based AR workspace for handheld and calibrated Cardboard viewing, with responsive hand manipulation and local hand visibility assistance.

**Architecture:** ARCore continues to own camera and pose. A single bounded MediaPipe worker publishes hands before optional person segmentation. A local landmark-guided camera-reveal matte assists thin fingers; it is approximate foreground priority, not physical depth. One camera/geometry image is reprojected into the two calibrated lens projections before Cardboard distortion, without fabricated stereo camera depth.

**Stack:** Existing Kotlin, GLES2, ARCore 1.56.0, MediaPipe Tasks 1.1.1, vendored Cardboard. No new dependency, provider, camera upload or backend change.

**Spec:** User request of 24 October 2026; preserve PRODUCT.md learning/record constraints. Remove separate VR entry points and simulated-room switching. Retain historical records, reviewed lessons, school restrictions, touch alternatives, Back/pause/thermal recovery and offline content.

## Acceptance and review focus

- No interaction from stale, ambiguous, rotated, unfocused or paused input; cancellation restores all model coordinates.
- Finger/palm reveal uses current camera pixels only; local assistance must not hide unrelated scene geometry or reuse old/session-mismatched hand data.
- Calibrated asymmetric eye projections preserve camera ray alignment, black outside captured FOV, and do not invent IPD depth.
- Models can lift within a bounded height and maintain their grip pivot through two-hand scale/rotation and release.
- No reachable VR mode or virtual-room control; existing VR decisions remain readable without being relabelled as new AR achievements.

## Tasks

- [x] Add failing unit cases for bounded vertical movement/pivot, lens ray mapping, fresh/confident hand regions and segmentation scheduling. Implement the small pure-domain helpers and rerun.
- [x] Integrate hand-first publication, optional throttled segmentation and local camera reveal; test GPU composition with labelled generated data and offline models.
- [x] Reproject the shared camera/scene into calibrated eye textures, add optical-check grid, and test projection/composition on emulator. Keep explicit single-camera limits.
- [x] Route headset access to camera AR, retire VR activities and virtual-room commands, retain read-only historical records; exercise native preflight, Back and consent.
- [x] Build debug/test/release APKs, run affected unit/instrumentation and regression checks, inspect screenshots, record measured results and physical-test gaps.

## Physical testing boundary

Initial USB check: no device visible. The user chose software/emulator checks for now. Phone/headset hand precision, capture-to-display latency, real occlusion, lens comfort, button hardware and thermal endurance remain unverified until hardware is available. Emulator fixtures never count as that evidence.

## Current primary references (checked 24 October 2026)

- https://developers.google.com/cardboard/develop/c/quickstart
- https://developers.google.com/cardboard/reference/c/group/lens-distortion
- https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android
- https://developers.google.com/ar/develop/java/camera-configs
