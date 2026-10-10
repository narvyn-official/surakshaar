# Role workspace acceptance — 11 October 2026

Feature scope: role-specific web/native landing, role navigation, current summaries, metadata-only auditor access, approved-institute selection and recovery. Light Spatial Studio retained. This is an evaluation prerelease, not clinical-content or regulatory approval.

| Acceptance | Result | Executed evidence and limits |
| --- | --- | --- |
| Web logic and packaging | Pass | 138 unit tests, TypeScript, ESLint with no errors, production Next build, isolated Safety Updates worker package check. One existing CourseworkReview cleanup-ref lint warning remains. |
| Authorisation and identity context | Pass | 161 isolated HTTP assertions across 13 role homes: default/explicit workspace, direct links, role/tenant boundaries, unknown and revoked selection, metadata-only audit and mutation denial. |
| Staff MFA | Pass | Ten privileged role homes retain the correct role, return no counts or queue and direct the user to account security while unenrolled. Production MFA is not disabled. |
| Web navigation and responsive layout | Pass | Chromium: all 13 role homes and 47 own-role task routes; 1440, 390 and 320px widths, no overflow or runtime errors after fixes. Menu disclosure, audit filtering/export, reviewer without authoring controls and keyboard skip link exercised. |
| Access recovery | Pass | Browser clears a removed workspace selection and reopens the authorised institute. Wrong-role task navigation returns a visible notice. |
| Android logic and package | Pass | 317 JVM tests, debug build, lint and instrumentation compilation; locally signed release code 35, APK signature and 16KiB zip alignment checks. Signed update installed over the prior release on API36; launcher opened successfully without clearing app data. |
| Native account flow | Pass | Dedicated API36 emulator: 13 real synthetic account sign-ins against isolated HTTP server; each native role dashboard rendered its expected role and actions, survived recreation and signed out. Rate limits were respected with bounded waits. The final native source also passed a second 13-role flow plus a controlled service-failure test: only a cached learner receives offline practice, and account changes clear that hint. |
| Browser/native task boundary | Documented | Native learning, coursework and compatible legacy administration are retained; other operational tools open web routes with their own browser sign-in/MFA. |
| Data and rollout | Pass | Zero migrations; all 61 original tables preserved. Isolated rehearsal, service-account journal ownership recovery, 3-second cutover and checked post-release backup. See deployed report. |
| Accessibility scope | Partial | Visible focus, keyboard skip link, labelled controls, mobile menu and reflow checked. Reduced-motion and forced-colour styles supplied; full screen-reader audit and every OS/browser not covered. |
| Theme/assets/dependencies | Pass / N/A | Existing light visual direction and installed icons retained. No new dependencies, fonts or external visual assets adopted. No prohibited sparkle or northeast-arrow decoration added. |
| Earlier domain limitations | Unchanged | Clinical review, physical-AR acceptance, cohort-specific staff scope and unconfigured external providers remain documented in the 0.17 checklist. No such capability is newly claimed here. |

Local private evidence: `.runtime/roles-0.18/` contains logs and synthetic screenshots. It is deliberately excluded from the public source. Real demonstration passwords and session cookies are not published.

Hosted CI for application commit `5a648f6940c00d2f90823b260acf333e02286e70`: [web and Android passed](https://github.com/narvyn-official/surakshaar/actions/runs/38094265185).

The final Android commit `3bb003f0245b8b59ecf4ce9dec26bd6b4c88f8f9` also [passed hosted web and Android CI](https://github.com/narvyn-official/surakshaar/actions/runs/38094860631). Public static delivery was separated from page/API throttling after reproducing a live chunk-load failure. Nginx configuration validation and a 40-request static burst passed; protected APIs remained closed to anonymous requests.
