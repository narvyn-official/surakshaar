# Safety Updates verification

10 October 2026. Local pilot only; no deployment, push, public source approval or recurring collection activation.

## Verified

| Area | Evidence |
|---|---|
| Existing workflows | Full web suite: **111 tests passed**, including existing assessment, credential, learning, auth, tutor and migration tests. No Android, course content or assessment files changed for this feature. |
| New policy/collection tests | **11 focused tests** cover staff role boundaries, strict commands, source-operator permission, URL tricks/private ranges, private/mixed DNS rejection, pinned preflight addresses, redirect refusal, missing credentials, malformed provider data, missing/invalid dates, small wording/number changes, bounded body parsing, rate limits, provider errors/timeouts, budgets, job/content deduplication, reversion detection, cross-workspace decisions/audits, stale reviews, partial failures, revocation, cancellation, concurrency and expired-lease recovery. Fault cases use explicit synthetic transports and in-memory SQLite; they are not live provider incidents. |
| Authenticated API | Isolated Node service; actual Better Auth sign-in cookies for administrator, other-centre administrator, trainer, viewer, certifier and learner. **30 HTTP status assertions**, plus response/database assertions, passed for role and tenant denial, CSRF rejection, extra-field rejection, source approval/stale revisions, duplicate jobs, foreign cancellation, final review decisions, scoped notes/audit, disabled schedules and untouched assessment/credential tables. Login rate limits were honoured. |
| Live provider | **One real TinyFish Fetch request passed** against the USFA extinguisher page. Validated final URL/title/content; nullable publication date was preserved. No learner information or institution records were sent. This was a bounded adapter probe, separate from synthetic job and UI tests. |
| Source preflight | Six of eight proposals passed local public preflight. OSHA and AHA returned source-unavailable; no TinyFish request was made for those two. All eight shipped sources remain proposed. |
| Database | All **21 migrations** applied to a new isolated database; integrity and foreign-key checks passed. Existing migrations were not rewritten. Public versions and source-audit entries reject updates. |
| Builds and analysis | TypeScript, ESLint, Node standalone build and separate collection-worker bundle passed. Existing Cloudflare web/API build also passed during implementation; a Cloudflare collection worker is not included. Existing Vite native-loader/deprecation warnings remain. |
| Dependencies/secrets | No new dependencies. Production-dependency audit: **0 reported vulnerabilities**. A value-based check of **3,229 repository/build files found 0 copies of the supplied key** outside private storage. Local backend environment file is git-ignored and mode `0600`; no VPS credential configuration changed. This is a scoped check, not an exhaustive security audit. |
| Desktop/mobile rendering | Inspected at **1440 px**, **390 px** and **320 px**. Fixed a 320-pixel horizontal overflow; final document width equals 320 px. Fixed the detail dialog height: at 320×740 it is bounded to x=16…304 and y=16…724, with the focused Save action visible at y=581…623. |
| Keyboard and recovery | Open/review/save with keyboard, focus restoration to the opening reference, Escape, unsaved-review confirmation, Keep editing, and reachable bottom Close control checked. Loading placeholder, filtered empty state/recovery, missing-key unavailable state and invalid-configuration error with Retry inspected in the browser. Fixtures are conspicuously labelled. |
| Final local state | 8 proposed sources, 0 enabled schedules, 0 active collection jobs; two explicitly synthetic UI records. Local server/queue worker are available for manual checks; recurring collection is off. |

## Not verified / bounded scope

- Live collection was checked for **one** source and one account plan. Broad source reliability, long-running load, provider billing and adversarial remote-browser egress were not proven. The application’s request and estimated-cost budgets do not replace provider-side wallet limits.
- Remote TinyFish network internals and any intermediate/subresource requests are a provider trust boundary. The app pins its own preflight IP, refuses HTTP redirects and rejects any provider final URL differing from the approved URL; it also relies on TinyFish’s documented private-destination protections.
- Cloudflare D1/Queues collection execution, distributed multi-host deployment, a scheduler enabled in production, actual mobile hardware, VoiceOver/TalkBack and formal WCAG conformance were not tested. The collector is for the current Node/SQLite runtime; scheduling remains intentionally disabled.
- Proposals have not received a real platform administrator’s source approval. No clinical, fire-professional or legal review of collected material was performed. International references need local interpretation. Human staff approval cannot certify competence or change a curriculum.
- Change detection identifies textual differences, not their clinical importance. Page restructuring may produce noise; reviewers see excerpts and the original source. It does not search linked publications automatically, run an Agent, or generate AI summaries.
- No production deployment, release APK, GitHub push or recurring task was performed for this request.

## Changed files

New implementation:

- `app/api/safety-updates/route.ts`
- `app/api/safety-updates/sources/route.ts`
- `components/training/SafetyUpdates.tsx`
- `components/training/SafetySources.tsx`
- `lib/safety/model.ts`, `messages.ts`, `url-policy.ts`, `network.ts`, `provider.ts`, `store.ts`, `worker.ts`
- `drizzle/0020_safety_updates.sql`
- `scripts/safety-worker.ts`, `scripts/seed-safety-fixtures.ts`
- `tests/safety-updates.test.ts`, `tests/safety-updates.integration.ts`
- `vite.safety-worker.config.ts`
- `docs/safety-updates-pilot.md`, `docs/safety-updates-verification.md`

Small integration changes:

- `components/training/WorkspaceShell.tsx`: role-scoped navigation entry.
- `components/training/Dashboard.tsx`: lazy-load panel and keep unauthorised deep links out of the section.
- `components/training/GovernancePanel.tsx`: operator access to the global registry without requiring their own centre.
- `app/globals.css`: scoped light-theme styles, narrow layout and bounded review dialog.
- `package.json`: worker build/run commands.
- `.gitignore`, `eslint.config.mjs`: ignore generated worker output.
- `README.md`: link to pilot documentation.

All paths above are relative to `apps/admin-web`. Pre-existing unrelated working-tree changes were preserved. The private local configuration, fixture database, probe evidence, test logs and screenshot are ignored local artifacts, not source or deployment files.

Screenshot: `artifacts/safety-updates/desktop-fixture.jpg` at repository root. This image shows **synthetic UI fixtures**, not live TinyFish output.
