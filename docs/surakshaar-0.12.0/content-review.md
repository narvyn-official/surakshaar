# Learning content review — 10 October 2026

The new catalogue contains **20 draft scenarios and 94 authored decision stages**: 13 First Aid scenarios and seven Fire Safety scenarios. These are learning exercises, not treatment of a real casualty, practical skills certification, or evidence that a learner can perform CPR or operate an extinguisher safely.

The review performed here is a primary-source cross-check and scenario consistency review by an AI coding assistant. It is **not a review by an experienced doctor**, independent clinical approval, or endorsement by any cited organisation. `review.status` and every scenario's `reviewStatus` remain `draft`; reviewer fields remain null. A qualified first-aid reviewer and a fire instructor still need to approve the adapted curriculum before an unrestricted public release. This distinction is required for honest evidence, not an assertion that no useful learning can be delivered meanwhile.

## Implemented coverage

| Scenario | Stages | Scope |
|---|---:|---|
| An adult collapses | 10 | Safe approach, responsiveness, abnormal gasping, emergency delegation, CPR/AED sequence, chest landmark, clear during analysis/shock, continuation and handover |
| Get help for an unwell adult | 4 | School awareness: recognise, alert, communicate, protect privacy and avoid peer CPR practice |
| A serious bleeding injury | 5 | Permission, infection barrier, direct pressure, embedded-object caution, escalation and handover |
| A hot-water burn | 5 | Thermal scald, cooling, clothing/jewellery, safe covering, urgent assessment |
| Unresponsive but breathing | 4 | Recovery-position indication, airway anatomy, monitoring and trauma exception |
| An adult is choking | 5 | Effective cough, severe obstruction, adult escalation, no blind sweeps, collapse and follow-up |
| Protect during a seizure | 5 | Safe space, no restraint or mouth objects, emergency help, breathing reassessment and factual handover |
| A painful arm after a fall | 3 | Avoid testing/realignment, support and urgent warning signs |
| Smoke blocks your usual route | 6 | Alarm, alternate exit, limits of role, leave belongings, accountability and transfer |
| Inspect before you respond | 7 | Preconditions, equipment label, damaged hose/seal, model-specific gauge, PASS, withdrawal and reporting |
| Match the hazard, not the colour | 5 | Cooking oil, electrical involvement, label suitability, limits of sand and refusal under uncertainty |
| Leave safely with your class | 3 | School alarm response, safe alternate route and reporting without re-entry |
| Feeling faint or collapsing | 4 | Responsive support, gradual recovery, abnormal-breathing escalation and exercise/chest-pain warning signs |
| Heat illness: cool and escalate | 5 | Stop exposure, cooling, alertness/swallowing boundaries, emergency deterioration and factual handover |
| An injured ankle: support and seek help | 4 | Stop activity, protected cold pack, urgent assessment and serious-injury signs |
| A small cut: clean, cover, reassess | 4 | Minor-wound scope, clean pressure, washing/dressing, embedded-object and infection escalation |
| School: recognise injury and get help | 3 | Adult visitor injury, teacher/clear-location response, changing responsiveness; no child-treatment claim |
| Laboratory: unknown substance, clear exit | 4 | Unknown chemicals, alert/evacuate, alternate route, known hazard information and accountability |
| Kitchen: when the obvious tool is wrong | 3 | Cooking oil, no water or pan movement, unsafe switch access and no re-entry |
| Workplace: authorisation, equipment and handover | 5 | Site role, damaged hose, unreadable agent label, withdrawal and missing-person information |

The existing industrial catalogue and historic certification records are outside this content change. The launch catalogue lists every new scenario explicitly. School awareness and adult responder scenarios are labelled separately; adult-casualty lessons do not establish paediatric or infant competence. The First Aid body is adult anatomy, not a representation of all ages or body types.

## Sources checked and decisions taken

