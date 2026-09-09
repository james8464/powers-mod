import importlib.util
import unittest
from pathlib import Path

from PIL import Image


SCRIPT = Path(__file__).parents[1] / "generate_conversion_weapon_animations.py"
class ConversionWeaponAnimationTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(SCRIPT.is_file(), "conversion weapon animation generator is missing")
        spec = importlib.util.spec_from_file_location("conversion_weapon_animation", SCRIPT)
        self.module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.module)
        self.source = Image.new("RGBA", (2, 2))
        self.source.putdata([
            (40, 80, 160, 255),
            (180, 120, 40, 255),
            (20, 30, 40, 0),
            (240, 240, 240, 128),
        ])

    def test_first_frame_preserves_visible_pixels_and_normalizes_transparent_edges(self):
        sheet = self.module.build_sheet(self.source, self.module.Alignment.LIGHT)

        self.assertEqual((2, 8), sheet.size)
        self.assertEqual(self.source.getpixel((0, 0)), sheet.getpixel((0, 0)))
        self.assertEqual(self.source.getpixel((1, 0)), sheet.getpixel((1, 0)))
        self.assertEqual(self.source.getpixel((1, 1)), sheet.getpixel((1, 1)))
        for frame in range(4):
            self.assertEqual((0, 0, 0, 0), sheet.getpixel((0, frame * 2 + 1)))
            self.assertEqual(128, sheet.getpixel((1, frame * 2 + 1))[3])

    def test_alignment_peak_moves_luminance_in_opposite_directions(self):
        light = self.module.build_sheet(self.source, self.module.Alignment.LIGHT)
        dark = self.module.build_sheet(self.source, self.module.Alignment.DARK)

        base = sum(self.module.luminance(pixel)
                   for pixel in self.source.get_flattened_data() if pixel[3])
        light_peak = sum(self.module.luminance(light.getpixel((x, y + 4)))
                         for y in range(2) for x in range(2) if self.source.getpixel((x, y))[3])
        dark_peak = sum(self.module.luminance(dark.getpixel((x, y + 4)))
                        for y in range(2) for x in range(2) if self.source.getpixel((x, y))[3])

        self.assertGreater(light_peak, base)
        self.assertLess(dark_peak, base)


if __name__ == "__main__":
    unittest.main()
