# Anatomy refinement — 10 October 2026

This iteration expands licensed source coverage and improves mobile materials.
It does not claim photorealism, a complete anatomical atlas, clinical approval or
proven performance on a mid-range phone.

| Property | Earlier pack | Refined balanced | Optional detailed |
|---|---:|---:|---:|
| Selectable structures/groups | 68 | 117 | 117 |
| Skin / skeletal / muscle / organ groups | 1 / 12 / 44 / 11 | 1 / 12 / 93 / 11 | 1 / 12 / 93 / 11 |
| Triangles | 198,384 | 248,058 | 471,068 |
| Balanced geometry gzip bytes | 3,258,733 | 4,208,700 | Distributed as the archive below |
| Balanced decoded vertex-buffer bytes | 14,284,192 | 17,861,112 | Only one quality should be active |

The expanded muscles retain 362 original source elements, including additional
neck, abdominal, forearm, hand, thigh and lower-leg coverage. Existing semantic IDs
are preserved. The proportions and coordinate frame are unchanged; there is no
invented anatomy, mirrored replacement, cosmetic resculpting or fabricated fibre
direction. Some structures remain grouped. The face and external skin still look
like source-derived anatomy rather than a photographic person.

Both quality packs have the same 117 IDs, names, source-element associations and
rights. Decimation changes extrema by at most 0.005157 metres between qualities;
it does not translate the structures. These are visual teaching meshes, not
measurement-grade clinical surfaces. The static lung meshes retain visible source
segment boundaries; removing those by inventing a smoothed envelope would need a
separate anatomical review.

## Materials and interaction contract

The native GLES2 renderer now separates matte bone/skin, muscle, softer lung and
brain surfaces, wetter organ highlights, and generic AED polymer/rubber/display
materials. Lighting uses a bounded analytic key/fill model, a world-space view
position and inverse-transpose normals. It adds no texture download or extra render
pass. Small surface variation is cosmetic, not histology or injury evidence.
Selection preserves the tissue colour with a blue edge/tint instead of replacing
the whole material.

Six learner-selectable skin tones are appearance choices. They do not classify a
person or infer pallor, oxygenation, injury or skin type. `AnatomyMaterials.skinTones`
contains stable IDs 0–5, labels and RGB values; default ID 3 is Medium brown.
Optional local part transforms are applied by the renderer before the scene model
transform; the scene owner must use identical transforms for picking. Explicit GL
resource release and partial-initialisation cleanup were added. Actual Android
appearance, lifecycle and performance must be checked after integration.

## Distribution and source rights

The [official BodyParts3D archive](https://dbarchive.biosciencedbc.jp/en/bodyparts3d/download.html)
and its [licence page](https://dbarchive.biosciencedbc.jp/en/bodyparts3d/lic.html) were
checked again for this expansion. The 115 base parts retain CC BY 4.0. Two separately
sourced lung derivatives still retain CC BY-SA 2.1 Japan; their attribution and
ShareAlike notices remain separate. No new paid source or asset was adopted.

The optional package is a real bundled offline archive, not a simulated download:

- Path: `content/anatomy/optional/body-detail.zip`.
- Bytes: 8,556,418; extracted compressed-file bytes: 8,541,378.
- Archive SHA-256: `7e85acb0410ed117cfde4aaa7b3a69f7ffc15759346afe26dfd2fa676c4e9e6b`.
- Detailed manifest SHA-256: `f150dad7dbd9b1358a7bb72a3d89cf2e740f1b00e6eca3a0f2166700b93bda75`.
- Balanced manifest SHA-256: `18bcfb566782debae0cddab65a2c9e0def1f8560ff54007c18ac3d034d564842`.

The archive has `manifest.json`, `anatomy/<id>.meshpack` entries and both licence
notices. The catalogue pins version, hashes, bytes, entry count and compatibility.
The authoring script checks geometry, semantic identity and cross-quality alignment.
The release validator also checks the nested optional archive in the actual APK,
including exact entry sets, size bounds, mesh hashes and separate licences.

## Reviewed evidence and limits

- Original previews remain in [artifacts/anatomy](../../artifacts/anatomy/).
- Updated balanced references: [muscles](../../artifacts/anatomy/refined-balanced/muscles.png),
  [organs](../../artifacts/anatomy/refined-balanced/organs.png),
  [skin](../../artifacts/anatomy/refined-balanced/skin.png).
- Detailed references: [muscles at an angle](../../artifacts/anatomy/refined-detail/muscles-three-quarter.png),
  [organs](../../artifacts/anatomy/refined-detail/organs.png),
  [skin](../../artifacts/anatomy/refined-detail/skin.png).
- [Promoted source/optional archive validation](../../artifacts/anatomy/refined-balanced/promoted-validation.json) passed.
- 17 existing validator tests and 9 optional-package/negative tests passed.

The rendered images are Blender reference renders, not screenshots of the APK.
They were inspected for coverage, colour, orientation and framing. A three-quarter
preview camera-roll issue was fixed without modifying geometry. Qualified anatomy
review, physical AR alignment and memory/thermal endurance remain separate gates.
