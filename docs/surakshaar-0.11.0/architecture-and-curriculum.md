# SurakshaAr 0.11.0 development pilot

4 October 2026. Baseline local and remote commit `0175622`, v0.10.0/code 23. New Android version `0.11.0-dev`, code 24, same installation ID. This is a local debug pilot, not a release candidate. No production deployment, issuer rotation or production signing was performed.

## Learner workflow

Home offers one Continue action for the most recently updated unfinished knowledge, procedure, room or transfer activity. Otherwise it recommends learning. Training exposes five courses with a recommended sequence: read/listen, worked example, guided procedure, independent procedure, review/transfer, knowledge assessment. Progress separates knowledge records, procedure decision replay, formative objective practice, complete local procedure/transfer history, account-linked review status and credentials. The course menu retains room missions, equipment and knowledge rehearsals without creating competing primary starts.

Language and automatic-speech preference remain in the header menu. Profile/help/account actions are available across the three destinations. Manage training appears after a current authenticated server workspace check; the invitation entry offers account onboarding, not privileges. Existing server approval, roles, MFA, independent review and credential policy still decide every staff action. The underlying staff workspace is preserved; a complete task-oriented staff redesign is not implemented in this slice.

Demonstration completion is version/profile scoped and carries no assessment score. The demonstration and transfer recommendations are local learning stages. Existing server certification prerequisites and historical requests remain compatible; the new formative record does not weaken or replace them.

## Record authority and recovery

| Record | Storage and scope | Authority |
| --- | --- | --- |
| Knowledge assessment | Existing `suraksha.db`, worker-scoped, curriculum 0.4.0 and retained archives | Existing replay/regrading and critical-failure rules |
| Procedure attempt | Existing `procedures.db`, worker-scoped, procedure catalogs 1 and 2 | Original action events; completion retains camera/screen/description attribution |
| Room mission | Existing worker-scoped room database | Existing mission/placement/camera gates; separate formative journal |
| Transfer practice | New `skill-practice.db`, schema 1, `transfer-1` | Formative screen decisions, explicitly non-certifiable; strict replay on restore |
| Worked-example completion | Profile + curriculum version preference | Local recommended learning stage only |
| Credentials | Existing wallet/trust and server review systems | Issuer signature, expiry/revocation and independent review; unchanged format |

An action is committed before its next view renders. Failed transfer writes leave the previous record intact. Restoration replays the original choices, checks order, time, result, owner and content version. Any unsafe transfer choice stops the attempt; retry creates a separate UUID. Decision replay only reads finished original procedures. Progress can open an older completed record even after a new attempt starts; unfinished history resumes by its exact worker-scoped ID. Sensitive progress overlays close when leaving the activity. The passport is a conservative projection labelled simulation only or needs practice; workplace competence is explicitly unverified.

New transfer practice is not silently uploaded through the existing certification queue. Existing queue encryption, original-account binding and idempotent sync remain intact. New narration uses installed offline voices and audio focus; Pause/Replay/Mute are visible or in camera Options. Automatic narration avoids competing with touch-exploration screen readers. Real call/music/Bluetooth interruption and spoken-language quality still need device review.

## Curriculum map

Original 40 knowledge decisions and 55 procedure steps are retained unchanged. New `transfer-scenarios.json` defines 13 formative objectives and 15 short cue variations / 30 decisions:

| Domain | Normal | Abnormal / hidden fault | Uncertain |
| --- | --- | --- | --- |
| Fire | Authored paper store, equipment inspection and escape-route change | Electrical scenario with a damaged hose and unavailable serviceable suitable unit | Liquid identity/agent label absent; sand is not an assumed substitute |
| Gas | Outside attendant, permit/team roles | Communication loss and reported collapse; refuse untrained entry rescue | Simulated meter/instrument/permit information incomplete |
| Machinery | Electrical and stored hydraulic energy | Missing guard and unauthorised jam clearing | Incomplete handover/lock ownership/restart authority |
| PPE | Coal/mica dust, selection and fit-test limitations | Damaged strap and unavailable replacement | Steel heat/noise compatibility or exposure context incomplete |
| Emergency | Clear approved route and accountability | Blocked/changed route and designated alternatives | Missing colleague information; report rather than speculate or re-enter |

