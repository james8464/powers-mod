#!/usr/bin/env python3

import importlib.util
import json
import os
import subprocess
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
NEAR_BODY_ROI = (606, 382, 674, 500)


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
                 False, "all", 0, VERIFY.BASE_EPOCH)
        for index, power_id in enumerate(VERIFY.POWER_IDS):
            normal = baseline.copy()
            self.draw_shape(normal, index, (255, 210, 70))
            self.add(rows, screenshots, normal, "far_normal", power_id,
                     "radiant", 96, False, "all", 0, VERIFY.BASE_EPOCH)
        for index, power_id in enumerate(VERIFY.POWER_IDS):
            reduced = baseline.copy()
            self.draw_shape(reduced, index, (205, 175, 85))
            self.add(rows, screenshots, reduced, "far_reduced", power_id,
                     "radiant", 96, True, "minimal", 0, VERIFY.BASE_EPOCH)
        for index, power_id in enumerate(VERIFY.ALIGNMENT_VARIANT_IDS):
            image = baseline.copy()
            self.draw_shape(image, VERIFY.POWER_IDS.index(power_id), (190, 30, 220))
            self.add(rows, screenshots, image, "alignment_variant", power_id,
                     "darkness", 96, False, "all", 0, VERIFY.BASE_EPOCH)
        special = baseline.copy()
        self.draw_shape(special, 2, (255, 210, 70), offset=(0, 70))
        ImageDraw.Draw(special).rectangle(NEAR_BODY_ROI, fill=(31, 34, 42))
        self.add(rows, screenshots, special, "near", "flight", "radiant", 8,
                 False, "all", 0, VERIFY.BASE_EPOCH)
        self.add(rows, screenshots, baseline.copy(), "wall_baseline", "forcefield",
                 "radiant", 96, False, "all", 0, VERIFY.BASE_EPOCH)
        self.add(rows, screenshots, baseline.copy(), "wall", "forcefield",
                 "radiant", 96, False, "all", 0, VERIFY.BASE_EPOCH)
        for category, power_id, particles, revision, epoch in (
                ("minimal_particles", "starfall", "minimal", 0, VERIFY.BASE_EPOCH),
                ("post_reload", "void_beam", "all", 1, VERIFY.BASE_EPOCH),
                ("post_dimension", "time_freeze", "all", 1, VERIFY.DIMENSION_EPOCH),
                ("post_reconnect", "double_health", "all", 1, VERIFY.RECONNECT_EPOCH)):
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
        capture_id = f"vfx005-{category}-{power_id}"
        name = f"{len(rows):04d}_{capture_id}.png"
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

    def test_extra_screenshot_file_is_rejected(self):
        def extra(screenshots, _manifest):
            Image.new("RGB", VERIFY.EXPECTED_SIZE).save(screenshots / "extra.png")
        self.mutate(extra, "screenshot inventory")

    def test_noncanonical_capture_id_is_rejected(self):
        def rename_id(_screenshots, manifest):
            rows = [json.loads(line) for line in manifest.read_text(encoding="utf-8").splitlines()]
            rows[1]["captureId"] = "vfx005-far_normal-not_size_shift"
            manifest.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        self.mutate(rename_id, "canonical gallery")

    def test_noncanonical_safe_image_path_is_rejected(self):
        def rename_path(screenshots, manifest):
            rows = [json.loads(line) for line in manifest.read_text(encoding="utf-8").splitlines()]
            original = screenshots / rows[1]["imagePath"]
            replacement = screenshots / "renamed.png"
            original.rename(replacement)
            rows[1]["imagePath"] = replacement.name
            manifest.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        self.mutate(rename_path, "canonical gallery")

    def test_noncanonical_metadata_and_epoch_are_rejected(self):
        def mutate_metadata(_screenshots, manifest):
            rows = [json.loads(line) for line in manifest.read_text(encoding="utf-8").splitlines()]
            variant = next(row for row in rows if row["category"] == "alignment_variant")
            variant["particles"] = "minimal"
            rows[0]["epoch"] = 99
            manifest.write_text("".join(json.dumps(row) + "\n" for row in rows), encoding="utf-8")
        self.mutate(mutate_metadata, "canonical gallery")

    def test_png_suffix_with_non_png_content_is_rejected(self):
        def spoof(screenshots, _manifest):
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            image = Image.open(target).convert("RGB")
            image.save(target, format="JPEG")
        self.mutate(spoof, "decoded PNG")

    def test_outside_roi_lifecycle_overlay_is_rejected(self):
        def overlay(screenshots, _manifest):
            target = next(screenshots.glob("*post_reload-void_beam.png"))
            image = Image.open(target).convert("RGB")
            ImageDraw.Draw(image).rectangle((400, 20, 560, 120), fill=(239, 50, 61))
            image.save(target)
        self.mutate(overlay, "lifecycle background mismatch")

    def test_outside_roi_wall_leakage_is_rejected(self):
        def leak(screenshots, _manifest):
            target = next(screenshots.glob("*wall-forcefield.png"))
            image = Image.open(target).convert("RGB")
            ImageDraw.Draw(image).rectangle((400, 20, 560, 120), fill=(255, 210, 70))
            image.save(target)
        self.mutate(leak, "wall leakage")

    def test_advancement_toast_is_rejected(self):
        def toast(screenshots, _manifest):
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            image = Image.open(target).convert("RGB")
            ImageDraw.Draw(image).rectangle((1000, 20, 1260, 100), fill=(20, 20, 20))
            image.save(target)
        self.mutate(toast, "toast or overlay")

    def test_near_silhouette_obstructing_body_is_rejected(self):
        def obstruct(screenshots, _manifest):
            target = next(screenshots.glob("*near-flight.png"))
            image = Image.open(target).convert("RGB")
            ImageDraw.Draw(image).rectangle(NEAR_BODY_ROI, fill=(255, 255, 255))
            image.save(target)
        self.mutate(obstruct, "near body obstruction")

    def test_gradle_profiles_emit_valid_exact_client_entrypoints(self):
        java_home = "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
        env = os.environ.copy()
        env["JAVA_HOME"] = java_home

        def entrypoints(enabled: bool) -> list[str]:
            command = ["./gradlew", "processGametestResources", "--rerun-tasks",
                       "--no-daemon", "--console=plain"]
            if enabled:
                command.append("-Pvfx005ClientOnly")
            completed = subprocess.run(command, cwd=ROOT, env=env, text=True,
                                       capture_output=True, check=False)
            self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
            rendered = ROOT / "build/resources/gametest/fabric.mod.json"
            return json.loads(rendered.read_text(encoding="utf-8"))["entrypoints"][
                "fabric-client-gametest"]

        self.assertEqual([
            "com.powers.gametest.PowersClientGameTests",
            "com.powers.client.VfxGalleryClientGameTests",
            "com.powers.client.LightRealmSkyClientGameTests",
        ], entrypoints(False))
        self.assertEqual([
            "com.powers.client.RankTenSilhouetteClientGameTests",
        ], entrypoints(True))


if __name__ == "__main__":
    unittest.main()
