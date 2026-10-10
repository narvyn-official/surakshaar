# SurakshaAR 0.13.0 verification — 10 October 2026

This is a development preview, not a production release. Evidence applies to the stated build and environment; emulator interaction does not establish physical AR stability, clinical accuracy, treatment competence or audible voice quality.

## Build and automated checks

The packaged APK, exact SHA-256, byte size and aggregate results are recorded in [`build-checks.json`](build-checks.json). The filename is `surakshaar-0.13.0-dev-debug.apk`; it is debug-signed, version code 27, Android 10+. See the [new repository's downloads](https://github.com/narvyn-official/surakshaar/releases).

| Check | Evidence and scope |
| --- | --- |
| Android JVM suite | 261 tests passed, no failures/errors/skips, 40 test classes. Archived XML is in `validation/jvm/`. |
| Android build | Debug APK and instrumentation APK compiled. `validation/final-build.log` records the command and result. |
| Android lint | No errors; 54 warnings remain. See `validation/lint-results-debug.xml`; this is not a warning-free release. |
| Content tooling | 26 Python unit tests passed with `PYTHONPATH=scripts`. Curriculum structural validator passed 332 checks for 20 draft decision scenarios and 94 stages. These counts exclude the separate native casualty/fire journeys. |
| Packaged anatomy/content | Exact APK assets, bounded decoding, geometry counts, digests and licence notices validated. Balanced anatomy has 117 parts and 248,058 triangles; the optional detail archive has 117 parts and 471,068 triangles. See `validation/apk-validation.json`. |
| Backend | 94 unit/SQLite tests passed. TypeScript, ESLint and production Vinext build passed. Real local HTTP checks passed for institution isolation, private practice and public QR records. See the [backend checkpoint](../../apps/admin-web/docs/release-priorities-2026-10-10.md). |
| Dependency findings | Production-only npm audit reported zero findings. Full audit has eight high findings through development-tooling `braces` with no patched upstream release recorded at inspection. The release gate remains open. |

Local evidence directory: `artifacts/surakshaar-0.13.0/validation/` (ignored from source control). The release includes a selected automated-evidence archive; private phone imagery and fixture credentials are excluded. Earlier failed runs are retained locally for diagnosis and are not counted as passing checks.

## Rendered native interactions

The emulator is an API 36 Android virtual device using software graphics, 1080 × 2400 at 420 dpi. Tests create synthetic learner records and preserve the existing profile's assessment/certificate evidence.

- Native Audio controls, language choice, speed/mute persistence, recall behaviour and system Back passed a five-test batch. A separate 200% landscape test passed after moving Voice settings into the native dialog footer. These checks used APK SHA-256 `95c0956d8585339ee813004aa27d05b8a8b343ea299d930e5b9e5a942199b186`; they are component evidence from an earlier candidate. See [narration evidence](narration-controls.md).
- Casualty and View controls passed separate 200% portrait and landscape checks after the short-landscape layout changed to model-left/task-right and large-text controls gained semantic icons. Screenshots were visually inspected. This is focused layout/Back evidence, not a full TalkBack audit.
- Fire inspection, damaged equipment and changed-route scenes rendered successfully without fabricating learner actions. Saved scenario identity, ownership and the existing demonstration/transfer regression checks passed. Exact logs and build records are in `validation/casualty/`.
- APK `27d4500984a1f2a730e811e6a562a03c6d8b5abf2390498a0b4c19df027a6a0e` passed all three casualty cases and the Fire rendering case: **4 tests**, 49.482 seconds. Cue-centre picking, short/moved/off-target hold rejection, completed hold, wrong-answer retention, pause/recreation, explicit scenario isolation and completion were exercised. The first completed hold sample was fraction 1 at age 94 ms. See [casualty UI evidence](casualty-ui-validation.md).
- The same APK passed the complete signed-in native institution and QR workflow: **1 test**, 6.511 seconds. Tests switched between two institutions, checked assignment isolation, cancelled consent without creating a link, generated/decoded the QR bitmap, verified anonymous public metadata, revoked access and retained the private source record. All synthetic records and temporary credentials were removed, and original app state restored. This tests bitmap decoding, not optical scanning with another phone.
- The final candidate `706425d93e82cb11d9d32f29dbdbb0aa4bda33aa76483344423ff84d9d301ec1` changes only the Fire inspection presentation and a revoked-history date label after those tests. The focused rendered Fire rerun passed **1 test**, 9.701 seconds: inspection choice rectangles are separate, on-screen and at least 48 dp; real touch events map to the correct actions; inspecting does not modify saved evidence. The magnified gauge/split hose and separate controls were visually inspected. The published APK also passed the complete native institution/QR fixture workflow again: **1 test**, 6.069 seconds. This rerun asserted the actual revoked timestamp and visually checked its label. Synthetic records were removed and baseline state restored; see [the final native record-sharing check](../../apps/admin-web/docs/formative-record-sharing.md#native-verification--10-october-2026).

The hold gesture is an interface action; its dwell time does not prescribe treatment duration or measure physical pressure. Wrong decisions remain in saved attempts. Choosing an accessible 3D alternative cannot create AR evidence. A static authored injury cue is not camera diagnosis or a physiological animation.

## Physical phone evidence

The Samsung SM-S921B received the preliminary candidate with SHA-256 `5917c80435eae21081d856f756a28ec1552cb38b76d0e9c8df9761fdbebe29c7`. The 3D casualty and live camera scanning screen were inspected. After being asked to aim at a well-lit textured surface, the user reported: **“Placed—the casualty is visible.”**

USB debugging disconnected before placement telemetry, anchor persistence, interruption recovery or a sustained run could be captured. This is user-observed placement on that preliminary candidate, not logged acceptance of the final APK. Earlier phone reports for prior versions are not substituted for this missing evidence. A short thermal snapshot is not an endurance benchmark.

## Remaining release gates

| Area | Status |
| --- | --- |
| Exact clinical/fire lessons and anatomy | Source-checked drafts. Qualified review and practical instructional validation remain required; no professional approval was invented. |
| Languages | Authored English/Hindi flows have software checks. New Hindi needs fluent domain review. Santali remains explicitly pending, not a working fully localised release. |
| Physical AR and performance | Final-build camera placement, tracking loss/recovery, repeated sessions and sustained memory/thermal/input checks on representative supported phones remain unverified. |
| Narration and assistive technology | Actual sound, pronunciation, phone-call/Bluetooth/headset interruptions, installed offline voices and complete TalkBack journeys remain unverified. |
| Backend operations | HTTPS account origin, recovery delivery, backup restoration/retention, role governance and support operations are not production-accepted. Local migrations and HTTP checks do not prove deployment readiness. |
| Contextual AI | Permission-scoped control assistance and readiness diagnostics are implemented. A live provider is not configured or validated; no paid service was activated. |
| Distribution | Development signing only. A new public repository and preview APK were explicitly requested; this does not approve production signing or a backend deployment. |
| Supply chain | Full dependency release gate remains open as described above. |

Applied development-rulebook acceptance checks cover one owner for scene transforms/picking, cancelled/interrupted input, native Back, 48 dp controls, enlarged text, recoverable rendering, explicit language fallback, record ownership, institution isolation, revocable consent and evidence tied to builds. Production release remains blocked by the explicit gates above; passing structural and UI tests does not remove them.
