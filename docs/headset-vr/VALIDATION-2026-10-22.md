# Cardboard implementation validation — 22 October 2026

## Delivery and scope

Version **0.20.0-dev**, Android version code **38**, package `com.narvyn.suraksha`. This is a signed development preview for device acceptance, not a claim of completed physical headset or clinical validation.

The local APK is `artifacts/surakshaar-0.20.0-dev/surakshaar-0.20.0-dev-release.apk` (96,316,896 bytes). SHA-256: `53ff5e7ee29b46fe6437d4cdee738db79329ea4bb09bac3e03edb6d85e216274`. The signing certificate matches the existing 0.19.1-rc.1 distribution. Existing installations signed with that identity can update without uninstalling. Publication and deployment were not performed. The final package also includes the added [live hand-interaction workspace](../hand-interaction/README.md), whose separate [validation report](../hand-interaction/VALIDATION-2026-10-22.md) covers camera/gesture scope.

The learner shell now separates Home, Learn, Explore and Progress. Home has distinct handheld and headset entries; headset setup checks eligibility, sensors, viewer calibration, input, seated-space acknowledgement and English consent. All 20 existing authored scenarios / 94 steps have stereo presentation; curriculum answers, translations and clinical review statuses were not rewritten.

## Implementation files

| Responsibility | Files |
|---|---|
| Native stereo, head tracker and lens correction | `CardboardBridge.kt`, `app/src/main/cpp/`, `cardboard-sdk/`, Android Gradle/settings |
| Learner entry and preflight | `MainActivity.kt`, `HeadsetActivity.kt`, `HeadsetIcon.kt`, `HeadsetLessonPolicy.kt`, manifest |
| Stereo workflow and local narration preference | `VrTrainingActivity.kt`, `TrainingNarrator.kt` |
| Rendering, visible command bounds and head-direction input | `VrSceneView.kt`, `VrGraphics.kt`, `VrPanels.kt`, `VrInteraction.kt` |
| Verified lesson-specific anatomical assets | `VrAssets.kt`, `VrPartDetail.kt` |
| Profile-scoped validated local journal | `VrLearning.kt`, `VrLearningStore.kt` |
| Verification | headset/domain/setup/store tests, `scripts/test-cardboard-native.sh`, `scripts/verify-cardboard-source.py` |

All Kotlin paths above are under `apps/android/app/src/main/java/com/narvyn/suraksha/` unless stated otherwise. A substantial pre-existing dirty working tree was preserved. No broad reset, staging or commit was performed.

## Verified checks

| Check | Evidence |
|---|---|
| Android debug/test APK build, JVM tests and lint | `artifacts/hand-interaction/build-permission-recovery.txt`; build passed. **368 tests, zero failures/errors/skips**. Lint has zero errors and existing/advisory warnings; this is not a warning-free claim. |
| Distribution APK build and signing/ZIP alignment | `artifacts/hand-interaction/release-permission-recovery.txt`, `artifacts/surakshaar-0.20.0-dev/android-signature.txt` |
| Final APK inspection | `artifact-verification.json` beside the APK: retained release identity, 15 native libraries inspected, required arm64/x86_64 16 KiB LOAD alignment, Cardboard bridge alignment on all ABIs, packaged SDK licenses, no TinyFish credential prefix and no static test fixtures. Legacy 32-bit ARCore uses 4 KiB LOAD alignment and is not represented as a 16 KiB Android ABI. |
| Native pose/lens invariants | `sh scripts/test-cardboard-native.sh` passed: view convention, finite validation, default-lens round trips and bounded unusual-profile solves. |
| Vendored SDK integrity | `python3 scripts/verify-cardboard-source.py` verified **130** pinned source/generated files. |
| Authored content preservation | Four catalogue/curriculum SHA-256 values match the pre-change baseline; `artifacts/headset-remake/content-preservation.json`. |
| Native Home/course entry and School preflight/back/recreation | **2 instrumentation tests passed**, `artifacts/headset-remake/setup-final-pass.txt`. Source review also confirmed direct-entry School enforcement and English consent. |
| Stereo and persistence iterations | Earlier runs exercised per-eye SDK offsets, Fire wrong/retry/next/completion, Back/pause/resume, scald scene changes and EGL recreation, actual sternum/trachea mesh selection, thermal gating, voice preference isolation, gaze fallback, journal isolation and corrupt-record retention. Individual outcomes and failures are retained in the run logs; these are not presented as a complete final-suite pass. |
| Final selected headset journeys | Fire evacuation, adult collapse with required mesh observation and scald completed via synthetic emulator gaze; optional tibia description did not invent observation evidence; held gaze enlarged only once and disabling dwell remained disabled. Separate simulated button checks passed Fire wrong/retry/pause/resume/completion and cancelled/held/unfocused input rejection. Evidence: `artifacts/hand-interaction/emulator-acceptance.txt`, `recovery-checks.txt` and `simulated-button-acceptance.txt`. Failed test-harness sequences remain in the initial log; recovery was rerun after correcting invisible target requests. These checks do not establish physical button mechanics. |

