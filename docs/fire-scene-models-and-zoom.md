# Fire scene models and view magnification

Implementation review: 10 October 2026. This document covers presentation and interaction changes, not approval of firefighting procedures or a production-readiness claim.

## Scope and learning contract

The native room simulator has **two authored Fire journeys**: `inspection-and-withdrawal` and `damaged-unit-evacuation`. Existing legacy Fire and announced explosion-risk/evacuation branches remain readable. `FireScenePlan` presents the existing engine phase and condition; it cannot advance the engine, change the pinned content version, issue a certificate or classify a real hazard.

The equipment explorer additionally presents **seven existing comparison cases** from `FireResponseLearning`. These are distinct 3D illustrations with the same authored situation, choices and explanations. They are not seven additional complete AR response journeys. The new Scenes picker has five extinguisher inspection specimens and seven context illustrations. Guide opens the matching case. Component Practice is hidden for context illustrations, because a fryer or electrical panel must not launch an extinguisher-parts exercise under the wrong subject. Selecting a scene and resetting the view never records lesson completion.

All new meshes are original procedural geometry. No new third-party model, texture, brand mark or asset licence was adopted. Units are metres. Materials use the existing lightweight GLES2 lighting model; this is not physically based fire, smoke, gas or fluid simulation.

## Coverage matrix

| Existing phase / case | Presentation | Interaction and safety boundary |
| --- | --- | --- |
| ALARM | Raised bell face, recessed call point, fixings and stand | Existing alarm target aligns to the model. No real alarm or emergency call is made. |
| EXIT | Clear doorway and visibly obstructed doorway with crates | Existing route choices remain; virtual signs do not identify real exits. |
| EQUIPMENT | Labelled scenario unit beside a separate unit with obscured label | The second unit has its own projected target at its displayed station; suitability remains unconfirmed. Colour is not an approval. |
| LABEL_CHECK | Curved bilingual paper/wood training decal and intact extinguisher | Existing native answer controls; magnification enlarges the illustration. |
| CONDITION_CHECK | Same vessel with operating-range gauge, focus raised towards the gauge | Authored indication only, not pressure measured by the phone; other extinguisher types may have no gauge. |
| HOSE_CHECK | Attached hose/nozzle, retaining pin and seal | Focus on delivery/safety parts. Existing inspection decision unchanged. |
| DAMAGED_UNIT | Low indicated needle and physically separated/frayed hose ends | Damaged variants never display pin removal, aiming or discharge, even if a held-input flag is supplied. |
| PIN / AIM / SWEEP | Pin preparation, detached pin, directed hose/nozzle, held discharge state | Existing engine owns timing and procedure. View changes cancel incomplete gesture evidence. Model spacing is not a firefighting distance. |
| WITHDRAW / EVACUATE | Persistent worsening fire plus a bounded grey smoke cue | No extinguished-fire claim. Explosion-risk presentation never activates a discharge stream. |
| ROUTE_RECHECK | Original doorway blocked; former equipment station becomes a clear alternate exit | The route change is authored, not camera navigation. Equipment is removed from the repurposed station so it does not obstruct the alternate doorway. |
| ASSEMBLY | Group marker and a blank roll-call clipboard | No fabricated names, attendance or certificate. |
| REPORT / FIRE_HANDOVER / COMPLETE | Roll-call board plus a detailed generic reporting radio | Fire remains a hazard cue; completion does not claim it is out or authorise re-entry. The radio cannot transmit. |
| Paper / wood | Charred timber with grain detail and folded sheets | Reuses `solids` case. No liquid, electrical or chemical risk is assumed by that authored example. |
| Diesel spill | Handled fuel can, bracing and a bounded dark spill | Reuses `liquid` case; no improvised water response is taught. |
| Electrical involvement | Enclosure, grouped breakers, contacts and cable entries | Reuses `electrical`; power isolation is unconfirmed. No interactive live-panel operation is added. |
| Fuel gas | Rounded cylinder, protected valve and collar | Reuses `gas`; worker response remains withdrawal/reporting. No valve-opening or extinguish-the-leak demonstration. |
| Reactive metal | Curled metal swarf on a tray | Reuses `metal`; a generic ABC extinguisher is not presented as universally suitable. |
| Cooking oil | Steel fryer, contained oil surface, wire basket and insulated handle | Reuses `cooking`; no movement, water-pouring or improvised treatment animation. |
| Sand | Tapered bucket, handle and textured sand surface | Reuses `sand`; presence of sand is not proof of compatibility. |

