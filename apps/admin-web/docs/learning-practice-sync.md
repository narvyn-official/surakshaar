# Private formative learning sync

`/api/learning-practice` is a separate authenticated endpoint for the Fire / First Aid launch catalogue. It does not require a training-centre membership. Its records are never read by the legacy industrial certificate issuers, and completing them does not award a practical or clinical qualification.

Apply additive migration `0014_learning_practice.sql` locally after backing up the development database. Production migration/publication is a separate deployment action. The new tables are `learning_profile_accounts`, `learning_practice_heads`, `learning_practice_snapshots` and `learning_sync_budget`; the migration does not rewrite historic training or credentials.

## Android contract

With the normal Suraksha account session:

```json
{
  "schemaVersion": 1,
  "workerId": "the-local-profile-uuid",
  "attempts": ["raw LearningAttempt JSON objects, not strings"]
}
```

`POST /api/learning-practice` accepts 1–20 attempts per request, at most 900,000 UTF-8 bytes. Records must use the current draft2 catalogue or the exact packaged draft1 archive, exact scenario/course, UUID identity and `evidenceType: formative-decision-practice`. Per-record bounds are 1,000 events and 1,000 characters per optional teach-back. Records more than five minutes ahead of the server are rejected with a clock error. Review status is currently draft.

The server deterministically replays choices, original correctness, hint counts, required anatomical observations, continuation order and post-completion reflections, and verifies the resulting index/state/time. Client scores, certification flags, owner fields and arbitrary new properties are rejected. Anatomy observation is self-reported app evidence, not a sensor attestation or practical-skill measurement.

A successful response is:

```json
{
  "schemaVersion": 1,
  "evidenceType": "formative-decision-practice",
  "certifiable": false,
  "accepted": [{"id":"attempt-uuid","digest":"server-sha256","updatedAt":0}]
}
```

Identical current snapshots are idempotent. Later snapshots must retain identity, course/version, guided mode and the exact original event prefix. Existing snapshots are immutable; a digest parent guard rejects a competing writer that read an older head. D1 `batch()` rolls back the whole upload if any snapshot fails. A 409 response means keep local evidence and resolve the conflict; do not truncate events or silently replace either history.

`GET /api/learning-practice?workerId=<uuid>&limit=10` returns private `records[]` and `nextCursor: {beforeAt,beforeId}` or null. To continue, provide both cursor fields as query parameters. Default and maximum page size is **10**, bounding response size for the Android account client. No queries allow an arbitrary account/owner ID. Responses are not browser-cached.

## Ownership and consent

The first successful upload binds a local profile UUID to one account. Another account cannot read or upload that profile's records. Existing centre links to another account also block a claim. A UUID and an imported event stream do not establish real-world identity; this rule prevents account-switch merging, not identity verification or hardware attestation. There is no profile-reassignment/recovery UI in this change; use a separate profile for a different account rather than silently moving evidence.

Native sync should be an explicit user action with a disclosure that event history **and any optional teach-back text** will leave the device. Bind the request to account identity/server/profile, stop when these change and ignore stale callbacks. Signing into a new account is not consent to upload another local profile's evidence. Synced reflection text remains private to the account; no automatic educator or institution sharing is introduced. Institutional sharing is now a separate explicit workflow described in [learning centres](learning-centres.md). Private upload alone never shares records with a centre. These records are never sent to the AI coach.

The server retains immutable snapshots for the account until account deletion under the existing account-management policy. Foreign keys cascade deletion to profile bindings, heads, snapshots and sync quotas. The native saved-copy panel offers paginated read-only retrieval and explicit export, including private reflections. It does not overwrite local records, restore an old snapshot into an active attempt, or provide individual-record deletion. Release data-management flows must describe the actual retention and available recovery. Backups have their own retention and are not claimed to disappear instantly.

Limits: 20 uploads per account per minute, 200,000 UTF-8 bytes per snapshot, 10,000 snapshots and 20,000,000 snapshot bytes per account. Storage-limit failures preserve earlier records. Minute quota rows are pruned on later POST traffic after an hour. No successful sync resets practical or certification prerequisites.

## Validation

Unit tests replay all current scenarios and reject forged results, wrong profiles/course/version, skipped steps, wrong body parts, missing required observations, altered original histories and oversized reflections. SQLite tests cover account binding, duplicate uploads, immutable snapshots, stale-parent writers and rollback. The integration test exercises independent accounts, authentication, cross-account denial, correct state replay, private append, pagination, certificate-table separation and contextual coach gates against the local service.

The transaction design follows the [D1 batch contract](https://developers.cloudflare.com/d1/worker-api/d1-database/#batch), checked 10 October 2026. Local tests are not evidence of production deployment, live AI availability or verified Android-to-server delivery on a physical phone.
