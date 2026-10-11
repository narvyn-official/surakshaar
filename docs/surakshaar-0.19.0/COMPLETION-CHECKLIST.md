# SurakshaAR completion checklist

11 October 2026. Owner decision: keep new external login/recovery, payment and offsite-backup services disabled for now. Existing configured TinyFish service is unchanged.

Baseline: the 26-section architecture dated 10 October, the 0.17 implementation checklist, and the deployed 0.18 role release. This replaces neither historical evidence nor qualified safety review. “Implemented” means code exists; release verification is recorded separately. There is no defensible “perfect” or field-certified status.

## Work being completed in this release

| ID | Work | Status | Acceptance evidence required |
|---|---|---|---|
| S01 | Cohort/department access for instructor, examiner, issuer, combined assessor and viewer | Implemented and tested; deployment pending | Descendants included; siblings, other tenants, archived people, moved learners and revoked staff excluded from lists, detail, photos, writes, exports and counts |
| S02 | Admin invitation/update controls for staff scope | Implemented and tested; deployment pending | Explicit entire-institution versus selected units; revoke/restore preserves scope; invalid units rejected |
| S03 | Safe navigation for scoped staff on web and Android | Implemented and tested; deployment pending | Only scoped tools shown; legacy whole-institution tools deny direct API access |
| L01 | Web assigned practice, save/resume, explicit submission | Implemented and tested; deployment pending | Correct institute/profile/version/mode; private save distinct from sharing; failed save recoverable |
| L02 | Web learner feedback, correction, withdrawal and pagination | Implemented and tested; deployment pending | Original decisions immutable; reviewed history visible; withdrawal immediately removes future staff evidence access |
| O01 | Local operations monitor and owner status panel | Implemented; VPS activation pending | Readiness, disk, verified backup freshness, worker and retention timer; distinguish stale/unavailable |
| V01 | Unit, integration, browser, native and release validation | Local checks passed; deployment pending | Test logs, migration preservation, signed APK, deployed smoke checks |

## Existing capabilities retained

| Area | Status | Evidence / limits |
|---|---|---|
| Guest/personal learning without monitoring | Implemented | Web studio, native local profiles and account-separated backups |
| Password login, MFA, sessions and role routing | Implemented | 0.18 evidence; staff MFA remains required |
| 14 demo accounts / 13 role workspaces | Implemented | Owner, administrator, instructor, examiner, issuer, assessor, auditor, author, reviewer, viewer, learner, personal learner, guardian/facilitator; private handover retained |
| Independent institute onboarding and governance | Implemented | Approval, suspension, role invitations, manual plans, quotas and restrictions |
| Branches/departments/cohorts, bulk learner import | Implemented | Bounded CSV preview, duplicate protection, archival, explicit account linking |
| Policy versions, assignments, deadlines, attempts | Implemented | Assignment-pinned policy/content; monitored attempts counted; generic practice is not a verified exam |
| Coursework review and supervised observations | Implemented | Immutable decisions, review history and practical rubrics |
| Adult reference identity and monitored web practice | Implemented | Approved reference, disclosed foreground capture, final frame, encryption, expiry, examiner review and appeal |
| Institution-issued signed records | Implemented | Prerequisite checks, independent issuer option, QR verification, expiry, replace/revoke, key history |
| Local procedure author/reviewer separation | Implemented | Independent approval, withdrawal, approved library on web/native |
| TinyFish public-source collection | Implemented | Server key, quotas, review, immutable reference versions and lesson dependencies; does not approve safety content |
| Approved-source learning coach | Implemented | Bounded explanations, examination restrictions; no autonomous grading or certification |
| Guardian/facilitator progress sharing | Implemented | Owner-controlled invitation, limited summaries, expiry/revocation; not proof of legal guardianship |
| Offline Android learning and sync | Implemented | Local packs, visible queue, account isolation and recovery; not an offline verified exam |
| Native AR / 3D alternatives | Implemented, device validation partial | Shared assets and step runtime; detail and device acceptance are not established by compilation |
| Responsive light Spatial Studio | Implemented | Existing selected theme retained; prior desktop/390/320 checks |
| TLS, CI, migrations, backup and rollback | Implemented | Prior release evidence and single-node VPS operations reports |

