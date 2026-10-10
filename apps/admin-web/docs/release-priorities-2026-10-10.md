# Backend and institution release checkpoint — 10 October 2026

The native app is available as a development preview. The account service remains **local-only and is not deployed**.

- Explicit multi-institution learner selection, tenant-scoped assignment/notification routes and state, and ambiguity rejection for older clients. Private source copies remain account-owned, with deliberate sharing to each institution.
- Compact native institution home, readable assignment/review/observation controls, and a separate legacy industrial-record path. Encrypted legacy transfers now pin institution as well as account/server/profile; old unscoped queues require a deliberate reset of the transfer copy, retaining local evidence.
- Admin-only AI readiness and explicit synthetic controls-only contract test. Configuration status is distinct from a tested connection. No live provider configuration, paid service setup or safety approval is implied.
- Revocable, expiring anonymous formative-record QR sharing, separate from the credential issuer. See [record-sharing contract](formative-record-sharing.md).
- Local configuration inspection with no network calls or secret output. Current local result: account secret/operator configuration present; pilot signing key matches bundled trust; production HTTPS account origin and recovery delivery are not configured; contextual AI is disabled.

Verification:

| Check | Result |
| --- | --- |
| Backend unit / SQLite suite | 94 passed |
| TypeScript, ESLint, production Vinext build | Passed; existing native-config/deprecation build warnings remain |
| Institution real local HTTP suite | Passed, 65+ assertions including multi-institution state isolation and role-denied diagnostics |
| Private learning real local HTTP suite | Passed |
| Public practice QR real local HTTP suite | Passed; synthetic records only |
| Android focused session/queue/upload JVM checks | 21 passed before QR addition |
| Current native QR and account source compile | Root's current APK + AndroidTest build passed |
| Public verification UI | Chrome screenshot inspected; unavailable-link action exercised |
| Signed-in native institution and QR workflow | Passed on the published preview APK (`706425d9…`), API 36 emulator, against the real local service: institution switching/isolation, cancelled consent, QR generation and bitmap decode, anonymous public verification, revocation with correct timestamp and retained private source. Four screenshots retained; corrected timestamp visually inspected. See [native verification details](formative-record-sharing.md#native-verification--10-october-2026). |

Migrations 0018 and 0019 were applied only to the local database after private backups. Earlier migrations 0014–0017 remain prerequisites. No industrial certificate prerequisites changed.

External release blockers remain: reachable HTTPS account service, tested recovery delivery, backup restore/retention and support ownership, production role governance, signed APK/device acceptance, and qualified review of exact safety lessons/translations. Contextual AI additionally needs an operator-selected provider gateway, backend credentials and reviewed disclosure. Local source checks or a synthetic gateway probe do not substitute for these.

The current full dependency audit has eight high findings in development tooling through unpatched braces 3.0.3; production-only audit has zero reported findings. The full release security gate remains blocked. No forced audit remediation, unverified downgrade or waiver was introduced. The upstream advisory currently reports no patched release: [GHSA-vfj7-8cjw-p6xm](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm).
