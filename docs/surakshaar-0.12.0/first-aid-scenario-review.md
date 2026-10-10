# First-aid practice bay: supported scenarios

Checked 10 October 2026. This is authored, source-checked **draft** content, not a qualified clinical review. English and Hindi were authored for the app; Hindi requires fluent clinical-language review. No source organisation reviewed or endorsed these lessons. No claim covers every first-aid condition, paediatric care or real-patient diagnosis.

## Scenario contract

`FirstAidScenarioCatalog.all` contains eight adult scenarios: `adult-collapse-response`, `unresponsive-normal-breathing`, `bleeding-injury`, `thermal-burn-response`, `suspected-limb-injury`, `seizure-response`, `fainting-response-bay`, and `heat-illness-response`. The default is `adult-collapse-response`.

Each scenario has source-linked steps, English/Hindi titles, cues, alternatives and consequence feedback. Each includes an explicit authored-cue observation associated with a body region. Selecting a cue marker records observation of simulation information; it does not measure a casualty. The current renderer's supine and side-lying illustrations are distinct from an articulated demonstration of positioning. The whole static body mesh is rotated; it does not demonstrate the limb positions or movements of the recovery-position procedure. Seated posture and complete recovery-position body mechanics are not promised by these descriptors.

The engine exposes a `VisualCue` per stage: pose, region, authored responsiveness, authored breathing state and bilingual description. New sequences never require finding an internal anatomical part in the middle of an urgent response. Optional anatomy study remains a separate learning activity.

## Primary sources and adaptations

- [RCUK 2025 adult basic life support](https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/adult-basic-life-support-guidelines): new unresponsive-person sequences summon help promptly, before the authored breathing inspection. Abnormal breathing changes the response to CPR. The AED branch follows its own explicit, predefined prompt; no real rhythm is read.
- [RCUK 2025 first aid](https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/first-aid-guidelines): general first-aid scope, reassessment and handover. Source check does not turn an app into an emergency-dispatch service.
- [St John recovery position](https://www.sja.org.uk/first-aid-advice/recovery-position/): the authored case explicitly excludes suspected trauma, retains monitoring and changes pathway when breathing becomes abnormal. The side-lying mesh is labelled illustrative, not complete physical technique training.
- [St John severe bleeding](https://www.sja.org.uk/first-aid-advice/severe-bleeding/): protection, urgent help, direct-pressure decision and the embedded-object exception. No phone-derived pressure measurement, improvised tourniquet technique or wound-depth claim.
- [NHS burns/scalds](https://www.nhs.uk/conditions/burns-and-scalds/): thermal scald only, cooling, stuck clothing, covering and escalation. Chemical/electrical burns are not silently treated as the same case. The sequence is not a real treatment timer.
- [St John fractures](https://www.sja.org.uk/first-aid-advice/fractures-and-broken-bones/) and [NHS sprains/strains](https://www.nhs.uk/conditions/sprains-and-strains/): support without forced realignment, avoid testing by walking, and escalate serious signs. No diagnosis distinguishes fracture from sprain through the camera.
- [St John seizures](https://www.sja.org.uk/first-aid-advice/seizure/): protection, no restraint/mouth objects, emergency escalation in an explicitly first-known-seizure case, then breathing reassessment. The static mesh does not animate seizure jerking or breathing; those are explicitly authored reports.
- [St John fainting](https://www.sja.org.uk/first-aid-advice/fainting/): responsive faintness, gradual recovery, and a separate deterioration cue. The response changes immediately if the person remains unresponsive with abnormal breathing.
- [NHS heat illness](https://www.nhs.uk/conditions/heat-exhaustion-heatstroke/): cooling and alertness/swallowing boundaries. Confusion triggers emergency help without waiting for a timer. The app does not measure temperature.
- [India ERSS](https://112.gov.in/): 112 is the India-specific number used in authored delegation. No real call is triggered. Overseas emergency numbers are not copied into controls.

Drug dosing, paediatric/infant protocols, oxygen delivery, diagnosis, compression-force/depth grading and assessed pad placement are outside this version. All correct/incorrect alternatives are app-authored summaries, not copied guideline passages; source graphics and handbooks are not bundled through this catalogue.

## Evidence and record boundaries

The original `FirstAidBay.VERSION == 1`, `SCENARIO_ID`, twelve legacy steps and legacy `create` API remain available unchanged. `createScenario` creates new scenario-bound records. A scenario definition version and fingerprint pin its exact bilingual content and visual cues; restoring against a changed definition fails closed and preserves the record rather than silently regrading it.

All actions remain append-only; incorrect and out-of-order actions, tracking failures, pause/resume and stop remain in the journal. AR actions cannot advance when valid tracking is absent. An explicit accessible 3D action remains `3d`; mixed presentation remains `mixed`. Neither camera mode nor marker selection asserts physical competence. The existing record fields continue to state `certifiable=false`, `practicalCompetence=not-assessed` and `reviewStatus=draft`.

The new engine tests cover all eight sequences and replays, mistakes, tracking loss at every stage, identity/definition tampering, legacy preservation, Hindi copy presence, no anatomy-selection gate, recovery deterioration and harmful alternatives. Final focused execution on 10 October 2026 passed 21 tests (12 legacy + 9 scenario tests), with zero failures, errors or skips. XML and the final shared compile log are retained under `artifacts/surakshaar-0.13.0/validation/firstaid-engine/`. Renderer interaction, scene picking, posture accuracy, narration pronunciation and device usability require their own checks; engine tests do not establish them. The final truthfulness pass makes leg-shape changes reported symptoms, not mesh deformation, and corrects stale visual labels after abnormal-breathing transitions. Definition fingerprints change automatically with this unreleased authored copy; mismatched records remain preserved but are not silently replayed under new text.
