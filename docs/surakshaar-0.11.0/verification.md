# SurakshaAr 0.11.0-dev verification and remaining gates

4 October 2026. **Development pilot; not release ready.** Working source and a debug APK are provided. No production deployment, signing-key rotation, live AI provisioning, external email or physical-device demonstration was performed.

Baseline: repository and remote HEAD `017562289a879495e800bda01828bac7c7deec50`, v0.10.0/code 23. Current Android: `com.narvyn.suraksha`, **0.11.0-dev/code 24**, min API 29, target API 36. Final APK SHA-256: `8ae6e5b4def056a083823b63c9c764e29f8181a180d1386f40696b384d3b45c3`. Evidence from a different APK is identified below and is not promoted to current-build proof.

## Delivered behavior

- Home / Training / Progress, one recommended next action, existing course/room/equipment routes retained, and staff entry dependent on current server authorisation.
- Demonstration stage, narration controls and audio focus, native Back/recovery, read-only decision replay, older attempt history and worker-scoped targeted formative practice.
- Thirteen draft objectives, fifteen normal/abnormal/uncertain cue scenarios and thirty critical formative decisions across all five domains. Mistakes stop the attempt; a retry creates a separate record. These new checks are screen simulations, not new spatial AR environments.
- Conservative objective passport: knowledge, simulated decisions and practical competence remain separate. New practice cannot modify original assessments, certificate evidence or issuer trust.
- Eighty-one bundled questions, offline topic/search/context/source details, explicit safety/language draft labels, and authenticated typed tutor requests.
- Backend reference selection with account/centre checks, minute/hour quotas, timeout and response validation. Unapproved safety sources remain blocked. The optional provider may classify a product-help reference; it cannot write safety instructions or execute tools.
- Bounded ARCore update/camera-cadence timing windows and reproducible lab test/report scripts. Camera validity thresholds are unchanged.

See [architecture and curriculum map](architecture-and-curriculum.md), [question matrix](question-coverage.csv), and [build/recovery instructions](recovery-and-release.md).

## Current verification

| Check | Status | Evidence and scope |
| --- | --- | --- |
| Android build, JVM replay/model tests and lint | Pass | 165 JVM tests, zero failures/errors/skips; `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`; [build log](evidence/android-0.11-build.log) |
| Web unit, types, lint and production bundle | Pass | 38 unit tests, zero failures; TypeScript, ESLint and build succeeded; [full log](evidence/web-0.11-check.log) |
| Content structure / Android-web question parity | Pass | 81 question cases, 13 objective definitions, 15 scenarios / 30 critical decisions; checks establish structure, not safety approval |
| Exact-question tutor/source policy | Pass | All 81 supplied cases produce a bounded answer, clarification or refusal; unapproved safety sources cannot become supported answers. This does not establish semantic accuracy for every paraphrase or language. [Matrix](question-coverage.json) |
| Tutor local API integration | Pass | 12 synthetic checks: authentication, learner/centre scope, both cross-account denials, strict schema/length, no-cache, draft/injection/conflict handling, concurrent 10/minute and 60/hour limits; [results](evidence/tutor-0.11-integration.json) |
| Final native emulator matrix | Pass | 116 test executions: ordinary 95, placement 2, workspace 2, 200% text 15, thermal fixture 2; zero failing groups. Some tests repeat at both font sizes. [Summary](evidence/qa-0.11-current-summary.log), [matching build/device](evidence/qa-0.11-current/build-device.json), [ordinary log](evidence/qa-0.11-current/ordinary.log). No physical AR claim. |
| Debug signature continuity | Pass | Final APK verifies with Android Debug certificate SHA-256 `cf761b5f2f4b8e77b522e6e076786cce521e8f2220608d6b5da5a2ebf2be086b`, matching retained v0.10 debug signer; [signature log](evidence/signing-0.11-debug.log). No production signer claim. |
| In-place upgrade fixture | Pass | Two explicit checks: install retained code 23 without clearing storage, seed a synthetic learner and stopped original procedure, update to code 24, compare exact record/profile and restore previous learner. [Seed](evidence/upgrade-0.11/seed.log), [verify](evidence/upgrade-0.11/verify.log). This covers one record type and debug signing only. |
| Current local review / credential contract | Pass | Enrolment possession/single use, learner/staff isolation, repeated/stale sync, forged replay, course readiness, correction, independent rubric approval, signed wallet, printable simulation label, public name omission and revocation passed on the current backend. [Local journey log](evidence/journey-0.11-integration.log). No deployed service claim. |
| Local authentication/recovery and journal regression | Pass | Current synthetic suites passed: [authentication](evidence/auth-0.11-integration.log), [MFA](evidence/mfa-0.11-integration.log), [concurrency/password recovery/session invalidation](evidence/reliability-0.11-integration.log), [append-only room journals](evidence/room-journals-0.11-integration.log). Recovery delivery uses the loopback inbox only. |
| Startup/performance lab sampling | Partial | Thirty cold-process first-frame samples on the named host-rendered emulator: p50 759 ms, p95 826 ms, p99 985 ms. [Samples and build hash](evidence/startup-0.11-host.json). Android `am start -W TotalTime` is not usable-Home time or physical-device proof. |
| Physical fire/gas AR and demonstration | Not tested | Only `emulator-5554` is attached. No physical phone or AR services are available. No real camera/surface/anchor/tracking completion is claimed. |

