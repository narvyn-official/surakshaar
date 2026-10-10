# Verification · 11 October 2026

Candidate: **SurakshaAR 0.16.0-rc.1 / Android code 33**. Spatial Studio light. This report covers the spatial upgrade; historical account/institution tests are not relabelled as newly executed. Browser checks used headless Chrome with ANGLE/SwiftShader. Android UI checks used the Android 16 emulator; neither environment is a physical AR tracking benchmark.

## Executed checks

| Check | Result |
| --- | --- |
| Web unit suite | 127 passed, no failures or skips. Includes all current lesson completion paths and five new spatial contract/replay tests. |
| Web types, lint and production build | Passed TypeScript, ESLint and Next.js standalone production build. |
| Complete browser lesson paths | All 94 steps across 20 scenarios on the packaged standalone runtime rendered and completed through the real UI; required anatomy observations exercised; no page exceptions. |
| Restored web procedures | On the packaged standalone runtime, all 55 ordered steps across fire (16), gas (13), machinery (10), PPE (8) and emergency (8) completed through the UI. Incorrect-answer feedback and reload/resume exercised. |
| Browser recovery | Blocked model request produced written fallback; Retry loaded actual geometry. A real forced WebGL context loss also recovered with Retry. Cancelled AR preparation, mocked denied WebXR request and working 3D fallback verified. This did not test physical WebXR tracking. |
| Responsive and visual | No horizontal page overflow at 320, 390, 768 and 1440 CSS pixels. Inspected desktop/mobile, teaching scenes and actual tag, thread, fibre and gauze close-ups. |
| Native unit suite | 314 passed, zero failures/errors/skips. Includes all 94 step mappings, mesh bounds/finite normals, distinct state geometry and streamed mesh decoding with budget, truncation, trailing-data, non-finite and cancellation checks. |
| Android release lint | Zero errors/fatals; 70 warnings remain (including 21 text/localisation, 19 draw-allocation, 10 custom-touch accessibility and 8 view-constructor warnings). Not a warning-free audit. |
| Geometry preservation | Regenerated native-to-web exports after allocation changes: all 29 manifest records and SHA-256 hashes remained identical. |
| Android UI / lifecycle | Nine regression tests passed together: the first step scene for all 20 courses opened, body geometry sharing, five equipment families rendered/recreated, tag/thread/fibre close-ups, retained equipment controls, eight first-aid situations, teaching detail controls and fire-room/scene controls. Two additional checks passed on the final signed APK: all five equipment models render and survive recreation, and the helmet underside close-up renders its stitching. |
| Distribution | Final APK installed in place with the persistent release key; v2 signature and 16-KiB alignment verified. Packaged standalone runtime serves the studio and model files locally. No unexpected private runtime/database/key files were found in the package allowlist check. |

## Corrections prompted by testing

The walkthrough found first-aid framing too distant and poorly angled. Scenes now frame visible step apparatus, use an overhead viewing angle, and offer explicit prop close-ups plus a full-body overview. Inspection tags carry readable, visibly unverified training text. Thread close-up opens from the side to avoid the gauge hiding the fitting.

Repeated native transitions exposed transient memory pressure. Mesh authoring now uses primitive float storage, anatomy decompression streams into the final bounded array, and ordinary inspection avoids prewarming unused procedure variants. Overlapping body scenes reuse verified geometry through a weak cache without sharing learner state; closed teaching views release their references. Geometry hashes remained unchanged. Recreation checks wait for an actual rendered frame before inspecting pixels. An intermittent Espresso root/window-focus failure was resolved by waiting for the recreated window to receive focus and explicitly selecting the active dialog root. The nine-test rerun passed; the signed-APK dialog check also passed.

The browser now persists the initial guest identity before starting a procedure, so local checkpoints survive reload. Controls recover after failed model requests; pointer cancellation and multitouch do not submit a hotspot action.

## Rulebook acceptance and limits

| Area | Status / evidence |
| --- | --- |
| Selected design and task fit | Pass: retained light Spatial Studio, per-step views, dedicated detail controls and visible draft boundaries. |
| Spatial state and workflow | Pass for automated paths: shared map/replay, selected-part state, named controls, reset, checkpoints and no automatic assessment evidence from inspection. |
| Keyboard and accessibility | Partial: ordinary buttons/selects expose names and gesture alternatives; responsive checks passed. Full manual TalkBack/VoiceOver and all large-font/forced-colour combinations remain untested. |
| Privacy and permissions | Camera is opt-in for AR; personal AR does not save frames. Inspection does not invoke institutional proctoring. Mocked request-denial recovery passed; real-phone camera behavior is pending. |
| Materials, rights and budgets | Pass for local asset checks: self-contained allowlisted GLBs, finite bounded geometry, notices retained, selected-scene loading. No manufacturer precision or microscopic physics claim. |
| Performance and lifecycle | Partial: rendering/recreation tests and allocation fixes; no low-end physical-device thermal, battery or sustained tracking benchmark. |
| Identity, tenant administration, billing | Existing features preserved. No new billing or institutional policy flow added. Current web unit regression suite ran; broad prior account/API/backup evidence belongs to 0.15.0 and was not repeated in full. |
| Release operations | Local preparation only. Public hosting, store distribution, production secrets and operator recovery/monitoring require a deployment environment. |

## Still requiring external acceptance

- Physical ARCore and browser WebXR placement/tracking, interruption, varied lighting and long sessions on supported phones. An emulator or mocked XR request cannot prove these.
- Qualified safety/clinical review of all draft scenarios and complete English/Hindi terminology review. Supervised practical assessment remains necessary where physical competence matters.
- Linux/container rebuild: prebuilt standalone runtime is macOS ARM64. Use the included source and Docker recipe for Linux; no Linux-container pass is claimed.
- Production HTTPS/camera policy, issuer configuration, password-reset mail delivery and institutional privacy/retention setup. No live service or saved server was modified.
- Wider accessibility and low-resource browser coverage. The existing 0.15.0 development dependency advisory remains documented in its report; no dependency was introduced by this upgrade.

**Decision:** locally testable, signed distribution candidate. This is not clinical approval, statutory certification or proof that every phone supports camera AR.