Each objective has recognition, decision, action, refusal/escalation, misconception, observable evidence, remediation, source/version and reviewer fields. Each variation uses a changed authored cue set and a stable per-attempt choice shuffle. **These are text/screen transfer checks, not new spatial AR arrangements or real equipment inspection.** They deepen formative decisions but do not establish transfer to real work. Objective-to-procedure mapping is a coarse pilot mapping requiring instructional review. Hidden faults are authored cues, not automatic fault recognition.

All new safety material has `reviewStatus: draft` and `reviewer: null`. Existing English/Hindi pilot content is also subject to competent review. New detailed questions/scenarios are English; Hindi titles/status text are drafts, and Santali remains unavailable/pending. No new safety thresholds, universal agent/distance rule, real gas reading, respirator fit claim, government accreditation or permission to work is introduced.

## Bounded tutor architecture

`QuestionGuideActivity` bundles 81 questions in 12 topic groups, with on-device search, context, source/version and explicit draft status. Only an explicit typed online request sends the question. No microphone or camera is uploaded. Core lessons/practice remain offline. Independent knowledge assessments block online tutoring on the client; the backend additionally has no assessment answer corpus or score-changing capability.

`POST /api/tutor` accepts a strict 3–500-character request (4 KiB body cap), requires an authenticated learner link in an approved centre, and applies durable atomic budgets of 10/account/minute and 60/account/hour. Quota rows contain aggregate counts; no question/transcript is saved. Suspended centres and cross-account learner IDs are denied before selection. Replies are not cacheable.

The pure tutor policy first refuses bypass/credential/privilege/record/assessment/live-emergency requests and clarifies procedure conflicts. It matches authored references by exact question or bounded lexical candidates. Safety drafts never become answer sources. The optional provider is a **product-reference intent classifier**, not an unconstrained safety chatbot: it can select only a listed product-help case ID. Its output is untrusted, capped at 8 KiB and validated. The service returns authored reference text, never provider prose. HTTPS, no redirects, a 3-second provider deadline and deterministic clarification/fallback bound failures.

`TUTOR_PROVIDER_URL` and `TUTOR_PROVIDER_KEY` are optional backend-only configuration for the documented classifier protocol. No live provider is provisioned or claimed tested. Approved site retrieval, generative simplification, progressive safety hints, explain-back feedback, multilingual transcription and AI revision recommendations remain pending competent corpus review and provider integration. Drafts must not be enabled merely by changing a status field: require a named reviewer, applicable equipment/site context, evidence and versioned evaluation.

## AR and performance boundary

Existing `NativeArDriver` remains the sole Session/update owner. Camera lease, monotonic fresh-frame gate, stable-frame placement, recovery and separate presentation records are retained. New bounded timing windows record p50/p95/p99 **ARCore update CPU cost** and **distinct camera timestamp intervals**, alongside tracking counts. They are not full GPU/render frame times or sustained-fps proof. Memory is fixed at two 300-sample arrays per driver; resume resets cadence to avoid counting a background gap as a frame.

No camera thresholds or quality tier was relaxed. No third-party component, copied asset, new dependency or expressive effect was added. Mineral Light native task panels, readable controls, semantic colors, decision replay and a resumable journey are the selected design details (rulebook P005/P024). Release-performance targets remain physical-device gates.

## Primary guidance inspected for this work

- [Android predictive Back](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture).
- [ARCore session configuration](https://developers.google.com/ar/develop/java/session-config).
- [OSHA confined-space standard](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.146).
- [OSHA energy-control standard](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.147).
- [OSHA extinguisher-use training principles](https://www.osha.gov/etools/evacuation-plans-procedures/emergency-standards/portable-extinguishers/use).

These general sources were inspected on 4 October 2026. US guidance is not Indian legal approval or a site-specific procedure. No copied procedure or external endorsement is claimed.
