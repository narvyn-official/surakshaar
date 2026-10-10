#!/usr/bin/env python3
"""Validate release content and the exact anatomy bytes without downloading anything.

This checks referential integrity and geometry packaging, not clinical accuracy,
physical AR tracking or training effectiveness. Exit 0 is not release approval.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import math
import re
from pathlib import Path
import struct
import sys
import tempfile
from urllib.parse import urlparse
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def unique_ids(items: list[dict], label: str) -> set[str]:
    ids = [item.get("id") for item in items]
    require(all(isinstance(value, str) and value.strip() for value in ids), f"{label}: missing ID")
    require(len(ids) == len(set(ids)), f"{label}: duplicate ID")
    return set(ids)


def source_url(value: str, label: str) -> None:
    parsed = urlparse(value)
    require(parsed.scheme == "https" and bool(parsed.hostname) and not parsed.username, f"{label}: invalid HTTPS source")


def validate_licensing(manifest: dict, content: Path) -> dict:
    """A default licence cannot silently replace a separately sourced part's terms."""
    notice = (content / "anatomy/LICENSE.txt").read_text()
    require(manifest["attribution"] in notice and manifest["licenceUrl"] in notice, "base asset attribution or source licence link missing from notice")
    additional = manifest.get("additionalSources", [])
    source_keys = [(row["source"], row["sourceVersion"]) for row in additional]
    require(len(source_keys) == len(set(source_keys)), "duplicate additional anatomy source")
    for source in additional:
        for key in ("source", "mappingSource", "downloadEndpoint", "licenceUrl", "licenceEvidence"):
            source_url(source[key], f"additional source {key}")
        require(bool(re.fullmatch(r"[a-f0-9]{64}", source["downloadSha256"])), "additional source archive hash missing or malformed")
        require(source["licence"] == "CC-BY-SA-2.1-JP", "unreviewed additional-source licence; update provenance review before adoption")
        require(source["licenceUrl"] == "https://creativecommons.org/licenses/by-sa/2.1/jp/", "additional-source ShareAlike licence link mismatch")
        require(bool(source["attribution"]), "additional source attribution missing")
    counts = {}
    used_sources = set()
    for part in manifest["parts"]:
        external = any(key in part for key in ("sourceVersion", "sourceParts", "source", "licence"))
        if not external:
            licence = manifest["licence"]
        else:
            for key in ("sourceVersion", "source", "licence", "licenceUrl", "attribution", "changes", "sourceParts"):
                require(bool(part.get(key)), f"{part['id']}: incomplete per-part source/licence metadata ({key})")
            key = (part["source"], part["sourceVersion"])
            require(key in source_keys, f"{part['id']}: separate source not declared")
            source = additional[source_keys.index(key)]
            for field in ("licence", "licenceUrl", "attribution"):
                require(part[field] == source[field], f"{part['id']}: per-part {field} contradicts source")
            used_sources.add(key)
            licence = part["licence"]
            source_parts = part["sourceParts"]
            ids = [item["fj_id"] for item in source_parts]
            require(len(ids) == len(set(ids)) and set(ids) == set(part["sourceElements"]), f"{part['id']}: source elements differ from provenance mapping")
            for item in source_parts:
                require(bool(item.get("fma_id")) and bool(item.get("name")) and bool(re.fullmatch(r"[a-f0-9]{64}", item["fileSha256"])), f"{part['id']}: incomplete original part provenance")
            if licence == "CC-BY-SA-2.1-JP":
                separate_notice = content / "anatomy/LICENSE-LUNGS.txt"
                require(separate_notice.is_file(), "ShareAlike asset notice missing")
                text = separate_notice.read_text()
                require("CC BY-SA 2.1" in text and "ShareAlike" in text and part["licenceUrl"] in text, "ShareAlike terms/link missing from separate notice")
                require(Path(part["file"]).name in notice, f"{part['id']}: main notice does not identify the licence exception")
        counts[licence] = counts.get(licence, 0) + 1
    require(used_sources == set(source_keys), "additional source metadata is not linked to a packaged part")
    return counts


