# SurakshaAR public account preview — 10 October 2026

The Android account service is reachable at **https://surakshaar.213-210-36-82.sslip.io**. This is a dedicated preview hostname, not an organisation-owned permanent domain. Account creation, sign-in, password change, private formative sync and consent-based QR sharing use the existing application routes. Offline learning does not require an account. A personal account does not create a staff role or institution.

**Email recovery is disabled by the user's explicit choice.** No SMTP credentials were retrieved, no mail provider was configured and no messages were sent. `/api/account-capabilities` reports `passwordRecovery:false`; the native recovery screen says recovery is unavailable, and the reset endpoint fails honestly with 503. Email addresses are not verified identities. Staff access, institution membership and professional certification still need their separate authorised workflows. AI/provider and certificate-issuer secrets were not configured by this deployment.

## Runtime and isolation

- VPS: saved server `213.210.36.82`, inspected `/srv/server-ops/README.md` and live services before deployment. Existing sites remain separate.
- Node **24.13.1 LTS**, production Vinext standalone output, one process. No Wrangler development server, dev inbox or development database is public.
- Dedicated non-login user/group `surakshaar`; service `surakshaar.service`; loopback `127.0.0.1:3197` only. Nginx terminates HTTPS and redirects HTTP. Let's Encrypt certificate `surakshaar-preview` uses existing Certbot renewal scheduling.
- Code `/opt/surakshaar/releases/20261010-accounts-v1`, current symlink `/opt/surakshaar/current`.
- SQLite `/var/lib/surakshaar/accounts.sqlite` (private directory 700, DB 600); WAL, FULL synchronous mode, foreign keys, five-second busy timeout. All 20 real migrations were applied with a hash ledger; changed migration history is rejected.
- Root-only runtime configuration `/etc/surakshaar/runtime.env` (600), generated backend auth secret. No backend secret in Git or APK. `BETTER_AUTH_URL` and `VINEXT_TRUSTED_HOSTS` are restricted to the exact preview origin/host.
- Nginx overwrites `Host`, forwarded host/proto/client addressing and `CF-Connecting-IP`; incoming client-supplied versions are not trusted. `Forwarded` is removed. This is necessary because the existing account API validates same-origin HTTPS requests.
- Systemd restart-on-failure; memory high 256 MiB / max 384 MiB, JS heap 256 MiB, 128 tasks, process hardening and write access confined to the service data directory. Observed service cgroup memory after public checks was approximately 45 MiB. This is a functional smoke measurement, not load capacity evidence.
- Reverse proxy has a 5 MiB body cap and per-IP request limiting; application authentication and record quotas also remain active.

The D1 adapter implements the parameterised SQLite methods used by this application. It is not a universal D1 replacement: omitted positional bindings follow Node SQLite's null behaviour, and SELECT change metadata can reflect the preceding write. Application callers provide their complete binding sets and use write metadata only. Real Drizzle/BetterAuth session mappings, transaction rollback and schema ownership triggers were checked independently. Node's SQLite API still emits an experimental warning in this LTS runtime; this is explicitly a preview service and requires runtime update review.

## Backups, migrations and rollback

`surakshaar-backup.timer` runs daily at 03:35 UTC with up to five minutes jitter and persistent catch-up. It uses SQLite's online backup API, verifies integrity and foreign keys, and retains **14 days of its own scheduled backups** in `/var/backups/surakshaar` (root-only; files 600). Pre-migration backups are separate and are not deleted by scheduled retention. These backups are on the same VPS: off-site disaster recovery and alert delivery are not configured.

Source utilities: `scripts/check-node-runtime.mjs`, `scripts/migrate-node.mjs`, `scripts/backup-node.mjs`. Use the private service environment when invoking them; do not echo its contents. Build on a development machine with `npm ci && npm run build:node`, package only `dist/standalone` plus these scripts and migrations, and stage a new release directory. Never package `.env`, SQLite files or fixture credentials. Before a database update, run the migration utility with the dedicated DB and backup paths; it backs up existing data, validates migration hashes and rolls back a failed individual migration.

For a code rollback, first take a verified backup, confirm the older code supports the current schema, change `/opt/surakshaar/current` to the known compatible release and restart only `surakshaar.service`. There is no automatic downgrade migration. For a database recovery, stop only this service, preserve the current DB plus WAL/SHM files as a private incident copy, restore a verified backup with service ownership/600 permissions, then restart and check sessions/data. Restore rolls back changes after that backup; never do it over active writes or without a deliberate recovery decision. To retire the preview, stop its service/timer and remove only its nginx enabled link, run `nginx -t`, reload nginx, and retain private data/backups until the owner chooses a retention action. Do not touch other VPS sites.

## Evidence

- Node standalone production build passed. Runtime bundle: approximately 48 MiB before compression. No native macOS binaries in the deployed package.
- Six adapter/review tests passed: real migrations, BetterAuth/Drizzle session read and isolated revocation, account cascade, owner-bound records, atomic multi-statement rollback, migration history validation and failed-migration rollback.
- Public HTTPS smoke passed: two disposable real accounts, signup/re-login, private Fire practice persistence, 403 cross-account ownership denial, no automatic admin access, consent-based public QR metadata, anonymous verify/revoke, private retention, foreign-origin rejection and independent session logout.
- Restart proof passed: both fixture sessions and private record remained usable after `systemctl restart surakshaar.service`.
- Actual private scheduled backup passed integrity and foreign-key checks. A separate restored copy retained both fixture accounts and the private practice record, then the temporary restored copy was removed.
- Native public account suite passed 2/2 (6.9 seconds) on APK SHA256 `0a2877b11d9424232d36d6733ec56b0e208808053b490185929c23b346db19c8`: actual HTTPS sign-in, password change, sign-out, disabled recovery, validation/reveal/autofill, recreation without password retention, Back, and unchanged offline practice. Native fixture removed and original preferences/language restored.
- `nginx -t` passed. Existing unrelated TLS-option warnings in other site files were left unchanged.

Synthetic accounts are scoped by private fixture IDs and exact `qa-…@example.test` addresses; cleanup checks both before deletion. Credentials and session cookies remain in private temporary files only. No production users are selected by broad prefix deletion.

## Native session migration

New APKs default to this HTTPS preview service. An explicitly configured institution server remains selected. Persisted cookies are encrypted together with an exact origin and version. Old unbound jars and mismatched origin envelopes are removed and require a fresh sign-in; they are never forwarded to the new host. Process-only cookies are also origin-scoped. Three focused tests cover legacy/default migration, scheme/port/host separation and malformed-envelope rejection.

Final cleanup removed exactly the two disposable public QA accounts, verified their private practice/share children were gone, and passed DB integrity/foreign-key checks. The private host fixture was deleted; a fresh post-cleanup scheduled backup succeeded. Older private backups retain their snapshot history under the documented retention policy.
