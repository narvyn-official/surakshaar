# SurakshaAR 0.13.1 verification — 10 October 2026

Development preview, Android 10+, version `0.13.1-dev` / code 28. Debug-signed APK: `surakshaar-0.13.1-dev-debug.apk`.

SHA-256: `e2636826e57353cda516c3ec5da223742a16b54391b9e6b179bf8cffcc48648a`

Bytes: 26,285,643

## Changes and result

Fire inspection now uses native answer buttons, separate model space, accessible click semantics and a minimum 48 dp touch height. Tall screens put the actions below the model; short, wide screens put them beside it. Explicit page controls expose overflow without scrolling. The model is framed within the remaining area. Header Back uses a drawable, and the duplicate instruction action is removed during inspection while Coach remains available.

A failed intermediate 200% landscape check exposed a stale child measurement: actual height 433 pixels instead of the requested 611. Dock sizing now happens during measurement, before child layout. The readiness guard remains enabled. On the final APK the panel measures 735 × 611 pixels and the model region is 801 × 611 pixels. Inspection page counts remain stable through all tested actions.

## Evidence on this exact APK

| Check | Result and boundary |
| --- | --- |
| Android build | Debug app and instrumentation APK compiled successfully. |
| JVM tests | 261 passed across 40 test classes, zero failures/errors/skips. |
| Lint | Zero errors; 53 warnings remain. |
| APK content | Packaged anatomy, curriculum structure, licence notices and digests passed. Balanced body: 117 parts / 248,058 triangles; optional detail archive: 117 parts / 471,068 triangles. This is packaging evidence, not medical validation. |
| Standard portrait | Native Fire interaction test passed, 11.557 seconds. |
| 200% text portrait | Same interaction test passed, 12.320 seconds. |
| 200% text landscape | Same interaction test passed, 15.852 seconds. |
| Visual inspection | Final normal portrait, large-text portrait/landscape, gauge and damaged-unit pages inspected. No overlapping model title; complete reachable answer labels. |

The API 36 emulator uses software graphics, 1080 × 2400 at 420 dpi (rotated for landscape). Each native test checks gauge/damaged-unit inspection and the alternate evacuation scene; answer labels, focus/click semantics, 48 dp minimum targets, full bounds, panel/model separation, pointer-to-action mapping and unchanged saved journals. Mapping tests temporarily intercept and restore the action callback; they do not invent completed learning evidence. PixelCopy confirms a rendered GL scene. Original records are retained and synthetic fixtures removed. Font scale is restored to normal.

Machine-readable reports are `build-checks.json` and `apk-validation.json` in the source documentation. Release asset `validation-evidence.zip` contains the selected automated logs, geometry reports, lint XML and JVM XML. Phone images and credentials are excluded. Failed intermediate evidence remains local and is described in the source's Fire validation report.

## Evidence retained from 0.13.0

First Aid, narration, content definitions, backend, QR and account logic are unchanged by this refinement. Their results apply to their own recorded builds, not a claimed full regression on 0.13.1. The published 0.13.0 APK passed the signed-in native institution/QR workflow, including actual revocation date display, in 6.069 seconds. Earlier candidate tests cover casualty holds, wrong-answer retention, resume, isolation and Audio controls. See the [0.13.0 verification](https://github.com/narvyn-official/surakshaar/blob/main/docs/surakshaar-0.13.0/verification.md).

## Open release gates

- Final-build physical AR placement, tracking interruptions, sustained thermal/memory performance and representative phone acceptance remain unverified. The user's casualty-placement confirmation belongs to a preliminary 0.13.0 candidate.
- Exact lessons and anatomy remain source-checked drafts requiring qualified clinical/fire review. This preview does not establish practical competence or provide statutory certification.
- Santali remains incomplete; Hindi domain review, audible voice quality and complete TalkBack/assistive-technology journeys remain open.
- Production signing, public HTTPS account hosting, recovery delivery, backup restoration and support operations are not accepted. Offline practice is available; online account features need a configured backend.
- AI control assistance and readiness diagnostics exist, but no live AI provider is configured or validated.
- The full web development-dependency audit retains eight high findings through unpatched `braces`; the production-only audit reported zero. The audit gate remains enabled. The first new-repository CI run passed Android and web checks before failing that audit gate.

**Production-ready decision: blocked.** This authorised public preview is for development evaluation. Automated tests, structural asset checks and screen simulation do not remove these gates.
