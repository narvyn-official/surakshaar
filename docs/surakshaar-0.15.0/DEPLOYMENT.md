# SurakshaAR 0.15.0-rc.1 · deployment guide

Spatial Studio light is the selected design. This is a locally built, signed release candidate for evaluation and deployment preparation. It has not been uploaded to a store, published on GitHub, or deployed to the existing preview server. Safety and translation content remains explicitly draft. Read VERIFICATION.md before inviting real learners.

## Deliverables

- `surakshaar-0.15.0-rc.1-release.apk`: Android 10–16, package `com.narvyn.suraksha`, version code 32. Signed with a persistent local release key, not the Android debug key.
- `surakshaar-0.15.0-rc.1-web.tar.gz`: Next.js standalone server, static/model assets, migrations, operations scripts, deployment templates and rebuildable web source.
- `SHA256SUMS.txt`, `android-signature.txt`, `BUILD.json`, third-party notices and screenshots.

The archive’s prebuilt web runtime was assembled on macOS ARM64. It was exercised locally with Node 24.20.0. For Linux deployment, rebuild from the included source on Linux (recommended) or use the included Docker recipe. The Docker daemon was unavailable here, so a Linux container build is not represented as tested. Use Node 24 LTS, at least 24.13.1. A single Node process owns a local persistent SQLite database. Do not put SQLite on an ephemeral/serverless filesystem, a shared network volume, or multiple web replicas.

## Configure the service

1. Extract the archive into a versioned release directory. Keep a previous working release for rollback. On Linux, rebuild from `source/apps/admin-web` with `npm ci`, `npm test`, `npm run lint`, `npm run build`. Copy `.next/standalone`, `.next/static` and `public` into the same layout as `web/`. Alternatively build with `docker build -f web/deployment/Dockerfile -t surakshaar:0.15.0-rc.1 .` from the extracted directory.
2. Create a dedicated unprivileged `surakshaar` service account. Create `/var/lib/surakshaar` and `/etc/surakshaar` accessible only to that service and the administrator. Never serve these directories through the web server.
3. Copy `web/deployment/production.env.example` to `/etc/surakshaar/production.env`, mode 0600. Supply the real dedicated HTTPS origin and a freshly generated account secret. Keep `REQUIRE_STAFF_MFA=true`. The bundled example is not a working production configuration.
4. Configure platform operator account IDs after those accounts have registered and enabled authenticator MFA at `/account`. A registered account receives no institutional authority automatically. Operators approve institutions; institutional admins enrol learners and appoint trainers/certifiers. A separate certifier reviews and issues the supported simulation credentials.
5. For certificate issuance, provide the existing authorised issuer private JWK matching the public trust bundled in web and Android. Do not generate a replacement key without an issuer trust/rotation migration and rebuilding both clients. No private issuer key is in this release.
6. Configure the HTTPS recovery mail gateway and API key only after sender ownership and delivery have been verified. The app reports recovery as unavailable until a gateway is configured. No mail provider was activated during this work.
7. Optional institutional identity capture requires `IDENTITY_EVIDENCE_KEY`, a base64-encoded 32-byte random key, retained securely alongside backups. Enable the centre policy at `/identity-review` only after publishing its notice, support contact, retention period and supervised alternative. Capture is disabled without this key and an enabled policy. Personal learning remains unmonitored.

Migrations, from the extracted `web` directory:

```sh
node --env-file=/etc/surakshaar/production.env scripts/migrate-node.mjs drizzle
node --env-file=/etc/surakshaar/production.env deployment/validate-runtime.mjs
node --env-file=/etc/surakshaar/production.env apps/admin-web/server.js
```

An existing database requires `SURAKSHA_BACKUP_DIR`; the migration runner makes a private online backup, checks migration digests, runs each migration in a transaction and checks integrity/foreign keys. Do not copy a live SQLite database with an ordinary file copy.

