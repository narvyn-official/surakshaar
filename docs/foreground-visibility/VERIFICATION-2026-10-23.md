# 0.21.0 development preview — verification

This preview implements foreground visibility, hand-controlled authored teaching scenes and a shared tactile interface. It is not a declaration of production readiness or completion of physical AR/headset acceptance.

## Changes

- Real on-device person segmentation draws camera foreground over virtual models. The exact crop/rotation map is retained; masks expire after 180 ms and cannot cross a session/geometry generation.
- Missing thumb/index tips or palm anchors trigger automatic see-through assistance. The eye control enables it explicitly. This is foreground priority and transparency assistance, not measured depth or pixel-perfect hand segmentation.
- The current authored lesson opens directly in Hands AR. Bounded Previous/Next covers 20 scenarios and 94 mapped steps, with the exact fire variants and changing casualty props. Water/breathing/CPR teaching illustrations animate; attached material remains present in the relevant burn steps.
- Camera and virtual practice views share one ARCore camera and hand-input owner. The Cardboard camera route remains monoscopic; the separate stereo VR player is preserved.
- A short pinch/tap can inspect a model part; dragging moves it. Two pinches resize/turn; open palm cancels. Existing touch and viewer-button alternatives remain.
- Native surfaces now share cached raised/inset neumorphic states, semantic colours, visible focus and 48 dp targets. Home no longer repeats viewing-mode launchers. AR preparation is one setup screen at normal portrait and landscape sizes; larger text retains explicit paging.
- Web login and dashboard CSS use the same palette, without route/authentication/backend changes. Fire equipment materials/nozzle detail, beveled AED controls and first-aid props were refined. AED meshes/draws decrease from 38 to 12; teaching-prop triangles decrease from 29,632 to 20,770.
- Authored English lesson text is explicitly offered with “Start lesson in English”; the app language preference stays unchanged. Existing Hindi anatomy descriptions and first-aid captions use Hindi narration.

## Verified

| Check | Result and evidence |
|---|---|
| Android build and lint | Debug APK, test APK, release APK and lint pass. Three inherited, exact SDK QR-scanner findings remain in the existing lint baseline; existing warnings are not represented as new clean findings. |
| JVM behavior | **405 tests, zero failures/errors/skips**. Includes gesture/drag constraints, foreground geometry/expiry/critical fingertips and all 94 teaching mappings. |
| Selected Android checks | **26 distinct tests pass at their latest execution**, plus the 200% text/landscape Home variant. Focused checks cover real offline hand-model inference, real offline segmentation against an official portrait matte, generated GPU foreground/UV/see-through checks, native visual states/contrast, lesson/curriculum decisions, direct prop interaction, consent/School fences, native Back, and permission continuation. This is not the full instrumentation suite. |
| Rendering | Inspected native Home, Learn, equipment, first-aid practice and normal portrait/landscape AR setup. Home/Learn were exercised in Hindi and English; 200% text landscape navigation passes. |
| Web | Actual login rendered at 1440, 390 and 320 px, including keyboard focus and no horizontal overflow. Dashboard pictures use an explicitly labelled synthetic DOM fixture; no live staff session was used. Both CSS files parse; the local Next development server compiled the login route. |
| Artifact | APK signatures and zip alignment pass; same signing identity as earlier distribution builds. All 15 native libraries inspected; required 64-bit LOAD segments are at least 16 KiB aligned. Both ML model digests and licences verified inside the APK. Static test photos and the TinyFish credential prefix are absent. |
| Preservation | `content/learning/scenarios.json`, `content/learning/catalog.json`, `content/curriculum.json` and `content/spatial/catalog.json` are byte-for-byte unchanged from published 0.20.1. No assessment/certificate database or backend permission change was made. |

Initial failures were retained as evidence: the official portrait first exceeded the production 480 px input bound, so the test was corrected to use that actual input contract; Android System UI ANR stole focus during an early preflight run; a Back assertion required waiting for the real finish transition; the first pointer after rotation required waiting for Android's window transition. Recovery checks passed without bypassing the consent, viewer or School guards.

## Untested and release boundaries

- No authorised physical phone is currently available to ADB, and the user previously reported that the headset is unavailable. **No current-build live hand accuracy, edge lag, anchored manipulation, sustained mid-range performance, voice quality, headset optics, comfort or real viewer-button acceptance is claimed.** An emulator cannot establish these.
- Segmentation can miss fingers or lag fast movement. The automatic fallback makes the camera visible but does not resolve metric hand depth, true stereo passthrough, physical collision or treatment-force measurement.
- Hand translation remains bounded in the anchor's surface coordinates, with model scale and yaw controls. It is not measured six-degree-of-freedom hand pose or unrestricted physical object manipulation.
- The current AR workspace's full live-camera HUD and camera/virtual-view switching have not been observed on a physical device. Its CPU mapping, generated GPU composite and setup/permission recovery have separate evidence.
- Qualified clinical review and Santali native-speaker review remain existing content gates. Model animation is illustrative, not a measured treatment demonstration.
- No web production build or real staff session was run for this CSS change. Existing web dependency-audit findings and the installed Next version's upgrade reminder remain open; this is not an all-CI/security-green claim.
- Backend deployment, account changes, email configuration and recurring collection were not performed. TinyFish collection behavior and credentials were not changed in this work.

Raw logs/screenshots are in the ignored local `artifacts/foreground-ux-2026-10-23/` and `artifacts/tactile-web/` folders. Public machine-readable summaries and the exact changed-file list accompany the release documentation. Official model provenance and limitations are in [README](README.md).

The requested neumorphic direction is applied with contrast/focus boundaries and a bounded bitmap cache. The camera HUD deliberately uses flat high-contrast controls rather than decorative raised panels over the practice area. The existing readable font stack was retained; a mechanical detector's font-popularity warnings are not treated as a reason to introduce an unrelated font dependency.
