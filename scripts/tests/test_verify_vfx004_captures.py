#!/usr/bin/env python3

import importlib.util
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "verify_vfx004_captures", ROOT / "scripts/verify_vfx004_captures.py")
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)


class Vfx004CaptureVerifierTest(unittest.TestCase):
    def make_capture(self, path: Path, seed: int, scar: bool = True) -> None:
        image = Image.new("RGB", VERIFY.EXPECTED_SIZE, (30 + seed % 20, 70, 35))
        draw = ImageDraw.Draw(image)
        draw.rectangle(VERIFY.ROI, fill=(80 + seed % 50, 82, 102))
        for index in range(50):
            x = VERIFY.ROI[0] + (index * 17 + seed) % 130
            y = VERIFY.ROI[1] + (index * 11 + seed) % 60
            color = ((90 + index % 80,) * 3 if not scar else
                     (120 + (index * 3 + seed) % 100,
                      70 + index % 70, 90 + index % 90))
            draw.rectangle((x, y, x + 4, y + 4),
                           fill=color)
        if scar:
            draw.ellipse((595, 225, 685, 265), fill=(255, 120, 40))
        image.save(path)

    def make_set(self, root: Path) -> None:
        index = 1
        for impact in VERIFY.IMPACTS:
            for material in VERIFY.MATERIALS:
                self.make_capture(root / f"{index:04d}_vfx004-scar-matrix-{impact}-{material}.png", index)
                index += 1
        self.make_capture(root / f"{index:04d}_vfx004-scar-visible-front.png", index)
        index += 1
        self.make_capture(root / f"{index:04d}_vfx004-scar-occluded-wall.png", index, scar=False)
        index += 1
        self.make_capture(root / f"{index:04d}_vfx004-scar-post-resource-reload.png", index)

    def test_valid_exact_set_passes(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            self.make_set(root)
            self.assertEqual(30, len(VERIFY.validate(root)["matrixRows"]))

    def test_duplicate_matrix_frames_are_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            self.make_set(root)
            first = VERIFY.capture(root, "vfx004-scar-matrix-beam-stone")
            second = VERIFY.capture(root, "vfx004-scar-matrix-beam-earth")
            second.write_bytes(first.read_bytes())
            with self.assertRaisesRegex(ValueError, "duplicate frames"):
                VERIFY.validate(root)

    def test_wall_bleed_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            self.make_set(root)
            wall = VERIFY.capture(root, "vfx004-scar-occluded-wall")
            self.make_capture(wall, 99, scar=True)
            with self.assertRaisesRegex(ValueError, "opaque-wall"):
                VERIFY.validate(root)


if __name__ == "__main__":
    unittest.main()
