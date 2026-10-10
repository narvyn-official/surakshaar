# Compact anatomy controls — verification

10 October 2026. Local development build; no publication or production approval.

## Artifact

- Android `0.12.1-dev`, version code `26`.
- APK: `artifacts/surakshaar-0.12.1/surakshaar-0.12.1-dev-debug.apk`, 28,104,959 bytes.
- SHA-256: `178c67ffe987b18fcc73370865a3ef0d4b1fa32e00ce5cf391c92b5b30d268e8`.
- Debug signing. Exact packaged curriculum and balanced/detailed anatomy validation passed; the validator explicitly reports `productionApproval: false`.

## Acceptance checks

| Requirement | Evidence |
| --- | --- |
| Replace the oversized paginated View modal | Native inline Angle/Move/Look dock; portrait below the model, short landscape beside it. No control pagination. |
| Keep the model usable while adjusting it | Instrumented live viewport visibility and minimum 120 dp height; zoom, orientation and pan change the actual scene. |
| Accessible control alternatives | Labelled native controls, at least 48 dp button height, slider and zoom buttons; enlarged-text and orientation checks recorded below. |
| Back closes controls first | Instrumentation verifies the activity and existing scene remain alive after Back closes the dock. |
| Reset does not erase exploration | Selection, isolation and hidden structures survive camera reset; appearance/isolation survive activity recreation. |
| Retire rendering work on exit | Five ordinary enter/finish cycles reached DESTROYED and returned live GL-thread counts to the baseline. This does not measure GPU/native memory or physical AR thermal behavior. |
| Recover optional geometry safely | Actual bundled detailed archive: pause/resume, verified activation, 117-part load, deliberate mesh corruption, balanced fallback, reinstall and removal passed on the emulator. |

## Build and source checks

`assembleDebug`, `testDebugUnitTest`, `lintDebug` and `assembleDebugAndroidTest` succeeded. JVM results: **229 tests, zero failures/errors/skips**. Android lint: **zero errors, 53 warnings**; warnings remain and this is not a clean production audit. `git diff --check` passed.

The content validator confirmed two courses, 20 draft scenarios and 94 stages. Balanced anatomy contains 117 structures/groups and 248,058 triangles; the optional detailed archive contains the same semantic IDs and 471,068 triangles. License and mesh digests were checked against the packaged assets. These checks do not establish anatomical completeness or clinical correctness.

## Emulator evidence and build boundaries

Device: `Suraksha_API36`, Android API 36, 1080 × 2400, 420 dpi, software graphics. User requested emulator checks while the Samsung was disconnected. No phone installation or physical-camera AR claim is made for this refinement.

The earlier UI candidate `b4e5f903d0cd6f21bb08c3d8351b6015aaebf263c629f53cb5777838ceeb4681` passed three anatomy tests in `anatomy-controls-pack.txt`: compact dock, five rendering-thread cleanup cycles, and optional-pack recovery. The final APK adds account-session isolation fixes; the anatomy implementation is unchanged.

The six-test learning run in `learning-flows.txt` passed five cases and found a test-harness error in the sixth: `Espresso.pressBack()` rejects exiting the last standalone activity. All revision persistence assertions had already succeeded. The test now presses Back unconditionally and explicitly asserts DESTROYED, preserving the expected exit check. Final-APK rerun evidence is listed below.

An initial broader run in `native-focused.txt` was interrupted when the emulator disconnected, so it is not counted as a pass. An earlier cleanup-test variant stalled in AndroidX's EmptyActivity handshake when closing from CREATED; the final test uses the user's normal finish path, where onPause precedes destruction. No app deadlock or GPU-leak claim is inferred from that harness stall.

Final APK, normal portrait: `final-smoke-normal.txt` confirms **2 tests passed** (revision persistence/recreation/Back exit and the complete compact-dock interaction test). The actual render in `dock-angle-1-1.0.png` was inspected: the model and all controls are visible, with no modal obscuring it.

Final APK at 200% font scale: `final-controls-portrait-2x.txt` and `final-controls-landscape-2x.txt` each confirm **one compact-dock test passed**. The landscape run explicitly requests and asserts landscape orientation. The first attempt relied on an emulator rotation setting that was ignored; its passing interaction result is preserved as `rotation-setting-only-check.txt` and is not landscape evidence. Portrait Angle/Look and landscape Angle/Move renders were inspected without clipping or paging. Emulator font and rotation settings were restored afterward.

Evidence files and screenshots are in [the validation directory](../../artifacts/surakshaar-0.12.1/validation/). `build-checks.json` records the final APK hash and source-check counts; `apk-validation.json` records packaged content and rights checks.

## Limits

The new UI is verified as emulator 3D interaction. Physical AR anchoring, camera restart, sustained memory/thermal performance and the AR-specific placement controls still require representative-device checks. The larger integrated build also retains the qualified clinical/fire review, reviewed localisation, production signing/configuration, live-provider verification and dependency release gates described in the [build notes](README.md). Full signed-in native institution workflows were not visually tested here; backend tests are separate evidence.
