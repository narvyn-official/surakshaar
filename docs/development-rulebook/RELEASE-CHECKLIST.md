# Development acceptance and release checklist

Use with [The Development Rulebook](RULEBOOK.md). Select checks for the feature and release; mark unrelated checks **N/A with a reason**. This checklist does not replace full WCAG, ASVS, domain, or regulatory verification.

**Feature / version:**  
**Reviewer / date:**  
**Platforms / roles tested:**  
**Environment / build:**  
**Release owner:**

Status vocabulary: **Pass · Fail · Partial · Not tested · N/A**. Record actual evidence, not a checked box based on intent.

## Product and workflows

- [ ] Primary users, task, success event, and guardrails are defined. (PRO, MET)
- [ ] Each role can discover and access its legitimate tasks; backend permissions match the access matrix. (FLOW, SEC)
- [ ] First use, returning use, direct links, invitation/notification entry, and wrong-account states are covered. (FLOW)
- [ ] Users can review, correct, complete, recover from, and revisit consequential tasks. (FLOW)
- [ ] Prices, commitments, cancellation, support, and privacy information are accurate. (PRO, PRI, PAY)

## Screen anatomy and interaction

- [ ] Global, page, collection, item, and selection actions have clear placement and scope. (LAY)
- [ ] Navigation, search, filters, sorting, back/forward, and deep links work as intended. (NAV)
- [ ] Transitions define focus, title, route/history, scroll restoration, dirty state, and final data. (TRN)
- [ ] Loading, empty, error, offline, denied, pending, and success states are distinct and useful. (STA)
- [ ] Rapid actions, stale responses, interrupted transitions, and concurrent edits behave safely. (TRN, MIC)

## Overlays and messaging

- [ ] Each overlay has a justified surface, declared modality, reachable actions, and a dismissal contract. (OVR)
- [ ] Modal focus, inert background, scroll behavior, Escape/Back, and focus restoration are verified. (OVR)
- [ ] Sheets adapt to content, text zoom, keyboard, safe areas, and viewport height. (OVR)
- [ ] Tooltips/popovers work with keyboard and pointer and do not hide essential instructions. (OVR)
- [ ] Banners, chat, toasts, sticky bars, and dialogs do not obscure one another or focus. (OVR, MSG)
- [ ] Urgency, scope, lifetime, announcements, deduplication, and durable notification destinations are correct. (MSG)
- [ ] Copy, save, autosave, favorite, upload, delete, undo, and bulk actions show real outcomes and recover from errors. (MIC)

## Visual quality, themes, and responsiveness

- [ ] Real content, long text, large values, localization, and truncation are checked. (VIS, CNT)
- [ ] Semantic colors and all applicable contrast pairs are verified, including interaction states. (COL)
- [ ] Both themes and System preference work where in scope; choice persists and first paint is consistent. (THM)
- [ ] Narrow/wide widths, intermediate breakpoints, portrait/landscape, short windows, and virtual keyboards are checked. (RSP)
- [ ] Essential targets meet applicable sizing rules; hover/drag/gesture alternatives exist. (RSP)

## Accessibility

- [ ] Complete primary journeys work using only a keyboard, with logical visible focus. (A11Y)
- [ ] Screen-reader labels, roles, headings, errors, dialogs, and dynamic announcements are verified. (A11Y)
- [ ] 200% text resizing, 320 CSS px reflow/appropriate 400% zoom, and text-spacing overrides work. (A11Y, RSP)
- [ ] Reduced motion, forced colors, media alternatives, and non-color cues are checked as applicable. (A11Y, COL)
- [ ] Automated accessibility findings are triaged; manual checks and remaining gaps are recorded. (A11Y)

## Accounts and security

