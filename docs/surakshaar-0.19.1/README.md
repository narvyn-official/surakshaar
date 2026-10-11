# SurakshaAR 0.19.1-rc.1 — complete role workflows

Web and Android build 37 make institute onboarding reachable from every account state, complete content draft/edit/submission/revision workflows, enforce independent author/reviewer permissions, and give staff role-appropriate queues and task navigation. The light Spatial Studio design is retained.

## Verification before release

145 web unit tests, type checking, lint, production packaging and isolated worker startup passed. Isolated HTTP suites passed 589 combined checks covering institutional lifecycle, access boundaries, scopes, workflows and production MFA. Browser verification covered 13 role homes and 48 routes at 1440/390/320 CSS pixels, including invitation, institute application, content review and failed-save recovery. Android passed 317 JVM tests, lint/build and two API 36 emulator role/offline tests.

See the [role checklist](../role-access-checklist.md) for role contracts, evidence and limits. This release introduces no database migration and no dependency change. The web and signed Android update are published; see [deployment evidence](DEPLOYMENT.md).

## Release acceptance and limitations

- Role access, onboarding, navigation, safe draft recovery and independent review: passed in isolated HTTP/browser tests; staff MFA retained.
- Data migration: N/A (schema unchanged). Backup restore, artifact verification and all 63-table fingerprints passed before live cutover.
- Providers, prices, consent, camera collection, clinical instructions, visual assets and rendering architecture: unchanged. No new provider or external service activation.
- Accessibility/responsive behavior: tested keyboard skip, form labels/errors and narrow reflow; comprehensive assistive-technology and physical-device coverage remain open.
- Android operational forms can open a separate authenticated browser session; full native parity and physical AR acceptance remain open.
- This remains an evaluation prerelease. Qualified safety/clinical review, curriculum validation, accreditation, biometric validation, external recovery/payment/offsite services and field acceptance are not established by these tests.

## Recovery

Keep the pre-release backup, protected runtime environment and previous 0.19.0 application. A code rollback can retain the current database because this release changes no schema. Do not roll back to 0.18 or earlier, which cannot enforce active staff unit scopes. Never overwrite new records with an older backup as an incidental code rollback.
