# Current-build verification ledger

Date: 10 October 2026. Development candidate; not an approved clinical or public release. This ledger distinguishes source/data checks, emulator evidence and physical-device evidence.

## Environment

- macOS host; native Android build uses the bundled Android Studio JBR and the repository Gradle wrapper.
- Installed Blender executable: `/Applications/Blender.app/Contents/MacOS/Blender`, reporting **5.2.2 LTS**, build hash `d13f752e3b9c`.
- Android emulator: `Suraksha_API36`, serial `emulator-5554`, API 36, `sdk_gphone64_arm64`; started with `-no-snapshot-load -no-boot-anim -gpu host`. Boot completed; no data wipe was requested.
- No physical Android device was connected at the initial validation check. Emulator results cannot establish ARCore camera tracking, real-world anchor stability or mid-range thermal performance.
- A Samsung SM-S921B was subsequently connected and inspected by the native implementation owner. Physical observations below are identified by their tested APK; a later build is not automatically covered.

## Executed checks

| Check | Outcome | Evidence / limitation |
|---|---|---|
| Python validator negative tests | 17 passed | `python3 -m unittest discover -s scripts -p test_validate_learning_release.py -v`; tests altered mesh bytes, geometry bounds/count/non-finite data/path escape, missing semantic parts, catalogue omissions/version mismatch, unsupported approval metadata, source assets changing after APK packaging, Android-unsafe `.gz` filenames and incorrect inheritance/omission of the lungs' separate licence. |
| Final source anatomy and curriculum package | Passed | `asset-validation.json`: 68 parts, 198,384 triangles, 3,258,733 compressed bytes / 14,284,192 decoded bytes; 12 scenarios / 62 stages. Includes hash, geometry, runtime size limit, semantic reference and per-source licence checks. This is source-package evidence, not a packaged APK or clinical approval. |
| Blender export cleanup and four layer previews | Completed; visually inspected | `/tmp/surakshaar-012-blender-cleanup.log`; 85 degenerate triangles removed, zero fallback normals needed; four actual renders under `artifacts/anatomy/`. See the coverage limitations below. |
| Targeted new Kotlin tests | 16 passed, 0 failures | Six `AnatomyPickingTest` and ten `LearningAttemptTest` cases; `/tmp/surakshaar-012-unit.log`. Tests ray geometry, replay integrity, isolation, hints and non-certifying observation. |
| Android JVM/lint/debug build checkpoint | 181 tests passed; build passed in 40 seconds | `./gradlew testDebugUnitTest lintDebug assembleDebug`; `/tmp/surakshaar-012-android-check.log`. **Intermediate checkpoint**: anatomy assets were still being optimised and subsequent source edits require a final rerun. This APK was not installed. |
| Current Android JVM suite | 203 passed, 0 failures/errors | `artifacts/surakshaar-0.12.0/final-stable-build.log` and JUnit XML under `app/build/test-results/testDebugUnitTest`. Includes both clock-rollback tests, virtual equipment geometry and six upload-plan tests. |
| Current Android lint and debug/test packaging | Build passed; lint 0 errors / 47 warnings | Same final build log; current Android lint XML. Warnings include known custom-GL accessibility/tool-constructor checks and English draft strings; no whole-app accessibility/localisation claim. |
| Packaged APK anatomy validation | Passed | `distribution-apk-validation.json`: APK SHA-256 `6492c376eaeaccbd9f829ae60d8489233d63e018a43abb99b87762567809c7d6`, 18,295,106 bytes. Exact packaged assets match all 68 compressed source meshes, semantic links and separate source licences. |
| Final focused emulator journeys | 3 passed on APK `6492c376…` | `artifacts/surakshaar-0.12.0/regression-2026-10-10/final-rendered-pixels-launch-flow.log`; 22.727 seconds. Actual mesh load and nonblank GL PixelCopy, touch-picked isolated sternum, 1.3× text controls/Back, full 12-stage 3D bay with wrong action/pause/recreation/resume/old records, and cancelled/successful anatomy handoff. |
| Final 2.0× font emulator journeys | Same 3 passed on APK `6492c376…` | `artifacts/surakshaar-0.12.0/regression-2026-10-10/final-rendered-pixels-large-font-flow.log`; 22.889 seconds. Actual mesh touch selection and Bay buddy/AED action bounds retained. Original data, font, animation settings and camera permission restored after isolated tests. |
| Legacy navigation/assessment/recovery regression | 26 unique tests passed; 4 repeated at 2.0× font | APK `0a3d2811…`, before renderer-only changes. Five MainNavigation, seven AppFlow, three EmergencyFlow, six LearningWorkspace, one Android Back and four permission-denied AR recovery tests. `regression-2026-10-10/summary.json` records exact runs/hashes. Tests use the new Home → Library → earlier course route and retain scoring, certificate gating, replay and recovery assertions. |
| Lint checkpoint | 0 errors, 40 warnings | `apps/android/app/build/reports/lint-results-debug.xml`; four more than the prior baseline: programmatic view constructor, two GLSurfaceView click-accessibility warnings, one hard-coded anatomy label. Manual accessibility not established. |
| Draft curriculum structure | 12 scenarios / 62 stages; 203 authored-data assertions passed | `validate-launch-curriculum.py`, including 33 safety-decision boundaries and correct semantic references; source-check/reviewer scope is recorded in `content-review.md`. Source assertions are not clinical approval. |
| Web account/coach/practice checkpoint | 60 tests, TypeScript, ESLint and production build passed | Backend owner reported `npm run check` completion; `/tmp/suraksha-context-coach-check.log`. Further dependency edits require a new check. |
| Private learning sync, local HTTP integration | Passed on local configured service | Backend owner executed `tests/learning-practice.integration.ts`; `/tmp/suraksha-learning-integration.log`. Migration 0014 applied locally after a permission-restricted test DB backup. This is private account sync, not institutional sharing or deployment. |
| Dependency audit after targeted updates | 8 high findings remain, 0 critical | Backend owner reported Node 24.20 checks passed after framework/toolchain updates. Remaining findings are in the `braces` 3.0.3 dependency chain with no available upstream patch at this checkpoint. Do not treat a successful build as resolution or assert an exploitable production path without analysis. |

