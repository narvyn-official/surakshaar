# Institutional pilot 0.17.0

The light Spatial Studio design is retained. This release adds organisation units, CSV enrolment, cohort programmes, versioned policy, adult reference identity, coursework review, institute-issued learning records, local procedure review, limited guardian/facilitator support and manual plan controls.

## Role entry points

| Account | Entry point | Authority |
|---|---|---|
| Platform operator | / then Platform governance | Approvals, manual plans, public safety sources; no automatic access to private institution photos |
| Institution administrator | /institution | People, units, policy, enrolment references, staff invites; cannot issue course records or examine snapshots |
| Instructor / trainer | /coursework | Assignments, learning feedback, observed practice |
| Examiner | /coursework and /identity-review | Coursework feedback and private identity review; cannot issue |
| Certificate issuer / certifier | /awards | Issue eligible institution records, replace and revoke |
| Combined examiner/issuer / assessor | /coursework, /identity-review, /awards | Both duties, still subject to self-review and separate-issuer policy |
| Content author | /institution | Submit local procedures; no learner record access |
| Content reviewer | /institution | Independently approve/withdraw local procedures, with recorded review scope |
| Read-only institution viewer | /coursework | Learner records, no writes or private identity photos |
| Enrolled learner | /learn, /identity-enrolment, /identity, /awards?learner=1 | Own institutional learning and records |
| Personal learner | /studio | Self-directed learning without camera monitoring |
| Guardian / facilitator | /learning-support | Explicit owner-invited, expiring learning summary only |
| Guest | /studio | Learn without an account |

Staff first sign in, open Account security, enable their own authenticator and save recovery codes privately. No staff bypass is installed. Reserved-domain demonstration addresses have no inbox; password recovery mail is not enabled. Change supplied passwords and do not reuse demo accounts for real learner data.

## Data and release operations

- Migrations 0024–0028 are additive. Use the numbered SQL migration runner. Do not apply a generated Drizzle schema diff: the historical ORM schema is not a complete inventory of raw-SQL feature tables.
- `IDENTITY_EVIDENCE_KEY` encrypts adult reference/session photos and institute private signing keys. Keep it backed up with the database. Never rotate by replacing the environment value; a key migration is required.
- Public learning-record verification includes learner name, institution, course, evidence methods, dates and current status. It does not expose employee IDs, answers or photos. References expire after 30 days pending or 90 days approved; session retention follows the institution notice. Private backups expire separately.
- `PLATFORM_OPERATOR_IDS` is a server-only explicit allowlist. `REQUIRE_STAFF_MFA=true` stays enabled in production.
- The support relationship is permission to see a limited progress summary; it does not verify legal guardianship. Monitoring excludes child awareness courses.
- Stored local procedures require separate appointed review. Author/reviewer qualifications are institution attestations, not credentials verified by the platform.
- Course records use per-institution Ed25519 keys. Old public keys remain available after key rotation. The legacy simulation credential feature is separate and remains unavailable without its separate issuer configuration.
- Attempt counters survive routine image deletion to enforce configured limits. Images can be withdrawn. Interrupted sessions are not misconduct findings.
- Keep old release directories and take an online database backup plus environment backup before migration. Verify original columns and rows after migration. Rehearse against a copy first.
- On rollback, retain the new encryption key. New identity-start triggers depend on approved references; disable new monitored attempts when reverting to old code. Prefer a forward repair. Do not silently restore an old database over new activity.

## Evidence and release limits

See [implementation checklist](IMPLEMENTATION-CHECKLIST.md) and the dated deployment report. This is a release candidate for controlled demonstration. Physical-phone AR, clinical approval, legal review, offsite disaster recovery, production load and full assistive-technology checks are not certified by software tests.
