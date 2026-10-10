# SurakshaAR 0.12 acceptance matrix

Working acceptance plan, 10 October 2026. This is a test ledger, not a statement that the requested release is complete. See `verification.md` for executed checks. “Pending” means no current-build evidence has been recorded.

## Audience and journey coverage

| ID | Learner / state | Required journey and acceptance | Evidence status |
|---|---|---|---|
| A01 | New individual | Open locally without role selection or account; find Fire Safety and First Aid; start and resume one activity; see that draft practice is formative. | Pending UI walkthrough |
| A02 | School context | Awareness activities are suggested; adult responder activities and industrial fire response cannot be launched through the school-context controls; no implied paediatric scenario or practical competence. | Pending UI and negative path tests |
| A03 | College context | Explore anatomy, select a structure, start the matching first-aid explanation, return to the same stage. | Emulator observation handoff passed, including cancellation without advancement; college-specific entry remains untested |
| A04 | Worker / legacy record | Find industrial library and original records; new launch-course progress does not overwrite industrial assessments or require hidden modules. | Pending upgrade walkthrough |
| A05 | Educator | Teaching workspace appears only for an authorised identity; class invite, assignment, evidence review, correction and scoped report are reachable inside the app; no self-approval shortcut. | Pending end-to-end test against configured backend |
| A06 | Institution administrator | Tenant isolation holds for lists, detail, exports, invitations and direct IDs; permitted content versions govern assignments. | Existing baseline is not proof for changed catalogue; pending current checks |
| A07 | Shared phone | Switching local profile/account changes anatomy-practice history and cached private data; in-flight work cannot save to the replacement profile. | Pending two-profile walkthrough and negative tests |
| A08 | Offline learner | Bundled model and authored practice load in airplane mode; records persist after process death; authored help is labelled as offline help, never live AI. | Pending emulator/device walkthrough |
| A09 | Non-AR device | Camera denial / missing ARCore offers an explicit 3D alternative; ordinary model use earns no AR-specific evidence. | Pending unsupported/denial walkthrough |
| A10 | Accessibility | Required controls remain reachable with large text and TalkBack; every gesture has a visible alternative; Back dismisses overlay before activity exit. | Selected Anatomy/Bay flows passed at 1.3× and 2.0× font; model/dialog Back passed. TalkBack and all alternative gestures/screens remain pending |

## Functional acceptance

