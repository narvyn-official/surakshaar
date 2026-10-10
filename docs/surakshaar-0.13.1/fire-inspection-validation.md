# Fire inspection native controls — 10 October 2026

These are API 36 emulator screen-simulation checks, not physical AR or practical fire-safety certification.

## Final validated candidate

App SHA-256: `e2636826e57353cda516c3ec5da223742a16b54391b9e6b179bf8cffcc48648a`.

The affected regression passed in all three requested layouts: standard portrait **OK (1 test)** in 11.557 seconds; 200% portrait **OK (1 test)** in 12.320 seconds; 200% landscape **OK (1 test)** in 15.852 seconds. These are three executions of the same regression, each covering the gauge, damaged-unit and changed-route fixtures. The final app/test APK hashes are in `native-dock-e26368-build.json`; logs and fresh screenshots/geometry have the `e26368` suffix under `artifacts/surakshaar-0.13.1/validation/fire/`.

The root fix measures the inspection dock in the parent's measurement pass, then measures the child with those final parameters. The readiness guard remains in place. At 200% landscape the native panel is now exactly 735 × 611 pixels beside a model region of 801 × 611 pixels. Its measured rectangle matches the reserved space, every pending-layout flag is false, and pagination stays stable before and after taps: one answer page for gauge inspection, two for the longer damaged-unit answers. No timeout or delay was increased to obtain this result.

Visual review of the final gauge, damaged equipment, second answer page and 200% portrait images confirms the overlapping canvas title is gone, Back is a clear drawable, and the gauge needle/split hose stay visible beside readable native answers. Every answer remains at least 48 dp, fully reachable through explicit page controls, semantically a Button, and correctly mapped by actual pointer events. Viewing and intercepted mapping checks do not add learning actions or change the fixture journals.

The targeted Fire presentation change is verified for this preview scope. This is not a claim of complete release readiness across every device, camera session or assistive technology. Earlier failures and intermediate candidates below are retained to show what was corrected.

## Published preview baseline

The published 0.13.0 preview (`706425d93e82cb11d9d32f29dbdbb0aa4bda33aa76483344423ff84d9d301ec1`) passed standard portrait and 200% portrait dock checks but failed 200% landscape. Its canvas choices shrank to 25.3 dp high, with only 65.9 dp above them for inspecting the extinguisher. The original failure log, geometry JSON and screenshot are preserved under `artifacts/surakshaar-0.13.0/validation/casualty/` and described in that version's casualty validation report.

## Intermediate native dock candidate

Candidate app SHA-256: `95a3e7b3e285f3856abcc954e667da344795839daa1557a040849055492c69b9`.

`FireRoomRenderedFlowTest` passed separately in standard portrait (13.789 seconds), 200% portrait (13.967 seconds), and 200% landscape (16.642 seconds): **OK (1 test)** in each configuration. The exact app/test hashes, logs, screenshots and per-choice geometry are under `artifacts/surakshaar-0.13.1/validation/fire/`, with the `95a3e7` suffix.

The test checks the gauge and damaged-unit phases, plus the alternate evacuation route. Each inspection answer is now a native Button with its authored label and content description, focusability, accessibility click role, at least 48 dp dimensions, full reachable bounds and an uncut caption. It reaches any extra pages through the actual Earlier/More controls, then sends real pointer DOWN/UP events and checks the action callback. Those deliberate mapping checks intercept and restore the callback, so they do not fabricate learning actions: mission and coaching journals remain unchanged. GL PixelCopy confirms a rendered scene. Remaining model width/height is checked separately from the answer panel.

At 200% landscape the model region increased to 801 × 611 pixels (approximately 305 × 233 dp). Choice heights were 134 or 214 pixels, at least 51 dp. All three inspected layouts provide larger, legible controls than the published canvas baseline.

Visual review found two refinements needed on this intermediate candidate: the redundant canvas “3D inspection” label overlaps the handle at 200% landscape, and the first inspection panel initially reports more pages than it does after subsequent taps without a mission change. These findings were sent to the app implementation owner. A subsequent test revision records the panel's actual rectangle and pending-layout flags and checks that it matches the model's reserved rectangle. The final candidate above subsequently resolved these issues and passed the stronger checks.

The next candidate (`c505bf70ccc7adfc0e21beba43cc1e8be72cbab5e48681d133c98a46217e4ca0`) removed the overlapping label and made the Back control a drawable. Standard and 200% portrait checks passed, but a new readiness guard exposed the underlying landscape layout bug: after 8 seconds the panel's measured height was still 433 pixels while its layout parameters and available height were 611 pixels. Every pending-layout flag was false, and the GL image matched the current phase/revision and frame size. A separate diagnostic run reproduced the same failure. The failure logs and fresh `fire-CONDITION_CHECK-not-ready-landscape-2.0-c505bf` image/JSON are retained; this candidate is not described as ready. No test timeouts were relaxed.

Font scale was restored to 1.0, and only the test's UUID practice records were deleted. This does not validate TalkBack speech/navigation as a user, physical camera targeting, industrial competence, or model realism beyond the recorded visual inspection.
