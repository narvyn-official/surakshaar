# Adult-collapse learning bay contract

This is a source-checked software draft. It has **not** received qualified medical/instructor approval and does not assess practical CPR, ventilation, pad placement or treatment skill. Implementation of the state machine is separate from verification of its rendered AR activity.

## Interaction contract

`FirstAidBay` is the deterministic owner of the current stage and original events. The renderer only proposes an action ID. An AI answer cannot advance this state machine. The application must only submit a model-selection action after a valid visible-part selection, and must provide current tracking validity for an AR interaction.

| Stage | Accepted action | Interaction meaning |
|---|---|---|
| `safe_space` | `ready-space` | Learner acknowledges checking the real practice area; software does not declare it safe. |
| `response` | `select:FMA50801` | Identify the brain during an instructional pause. |
| `breathing` | `select:FMA7394` | Identify the airway during an instructional pause; the irregular-gasp cue is authored. |
| `buddy` | `call-help-aed` | Give a named virtual helper a specific help/AED task. No actual call occurs. |
| `chest` | `select:FMA7485` | Identify the sternum during an instructional pause. No compression measurement occurs. |
| `begin_cpr` | `begin-cpr-sequence` | Acknowledge the next simulated sequence step; no action on another learner. |
| `aed_power` | `aed-power-on` | Select the virtual trainer's power control. |
| `aed_diagram` | `follow-device-diagram` | Acknowledge following the real device's instructions/diagrams; no clinically unreviewed pad-coordinate grading. |
| `clear` | `stand-clear` | Communicate that everyone should remain clear when instructed. |
| `analysis` | `follow-aed-prompt` | Follow the predefined virtual-device prompt. No rhythm is sensed or diagnosed. |
| `resume_cpr` | `resume-cpr-sequence` | Resume sequence learning as prompted. |
| `handover` | `handover-observations` | Communicate observations and actions without claiming a diagnosis. |

The anatomy stages are teaching pauses. The interface must distinguish them from real-response actions and must not imply that emergency help should be delayed to inspect a model. The app records no speed score.

## Evidence and recovery

- Every recorded event has a unique UUID, stage/index, event kind, presentation mode, tracking-validity flag and timestamp. Attempts have a worker ID and exact curriculum/bay version.
- A wrong part, wrong choice, future-stage action, paused interaction or tracking failure is retained without advancing the stage.
- AR progression requires current valid tracking at every stage. A `3d` action can support learning but remains labelled `3d`; mixed attempts are labelled `mixed`, not pure AR.
- Pause/resume preserves the exact stage. Stop is terminal and carries no penalty. Retrying creates a separate attempt so mistakes are not rewritten.
- Restore replays events, validates identity and versions, and rejects modified state, completion, tracking outcomes, duplicate event IDs, grades or qualification claims.
- `SkillPracticeStore` can persist the append-only `answers` array in a dedicated bay database. This new bay is not implicitly eligible for the existing cloud practice endpoint or historical certificate logic.
- Source and schema checks cannot prove that a caller supplied truthful tracking. The native activity must derive the flag from the actual current AR frame/anchor validity and handle pause/lifecycle recovery.

## Source scope

Medical sequence principles were checked against the [AHA 2025 adult basic life support guideline](https://cpr.heart.org/en/resuscitation-science/cpr-and-ecc-guidelines/adult-basic-life-support) and [2025 adult lay-rescuer algorithm](https://cpr.heart.org/-/media/CPR-Files/CPR-Guidelines-Files/2025-Algorithms/Algorithm-BLS-Adult-Lay-Rescuer-250514.pdf). India emergency-number context uses [ERSS 112](https://112.gov.in/). Original diagrams, logos and handbook text are not bundled by this engine.

The source check supports recognition of unresponsiveness with abnormal breathing, help activation, CPR/AED sequencing and following device prompts. It does not turn these authored scenarios into a reviewed course, validate a simulated physiology model, or grant endorsement. Qualified Indian clinical/instructor review and device-specific demonstration review remain release gates.
