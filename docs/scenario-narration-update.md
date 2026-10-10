# Scenario visuals, narration and staff references

Local development update, 10 October 2026. Android version `0.14.2-dev` (31).

## What changed

- Narration uses connected phrases, a single start per request and neutral pitch. New installations start at normal speed; saved speed preferences survive. Installed offline voices are ranked by language, locale and the engine's quality metadata. Audio includes a saved voice choice and preview without consuming the lesson's position. Deliberately unsafe answer choices are not read as instructions.
- First Aid maps each step of the eight current casualty scenarios, plus the historical twelve-stage bay, to a shared AR/3D presentation. Scene props illustrate the current action instead of leaving an identical injury marker throughout the lesson. Cooling water, adhered clothing, loose covering, dressings, embedded-object protection, cushioning, limb supports and heat-illness care are distinct visual states. Motion can be frozen and replayed. Direct model targets invoke existing actions; button alternatives remain available.
- CPR hand placement is calibrated to the loaded sternum and skin geometry in both anatomy packs. The illustration uses thin stacked hands above the lower half of the sternum; its small visual motion does not teach or measure compression depth or rate. An early Detail tap is retained until the viewport is ready and cancelled if the lesson step changes.
- Fire adds scenario props and inspection states for the existing authored content. Seven context models represent paper/wood, liquid fuel, electrical equipment, gas cylinders, metal swarf, cooking oil and sand. Room stages show alarm, damaged equipment, obstruction, smoke, assembly and handover states. View magnification has pinch, decrease, reset and increase controls. AR magnifies camera and projection together; it does not move anchors or change physical distance evidence.
- The local Safety Updates pilot adds proposed NHS burns and cuts references and separates demo fixtures from TinyFish collections. Staff can filter their queue by provenance. Last successful collection comes from actual collection state, not a fixture's timestamp. Institution review decisions remain isolated.

## Boundaries

These are instructional illustrations, not measurements of real injuries, breathing, pressure, CPR quality, temperature, gas or extinguisher pressure. An animation is not a treatment timer. The original anatomy asset is not a clinically validated articulated patient; separate pose guides illustrate bent-limb positioning without distorting anatomy. The current scald case concerns the forearm, not every possible burn location.

No lessons, answers, curriculum fingerprints, certificates or previous attempts are changed by staff reference approval. New scene controls do not create assessment credit. Voice quality depends on the installed Android speech engine; metadata selection alone does not prove that a voice sounds natural. No additional speech or summary provider is introduced.

TinyFish credentials stay in ignored local backend secret storage. The two new references are proposed and require administrator approval. Scheduling remains disabled. This update is local, with no VPS deployment or GitHub publication. No new live TinyFish collection is claimed by these changes.

## Verification

Evidence is stored locally under `artifacts/scenario-upgrade/`.

- Android debug app and instrumentation APK built; **306 JVM tests passed** with no failures or skips. Android lint completed with zero errors and **58 warnings**; this is not a warning-free release claim.
- **22/22 final emulator regression tests passed** in 120.503 seconds (`android-verified-final.txt`). Covered workflows include voice controls, seven Fire illustrations, Fire inspection/zoom, rendered Fire decisions, all eight First Aid scenes, saved casualty progress, skin-only clothing and restart/continue choices.
- The **direct-touch check passed**: touching the visible blue water control advances the cooling step exactly once, feedback rejects repeated touches, and a sleeve obscured by the body cannot be selected through it. These are actual emulator touch events and GL projections, not fabricated AR camera evidence.
- Voice controls passed **two landscape tests at 200% text size**. On the final APK, **two First Aid scene tests and one Fire controls test passed at 200% in landscape**. The Fire test exercises three saved phases, full paged instruction access, useful model area, zoom and unchanged lesson records. The First Aid checks include queued early Detail taps and actual projected CPR hand/chest bounds. Fresh CPR close-ups, scald cooling/covering and Fire fit/zoom/details captures were visually inspected. See `screenshots-final/`, `screenshots-first-aid-large-final/` and `screenshots-compact-final/`.
- Web: **117 tests passed**, TypeScript, ESLint, Node production build and collector-worker build passed. The isolated HTTP suite passed **33 status assertions** plus authentication, cross-workspace isolation, duplicate jobs, audit, stale decisions and curriculum-separation checks, without provider requests.
- Provider tests exercise absent credentials, whitespace-only keys, invalid/public-private URLs, redirects, malformed/oversized responses, rate limits, timeouts, partial failures, retry limits, cancellation and source revocation. Deterministic hash deduplication and last-success preservation are covered.
- The dashboard was inspected at **1280×900 and 390×844**, including native keyboard filter interaction, proposed-source readiness, empty live results and labelled demo data. Mobile document width matched its viewport; the filter no longer displays an older response under a newly selected origin. Captures: `safety-updates-desktop.png` and `safety-updates-mobile.png`.
- Both new NHS URLs passed the strict public-network preflight. This did not call TinyFish or approve either source. The local database migration completed with integrity and foreign-key checks.
- SHA-256 comparisons against the published source checkout confirm that `FirstAidScenarioCatalog.kt`, `FireRoomScenarioCatalog.kt`, `FirstAidBay.kt` and `FireResponseLearning.kt` are unchanged. Existing questions, correct answers and pinned lesson definitions therefore remain intact.

