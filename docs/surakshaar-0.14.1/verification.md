# SurakshaAR 0.14.1 — resume choices and body layers

10 October 2026. Android 10+, `0.14.1-dev` / version code 30. Development preview; no backend deployment or recurring collection is part of this release.

## Changes

- A meaningful unfinished lesson or practical attempt offers **Continue** and **Start from step 1**. Continue retains its identity and saved decisions; restart creates a separate attempt and retains the earlier record. Fresh practice does not need this choice.
- The Fire room choice shares its existing safety briefing. Restart keeps the chosen scenario, presentation and coaching mode. In-session Options also offers **Start from step 1**.
- Training shorts follow actually visible external skin, including an explicitly enabled skin overlay. Skeleton, muscle and organ views without skin no longer draw clothing or let invisible clothing block selection. Hiding skin and isolating internal structures also removes clothing.
- The garment occupies a smaller waist/upper-thigh region. Covered external skin remains excluded from rendering and picking; existing body pose transforms are shared with clothing.
- Includes the earlier local learning-flow refinements: direct lesson selection, next-step completion actions, all 117 anatomy descriptions in English/Hindi and detailed-model lifecycle cleanup. [Earlier implementation evidence](../learning-flow/verification.md) · [Anatomy references](../learning-flow/anatomy-sources.md).

## Acceptance and evidence

- Final Android build: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` passed. **278 JVM tests**, zero failures/errors/skips. Lint has **zero errors and 53 existing warnings**; no zero-warning or comprehensive accessibility claim.
- Final APK standard emulator coverage: 23 distinct targeted tests passed across the main run and focused rerun. These cover exact saved IDs, continuation, fresh starts, preserved earlier bytes, cancellation, recreation, active restart, school restrictions, Home/course navigation, required anatomy observation, matched descriptions, detailed clothing coverage and all three saved Fire inspection/route states.
- Main run `emulator-final.txt` had two test-harness failures: Back deliberately finished an isolated root activity, and an accessibility substring query selected the dialog title instead of its action. The harness now asserts expected destruction and selects the exact clickable action. All four affected Fire/resume checks pass in `emulator-final-fire.txt`; failed logs remain available locally.
- Earlier candidate component checks also completed a casualty bleeding/hold sequence and cancelled/returned anatomy observation (two tests in `emulator-resume-and-practice.txt`; their source is unchanged in the final build). These are 3D emulator interactions, not physical medical skill or AR tracking measurements.
- Enlarged-text checks: all three Fire resume/restart/cancel checks passed at 200% text in portrait and landscape (`emulator-final-large-text-fire.txt`, `emulator-final-landscape.txt`). Lesson restart and First Aid continue also passed at 200% (`emulator-final-large-text.txt`). Final rendered panels were inspected: full action labels remain visible; lengthy safety text uses the existing paging control in landscape. A transient test assertion ran before Back completed; the harness now waits for expected destruction, and the focused checks pass.
- APK content validator and curriculum contracts passed for the packaged content. Signature verification passed; the certificate matches the previous published preview, and version code 30 supports an in-place update.
- Changed source and APK passed targeted secret-pattern scanning. No backend credentials, account databases, device screenshots from a real phone or learner records are released.

A prior 200% screenshot exposed clipped platform dialog buttons even though accessibility action injection could activate them. The Fire resume choice now uses a bounded custom panel with fixed actions, and the test requires visible button bounds and native minimum height. Screenshots are inspected in addition to action assertions. The blank launch-transition capture was discarded; capture waits for actual rendered controls.

| Release scope check | Result |
| --- | --- |
| Resume/restart, history ownership and cancellation | Pass in synthetic emulator fixtures; earlier learner/assessment/credential data preserved |
| Body layer render and picking consistency | Pass; skin-only coverage, explicit overlay, hidden skin and isolated parts checked |
| Configuration recreation | Pass; selected attempt retained |
| Process-death and storage-failure injection | Not newly tested by these changes |
| New backend/auth, payments, AI providers, data migrations | N/A: none changed or deployed |
| New external art/assets/dependencies | N/A: none imported; existing licence notices retained |
| Full production acceptance | Not established; see boundaries below |

Affected rulebook checks: FLOW-07 (resume), UX-04/TRN-03 (retain earlier records), TRN-07 (recreation), OVR-02/05 (dismissal and scaled text), GFX (render/pick consistency), DSO-05 (exact artifact identity). No identity, certification or curriculum logic is changed. Existing Safety Updates work is not included in this Android-only source update.

## Boundaries

Only an emulator was connected during this update. Physical ARCore tracking/placement, audible narration quality, sustained phone thermal performance and full manual TalkBack journeys were not revalidated. Source-checked educational content still requires qualified clinical/fire and translation review. The APK remains debug-signed; this is not a production or clinical release. Existing incomplete Santali, disabled email recovery and previous preview limitations remain. No new credentials or private learner data are included in this release.

## Release artifact

File: `surakshaar-0.14.1-dev-debug.apk` · 38,053,927 bytes.

SHA-256: `21a65abbb78be78b0af6e42b31b094d43983568f58cc1b012ccd08ff6c402cde`.

[Public APK](https://github.com/narvyn-official/surakshaar/releases/download/v0.14.1-preview/surakshaar-0.14.1-dev-debug.apk) · [Release](https://github.com/narvyn-official/surakshaar/releases/tag/v0.14.1-preview). The release tag points to the source commit containing this refinement. Selected synthetic screenshots and this report accompany the APK; checksums cover all uploaded files.
