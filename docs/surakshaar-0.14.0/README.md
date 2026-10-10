# SurakshaAR 0.14.0 development preview

This iteration separates everyday navigation, improves model inspection and provides a public account connection. It remains an Android 10+ debug-signed development preview.

## What changed

- **Home:** the learner's next valid saved practice, progress and account status. Resume respects the active profile, audience and current content version. Stale or unsupported attempts are not promoted.
- **Learn:** Fire Safety and First Aid courses, with the earlier industrial library separately available. It no longer repeats the Home task list. Short landscape screens use compact navigation and two-column content; text enlargement retains explicit page controls when needed.
- **Body models:** 987,482 source-derived triangles across 117 anatomical parts, about 2.1 times the previous detailed pack. Detail is prepared offline by default; an explicit Balanced selection and a recoverable fallback remain available. See [model evidence and licences](model-quality-and-coverage.md).
- **Coverage:** opaque navy training shorts in exploration, layer views, placement previews and lying/side poses. Covered external skin triangles are excluded from rendering and picking. Clothing cannot be hidden by anatomy controls.
- **Fire equipment:** finer vessel, hose, nozzle, collar and seal geometry; original English/Hindi training label. Label and Gauge focus actions, rotation, tilt, pinch zoom and Fit work in a fixed model viewport. Opening controls does not page the model out of view. Equipment meshes are created on demand by module.
- **Accounts:** separate native sign-in, sign-up, account, password-change and connection screens. Password-manager hints, reveal/hide, validation, progress/error states and Android Back behavior are retained. Account screens work independently of institution enrolment; signing up grants no staff authority.
- **Public connection:** a dedicated HTTPS preview account service is deployed on the authorised VPS. Email recovery and outgoing account mail remain disabled at the owner's request.

## Use

Install the APK from the [release page](https://github.com/narvyn-official/surakshaar/releases/tag/v0.14.0-preview). Open Learn to choose a course, or Home to continue saved practice. The account entry opens independent Suraksha authentication. Existing manually configured server addresses remain in effect; Connection shows the selected server.

The body detail pack is bundled and does not need a download. The explorer's View dock offers button alternatives to touch gestures. In the equipment explorer, Label and Gauge focus on those parts of the illustrative extinguisher; use Fit to restore the whole view.

## Boundaries

The body and equipment are teaching models, not photographic people, real instruments or clinically validated simulations. The paper/wood training label is authored content; users must read actual equipment instructions. Accounts do not verify identity, grant practical competence or provide statutory certification. Email recovery is deliberately unavailable, so the account flow does not claim to send a reset message.

Final-build physical AR tracking/placement and sustained phone performance remain unverified. Clinical/fire review, complete Santali localisation, production signing and full assistive-technology acceptance are still release gates. See [verification](verification.md) for exact tested builds and remaining limitations.
