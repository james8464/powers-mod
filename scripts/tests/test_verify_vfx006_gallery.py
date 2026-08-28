#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "verify_vfx006_gallery", ROOT / "scripts/verify_vfx006_gallery.py")
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)


class Vfx006GalleryVerifierTest(unittest.TestCase):
    def make_fixture(self, root: Path) -> Path:
        screenshots = root / "screenshots"
        screenshots.mkdir(parents=True)
        rows = []
        sequence = 1
        for reduced in (False, True):
            for style in VERIFY.STYLES:
                for pose in VERIFY.POSES:
                    rows.append(self.row(screenshots, sequence, "gallery", style, pose,
                                         reduced, active=True))
                    sequence += 1
        for scenario in VERIFY.LIFECYCLE_SCENARIOS:
            active = scenario in {"latency", "late_tracking", "locomotion_walk"}
            rows.append(self.row(screenshots, sequence, scenario, "RADIANT", "PROJECT",
                                 False, active=active,
                                 progress=0.5 if active else 1.0,
                                 replacement_uuid=scenario == "entity_id_reuse"))
            sequence += 1
        (root / "capture-manifest.jsonl").write_text(
            "".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
            encoding="utf-8")
        return root

    @staticmethod
    def row(screenshots: Path, sequence: int, scenario: str, style: str, pose: str,
            reduced: bool, active: bool, progress: float = 0.5,
            replacement_uuid: bool = False) -> dict:
        capture_id = f"vfx006-{scenario}-{style.lower()}-{pose.lower()}-{sequence:03d}"
        image_path = f"{sequence:04d}_{capture_id}.png"
        image = Image.new("RGB", VERIFY.EXPECTED_SIZE,
                          (20 + sequence % 200, 35, 55))
        image.save(screenshots / image_path)
        digest = hashlib.sha256((screenshots / image_path).read_bytes()).hexdigest()
        scale = 0.0 if not active else (0.55 if reduced else 0.8)
        entity_uuid = f"11111111-1111-1111-1111-{sequence:012d}"
        return {
            "schemaVersion": 1,
            "implementationSha": "a" * 40,
            "captureId": capture_id,
            "scenario": scenario,
            "entityType": VERIFY.ENTITY_TYPES[style],
            "entityId": 100 + sequence,
            "entityUuid": entity_uuid,
            "resolvedEntityUuid": (f"22222222-2222-2222-2222-{sequence:012d}"
                                   if replacement_uuid else entity_uuid),
            "sequence": sequence,
            "pose": pose,
            "style": style,
            "hand": "RIGHT" if pose == "PROJECT" else "BOTH",
            "authoritativeStartTick": 1000,
            "durationTicks": 20,
            "receiptTick": 1004,
            "captureTick": 1010,
            "reducedMotion": reduced,
            "active": active,
            "progress": progress,
            "angles": {name: scale * VERIFY.ANGLE_LIMITS[name]
                       for name in VERIFY.ANGLE_FIELDS},
            "imagePath": image_path,
            "sha256": digest,
        }

    def mutate(self, operation, message: str) -> None:
        with tempfile.TemporaryDirectory() as raw:
            root = self.make_fixture(Path(raw))
            operation(root)
            with self.assertRaisesRegex(ValueError, message):
                VERIFY.validate(root)

    @staticmethod
    def rows(root: Path) -> list[dict]:
        return [json.loads(line) for line in
                (root / "capture-manifest.jsonl").read_text(encoding="utf-8").splitlines()]

    @staticmethod
    def write_rows(root: Path, rows: list[dict]) -> None:
        (root / "capture-manifest.jsonl").write_text(
            "".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
            encoding="utf-8")

    def test_exact_gallery_passes_deterministically(self):
        with tempfile.TemporaryDirectory() as raw:
            root = self.make_fixture(Path(raw))
            first = VERIFY.validate(root)
            second = VERIFY.validate(root)
            self.assertEqual(first, second)
            self.assertEqual(55, first["rowCount"])
            self.assertEqual(48, first["galleryCount"])
            self.assertEqual(7, first["lifecycleCount"])

    def test_latency_requires_actual_delayed_receipt(self):
        def remove_delay(root):
            rows = self.rows(root)
            row = next(item for item in rows if item["scenario"] == "latency")
            row["receiptTick"] = row["authoritativeStartTick"]
            self.write_rows(root, rows)
        self.mutate(remove_delay, "latency receipt was not delayed")

    def test_latency_progress_uses_authoritative_elapsed_time(self):
        def change_progress(root):
            rows = self.rows(root)
            row = next(item for item in rows if item["scenario"] == "latency")
            row["progress"] = 0.25
            self.write_rows(root, rows)
        self.mutate(change_progress, "authoritative progress mismatch")

    def test_missing_release_pose_is_rejected(self):
        def remove(root):
            rows = self.rows(root)
            victim = next(row for row in rows if row["scenario"] == "gallery"
                          and row["pose"] == "RELEASE")
            (root / "screenshots" / victim["imagePath"]).unlink()
            rows.remove(victim)
            self.write_rows(root, rows)
        self.mutate(remove, "missing pose coverage: RELEASE")

    def test_wrong_dimensions_are_rejected(self):
        def resize(root):
            rows = self.rows(root)
            path = root / "screenshots" / rows[0]["imagePath"]
            Image.new("RGB", (640, 360)).save(path)
            rows[0]["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
            self.write_rows(root, rows)
        self.mutate(resize, "1280x720")

    def test_stale_image_checksum_is_rejected(self):
        def stale(root):
            rows = self.rows(root)
            rows[0]["sha256"] = "0" * 64
            self.write_rows(root, rows)
        self.mutate(stale, "image checksum mismatch")

    def test_absolute_private_path_is_rejected(self):
        def leak(root):
            rows = self.rows(root)
            rows[0]["imagePath"] = "/Users/james/private.png"
            self.write_rows(root, rows)
        self.mutate(leak, "path privacy")

    def test_out_of_bounds_angle_is_rejected(self):
        def angle(root):
            rows = self.rows(root)
            rows[0]["angles"]["leftArmX"] = 1.26
            self.write_rows(root, rows)
        self.mutate(angle, "angle bounds")

    def test_gallery_outside_authored_hold_is_rejected(self):
        def early(root):
            rows = self.rows(root)
            row = next(item for item in rows if item["scenario"] == "gallery"
                       and not item["reducedMotion"])
            row["progress"] = 0.1
            self.write_rows(root, rows)
        self.mutate(early, "gallery capture is outside authored hold")

    def test_entity_id_reuse_requires_distinct_resolved_uuid(self):
        def identity(root):
            rows = self.rows(root)
            row = next(item for item in rows if item["scenario"] == "entity_id_reuse")
            row["resolvedEntityUuid"] = row["entityUuid"]
            self.write_rows(root, rows)
        self.mutate(identity, "entity-ID reuse identity mismatch")


if __name__ == "__main__":
    unittest.main()
