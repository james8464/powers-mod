#!/usr/bin/env python3
"""Generate deterministic 64x64 player skins for POWERS entities."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/powers/textures/entity"


def darkness_skin() -> Image.Image:
    """Return a light-absorbing, completely black player canvas."""
    return Image.new("RGBA", (64, 64), (1, 0, 3, 255))


def test_actor_skin() -> Image.Image:
    """Return a high-contrast arcane mannequin skin for single-player tests."""
    image = Image.new("RGBA", (64, 64), (31, 34, 42, 255))
    draw = ImageDraw.Draw(image)
    for y in range(0, 64, 8):
        for x in range(0, 64, 8):
            if (x // 8 + y // 8) % 2 == 0:
                draw.rectangle((x, y, x + 7, y + 7), fill=(50, 55, 68, 255))
    draw.rectangle((8, 10, 15, 11), fill=(87, 231, 255, 255))
    draw.rectangle((16, 20, 23, 21), fill=(255, 196, 82, 255))
    return image


def main() -> None:
    """Write both uncompressed semantic entity skins."""
    OUTPUT.mkdir(parents=True, exist_ok=True)
    darkness_skin().save(OUTPUT / "darkness_player.png", format="PNG", optimize=True)
    test_actor_skin().save(OUTPUT / "test_actor.png", format="PNG", optimize=True)


if __name__ == "__main__":
    main()