| ID | Area | Pass condition | Evidence status |
|---|---|---|---|
| F01 | Catalogue | Exactly Fire Safety and First Aid are active, all authored scenarios are referenced, content versions match, five historic course IDs remain in library. | Structural validator implemented |
| F02 | Anatomy provenance | Every packaged mesh has an ID, name, layer, bounds, source elements, licence notice and SHA-256; model coverage and omissions are disclosed. | Source and final APK validator passed: 68 parts, including separate licence/ShareAlike checks for two lungs; medical validation separate |
| F03 | Actual mesh selection | Nearest visible triangle wins, misses/behind-camera/parallel/non-finite rays do not select, and transformed rays preserve hit positions. | JVM tests passed; actual visible sternum triangle picked through native GL touch path on emulator; physical AR alignment pending |
| F04 | Exploration | Rotate, zoom, two-finger pan, layer switching, select, hide, isolate, focus, reset and orientation work with gestures and controls; occluded/hidden parts cannot be tapped as if visible. | Pending rendered inspection |
| F05 | AR placement | Permission → camera → scanning → stable detected surface → preview → placement → locked model; footprint matches scale and useful space; edit mode cannot transform an assessed interaction. | Samsung SM-S921B, APK `f93c3f…`: 40% tabletop placement locked; user reported stable attachment during sideways movement and look-away/return. Later `0a3d…` also reached surface-ready and 40% Body locked. Full-size/clearance/transform cases and final `6492c376…` AR rerun remain pending |
| F06 | AR recovery | Camera interruption / tracking loss pauses interactions; a new session reacquires placement; lesson stage survives; stale local anchors are never restored as valid. | User-reported look-away/return stability on the earlier Samsung APK; `0a3d…` logs show tracking recovery after interruption. Final camera/session re-entry and lesson-stage recovery still pending physical checks |
| F07 | First-aid practice | One full scenario connects an authored virtual casualty and observable cues with appropriate spatial tool/action interactions, decisions, relevant anatomy, explanation, teach-back and retry; wrong choices and assistance remain in evidence; no practical CPR credential from taps. | Full 12-stage 3D bay passed with original mistakes, pause/recreation/resume and record preservation; accessible AED control used. Engine tracking/order/replay tests pass. Physical AR/equipment touch and practical skills are separate unverified gates |
| F08 | Fire practice | One full scenario covers recognition, alarm, escape, authorised-response decision, suitable equipment, inspection, withdrawal and handover; wrong agent / blocked route variants have authored feedback. | Content and UI validation pending |
| F09 | Narration | Selected language is honoured; missing translation is explicit; audio cancels on pause/leave and does not overlap after rapid navigation; audio controls remain discoverable. | Pending native audio test |
| F10 | Decision history | Reloaded record is checked against identity, exact content version and deterministic replay; replay does not alter original evidence; teach-back cannot change correctness. | Final JVM restore/replay/clock tests and Bay native recreation/old-record preservation passed; full existing-install upgrade remains pending |
| F11 | AI coach | Server allows approved sources only, bounds context/actions, rejects inapplicable sources, keeps provider secret server-side, explains unavailability; model output cannot mutate scores or certificates. | Backend owner supplies test evidence; live provider pending configuration |
| F12 | Asset lifecycle | Bundled pack works offline; optional download, integrity failure, pause/retry and removal preserve required records and recover correctly. | Bundled manifest validation only; optional-pack lifecycle not yet accepted |
| F13 | Notifications | Read/unread, grouping, dismiss/restore and task destination work without exposing another account's notices. | Pending implementation and end-to-end evidence |
| F14 | Credentials | Formative completion, simulation assessment, observed practical skills and recognised qualifications are distinct; original signatures verify unchanged. | Pending upgrade and end-to-end checks |
| F15 | Navigation | Main action is visible without discovery by scrolling; contextual detail uses panels; large text can overflow appropriately without clipping; hardware Back is predictable at every layer. | Pending UI inspection |
| F16 | Child data | Institution/guardian permissions and retention are reviewed before deploying school accounts; no public rankings or unnecessary voice/image/health collection. | Deployment requirement; not satisfied by school-context UI |

## Performance and release gates

| ID | Required evidence | Acceptance threshold | Current status |
|---|---|---|---|
| P01 | Named supported mid-range Android physical device / exact APK | Sustained ≥30 fps while using representative anatomy and fire scenes; report distribution, renderer, scene and temperature. | Samsung SM-S921B / `0a3d…`: brief ~30.3 rendered fps, later ~20fps, subsequently critical thermal status; transient PSS/native-memory peak. Sustained 30fps not passed; no mid-range baseline. Final allocation/render changes need a cooled comparison; see [phone report](phone-performance.md) and [resource review](ar-resource-review.md) |
| P02 | At least 20 cold starts under declared device conditions | Local Home p95 ≤3 s; report startup definition and raw observations. | Not measured |
| P03 | Complete 20-minute training session | No unrecovered failure; tracking interruptions handled without false evidence; memory and thermal trace retained. | Not measured |
| P04 | Repeated AR enter/exit cycles | Stable memory after warm-up, camera releases, no stale anchors or callbacks. | Not measured |
| P05 | Current reproducible build | Kotlin tests, Android lint/build, web checks, content/asset integrity and dependency findings triaged. | In progress |
| P06 | Upgrade rehearsal | Preserve existing accounts, records, signatures and offline work; no destructive schema downgrade. | Pending |
| P07 | Distribution | Release signing, production configuration, source/asset licence notices, current demo, rollback and support ownership verified. | Not authorised for publication; debug candidate only |

No build, emulator frame count or content-policy test establishes physical training effectiveness. Qualified clinical/fire-instructor review, language review and real-device evidence are separate release requirements.
