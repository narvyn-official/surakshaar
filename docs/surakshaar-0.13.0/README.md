# SurakshaAR 0.13.0 development candidate

This candidate expands the Fire Safety and First Aid learning app. It is a debug-signed development preview, not an approved public clinical/safety training product. The exact APK hash and completed checks are recorded in [verification](verification.md). Downloads belong to the new [SurakshaAR repository](https://github.com/narvyn-official/surakshaar/releases).

## Learning and control changes

- Eight adult casualty situations, with 53 authored stages: collapse, unresponsiveness with normal breathing, forearm bleeding, thermal scald, lower-leg injury, seizure, faintness and heat illness. Each has consequence feedback, authored English/Hindi cues, reassessment and factual handover. These extend the separate 20-scenario, 94-stage decision curriculum.
- Lying-body practice uses the same model transforms for rendering and picking, a virtual mat, modesty drape, distinct non-graphic injury cues and a side-lying illustration. Whole-body rotation is not a rigged recovery-position demonstration. Breathing/seizure symptoms are explicitly described authored cues, not physiological animations or live measurements.
- Drag, pinch, pan and labelled View controls remain available in 3D. Camera practice keeps the casualty anchored; choose size before placement and rescan to reposition. Back closes View controls before leaving practice. Short landscape places the model and task side by side; enlarged text uses semantic icon controls with complete descriptions.
- The wound marker supports a hold-and-release dressing gesture with a progress ring. The 1.5-second interaction dwell only accumulates while a fresh rendered cue intersects the touch ray. Off-target time, camera tracking loss, changed view, excessive finger movement, extra pointers and backgrounding cannot complete it. The duration is a UI setting, not a treatment duration; force is not measured. The accessible button alternative stays labelled and recorded as 3D.
- Fire room practice has two explicit journeys: inspect/respond/withdraw, and reject a damaged unit. They add material-label matching, gauge and hose checks, visible low-gauge/split-hose defects, a changed exit, assembly, missing-person reporting and responder handover. Inspection choices have a separate answer dock, and the screen-mode equipment view is magnified without changing model dimensions. Camera models remain anchored. Evacuation remains available. Variant, recall mode and earlier evidence survive restart/resume paths.
- One Audio panel provides pause/continue, replay, automatic speech and speed. Only an installed offline voice matching the authored language is used automatically. Missing content/voice is stated; English fallback requires a deliberate choice. Backgrounding and audio interruptions stop speech. No microphone or cloud dictation was added.

## Platform and records

Anatomy GPU buffers upload only for displayed parts and reuse a bounded staging buffer. Camera adapters reuse the driver’s camera wrapper within each frame. Renderer failures return a recoverable state. These are implementation improvements, not a measured sustained-performance claim.

Scenario definitions are pinned to saved records. Older or changed content is preserved without silent regrading. Shared-phone profiles, account/institution boundaries and legacy assessment/certificate evidence remain separate from these formative lessons.

Institution work adds explicit institution selection, scoped assignment/inbox/sync, simpler native controls, and admin AI readiness diagnostics. Formative decision-practice sharing uses opt-in, expiring/revocable QR links, opaque random tokens stored only as hashes, and anonymous public course/scenario metadata. It does not issue a qualification or expose names/raw answers. See the [backend checkpoint](../../apps/admin-web/docs/release-priorities-2026-10-10.md) and [QR contract](../../apps/admin-web/docs/formative-record-sharing.md).

## Acceptance and release boundary

The [first-aid review](../surakshaar-0.12.0/first-aid-scenario-review.md), [Fire review](fire-room-scenario-review.md) and [narration contract](narration-controls.md) identify sources, supported cases and limits. Review status remains draft; school mode does not unlock adult intervention practice. This is not every possible emergency, paediatric care, real-patient diagnosis, validated CPR mechanics or statutory certification.

Production release still requires qualified review of exact clinical/fire content, reviewed Hindi/Santali localisation, a representative physical-device and sustained-performance acceptance run, production signing, HTTPS account/recovery operations, and the dependency release gate. A live contextual AI provider is not configured; readiness diagnostics are distinct from provider validation. No paid service, live provider or production backend deployment was performed. The user authorised a new public repository and downloadable development preview on 10 October 2026.

## Relevant primary engineering references

- [ARCore performance guidance](https://developers.google.com/ar/develop/performance): device profiling, camera/CPU contention, thermal behavior and optional-feature cost.
- [ARCore anchors](https://developers.google.com/ar/develop/anchors): world poses belong to the active AR session; models follow their anchors.
- [Android scale gestures](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale): bounded gesture interaction and explicit controls.

The development rulebook checks applied here are spatial transform/picking consistency, interruption/cancellation, native Back behavior, complete text alternatives, 48 dp controls, large-text layouts, tenant/profile ownership, truthful source/evidence claims, recoverable rendering, and exact-build validation. Unverified areas are recorded explicitly rather than treated as passed.
