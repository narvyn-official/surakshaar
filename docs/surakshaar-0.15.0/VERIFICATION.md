# Release verification · 11 October 2026

**Candidate:** SurakshaAR 0.15.0-rc.1 / Android code 32. Selected design: Spatial Studio light. No public deployment was performed.

| Check | Result and evidence |
|---|---|
| Web unit suite | 122 tests; includes all 20 scenario completion paths, required observation gating, malformed progress recovery, authenticated encryption/binding and configured-origin validation. Final log supplied with the artifacts. |
| Web types, lint and build | TypeScript, ESLint and Next.js standalone production build passed. The packaged source also rebuilt in a separate temporary directory using the installed lockfile dependencies. |
| Android unit suite | 306 tests, no failures, errors or skips. |
| Android release lint | No errors/fatals; 66 warnings remain, including 19 draw-allocation, 17 text-localisation, 10 custom touch accessibility and 8 programmatic-view constructor findings. This is not a warning-free Android audit. |
| Android instrumentation | 18 selected navigation, home, equipment, anatomy-control and resume tests exercised. Two screenshot-readiness assertions initially assumed the old theme colour; the affected seven-test resume class passed after using the actual theme colour. Other selected tests passed. |
| Signed APK | Persistent RSA-3072 release key; APK v2 signature and 16-KiB alignment verified. Signed candidate installed and launched on the Android 16 emulator; equipment explorer opened and rendered. |
| Web visual/interaction | Inspected actual desktop, 390px mobile, anatomy, lessons, practice and progress renders. Width checks at 320, 360, 390, 768 and 1440px found no page overflow. Guest scenario completed and survived reload, without page exceptions. |
| Account integration | Fresh standalone SQLite service: registration, sign-in, password change, logout invalidation, tenant ownership, cross-origin rejection and truthful unavailable-recovery response. |
| Mandatory staff MFA | Separate production-policy check: protected access rejected before enrolment, authenticator enabled and TOTP verified, protected access accepted afterwards. |
| Identity API | Adult consent, school exclusion, membership enforcement, entry capture, one-use challenge, encrypted storage, private cross-tenant photo access rejection, server replay of practice, formative submission, independent appeal, withdrawal, interruption and expiry. |
| Identity browser | Simulated camera produced actual JPEG uploads; visible capture, completed institutional practice, background interruption and examiner images rendered. No real employee images used. |
| Browser account recovery | Completed personal practice, explicit backup, cleared local records, selected an account-owned backup and restored completion. |
| Backup/restore | Verified online snapshot restored to an isolated instance; integrity/foreign keys, saved submission, service health and authenticated learner membership checked. |
| Dependency audit | Production dependency audit: zero reported vulnerabilities at check time. Full development tree reports eight high paths rooted in the unpatched braces advisory; see below. |

The old safety-worker test selected the latest job by a millisecond timestamp, which could tie; it now checks the exact job returned by enqueue. No product authorisation was weakened to make the assertion pass.

## Security and operational limits

The development dependency advisory is [GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm), checked against the published advisory on 11 October 2026. It reports no patched braces release. The affected paths are development glob/lint/Vinext tooling; do not expose those tools to untrusted patterns or run a development server publicly. The delivered standalone runtime does not include braces. Do not downgrade Next.js to an older major solely to satisfy npm’s suggested development-tree fix.

Staff MFA is enabled by default in the release. Isolated general integration fixtures explicitly disable that requirement to exercise non-MFA flows; the separate mandatory-MFA check must be read independently. Camera evidence and account fixtures stay in the ignored QA directory, never in release archives. Evidence images are AES-GCM encrypted with per-image random nonces and bound to the session/image IDs. Metadata is tenant-scoped. Server receipt time and device-reported time are labelled separately.

This release does not claim a complete external security audit, legal compliance assessment, clinical validation, liveness assurance or cheating detection. A manual reviewer may request another supervised assessment. The original qualified-content review and practical-competence boundaries remain in force.

## Untested or externally gated

- This exact build has not had sustained physical-phone ARCore, thermal, battery, voice-quality or varied front-camera tests.
- Full manual TalkBack/VoiceOver coverage, every large-font/keyboard combination, low-end GPU/network coverage and cross-browser camera compatibility remain wider acceptance work.
- Linux Docker recipe supplied; local Docker daemon unavailable, so no container-build pass is claimed.
- Final public HTTPS ingress, delivery-domain authentication and real password-reset mail remain unconfigured here.
- Recovery objectives, encrypted off-host backups, on-call support and the privacy/retention notice require the deployment operator’s ownership.
- The scenario catalogue and translations remain draft until qualified review. No government endorsement or number of lives saved is asserted.

**Decision:** installable Android and locally verified web deployment candidate; not an assertion that all public-service, clinical or institutional go-live requirements have been met. Deployment action authorised here: preparation only.