The emulator uses `Suraksha_API36`, `sdk_gphone64_arm64`, Android 16/API 36, 3072 MiB RAM, four emulated cores and `-gpu host` (Apple M5 Metal/GLES renderer). Native tests use explicit camera denial, font 1.0/2.0, rotation/recreation and severe-thermal fixtures; settings are restored. Simulated thermal state and saved camera feedback are not real sustained camera or heat tests. Account checks use a loopback local server and synthetic identities.

Current native captures inspected: [Home](evidence/qa-0.11-current/renders/ordinary/workspace-011-home.png), [Training](evidence/qa-0.11-current/renders/ordinary/workspace-011-courses.png), [decision feedback](evidence/qa-0.11-current/renders/ordinary/workspace-011-transfer-feedback.png), [source/draft detail](evidence/qa-0.11-current/renders/ordinary/workspace-011-question-reference.png); 200% text [Home](evidence/qa-0.11-current/renders/large-text/workspace-011-home.png), [paged feedback](evidence/qa-0.11-current/renders/large-text/workspace-011-transfer-feedback.png), [decision replay](evidence/qa-0.11-current/renders/large-text/workspace-011-replay.png), [source dialog](evidence/qa-0.11-current/renders/large-text/workspace-011-question-reference.png). Main actions/labels are readable; long feedback moves to an explicit next page. These are emulator screenshots, not physical-camera demonstrations or a full accessibility audit.

The 81-case matrix records **13 scoped Pass and 68 Partial** for product/field behavior. All 81 pass the narrower exact-question policy/source-boundary evaluation. The two columns deliberately answer different questions: a safe reference response does not prove physical AR, competent-reviewed instruction, translated speech or all operational behavior. Each Partial names its evidence scope and gap.

## Failures and evidence boundaries

An earlier v0.11 candidate (`c2e60b6bd7408eecc070d4d604c272d232178470dcf4943604be9190b174e82f`) **failed** two fire gesture checks with the SwiftShader software renderer: measured alignment/sweep did not reach the required phase. Its native UI diagnostic reported p50 150 ms, p95 500 ms, p99 800 ms and 97.30% janky frames. These gaps exceeded the gesture validity window; the app rejected progress conservatively. [Failure log](evidence/qa-0.11-recovery-check/ordinary.log), [UI diagnostic](evidence/emulator-swiftshader-0.11-gfxinfo.txt). This is an unresolved software-renderer compatibility/performance limit, not physical-phone evidence. No threshold was loosened to obtain a pass. The final APK has not been claimed verified on SwiftShader.

