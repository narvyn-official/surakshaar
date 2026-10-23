# Camera-based Cardboard AR — 24 October 2026

Development version **0.22.0-dev**, Android version code **42**. This is a locally built development APK, not a physical headset acceptance or production-readiness claim.

## Implemented behavior

- Cardboard opens the same camera-AR preflight as handheld exploration. Separate VR player/room renderers and their installed activity are removed. The compatibility route leads to AR consent. Historical VR decision records are retained as read-only history and are excluded from Home's resumable practice suggestions.
- ARCore remains the single camera/pose owner. Capture aspect follows the configured GPU texture, independently of the physical eye viewport. MediaPipe consumes the existing bounded 480-pixel CPU image and its actual ARCore crop/rotation transform.
- Hands publish before optional person segmentation. A worker-owned schedule limits matting cadence and skips it if the hand input is already over 70 ms old. One inference remains in flight; total worker cost still controls capture pacing. These are implementation bounds, not a measured phone-latency improvement.
- Current camera pixels are restored over geometry through fresh person masks and a soft landmark-guided local hand region. The local region is limited to two stable, confident, complete hands; it expires after 160 ms and is retired on pause, geometry change and disabling hands. It can reveal surrounding background and cannot establish physical hand/object depth. Manual see-through remains available.
- One camera/scene/foreground image is rendered, then mapped into the two calibrated eye projections and passed to the existing Cardboard lens-distortion renderer. Camera rays outside the captured field render black instead of stretching edge pixels. No stereo camera depth or fabricated eye translation is introduced.
- One-hand dragging now includes bounded lifting, with a camera-facing constraint through the actual grip. Two-hand translation, scale and vertical-axis rotation retain the grip pivot, including height. Open-palm cancellation, tracking loss and interruptions restore the transaction's original full pose. This remains projected landmark input, not full metric 6DoF hand reconstruction.
- Selection/holding/two-hand hints, haptic acknowledgement, smaller headset overlays, a lens alignment grid and an optional Session check are available. Session check retains only bounded in-memory timing aggregates and committed action observations. It cannot certify accuracy, comfort, practical skill or hand visibility.
- Authored lessons, assessment/certificate records, account server configuration, anatomy assets and offline content are unchanged. No new provider, package, model, upload, scheduled job or backend deployment is introduced.

## Verification status

Local raw output is under `artifacts/headset-ar-2026-10-24/`.

| Area | Status / scope |
|---|---|
| Unit behavior | **415 passed**, zero failures/errors/skips; includes projection rays, capture layout, hand-region freshness/quality, vertical bounds/pivot, scheduling and metrics |
| GPU and offline inference | Passed; generated camera/geometry colours, asymmetric reprojection, camera-field edges, actual SDK lens rendering and pinned MediaPipe models on labelled static fixtures; no live-hand claim |
| Native journeys | **30 distinct targeted Android checks passed**, plus a repeated headset-preflight check at 200% text; debug variant on API 36 ARM64 emulator. Consent, first permission grant/recovery, school restrictions, profile switch, recreation, Back, main navigation and learning-evidence regressions; emulator only |
| Visual inspection | Portrait/landscape preflight and large-text recovery inspected; normal viewport has no swipe scrolling. At 200% text, explicit paging is the accessible overflow alternative |
| Physical phone / headset | **Not tested**. No USB-debugging phone was visible; the user chose software/emulator checks for this pass |
| Live ARCore on this emulator | **Unavailable**. ARM64 AVD; installer reports a bind failure. The permission test validates explicit recovery, not successful camera tracking |
| Authentication, tenant backend, web dependency audit | Not rerun / unchanged. No backend or web modification; previously reported web dependency findings are not represented as resolved |
| Clinical / curriculum approval | Unchanged, outside this rendering/input change |

The first UI run encountered the emulator's System UI ANR/keyguard dialog. Its original failure log and event evidence are retained. Two older navigation tests also still expected the pre-existing three-tab/old-library-label design; they were corrected to the existing four destinations and current library control without changing those product routes. The recovered run passed. The new read-only-history test initially used a premature accessibility-root query; waiting for the real dialog through Espresso fixed the test, and record/evidence equality checks passed. An added renderer-contract check constructs the actual scene GPU targets and every optional panel without opening a camera; its images are explicitly generated fixtures.

## Primary guidance checked

- [Cardboard Android NDK quickstart](https://developers.google.com/cardboard/develop/c/quickstart): viewer profiles, lens renderer and button integration.
- [Cardboard lens distortion reference](https://developers.google.com/cardboard/reference/c/group/lens-distortion): per-eye projection/mesh ownership.
- [ARCore CameraConfig](https://developers.google.com/ar/reference/java/com/google/ar/core/CameraConfig): CPU image and GPU texture dimensions are distinct. The headset view follows GPU texture dimensions.
- [MediaPipe Android Hand Landmarker](https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android): existing pinned hand pipeline and running-mode contract.
- [ARCore emulator support](https://developers.google.com/ar/develop/java/emulator): Google currently supports x86/x86_64 AR emulators, not the ARM64 AVD used for native UI/GPU checks here.

The projection mapping is implemented locally from the frustum equations. Existing SDK/model licenses and notices remain bundled. The existing light tactile design is preserved; the spatial canvas/inspector and explicit-control guidance informed the optional controls without adding an external visual library.

## Physical acceptance still required

Use the actual phone and manufacturer's viewer QR. From a seated clear area, verify that real camera imagery is upright and comfortable, then place a model on a textured surface. Check each hand independently; point/pinch to select, move/lift, release, use two pinches to resize and turn, and cancel with an open palm. Check fast movement, partial/out-of-frame hands, reacquisition, looking away/back, Back/pause, headset button and a sustained session. Check actual foreground alignment and halos visually; Session check only reports software timing/action observations. These checks have not been completed by emulator inference or generated GPU fixtures.

## Delivery boundary

Signed APK: `surakshaar-0.22.0-dev-release.apk`, 96567173 bytes. SHA-256: `814067021aeff076cec1884d772ee925fb0923d2888b3122fc72dd12030c6089`. Signature matches 0.21.0; zip alignment and all 64-bit native 16 KiB LOAD segments verified. Both pinned on-device models are included; test photos and TinyFish key prefixes are absent. The existing public account origin is preserved.

Debug build, test APK, release signing and lint tasks passed. Lint still reports 76 warnings and the existing baseline retains three SDK QR permission findings; this is not a clean-security or complete-device-coverage claim. No fresh dependency audit was run.

Local signed APK only. Public headset acceptance remains blocked on physical verification. No recurring collection, server deployment, account change or new public release is implied by this report.
