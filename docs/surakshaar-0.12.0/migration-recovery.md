# Migration and recovery contract

The new two-course catalogue is a separate formative learning track. It must not rewrite industrial assessment history or reinterpret an older signed certificate using newer content.

## Data boundaries to preserve

| Data | Intended treatment | Required verification |
|---|---|---|
| Existing account identity and memberships | Reuse existing authentication; local learning context does not grant a staff role. | Login/recovery and two-tenant negative checks |
| Historic fire/gas/machinery/PPE/emergency records | Keep original IDs, content versions, action evidence and certificate logic. | Before/after record count and content hashes in upgrade fixture |
| New launch practice | Keep `contentVersion`, `scenarioId`, `workerId`, assistance and original decision events; do not feed it to industrial certification. | Deterministic restore rejects wrong identity/version or edited correctness |
| Anatomy selection/camera | Selection is navigation state; model coordinates are not real patient data. Local AR anchors belong only to their camera session. | Resume keeps scenario stage but requests new placement after session recreation |
| Anatomy pack | Manifest version/hash identifies actual shipped bytes. Missing/incompatible geometry produces a recoverable unavailable state. | Corrupt/missing asset tests and honest 3D fallback |
| Local learning context | School/college/workplace changes examples and age-appropriate choices, not backend permissions or casualty age. | School/worker context switch and permissions checks |

## Upgrade rehearsal before release

Use an emulator snapshot or a designated test device with a copy of representative synthetic records. Do not erase the user's phone or extract real private records into a report.

1. Record baseline app version, signing-certificate digest, account/profile identifiers, counts of each local record type and an offline pending-sync fixture. Preserve the baseline APK and test snapshot.
2. Create one completed industrial assessment and one interrupted legacy practice. Include one signed synthetic certificate with its original content version. Add a second profile and a separate institution identity.
3. Install the candidate as an upgrade using the same authorised signing identity, without clearing data or uninstalling. A signing mismatch is a failed upgrade path, not a reason to wipe the phone.
4. Verify legacy library routes, original assessment events, certificate verification, profile isolation and pending-sync ownership. Count comparisons alone are insufficient; compare known IDs and immutable field values.
5. Complete part of a new first-aid scenario, pause/kill/restart, and verify the same stage, exact content version and original decisions. Repeat with a wrong answer plus hint so restoration cannot silently erase mistakes.
6. Switch profiles while a model is loading and while a record is being saved. No result may appear in the other profile. Verify a deleted or incompatible scenario is reported as historical/unavailable rather than graded under a replacement version.
7. In camera AR, background the app, deny/regrant permission, and recreate the process. Preserve learning history but never persist a local anchor as if it survived a new AR session.

## Recovery rules

- Do not use `adb uninstall`, `pm clear` or destructive database resets on a connected user's device to make tests pass.
- Do not change certificate issuer keys or overwrite a signed credential during a catalogue update.
- Interrupted practice may resume only after record ownership, content version and event replay agree. If validation fails, retain the record as an error/historical item and offer a new attempt; do not silently repair its correctness.
- A corrupt anatomy asset should disable that model with a clear retry/reinstall path. It must not present a substitute mesh as the same reviewed model.
- Rollback is not an assumption that an older APK can read newer databases. Validate backward compatibility or restore the designated synthetic test snapshot. Production rollback needs a reviewed data-preservation plan.
- No deployment, publication, key rotation, paid service or asset purchase is authorised by this document.

Status: this is the migration procedure. Execution evidence belongs in `verification.md`; no successful real-device upgrade is claimed here.
