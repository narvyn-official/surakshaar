# Fire room scenario review · 10 October 2026

The two new authored adult-role journeys close specific gaps in the legacy room drill: visual equipment inspection before manipulation, a route that changes after the initial exit decision, and factual responder handover. They remain **source-checked drafts**. Fire-instructor, local operational and Hindi-language approval have not been obtained. No certification or permission to fight a real fire is granted.

| Journey | Required decisions and actions |
| --- | --- |
| `inspection-and-withdrawal` | Alarm → identify current clear exit → select an appropriate provisional unit → inspect material label → inspect gauge → inspect hose/body/safety parts → pin, measured virtual aim and sweep → release and withdraw when conditions worsen → reject the now-blocked original exit → clear alternate route → assembly → missing-person report → factual handover. |
| `damaged-unit-evacuation` | Same initial response and label check → identify authored low gauge and split hose → refuse defective equipment → changed-route evacuation → assembly, missing-person report and handover. Equipment manipulation cannot be reached. |

Evacuation remains a valid choice throughout the inspection stages. Announced explosion risk prevents the equipment path before any inspection. Choosing evacuation does not fabricate an equipment inspection or a defect report.

The case explicitly names a small, contained paper/wood fire and a labelled dry-powder unit. It does not generalise this unit to cooking fats, combustible metals, gas leakage, unknown material or energised electrical equipment. Materials and manufacturer instructions are used instead of importing US class letters into India.

The model state is authored: `fireEquipmentCondition` is good/damaged, and `routeChanged` moves the current clear alternative from station 2 to station 0. These are not camera detections. The model must visibly match the label, needle and damaged-hose cues before the new path is treated as an integrated visual training experience. Learners turn the phone in a clear training area; they are not instructed to walk toward virtual fire or follow virtual signs in a real emergency. Virtual spacing, dwell and sweep thresholds are interaction settings, not firefighting distances or competency measurements.

## Primary sources checked

- [OSHA extinguisher-use eTool](https://www.osha.gov/etools/evacuation-plans-procedures/emergency-standards/portable-extinguishers/use): alarm, escape planning, appropriate agent, trained response and withdrawal when response is no longer appropriate.
- [US Fire Administration extinguisher guidance](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/): access, model-specific pressure indication, damaged equipment and manufacturer instructions.
- [Kidde rechargeable dry-chemical owner manual](https://brand.kidde.com/m/f7fe5f74821aca5/original/Rechargeable-FX-User-Manual.pdf), reached through its current product page: label, lock pin, gauge, external damage, nozzle and avoiding test discharge. This supports equipment-awareness concepts, not a claim that our generic mesh is a certified model replica.
- [OSHA evacuation elements](https://www.osha.gov/etools/evacuation-plans-procedures/eap/elements): planned alternative routes, accountability, reporting known missing-person locations and site-specific responsibilities.
- [OSHA 1910.157](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.157): inspected/maintained equipment and training context. US regulations are reference material, **not Indian legal requirements**; no imported maintenance interval is taught as local law.

Scenario prose and Hindi text were authored for this repository; no third-party imagery or guideline passages were copied. Source references do not imply endorsement. Site plans, extinguisher manufacturer instructions and qualified local training determine real procedures. The first-aid/clinical review boundary is unchanged.

## Record compatibility

`RoomMission("fire")` retains its previous version-2 structure and phase sequence. Optional `fireScenarioId` introduces catalog version 2 with frozen definition version 1, explicit authored condition, individual accepted inspections and the alternate-route/handover fields. Restore replays accepted and rejected actions and checks all scenario/result metadata. It does not regrade an older journey under the new sequence. Invalid/unsupported variants fail closed; previous records are retained by the host. The store locks the engine/catalog/definition identity and explosion variant to an existing record ID, keeps worker ownership checks, and validates new-variant replay before saving. Coaching retains its format and allows the extra named phases. Reviews use the attempt's actual journey.

Engine checks and device-render integration are distinct. Final focused execution on 10 October 2026 passed 49 tests (7 new scenario, 25 legacy mission, 13 coaching and 4 learning tests), with zero failures, errors or skips. XML, log and summary are retained in `artifacts/surakshaar-0.13.0/validation/fire-engine/`. The new isolated-store ownership/definition regression and three-phase rendered regression compiled successfully; their device execution is recorded separately. Model appearance, marker positions, narration and entry/resume integration require the host/device checks.