All scenario steps carry a source URL. The top-level `checkedAgainst` list is generated from those exact references. References were checked on 10 October 2026; the access date does not make the whole app clinically approved.

- [AHA 2025 Adult Basic Life Support](https://cpr.heart.org/en/resuscitation-science/cpr-and-ecc-guidelines/adult-basic-life-support): checked the adult arrest and choking decision branches. Gasping is not treated as normal breathing. Adult choking includes the 2025 back-blow/abdominal-thrust sequence and changes to CPR after collapse. There are no phone-derived claims about compression depth, force, pulse or heart rhythm.
- [Resuscitation Council UK 2025 First Aid](https://www.resus.org.uk/professional-library/2025-resuscitation-guidelines/first-aid-guidelines): cross-check for airway/trauma limitations, bleeding escalation and review after choking interventions. The authored scenarios use explicit context rather than presenting every unconscious casualty as eligible for routine rolling.
- [St John Ambulance: duties](https://www.sja.org.uk/first-aid-advice/duties-and-responsibilities-of-first-aiders/), [severe bleeding](https://www.sja.org.uk/first-aid-advice/severe-bleeding/), [recovery position](https://www.sja.org.uk/first-aid-advice/recovery-position/), [choking](https://www.sja.org.uk/first-aid-advice/choking/), [seizure](https://www.sja.org.uk/first-aid-advice/seizure/), and [fractures](https://www.sja.org.uk/first-aid-advice/fractures-and-broken-bones/): checked the limited actions used in their corresponding scenarios. The source clinician's name is not reused as the app's reviewer. The app does not ask learners to record a real casualty or share casualty images.
- [NHS burns and scalds](https://www.nhs.uk/conditions/burns-and-scalds/) (page reviewed March 2026): the authored case is a thermal scald. Chemical and electrical burns are explicitly outside that case. The only burn-cooling duration used is source-backed; the rest of the person must be kept warm.
- [US Fire Administration extinguisher guidance](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/index.html) and [escape planning](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/home-fire-escape-plans/): response preconditions, equipment condition and evacuation principles. US class letters, US emergency numbers and UL marking are not represented as Indian requirements. Some units have no gauge; the pressure question names a gauged stored-pressure unit.
- [London Fire Brigade pan fires](https://www.london-fire.gov.uk/safety/the-home/cooking/pan-fires/): the home scenario prioritises leaving and seeking help rather than encouraging an untrained person to approach a pan fire.
- [Delhi Fire Service electrical precautions](https://dfs.delhi.gov.in/dfs/fire-safety-precautions-against-electricity): electrical involvement adds a separate hazard. The course does not promote a universal agent or teach approaching a dangerous isolator. Older/extinguisher-specific suggestions on that page are not blanket recommendations for learners.
- [India ERSS](https://112.gov.in/) and [Ministry of Home Affairs ERSS](https://www.mha.gov.in/en/commoncontent/emergency-response-support-system-erss): 112 is the Indian emergency reference. No exercise places a real call. UK/US emergency numbers in overseas sources are not copied into learner instructions.

No drug doses, diagnostic scoring, oxygen targets, spinal manoeuvre technique, infant CPR, paediatric choking, improvised tourniquet procedure or real-patient camera interpretation are introduced. Adult choking explicitly excludes pregnancy and inability to encircle the abdomen, which require adapted techniques. Those omissions are curriculum boundaries, not claims that such situations do not occur.

## Draft2 additions and source decisions

`learning-2026.10.10-draft2` retains all existing IDs. The exact original draft1 is bundled at `content/learning/archive/learning-2026.10.10-draft1.json` (SHA-256 `ebaae0b6cbfae944c231344c041838e2d840ce50da7b65a32c779903717c857f`). History resolves that version's original wording; old events are neither relabelled nor regraded against draft2.

- [NHS fainting](https://www.nhs.uk/symptoms/fainting/) and [St John fainting](https://www.sja.org.uk/first-aid-advice/fainting/) inform responsive support and red flags. Persistent unresponsiveness returns to the adult-collapse pathway; no assumption that every collapse is a simple faint.
- [NHS heat illness](https://www.nhs.uk/conditions/heat-exhaustion-heatstroke/), [St John heat exhaustion](https://www.sja.org.uk/first-aid-advice/heat-exhaustion/) and [heatstroke](https://www.sja.org.uk/first-aid-advice/heatstroke/) support the changing-condition case. Confusion prompts immediate emergency help; a timer must never delay escalation. Oral fluid is only offered to an alert person able to swallow.
- [NHS sprains](https://www.nhs.uk/conditions/sprains-and-strains/) and [St John sprains](https://www.sja.org.uk/first-aid-advice/sprains-and-strains/) support stopping activity, comfortable support and assessment cues. The lesson avoids medication dosing and a universal rehabilitation regimen.
- [NHS cuts](https://www.nhs.uk/conditions/cuts-and-grazes/) and [St John cuts](https://www.sja.org.uk/first-aid-advice/cuts-and-grazes/) distinguish small wounds from embedded objects, uncontrolled bleeding and infection. No antibiotic or immunisation schedule is prescribed.
- [ACS spill response](https://www.acs.org/about/governance/committees/chemical-safety/publications-resources/guide-for-chemical-spill-response.html) informs a laboratory uncertainty/evacuation scenario, not student chemical-control instruction. [OSHA emergency plans](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.38), [exit routes](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.37) and [extinguisher roles/maintenance](https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.157) support general response principles. These US provisions are not presented as Indian statutory duties.

Context examples now distinguish School, College, Workplace and Home/community. All first-aid casualties remain explicitly adult; School is help-seeking awareness. These additions are authored summaries linked to primary sources, not copied course handbooks or third-party endorsement.

## Anatomy links corrected

The initial draft's shoulder, lung and sternum links were not all valid for the selected pack. Unsupported shoulder links were removed. The current steps reference only these named structures:

| Pack ID | Structure / reason |
|---|---|
| FMA7485 | Sternum — anatomical location, not compression force/depth |
| FMA7309 | Right lung — breathing context |
| FMA7310 | Left lung — breathing context |
| FMA7394 | Trachea — airway location |
| FMA50801 | Brain — seizure context without diagnosis |

Every linked part is present in the generated anatomy manifest. Anatomical left/right refer to the person's side. Selecting a mesh does not establish diagnosis or practical ability. Geometry accuracy, orientation and renderer picking must also pass the separate anatomy/device review; JSON referential integrity cannot establish them.

## Rights and localisation

The scenario wording, settings, distractors and learning explanations are newly authored. The cited organisations' graphics, PDFs, branding, videos and course handbooks are not bundled or relicensed. Source links do not imply endorsement. The body pack has its own attribution and licence manifest; that permission is separate from curriculum approval.

New scenarios are authored in English. Course names, context labels and native controls now have draft Hindi labels; these are not medical translations. Hindi and Santali clinical lessons remain `pending-review` in the new content's metadata. Existing localisation elsewhere must not be presented as completed localisation of these 94 stages. Translation needs fluent-language and clinical review before language-specific approval.

## Verification and remaining checks

Passed on this content revision:

- The shared release validator's `validate_content` function: two active courses, 20 unique scenarios, 94 stages, catalogue membership, required fields, source URLs, draft reviewer metadata and packaged anatomy references.
- An additional consistency check: exactly one correct answer per step and exact equality between the source index and step source URLs.
- `scripts/validate-launch-curriculum.py`: 332 authored-data assertions, including 49 curated safety decisions, passed; the detailed output is in `content-validation.json`. These are regression assertions, not clinical approval.
- No old unsupported anatomy IDs remain. Correct-choice order is varied in authored data; the Android engine additionally shuffles stably per attempt.

The initial `FMA23881: decoded size outside contract` failure was resolved by mobile mesh optimisation. The preceding draft1 source package and packaged APK checkpoint passed with **68 parts and 198,384 triangles**; the subsequent 117-part pack has its own validation and APK evidence. Validation checks the 66 CC BY 4.0 base parts separately from the two CC BY-SA 2.1 Japan lung derivatives. Android assets use `.meshpack` names to preserve their gzip bytes through APK packaging. Exact hashes and evidence scope are in [asset-validation.json](asset-validation.json), [apk-validation.json](apk-validation.json) and the [verification ledger](verification.md). These checks do not validate emergency outcomes, medical correctness of every future branch, voice pronunciation, translation, learner comprehension or clinical training effectiveness.

Release review should exercise every scenario's correct and incorrect paths, recovery after interruption, school/adult route boundaries, source display, completion evidence, large-text layout, narration language and offline operation. Independent clinical/fire approval and representative learner testing remain required release evidence.

## Native learning integration

The course screen now presents one selected lesson, explicit previous/next controls and a lesson picker. School context exposes only awareness lessons, with a second guard when starting or resuming an adult practice. Hindi control text does not conceal that new clinical lessons and their narration are in English.

Two explicit anatomy-learning stages require an observation before progression: `adult-collapse/landmark` and `recovery-monitor/anatomy`. The activity accepts only the currently requested part/stage result and records the returned mode as `3d` or `ar-exploration`. Using the accessible structure list is labelled exploration; it is not physical skill assessment. Cancelling the viewer does not satisfy the gate. Other anatomy links remain optional so urgent-response decision sequences do not repeatedly require opening a model.

Independent practice does not automatically reveal the explanation after a wrong choice; learners can request help. Guided practice can speak authored corrective guidance. Contextual help opens local authored content first, and the optional online helper uses the separate backend approval/consent contract. The host cancels the help dialog on pause and guards its callbacks against a changed step or learner.

`compileDebugKotlin` passed after these activity changes and the contextual coach integration. Rendered layout, large-text and actual interaction results are recorded separately by the release validation owner; compilation alone does not establish usability.

## Targeted revision and transfer

The course and completion screens derive a revision queue from validated current-version wrong decisions and requested hints. A later clean decision can resolve an objective; adding a reflection to an old attempt cannot override newer decision evidence. Tied timestamps conservatively retain a recorded difficulty. Invalid, foreign-profile and older-version records do not drive the current queue.

A targeted retry records its own decisions, hints and optional teach-back in `learning-revision.db`, evidence type `formative-objective-revision`. It does not alter whole-lesson completion, enter normal private sync, issue a certificate or assert a spatial/physical skill. Retries and help remain visible; only a fresh correct answer without a hint resolves that check. The original attempt remains unchanged. Both ordinary and revision records display original-version wording when available.

After practice, learners can choose an authored transfer situation and begin an independent attempt, with hints available on request. Fire completion links to the existing room-practice route; saved room sessions can be resumed from the course. These journals are shown as camera/screen virtual practice, not inferred AR success merely because a camera Activity opened.

Acceptance checks: wrong-answer → targeted queue → retry → source/why → reflection → next objective; cancel/recreate preserves evidence; School excludes adult revision and transfer; profile changes invalidate actions; histories retain version-specific wording; source links require explicit opening. Data/JVM checks and actual native UI verification are reported separately in the verification ledger. Draft2 UI checks are pending until an executed result is recorded.

Draft2 native checkpoint: **23 JVM tests passed** (12 learning-attempt and 11 objective-revision tests), and `compileDebugAndroidTestKotlin` passed. Evidence: `artifacts/surakshaar-0.12.0/curriculum-draft2/native-learning-check.log` and `jvm-summary.json`. New UI tests in `LearningRevisionFlowTest` cover revision/recreation/original-record preservation, School boundaries and archived wording; they are compiled but not yet executed at this checkpoint.
