#!/usr/bin/env python3
"""Build deterministic four-frame Light/Dark pulses for Crucible conversion weapons."""

from __future__ import annotations

import argparse
import json
from enum import Enum
from pathlib import Path

from PIL import Image


class Alignment(Enum):
    DARK = "dark"
    LIGHT = "light"


WEAPONS = {
    Alignment.DARK: ("nocturne", "calamity_blade", "revenants_gravecleaver"),
    Alignment.LIGHT: ("solstice", "valhakyra", "zenith"),
}
PULSE_STRENGTHS = (0.0, 0.08, 0.18, 0.10)
TARGETS = {
    Alignment.DARK: (3, 2, 12),
    Alignment.LIGHT: (255, 255, 224),
}
ANIMATION = {"animation": {"frametime": 2, "frames": [0, 1, 2, 3, 2, 1]}}


def luminance(pixel: tuple[int, int, int, int]) -> int:
    red, green, blue, _alpha = pixel
    return red * 3 + green * 6 + blue


def build_sheet(source: Image.Image, alignment: Alignment) -> Image.Image:
    source = source.convert("RGBA")
    if source.width != source.height:
        raise ValueError(f"source frame must be square, got {source.width}x{source.height}")
    source.putdata([
        (0, 0, 0, 0) if alpha == 0 else (red, green, blue, alpha)
        for red, green, blue, alpha in source.get_flattened_data()
    ])
    sheet = Image.new("RGBA", (source.width, source.height * len(PULSE_STRENGTHS)))
    target = TARGETS[alignment]
    for index, strength in enumerate(PULSE_STRENGTHS):
        frame = source.copy()
        if strength:
            pixels = []
            for red, green, blue, alpha in frame.get_flattened_data():
                if alpha == 0:
                    pixels.append((red, green, blue, alpha))
                    continue
                pixels.append((
                    round(red + (target[0] - red) * strength),
                    round(green + (target[1] - green) * strength),
                    round(blue + (target[2] - blue) * strength),
                    alpha,
                ))
            frame.putdata(pixels)
        sheet.paste(frame, (0, index * source.height))
    return sheet


def generate(root: Path) -> None:
    texture_root = root / "src/main/resources/assets/powers/textures/item"
    for alignment, weapons in WEAPONS.items():
        for weapon in weapons:
            path = texture_root / f"{weapon}.png"
            with Image.open(path) as image:
                rgba = image.convert("RGBA")
            source = rgba.crop((0, 0, rgba.width, rgba.width)) if rgba.height != rgba.width else rgba
            build_sheet(source, alignment).save(path, optimize=False)
            metadata = path.with_name(path.name + ".mcmeta")
            metadata.write_text(json.dumps(ANIMATION, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    generate(args.root.resolve())


if __name__ == "__main__":
    main()
