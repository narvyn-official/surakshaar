#!/usr/bin/env python3
"""Adversarial packaging tests; run with python3 -m unittest discover -s scripts -p test_validate_learning_release.py."""
import gzip
import hashlib
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile

from validate_learning_release import validate_apk, validate_content, validate_licensing, validate_mesh


class PackagingValidationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.content = Path(self.temp.name)
        self.path = self.content / "anatomy/part.meshpack"
        self.path.parent.mkdir()
        vertices = [-1., -1., 0., 0., 0., 1., 1., -1., 0., 0., 0., 1., 0., 1., 0., 0., 0., 1.]
        self.raw = b"ARA1" + struct.pack("<I", 3) + struct.pack("<18f", *vertices)
        self.part = {"id": "FMA-test", "name": "Test surface", "explanation": "Packaging test only", "layer": "skeleton",
                     "file": "anatomy/part.meshpack", "vertices": 3, "bounds": [-1., -1., 0., 1., 1., 0.],
                     "colour": [.8, .8, .7], "sourceElements": ["test-source"]}
        self.write_mesh(self.raw)

    def write_mesh(self, raw):
        blob = gzip.compress(raw, mtime=0)
        self.path.write_bytes(blob)
        self.part.update(bytes=len(blob), sha256=hashlib.sha256(blob).hexdigest())

    def test_valid_triangle(self):
        self.assertEqual(1, validate_mesh(self.part, self.content)["triangles"])

    def test_android_transformed_gzip_suffix_is_rejected(self):
        self.part["file"] = "anatomy/part.mesh.gz"
        with self.assertRaisesRegex(ValueError, "extension must be .meshpack"):
            validate_mesh(self.part, self.content)

    def test_altered_bytes_fail_before_decode(self):
        self.path.write_bytes(self.path.read_bytes()[:-1] + b"x")
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            validate_mesh(self.part, self.content)

    def test_metadata_cannot_hide_wrong_bounds(self):
        self.part["bounds"][3] = .5
        with self.assertRaisesRegex(ValueError, "bounds do not match"):
            validate_mesh(self.part, self.content)

    def test_path_cannot_escape_content(self):
        self.part["file"] = "../secret.meshpack"
        with self.assertRaisesRegex(ValueError, "unsafe asset path"):
            validate_mesh(self.part, self.content)

    def test_infinite_geometry_rejected(self):
        self.write_mesh(self.raw[:8] + struct.pack("<f", float("inf")) + self.raw[12:])
        with self.assertRaisesRegex(ValueError, "non-finite"):
            validate_mesh(self.part, self.content)

    def test_vertex_count_cannot_lie(self):
        self.write_mesh(b"ARA1" + struct.pack("<I", 6) + self.raw[8:])
        with self.assertRaisesRegex(ValueError, "vertex/byte"):
            validate_mesh(self.part, self.content)

    def test_validates_actual_apk_asset_bytes(self):
        fixture = LearningReferenceTest()
        fixture.setUp()
        for scene in fixture.content["scenarios"]:
            scene["steps"][0]["part"] = self.part["id"]
        manifest = {"version": "test-pack", "units": "metres", "axes": "X anatomical left, Y superior, Z anterior",
                    "heightMetres": 1.72, "source": "https://example.com/source", "licenceUrl": "https://example.com/licence",
                    "licence": "CC-BY-4.0", "attribution": "Test fixture", "reviewStatus": "pending",
                    "limitations": ["A test triangle, not anatomy"], "parts": [self.part]}
        apk = self.content / "test.apk"
        with zipfile.ZipFile(apk, "w") as archive:
            archive.writestr("assets/anatomy/manifest.json", json.dumps(manifest))
            archive.writestr("assets/anatomy/LICENSE.txt", "Test fixture\nhttps://example.com/licence")
            archive.writestr("assets/anatomy/part.meshpack", self.path.read_bytes())
            archive.writestr("assets/learning/catalog.json", json.dumps(fixture.catalog))
            archive.writestr("assets/learning/scenarios.json", json.dumps(fixture.content))
        # Corrupt the source after packaging: the report must describe the APK, not these bytes.
        self.path.write_bytes(b"source changed after build")
        result = validate_apk(apk)
        self.assertEqual(1, result["triangles"])
        self.assertEqual(hashlib.sha256(apk.read_bytes()).hexdigest(), result["apkSha256"])
        self.assertFalse(result["productionApproval"])