The validator writes a report only when an explicit `--report` path is supplied. It never regenerates the source curriculum or original assessment evidence.

Use `python3 scripts/validate_learning_release.py --apk <candidate.apk> --report <report.json>` for the final artifact. This checks the **packaged** manifest and geometry and records the APK SHA-256; checking only source assets could miss an APK built during an asset rewrite.

## Regression isolation and fixture corrections

The regression checkpoint contains 36 successful executions (26 unique legacy tests plus four large-text repeats and six final focused runs), not 36 unique tests. Original app-private databases, preferences and files were restored byte-for-byte: the pre/post tar SHA-256 is `e74dafdb183d6638dc08893df95fcb6f1e1e72b4f604ec06ba082adc2f33f536`. Restoration proof is `artifacts/surakshaar-0.12.0/regression-2026-10-10/restoration-verification.json`; the private backup is excluded from public artifacts. The app was left stopped with the final APK installed.

Initial fixture failures are retained in that directory: old navigation visibility assumptions required the actual page controls; Espresso partial FMA input was replaced by full-ID input plus an exact text assertion; a continuous-frame-count assumption became invalid after dirty rendering. The final rendering check captures actual nonblank GL pixels and requires interaction readiness, keeping anatomy mesh-touch and full Bay action assertions. These changes do not assert physical AR reliability.

The later phone resource measurement and implementation review are in [phone-performance.md](phone-performance.md) and [ar-resource-review.md](ar-resource-review.md). The final APK was installed on the Samsung; sustained physical AR performance has not passed.

## Palette arithmetic

These ratios use the actual light-theme tokens and standard sRGB relative luminance. They are checks on specified pairs, not a whole-app accessibility claim.

| Pair | Contrast ratio |
|---|---:|
| White on primary blue `#315BE2` | 5.64:1 |
| Main text `#202C40` on canvas `#F5F7FB` | 13.09:1 |
| Secondary text `#59667B` on canvas | 5.42:1 |
| Primary blue on subtle surface `#EDF1FB` | 4.99:1 |
| First-aid accent `#0F766E` on white | 5.47:1 |
| Fire accent `#C65B22` on white | 4.27:1 |
| Success text on success surface | 6.11:1 |
| Warning text on warning surface | 6.10:1 |
| Danger text on danger surface | 5.56:1 |

Fire orange is used as a non-text course icon accent; it is not suitable for normal-sized text on white at a 4.5:1 target. Selection and correctness must also use labels or shape/state, not colour alone.

## Findings caught during implementation

- Original scenario references pointed to absent anatomy IDs and used the rib-cage ID for a sternum explanation. Content references were reconciled with the package; the structural validator now prevents unresolved part IDs.
- The first raw anatomy package contained 1,917,642 triangles. Its skeletal remainder exceeded the runtime's 1,000,000-vertex per-part guard, causing a definite load failure. This was reported before installation. Blender optimisation is required; raising the load cap alone does not solve the mobile memory budget.
- APK inspection caught AAPT transparently decompressing assets named `.mesh.gz` and removing the suffix. All 68 bundled files and their generator/manifest paths now use `.meshpack`, retaining their exact gzip bytes and hashes; validation rejects the old suffix. Source validation alone would not have caught the transformed APK payload.
- The AR placement footprint was initially a fixed small rectangle. It was changed to follow the actual model bounds/scale/orientation. A 40% tabletop placement was subsequently observed on the Samsung; full-size placement, clearance and all rotation/scale combinations remain to be verified.
- Camera shader compilation and linking now check errors and clean up partially created resources. Recovery must be tested on an actual failing/unsupported renderer.
- New learning records now restore by identity, content version and deterministic replay. Original wrong decisions and hints remain visible; neither observed model selection nor completed text decisions establish hands-on practical competence.
- Default UI event timestamps use the greater of current wall time and the record's previous timestamp, so a device-clock correction cannot crash an ongoing attempt. Explicit event/replay timestamps still reject regression; dedicated tests were added for both engines.

