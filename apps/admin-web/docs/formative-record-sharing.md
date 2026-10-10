# Revocable practice QR records

This feature shares **formative learning metadata**, separately from industrial certificate issuance. It is not a professional certificate, identity check, safety-content approval or practical skill assessment. The exact stored decision sequence is replayed against the supported content version before a completed practice can be shared.

## App flow

Open private saved practice, choose a completed copy, then **Practice QR sharing**. The app states exactly what becomes public and asks the owner to confirm. It creates a seven-day link and displays a QR with an explicit online-verification limitation. The link is shown once and can be deliberately shared using the system chooser. Returning to the management screen lists active links first and supports revocation. Private source records remain unchanged. Previously saved public metadata cannot be recalled from recipients.

An unknown create outcome must be reconciled using **Refresh share links**. Reusing the same request UUID never reveals a second secret link; revoke an unwanted active entry before creating a replacement. At most five links per practice copy and twenty per account can be active. The API permits 1, 7 or 30 days; the native default is seven.

## API and privacy contract

- `GET /api/learning-practice/shares?workerId=…&attemptId=…` requires the owning account and lists at most 50 entries, active first. It never returns a token or token hash.
- `POST /api/learning-practice/shares` accepts `create` with UUID `id`, matching worker/attempt IDs, `expiresInDays`, and literal `confirmPublicMetadata: true`. Client-provided tokens, metadata or certification flags are rejected. Creation returns a relative `/practice-record#token=…` path once.
- `revoke` requires the same account, worker, attempt and share ID. It is idempotent and is not blocked by the creation quota.
- `POST /api/public-learning-record` accepts only `{token}` and is intentionally unauthenticated. Invalid, missing, expired and revoked links expose no record metadata. Active replies contain course title, exercise title, practice mode, exact content version, draft status, sharing/expiry/check timestamps and a non-certifying scope statement. They contain no learner name, email, profile ID, attempt ID, raw answers, score, reflection or institution membership.
- The token contains 32 cryptographically random bytes (256 bits), encoded base64url. Storage contains only a purpose-separated SHA-256 hash. The URL fragment is read locally and removed from the browser address before verification; the token travels to the service in the bounded POST body. Responses are `no-store`, and the page is marked `noindex`/`no-referrer`.
- The share pins an immutable source snapshot digest. Later private reflections do not change its metadata. Account/source deletion cascades to shares; revocation cannot be undone. Old expired/revoked share rows are pruned during the owner's subsequent creation after thirty days. Existing backup retention still needs an operator policy.

## Migration and validation

Migration `0019_learning_record_shares.sql` adds a separate table, source/ownership/quota guards and immutability triggers. It was applied locally only after a private SQLite backup. It does not write any legacy assessment, certification request or credential table. No production migration, hosting or user record publication was performed.

Unit/SQLite checks cover anonymous projection, explicit consent, expiry, revocation, token shape, immutable metadata, account/source ownership, quota and account deletion. `tests/learning-record-share.integration.ts` checks the real local HTTP paths with synthetic accounts and removes its fixtures. The Chrome verification page was visually inspected and its unavailable-link recovery action exercised. The native workflow and generated QR bitmap were exercised against the real local backend as detailed below. Optical scanning with a second physical device remains untested.

The link needs a reachable account service. A localhost development server is not a publicly available verification service, and the QR is not an offline signed certificate. HTTPS hosting and deployment rate limits/operations remain release prerequisites.

## Native verification — 10 October 2026

`InstitutionSharingFlowTest.nativeInstitutionSelectionAndExplicitFormativeQrRevocation` passed on the API 36 emulator in 6.069 seconds, using the published preview APK with installed app SHA-256 `706425d93e82cb11d9d32f29dbdbb0aa4bda33aa76483344423ff84d9d301ec1`. Authentication was established through the normal local sign-in API; the test then operated the actual native panels. This does not claim that the login form itself was tested here.

The test chose each of two synthetic institutions and verified assignment isolation. Cancelling publication created no share. Confirming publication rendered a QR; decoding its bitmap yielded the expected local URL with a fragment token and no query parameters. Public verification returned active, non-certifying metadata without the learner ID, email, attempt ID, answers or reflection. Revoking in the native screen changed public verification to revoked with no record, while the private source remained available. Four rendered screenshots were inspected.

The app's original profile, preferences, attempts and credentials were restored. The private fixture file was removed from the emulator. All three temporary accounts, two centres and associated synthetic records were deleted; counts in six affected backend tables matched the non-fixture baseline afterward. Fixture passwords and cookies were not placed in source, logs or screenshots. The pictured QR is revoked and its synthetic source has been deleted.

An earlier run on APK `27d4500984a1f2a730e811e6a562a03c6d8b5abf2390498a0b4c19df027a6a0e` passed in 6.511 seconds, but screenshot inspection caught a history-label bug: revoked entries displayed their original expiry date without a separator. The published preview now uses the actual revocation timestamp with a separator. The final rerun above asserted the exact displayed revocation time, and its corrected screenshot was inspected.

Final evidence is retained under `artifacts/surakshaar-0.13.0/validation/institution/published-preview/`: `native-flow.txt`, `installed-app.sha256`, and four PNGs. Earlier evidence remains in the parent directory. No deployment, external mail delivery, live AI provider or physical QR camera scan was part of this check.