## Rendered inspection

Native Home, course catalogue, School readiness and chooser were captured and inspected in portrait. Stereo Fire, feedback, pause, completion, scald steps and anatomical specimens were also inspected during implementation. This exposed and corrected plinth occlusion of controls, first-use fullscreen confirmation placement, and the setup Back control's proximity to transient system bars. The final stereo controls render above teaching props.

The first isolated emulator used Android 36 / arm64, host GPU, 4 GiB RAM. The original emulator's release-signed app was not uninstalled; a separate `Suraksha_Headset_API36` AVD was created for synthetic learners. Subsequent emulator failures included Android System UI, keyboard and Google speech-service ANRs and window-focus loss. Captured ANR evidence is under `artifacts/headset-remake/`; those system failures are not counted as passed app checks. A software-rendered restart used 2 GiB and two cores. Test animations were disabled, the device unlocked, and airplane mode enabled to exercise offline operation. Google TTS was temporarily disabled only in this isolated AVD for text-fallback checks.

Earlier crash causes (accessing insets before the decor view, and unregistering an unregistered thermal listener) were fixed and rebuilt; failed logs remain available.

## Performance evidence and emulator limits

The software-rendered preview has a separate 1280 × 576 pixel budget on this 2400 × 1080 emulator. The real-phone rendering budget is unchanged. In the final software-emulator sample, Fire rendered at **7.76 FPS** and scald/anatomy at **5.44 FPS**, measured over the renderer's last two-second window after a six-second warm-up. The metric instrumentation check passed for both scenes; JSON evidence is in `preview-metrics-fire-final.json` and `preview-metrics-scald-final.json` under `artifacts/headset-remake/`. These short, host-load-sensitive samples establish that stereo rendering continued; they do **not** establish acceptable headset performance or an improvement on every scene.

The input gate deliberately rejects stale targets and restarts dwell after a frame gap over 250 ms. Software-emulator stalls prevented some earlier button/dwell journeys from completing reliably. A subsequent host-GPU emulator run completed the selected gaze and simulated-button journeys. Some other failures were test-harness errors: requesting a control absent from the current page, and combining four journeys into one test timeout. The tests were corrected/split, with no relaxation of the input guard. Failed logs remain available. Real-phone sustained performance and input acceptance remain prerequisites for public headset release.

## Physical acceptance still required

- Manufacturer viewer QR scan, permission denial/cancellation, optical alignment and comfortable text in the actual viewer. The explicit Cardboard v1 profile is not a generic viewer calibration.
- Phone gyro orientation, prediction/recentering, landscape rotations, button mechanics and optional dwell. Emulator head direction is synthetic and is explicitly excluded from release builds.
- Sustained phone frame rate, motion-to-display response, memory and thermal endurance. Emulator timings do not establish headset comfort or a phone performance guarantee.
- Audible English voice quality, installed/offline voice failure recovery, audio interruptions and speaker/headphone behaviour. Preference logic checks do not prove audible playback quality.
- Screen-reader/native accessibility testing on a device. In-viewer targets are visual head-direction controls; handheld learning remains the alternative for ordinary assistive navigation.
- Qualified review of the existing draft clinical lessons and anatomy. Headset decisions are labelled separately from supervised physical skill assessment and cannot automatically create practical certificates.

Current headset lessons are English drafts with explicit consent; this implementation does not claim new reviewed Hindi/Santali headset lesson translations. Account entry remains before insertion. No new AI/speech provider, learner data upload, recurring safety collection or backend deployment was enabled.
