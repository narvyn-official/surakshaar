# SurakshaAR 0.14.0 verification — 10 October 2026

Development preview, Android 10+, version `0.14.0-dev` / code 29. Debug-signed APK `surakshaar-0.14.0-dev-debug.apk`.

SHA-256: `0a2877b11d9424232d36d6733ec56b0e208808053b490185929c23b346db19c8`

Bytes: 37,803,291. APK v2 signature verifies. The signing certificate matches the published 0.13.1 APK, allowing an in-place package update.

## Build and data checks

| Check | Result and scope |
| --- | --- |
| Android build | Debug app and instrumentation APK compiled successfully. |
| JVM tests | 268 passed across 42 classes; zero failures, errors or skips. Includes three new origin-bound session-envelope tests and four clothing tests. |
| Android lint | Zero errors; 54 warnings remain. |
| Exact APK content | Packaged curriculum, anatomy structures, licence notices, geometry and archive digests passed. 20 draft decision scenarios / 94 stages; 117 anatomical parts. |
| Detailed model | 987,482 source-derived triangles / 71,099,640 decoded bytes; bundled archive 17,690,952 bytes. Balanced 248,058-triangle fallback remains. |
| Authoring negative tests | 26 Python checks passed: nine detail-pack and seventeen content-validation checks. |
| Web unit tests | 100 passed; zero failures/skips. Includes real Drizzle/BetterAuth adapter and migration/rollback checks. |
| Web source | TypeScript and full ESLint passed. The standalone Node production build and isolated runtime smoke passed. The standard Cloudflare production build also passed. |
| Dependency audit | Production-only: zero reported vulnerabilities. Full development tree: eight high findings remain through the known development-tooling dependency chain. The audit gate is not bypassed. |

## Android UI evidence

API 36 emulator, software graphics, 1080 × 2400 at 420 dpi, rotated where stated. Synthetic fixtures are isolated and cleaned up; existing learner data and preferences are restored. Tests and screenshots establish the stated native flows, not camera AR or clinical effectiveness.

- **Final APK Home and navigation:** 3/3 tests passed in 200% landscape (12.615 s); 9/9 standard Home, earlier navigation and system Back tests passed (60.792 s). The completed-practice case asserts that its primary controls are fully visible on the first screen. Empty, resumed, completed and Hindi catalog screenshots were inspected. [Detailed navigation evidence](home-learning-navigation.md).
- **200% portrait Home:** 3/3 passed on immediately preceding `ea86575c…` candidate. The subsequent patch changes only the short/wide completed summary. This is component evidence, not a claimed final-APK portrait rerun.
- **Model coverage:** ten inspected views and the native geometry/coverage test passed on candidate `cd72ce4f…` (10.098 s). It checks front/back/side, low/high angles, tissue layers and supine/side-rolled poses. Source for these models is unchanged in the final APK. [Model scope and sources](model-quality-and-coverage.md).
- **Equipment inspection:** candidate `37b90dd6…` passed native focus/rotation/Back/reset in portrait (4.229 s) and 200% landscape (4.578 s); all five equipment renderers passed PixelCopy and rotation (8.957 s). Inspected label/gauge screenshots confirm readable authored labels and a retained model viewport. [Equipment scope](equipment-inspection.md).

Final APK repeated checks also passed:

- **Native public accounts:** 2/2 in 6.9 s. Actual HTTPS sign-in, account identity, password change, sign-out, disabled-recovery message and unchanged offline practice export; plus validation, reveal/hide, autofill hints, recreation without password persistence and Back. Fixture files were removed and exactly the two synthetic server accounts were deleted, followed by integrity/FK checks and a fresh backup.
- **Body and equipment:** 3/3 in 23.748 s. Detailed clothing coverage and protected geometry, fixed-viewport equipment focus/rotation/Back, and all five original GL equipment models. Final low-angle/back model and equipment images were inspected.
- **200% landscape equipment:** 1/1 in 4.498 s. Focus and controls remained usable with the model visible. Font scale and rotation preferences were restored.

Selected automated evidence is included in the release's `validation-evidence.zip`; no account credentials, database or physical phone images are published.

## Public account service

The dedicated HTTPS preview origin is `https://surakshaar.213-210-36-82.sslip.io`. Native default connections use it; explicit existing server selections remain respected. Old origin-unbound saved sessions require fresh sign-in. Offline practice remains available.

Public HTTP checks passed for real disposable signup/sign-in, private record ownership isolation, no automatic staff access, anonymous QR verification/revocation, foreign-origin denial and independent logout. A service restart retained sessions and private records. The daily online SQLite backup was validated and separately reopened to verify integrity, foreign keys and retained test data. Root independently confirmed HTTPS login returns 200 with successful certificate verification and account capabilities report `passwordRecovery:false`.

Email recovery and outgoing account mail are disabled by explicit user choice. No SMTP credentials were retrieved or messages sent. This is a preview hostname, with 14-day local backups on the same VPS. No off-site disaster recovery, alert delivery or capacity/load acceptance is claimed. [Deployment, recovery and rollback record](../../apps/admin-web/docs/vps-account-preview.md) · [independent storage review](node-storage-review.md).

## Release gates and exceptions

- Final-build physical AR placement, tracking interruptions and sustained memory/thermal performance remain unverified. Earlier Samsung confirmations do not validate this APK.
- Lessons/anatomy remain source-checked drafts requiring qualified clinical/fire review. No practical competence, permission to work, statutory certificate or measured learning benefit is established.
- Santali is incomplete; qualified Hindi domain review, audible voice quality and complete TalkBack journeys remain open.
- Production signing, permanent domain, off-site recovery, operational alerting and broader production identity acceptance are not complete. Email recovery is intentionally disabled, not an unreported delivery failure.
- No live AI provider or certificate issuer was configured by this VPS deployment. Existing offline learning and account features do not imply those services are active.
- Full development-dependency audit remains failing; 54 Android lint warnings remain. Automated checks are not an exhaustive security or accessibility audit.

**Production-ready decision: blocked.** This authorised development preview is suitable for evaluation of the documented changes. The limitations above are not deferred promises or claims of completed acceptance.