The unconfirmed-label unit is displayed at equipment station 0, not among the flames. Its pick target uses the same translated model position. Existing action ID and deterministic grading are unchanged. New props remain inside bounded station-sized envelopes; these envelopes are layout geometry, not exclusion zones or safe response distances.

## Magnification and gesture ownership

`RoomMissionView` now offers two-finger pinch and native 48 dp minus / reset / plus controls for Fire. The range is 1–2.5×. At font scale 1.6 or greater, reset uses a centred 24 dp icon inside its 48 dp button; its accessible name and tooltip retain the current factor. Normal text size retains the numeric factor caption. Before AR placement completes, magnification remains 1× and controls are hidden, so ARCore placement hit tests continue using the unmodified display coordinates.

After placement:

1. A frame reads one immutable factor/serial snapshot.
2. Its camera background maps `[-1/zoom, +1/zoom]` display-normalized corners through ARCore `Frame.transformCoordinates2d` to texture UVs, then renders the full screen quad.
3. The same factor scales projection clip x/y. Depth, world/model matrices, metre coordinates and all anchor poses are unchanged.
4. Rendering, projected action targets and inverse-matrix ray picking all use that same final view-projection matrix. Screen inspection applies zoom before fitting the remaining model viewport, preserving its centre.
5. Each view change cancels an in-progress pointer/hold and increments a view serial. Old projected targets are immediately ineligible and stale frame callbacks are discarded.
6. A second pointer takes ownership until every pointer has lifted. It cannot become a pin drag, alarm tap or answer on release. One-finger quick-scale/stylus scale is disabled in the room scene.
7. Reset changes only magnification, not the scenario, placement, selected answer or accepted history. A retained view keeps its magnification across pause/resume; a new view/camera placement starts at 1×. Engine resume semantics are unchanged.

The camera zoom is a **digital crop**, not optical camera control. It cannot improve source-camera resolution or create new detail. Status explicitly says distances are unchanged; if a needed target is cropped out, guidance offers zooming out or turning the phone. No walking towards the hazard is required. Actual ARCore/device alignment and registration under motion still need a supported phone test; emulator images cannot establish that.

The equipment explorer keeps its existing orbit, pinch, front/back/side, label/gauge focus and accessible zoom/reset controls. Context illustrations start in a modest three-quarter view; cooking oil, metal swarf, sand and diesel have elevated views so their defining top surfaces are immediately visible. The selected illustration and view parameters survive Activity recreation. Reset preserves the chosen illustration and restores its authored view preset. Invalid/nonfinite restored values and scale factors are rejected or bounded.

## Primary references inspected

