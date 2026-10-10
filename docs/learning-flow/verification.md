# Learning flow and anatomy refinement

Local implementation, 10 October 2026. No service deployment, source-code push, curriculum-version change, or certification change. The APK was subsequently uploaded for the owner's requested WhatsApp sharing.

## User paths

- Course → **All lessons** → select a lesson → lesson opens. Removed the intermediate selected-card screen and lesson carousel.
- Returning learner → course → **Continue** restores a validated current-version unfinished attempt. Existing decision history remains intact.
- Fire course → **Practise in AR** or **Practise in 3D** → scenario → existing practice briefing. Removed the second mode/confirmation picker; required safety steps remain.
- Lesson complete → review difficult decisions or open the next lesson. Replay remains visible; optional independent practice and reflection sit under **More practice**.
- Body explorer → select a mesh or named part → a matching short description appears beside the model. One information control opens location and function; focus and audio controls remain directly available.

## Anatomy accuracy and control

`AnatomyKnowledge` maps exact model IDs to English and Hindi descriptions. It covers the 117 shipped named parts/groups; regional remainder meshes explicitly describe a group instead of claiming a specific muscle. Unknown IDs have no inferred anatomy entry. Content sources and limitations are in [anatomy-sources.md](anatomy-sources.md).

Layer changes and hidden parts clear the description and observation eligibility. Delayed touch results are discarded when scene controls have changed. Recreation restores the selected structure rather than overwriting it with the initial lesson target. Narration uses the same description as the inspector and the authored language tag. Verified immutable model geometry is retained across configuration changes, and retired views release their scene references. Cancelling a loader preserves a valid installed pack instead of treating interruption as corruption.

The inspector uses the existing light palette and 48 dp controls with accessible names. In short landscape layouts it moves beside the model. Full descriptions remain available when the compact summary is truncated. Existing explicit paging is retained only for content that cannot fit at large text sizes; the model view remains fixed.

## Boundaries

The evidence engine, learner identity checks, school-awareness restrictions, saved records, first-aid/fire safety sequences, certification workflows, models, and Safety Updates pilot are unchanged. These are source-checked educational drafts, not new qualified clinical, fire-instructor or translation approval.

The refinement follows the personal rulebook's FLOW-01/07/08, NAV-02/03, UQ-02/04/06, and RSP/A11Y control and text-scaling requirements. Android's [accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps) informed touch targets and meaningful icon descriptions. The visual direction stays consistent with the app's existing canvas-and-inspector interface; no new UI library was introduced.

## Verification evidence

Emulator captures and logs are in `artifacts/learning-flow/`.

- Build: `testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug` succeeded. **275 JVM tests, zero failures/errors/skips**. Lint reports zero errors and 53 repository warnings; this is not a zero-warning or full accessibility certification claim.
- Main emulator regression run: **12 tests passed** (`emulator-fixed.txt`). Covers direct entry, resume, school filtering, Fire AR/3D routing, completion evidence, descriptions, layer clearing, recreation, existing compact controls and Home/Learn navigation. This run used the balanced fallback after the initial failure; it is not the detailed-pack evidence.
- The first combined run exposed two failures from memory pressure after detailed-model recreation (`emulator-main.txt`). A subsequent explicitly detailed run exposed a remaining retired-parent reference (`emulator-detailed.txt`, four passed and one failed). Both failed logs are retained. The fix retains verified immutable geometry across recreation, explicitly detaches the retired scene from its parent, and clears stale view references; cancellation no longer invalidates a good pack.
- Final explicitly **Detailed** pack run: **5 tests passed** (`emulator-detailed-fixed.txt`), including Hindi/English selection descriptions, layer clearing, recreation with shared geometry, compact controls, five repeated entries and loader cancellation. Quality is asserted by the tests, not inferred from the filename.
- Adaptive layouts: **3 tests passed** (`emulator-large-text.txt`) covering course entry and anatomy descriptions at 200% font scale in portrait, plus landscape controls at the default scale. A separate **1-test pass** (`emulator-landscape-large-text.txt`) verifies landscape controls at 200% scale. The original system text scale was restored after checks.
- Rendered captures inspected: normal course, selected heart and full description; 200% course and heart inspector; landscape controls at 200%. No clipped controls were found in these captures. `first-aid-course.png`, `heart-selected.png`, `heart-description.png`, `course-large-text.png`, `heart-selected-large-text.png`, and `controls-landscape-large-text.png` are kept beside the logs. These are emulator renders, not physical AR camera evidence.
- Primary source references were checked for factual anatomy. No clinical or translation sign-off is inferred from browsing or automated tests.

Not verified in this turn: physical ARCore camera placement/tracking (only an emulator was connected), human hearing of Hindi/English TTS, manual TalkBack/switch-access testing, and usability with real learners. Previous AR claims are not upgraded by these emulator results.

## Files in this change

Android sources: `LaunchCourseActivity.kt`, `LearningScenarioActivity.kt`, `AnatomyActivity.kt`, `AnatomyScene.kt`, `AnatomyData.kt`, and new `AnatomyKnowledge.kt`.

Tests: new `LearningFlowRefinementTest.kt`, `AnatomyDescriptionFlowTest.kt`, `AnatomyKnowledgeTest.kt`; updated anatomy assertions in `LaunchLearningFlowTest.kt`, explicit Detailed quality coverage in `AnatomyViewControlsTest.kt`.

Documentation: this report and `anatomy-sources.md`. Unrelated existing work remains in the working tree.

## Local build

`artifacts/learning-flow/surakshaar-learning-refinement-debug.apk` is the verified debug APK (version remains `0.14.0-dev`). It is installed on the emulator. On 10 October 2026, the identical file was added as a separately named asset to the existing public `v0.14.0-preview` release for WhatsApp sharing; the older APK, release tag and source code were not replaced. GitHub's asset digest matches the SHA-256 below. The existing tag predates this incremental binary; the local changed-file inventory above records this refinement.

[Public APK download](https://github.com/narvyn-official/surakshaar/releases/download/v0.14.0-preview/surakshaar-learning-refinement-debug.apk)

SHA-256: `8decf8ccef3e494a3b54e94cb368907f53701292b7e47c9065ac880b01d28f75`.
