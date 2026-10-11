# SurakshaAR role and workflow checklist

11 October 2026. Scope: make each existing role able to discover and complete its intended work, and verify both allowed and forbidden operations. Implementation and verification are complete for the role-workflow scope below. These are local verification results, not a deployment or field-certification claim. External login/recovery, payment and offsite-backup providers remain disabled by the owner's decision. Existing light Spatial Studio styling is retained.

## Role contract

| Role | Required features and successful outcome | Scope and explicit limits |
| --- | --- | --- |
| Guest | Open public learning, explore 3D/AR where supported, practise without monitoring, view public record verification, create an account | No private institute, account, evidence or administrative data |
| Personal learner | Own dashboard, learning, account practice save/resume, optional sharing/support, join learner or staff invitations, request an institute | Own records only; no camera monitoring for personal practice; an application does not confer admin rights |
| Institution learner | Own assignments, private save, explicit submission, feedback/correction/withdrawal, approved local procedures, own issued records; eligible adult monitored practice and identity enrolment | Active linked institute membership; cannot review own work or issue records; no other learner's evidence |
| Guardian / facilitator | Accept a learner's invitation, read the limited shared progress summary, leave the relationship | Explicit unexpired sharing only; no private reflections, answers, identity photos, assessment or issuance powers; no claim of verified legal guardianship |
| Platform owner | Institute application decisions, suspend/restore institutes, service plans/limits, source governance and operational status | Platform operations dashboard; no default training/test tasks; cannot approve own institute; staff MFA and recent sign-in for sensitive changes |
| Institute administrator | Register/import/organise people, link learner accounts, invite/revoke staff with unit scope, publish programme/identity policy, assign cohorts, record observations, oversee/revoke records, settings and audit | Own approved institute; cannot certify without issuer role, perform examiner identity decisions, or approve authored content; policy changes are prospective |
| Instructor | Assigned-scope learners/programmes, assign/cancel practice, review shared coursework, record supervised observations; sourced local authorship when institution-wide | No team/policy administration, identity-photo assessment, independent content approval or issuance; scoped staff use only scoped tools |
| Examiner | Shared coursework, reasoned feedback, adult identity/reference evidence, independent appeal review | Own active institution and allowed units; no assigning, staffing, policy changes or issuance; no self-review |
| Certificate issuer | Read reviewed coursework, check prerequisites, issue/replace/revoke signed institution records, eligible audit metadata | No grading or identity-photo decisions; no self-issuance; independent issuer/practical/identity prerequisites enforced |
| Examiner and issuer | Examiner and issuer tools in one workspace | Each operation retains its own checks; separate-issuer policy can require another person |
| Auditor | Filter chronological activity, inspect event metadata, export the visible page | Read only; excludes private target identifiers, event payloads, answers and identity photos |
| Read-only learning viewer | Read authorised learning progress, shared coursework and issued records | No changes, feedback, observation, issuance or identity photos; unit scope applies to reads and exports |
| Content author | Create/save/reopen/edit a draft, submit it, see independent feedback, create a new revision after review | Own procedures only; no learner directory/evidence, self-approval or editing approved history |
| Content reviewer | Read submitted procedures by others, record qualification/reasoned approval or return, withdraw approved content | No authoring via UI or API; no learner data; no self-review or visibility into others' private drafts |

## Shared acceptance checks

- [x] Fresh sign-in dispatches all 13 account roles to the correct dashboard; guests keep public learning.
- [x] Legitimate tools are reachable from role navigation. Wrong-role direct pages redirect and direct APIs deny access.
- [x] New staff can accept an email-bound invitation before MFA setup, then reach their assigned dashboard and complete security setup.
- [x] A new institute applicant can submit, revisit pending/rejected/suspended status and reach approved administration.
- [x] Membership, institution approval, selected workspace and staff MFA remain authoritative on every protected request.
- [x] Unit scope applies to lists, detail, writes, private photos, exports and dashboard counts. Revocation/moving/archive removes future access.
- [x] Reviewer and author duties remain separate on the server; content drafts can be saved, resumed, edited and submitted without overwriting reviewed history.
- [x] Every role's dashboard summary and queue lead to work that role can perform; withdrawn/archived/unlinked evidence is not presented as actionable.
- [x] Save/pending/error/retry states preserve entered work and avoid duplicate consequential writes.
- [x] Account/workspace transitions discard stale data; role navigation contains no inaccessible task links.
- [x] Desktop and narrow layouts, keyboard access, accessible labels and displayed errors are checked in a browser.
- [x] Android shows the same permitted server actions and clearly identifies operations that open a separate browser session.

