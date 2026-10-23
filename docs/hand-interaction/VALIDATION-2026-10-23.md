# Hand-control refinement validation — 23 October 2026

## Deliverable and acceptance decision

Signed **0.20.1-dev**, Android code **39**, package `com.narvyn.suraksha`. APK: `artifacts/surakshaar-0.20.1-dev/surakshaar-0.20.1-dev-release.apk`, **96,333,312 bytes**, SHA-256 `37797844b99ea94033ac81b6f2037bbc969c08ace7d2a63a4b4cbeaaf3d94e58`. The release signing identity is retained.

This is a development/testing build. **Physical phone and headset acceptance are not complete.** The Samsung was detected by macOS USB, but ADB transport reads repeatedly failed. SDK 37 native and libusb backends and an isolated official SDK 35 client did not establish a usable USB connection. Wireless debugging advertised the phone and its TCP port was reachable, but the ADB connection timed out and pairing was not completed. No app install or live hand test on that physical phone is claimed. The user reported the headset unavailable.

## Implemented changes

- Pinch selection latches the first deliberate pinch target. Opening the fingers freezes the last held pointer through release confirmation, avoiding a placement shift. Neutral rearming, freshness, ambiguity and interruption cancellation remain enforced.
- Aspect-corrected, speed-adaptive pointer filtering reduces synthetic still-hand jitter while following deliberate motion, without predicting unobserved input. Renderer interpolation fills the gaps between inference results. These are software properties; physical perceived smoothness has not been measured.
- Two-hand translation, scale and yaw share the actual off-centre grip pivot, including the effective bounded scale. Relative rotation unwraps across the half-turn boundary. Releasing one touch pointer rebases the remaining grip rather than jumping.
- Inference offers are paced up to 20 Hz, backing off with measured worker cost. There is still only one frame in flight. This configured ceiling is not a measured phone input rate.
- Left/right classification probability is no longer mistaken for landmark quality. Uncertain handedness remains UNKNOWN; the adapter's internal admission marker is not a measured confidence probability. The official [MediaPipe Android result contract](https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android#handle_and_display_results) was rechecked for this correction.
- The monoscopic camera/model/HUD scene renders once at the previous per-eye resolution. Both calibrated Cardboard meshes sample that image. Stereoscopic VR retains separate eye image regions. Single-camera AR still has no stereo camera depth or reliable real-hand occlusion.
- HUD uploads reuse the texture and are capped at 10 Hz. Native alternative controls refresh labels and enabled states; unavailable placement/model actions cannot be triggered. School/profile, privacy, camera recovery and separate non-certifying records remain intact.

## Checks and evidence

Evidence is under `artifacts/hand-controls-phone-2026-10-23/` unless noted otherwise.

| Check | Result |
| --- | --- |
| Regression reproduction | Pass: `gestures-red.txt` reproduced release drift, delayed target selection and reversed long rotation. `motion-red.txt`, `pacer-red.txt`, `pointer-red.txt`, `quality-red.txt` and `native-red.txt` preserve the missing-behaviour failures before correction. |
| JVM checks | **386 passed**, zero failures/errors/skips. `unit-results.json`, `final-build.txt` and the signed-release build's `release.txt`. |
| Debug app/test build and lint | Pass. Advisory warnings remain; the same three reviewed upstream QR permission findings are filtered by the existing narrow library baseline. |
| Static model inference | Four Android checks passed on official labelled static fixtures and an empty image. These are not live-camera results. |
| Android workflow recovery | Initial seven-test run: four passed, three failed while System UI's ANR dialog held focus. Screenshot and window-focus evidence retained. Closing that observed system dialog, without changing app code or test input guards, allowed all three failed checks to pass. `android-checks.txt`, `android-recovery.txt`, `emulator-recovery.png`. |
| Native GPU routing | Pass: a generated red/green/blue texture produced separate left/right stereo regions and the same centre region in both shared-image eye views. This exercised the actual JNI/GL compositor, not a camera. `compositor-check.txt`. |
| Selected Android result | **8 distinct selected tests have passing latest results**, across the initial, recovery and compositor runs. `android-results.json`. This is not a full Android-suite claim. |
| Native source/math | Pinned 130 Cardboard/generated sources verified; pose, lens bounds and stereo/shared-image UV assertions passed. |
| Rendered inspection | Handheld consent/readiness and stereo Fire screens inspected. This is not a live Hands AR UI or optical/comfort acceptance. |
| Packaging | APK signature, 16 KiB ZIP alignment, required arm64/x86_64 native LOAD alignment, 15 native libraries, model hash and notices verified. Test fixtures and the TinyFish credential prefix are absent. Scoped inspection is not an exhaustive security audit. |
| Content/workflows | Four authored content hashes unchanged. Backend, account, assessment and certification source were not modified. |

## Remaining physical checks

A supported phone and a working authorised debugging connection, or explicitly documented manual device observations, are needed for live surface placement and the camera → inference → selection/drag/scale/turn/release loop. Check tracking loss, hand overlap, both orientations, lighting/occlusion, off-centre grips, pauses/re-entry, touch alternatives and model bounds. Measure camera/inference age, sustained frame/display rate, CPU/memory and thermal behaviour on the actual phone before claiming performance or accuracy.

With the actual viewer, verify its manufacturer QR, optical alignment, both eyes' readable controls, camera visibility, physical button behaviour and comfort in a clear seated area. No emulator/static fixture establishes those properties. New experimental controls remain English; reviewed new Hindi/Santali content and clinical approval are not claimed.

The existing web dependency audit also failed on the prior 0.20.0 source run because high/critical production dependency findings were reported. Its Android CI job passed. That separate backend/web issue was not changed or deployed in this Android follow-up; the system must not be described as having all CI/security checks passing.

**Production readiness: blocked pending physical acceptance and the separately reported existing web dependency findings.** This report supports a development testing build only.
