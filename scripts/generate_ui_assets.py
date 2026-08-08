#!/usr/bin/env python3
"""Generate deterministic pixel assets for POWERS' ancient-magic interfaces."""

from __future__ import annotations

import math
import random
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/powers/textures"
GUI = TEXTURES / "gui"
EFFECTS = TEXTURES / "mob_effect"
ADVANCEMENTS = GUI / "advancements"
PARTICLES = TEXTURES / "particle"


def save(image: Image.Image, path: Path) -> None:
    """Write an RGBA PNG after creating its namespace directory."""
    path.parent.mkdir(parents=True, exist_ok=True)
    if image.mode != "RGBA":
        image = image.convert("RGBA")
    image.save(path, format="PNG", optimize=True)


def diamond(draw: ImageDraw.ImageDraw, center: tuple[int, int], radius: int,
            fill: tuple[int, int, int, int], outline: tuple[int, int, int, int] | None = None) -> None:
    """Draw a crisp four-point crystal suited to low-resolution GUI art."""
    cx, cy = center
    points = [(cx, cy - radius), (cx + radius, cy), (cx, cy + radius), (cx - radius, cy)]
    draw.polygon(points, fill=fill, outline=outline)


def line_rune(draw: ImageDraw.ImageDraw, center: tuple[int, int], radius: int,
              color: tuple[int, int, int, int]) -> None:
    """Draw the mod's original four-axis sigil without relying on text glyphs."""
    cx, cy = center
    draw.line((cx - radius, cy, cx + radius, cy), fill=color, width=1)
    draw.line((cx, cy - radius, cx, cy + radius), fill=color, width=1)
    diamond(draw, center, max(1, radius // 3), color)
    for dx, dy in ((-radius, 0), (radius, 0), (0, -radius), (0, radius)):
        diamond(draw, (cx + dx, cy + dy), 1, color)


def energy_frame() -> Image.Image:
    """Create a horizontal carved reliquary with a transparent fill aperture."""
    image = Image.new("RGBA", (172, 22), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((6, 3, 165, 18), radius=4, fill=(9, 12, 18, 224),
                           outline=(46, 52, 61, 255), width=2)
    draw.rectangle((13, 6, 158, 15), fill=(3, 5, 9, 244), outline=(104, 109, 118, 255))
    draw.line((14, 5, 157, 5), fill=(190, 181, 163, 180), width=1)
    # Asymmetric scratches keep the metal ancient while retaining a clean silhouette.
    for x in (23, 42, 73, 112, 139):
        draw.point((x, 4), fill=(119, 111, 100, 210))
        draw.point((171 - x, 17), fill=(72, 77, 86, 230))
    for x in (3, 168):
        diamond(draw, (x, 11), 3, (35, 40, 49, 255), (151, 146, 135, 255))
    diamond(draw, (9, 11), 5, (13, 18, 27, 255), (138, 144, 153, 255))
    diamond(draw, (163, 11), 5, (13, 18, 27, 255), (138, 144, 153, 255))
    return image


def energy_fill() -> Image.Image:
    """Create five eight-pixel-high mode rows used by the clipped energy fill."""
    image = Image.new("RGBA", (144, 40), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    palettes = [
        ((19, 103, 132, 255), (98, 230, 255, 255)),
        ((55, 5, 8, 255), (205, 55, 49, 255)),
        ((54, 21, 80, 255), (190, 105, 255, 255)),
        ((15, 5, 38, 255), (112, 62, 207, 255)),
        ((35, 83, 126, 255), (180, 242, 255, 255)),
    ]
    for row, (dark, light) in enumerate(palettes):
        y = row * 8
        draw.rectangle((0, y, 143, y + 7), fill=dark)
        draw.line((0, y + 1, 143, y + 1), fill=light)
        draw.line((0, y + 6, 143, y + 6), fill=tuple(max(0, c - 35) for c in light[:3]) + (255,))
        for x in range(4 + row, 144, 13):
            draw.point((x, y + 3 + (x // 13) % 2), fill=light)
    # Empty and amethyst rows receive readable, state-specific fracture marks.
    for row, color in ((1, (255, 104, 76, 255)), (2, (236, 190, 255, 255))):
        y = row * 8
        for x in range(10, 140, 24):
            draw.line((x, y, x + 3, y + 3, x + 1, y + 7), fill=color, width=1)
    return image


def power_slot(active: bool) -> Image.Image:
    """Create a compact round medallion used by all three power bindings."""
    image = Image.new("RGBA", (36, 36), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outer = (121, 113, 101, 255) if not active else (188, 229, 240, 255)
    glow = (49, 35, 73, 190) if not active else (53, 191, 224, 220)
    draw.ellipse((2, 2, 33, 33), fill=(8, 10, 15, 230), outline=outer, width=2)
    draw.ellipse((6, 6, 29, 29), fill=(17, 18, 26, 245), outline=(57, 53, 68, 255), width=1)
    for angle in range(0, 360, 45):
        radians = math.radians(angle)
        x = 18 + round(math.cos(radians) * 15)
        y = 18 + round(math.sin(radians) * 15)
        diamond(draw, (x, y), 2, (28, 31, 39, 255), outer)
    diamond(draw, (18, 18), 7, glow, outer)
    diamond(draw, (18, 18), 3, (10, 12, 18, 255), glow)
    return image


def ritual_panel(width: int, height: int, accent: tuple[int, int, int, int]) -> Image.Image:
    """Create a fixed-size stone-and-glass ritual panel for centered screens."""
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((2, 2, width - 3, height - 3), radius=8,
                           fill=(6, 8, 14, 232), outline=(79, 76, 86, 255), width=3)
    draw.rounded_rectangle((8, 8, width - 9, height - 9), radius=5,
                           fill=(13, 15, 24, 242), outline=(35, 39, 51, 255), width=2)
    draw.rectangle((15, 30, width - 16, height - 42), fill=(7, 10, 17, 178),
                   outline=(45, 50, 64, 220))
    line_rune(draw, (width // 2, 14), 7, accent)
    for center in ((10, 10), (width - 11, 10), (10, height - 11), (width - 11, height - 11)):
        diamond(draw, center, 4, (20, 22, 31, 255), accent)
    for x in range(26, width - 25, 19):
        draw.point((x, 22), fill=accent)
        draw.point((width - x, height - 22), fill=accent)
    return image


def advancement_tile(light: bool) -> Image.Image:
    """Create a seamless 256-pixel advancement canvas with restrained runes."""
    rng = random.Random(0x51A7 if light else 0xDA4C)
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 255))
    draw = ImageDraw.Draw(image)
    base = (203, 196, 169) if light else (25, 18, 37)
    mortar = (142, 134, 114, 255) if light else (52, 35, 69, 255)
    accent = (241, 208, 104, 150) if light else (144, 73, 207, 155)
    for y in range(256):
        for x in range(256):
            noise = rng.randrange(-10, 11)
            image.putpixel((x, y), tuple(max(0, min(255, channel + noise)) for channel in base) + (255,))
    # Offset masonry seams tile perfectly at all four boundaries.
    for y in range(0, 256, 16):
        draw.line((0, y, 255, y), fill=mortar)
        offset = 8 if (y // 16) % 2 else 0
        for x in range(offset, 256, 32):
            draw.line((x, y, x, min(255, y + 16)), fill=mortar)
    for cx, cy in ((32, 32), (96, 96), (160, 160), (224, 224), (32, 224), (224, 32)):
        for radius in (6, 12, 20):
            draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline=accent, width=1)
        line_rune(draw, (cx, cy), 8, accent)
    return image


def effect_icon(amethyst: bool) -> Image.Image:
    """Create an exact 18x18 status icon with transparent padding."""
    image = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if amethyst:
        dark = (66, 25, 91, 255)
        bright = (207, 122, 255, 255)
        diamond(draw, (9, 9), 7, dark, bright)
        diamond(draw, (9, 7), 4, (138, 62, 185, 255), (240, 201, 255, 255))
        draw.line((8, 2, 10, 6, 8, 9, 11, 13, 9, 16), fill=(255, 238, 255, 255), width=1)
        draw.point((4, 12), fill=bright)
        draw.point((14, 5), fill=bright)
    else:
        dark = (25, 12, 53, 255)
        bright = (123, 99, 203, 255)
        draw.ellipse((2, 2, 15, 15), fill=(7, 7, 14, 235), outline=dark, width=2)
        # A draining hourglass communicates reserves collapsing over time.
        draw.line((5, 4, 12, 4, 10, 8, 12, 13, 5, 13, 7, 8, 5, 4), fill=bright, width=2)
        diamond(draw, (9, 9), 2, (73, 45, 136, 255), (188, 171, 255, 255))
    return image


def particle_sprite(kind: str) -> Image.Image:
    """Create one crisp 16×16 shape channel for the semantic particle renderer."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    colors = {
        "mote": ((105, 219, 255, 90), (224, 252, 255, 255)),
        "shard": ((138, 58, 202, 120), (235, 191, 255, 255)),
        "glyph": ((49, 113, 171, 100), (158, 235, 255, 255)),
        "ribbon": ((78, 166, 193, 100), (202, 250, 255, 245)),
        "spark": ((225, 137, 46, 100), (255, 239, 162, 255)),
        "eclipse": ((93, 45, 133, 120), (255, 224, 139, 255)),
        "root": ((76, 61, 128, 110), (167, 139, 237, 255)),
        "fracture": ((160, 68, 210, 115), (249, 221, 255, 255)),
    }
    glow, bright = colors[kind]
    if kind == "mote":
        draw.ellipse((3, 3, 12, 12), fill=glow)
        diamond(draw, (8, 8), 3, bright)
    elif kind == "shard":
        draw.polygon(((8, 1), (12, 7), (9, 15), (5, 9)), fill=glow, outline=bright)
        draw.line((8, 3, 8, 12), fill=bright)
    elif kind == "glyph":
        draw.ellipse((1, 1, 14, 14), outline=glow, width=2)
        line_rune(draw, (8, 8), 5, bright)
    elif kind == "ribbon":
        draw.arc((1, 2, 14, 12), 190, 355, fill=glow, width=4)
        draw.arc((1, 2, 14, 12), 190, 355, fill=bright, width=1)
    elif kind == "spark":
        draw.line((8, 0, 7, 6, 2, 8, 7, 9, 8, 15, 9, 9, 14, 8, 9, 6, 8, 0),
                  fill=glow, width=3)
        draw.line((8, 1, 8, 14), fill=bright)
    elif kind == "eclipse":
        draw.ellipse((1, 1, 14, 14), fill=glow, outline=bright)
        draw.ellipse((5, 1, 14, 14), fill=(15, 7, 28, 245))
        draw.arc((1, 1, 14, 14), 72, 288, fill=bright, width=2)
    elif kind == "root":
        draw.line((8, 1, 8, 7, 3, 14), fill=glow, width=3)
        draw.line((8, 7, 13, 14), fill=glow, width=3)
        draw.line((8, 1, 8, 8, 3, 14), fill=bright)
        draw.line((8, 8, 13, 14), fill=bright)
    else:
        draw.line((8, 0, 6, 5, 9, 7, 4, 10, 8, 15), fill=glow, width=4)
        draw.line((8, 1, 7, 5, 10, 7, 5, 10, 8, 14), fill=bright)
    return image


def main() -> None:
    """Regenerate every UI texture from the checked-in deterministic recipe."""
    save(energy_frame(), GUI / "energy_frame.png")
    save(energy_fill(), GUI / "energy_fill.png")
    save(power_slot(False), GUI / "power_slot.png")
    save(power_slot(True), GUI / "power_slot_active.png")
    save(ritual_panel(256, 192, (104, 222, 255, 255)), GUI / "teleport_panel.png")
    save(ritual_panel(240, 224, (191, 155, 255, 255)), GUI / "locator_panel.png")
    save(advancement_tile(True), ADVANCEMENTS / "radiant_path.png")
    save(advancement_tile(False), ADVANCEMENTS / "shadow_path.png")
    save(effect_icon(False), EFFECTS / "exhaustion.png")
    save(effect_icon(True), EFFECTS / "amethyst_poisoning.png")
    for particle in ("mote", "shard", "glyph", "ribbon", "spark", "eclipse", "root", "fracture"):
        save(particle_sprite(particle), PARTICLES / f"{particle}.png")


if __name__ == "__main__":
    main()
