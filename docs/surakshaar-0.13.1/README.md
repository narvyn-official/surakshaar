# SurakshaAR 0.13.1 — native Fire inspection controls

This development preview follows the new public repository's 0.13.0 preview. It is an Android 10+ debug build, version code 28. The earlier APK stays available as a separate release.

## Changes

- Extinguisher inspection answers are real Android buttons with complete labels, accessible click semantics, keyboard focus, native feedback and a minimum 48 dp height.
- Tall layouts place the choices below the model. Short, wide layouts place them beside it. If enlarged text needs more space, explicit page controls retain the full labels; no swipe/scroll is required.
- The 3D camera frames the space remaining outside the controls. Model dimensions and AR anchor transforms are unchanged. The camera action remains gated by tracking, current phase and visible associated virtual station.
- Dock geometry is resolved during measurement, so rotation cannot leave the controls at a stale height. Buttons wait for their layout to settle. Frame/layout mismatch, backgrounding or a changed phase cannot silently apply a previous selection.
- The header says “Fire practice · AR/3D”; Back uses a fixed-size drawable. The duplicated “How to do this action” button is hidden during equipment inspection because Coach already supplies that guidance. Placement, retry, withdrawal and completion controls remain available in their own phases.
- Removed the redundant canvas title that crossed the extinguisher handle at enlarged text sizes.

The underlying lesson definitions, First Aid content, narration, record ownership, QR generation and backend are unchanged from 0.13.0. See [the broader changes and release boundaries](../surakshaar-0.13.0/README.md).

## Verification

The exact APK hash and build results are in [build-checks.json](build-checks.json). Android unit tests: **261 passed**. Lint: **0 errors, 53 warnings**. Exact packaged anatomy/content digests and licence notices passed structural validation; this is not clinical approval.

The published 0.13.0 baseline passed standard and enlarged portrait inspection, but its 200% landscape Canvas answer rows were only about 25 dp high. That failed check was preserved, and motivated the native-control change.

Native tests cover standard portrait, 200% portrait and 200% landscape: reachable complete answer labels, native click/focus semantics, 48 dp targets, separate model/panel rectangles, actual pointer-to-action mapping and unchanged saved journals. All three final runs passed on APK `e2636826…`: 11.557 s, 12.320 s and 15.852 s respectively. See [exact verification](verification.md) and [rendered interaction evidence](fire-inspection-validation.md). This is emulator evidence, not full TalkBack or physical-camera acceptance.

The new repository's [first CI run](https://github.com/narvyn-official/surakshaar/actions/runs/38048201792) passed Android and web checks before the web dependency-audit gate failed on eight high development-tooling findings through `braces`. That gate remains enabled; production-only audit reported zero. No forced downgrade or audit waiver was applied.

Qualified clinical/fire and translation review, complete Santali localisation, final-device AR/endurance/audio/accessibility checks, production signing, backend/recovery operations and live-AI configuration remain open. The preview is not a qualification, diagnosis tool, real emergency service or approved public safety curriculum.

The layout implementation follows Android’s [custom-view measurement guidance](https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing#onmeasure), inspected 10 October 2026. Actual behaviour is established by the build-specific tests above, not by documentation alone.
