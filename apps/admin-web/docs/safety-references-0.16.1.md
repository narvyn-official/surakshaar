# TinyFish public reference integration — 0.16.1-rc.1

The existing manual Fetch collector is packaged as a separate supervised worker. Equipment discovery uses TinyFish Search. Workspace lesson links track the impact of collected reference changes on instructions, AR details and practice questions. Nothing in this feature writes learner answers, certificates, authored lessons or model geometry.

## Use

1. Assign an existing controlled account ID through backend `PLATFORM_OPERATOR_IDS` when the owner is ready. The owner explicitly deferred this assignment on 11 October 2026; do not infer an operator from the first signup or an email string. Privileged access requires MFA. Operators without a centre can access source management and equipment search under Centre approval.
2. Approve an appropriate public source with a reason and recent sign-in. Proposals remain uncollected. Check publisher authority, exact page, rights to retain excerpts, applicable market and model revision; foreign guidance does not establish Indian legal requirements.
3. An approved centre's administrator or trainer uses Safety Updates → Check for updates. The separate worker processes the durable queue; no browser needs to remain open. Manual collection and scheduled collection are separate; recurring checks remain off.
4. Equipment references searches one selected exact manufacturer host using a public make/model. Results are unreviewed. Operators can propose a result but must approve it separately under Sources. There is no automatic expansion of allowed hosts or following of discovered links. Fetch uses default markdown extraction without HTML selectors, supporting both web pages and document download handlers (including PDFs without a `.pdf` extension). Redirect URLs still fail closed.
5. Lesson links select a scenario, step, content version and dependency type (instruction, 3D/AR detail, practice question). Trainers can create links; administrators remove links and record impact decisions. A new live reference version or source revision makes the previous decision stale. Fixture data cannot satisfy a dependency. Curriculum-version changes flag links for replacement. 'Revision needed' remains an action item until a separate reviewed content release and new reference decision; this tool does not perform publication.

## Operational limits and data boundaries

- Search and Fetch share the atomic `safety_budget` ledger and expiring concurrency leases across processes. Deployment starts at five attempted requests per UTC day and one concurrent provider request. Failed attempts count; Search has no retries. Fetch uses one attempt in this deployment. One workspace can request four searches per hour; identical successful searches are reused for one hour. The provider may bill beyond its account-wide free allowance; the local estimated-cost ledger is not a guarantee of free usage or a provider wallet cap. No credits are purchased.
- Only public URLs, a public equipment make/model, selected host and fixed search/extraction options cross the TinyFish boundary. No photos, biometrics, worker records, institutional documents or assessment answers are passed. Queries are not added to audit detail; search history is workspace-private and trimmed after 30 days by the existing hourly maintenance service. Backups use their separate retention period. Do not type personal identifiers in the search box.
- Search returns at most ten validated results from a maximum one-megabyte provider response. Exact-host HTTPS URLs only; credentials, IP literals, queries, fragments, unsafe encoded paths and internal hosts are rejected. Fetch also pins validated public DNS addresses for preflight and rejects every redirect. TinyFish remote infrastructure/subresources remain a provider trust boundary. Text is displayed through escaped React text, never interpreted as instructions or executable HTML.
- Existing authority is rechecked before and after Search; collection rechecks source revision and staff/centre access. Reviewer notes, jobs and lesson links remain tenant-scoped. Mapping decisions use optimistic revisions and exact latest live snapshot checks. No implicit source approval, medical judgement or government endorsement is inferred.
- Two default discovery hosts were inspected on 11 October 2026: [Kidde](https://www.kidde.com/) and [ZOLL](https://www.zoll.com/). These are a configurable initial catalogue, not an endorsement or a complete manufacturer directory. Add further exact hosts deliberately through backend `SAFETY_EQUIPMENT_HOSTS` and, if overridden, `SAFETY_SOURCE_HOSTS` after publisher review.

## Deployment

Run the additive `0023_safety_references.sql` migration against a backed-up database. Build `npm run check` and `npm run release:package`. Packaging requires and includes the worker and explicit diagnostic bundles. Do not build the Next application on the small VPS.

Install `deployment/surakshaar-safety-worker.service` using the same private runtime environment and database as the web service. Paths in the template match this VPS's `/opt/surakshaar/current` layout. Keep `surakshaar-maintain.timer` running for search retention as well as identity expiry. Never put the provider key in browser/mobile config, source, logs or GitHub Actions.

An explicitly invoked backend diagnostic can validate two public requests without approving a source, creating account authority or saving curriculum data:

```sh
node apps/admin-web/dist-safety-worker/check-provider.mjs --live
```

Run only with the private backend environment loaded. It shares the deployed call budget and concurrency lease, prints hashes/counts and safe error codes, and does not print keys or provider payloads. It is not scheduled. A passing diagnostic is distinct from an approved workspace collection.

Rollback: stop the new worker, restore the previous application symlink, restart the web service; keep the additive tables and all later learner data. Preserve the pre-cutover private environment and online database backup. Do not replay an old database over post-deployment records.

## Acceptance evidence

See the dated deployment report for actual live status. Local checks cover Search schema/host filtering, quota errors, shared budget/cache/lease behavior, role and tenant isolation, source-revision and snapshot conflicts, PDF extraction settings, review-only writes and UI recovery. These tests do not establish clinical review, physical-device AR accuracy, worldwide manufacturer coverage or full accessibility conformance.

Current API contracts checked against [TinyFish Search reference](https://docs.tinyfish.ai/search-api/reference) and [TinyFish Fetch reference](https://docs.tinyfish.ai/fetch-api/reference) on 11 October 2026. Search uses a fixed GET endpoint with `include_domains`, India locale and server-only `X-API-Key`; Fetch retains the existing fixed POST endpoint. No Agent or Research model is used.
