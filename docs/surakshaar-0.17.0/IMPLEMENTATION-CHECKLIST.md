# SurakshaAR architecture implementation checklist

Audit started 11 October 2026 against the attached architecture and the deployed 0.16.1 code. This is a live delivery record, not a claim that every proposed feature is production validated.

Legend: **Implemented** = working code exists; **Partial** = important parts remain; **Pending** = no complete workflow; **External gate** = needs credentials, qualified people, devices or field evidence. Test and deployment evidence is recorded separately.

| Requirement | Status in 0.17.0 | Evidence / missing work |
|---|---|---|
| Guest learning without proctoring | Implemented | Spatial Studio; Android local profiles |
| Signed-in progress and multi-device sync | Implemented | learning-profiles / learning-practice APIs, native sync |
| Google / email OTP | Pending | Password accounts exist; provider configuration and delivery needed |
| Account MFA / sessions / password change | Implemented | Better Auth; staff MFA enforced |
| Email recovery delivery | External gate | Adapter exists; production mail deliberately disabled |
| Multi-institute learner membership | Implemented | learner_links with tenant-scoped account access |
| Institute approval and suspension | Implemented | governance API, explicit platform operator allowlist |
| Branches, departments, cohorts | Implemented | Tenant-scoped units, parent hierarchy, directory placement and archival; /institution. |
| Bulk enrolment with review and duplicate protection | Implemented | CSV preview, 200-row bound, case-insensitive employee IDs, idempotent batches; account linking remains explicit. |
| Distinct instructor, examiner, issuer, author, reviewer roles | Implemented | Eight institution roles; backend role matrix, independent reviewers, MFA; content roles cannot read learner records. |
| Institution policy versions / programme rules | Implemented | Immutable policy versions, pinned assignments, cohort snapshots, attempts, practical and separate-issuer requirements. |
| Plans, quotas, renewal / restricted operation | Implemented, manual plans | Platform operator manages allowance, service end date and restrictions; no checkout, payment processing or automated billing. |
| Course assignment / deadlines / cancellation | Implemented | Version-pinned launch courses and industrial assignments |
| Learn, Explore, Practise, Demonstrate, Refresh | Partial | Interactive lessons, practice, assessment and recall exist; not all domains reviewed |
| Detailed shared 3D / native AR and screen alternatives | Implemented | Shared spatial assets and native runtime; physical-phone validation still required |
| Full eight-domain reviewed catalogue | External gate | Fire / first-aid launch drafts, industrial pathways; qualified review and remaining authored domains needed |
| Local procedure author / independent review / withdrawal | Implemented | Author submits; independent appointed reviewer records qualification/scope; approved-only learner library on web and Android. |
| Qualified content / translation approval | External gate | Software cannot supply real reviewer approval |
| Approved reference identity versions | Implemented | Encrypted adult references, attended approval, expiry/replacement/withdrawal; no automated face matching. |
| Disclosed snapshots and private evidence | Implemented; deployment pending | Entry, random foreground snapshots and mandatory final frame; encrypted storage, challenge/replay controls, private examiner access. |
| Reference pinned to each attempt | Implemented | Only approved current enrolment reference starts a session; immutable pinned ID used during review. |
| Review, reasons and appeal | Implemented | Formative evidence review and independent identity appeal |
| Face matching and liveness | External gate | No configured/validated provider; manual identity review remains available |
| Front / rear camera AR checkpoints | Partial | Native camera lifecycle controls exist; monitored web practice uses front camera only |
| Practical observations / simulation distinction | Implemented | Rubrics, immutable observations and explicit evidence scope |
| Independent institutional certificate issuance | Implemented, formative scope | Ed25519 institute-issued course records gated by actual reviews, policy and observed practice; no accreditation or work authorisation. |
| Certificate verification / expiry / revoke / replace | Implemented | Public signature verification, Active/Expired/Replaced/Revoked states, QR, print, atomic replacement and key rotation. |
| Notifications / inbox / reports / CSV export | Implemented | Learner inbox, insights, review timelines and bounded exports |
| Guardian / facilitator linked profiles | Implemented | Owner-invited, email-bound, expiring access to completed-scenario counts and last practice only; revocation and leave; no legal guardianship assertion. |
| Offline learning / visible pending sync | Implemented | Android local packs and scoped queues |
| Fully offline verified exams | External gate | Requires an approved supervised route, not uploaded logs alone |
| Approved-content coach with exam boundary | Implemented | Bounded authored/retrieved explanations and provider adapter |
| TinyFish public reference collection / review | Implemented | Server-only provider, source approval, budget, immutable versions, lesson links |
| Private retention / withdrawal / deletion | Implemented for new workflows | Session/reference expiry and erasure, immediate withdrawal, support-link expiry; encrypted backups have separate retention. |
| Light Spatial Studio / responsive alternatives | Implemented | User-selected theme; no redesign requested |
| Production TLS / CI / backups / migration rollback | Implemented | VPS systemd, Linux CI artifacts, verified local backups |
| Managed PostgreSQL / RLS / S3 / queue migration | Pending | Current bounded single-node SQLite deployment; this is an infrastructure migration, not a cosmetic stack change |
| Offsite recovery / production alerts / load targets | External gate | Local restore evidence exists; destination and representative load remain unvalidated |
| Legal / child / biometric governance | External gate | Requires deployment-specific legal and institutional review |
| AR-vs-video trial / retention / field effectiveness | External gate | Must collect real pilot evidence; no lives-saved or accident-reduction claims |
| Role login handover | In progress | Controlled demonstration institute and 13 account types; passwords private, staff MFA required; existing accounts preserved. |

