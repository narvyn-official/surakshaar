# Native accounts and hosted preview

The APK uses `https://surakshaar.213-210-36-82.sslip.io` by default. A previously chosen institution address stays configured. The service provides genuine Suraksha accounts independently of institution enrolment; no ChatGPT login and no automatic staff privileges. Offline practice remains available before sign-in and after sign-out.

Email recovery is deliberately disabled by the owner. No SMTP credentials were retrieved and no email was sent. Account email addresses are not proof of identity. Keep a password-manager copy; the app does not pretend that an unavailable reset link was sent.

Native changes include masked password fields, controlled reveal/hide, password-manager/autofill hints, no password state in recreation bundles, neutral sign-in errors, retry/connection controls, explicit pending state, password change with other-session revocation and one-step Back behavior. Saved cookies are encrypted with an exact origin/version; older unbound session jars require re-authentication, protecting the migration from localhost to public HTTPS.

The [deployment record](../../apps/admin-web/docs/vps-account-preview.md) covers Node/SQLite isolation, TLS, resource limits, backup verification and rollback. The standalone service, real migrations, account isolation and public QR operations passed focused checks. A service restart retained sessions and private records; a private online backup was reopened separately and validated. Local backup retention is 14 days, with no off-site backup/alerting configured. The domain is a preview hostname.

Native public account tests passed 2/2 in 6.9 seconds on final APK `0a2877b11d9424232d36d6733ec56b0e208808053b490185929c23b346db19c8`. They exercised real HTTPS sign-in/password change/sign-out, truthful recovery-disabled state, input validation/reveal/autofill, recreation without password retention, Back and unchanged offline practice. Evidence is under `artifacts/surakshaar-0.14.0/validation/accounts`. Native/host fixtures were removed; exactly the two synthetic VPS accounts and their practice/share children were deleted. The private fixture credential scan found no repository matches.
