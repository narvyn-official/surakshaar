# Fixed-viewport equipment inspection

The equipment scene now stays in the viewport while the learner opens controls. Portrait places a compact dock below the model; landscape puts it alongside. The model is never a child of the text pagination container. Native Back closes the dock before leaving the explorer. The header reset and Fit restore the complete view.

Fire has Label and Gauge focus presets. All five retained equipment modules keep button rotation, zoom and viewpoint alternatives; single-finger rotation/tilt and pinch zoom remain available. Bounds limit camera distance and tilt. Changing from a multi-touch gesture does not jump the rotation.

The original extinguisher uses a finer vessel profile, smoother hose, nozzle grip, valve collars and a tamper-seal illustration. An original 1024-pixel English/Hindi decal supplies a legible paper/wood training cue. The same world mesh and decal draw path are shared with AR. Module meshes now initialise on demand rather than all five at once. No new external asset or dependency was added.

## Verified candidate

On APK `37b90dd60b6bcd56150a31903338e88f9b8fa2d82f8b23e557060d862e0f6ee5`:

- Native focus/rotation/Back/reset test passed in normal portrait (4.229 s).
- The same test passed with 200% text in landscape (4.578 s).
- The five-module GL rendering/rotation test passed (8.957 s), with PixelCopy checking a non-blank rendered model for each module.
- Label and gauge images were inspected: the decal is readable, the focus presets target their intended part, controls retain 48 dp touch heights and the model stays visible while the dock is open.

Later Home/account-only changes do not alter this renderer; the release verification identifies any repeated checks on the final APK. Evidence is under `artifacts/surakshaar-0.14.0/validation/equipment/` and the adjacent logs.

## Limits

This is an illustrative extinguisher, not a manufacturer's instrument. The label is explicitly simulated, and its gauge is not a pressure reading. The appearance is more detailed but not photorealistic. Emulator screenshots do not prove AR camera tracking, real-world lighting integration, equipment operation or physical-phone performance. No equipment inspection, safe-use certification or measured learning gain is inferred from these rendering checks.
