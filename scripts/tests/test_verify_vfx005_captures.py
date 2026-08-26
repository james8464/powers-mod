#!/usr/bin/env python3

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "verify_vfx005_captures", ROOT / "scripts/verify_vfx005_captures.py")
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)


class Vfx005CaptureVerifierTest(unittest.TestCase):
    def make_fixture(self, root: Path) -> tuple[Path, Path]:
        screenshots = root / "screenshots"
        screenshots.mkdir()
        manifest = root / "vfx005-manifest.jsonl"
        rows = []
        baseline = Image.new("RGB", VERIFY.EXPECTED_SIZE, (42, 67, 91))
        baseline_draw = ImageDraw.Draw(baseline)
        baseline_draw.line((635, 360, 645, 360), fill=(220, 220, 220), width=1)
        baseline_draw.line((640, 355, 640, 365), fill=(220, 220, 220), width=1)
        self.add(rows, screenshots, baseline, "baseline", "size_shift", "radiant", 96,
                 False, "all", 0, 1)
        for index, power_id in enumerate(VERIFY.POWER_IDS):
            normal = baseline.copy()
            self.draw_shape(normal, index, (255, 210, 70))
            self.add(rows, screenshots, normal, "far_normal", power_id,
                     "radiant", 96, False, "all", 0, 1)
            reduced = baseline.copy()
            self.draw_shape(reduced, index, (205, 175, 85))
            self.add(rows, screenshots, reduced, "far_reduced", power_id,
                     "radiant", 96, True, "minimal", 0, 1)
        for index, power_id in enumerate(VERIFY.ALIGNMENT_VARIANT_IDS):
            image = baseline.copy()
            self.draw_shape(image, VERIFY.POWER_IDS.index(power_id), (190, 30, 220))
            self.add(rows, screenshots, image, "alignment_variant", power_id,
                     "darkness", 96, False, "all", 0, 1)
        special = baseline.copy()
        self.draw_shape(special, 2, (255, 210, 70), offset=(150, 0))
        self.add(rows, screenshots, special, "near", "flight", "radiant", 8,
                 False, "all", 0, 1)
        self.add(rows, screenshots, baseline.copy(), "wall_baseline", "forcefield",
                 "radiant", 96, False, "all", 0, 1)
        self.add(rows, screenshots, baseline.copy(), "wall", "forcefield",
                 "radiant", 96, False, "all", 0, 1)
        for category, power_id, particles, revision, epoch in (
                ("minimal_particles", "starfall", "minimal", 0, 1),
                ("post_reload", "void_beam", "all", 1, 1),
                ("post_dimension", "time_freeze", "all", 1, 2),
                ("post_reconnect", "double_health", "all", 1, 3)):
            image = baseline.copy()
            self.draw_shape(image, VERIFY.POWER_IDS.index(power_id), (255, 210, 70))
            self.add(rows, screenshots, image, category, power_id, "radiant", 96,
                     False, particles, revision, epoch)
        manifest.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
                            encoding="utf-8")
        return screenshots, manifest

    @staticmethod
    def draw_shape(image: Image.Image, index: int, color: tuple[int, int, int],
                   offset: tuple[int, int] = (0, 0)) -> None:
        draw = ImageDraw.Draw(image)
        cx, cy = 640 + offset[0], 420 + offset[1]
        # Every mask shares an anchor but receives a unique two-arm binary signature.
        draw.line((cx, cy - 28, cx, cy + 28), fill=color, width=4)
        first = index % 7
        second = index // 7
        draw.line((cx, cy - 24 + first * 7, cx + 24 + first * 3,
                   cy - 18 + first * 5), fill=color, width=4)
        draw.line((cx, cy + 24 - second * 9, cx - 28 - second * 4,
                   cy + 18 - second * 6), fill=color, width=4)

    @staticmethod
    def add(rows: list[dict], screenshots: Path, image: Image.Image, category: str,
            power_id: str, alignment: str, distance: int, reduced: bool,
            particles: str, revision: int, epoch: int) -> None:
        capture_id = f"vfx005-{len(rows):03d}-{category}-{power_id}"
        name = capture_id + ".png"
        image.save(screenshots / name)
        rows.append({"captureId": capture_id, "category": category,
                     "powerId": power_id, "alignment": alignment,
                     "distance": distance, "reducedMotion": reduced,
                     "particles": particles, "reloadRevision": revision,
                     "epoch": epoch, "imagePath": name})

    def test_valid_exact_gallery_passes_and_is_deterministic(self):
        with tempfile.TemporaryDirectory() as raw:
            screenshots, manifest = self.make_fixture(Path(raw))
            first = VERIFY.validate(screenshots, manifest)
            second = VERIFY.validate(screenshots, manifest)
            self.assertEqual(first, second)
            self.assertEqual(23, first["farNormalCount"])
            self.assertEqual(23, first["farReducedCount"])
            self.assertEqual(56, first["rowCount"])
            self.assertEqual(json.dumps(first, sort_keys=True),
                             json.dumps(second, sort_keys=True))

    def mutate(self, operation, message: str) -> None:
        with tempfile.TemporaryDirectory() as raw:
            screenshots, manifest = self.make_fixture(Path(raw))
            operation(screenshots, manifest)
            with self.assertRaisesRegex(ValueError, message):
                VERIFY.validate(screenshots, manifest)

    def test_missing_required_row_is_rejected(self):
        def remove(_screenshots, manifest):
            rows = manifest.read_text(encoding="utf-8").splitlines()
            manifest.write_text("\n".join(rows[:-1]) + "\n", encoding="utf-8")
        self.mutate(remove, "required gallery rows")

    def test_wrong_dimensions_are_rejected(self):
        def resize(screenshots, _manifest):
            path = next(screenshots.glob("*far_normal-size_shift.png"))
            Image.new("RGB", (640, 360)).save(path)
        self.mutate(resize, "1280x720")

    def test_blank_foreground_is_rejected(self):
        def blank(screenshots, _manifest):
            baseline = next(screenshots.glob("*baseline-size_shift.png"))
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            target.write_bytes(baseline.read_bytes())
        self.mutate(blank, "blank foreground")

    def test_pairwise_duplicate_monochrome_masks_are_rejected(self):
        def duplicate(screenshots, _manifest):
            source = next(screenshots.glob("*far_normal-size_shift.png"))
            target = next(screenshots.glob("*far_normal-time_shift.png"))
            target.write_bytes(source.read_bytes())
        self.mutate(duplicate, "duplicate monochrome masks")

    def test_reduced_outline_mismatch_is_rejected(self):
        def mismatch(screenshots, _manifest):
            target = next(screenshots.glob("*far_reduced-flight.png"))
            image = Image.open(target).convert("RGB")
            draw = ImageDraw.Draw(image)
            draw.rectangle((590, 390, 690, 465), fill=(255, 0, 0))
            image.save(target)
        self.mutate(mismatch, "reduced outline mismatch")

    def test_crosshair_intrusion_is_rejected(self):
        def intrude(screenshots, _manifest):
            target = next(screenshots.glob("*near-flight.png"))
            image = Image.open(target).convert("RGB")
            ImageDraw.Draw(image).rectangle(VERIFY.CROSSHAIR_ROI, fill=(255, 210, 70))
            image.save(target)
        self.mutate(intrude, "crosshair intrusion")

    def test_wall_leakage_is_rejected(self):
        def leak(screenshots, _manifest):
            target = next(screenshots.glob("*wall-forcefield.png"))
            image = Image.open(target).convert("RGB")
            self.draw_shape(image, 15, (255, 210, 70))
            image.save(target)
        self.mutate(leak, "wall leakage")

    def test_lifecycle_capture_with_reload_overlay_is_rejected(self):
        def overlay(screenshots, _manifest):
            target = next(screenshots.glob("*post_reload-void_beam.png"))
            Image.new("RGB", VERIFY.EXPECTED_SIZE, (239, 50, 61)).save(target)
        self.mutate(overlay, "lifecycle outline mismatch")


if __name__ == "__main__":
    unittest.main()
