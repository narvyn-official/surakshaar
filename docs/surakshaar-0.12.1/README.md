# SurakshaAR 0.12.1 development build

This local Android 10+ build includes the compact anatomy controls requested on 10 October 2026. It is a debug-signed review build, not a clinically approved public release. Earlier 0.12.0 artifacts and their evidence remain separate.

## Review the interface

Open **First Aid → Explore the body → View**.

- The model remains visible and interactive while controls are open. The former paginated full-screen View dialog is removed.
- **Angle:** front, back and side presets; continuous zoom slider; zoom buttons and a view reset.
- **Move:** grouped pan, rotation and tilt controls. Touch gestures remain available directly on the model.
- **Look:** six illustrative skin tones and a translucent skin guide. Appearance never classifies a medical condition.
- In landscape, controls move beside the viewport. Short screens use a compact Fit action. Controls retain labelled accessibility alternatives and native touch heights.
- Android Back closes controls before leaving anatomy. Reset preserves layer, selected/hidden structures and isolation. Appearance and exploration state survive activity recreation.
- AR has placement/edit/lock/rescan controls. The physical phone controls the AR viewpoint; virtual camera pan is not offered in AR.

## Other integrated work

The current source includes 20 draft Fire/First Aid scenarios and 94 authored stages, targeted revision with separate records, preserved earlier wording, and transfer practice. The balanced anatomy pack now contains 117 structures/groups and 248,058 triangles; optional detailed geometry contains the same 117 semantic IDs and 471,068 triangles. See the [model refinement report](../surakshaar-0.12.0/anatomy-refinement.md).

**Profile → Offline content** prepares the included detailed archive locally, with pause/resume, digest verification, corruption fallback, reinstall and removal. The archive is bundled in the APK; this is not an internet-download demonstration. Removing it preserves core learning content and records.

Institution assignment/review, explicit sharing, saved-practice retrieval and inbox workflows were added with account/tenant boundaries and bounded response pages. Their backend scope and checks are in the [learning-centre report](../../apps/admin-web/docs/learning-centres.md). A live AI provider has not been provisioned or verified.

## Validation boundary

The [verification report](verification.md) records exact-build checks and UI evidence. Device testing for this refinement was limited to emulators at the user's request. Earlier Samsung placement reports are historical and are not a physical-AR test of this APK. The previous sustained thermal/native-memory concerns remain unverified by this UI change.

Qualified clinical/fire review, reviewed new Hindi/Santali content, representative physical-device validation, production signing/configuration and unresolved dependency findings remain release gates. Source checks and simulated assessment do not establish real first-aid or fire-response competence.

## Build

- Version: `0.12.1-dev` / code `26`.
- [APK](../../artifacts/surakshaar-0.12.1/surakshaar-0.12.1-dev-debug.apk).
- [SHA-256](../../artifacts/surakshaar-0.12.1/SHA256SUMS.txt).
- [Exact APK content validation](../../artifacts/surakshaar-0.12.1/validation/apk-validation.json).

Nothing was pushed, deployed or published during this refinement.
