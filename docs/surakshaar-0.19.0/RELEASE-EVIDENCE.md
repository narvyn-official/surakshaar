# Completion work: release evidence

11 October 2026 · candidate 0.19.0-rc.1 / Android code 36.

The delivery closes cohort-scoped access and the web coursework submission journey, and adds local operations monitoring. It does not make the full proposed architecture field-certified. See the [completion checklist](COMPLETION-CHECKLIST.md) for remaining engineering and external gates.

## Changes

- Staff scope is optional and defaults to existing institution-wide access. Administrators can restrict instructors, examiners, issuers, combined assessors and viewers to selected units and active descendants. People without placement are unavailable to scoped staff.
- Lists, detail views, CSV, private identity references/frames, issuance and dashboard counts enforce the same boundary. Consequential writes recheck membership and placement within the write transaction; stale access rolls back the whole batch. Legacy imports/audit tools deny scoped access by default. Their links are filtered on web and native dashboards.
- Learners can open an assignment on the web, complete its version/mode-matched practice, save privately, recover an account-scoped tab draft after reload, resume, explicitly share, read feedback, request correction and withdraw future access. Private reflections remain excluded. Snapshot/supervised requirements remain distinct from this unmonitored formative practice.
- The platform owner sees fresh/stale/unavailable local operational checks. A five-minute service checks web/database readiness, disk headroom, verified-backup freshness, worker and maintenance timer. This is not offsite restore validation or external alert delivery.
- Scheduled backups publish their final filename only after integrity and foreign-key checks. The public readiness endpoint is read-only.

## Verification record

- 144 web unit tests passed, including recursive tenant scope, revoked/moved/archived access, transactional rollback, backup restore and rejection of corrupt backup publication.
- 124 isolated HTTP assertions passed for five scoped roles and the actual private-save/share/review/correct/withdraw lifecycle.
- Type checking, lint (zero warnings/errors), final web production build and isolated Safety worker package checks passed.
- 317 Android JVM tests, debug packaging and lint passed; signed release APK built using the existing key. Exact signed APK updated an existing API 36 emulator installation to code 36 without clearing data and launched successfully. Native change is the scoped-access notice; dashboard actions are server supplied.
- Browser testing completed tab-draft reload recovery, account save/resume, practice, explicit sharing, correction, confirmation/cancellation and withdrawal at 1440/390/320 widths. It found a legacy direct-link routing gap and a cramped notice layout; both were corrected. Final rerun passed with no browser runtime errors. The final mobile team cards and form were inspected in viewport screenshots. A separate simulated save-failure check confirmed the draft survives reload, is keyed to the signed-in account, remains labelled unsaved until account upload succeeds, and is removed from temporary storage after a successful save. Explicit sign-out clears temporary coursework drafts.
- Owner operations UI passed temporary-failure retry and stale/fresh refresh checks using an explicitly synthetic report; 390-pixel viewport inspected with no overflow or runtime errors. Actual server checks remain part of deployment verification.
- A separate 161-assertion role regression passed for all 13 role homes and negative tenant/access cases.
- Live deployment, exact artifact verification and migration rehearsal pending.

## Rulebook application

PRO/FLOW/SEC/PRI/STA/JNY: scoped roles, backend enforcement, immutable feedback, separate private save/share, explicit withdrawal, stale-response protection and retryable save failures are acceptance requirements.

RSP/A11Y: existing light Spatial Studio components retained; no new visual library or effects. Labels, focus, mobile reflow and real rendered states are checked. Full assistive-device audit, low-end physical AR and domain validation remain open; this is not a WCAG/ASVS conformance claim.

DSO/OPS: two additive migrations, original-column data preservation, isolated rehearsal, retained previous release, immutable artifacts and bounded service resources are required before cutover. No production credential, identity sample or private fixture is in source or release assets.