The `surakshaar.service` and Caddy templates assume `/opt/surakshaar/web` and `/usr/bin/node`; adapt those paths to the host. Bind Node to loopback behind HTTPS. Set the canonical origin in both the reverse proxy and `BETTER_AUTH_URL`. The proxy must overwrite `X-Real-IP` with the actual peer address, remove untrusted inbound identity headers, and be the only public ingress. Never expose a development server. Check `/api/health` through the final HTTPS domain, sign in, enable staff MFA, and verify account recovery before public launch.

## Android distribution and connection

Install the signed APK on a supported Android device. This new release key cannot replace a previously debug-signed app with the same package name. Export important local practice first; do not uninstall an old app without preserving its records. Existing signed releases must use the same release key for updates.

The APK intentionally has no default external account server. Open Account & training centre, choose the account-service address and enter the institution’s HTTPS origin. Personal lessons, 3D and supported native AR remain usable without an account. The institutional monitored-practice link opens the browser and requires its own secure sign-in; native AR and front-camera identity capture are not run simultaneously.

Future signed builds:

```sh
python3 scripts/build-release-android.py
```

The script retains signing configuration and keystore under `.runtime/android-release-signing/`, outside distributable artifacts, with private permissions. Back up that directory in encrypted, access-controlled storage. Do not commit it, email it, or upload it with the APK. Losing this key prevents ordinary signed updates. The public certificate fingerprint is in `android-signature.txt`.

To bind a future APK to a known service, set `SURAKSHA_API_ORIGIN=https://your.actual.domain` before building. Existing users can still select their approved centre server; switching servers invalidates the old native session.

## Data maintenance and recovery

Run these through the host’s service scheduler with the private environment file (no automation was installed by this task):

```sh
# Daily verified online backup; scheduled copies retained 14 days.
node --env-file=/etc/surakshaar/production.env scripts/backup-node.mjs
# At least hourly, and immediately after restoring a backup.
node --env-file=/etc/surakshaar/production.env scripts/maintain-node.mjs
```

Identity sessions expire after their centre’s configured 1–30 days. Maintenance and health checks delete expired sessions and their encrypted images from active storage. Withdrawal removes active images immediately. Backups may retain encrypted historical copies for the backup retention window; disclose this, restrict restore access, and reapply expiry/withdrawal controls before reopening a restored service. Backup directories require protected, preferably encrypted off-host copies; no off-host copy was configured here.

For restore: stop the service, preserve the current database and its WAL state safely, restore a verified online-backup snapshot into a new private path, run integrity and foreign-key checks, run maintenance and required migrations, then start an isolated instance. Confirm authenticated learner membership, institutional scoping and credential verification before switching traffic. Restore the matching encryption and issuer keys securely. Application rollback must use a database schema compatible with that release; do not blindly run old code against new migrations.

## Actual feature boundary

Implemented now: guest and account learning; English/Hindi scenario content; responsive Spatial Studio; 20 formative scenarios; 3D equipment and 116 internal anatomy structures; native offline/AR workflows; saved/resumed practice; export, explicit account backup and restore; institution-scoped enrolment/assignment/review; independent legacy simulation certification and verification; disclosed adult identity sessions; encrypted images, timestamped examiner review, independent appeals, withdrawal and expiry; default staff MFA enforcement; migrations and signed packaging.

Institutional monitored practice completes the current assigned scenario on the server and shares its formative evidence with the centre. It never promotes draft first-aid/fire content into a qualification. Photos are sampled presence evidence, not liveness proof. Only an examiner with a trusted enrolment reference or attended check can record an identity decision. Automated facial recognition is disabled. Missing evidence is not an automatic misconduct finding.

Not shipped as completed features: Google/OTP/enterprise SSO, automated biometric matching, native background capture, concurrent front/rear capture, automated subscriptions/payments, PostgreSQL clustering, a reviewed full specialist curriculum, or production WebXR across browsers. The initial business model is institution-managed pilot/service onboarding; no money is collected by this release. These are explicit scope decisions, not hidden placeholder buttons. Native provides AR; the browser provides 3D learning and administration.

Public launch still requires the operator’s final domain/hosting, sender delivery, issuer and operator setup, institutional privacy/support ownership, qualified clinical/fire/translation approval, and sustained supported-phone AR testing. The software cannot supply those approvals on the operator’s behalf.
