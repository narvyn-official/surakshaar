# Offline anatomy pipeline

The Android app uses the checked-in, versioned pack in `content/anatomy/`.
Blender is an authoring dependency, not an Android runtime dependency. The native
renderer and ARCore session remain in the existing Android application.

## Source and scope

- The base geometry comes from the official BodyParts3D 4.0 archive:
  <https://dbarchive.biosciencedbc.jp/en/bodyparts3d/download.html>.
- The two outer lung surfaces come from separately mapped BodyParts3D 4.3
  source elements. Their provenance and source hashes are recorded in each part
  and `additionalSources` in the manifest.
- The 4.0 base retains CC BY 4.0. The two 4.3 lung derivatives retain CC BY-SA
  2.1 Japan. Preserve `LICENSE.txt`, `LICENSE-LUNGS.txt` and the per-part terms.
  Do not apply the base archive licence to the separately supplied lungs.
- This is a static adult source model with 117 selectable named structures/groups:
  one skin surface, 12 skeletal groups, 93 muscle structures/groups and 11 organs.
  The muscle layer includes 362 original source elements. Grouped remnants,
  missing source details and the external face remain explicit limitations.
  There is no infant/child model, rig, photographic texture, reviewed injury
  animation, physiological simulation or qualified clinical approval.

## Rebuild

With the source archive and mapping files already obtained from their recorded
official endpoints, use the three stages in order:

```sh
python3 scripts/anatomy/build_pack.py /path/to/bodyparts3d-4.0-source
python3 scripts/anatomy/add_lung_surfaces.py /path/to/bodyparts3d-4.0-source /path/to/lung-4.3-source
/Applications/Blender.app/Contents/MacOS/Blender --background --python scripts/anatomy/optimise_blender.py -- /absolute/path/to/SIH
python3 scripts/validate_learning_release.py
```

The lung source directory must include the downloaded `objs/`, its
`lung-manifest.json` and original licence notice. The augmentation validates the
source hashes. Running only the base generator removes the lung augmentation;
always run the complete sequence before distribution.

The default balanced profile is capped at 249,000 triangles. To prepare both
qualities without rewriting assets while Android builds, stage the raw export,
add the lung surfaces there, and copy that raw directory for each profile:

```sh
python3 scripts/anatomy/build_pack.py /path/to/bodyparts3d-4.0-source --output-dir /path/to/raw/anatomy
python3 scripts/anatomy/add_lung_surfaces.py /path/to/bodyparts3d-4.0-source /path/to/lung-4.3-source --output-dir /path/to/raw/anatomy
# Copy the augmented raw/anatomy tree to balanced/anatomy and detail/anatomy first.
/Applications/Blender.app/Contents/MacOS/Blender --background --python scripts/anatomy/optimise_blender.py -- /absolute/path/to/SIH --asset-dir /path/to/balanced/anatomy --review-dir /path/to/balanced-previews --profile balanced
/Applications/Blender.app/Contents/MacOS/Blender --background --python scripts/anatomy/optimise_blender.py -- /absolute/path/to/SIH --asset-dir /path/to/detail/anatomy --review-dir /path/to/detail-previews --profile detail
python3 scripts/anatomy/package_detail.py --base /path/to/balanced/anatomy --detail /path/to/detail/anatomy --output /path/to/staged-optional
```

Only the balanced tree plus `optional/body-detail.zip` and `optional/catalog.json`
belong under `content/anatomy`. Keep the expanded detailed/raw source outside
`content/`, since Gradle packages the whole content directory. The optional archive
has a pinned SHA-256, bounded entry set and the same semantic IDs, source elements,
coordinate frame and licences as the balanced pack. Installing it is offline
preparation from a bundled ZIP, not an internet download.

The tested authoring version is Blender 5.2.2 LTS. The exporter removes duplicate
and degenerate faces, decimates within semantic part boundaries, verifies normals
and preserves part IDs. It writes an editable review scene and four rendered
layer previews under `artifacts/anatomy/`. Inspect those previews after changes;
mesh validation does not replace anatomical review.

## Runtime format and checks

Each `.meshpack` file contains gzip-compressed bytes. The decompressed stream is
`ARA1`, a little-endian 32-bit vertex count, then six little-endian floats per
vertex: position XYZ and normal XYZ. Each successive three vertices form a
triangle. Units are metres; X is anatomical left, Y superior and Z anterior.
The manifest records bytes, SHA-256, bounds, source elements, colour and meaning.

**Do not rename these assets to `.gz`.** AAPT automatically expands that suffix
and changes the packaged path, which breaks integrity and runtime lookup. The
validator rejects it. Validate the APK itself after building:

```sh
python3 scripts/validate_learning_release.py --apk apps/android/app/build/outputs/apk/debug/app-debug.apk --report /path/to/apk-validation.json
```

The balanced pack is bundled; the detailed pack can be prepared from the included
archive. The current exports are 248,058 and 471,068 triangles respectively.
External downloading and high-detail regional packs remain future work. Static
source assets do not establish the accuracy of any illustrative runtime motion.
Never build the distributable APK while an exporter is rewriting its assets.

## Evidence boundary

Model exploration returns a selected semantic ID and its presentation mode.
The first-aid bay records actual simulated choices and tracking validity.
Neither route measures CPR pressure, chest depth, ventilation or real casualty
assessment. The clinical review status is independent of geometry integrity,
rendering quality and automated test results.
