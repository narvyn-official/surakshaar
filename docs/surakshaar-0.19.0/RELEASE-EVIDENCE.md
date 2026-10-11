# Completion work: release evidence

11 October 2026 · deployed 0.19.0-rc.1 / Android code 36.

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
- Linux x64 package from [CI run 38100505323](https://github.com/narvyn-official/surakshaar/actions/runs/38100505323), source `c0321deb1793f074fd898ac6e9d2c84e7d9ff132`, passed both CI jobs. Archive SHA-256 `e72f5eef7a4b9777be4c0c3481acce4754ffc474cb4bfcfff13f6d1e165cd306` was verified on the VPS.
- Rehearsal and live migration applied two additive migrations, preserving original values in all 61 existing tables and adding two tables. Integrity and foreign-key checks passed. Cutover took six seconds; previous application and private recovery files were retained.
- Live HTTPS passed 12 readiness, public-route, authentication and cache checks. All 14 demo accounts retained their role homes, unrestricted default scope and required staff MFA; no credentials changed. A live 390-pixel browser pass covered all 14 accounts with correct role routing, MFA state, no horizontal overflow, no failed HTTP resources and no runtime errors; owner, learner and auditor views were captured, with owner and learner screenshots inspected.
- Five local operational checks passed. A fresh scheduled backup was copied and opened independently: integrity, foreign keys and all 31 schema migrations passed. Root owns the monitor report, with read-only group access for the app.
- New Google/email OTP integrations remain disabled; the existing TinyFish configuration is retained. Payments and offsite services remain unconfigured by owner decision.

## Rulebook application

PRO/FLOW/SEC/PRI/STA/JNY: scoped roles, backend enforcement, immutable feedback, separate private save/share, explicit withdrawal, stale-response protection and retryable save failures are acceptance requirements.

RSP/A11Y: existing light Spatial Studio components retained; no new visual library or effects. Labels, focus, mobile reflow and real rendered states are checked. Full assistive-device audit, low-end physical AR and domain validation remain open; this is not a WCAG/ASVS conformance claim.

DSO/OPS: two additive migrations, original-column data preservation, isolated rehearsal, retained previous release, immutable artifacts and bounded service resources are required before cutover. No production credential, identity sample or private fixture is in source or release assets.

Rollback constraint: once any active staff member has restricted unit access, do not roll back to 0.18 because that application cannot enforce the new scope. The cutover rollback guard leaves services stopped instead of widening access; use a scope-aware repair. Application rollback retains the current database and encryption key.

## CI maintenance

Updated pinned actions after current GitHub release/runner guidance review: [checkout v7.0.1](https://github.com/actions/checkout/releases/tag/v7.0.1), [setup-node v7.1.0](https://github.com/actions/setup-node/releases/tag/v7.1.0), [setup-java v6.0.1](https://github.com/actions/setup-java/releases/tag/v6.0.1), [upload-artifact v7.0.2](https://github.com/actions/upload-artifact/releases/tag/v7.0.2). All use the Node 24 action runtime. Ubuntu is pinned to 24.04 to avoid an unreviewed major runner-image switch. This changes future CI; it does not replace the verified deployed artifact.

The updated CI workflow passed both web and Android jobs in [run 38100986817](https://github.com/narvyn-official/surakshaar/actions/runs/38100986817), commit `5339f18e321389c224688d0cca73d29a3ac6ec92`. This is a workflow/documentation change; deployed runtime source remains `c0321de`.
