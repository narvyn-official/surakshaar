# Model quality and coverage refinement

Implemented on 10 October 2026. User priorities: much better models, private-area coverage, precise controls. These are source-derived adult teaching models, not photographic people or validated clinical simulation.

## Detailed geometry

The detailed pack has been rebuilt from the original cached BodyParts3D meshes, not subdivided from the previous mobile model. Stable 117 semantic IDs, source elements, scale, orientation and licences are retained.

| Layer | Previous detailed triangles | Current detailed triangles |
|---|---:|---:|
| External skin | 72,000 | 203,382 |
| Skeleton | 109,905 | 209,908 |
| Muscles | 214,206 | 389,234 |
| Organs and airway | 74,957 | 184,958 |
| Total | 471,068 | 987,482 |

The full external source skin is retained before runtime modesty masking. Source face/hand detail and organ/muscle contours are preserved more closely; there is no invented tissue, photo texture, injury diagnosis or new clinical anatomy claim. The pack uses 71,099,640 decoded geometry bytes; its runtime cap remains 90,000,000 bytes. Assets are uploaded to GPU only for displayed parts. The balanced 248,058-triangle fallback remains available.

Detailed is prepared offline on first background model load. Explicit Balanced choices remain respected. Existing detailed selections upgrade from the new bundled archive. Integrity/preparation failures use Balanced with an explanation and retry through Offline content. Preparing an already-installed pack now correctly selects it again.

Archive: `content/anatomy/optional/body-detail.zip` · 17,690,952 bytes

SHA-256: `109d84e32bd21055154fabb9986f40104d598c4de5eddf41bfcf39e74062b920`

## Coverage contract

The old cylindrical drape was limited to the First Aid bay's skin layer and became transparent during AR placement. It did not protect the body explorer.

`AnatomyClothing` now supplies fitted, closed shorts with a matte fabric material, waistband, separate legs and hem trim. Opaque coverage is rendered in every layer and both presentation modes, including translucent skin guides, unplaced AR previews, lying poses and side rotation. Coverage cannot be hidden via anatomical structure controls. Clothing also blocks selecting hidden anatomy through the covered surface.

The loader removes external skin triangles intersecting the pelvic band before they enter runtime rendering or picking. This second protection covers close-up/inside-mesh views, clipping planes and transparency. The source meshes and their semantic IDs remain unchanged on disk. Chest, airway, forearms and lower-leg learning markers are not covered.

## Evidence

- 17 focused JVM checks passed: four clothing coverage/geometry checks, four pack integrity checks, six ray-intersection checks and three casualty-coordinate checks.
- 26 Python authoring/negative checks passed. Complete promoted-content validation passed, including the 987,482-triangle optional archive and per-part licensing/digests.
- Provisional APK `ead3e071123c6a1d204e89ce540217c2e2845d759e117f1301b2e97f397e10f7`: native model flow passed in 10.275 seconds; ten front/back/side/low/high/layer/lying screenshots were inspected. This exposed a too-broad mask nicking the inner forearms; the mask was narrowed and near-arm regression points added. The corrected candidate below passed.
- Corrected APK `cd72ce4f66d7d2537acc362187d2e9577d9235baad877d4b8c86a22e67f1de27`: the same native flow passed in 10.098 seconds. All ten new screenshots were inspected; the forearms are intact and clothing is opaque from front, back, side, low/high angles, ghosted tissue layers, supine and side-rolled views. Evidence: `artifacts/surakshaar-0.14.0/validation/models/cd72ce4f/`.
- AR preview opacity is enforced in the shared draw path but was not physically camera-tested in this iteration.
- During the provisional emulator run, one anatomy log sample recorded 81,467 KiB Java used and 97,180 KiB native allocated. This is a short emulator sample, not a peak-memory, frame-rate or physical-phone endurance benchmark.
- No final physical phone tracking, thermal endurance, all-device performance or qualified clinical approval is claimed.

The official [BodyParts3D download page](https://dbarchive.biosciencedbc.jp/en/bodyparts3d/download.html) and [licence page](https://dbarchive.biosciencedbc.jp/en/bodyparts3d/lic.html) were checked on 10 October 2026. The 115 base structures retain CC BY 4.0; the two separately sourced lung derivatives retain their existing CC BY-SA 2.1 Japan notices. No new external asset was introduced.
