# Seated AR start fix — 24 October 2026

Development patch **0.22.1-dev**, Android code **43**, based on the published 0.22.0 preview. This fixes the grey Start button; it does not establish physical headset acceptance.

## Cause and resulting behavior

Cardboard setup required a landscape configuration before enabling Start but never requested that configuration. A phone locked to portrait could therefore remain on a disabled button after completing the other prerequisites. A native regression test reproduced this failure before the fix.

Cardboard setup now requests sensor landscape, including when system auto-rotate is disabled. Phone mode releases this orientation request. Entering setup, rotating, checking consent or recreating an Activity still cannot open the camera. Explicit Start, the camera-processing acknowledgement, seated clear space, uncovered rear camera, a valid viewer profile and Android camera permission remain required. Active camera sessions keep their existing orientation lock.

A persistent English/Hindi readiness message beside Start identifies the next missing requirement. At normal text size, the tested landscape setup fits one screen; at 200% text the existing Earlier/More controls expose overflow while Start and its explanation remain reachable. No swipe scrolling was added.

## Current checks

| Check | Result and scope |
|---|---|
| Reproduction | Failed on the original implementation: Cardboard remained portrait with auto-rotate off. The missing-requirement explanation also failed its new regression check. |
| Unit tests | **415 passed**, zero failures, errors or skips. |
| Android flows | **14 distinct targeted checks passed** on API 36 ARM64 emulator: HandsArFlowTest (9), HeadsetSetupFlowTest (4), and systemBackPopsMainHistoryAndSurvivesRecreation (1). Includes the ready button, actual Start dispatch, first camera permission continuation, explicit consent, school restrictions, learner switching, recreation, history and native Back. |
| Text and language | Two relevant checks repeated at 200% text; readiness checked in English and Hindi at normal and large text. Rendered portrait, landscape and large-text screenshots inspected. |
| Build and lint | Debug, instrumentation and signed release APK builds passed. Lint: zero reported errors, 76 existing warnings; existing SDK permission baseline remains. This is not a clean-security claim. |
| Distribution | Signature matches 0.22.0. Zip alignment and all 64-bit native LOAD alignments support 16 KiB. Pinned models and public account origin preserved. Test fixtures excluded; no TinyFish key prefix found in inspected text/DEX assets. |
| Physical camera and viewer | **Not tested**. Start dispatch/permission recovery on this ARM64 emulator is not evidence of ARCore tracking, hand recognition, foreground alignment, lens fit or comfort. The physical acceptance list in the [0.22.0 report](VERIFICATION-2026-10-24.md) still applies. |

The first combined native run found that the existing route test closed a destroyed pre-rotation Activity returned by its monitor. It now inspects and closes the resumed Activity; the complete 13-check group then passed. Visual verification also caught an intermediate footer causing unnecessary paging at normal text size. Its regression test failed with two pages before the compact footer was corrected and verified at one page. Original failure output is retained locally under `artifacts/seated-ar-start-2026-10-24/`.

## Evidence and limits

- [Ready headset setup](screenshots/seated-start-ready.png)
- [200% text setup](screenshots/seated-start-large-text.png)
- [Hindi requirement explanation](screenshots/seated-start-hindi.png)
- [Current test output](evidence/seated-start-native.txt), [large text](evidence/seated-start-large-text.txt), [Back and language](evidence/seated-start-back-language.txt), [unit totals](evidence/seated-start-unit-summary.json), [APK verification](evidence/seated-start-artifact-verification.json).

Android's current [sensor landscape guidance](https://developer.android.com/guide/topics/manifest/activity-element#screen) was checked: sensor landscape permits either landscape direction even when sensor rotation is locked. Android can ignore orientation requests on some large-screen or windowed configurations; the UI retains the landscape readiness gate and explains the Phone/full-screen recovery. Such devices were not tested.

Learner content, models, hand rendering/manipulation, assessments, certificates, server authorization and offline storage are unchanged. Web/backend checks, dependency audits, clinical review and physical AR testing were not rerun for this UI patch. No server deployment or recurring collection is included.

Signed APK: `surakshaar-0.22.1-dev-release.apk`, 96,583,557 bytes. SHA-256: `c2e3b8890cd1450004fe00987fb087e60a4c9308fbab325f70d74b27ad9c8477`.
