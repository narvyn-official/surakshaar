# Casualty controls and narration validation — 10 October 2026

This report covers native emulator checks, not clinical approval or physical AR tracking. The tests used an isolated synthetic learner on the existing API 36 Suraksha emulator. Each case restored the original learner/language and removed only its synthetic practice records. Final font scale, rotation, automatic rotation and voice preferences match their saved values.

## Final casualty candidate

APK SHA-256: `27d4500984a1f2a730e811e6a562a03c6d8b5abf2390498a0b4c19df027a6a0e`.

`CasualtyScenarioFlowTest` and `FireRoomRenderedFlowTest` returned **OK (4 tests)** in 49.482 seconds. Exact app/test APK hashes and logs are in `artifacts/surakshaar-0.13.0/validation/casualty/final-27d450-build.json` and `final-27d450-casualty-fire-flows.txt`.

The three casualty cases verified:

- Actual GL-rendered geometry, projected cue-centre touch and a completed bleeding practice. They did not call the engine directly to simulate touch completion.
- A wrong decision stays in the journal through pause, activity recreation and resumption of the same attempt.
- Zoom, pan, reset and opening/closing view controls do not create learning actions. System Back closes controls first, then saves and returns to the practice introduction.
- Short taps, movement beyond the tolerance, time spent outside the marker, and a small movement outside/back within the general movement tolerance cannot satisfy the hold gesture.
- A stationary hold completes through the renderer's fresh progress gate. At the first sample after 1.7 seconds, the final run reported fraction `1` with sample age `94 ms`; no extra wait was needed. This measures the UI gesture only, not force or clinical treatment duration.
- Explicit burn, limb-injury and unresponsive/normal-breathing launches keep separate records, render their authored cues and finish through actual native answer controls. A prior unfinished bleeding record remains byte-for-byte unchanged.
- Finished records remain 3D formative practice with `certifiable=false` and practical competence `not-assessed`.

Screenshots with suffix `-27d450.png` include the authored scald and limb cues, illustrative side-lying view, pressure-stage instructions and completion record. Pressure, side-lying and scald screenshots were visually inspected.

## Large text and short-screen correction

The first 200% portrait check revealed broken toolbar label wrapping. The first 200% landscape check found the body viewport collapsed to 72 pixels high. Both failures and original screenshots remain preserved.

After the app fix, the 200% portrait and landscape checks both returned **OK (1 test)** (45.330 and 39.214 seconds) on APK `4583a87ab3e2abbe6fe976915064d5bc06b06a5d367bf8e7523af3098b8d3cb7`. Those layout sources were unchanged in the final casualty candidate. The checks verify visible 48 dp controls, a useful model viewport, the Angle/Move dock and Back preserving progress. Fixed screenshots were visually inspected: the landscape task/view panel now sits beside the model, and large-text toolbar icons remain legible with semantic labels. Full authored cues remain available from the Cue control and narration; the short landscape summary uses ellipsis.

## Investigated failures

- The initial multi-scenario test incorrectly expected a chest marker at a decision-only step. Its expectation was corrected; no app change was required.
- One stationary hold failed on an overloaded emulator run. After that runtime exited, restarting the same AVD and testing the identical installed APK `fc0ad4…` produced a fresh complete sample at the first 1.7-second check (age 15 ms), with **OK (1 test)**. The failure was not hidden or counted as a pass. Separate source review found and fixed a real small-movement bug: the hold now samples the current touch position while retaining the original DOWN point for motion tolerance. The final regression specifically verifies this fix.
- The earlier Fire rendered check passed journal/geometry assertions but screenshots still showed nearby choice rings obscuring equipment. The subsequent Fire-only presentation fix is verified below.

## Fire inspection correction

APK `706425d93e82cb11d9d32f29dbdbb0aa4bda33aa76483344423ff84d9d301ec1` magnifies screen-mode equipment inspection and places its choices in a separate dock. The updated `FireRoomRenderedFlowTest` returned **OK (1 test)** in 9.701 seconds. It checks every inspection choice is inside the viewport, at least 48 dp and non-overlapping. Actual pointer DOWN/UP events at each dock choice produce the expected action ID; an intercepted/restored callback keeps those deliberate mapping checks out of the fixture's learning record. Mission and coaching journals remain unchanged. The alternate-route phase still verifies projected targets and actual GL pixels.

The good/damaged equipment screenshots were visually inspected: the operating-range versus low gauge needle and split hose are visible, without choice rings covering the unit. Evidence is `fire-inspection-706425.txt`, `fire-inspection-706425-build.json`, and the three `fire-*-screen-706425.png` screenshots in the same validation directory. This check covers standard portrait screen inspection. It does not establish camera-mode spatial selection, short-landscape Fire layout or large-text Fire dock accessibility.

### Follow-up large-text baseline on the published preview

The same `706425…` APK was then checked at 200% text. Portrait returned **OK (1 test)** in 9.631 seconds. Landscape failed in 4.157 seconds: the inspection viewport was 1536 × 433 pixels at density 2.625, its choices were only 66.3 pixels (25.3 dp) high, and the equipment region above the choices was 173 pixels (65.9 dp). The published preview therefore does **not** meet the 48 dp target or useful inspection-area checks in this landscape configuration. The failure is preserved in `fire-inspection-706425-landscape-2x.txt`; exact bounds are in `fire-CONDITION_CHECK-layout-landscape-2.0-706425.json`.

Both screenshots were inspected. Landscape's extinguisher is too small for this inspection task, and its captions are cramped. At 200% portrait, the canvas status caption overlaps the extinguisher handle/gauge despite the target dimensions passing. The app source is being refined from these findings; the baseline is not evidence that the later fix passed. Test font scale was restored to 1.0.

## Related evidence and limits

Narration's seven JVM cases and six native voice/language/Back/large-text checks are recorded in `narration-controls.md`. A separate four-case Fire/store/learning-journey batch passed on `7d8f348…`, including encrypted retry and centre/account mismatch rejection (`fire-institution-regressions.txt`).

The emulator cannot establish reliable AR camera anchoring, actual spoken pronunciation, Bluetooth/call interruption behaviour, real first-aid technique, or medical accuracy. Side-lying graphics are illustrative and are not a reviewed recovery-position demonstration. No microphone capture, treatment measurement or qualification was added by these tests.
