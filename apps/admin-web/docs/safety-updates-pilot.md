# Safety Updates pilot

Local implementation, 10 October 2026. This feature is for **staff reference**, not curriculum publication or clinical approval. No deployment or recurring collection was enabled for this delivery. Learner, assessment, translation and certification paths are unchanged.

## Staff workflow

1. An approved centre’s trainer or administrator opens **Safety Updates**.
2. A configured **platform operator** reviews the global source registry. All ten shipped sources start as **Proposed · not collected**. Operators without a centre can manage sources under **Centre approval → Manage global Safety Updates sources**. Source decisions require a sign-in less than 15 minutes old and a recorded reason.
3. **Check for updates** queues a durable job and returns immediately. A separate Node worker processes it. Repeated submissions reuse the centre’s active job; cancellation stops further local acceptance. A request already sent to TinyFish may finish remotely.
4. Initial content is labelled **Initial reference**, not a newly published recommendation. Later wording changes create new entries. Staff can read the original page, inspect excerpts, provenance, retrieval time and publication date when supplied.
5. Trainers can record **New** or **Needs review**. Administrators can **Approve for staff reference**, **Reject**, or reopen a final decision. Notes are required. Optimistic revision checks reject stale decisions; history remains in the centre’s audit log.
6. Approval never edits lessons, translations, assessment answers, content versions or certificates. Separate institutions can reach different decisions about the same public reference.

## Architecture and boundaries

- `components/training/SafetyUpdates.tsx`: lazy-loaded review queue, filters, pagination, detail dialog, unsaved-draft protection, progress, errors, review history and disabled scheduling configuration.
- `components/training/SafetySources.tsx`: proposed/approved/disabled global source registry and operator audit.
- `app/api/safety-updates`: authenticated, same-origin JSON endpoints. Existing `access()` establishes centre approval, active membership and workspace cookie scope. Viewer/certifier/learner accounts have no Safety Updates access. A workspace admin cannot grant themselves platform source-management rights.
- `drizzle/0020_safety_updates.sql`: additive public source/version, workspace review/job, budget, lease and scheduling tables. It neither rewrites earlier migrations nor touches learning/certification data. Public snapshots are immutable; centre reviews use revision guards and transactional audit inserts.
- `lib/safety/worker.ts`: persisted work items, expiring leases, one active job per centre, source-level serialization, bounded retries, partial success and restart recovery. Centre/actor access and source approval are rechecked before storing results. Recent successful public collections are reused across centres for five minutes.
- `lib/safety/provider.ts`: fixed TinyFish **Fetch** endpoint, bounded JSON parsing, metadata validation, plaintext extraction and content hashes. No Agent, browser credentials, form submission, private uploads or additional summary model is used. These known pages do not need navigation. Links discovered inside a page are never followed or added to the allowlist automatically.
- `lib/safety/network.ts`: DNS checks and IP-pinned HTTPS preflight; every HTTP redirect is refused. Provider results must retain the exact approved URL. No user data is added to the provider request: only the public URL and fixed extraction settings.
- `scripts/safety-worker.ts`: separate Node/SQLite process. It can keep processing manually queued jobs while **recurring collection is off**. No request handler starts untracked background promises.

The collector targets the project’s current **Node/SQLite account runtime**. The existing Cloudflare web/API build still compiles; a D1/Queues collector deployment has not been implemented or validated. Do not start a Node worker against a different database than the account service. There is no deployment change in this pilot.

## Proposed sources

All are proposals, not endorsements, clinical sign-off, Indian legal guidance or automatic curriculum updates. International emergency numbers, fire classifications and regulatory requirements need local interpretation. No maintained structured API/feed for the particular selected reference pages was established during this research; the pilot fetches known public pages. Prefer a separately reviewed official API/feed if one later covers the same material.

