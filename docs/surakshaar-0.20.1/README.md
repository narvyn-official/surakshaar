# SurakshaAR 0.20.1 — hand-control refinement preview

Signed Android 10+ testing build, version code 39; existing release signing identity retained.

- Speed-adaptive pointer filtering, deliberate target locking and release without finger-opening drift.
- Two-hand movement, off-centre rotation/resize pivots, continuous turns and touch-pointer handoff.
- Up to 20 Hz adaptive inference offers with one frame in flight; configured limits are not measured phone performance.
- Single-render monoscopic headset AR, reused HUD textures and clearer alternative-control availability. VR eye regions remain separate.
- Correct handling of left/right classification scores, preserving valid landmark detections with uncertain handedness.

386 JVM tests and 8 selected Android checks passed across documented runs; three initial UI failures recovered after dismissing an observed emulator System UI ANR. Generated GPU colour bands tested the real mono/stereo compositor routing. This is not a full instrumentation-suite or live-hand claim.

**Physical phone testing remains incomplete:** USB transport failed and wireless pairing was not completed. The headset was unavailable. Live hand accuracy, actual gesture smoothness, sustained device performance, lens comfort and physical button mechanics are unverified. Single-camera headset AR remains a seated monoscopic experiment, with no stereo camera depth or reliable hand occlusion. Existing draft-content and non-certifying-practice boundaries remain.

Existing web production-dependency audit findings remain outside this Android change; backend/web were not changed or deployed. No all-CI/security-pass claim is made.

[Verification and limits](https://github.com/narvyn-official/surakshaar/blob/v0.20.1-preview/docs/hand-interaction/VALIDATION-2026-10-23.md) · [Hand controls](https://github.com/narvyn-official/surakshaar/blob/v0.20.1-preview/docs/hand-interaction/README.md)

APK SHA-256: `37797844b99ea94033ac81b6f2037bbc969c08ace7d2a63a4b4cbeaaf3d94e58`.