## Anatomy provenance and visual scope

The packaged base meshes use **CC BY 4.0** (66 parts). Two separately sourced 4.3 lung derivatives retain **CC BY-SA 2.1 Japan**, with separate source mappings, hashes and `LICENSE-LUNGS.txt`. The validator rejects missing or conflicting per-part terms; the app's model information includes additional-source attribution. Derivative distribution must preserve the lungs' attribution, changes and ShareAlike terms. The source's current archive and viewer licence notices were checked separately; the archive notice was not assumed to relabel every viewer-served asset.

The four Blender previews were inspected after cleanup. The skeletal preview includes forearms, hands, lower legs and feet. The organs preview includes separate lung surfaces and grouped internal organs. These are adult, static, simplified source meshes; muscles contain selected groups, and the external skin/face does not provide photorealistic or exhaustive anatomical detail. Preview lighting is pale and is not evidence of the Android renderer's final appearance. Neither attractive rendering nor intact meshes proves anatomical correctness or suitability for children; qualified review is still pending.

Native emulator captures are in `artifacts/surakshaar-0.12.0/emulator/`. The isolated sternum and required-observation return were visually inspected. The Bay now presents multiple choices in a paged sheet at large text sizes, retaining a single visible action and more model space. Original-resolution 2.0× captures of the buddy and AED stages confirm visible headers, model viewports, primary controls and Pause/Listen/Cue toolbars. This selected-screen evidence does not establish every combination of screen size, text scale, language and accessibility service.

## Limited physical-device observations

The native implementation owner reported a Samsung **SM-S921B** running APK `f93c3f78622cfc64a7daeb5e30470857391df17c0d2d5fb41b0d4a2653031b6a`. The user placed the body at 40% tabletop size; the interface reported **Body locked**. Logs from 13:35:35–13:36:55 recorded `camera=true placed=true`, first with 11 organ parts, then 44 rendered parts. The 68-part manifest for that checkpoint has 44 muscle parts and 12 skeletal groups; an earlier description incorrectly called those 44 parts skeletal. The user subsequently confirmed that the body stayed attached during sideways movement and when looking away then back. This is **user-reported anchor stability for that placement and APK**, alongside camera/placement logs; it is not an instrumented drift measurement or verification of an entire AR learning sequence.

Most five-second intervals recorded 145–148 frames (about 29 fps), with one 135-frame interval. This **does not pass** the sustained 30-fps target, and this flagship device is not a mid-range performance baseline. One memory snapshot reported total PSS 370,292 KiB and RSS 438,503 KiB. There was no 20-minute session or repeated-entry memory study. A subsequent frame-cadence change needs its own measurement. Real-room images contain incidental people and are excluded from the public demo assets.

## Outstanding evidence

The following remain **unverified** until an executed result replaces this entry:

- Inspection of every promised anatomy part/interaction and supported layer on the actual target phone. The APK/source package, four Blender layer previews and selected native views were checked; individual clinical structures have not been clinically approved.
- Physical AR validation of the integrated 12-stage first-aid practice bay, including authored casualty cues and equipment touch interaction. The full 3D path and accessible AED alternative passed; this does not establish world tracking or physical CPR/pad placement competence.
- Current-build fire journey, narration/audio cancellation, all reset/permission-denial/profile-switch variants and whole-app TalkBack/smaller-screen coverage. The focused first-aid/observation/Back/recreation/large-text checks above cover only their specified paths.
- Broader physical-device AR coverage: full-size placement, measured drift, gesture/picking alignment through rotation/scale, tracking interruption, camera re-entry and the final APK. The earlier Samsung 40% placement and user-reported anchor stability above are positive but limited evidence.
- Sustained 30 fps on a named mid-range supported phone; Home p95 ≤3 seconds; complete 20-minute session; repeated-entry memory/thermal results.
- Qualified clinical/fire-instructor approval, Hindi/Santali review of new lessons, and child-account deployment/privacy review. Research and software checks are not these approvals.
- Live AI provider credentials and approved learning sources, production authentication/deployment configuration, release signing, real upgrade/recovery rehearsal and current-build demo.
- Optional asset download/pause/retry/removal, complete institution/notification workflows and any requested curriculum/model capability not demonstrated in this ledger.

No publication, deployment, asset purchase or external expert approval has been performed by this verification work.