Earlier failed/interrupted runs remain in the evidence folder. They exposed an early Detail-tap race, overly tight CPR framing, and a dialog-root timing issue in the test. A long-running software-rendered emulator also reported a System UI ANR; it was cold-started with host GPU and 4 GB RAM, retaining app data. The affected five checks passed after that restart. Final results use the rebuilt APK on that emulator configuration; they are not a low-end-phone performance benchmark.

The user requested emulator checks for this update. **New physical-device AR alignment, placement under motion, thermal/battery performance, actual Hindi/English listening quality and TalkBack interaction were not verified.** Voice preview control tests do not establish audible speech quality. The clinical content and anatomy remain source-checked drafts awaiting qualified review; procedural illustrations are not photorealistic patient models or a substitute for supervised physical practice.

## Changed files for this update

The workspace also contains unrelated earlier changes. The paths below identify this update's scope; they are not an instruction to stage the whole working tree.

| Area | Files |
| --- | --- |
| Narrator | Android `TrainingNarrator.kt`, new `NarrationPlayback.kt`, `RoomLearningDialog.kt`, and the instruction text in `RoomMissionActivity.kt` |
| First Aid rendering | Android `AnatomyScene.kt`, `AnatomyMaterials.kt`, `CasualtyGeometry.kt`, `FirstAidBayActivity.kt`, `FirstAidEquipment.kt`; new `FirstAidSceneGuide.kt`, `FirstAidTeachingProps.kt`, `FirstAidTeachingPicking.kt`, `FirstAidCprPlacement.kt` |
| Fire rendering and controls | Android `WorldEquipment.kt`, `EquipmentActivity.kt`, `RoomMissionView.kt`, `RoomMissionActivity.kt`; new `FireScenePlan.kt`, `InspectionMagnification.kt` |
| Android verification | New `NarrationPlaybackTest.kt`, `RoomLearningNarrationTest.kt`, `FirstAidSceneGuideTest.kt`, `FirstAidCprPlacementTest.kt`, `FireScenePlanTest.kt`, `InspectionMagnificationTest.kt`, `NarrationControlsTest.kt`, `FirstAidTeachingSceneFlowTest.kt`, `FirstAidDirectPropFlowTest.kt`, `FireSceneControlsTest.kt`; resume-selector correction in `CasualtyScenarioFlowTest.kt` |
| Android packaging | `apps/android/app/build.gradle.kts`: development version 0.14.2, code 31 |
| Safety Updates backend | `apps/admin-web/drizzle/0021_safety_first_aid_sources.sql`, `lib/safety/model.ts`, `lib/safety/store.ts`, `app/api/safety-updates/route.ts` |
| Safety Updates UI | `apps/admin-web/components/training/SafetyUpdates.tsx`, `app/globals.css`; new `lib/safety/query-state.ts` |
| Safety Updates verification | `apps/admin-web/tests/safety-updates.test.ts`, `tests/safety-updates.integration.ts`; new `tests/safety-query-state.test.ts` |
| Documentation | This report, `docs/narration-device-voices.md`, `docs/learning-flow/first-aid-scene-matrix.md`, `docs/fire-scene-models-and-zoom.md`, `apps/admin-web/docs/safety-updates-pilot.md` |

## Local APK

`artifacts/scenario-upgrade/surakshaar-0.14.2-dev-debug.apk` — 38,400,619 bytes, Android 10+ (minimum API 29), version code 31. APK Signature Scheme v2 verification passed. This is a development/debug build, not a newly published GitHub release or a production-signing claim.

SHA-256: `1a7b923a65736d7234dd14d2b0936abf2318eb53127767e70f0d5821d4e49ed2`.

The APK and 27 frontend build files passed the TinyFish secret-pattern scan. The key remains in a Git-ignored backend file with mode `0600`; the APK contains no TinyFish credential. General credential setup, cost controls and operator/source-approval requirements are in the pilot guide below. Neither installing this APK nor adding a key deploys the local dashboard/collector to the public account server.

## Primary references

- [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [Voice](https://developer.android.com/reference/android/speech/tts/Voice).
- [TinyFish Fetch reference](https://docs.tinyfish.ai/fetch-api/reference), [authentication](https://docs.tinyfish.ai/authentication).
- [NHS burns and scalds](https://www.nhs.uk/conditions/burns-and-scalds/), [cuts and grazes](https://www.nhs.uk/conditions/cuts-and-grazes/).
- [Resuscitation Council UK: 2025 adult basic life support](https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/adult-basic-life-support-guidelines), used to check the illustrated chest landmark; this is not a claim of clinical approval.
- [US Fire Administration extinguisher guidance](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/index.html).

Backend setup, the full proposed source list, staff permissions and collection limits are documented in [Safety Updates pilot](../apps/admin-web/docs/safety-updates-pilot.md). Device speech setup is documented in [On-device narration](narration-device-voices.md). Visual coverage is listed in the [First Aid scene matrix](learning-flow/first-aid-scene-matrix.md) and [Fire scene guide](fire-scene-models-and-zoom.md).
