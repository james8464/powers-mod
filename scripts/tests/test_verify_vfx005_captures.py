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
        baseline = Image.new("RGBA", VERIFY.EXPECTED_SIZE, (42, 67, 91, 255))
        baseline_draw = ImageDraw.Draw(baseline)
        baseline_draw.line((635, 360, 645, 360), fill=(220, 220, 220, 255), width=1)
        baseline_draw.line((640, 355, 640, 365), fill=(220, 220, 220, 255), width=1)
        self.add(rows, screenshots, baseline, "baseline", "size_shift", "radiant", 96,
                 False, "all", 0, VERIFY.BASE_EPOCH)
        for index, power_id in enumerate(VERIFY.POWER_IDS):
            normal = baseline.copy()
            self.draw_shape(normal, index, (255, 210, 70, 255))
            self.add(rows, screenshots, normal, "far_normal", power_id,
                     "radiant", 96, False, "all", 0, VERIFY.BASE_EPOCH)
        for index, power_id in enumerate(VERIFY.POWER_IDS):
            reduced = baseline.copy()
            self.draw_shape(reduced, index, (205, 175, 85, 255))
            self.add(rows, screenshots, reduced, "far_reduced", power_id,
                     "radiant", 96, True, "minimal", 0, VERIFY.BASE_EPOCH)
        for index, power_id in enumerate(VERIFY.ALIGNMENT_VARIANT_IDS):
            image = baseline.copy()
            self.draw_shape(image, VERIFY.POWER_IDS.index(power_id), (190, 30, 220, 255))
            self.add(rows, screenshots, image, "alignment_variant", power_id,
                     "darkness", 96, False, "all", 0, VERIFY.BASE_EPOCH)
        special = baseline.copy()
        self.draw_shape(special, 2, (255, 210, 70, 255), offset=(0, 70))
        body = ImageDraw.Draw(special)
        # Literal 4,704-pixel identity footprint measured from the fixed production actor.
        body.rectangle((620, 385, 657, 411), fill=(38, 42, 52, 255))
        body.rectangle((620, 412, 659, 452), fill=(38, 42, 52, 255))
        body.rectangle((620, 453, 658, 462), fill=(38, 42, 52, 255))
        body.rectangle((620, 463, 657, 463), fill=(38, 42, 52, 255))
        body.rectangle((622, 464, 656, 509), fill=(38, 42, 52, 255))
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
            self.draw_shape(image, VERIFY.POWER_IDS.index(power_id), (255, 210, 70, 255))
            self.add(rows, screenshots, image, category, power_id, "radiant", 96,
                     False, particles, revision, epoch)
        manifest.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
                            encoding="utf-8")
        return screenshots, manifest

    @staticmethod
    def draw_shape(image: Image.Image, index: int, color: tuple[int, int, int, int],
                   offset: tuple[int, int] = (0, 0)) -> None:
        draw = ImageDraw.Draw(image)
        cx, cy = 640 + offset[0], 412 + offset[1]
        # The production far-mask union is [628,401,652,424). Each mask shares
        # a compact anchor and receives a unique literal signature inside it.
        draw.line((cx, cy - 8, cx, cy + 8), fill=color, width=2)
        signature_x = 628 + index % 10 + offset[0]
        signature_y = 401 + index // 10 + offset[1]
        draw.rectangle((signature_x, signature_y, signature_x + 1, signature_y + 1),
                       fill=color)

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
            self.assertEqual(1.0, first["minimumAlignmentOutlineJaccard"])
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
            Image.new("RGBA", (640, 360), (0, 0, 0, 255)).save(path)
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
            image = Image.open(target).convert("RGBA")
            draw = ImageDraw.Draw(image)
            draw.rectangle((624, 397, 655, 427), fill=(255, 0, 0, 255))
            image.save(target)
        self.mutate(mismatch, "reduced outline mismatch")

    def test_crosshair_intrusion_is_rejected(self):
        def intrude(screenshots, _manifest):
            target = next(screenshots.glob("*near-flight.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle(VERIFY.CROSSHAIR_ROI, fill=(255, 210, 70, 255))
            image.save(target)
        self.mutate(intrude, "crosshair intrusion")

    def test_wall_leakage_is_rejected(self):
        def leak(screenshots, _manifest):
            target = next(screenshots.glob("*wall-forcefield.png"))
            image = Image.open(target).convert("RGBA")
            self.draw_shape(image, 15, (255, 210, 70, 255))
            image.save(target)
        self.mutate(leak, "wall leakage")

    def test_lifecycle_capture_with_reload_overlay_is_rejected(self):
        def overlay(screenshots, _manifest):
            target = next(screenshots.glob("*post_reload-void_beam.png"))
            Image.new("RGBA", VERIFY.EXPECTED_SIZE, (239, 50, 61, 255)).save(target)
        self.mutate(overlay, "background mismatch")

    def test_extra_screenshot_file_is_rejected(self):
        def extra(screenshots, _manifest):
            Image.new("RGBA", VERIFY.EXPECTED_SIZE, (0, 0, 0, 255)).save(
                screenshots / "extra.png")
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
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle((400, 20, 560, 120), fill=(239, 50, 61, 255))
            image.save(target)
        self.mutate(overlay, "background mismatch")

    def test_outside_roi_wall_leakage_is_rejected(self):
        def leak(screenshots, _manifest):
            target = next(screenshots.glob("*wall-forcefield.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle((400, 20, 560, 120), fill=(255, 210, 70, 255))
            image.save(target)
        self.mutate(leak, "wall leakage")

    def test_advancement_toast_is_rejected(self):
        def toast(screenshots, _manifest):
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle((1000, 20, 1260, 100), fill=(20, 20, 20, 255))
            image.save(target)
        self.mutate(toast, "toast or overlay")

    def test_near_silhouette_obstructing_body_is_rejected(self):
        def obstruct(screenshots, _manifest):
            target = next(screenshots.glob("*near-flight.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle(NEAR_BODY_ROI, fill=(255, 255, 255, 255))
            image.save(target)
        self.mutate(obstruct, "near body obstruction")

    def test_uniform_full_frame_tint_on_alignment_variant_is_rejected(self):
        def tint(screenshots, _manifest):
            target = next(screenshots.glob("*alignment_variant-flight.png"))
            image = Image.open(target).convert("RGBA")
            image = image.point(lambda channel: min(255, channel + 20))
            image.save(target)
        self.mutate(tint, "background mismatch")

    def test_overlay_inside_old_roi_outside_far_envelope_is_rejected(self):
        def overlay(screenshots, _manifest):
            target = next(screenshots.glob("*alignment_variant-flight.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle((500, 280, 560, 330),
                                            fill=(239, 50, 61, 255))
            image.save(target)
        self.mutate(overlay, "background mismatch")

    def test_far_foreground_includes_four_pixel_margin_below_y424(self):
        with tempfile.TemporaryDirectory() as raw:
            screenshots, manifest = self.make_fixture(Path(raw))
            for pattern in ("*far_normal-plant_healing_acceleration.png",
                            "*far_reduced-plant_healing_acceleration.png"):
                target = next(screenshots.glob(pattern))
                image = Image.open(target).convert("RGBA")
                ImageDraw.Draw(image).line((650, 424, 650, 428),
                                           fill=(255, 210, 70, 255), width=1)
                image.save(target)
            result = VERIFY.validate(screenshots, manifest)
            normal = next(row for row in result["rows"] if row["captureId"] ==
                          "vfx005-far_normal-plant_healing_acceleration")
            self.assertEqual(43, normal["foregroundPixels"])

    def test_overlay_immediately_below_far_envelope_is_rejected(self):
        def overlay(screenshots, _manifest):
            target = next(screenshots.glob("*alignment_variant-flight.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle((624, 429, 655, 437),
                                            fill=(239, 50, 61, 255))
            image.save(target)
        self.mutate(overlay, "background mismatch")

    def test_alignment_variant_outline_mismatch_is_rejected(self):
        def mismatch(screenshots, _manifest):
            baseline = next(screenshots.glob("*baseline-size_shift.png"))
            target = next(screenshots.glob("*alignment_variant-flight.png"))
            image = Image.open(baseline).convert("RGBA")
            ImageDraw.Draw(image).rectangle((624, 397, 629, 402),
                                            fill=(190, 30, 220, 255))
            image.save(target)
        self.mutate(mismatch, "alignment outline mismatch")

    def test_rgb_png_source_mode_is_rejected(self):
        def rgb_mode(screenshots, _manifest):
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            image = Image.open(target).convert("RGB")
            image.save(target, format="PNG")
        self.mutate(rgb_mode, "source mode")

    def test_transparent_hidden_rgb_is_rejected(self):
        def transparent(screenshots, _manifest):
            target = next(screenshots.glob("*alignment_variant-flight.png"))
            image = Image.open(target).convert("RGBA")
            red, green, blue, _ = image.getpixel((500, 300))
            image.putpixel((500, 300), (red, green, blue, 0))
            image.save(target)
        self.mutate(transparent, "fully opaque")

    def test_alpha_only_wall_difference_is_rejected(self):
        def alpha_difference(screenshots, _manifest):
            target = next(screenshots.glob("*wall-forcefield.png"))
            image = Image.open(target).convert("RGBA")
            red, green, blue, _ = image.getpixel((400, 20))
            image.putpixel((400, 20), (red, green, blue, 254))
            image.save(target)
        self.mutate(alpha_difference, "fully opaque")

    @staticmethod
    def set_background_drift(path: Path, pixel_count: int) -> None:
        image = Image.open(path).convert("RGBA")
        for index in range(pixel_count):
            x = 100 + index % 32
            y = 100 + index // 32
            red, green, blue, alpha = image.getpixel((x, y))
            image.putpixel((x, y), (min(255, red + 20), green, blue, alpha))
        image.save(path)

    def test_exactly_256_all_row_background_pixels_are_accepted(self):
        with tempfile.TemporaryDirectory() as raw:
            screenshots, manifest = self.make_fixture(Path(raw))
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            self.set_background_drift(target, 256)
            result = VERIFY.validate(screenshots, manifest)
            self.assertEqual(256, result["maximumBackgroundDriftPixels"])

    def test_257_all_row_background_pixels_are_rejected(self):
        def drift(screenshots, _manifest):
            target = next(screenshots.glob("*far_normal-size_shift.png"))
            self.set_background_drift(target, 257)
        self.mutate(drift, "background mismatch")

    def test_reciprocal_baseline_background_direction_is_rejected(self):
        def drift(screenshots, _manifest):
            target = next(screenshots.glob("*baseline-size_shift.png"))
            self.set_background_drift(target, 257)
        self.mutate(drift, "background mismatch")

    def test_one_channel_wall_leakage_is_rejected(self):
        def leak(screenshots, _manifest):
            target = next(screenshots.glob("*wall-forcefield.png"))
            image = Image.open(target).convert("RGBA")
            red, green, blue, alpha = image.getpixel((400, 20))
            image.putpixel((400, 20), (red + 1, green, blue, alpha))
            image.save(target)
        self.mutate(leak, "wall leakage")

    def test_partial_near_body_occlusion_is_rejected(self):
        def obstruct(screenshots, _manifest):
            target = next(screenshots.glob("*near-flight.png"))
            image = Image.open(target).convert("RGBA")
            ImageDraw.Draw(image).rectangle((628, 405, 651, 484),
                                            fill=(255, 255, 255, 255))
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