| Proposed page | Publisher | Topic / review considerations |
|---|---|---|
| [Preparedness dos and don’ts](https://sachet.ndma.gov.in/DosDont) | NDMA, India | Fire preparedness within a broader hazard page; inspect extraction relevance. |
| [Choosing and using fire extinguishers](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/index.html) | U.S. Fire Administration | Selection and use; US classifications and emergency numbers differ. |
| [Smoke alarms](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/smoke-alarms/) | U.S. Fire Administration | Detection/maintenance; local codes take precedence. |
| [Portable extinguishers: basics](https://www.osha.gov/etools/evacuation-plans-procedures/emergency-standards/portable-extinguishers/about) | OSHA | Workplace reference; US rules are not Indian law. |
| [First aid steps](https://www.redcross.org/take-a-class/first-aid/performing-first-aid/first-aid-steps) | American Red Cross | First-aid sequence; review local applicability. |
| [CPR](https://www.sja.org.uk/first-aid-advice/cpr/) | St John Ambulance | Staff reference; separate practical and clinical review required. |
| [Recovery position](https://www.sja.org.uk/first-aid-advice/recovery-position/) | St John Ambulance | Unresponsive casualty reference, subject to qualified review. |
| [Burns and scalds](https://www.nhs.uk/conditions/burns-and-scalds/) | NHS | Cooling, stuck clothing and loose covering; UK emergency pathways require local interpretation. |
| [Cuts and grazes](https://www.nhs.uk/conditions/cuts-and-grazes/) | NHS | Wound care and embedded-object exceptions; distinguish minor wounds from severe bleeding. |
| [Guideline highlights and focused updates](https://cpr.heart.org/en/resuscitation-science/guidelines-updates-and-highlights) | American Heart Association | Publication index; linked documents need their own approved source entry. |

Public preflight succeeded for six original pages and, in the scenario follow-up, both new NHS pages (eight of the ten proposals). OSHA and AHA rejected the earlier preflight request; they remain proposals and are not presented as successfully collected. Preflight is an application network check, not a TinyFish collection. The pilot does not weaken its checks to bypass failures.

## Provider contract verified

Official references inspected: [TinyFish overview](https://docs.tinyfish.ai/), [Fetch reference](https://docs.tinyfish.ai/fetch-api/reference), [authentication](https://docs.tinyfish.ai/authentication). The adapter sends `POST https://api.fetch.tinyfish.ai` with backend `X-API-Key`, one approved URL, markdown, no links/images/iframes, `ttl: 0` and a bounded per-URL timeout. It validates `results`/`errors`, the original/final URLs, format and bounded text. It handles both HTTP failures and per-URL errors in successful HTTP responses.

The application's preflight rejects redirects rather than automatically approving canonical alternatives. A changed canonical URL needs a new source proposal. TinyFish separately documents private-destination and unsafe-redirect rejection. Its remote browser, DNS, intermediate redirects and subresource egress remain a provider trust boundary; the app cannot independently inspect every network request inside that service. The live probe did not validate that provider infrastructure. Untrusted text is rendered as plain React text and is never interpreted as a task, executable HTML or a link collection.

Meaningful-change detection is deterministic, not an AI clinical judgement: normalize line endings, repeated whitespace and presentation markers; exclude page chrome; hash the title, normalized text and available publication date. Small wording/number/negation changes are retained. Returning to earlier wording creates a new change; consecutive identical results are deduplicated. Excerpts are partial, and reviewers must consult the source. No AI summaries are generated.

## Backend setup (no secret values in this document)

Use an ignored, private backend environment file or the deployment secret manager. Do **not** add provider credentials to Android, a `NEXT_PUBLIC_`/`VITE_` variable, a web form, a URL, git or screenshots. The supplied key was placed only in the ignored local `.env.safety-pilot.local`, mode `0600`. It was not copied to the VPS.

Required existing account settings: `SURAKSHA_RUNTIME=node`, absolute private `SURAKSHA_DB_PATH`, `BETTER_AUTH_URL`, `BETTER_AUTH_SECRET`; set `SURAKSHA_BACKUP_DIR` before migrating an existing database. `PLATFORM_OPERATOR_IDS` is the existing explicit list of accounts allowed to manage global sources.

| Setting | Default / bounds |
|---|---|
| `TINYFISH_API_KEY` | Required for collection; no default. Missing key gives a clear unavailable state. |
| `SAFETY_SOURCE_HOSTS` | Exact hosts for the ten proposed pages; comma-separated override. No wildcard trust or private hosts. |
| `SAFETY_DAILY_CALL_LIMIT` | 20, configurable 0–1000 globally per database/UTC day; local preview uses 5. Every attempted request reserves a slot, including retries and failed preflights. |
| `SAFETY_FETCH_COST_MICROS` | 0, configurable 0–10,000,000; administrator-supplied estimated upper cost per attempt. |
| `SAFETY_DAILY_COST_MICROS` | 0, configurable 0–100,000,000; atomic local estimated-cost limit. |
| `SAFETY_CONCURRENCY` | 1, configurable 1–3 across worker processes sharing this database. |
| `SAFETY_TIMEOUT_MS` | 35,000, configurable 1,000–90,000; separate 5-second DNS and 10-second preflight limits. Provider client allows 5 seconds additional response overhead. |
| `SAFETY_MAX_ATTEMPTS` | 2, configurable 1–3, with persisted backoff and bounded `Retry-After`. |
| `SAFETY_SCHEDULE_ENABLED` | **false**. Enabling it also requires an explicitly enabled per-workspace schedule row. The pilot UI can only save an interval and disable the row. |

The cost ledger is an application limit based on configured cost assumptions, **not** authoritative TinyFish billing. Official materials describe a free allowance and quota/wallet outcomes; verify the account’s plan and set a provider-side wallet/spend cap before broader use. The application stops on HTTP 402 and never purchases credits. The request limit remains active when estimated price is zero.

From `apps/admin-web`, with dependencies installed and a Node 24.13.1+ runtime:

```sh
node --env-file=.env.safety-pilot.local scripts/migrate-node.mjs
npm run build:node
npm run build:safety-worker
PORT=5390 HOST=127.0.0.1 node --env-file=.env.safety-pilot.local dist/standalone/server.js
```

In a separate terminal (same private configuration/database):

```sh
node --env-file=.env.safety-pilot.local dist-safety-worker/worker.mjs
```

`--once` performs at most one attempt per configured concurrency slot, then exits. Normal worker execution polls **already queued** work; it does not opt institutions into scheduled collection. Back up the SQLite database as part of the existing account-service backup policy. Graceful shutdown waits for bounded in-flight work; crashed leases expire after three minutes. Provider errors are stored as fixed safe codes, never raw bodies or headers.

## Verification and local fixtures

The fixture script refuses to run without `SURAKSHA_SAFETY_FIXTURES=true`, a localhost account origin and a database inside `/.runtime/safety-pilot/`. It creates test-only accounts and explicitly labelled synthetic content; it never calls TinyFish. Do not run it against a real account database. The HTTP suite requires this same isolated environment, uses real sign-in cookies, honours rate limits, and restores its synthetic source decisions after testing.

```sh
SURAKSHA_SAFETY_FIXTURES=true node --env-file=.env.safety-pilot.local --import tsx scripts/seed-safety-fixtures.ts --with-updates
node --env-file=.env.safety-pilot.local --import tsx tests/safety-updates.integration.ts
npm test
npx tsc --noEmit
npm run lint
```

One authorised **live TinyFish Fetch** probe succeeded on 10 October 2026 for the USFA extinguisher page: validated title and final URL, 4,270 normalized characters, SHA-256 `7351d53da9b6f4579fb14ad88955452db8276486208be96b4050d3fa8c57ebad`. Publication date was absent and remains null. This probe did not approve a source or create a human review. UI demonstration records remain labelled **LOCAL FIXTURE · not live**.

Detailed final test evidence and limitations are in `safety-updates-verification.md`. No production deployment, source approval, recurring job activation, clinical approval or curriculum change is implied by this pilot.

## Scenario visual follow-up

Migration `0021_safety_first_aid_sources.sql` adds NHS burns/scalds and cuts/grazes as **proposed** sources without changing earlier approval decisions. The NHS host is allowlisted; each exact page still needs platform approval. The dashboard now separates actual TinyFish reference counts and last successful collection time from synthetic demo counts. The Content filter and review-status counts use the same origin filter on the server, keeping pagination and workspace review decisions consistent. A configured key alone is not shown as successful live collection. Whitespace-only keys are treated as absent.

Official Fetch documentation was rechecked for this change. Known pages continue to use Fetch; no Agent, new model provider, AI summaries or automatic curriculum changes were added. Scheduling stays disabled.
