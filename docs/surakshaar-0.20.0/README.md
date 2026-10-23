# SurakshaAR 0.20.0 development preview

[Download the signed Android APK — 96 MB](https://github.com/narvyn-official/surakshaar/releases/download/v0.20.0-preview/surakshaar-0.20.0-dev-release.apk)

Android 10+ · version code 38 · same release signing identity as 0.19.1. This is an experimental prerelease, not a production or field-accepted release.

## Changes

- Native Google Cardboard stereoscopic lessons, viewer calibration, button/gaze controls, recentering, pause, restart and resume.
- Separate opt-in Hands AR workspace with on-device MediaPipe landmarks, pointing, pinch selection, dragging, two-hand scaling/rotation and open-palm cancellation. Touch and viewer-button alternatives remain available.
- Interactive extinguisher, AED, clothed body, linked anatomical structures and step-specific First Aid props.
- Clearer Home, Learn, Explore and Progress navigation; independent headset narration controls and separate profile-scoped VR practice records.
- Existing authored lessons, assessment answers and certification workflows are preserved. VR/hand exploration does not certify practical competence.

## Requirements and limits

Handheld camera AR requires a supported ARCore phone. Headset lessons require compatible motion sensors and the correct viewer QR calibration. Experimental headset live AR also needs an uncovered rear camera and a clear seated space; it displays the same camera image to both eyes, without stereo camera depth or reliable hand occlusion. New headset and hand-mode controls are English; reviewed new Hindi/Santali translations are not claimed.

The complete live camera → hand inference → anchored manipulation loop, physical viewer-button operation, lens comfort, sustained performance and hand accuracy have not been verified on a phone/viewer. Emulator ARCore startup failed on this Mac's ARM64 AVD and produced the explicit recovery screen. Static hand fixtures and synthetic input are not live-hand validation.

MediaPipe inference runs on the device; the SDK's separate vendor usage/performance metrics are disclosed before enabling it. Existing clinical draft and supervised-practice labels still apply. No backend deployment or recurring collection is part of this release.

## Verification

- Fresh publication-source build passed: 368 JVM tests, debug APK and lint. Three exact upstream QR-camera permission-analysis findings are documented in a narrow lint baseline; advisory warnings remain visible.
- 20 distinct selected Android tests passed across documented runs, plus three large-text/layout variants. This is not a full instrumentation-suite claim.
- Real offline model inference passed on official, labelled static fixtures; fixtures are absent from the release APK.
- Pinned native Cardboard source and pose/lens checks passed.
- APK signature, ZIP alignment, bundled model hash/notices and required arm64/x86_64 16 KiB native alignment verified.

[Hands AR verification and limits](https://github.com/narvyn-official/surakshaar/blob/v0.20.0-preview/docs/hand-interaction/VALIDATION-2026-10-22.md) · [Headset guide](https://github.com/narvyn-official/surakshaar/blob/v0.20.0-preview/docs/headset-vr/README.md)

APK SHA-256: `53ff5e7ee29b46fe6437d4cdee738db79329ea4bb09bac3e03edb6d85e216274`.