- [Android: minimum scaling span](https://developer.android.com/reference/android/view/ViewConfiguration#getScaledMinimumScalingSpan()) and [AOSP ScaleGestureDetector](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/ScaleGestureDetector.java) — the test must cross both minimum span and span-slop thresholds before checking scale callbacks.
- [Android: Drag and scale](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale) — platform gesture detector and multi-pointer ownership.
- [ARCore: Working with anchors](https://developers.google.com/ar/develop/anchors) — keep placement attached to anchor poses; view magnification is not a world transform.
- [ARCore Frame API](https://developers.google.com/ar/reference/java/com/google/ar/core/Frame) — display-normalized to texture-coordinate conversion. The matching crop/projection construction is this implementation's design, not a claim of an ARCore optical-zoom API.
- [OSHA: Fire extinguisher use](https://www.osha.gov/etools/evacuation-plans-procedures/emergency-standards/portable-extinguishers/use) — trained response, maintained escape and withdrawal when conditions worsen. US guidance is not represented as Indian legal certification.
- [USFA: Choosing and using fire extinguishers](https://www.usfa.fema.gov/prevention/home-fires/prepare-for-fire/fire-extinguishers/) — conditional use, correct equipment and retreat.
- The existing authored journey also pins the [Kidde rechargeable-unit manual](https://brand.kidde.com/m/f7fe5f74821aca5/original/Rechargeable-FX-User-Manual.pdf) and [OSHA evacuation-plan elements](https://www.osha.gov/etools/evacuation-plans-procedures/eap/elements). Those sources remain attached to the existing content definition; this visual change does not generalise one manufacturer's gauge to all units.

Fire descriptions retain their existing source-checked draft status. No qualified instructor review or regulatory acceptance is implied by this implementation review.

## Acceptance evidence and pending checks

Added `FireScenePlanTest` (5 checks): complete current-phase mapping, damaged/explosion no-discharge invariants, meaningful scene transitions, exact existing-case mapping/bilingual inspection copy, and finite bounded cached mesh/normal data.

Added `InspectionMagnificationTest` (3 checks): finite bounds and serial/reset rules, camera-crop/projection agreement at centre/corners for 1–2.5×, and off-centre viewport/depth preservation.

Added `FireSceneControlsTest` (2 emulator flows): use the actual scene picker across all seven cases, require changed nonblank GPU model frames, exercise view reset/recreation; then resume isolated gauge/defect/changed-route records, capture scenes, exercise plus/minus/reset and multi-pointer cancellation, and verify persisted lesson history is unchanged. The room test accepts `orientation=landscape`; root sets/restores the device font scale for large-text runs. It asserts 48 dp control bounds, non-overlap, unclipped one-line captions and the icon/reset accessible factor at large text. Only fixture IDs are removed; original profile language is restored. Named captures go to the app's external `validation` directory (`fire-context-*.png`, `fire-view-*-fit.png`, `fire-view-*-zoom.png`, `fire-view-*-reset.png`). This does not replace visual inspection of those captures.

Root ran the Android app/unit build. The generated JVM XML confirms **8/8 new checks passed**, zero failures or skipped checks (5 scene checks and 3 magnification checks); copies are in `artifacts/scenario-upgrade/fire-scene-tests`. Root's earlier normal-size emulator suite passed **20/20 tests**, including both `FireSceneControlsTest` flows and `FireRoomRenderedFlowTest`. The room zoom flow also passed at **200% font scale in landscape**. These checks cover rendering, scene selection, view controls, replay preservation and pointer cancellation; they do not establish physical AR tracking or registration.

Visual inspection of the passing large-text capture revealed that the permanent instruction column and two-row toolbar still crowded the equipment. The current refinement applies only to large-text or short landscape layouts: a full-width scene with its own decision dock, a single 48 dp control row, and a details dialog for complete current instructions/status. The updated test requires a useful model area (at least half the scene width during equipment inspection and 144 dp high), visible non-overlapping controls, full instruction access and unchanged recorded actions after closing details. The compact-layout flow subsequently passed on emulator-5554 at 200% font scale in landscape (**1/1 test, three saved phase fixtures, 16.937 seconds**) against APK `1a7b923a65736d7234dd14d2b0936abf2318eb53127767e70f0d5821d4e49ed2`. The dialog assertion now taps through Espresso, waits for the visible dialog title and checks the complete text in the dialog root; the earlier failure inspected the old Activity window during the dialog transition. Gauge fit/zoom, damaged-unit fit and instruction-dialog captures were visually inspected: the internal model area occupies about 69% of scene width, native controls fit, the split hose/low gauge are visible, and long instructions retain a paged dialog. Evidence: `artifacts/scenario-upgrade/android-fire-large-dialog-rerun.txt`, `compact-fire-verification.json` and `screenshots-compact-final`. Normal layouts retain their existing arrangement; the final combined normal-size regression subsequently passed **22/22 tests** (`artifacts/scenario-upgrade/android-verified-final.txt`).

Remaining material limits: generic meshes, symbolic smoke, no real hazard recognition/occlusion/physical discharge force, no real equipment maintenance verification, and no camera-device validation from these emulator tests. Physical AR alignment under motion still needs a supported phone test.