def validate_mesh(part: dict, content: Path) -> dict:
    relative = Path(part["file"])
    require(not relative.is_absolute() and ".." not in relative.parts, f"{part['id']}: unsafe asset path")
    # AAPT transparently inflates .gz assets and removes that suffix. A neutral
    # extension preserves the gzip payload and manifest filename in the APK.
    require(relative.suffix == ".meshpack", f"{part['id']}: asset extension must be .meshpack for Android packaging")
    path = (content / relative).resolve()
    require(path.is_relative_to(content.resolve()), f"{part['id']}: asset outside content root")
    compressed = path.read_bytes()
    require(len(compressed) == part["bytes"], f"{part['id']}: byte count mismatch")
    require(hashlib.sha256(compressed).hexdigest() == part["sha256"], f"{part['id']}: SHA-256 mismatch")
    # Runtime's accepted vertex cap is 1,000,000. Never inflate an unbounded stream.
    max_decoded = 8 + 1_000_000 * 24
    with gzip.open(path, "rb") as stream:
        raw = stream.read(max_decoded + 1)
    require(len(raw) <= max_decoded and len(raw) >= 8, f"{part['id']}: decoded size outside contract")
    require(raw[:4] == b"ARA1", f"{part['id']}: unknown geometry format")
    count = struct.unpack_from("<I", raw, 4)[0]
    require(3 <= count <= 1_000_000 and count % 3 == 0, f"{part['id']}: invalid triangle count")
    require(count == part["vertices"] and len(raw) == 8 + count * 24, f"{part['id']}: vertex/byte mismatch")
    bounds = part["bounds"]
    require(len(bounds) == 6 and all(math.isfinite(v) for v in bounds), f"{part['id']}: invalid bounds")
    require(all(bounds[i] <= bounds[i + 3] for i in range(3)), f"{part['id']}: inverted bounds")
    colour = part["colour"]
    require(len(colour) == 3 and all(math.isfinite(v) and 0 <= v <= 1 for v in colour), f"{part['id']}: invalid colour")
    actual_min = [math.inf] * 3
    actual_max = [-math.inf] * 3
    for row in struct.iter_unpack("<6f", raw[8:]):
        require(all(math.isfinite(v) for v in row), f"{part['id']}: non-finite vertex or normal")
        length = math.sqrt(sum(v * v for v in row[3:]))
        require(.99 <= length <= 1.01, f"{part['id']}: non-unit normal")
        for axis in range(3):
            actual_min[axis] = min(actual_min[axis], row[axis])
            actual_max[axis] = max(actual_max[axis], row[axis])
    require(all(abs(a - b) <= .00001 for a, b in zip(actual_min + actual_max, bounds)), f"{part['id']}: bounds do not match geometry")
    require(bool(part.get("name")) and bool(part.get("explanation")), f"{part['id']}: missing accessible semantic content")
    require(part.get("layer") in {"skin", "skeleton", "muscles", "organs"}, f"{part['id']}: unknown layer")
    require(bool(part.get("sourceElements")), f"{part['id']}: missing source provenance")
    return {"bytes": len(compressed), "triangles": count // 3, "decodedBytes": len(raw)}


def validate_content(catalog: dict, curriculum: dict, part_ids: set[str]) -> dict:
    courses = catalog["active"]
    course_ids = unique_ids(courses, "course catalogue")
    require(course_ids == {"fire", "first-aid"}, "launch catalogue must contain exactly Fire Safety and First Aid")
    require(catalog["contentVersion"] == curriculum["version"], "catalogue and curriculum versions differ")
    require(set(catalog["legacy"]) == {"fire", "gas", "machinery", "ppe", "emergency"}, "legacy course route has changed")
    scenarios = curriculum["scenarios"]
    scenario_ids = unique_ids(scenarios, "scenarios")
    course_by_id = {course["id"]: course for course in courses}
    for course in courses:
        listed = course["scenarios"]
        actual = {scene["id"] for scene in scenarios if scene["course"] == course["id"]}
        require(len(listed) == len(set(listed)) and set(listed) == actual, f"{course['id']}: catalogue scenario membership mismatch")
        require(bool(actual), f"{course['id']}: empty launch course")
    steps_count = 0
    draft_ids = []
    for scene in scenarios:
        sid = scene["id"]
        require(scene["course"] in course_by_id, f"{sid}: unknown course")
        require(scene.get("audience") in {"adult", "awareness"}, f"{sid}: missing audience boundary")
        require(bool(scene.get("objectives")) and "prerequisites" in scene, f"{sid}: missing learning contract")
        require(scene.get("reviewStatus") in {"draft", "approved"}, f"{sid}: invalid review status")
        if scene["reviewStatus"] == "approved":
            require(bool(scene.get("reviewer")) and bool(scene.get("reviewedAt")), f"{sid}: approval missing named reviewer/date")
        else:
            draft_ids.append(sid)
        steps = scene["steps"]
        require(bool(steps), f"{sid}: no learning steps")
        unique_ids(steps, f"{sid} steps")
        steps_count += len(steps)
        for step in steps:
            label = f"{sid}/{step['id']}"
            for field in ("title", "cue", "question", "explanation"):
                require(isinstance(step.get(field), str) and bool(step[field].strip()), f"{label}: missing {field}")
            choices = step["choices"]
            unique_ids(choices, f"{label} choices")
            require(len(choices) >= 2, f"{label}: fewer than two alternatives")
            require(all(isinstance(choice.get("correct"), bool) and bool(choice.get("text")) for choice in choices), f"{label}: malformed choice")
            require(any(choice["correct"] for choice in choices) and any(not choice["correct"] for choice in choices), f"{label}: missing correct/incorrect alternative")
            if step.get("part"):
                require(step["part"] in part_ids, f"{label}: anatomy ID {step['part']} is not packaged")
            source_url(step["source"], label)
    return {"courses": len(courses), "scenarios": len(scenario_ids), "steps": steps_count, "draftScenarios": draft_ids}


def validate_optional_pack(content: Path, base: dict) -> dict | None:
    folder = content / "anatomy/optional"
    if not folder.exists():
        return None
    require({p.name for p in folder.iterdir()} == {"catalog.json", "body-detail.zip"}, "optional pack catalogue/archive missing or unexpected")
    catalog = json.loads((folder / "catalog.json").read_text())
    require(catalog["id"] == "body-detail" and catalog["archive"] == "anatomy/optional/body-detail.zip", "optional pack identity mismatch")
    require(catalog["compatibleBaseVersion"] == base["version"], "optional pack targets another base version")
    payload = (folder / "body-detail.zip").read_bytes()
    require(len(payload) == catalog["bytes"] and 0 < len(payload) <= 80_000_000, "optional archive size mismatch")
    require(hashlib.sha256(payload).hexdigest() == catalog["sha256"], "optional archive SHA-256 mismatch")
    expected = {p["id"]: p for p in base["parts"]}
    allowed = {"manifest.json", "LICENSE.txt", "LICENSE-LUNGS.txt"} | {p["file"] for p in expected.values()}
    with tempfile.TemporaryDirectory(prefix="surakshaar-detail-") as temp:
        destination = Path(temp)
        with zipfile.ZipFile(folder / "body-detail.zip") as archive:
            require(len(archive.namelist()) == len(set(archive.namelist())), "duplicate optional archive entries")
            require(set(archive.namelist()) == allowed, "optional archive entry set differs from semantic package")
            unpacked = sum(entry.file_size for entry in archive.infolist())
            require(unpacked == catalog["unpackedBytes"] and 0 < unpacked <= 160_000_000, "optional unpacked size mismatch")
            for entry in archive.infolist():
                relative = Path(entry.filename)
                require(not relative.is_absolute() and ".." not in relative.parts and "\\" not in entry.filename, "unsafe optional pack path")
                require(entry.file_size <= 32_000_000, "optional file outside storage budget")
                target = destination / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(entry) as stream:
                    blob = stream.read(32_000_001)
                require(len(blob) == entry.file_size, "optional file decoded length mismatch")
                target.write_bytes(blob)
        manifest_bytes = (destination / "manifest.json").read_bytes()
        require(hashlib.sha256(manifest_bytes).hexdigest() == catalog["manifestSha256"], "optional manifest SHA-256 mismatch")
        manifest = json.loads(manifest_bytes)
        require(manifest["version"] == catalog["version"] and manifest.get("quality") == "detail", "optional manifest version/quality mismatch")
        for key in ("units", "axes", "heightMetres"):
            require(manifest[key] == base[key], f"optional coordinate contract differs ({key})")
        require(unique_ids(manifest["parts"], "optional parts") == expected.keys(), "optional semantic IDs differ")
        maximum_delta = 0.0
        for part in manifest["parts"]:
            reference = expected[part["id"]]
            for key in ("name", "layer", "sourceElements", "sourceVersion", "licence", "licenceUrl"):
                require(reference.get(key) == part.get(key), f"optional semantic/licence mismatch ({part['id']}, {key})")
            maximum_delta = max(maximum_delta, max(abs(a-b) for a,b in zip(reference["bounds"], part["bounds"])))
        require(maximum_delta <= .015 and abs(maximum_delta-catalog["maxPartBoundsDeltaMetres"]) <= .00001, "optional quality alignment exceeds declared tolerance")
        meshes = [validate_mesh(part, destination) for part in manifest["parts"]]
        # The distribution ZIP stores notices at its root; the shared validator expects a content tree.
        for name in ("LICENSE.txt", "LICENSE-LUNGS.txt"):
            (destination / "anatomy" / name).write_bytes((destination / name).read_bytes())
        licences = validate_licensing(manifest, destination)
        triangles = sum(row["triangles"] for row in meshes)
        require(triangles == catalog["triangleCount"] and triangles <= 1_000_000, "optional triangle budget mismatch")
        require(sum(row["decodedBytes"] for row in meshes) <= 90_000_000, "optional decoded memory budget exceeded")
        require(len(manifest["parts"]) == catalog["partCount"], "optional part count mismatch")
    return {"id": catalog["id"], "version": catalog["version"], "bytes": catalog["bytes"], "sha256": catalog["sha256"],
            "manifestSha256": catalog["manifestSha256"], "parts": catalog["partCount"], "triangles": triangles,
            "licencesByPart": licences, "maxPartBoundsDeltaMetres": maximum_delta}


def validate(content: Path) -> dict:
    manifest_path = content / "anatomy/manifest.json"
    manifest = json.loads(manifest_path.read_text())
    part_ids = unique_ids(manifest["parts"], "anatomy parts")
    require(manifest["units"] == "metres", "anatomy units must be metres")
    require(manifest["axes"] == "X anatomical left, Y superior, Z anterior", "anatomy axes differ from renderer contract")
    require(.8 < manifest["heightMetres"] < 2.5, "adult model height outside reviewed package sanity bound")
    for field in ("source", "licenceUrl"):
        source_url(manifest[field], field)
    require(manifest["licence"] == "CC-BY-4.0" and bool(manifest["attribution"]), "licence attribution incomplete")
    require(bool(manifest["reviewStatus"]) and bool(manifest["limitations"]), "review or coverage limitations missing")
    require((content / "anatomy/LICENSE.txt").is_file(), "embedded anatomy licence missing")
    licence_counts = validate_licensing(manifest, content)
    meshes = [validate_mesh(part, content) for part in manifest["parts"]]
    packaged = {str(path.relative_to(content)) for path in (content / "anatomy").glob("*.meshpack")}
    require(not list((content / "anatomy").glob("*.mesh.gz")), "obsolete .gz anatomy assets would be rewritten by Android packaging")
    require(packaged == {part["file"] for part in manifest["parts"]}, "orphan or missing packaged anatomy mesh")
    source_ids = [sid for part in manifest["parts"] for sid in part["sourceElements"]]
    require(len(source_ids) == len(set(source_ids)), "anatomy source geometry is duplicated across semantic parts")
    catalog = json.loads((content / "learning/catalog.json").read_text())
    curriculum = json.loads((content / "learning/scenarios.json").read_text())
    result = validate_content(catalog, curriculum, part_ids)
    result.update({"status": "structural-validation-passed", "productionApproval": False,
                   "manifestSha256": hashlib.sha256(manifest_path.read_bytes()).hexdigest(),
                   "catalogSha256": hashlib.sha256((content / "learning/catalog.json").read_bytes()).hexdigest(),
                   "curriculumSha256": hashlib.sha256((content / "learning/scenarios.json").read_bytes()).hexdigest(),
                   "anatomyVersion": manifest["version"], "anatomyParts": len(part_ids),
                   "licencesByPart": licence_counts,
                   "geometryBytes": sum(row["bytes"] for row in meshes),
                   "decodedGeometryBytes": sum(row["decodedBytes"] for row in meshes),
                   "triangles": sum(row["triangles"] for row in meshes),
                   "evidenceBoundary": "Packaging and authored-data checks only; no expert review, device AR, usability or clinical effectiveness claim."})
    optional = validate_optional_pack(content, manifest)
    if optional is not None:
        result["optionalAnatomyPack"] = optional
    return result


def validate_apk(apk: Path) -> dict:
    """Check the bytes actually shipped, even if source assets changed mid-build."""
    with tempfile.TemporaryDirectory(prefix="surakshaar-apk-assets-") as folder:
        content = Path(folder)
        with zipfile.ZipFile(apk) as archive:
            names = archive.namelist()
            require(len(names) == len(set(names)), "APK contains duplicate zip entries")
            total = 0
            for entry in archive.infolist():
                if entry.is_dir() or not entry.filename.startswith(("assets/anatomy/", "assets/learning/")):
                    continue
                relative = Path(entry.filename.removeprefix("assets/"))
                require(not relative.is_absolute() and ".." not in relative.parts, "APK has unsafe learning asset path")
                total += entry.file_size
                require(entry.file_size <= 64 * 1024 * 1024 and total <= 256 * 1024 * 1024, "APK learning asset size outside validation budget")
                target = content / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(entry))
        result = validate(content)
    result["apkSha256"] = hashlib.sha256(apk.read_bytes()).hexdigest()
    result["apkBytes"] = apk.stat().st_size
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--content", type=Path, default=ROOT / "content")
    parser.add_argument("--apk", type=Path, help="Validate learning assets from the built APK instead of the source directory")
    parser.add_argument("--report", type=Path, help="Optional explicit output path; source files are never modified")
    args = parser.parse_args()
    try:
        result = validate_apk(args.apk) if args.apk else validate(args.content)
    except (ValueError, KeyError, OSError, TypeError, struct.error, EOFError, zipfile.BadZipFile) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    text = json.dumps(result, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text)
    print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
