# SurakshaAR 0.12 development candidate

This document preserves the earlier checkpoint. The [0.12.1 refinement build](../surakshaar-0.12.1/README.md) adds compact view controls, expanded anatomy/content and further integration work; use its separate APK and validation record for the latest local changes.

This is a locally built Android 10+ review candidate, **not an approved clinical course or production release**. Version `0.12.0-dev`, version code 25. No publication, deployment or external endorsement is implied.

## Review build

- [Verified debug APK](../../artifacts/surakshaar-0.12.0/surakshaar-0.12.0-dev-debug.apk) — 18,295,106 bytes; local artifact, not published.
- [SHA256SUMS.txt](../../artifacts/surakshaar-0.12.0/SHA256SUMS.txt).
- APK SHA-256: `6492c376eaeaccbd9f829ae60d8489233d63e018a43abb99b87762567809c7d6`.
- [Exact copied-APK validation](distribution-apk-validation.json), [native validation summary](native-validation.json), [full verification ledger](verification.md).
- [33.6-second anatomy demo](../../artifacts/surakshaar-0.12.0/demo/surakshaar-0.12.0-demo.mp4) — edited 3D emulator footage from APK `0a3d2811…`, before the later cadence adjustment; not live-camera AR or clinical approval. [Capture scope and editable assembly](../../artifacts/surakshaar-0.12.0/demo/README.md).

## Included

- Fire Safety and First Aid launch courses: **12 draft scenarios / 62 authored decision stages**, with school-awareness boundaries, source links, narration controls, explanations, mistakes, hints, retry and versioned practice records.
- A separate **12-stage adult-collapse practice bay** connecting anatomy recognition, authored response/breathing cues, help delegation and virtual AED sequencing. It records simulated learning, not CPR quality or physical pad-placement competence.
- Native anatomy exploration with **68 named structures/groups**, 198,384 triangles, skin/skeleton/selected-muscle/organ layers, mesh touch selection, focus/isolate/hide, rotate/zoom/pan controls, AR placement, a visible placement guide and tabletop/full-size choices. It is simplified static adult anatomy, not a complete body atlas or paediatric model.
- Bundled offline model/content, local profile isolation, interruption-safe learning records and preservation of earlier records. Optional explicit private practice upload has profile/account checks and disclosure; it does not automatically share with institutions or issue qualifications.
- Contextual authored help and a server-side AI integration boundary. Live AI requires configured provider credentials and approved applicable sources; draft safety content is not treated as approved AI instruction.

The industrial library and historical assessment/certificate paths are preserved. This candidate does not claim that every new institution, notification or optional downloadable-pack workflow is finished.

## Evidence

| Area | Recorded result |
|---|---|
| Android | 203 JVM tests passed; build passed; lint 0 errors / 47 warnings |
| Focused native UI | Three emulator tests passed normally and again at 2.0× font: visible mesh picking, complete 3D bay/recovery and observation handoff |
| Model/package | 17 validator tests; exact source/APK integrity passed; 66 CC BY 4.0 parts and two separately licensed CC BY-SA 2.1 Japan lung derivatives |
| Curriculum | 203 structural/authored-decision assertions passed; 12 scenarios remain draft |
| Backend | Owner-reported 60 tests, types/lint/build and local dev/production-preview integration passed; dependency audit still has 8 high findings, 0 critical |
| Physical AR | Samsung SM-S921B: user-reported stable attachment on `f93c3f…`; placement/tracking recovery on `0a3d…`. Brief 30.3fps intervals followed by lower cadence/critical thermals and a transient native-memory peak; sustained performance has not passed. Final `6492c376…` installed and Home opened; AR stress was deferred. [Device report](phone-performance.md) |

See [acceptance matrix](acceptance-matrix.md), [content review](content-review.md), [first-aid bay contract](first-aid-bay.md), [migration/recovery plan](migration-recovery.md), [source asset report](asset-validation.json), [curriculum report](content-validation.json), [native screenshots](../../artifacts/surakshaar-0.12.0/emulator/) and [test logs](../../artifacts/surakshaar-0.12.0/), [phone performance](phone-performance.md) and [AR resource review](ar-resource-review.md).

## Before public release

Qualified clinical/fire-instructor review, Hindi/Santali review of the new lessons, representative learner/TalkBack/smaller-screen checks, production authentication/privacy/signing configuration, upgrade rehearsal and unresolved dependency findings remain open. New school deployment requires reviewed institution/guardian/privacy arrangements.

The physical check found brief 30.3fps rendering followed by about 20fps, critical thermal status and a transient memory peak. The final build reduces unnecessary render work and native camera-wrapper acquisitions; those changes are not a demonstrated fix for that peak or a sustained-performance pass. A cooled comparison, named mid-range phone, full first-aid/fire AR journeys, measured drift, 20-minute sessions and repeated-entry memory/thermal checks remain required. A live AI provider and approved content must be configured before any claim that online AI teaching has been verified.

Model limitations and licence obligations are explicit. Source checks, a stable anchor report and successful tests do not establish clinical correctness, practical competence, learning effectiveness or universal device compatibility.