- [ ] Password login supports reveal/hide, managers, autofill/paste, useful errors, and working recovery. (AUTH)
- [ ] Password policy, storage, MFA, passkey/SSO, and sensitive account changes fit the assurance needs. (PWD, MFA, SES)
- [ ] Reset tokens are account-bound, expiring, single-use, protected from leaks, and atomically consumed. (REC)
- [ ] Sessions expire/revoke correctly; logout and account switching clear sensitive state. (SES)
- [ ] Role, object, property, and tenant authorization are tested with negative cases. (SEC)
- [ ] Applicable injection, CSRF, upload, SSRF, CORS, rate-limit, and business-logic controls are verified. (SEC)
- [ ] No secrets or sensitive records leak through bundles, errors, logs, analytics, or caches. (SEC, PRI)

## Data, commerce, and privacy

- [ ] Data minimization, purposes, recipients, retention, and applicable user controls are defined and tested. (PRI)
- [ ] Optional consent and notification preferences are honored; cancellation is discoverable. (PRI, RET)
- [ ] Prices and entitlements are server-authoritative; webhook authenticity and deduplication are tested. (PAY)
- [ ] Pending payments, refunds, renewals, export, deletion, and concurrency are covered where relevant. (PAY, ENG)
- [ ] Dashboard/chart labels, freshness, units, missing data, filters, and permission scope are accurate. (DATA)

## Performance, platforms, and discovery

- [ ] Representative performance budgets have evidence; field targets and lab results are labeled separately. (PERF)
- [ ] Supported browsers/devices and low-end/slow-network conditions have suitable coverage. (RSP, PERF)
- [ ] Native permissions, secure storage, deep links, lifecycle interruption, and distribution requirements are checked. (NAT)
- [ ] Public/private indexability, redirects, titles, metadata, and error pages are correct. (SEO)
- [ ] Language, right-to-left layout, currencies, dates, units, and time zones are checked as applicable. (CNT)

## DevSecOps and operations

- [ ] Relevant build, type, lint, unit, integration, security, and end-to-end checks pass or have documented blockers. (ENG)
- [ ] Dependency/secret/code/infrastructure findings are triaged with owners. (DSO)
- [ ] Untrusted builds cannot access deployment credentials; release artifact provenance is appropriate. (DSO)
- [ ] Migrations, environment separation, rollout, and rollback are validated for this release. (ENG, DSO)
- [ ] Monitoring, support, incident ownership, and alert destinations are ready. (OPS)
- [ ] Important data has a verified restore path with stated recovery objectives. (OPS)
- [ ] Current advisories, external requirements, and relevant legal/platform changes were reviewed. (DONE)

## AI and measurement

- [ ] AI retrieval/actions obey backend permissions; injection, output failures, privacy, and spend controls are evaluated. (AI)
- [ ] Analytics events and denominators are validated, with consent behavior and sensitive-data redaction. (MET)
- [ ] Any experiment has an outcome, guardrails, assignment/sampling plan, and stopping rule. (MET)

## Design enhancement adoption

- [ ] The visual direction fits the audience, task, actual content, and existing brand; signature effects have a reason. (ART)
- [ ] Selected code, asset, and font provenance, versions, licenses, and required notices are recorded. (LIB)
- [ ] Imported code/dependencies and registry installation behavior have appropriate inspection. (LIB, DSO)
- [ ] Each enhancement has an interaction contract, usable static baseline, and mobile/keyboard alternatives. (MOT)
- [ ] Reduced motion, pause controls, interruptions, cleanup, rendering failure, and expensive effects are checked as relevant. (MOT)
- [ ] Theme/material contrast, source order, real content, and supported responsive transformations are verified. (DSN)
- [ ] Actual performance and task evidence justify the enhancement; unverified concepts are labeled. (LIB)

## Evidence and exceptions

| Check / rule IDs | Status | Evidence: test, screenshot, report, commit | Gap or exception | Owner / review date |
|---|---|---|---|---|
| Fill with actual results | Not tested | — | — | — |

**Production-ready decision:** Ready / Ready with documented limitations / Blocked  
**Remaining blockers:**  
**Authorized release action, if any:**  
**Post-release measurement / incident owner:**

Do not mark this template complete by default. The rulebook’s creation does not mean any current project has passed these checks.
