# SurakshaAR 0.19.1 role-workflow deployment

11 October 2026. Application source `9955f033774649382edb71e0a666b4271bfa6e17`; web 0.19.1-rc.1 and Android code 37.

Live: https://surakshaar.213-210-36-82.sslip.io/dashboard

Release: https://github.com/narvyn-official/surakshaar/releases/tag/v0.19.1-rc.1

CI: https://github.com/narvyn-official/surakshaar/actions/runs/38115733837 — both jobs passed.

## Artifacts and changes

Linux x64 web archive SHA-256: `277867ead1ee4846df05ae0e1845169341e2c46e1ce5a43ee0ec9a097a1a7dfb`.

Signed APK SHA-256: `168d97052da510faf8aafb3967014b46a323a93a78da22b98105bcfb3a077c29`. Existing release signing identity retained; ZIP alignment and signature checks passed. Published asset sizes and GitHub digests match local artifacts.

Institute onboarding is reachable from new, personal and MFA-gated staff accounts. Draft save/edit/submit and new revisions are complete. Author/reviewer duties are separated on the backend. Dashboard queues, scoped counts and local navigation now correspond to each role's tasks. Native dashboards expose institute onboarding and server-provided queue titles. See the role checklist for the detailed contract and pre-release evidence.

## Data and runtime verification

The Linux package was built by CI from the pushed commit, inspected and checksum-verified on the VPS. An isolated service ran against a restored copy of the current database. Migration directories matched byte-for-byte; zero migrations applied. Original column values in all 63 tables were preserved, with integrity and foreign-key checks passing.

The cutover retained `/etc/surakshaar/runtime.env` byte-for-byte, along with all accounts, password hashes, existing MFA and encryption/provider settings. No live reseeding, role changes, provider activation or credential reset was performed. The stop-to-ready interval was four seconds. Web and worker report active with zero restarts after activation. All five operations checks passed; the scheduled backup and retention/monitor timers remain active. A fresh backup was independently restored and checked with all 31 migration records intact.

Live HTTPS checks passed 13 public/protected/cache/readiness assertions. Thirteen fictional demo accounts passed their role home, scope, MFA-enrolment gate and institute-access checks. The platform-owner account now has an authenticator enrolled: password acceptance correctly returned its MFA challenge. The owner dashboard was not opened live without that factor. Its full role journey and MFA behavior were verified with synthetic accounts before release.

Live mobile browser checks also passed for all 13 authenticated demonstration accounts, including opening institute access from navigation, correct MFA setup states, no overflow and no browser/runtime or failed-resource errors. Learner and auditor dashboard screenshots were visually inspected.

## Recovery

Current runtime: `/opt/surakshaar/releases/20261011-role-workflows-0.19.1/web`, linked by `/opt/surakshaar/current`.

Previous compatible runtime: `/opt/surakshaar/releases/20261011-completion-0.19.0/web`.

Root-only recovery files: `/var/backups/surakshaar/20261011-role-workflows-0.19.1`. Preserve the protected environment, pre/post database backups, fingerprints and scripts. These must never be published. No schema change is present, so code rollback to 0.19.0 can retain the current database. Do not roll back to 0.18 or earlier because those versions cannot enforce active unit scope. Never replay an old database over new writes as an incidental application rollback. Restore exact database/WAL/SHM ownership to `surakshaar` after root SQLite access before restarting services.

## Limits

This is an evaluation prerelease. Physical AR and broader device coverage, qualified clinical/safety/content review, accreditation, biometric validation and full native/web parity remain open. New external recovery/login, payment and offsite-backup services remain disabled by owner decision. Local operational reporting is not offsite alert delivery. This release does not claim an exhaustive security/accessibility audit or measured safety outcomes.