## Initial findings to implement

| ID | Gap | Required repair | Status |
| --- | --- | --- | --- |
| R01 | Invitation acceptance is inside a legacy workspace inaccessible to newly invited staff | Add a reachable authenticated institute-access page and entry links, including MFA/revoked recovery states | Implemented and verified locally |
| R02 | The institute application form is no longer reachable from a personal dashboard | Expose application/status and approved-dashboard continuation without bypassing approval | Implemented and verified locally |
| R03 | Content drafts cannot be edited or promoted to review; reviewer API can author despite UI separation | Revision-checked draft commands, separate author permission, own draft visibility, immutable reviewed revisions and complete editor | Implemented and verified locally |
| R04 | Several nested task screens advertise links/actions outside the current role | Use the same role contract for local navigation and empty-state actions | Implemented and verified locally |
| R05 | Restricted dashboards use generic counts and queues; some unrestricted counts include no-longer-actionable records | Role-specific, scope-filtered actionable counts and destinations | Implemented and verified locally |
| R06 | Previous checks cover routes but do not prove all current role journeys together | Repeat isolated end-to-end workflow and negative-access checks, then rendered browser and Android verification | Implemented and verified locally |

## Evidence and honest limits

The evidence below was executed against an isolated synthetic SQLite database and local Next.js service. Staff MFA was enabled for a separate production-build verification pass. Tests use isolated synthetic users/data, not real learner records. Local changes are not claimed deployed until a release is actually deployed and checked.

Physical AR device coverage, qualified safety/clinical review, actual accreditation, biometric provider validation and unavailable external services remain separate gates in `surakshaar-0.19.0/COMPLETION-CHECKLIST.md`. These cannot be established merely by passing role-access tests.

## Completed role verification

| Role | Actual verified outcome | Evidence |
| --- | --- | --- |
| Guest | Public studio available; institute access requires sign-in and private APIs deny unauthenticated access | Role journeys HTTP suite |
| Personal learner | Own practice save, another account denied the profile, explicit support invitation, pending institute application remains unprivileged | Role journeys HTTP suite; browser application/status flow |
| Institution learner | Enrolment, assigned practice private-save/share, feedback/correction/withdrawal, approved local library and own records | Institution and completion HTTP suites; browser permitted routes |
| Guardian / facilitator | Email-bound invitation accepted, only progress summary returned, relationship leave removes access | Institution and role journeys HTTP suites |
| Platform owner | Independent application rejection/approval, suspend/restore, manual plan configuration; other roles denied platform settings | Role journeys HTTP suite; actual browser application approval |
| Institute administrator | CSV/cohort setup, learner invitation, staff invitation and scope/revocation, programme policy, observations and signing-key rotation | Institution/completion HTTP suites; browser staff invitation |
| Instructor | Assign cohort/individual practice, scoped reads, practical observation, no identity-review/issuance privileges | Institution and completion HTTP suites |
| Examiner | Read/review shared work, adult identity decision, scoped private evidence; no staffing or issuance privileges | Institution, completion and role journeys HTTP suites |
| Certificate issuer | Prerequisite rejection, eligible signed issuance, replacement/revocation and historical-key verification; cannot grade or view identity photos | Institution and role journeys HTTP suites |
| Examiner and issuer | Both workspaces available; feedback succeeds; own issuance fails when the pinned policy requires a separate issuer | Role journeys HTTP suite |
| Auditor | Metadata-only timeline/export; protected learner APIs and all tested mutations denied | Role-workspace and role journeys HTTP suites |
| Read-only viewer | Assigned-scope coursework/records readable; writes and identity photos denied | Completion and role journeys HTTP suites; browser invitation lands at viewer dashboard |
| Content author | Own private draft save/retry/edit/submit, stale edits rejected, reviewed history cannot be overwritten, new revision accepted | Role journeys HTTP suite; actual browser save/reload/edit/submit with simulated save failure |
| Content reviewer | No author API, cannot inspect another author's private draft, independent return/approval/withdrawal, self-review denied after role change | Role journeys HTTP suite; actual browser content approval |

