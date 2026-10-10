# Build, migration, recovery and release gates

This is version 0.11.0-dev/code 24, a debug-signed local pilot. Preserve existing account/runtime keys, issuer trust and signing identity. Do not run issuer generation against a configured deployment or rotate keys as an upgrade shortcut.

## Reproduce locally

```sh
node scripts/check-content.mjs
bash scripts/build-android.sh lintDebug assembleDebugAndroidTest
cd apps/admin-web
npm run check
cd ../..
node scripts/local-dev.mjs
```

The local launcher refuses a non-local account-service URL, applies local D1 migrations, and uses a loopback development inbox. No external mail or deployment is performed. Build output: `artifacts/surakshaar-0.11.0-dev-debug.apk`. Provider settings are optional and absent from Android. The generic HTTPS classifier contract is `{task, question, candidates, output, instructions}` → strict JSON `{caseId: candidateId|null}`. Runtime configuration and provider privacy/cost review are required before hosting it.

After the local service starts and a named emulator is booted:

```sh
python3 scripts/test-android-matrix.py --serial emulator-5554 --out artifacts/qa-0.11-current
python3 scripts/test-upgrade-local.py --serial emulator-5554 --out artifacts/upgrade-0.11
python3 scripts/benchmark-startup-lab.py --serial emulator-5554 --gpu host --samples 30 --output artifacts/startup-0.11-host.json
node scripts/test-tutor-local.mjs
apps/admin-web/node_modules/.bin/tsx scripts/evaluate-tutor.mts
python3 scripts/ar-device-report.py --serial emulator-5554 --allow-emulator --output artifacts/device-0.11.json
```

The tutor integration script creates disposable synthetic accounts/centre fixtures in the one local Miniflare database, exercises cross-account denial and quotas, then deletes only its UUID-owned fixtures. It never provisions a production operator, reviewer or provider. The Android matrix explicitly sets/reverts emulator font, camera and thermal fixtures. It must not be used to claim physical camera tracking.

Run device tests, upgrade tests and performance sampling sequentially, without builds or other emulator workloads competing with timed gestures. The upgrade script is emulator-only: it downgrades to the retained debug v0.10 APK, seeds its own synthetic learner/procedure, reinstalls v0.11 without clearing storage, verifies those exact bytes and restores the previous learner. Both APKs must share the debug certificate. It does not prove production-signed updates or every stored history type. Startup sampling reports Android's first activity frame, not the proposed usable-Home target. Name the actual emulator GPU configuration; `--gpu` labels the report and does not change the renderer.

From `apps/admin-web`, run `node_modules/.bin/tsx tests/journey.integration.ts` against the same loopback stack for enrolment, course prerequisites, immutable sync, independent correction/review, signed wallet, public privacy and revocation. Its fixtures are synthetic and removed afterward. `tests/reliability.integration.ts` exercises local recovery mail, concurrent replay and session invalidation; no external delivery is used. These checks do not certify a deployed service.

## Additive migrations

Web migrations 0012/0013 add only account quota tables. Apply with the normal D1 migration mechanism after backup/restore rehearsal; **only local application was verified here**. Existing assessments, procedures, review requests, credentials and keys are unchanged. SQLite `skill-practice.db` is a new local schema 1 database, separate from existing learning records. Demonstration/review preferences do not migrate historical evidence into a new content version.

Keep the existing deployment's authorised backup process and installation signing key. Uninstalling/clearing Android storage removes local-only practice; it is not an upgrade or a safe troubleshooting step. Do not discard held sync queues or copy them to another account. Diagnose login/link/server/storage state and retry with the original linked account. Compatible reinstall uses the same application ID/key and `adb install -r` for a debug lab; end users need an appropriately signed update.

Rollback restores a known compatible app/server build while retaining the additive tables. New transfer records remain separate and are not rewritten into old assessments. Never erase event history to make a record pass. An unsupported/corrupt record is retained and must be investigated; versioned restore should fail conservatively. Archived curriculum/procedure versions must remain available.

## Distribution gates

| Gate | Current status / remaining work |
| --- | --- |
| Local debug source/build and automated regression | See current verification report and logs |
| Complete five-domain physical AR learning and real-device demo | Not tested on this build; no physical phone attached |
| Representative Android/device tiers, camera/call recovery, 20-minute AR, 20 AR cycles, thermal/battery/memory | Not tested on physical devices |
| Reviewed safety, realistic equipment cues, machinery/site-specific SOPs | Blocked on competent reviewer and approved procedures |
| Native Hindi review and complete Santali text/voice | Partial / Not tested; Santali is explicitly pending |
| Screen reader, keyboard, small phone, large text and interruption usability with learners | Emulator checks only; participant/device review pending |
| Complete staff permissions, independent issuance and public QR verification | Existing protocols retained and regression scope reported; current deployed end-to-end revalidation required |
| Approved safety retrieval / live provider / explain-back AI | Partial; classifier and offline references implemented, approved corpus/provider missing |
| Production signing, hosted auth/mail/provider, issuer custody and update continuity | Not tested / no authorised production arrangement used |
| Deployed backup/restore, privacy notice, retention/deletion owner and incident recovery | Not tested; requires deployed infrastructure/owner |

A release candidate needs all applicable gates, a named build/hash/device, observed outcomes and expert approvals. Older Samsung/v0.7.1 evidence is historical context only and cannot validate this APK. Debug signing and a successful emulator matrix do not make the pilot release ready.