## Remaining engineering and external dependencies

| ID | Remaining work | Why it is not complete | Next concrete completion criterion |
|---|---|---|---|
| E01 | Native monitored rear/front camera AR checkpoints | Native camera lifecycle exists; assessed switching/recovery is not connected end-to-end | Persist checkpoint state, release/reacquire cameras, bind private uploads and interruption outcomes; test real supported phones |
| E02 | Google/email OTP/SSO and delivered account recovery | Password/MFA works; provider choice, credentials and delivery are not validated | Configure a selected provider, preserve staff MFA, test recovery and account linking. No mail is sent merely because credentials exist |
| E03 | Online billing and automated renewal | Contracted manual plans work; no payment provider chosen | Approved pricing/tax/refund policy and provider, verified webhooks, duplicate/out-of-order handling |
| E04 | Managed PostgreSQL/RLS/object storage/queue migration | Current supported target is bounded SQLite with a separate worker | Migration rehearsal, tenancy parity, verified private objects, deployment/rollback and representative load; do not replace working infrastructure merely to match a proposed stack |
| E05 | Offsite restore and operational alert delivery | Local backup/restore and local monitoring work; independent destination and external responder are unconfigured | Encrypted offsite copy, isolated restore drill, chosen alert channel and incident ownership |
| E06 | Full native/web parity and multi-persona switching | Some Android operations open an authenticated browser; role precedence selects the operational role | Explicit supported route matrix; native camera/exam path and reversible account-context chooser |
| E07 | Institution branding/templates and closure export | Name/site, plans, archive, policy and signed record templates exist; full branded templates/closure wizard are incomplete | Validated template fields, export, access closure and retained verification/key history |
| E08 | Full appearance/language/accessibility matrix | Light selected; some Hindi support; complete theme parity, translation and assistive-device matrix not validated | Reviewed translations, layout/direction checks, keyboard/screen-reader/native accessibility and physical-device matrix |
| E09 | Shared content authoring/review governance | Local institute procedure workflow exists; reviewed global eight-domain catalogue does not | Versioned global publication and withdrawal with named qualified reviewers and signed-off translations |
| G01 | Qualified safety/clinical content review | Human expertise and domain authority cannot be created by code | Named competent reviewers, source records, dates, rubrics and approved releases |
| G02 | Optional face comparison/liveness | No selected validated provider | Consented provider setup, real-device spoofing/error evaluation, accommodations, human review and appeal |
| G03 | Detailed model acceptance | Models exist, but “every small detail” is not a measured specification | Per-model part inventory including pin/tag/seal/thread/label where relevant, provenance and qualified visual/functional sign-off |
| G04 | Child/worker/biometric deployment governance | Technical access and retention are not legal approval | Institution agreements, age-appropriate flows, applicable legal/privacy review |
| G05 | AR effectiveness / buyer evidence | No real pilot or willingness-to-pay measurement | Baseline/post/delayed retention study versus comparable learning; buyer interviews and cost model |
| G06 | Accreditation and recognition | Records explicitly state their formative scope | Actual issuer recognition/partnerships before any accreditation claim |

## Release rules

- Do not silently enable unconfigured providers or claim real clinical/biometric validation.
- Preserve live records, existing credentials, MFA and unrelated workspace changes.
- Assess implemented features through complete journeys and negative access checks; do not tick boxes from screenshots alone.
- Keep gaps here until tested completion evidence exists. No lives-saved or accident-reduction claim is inferred from government statistics.

Authorization implementation follows least privilege, object-level checks and deny-by-default guidance from the [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html), checked 11 October 2026. This is not an ASVS certification.
