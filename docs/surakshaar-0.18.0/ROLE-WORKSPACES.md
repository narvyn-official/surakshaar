# SurakshaAR role workspaces — 0.18.0-rc.1

This release gives each account a role-specific starting point, navigation, summary and work queue in the selected light Spatial Studio theme. The platform owner opens platform operations, not tests or a learner dashboard. Existing institute workflows remain behind their server permissions.

| Role | Default route | Primary responsibilities |
| --- | --- | --- |
| Platform owner | `/dashboard/platform` | Institute approvals, suspension, service allowances and safety-source governance |
| Institute administrator | `/dashboard/admin` | People and cohorts, invitations and role access, programme policy, enrolment, supervised observations and oversight |
| Instructor | `/dashboard/trainer` | Assign practice, provide feedback, record supervised observations, prepare local procedures |
| Examiner | `/dashboard/examiner` | Coursework and assessment evidence, adult identity review and appeals |
| Certificate issuer | `/dashboard/certifier` | Read reviewed coursework; issue, replace and revoke eligible signed learning records |
| Examiner and issuer | `/dashboard/assessor` | Both duties; existing self-review and independent-issuer requirements still apply |
| Auditor | `/dashboard/auditor` | Read-only event metadata, filtered chronological pages and current-page CSV |
| Read-only learning viewer | `/dashboard/viewer` | Shared learning progress, assessment and issuance status; no edits |
| Content author | `/dashboard/author` | Own sourced local procedures and independent-review status |
| Content reviewer | `/dashboard/reviewer` | Another author’s procedure review queue; no authoring form |
| Institution learner | `/dashboard/learner` | Own practice, assignments, feedback, enrolment and learning records |
| Personal learner | `/dashboard/personal` | Self-directed practice, account backup and optional institution enrolment |
| Guardian / facilitator | `/dashboard/supporter` | Only progress summaries a learner has explicitly shared |

## Dispatch and permissions

- Root and `/dashboard` dispatch authenticated users to their authorised home. Guests retain the public learning studio.
- Server precedence: explicitly configured platform operator, approved active staff membership, active institution learner, active support relationship, personal learner.
- Multiple staff memberships are selected using a server-validated institute switcher. A revoked, suspended or unknown prior selection reveals no dashboard counts or queue and must be explicitly changed or cleared.
- Staff MFA remains required in production. An unenrolled staff member sees their own role and account-security setup, with no records. This release does not enable MFA on anyone’s behalf or reset credentials.
- Wrong-role dashboard URLs and task pages redirect to the actual role home. Backend permissions, owner scope, MFA and existing workflow rules remain authoritative even for direct API requests.
- The new auditor role can read audit metadata only. The audit endpoint returns event ID, actor display name, action and timestamp; it excludes event payloads, actor email, target identifiers, learner answers and identity photographs. Export is limited to the current page of up to 100 events.
- Personal/learner summaries distinguish account-backed practice from unsynced device work. Counts are server-derived, refreshed explicitly and timestamped.

## Android

Version code 35 opens an authenticated account’s native role dashboard at launch and after sign-in. Guest launch still opens offline learning. Signed-in learners can recover to offline practice if the role service is unavailable; the last role is only a presentation hint and is cleared on account change. The native dashboard uses the same authorised summary endpoint and supports institute selection, access recovery, refresh and sign-out. Operational roles do not get learner actions.

Native learning, coursework review and compatible existing administration tasks open their native screens. Platform configuration, content governance, audit, institution awards and other web-only tasks open their corresponding browser route. The browser has a separate sign-in session, including MFA; native cookies are never copied into a URL. Full native implementations of those web tools are not claimed.

## Verification and scope

Acceptance: each role reaches its legitimate tasks; wrong roles, tenants and revoked selections fail closed; auditor exports contain only metadata; MFA gates preserve role identity; layouts reflow at 320, 390 and 1440 CSS pixels; native account changes discard stale responses.

Final executed evidence is recorded in `RELEASE-EVIDENCE.md`. No schema migration or new dependency is introduced. The existing identity encryption key and provider configuration must be preserved on rollout and recovery.

This change does not expand clinical claims or credential accreditation. Earlier content, physical-AR, provider, cohort-specific staff scope and native monitored-camera limitations remain as documented in the 0.17 implementation checklist.