## Acceptance for changes in this delivery

- Tenant and role checks apply to APIs, exports, photos and direct links.
- Pending/rejected/replaced references cannot satisfy a new identity session.
- Existing records and unrelated workspace changes are preserved.
- New workflows have useful empty/error/success states and keyboard/mobile access.
- New credentials distinguish participation, simulation and observed practice and never imply government accreditation.
- Secrets and login passwords are delivered privately, excluded from Git and public assets.
- Every external dependency remains visibly disabled or labelled until configured and verified.

## Verification and final status

Local verification: 136 web unit tests; type checking, ESLint, production build and portable worker checks; 89 institutional HTTP assertions; identity lifecycle regression; 314 Android JVM tests and signed release build. Browser checks cover 14 role pages at 1440/390/320 CSS px, policy publication, CSV import, local procedure submission and public verification. Additional coursework and deployment checks are recorded in the release report.

## Remaining implementation and external gates

- Fine-grained staff access restricted to particular cohorts/departments is **not implemented**. Current staff permissions span their approved institution. Do not enrol segregated departments expecting isolation within one institution.
- Google/OTP/SSO, verified email recovery and billing require selected credentials/provider setup plus complete MFA-preserving integration. These are not switched on.
- Full approved eight-domain curricula, translations, automated face matching/liveness and validated physical competence need qualified review or validated providers; this release does not fabricate them.
- Monitored native AR sessions that alternate rear/front cameras and recover after offline interruption are not implemented. Current monitoring is explicit foreground web practice. Native offline learning is available without a verified-exam claim.
- Staff coursework review is now available on web and Android. Unmonitored learner coursework sharing currently uses Android; web supports monitored assigned practice and personal learning.
- Managed PostgreSQL/RLS/object storage/queues, offsite backups, pager integration and representative scale/load evidence remain infrastructure work. Current production uses bounded SQLite, systemd workers and local backups.
- Legal/child/biometric governance, physical-phone AR acceptance, full accessibility audit and a measured AR-versus-video pilot remain release gates for real deployments.

These items mean the complete architecture is **not finished or field-certified**. The release is a usable institutional pilot with the limits above, not a claim that every future feature is implemented.