class LearningReferenceTest(unittest.TestCase):
    def setUp(self):
        self.catalog = {"contentVersion": "test-1", "active": [
            {"id": "fire", "scenarios": ["fire-one"]}, {"id": "first-aid", "scenarios": ["aid-one"]}],
            "legacy": ["fire", "gas", "machinery", "ppe", "emergency"]}
        self.content = {"version": "test-1", "scenarios": []}
        for sid, course in [("fire-one", "fire"), ("aid-one", "first-aid")]:
            self.content["scenarios"].append({"id": sid, "course": course, "audience": "adult", "objectives": ["Recognise"],
                "prerequisites": [], "reviewStatus": "draft", "reviewer": None, "steps": [{"id": "step", "title": "Title",
                "cue": "Cue", "question": "Question", "explanation": "Explanation", "part": "part-a",
                "source": "https://example.com/test", "choices": [{"id": "safe", "text": "Safe", "correct": True},
                {"id": "unsafe", "text": "Unsafe", "correct": False}]}]})

    def test_valid_draft_never_implies_expert_review(self):
        result = validate_content(self.catalog, self.content, {"part-a"})
        self.assertEqual(["fire-one", "aid-one"], result["draftScenarios"])

    def test_missing_semantic_part_fails(self):
        with self.assertRaisesRegex(ValueError, "not packaged"):
            validate_content(self.catalog, self.content, set())

    def test_claimed_approval_requires_named_reviewer_and_date(self):
        self.content["scenarios"][0]["reviewStatus"] = "approved"
        with self.assertRaisesRegex(ValueError, "approval missing"):
            validate_content(self.catalog, self.content, {"part-a"})

    def test_hidden_scenario_cannot_be_silently_omitted(self):
        self.catalog["active"][0]["scenarios"] = []
        with self.assertRaisesRegex(ValueError, "membership mismatch"):
            validate_content(self.catalog, self.content, {"part-a"})

    def test_content_version_mismatch_fails(self):
        self.catalog["contentVersion"] = "old-version"
        with self.assertRaisesRegex(ValueError, "versions differ"):
            validate_content(self.catalog, self.content, {"part-a"})


class MixedLicenceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.content = Path(self.temp.name)
        (self.content / "anatomy").mkdir()
        self.source = {"source": "https://example.com/4.3", "sourceVersion": "4.3", "mappingSource": "https://example.com/map",
                       "downloadEndpoint": "https://example.com/download", "licence": "CC-BY-SA-2.1-JP", "licenceUrl": "https://creativecommons.org/licenses/by-sa/2.1/jp/",
                       "licenceEvidence": "https://example.com/terms", "attribution": "Other source", "downloadSha256": "a" * 64}
        self.part = {"id": "FMA7309", "file": "anatomy/FMA7309.meshpack", "sourceElements": ["FJ-test"],
                     "changes": "converted", "sourceParts": [{"fj_id": "FJ-test", "fma_id": "FMA-part", "name": "Part", "fileSha256": "b" * 64}],
                     **{key: self.source[key] for key in ("source", "sourceVersion", "licence", "licenceUrl", "attribution")}}
        self.manifest = {"licence": "CC-BY-4.0", "licenceUrl": "https://example.com/base-terms", "attribution": "Base source",
                         "parts": [{"id": "base"}, self.part], "additionalSources": [self.source]}
        (self.content / "anatomy/LICENSE.txt").write_text("Base source\nhttps://example.com/base-terms\nFMA7309.meshpack is separately licensed")
        (self.content / "anatomy/LICENSE-LUNGS.txt").write_text("Other source\nCC BY-SA 2.1 JP ShareAlike\nhttps://creativecommons.org/licenses/by-sa/2.1/jp/")

    def test_separate_licences_are_counted_without_overriding_base(self):
        self.assertEqual({"CC-BY-4.0": 1, "CC-BY-SA-2.1-JP": 1}, validate_licensing(self.manifest, self.content))

    def test_lung_cannot_be_relabelled_as_base_licence(self):
        self.part["licence"] = "CC-BY-4.0"
        with self.assertRaisesRegex(ValueError, "contradicts source"):
            validate_licensing(self.manifest, self.content)

    def test_missing_override_cannot_inherit_base_licence(self):
        del self.part["licence"]
        with self.assertRaisesRegex(ValueError, "incomplete per-part"):
            validate_licensing(self.manifest, self.content)

    def test_missing_sharealike_notice_is_rejected(self):
        (self.content / "anatomy/LICENSE-LUNGS.txt").unlink()
        with self.assertRaisesRegex(ValueError, "notice missing"):
            validate_licensing(self.manifest, self.content)

if __name__ == "__main__":
    unittest.main()