All 13 account roles passed default dashboard dispatch, wrong-role redirection and intended tool discovery. Role test assertions also cover revoked/unknown selected institutes and multiple-institute switching. Core task HTTP checks cover tenant, cohort and individual record boundaries; this is not an exhaustive security audit.

## Executed evidence

- **145 web unit tests passed.** Type checking, ESLint (zero warnings/errors), production build and isolated safety-worker package verification passed.
- **589 integration checks passed:** 89 institution checks, 161 role checks, 124 scoped access/submission checks, 140 additional role journey checks, and 75 production-build MFA checks. Some suites count HTTP requests and others count explicit assertions, so this is a combined check count, not 589 independent user studies.
- **Browser:** all 13 dashboard homes and 48 advertised task routes; 1440, 390 and 320 CSS-pixel layouts. New staff invitation, applicant submission/pending reload/platform approval, author draft save/reload/edit/submit, reviewer approval, temporary-save failure recovery and keyboard skip navigation completed. No browser runtime errors. Dashboard, reviewer, invitation and application screenshots were visually inspected; a narrow-screen notice layout defect was corrected.
- **Android:** 317 JVM tests passed, debug packaging and lint passed. A fresh API 36 emulator passed the role dashboard transport test for all 13 roles and offline learner/owner recovery (2 instrumentation tests). The new institute-access browser entry is checked for each role. Existing emulator installation/data was preserved by using a separate test emulator.
- **MFA:** all 10 staff/operator roles keep their role identity but reveal no statistics/queue before enrolment and cannot call protected APIs. The 3 non-staff roles are not given a staff MFA gate. Actual TOTP enrolment, password-only session denial, one-use recovery, and invitation acceptance followed by the MFA gate passed against the production build. No live account credentials or factors were changed.
- Database integrity and foreign-key checks passed. **No migration is needed.** Existing data structures and reviewed records are preserved.

Test sources: `apps/admin-web/tests/role-journeys.integration.ts`, `role-journeys.browser.mjs`, `role-mfa.integration.ts`, and the existing institution/completion/role-workspace suites. The two older fixture-dependent suites now accept isolated fixture paths through environment variables so new checks do not overwrite old evidence.

Local execution records are under `.runtime/institution-0.17/role-workflows-20261011/`. This directory also contains private synthetic login fixtures and must not be published or bundled. The useful records are `institution.log`, `roles.log`, `completion.log`, `journeys.log`, `browser/result.json`, `android-flow.log`, `android.log`, `mfa.log`, and the final build/check logs.

## Boundaries and remaining work

- **Not deployed:** these source changes and build checks are local. The existing VPS and distributed release APK have not been updated by this task.
- Browser-based operational tools remain the supported path for some Android roles. The separate browser requires its own authentication and MFA; native session cookies are not transferred in URLs.
- Camera/AR mechanics, low-end physical-device coverage, qualified curriculum review, accessibility with actual assistive devices, and actual learning effectiveness remain separate validation work. The isolated identity test uses synthetic encrypted evidence, not human facial recognition or liveness verification.
- Google/OTP/SSO, delivered recovery mail, payment automation and offsite services remain disabled as requested. Manual institutional plans and existing configured production integrations are unchanged.
- Source-level role authorization follows least privilege and per-request checks. [OWASP's authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html) was checked on 11 October 2026. This verification is not an OWASP/WCAG certification.