An earlier System UI ANR interrupted another emulator run; that run is invalid as successful application evidence. A preceding hardware-rendered build passed its matrix, but those results do not substitute for the final hash above. Only normal and 200% text captures freshly generated by `LearningWorkspaceTest` are relevant to the new workspace visual review; copied captures from other groups must not be treated as newly rendered evidence.

`ArTimingWindow` measures ARCore update CPU time and distinct camera timestamp cadence. Neither is full GPU frame time, sustained AR fps, tracking quality under workplace lighting, a memory-cycle benchmark or battery measurement. Instrumented recreation/reopen checks do not prove every process-kill/OEM-background scenario.

The [current device snapshot](evidence/device-0.11-current.json) records version/code and absent AR services. Any buffered driver logs in a snapshot must be correlated with session/build; they are not completion evidence. The startup samples were taken after native/upgrade checks finished, without another emulator test or build running.

## Acceptance status and blockers

| Requested capability / release gate | Status | Remaining work |
| --- | --- | --- |
| Simplified learner journey and account-linked authority | Partial | Implemented native learner journey; new demo/transfer stages are recommendations, not rewritten server certification prerequisites. Complete task-oriented staff redesign is pending. |
| Five-domain depth and observable objectives | Partial | Original 55 steps/40 knowledge decisions preserved; new draft formative variants work in the model. Equipment/site-specific review, expanded reviewed spatial cues and supervised practical evidence remain absent. |
| Decision replay and skill passport | Partial | Read-only records and conservative labels implemented. Coarse objective mappings, retention/remediation effectiveness and reassessment policy need instructional review. |
| Hidden-fault inspection and transfer | Partial | Authored damaged/missing/incomplete cues and critical decisions implemented. No automatic real-machine recognition, new realistic licensed models or validated unfamiliar AR arrangements. |
| Explain-back and approved safety AI retrieval | Partial | Bounded classifier/guide foundation implemented. Explain-back, generative lesson coaching, approved site corpus, retrieval evaluations and live provider are missing. |
| Voice, Hindi, Santali and accessibility | Partial | Local installed EN/HI voice controls; new English-only drafts explicitly state unavailable Hindi detail narration. Hindi wording is unreviewed; Santali text/voice unavailable. TalkBack user trials, real audio/call/Bluetooth behavior and full small-phone/device-tier review remain untested. |
| Core AR validity and recovery | Partial | Existing freshness/lease/placement gates and emulator recovery tests retained. Physical surface/spacing/reposition/call/background recovery and fire/gas completion require named current devices. |
| Offline/storage/update reliability | Partial | Strict records, failed-write/queue fixtures and separate databases implemented. Physical storage exhaustion, broad process death, all-history upgrades and representative OEM behavior remain untested. |
| Authentication, tenant scope, independent review and public verifier | Partial | Existing server authority retained; tested local cases are separately recorded. Deployed credentials, institutional identity basis and operational ownership require current end-to-end evidence. |
| Performance targets | Not tested | Usable Home p95 ≤3 s, ordinary feedback ≤100 ms, sustained 30 fps AR, 20-minute training and 20 AR cycles need representative physical measurements. Lab first-frame timing must not be substituted. |
| Competent safety / language approval | Not tested | All new safety sources remain drafts, reviewer null. Obtain approved Jharkhand/site/equipment procedures and independent competent review. US general guidance is not Indian legal approval. |
| Production signing/configuration and deployed restore | Not tested | No authorised production signer, live provider/privacy review, hosted auth/mail revalidation, issuer-custody exercise, backup/restore drill or retention/deletion owner available. |

No learning-gain, retention-improvement, accident-reduction, accreditation, verified workplace competence or authorisation-to-work claim is made. Unrelated hospital/rulebook/solution-blueprint work is preserved. There are no new dependencies, external design assets or provider secrets in Android source.

## Publication

Published as a debug development pilot on 5 October 2026 under tag `v0.11.0-pilot`. The APK is the same byte-for-byte artifact verified above; publication does not close the remaining release gates. Selected evidence is archived in [evidence/manifest.json](evidence/manifest.json). Local filesystem prefixes were redacted and trailing whitespace normalized in public text logs; reported checks and results were preserved.
