#!/usr/bin/env python3
"""Generate deterministic 64x64 player skins for POWERS entities."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/powers/textures/entity"
ITEM_OUTPUT = ROOT / "src/main/resources/assets/powers/textures/item"


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


def herald_skin(light: bool) -> Image.Image:
    """Paint one fully opaque player UV with distinct force armour and eyes."""
    base = (224, 218, 185, 255) if light else (12, 5, 18, 255)
    shade = (164, 126, 42, 255) if light else (42, 15, 58, 255)
    rune = (255, 255, 238, 255) if light else (181, 77, 225, 255)
    eye = (113, 235, 255, 255) if light else (238, 74, 255, 255)
    image = Image.new("RGBA", (64, 64), base)
    draw = ImageDraw.Draw(image)
    # Tile shading keeps every limb face visibly mapped instead of stretching one panel.
    for y in range(0, 64, 8):
        for x in range(0, 64, 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 7, y + 7), fill=shade)
    # Head front (8..15, 8..15), torso front (20..27, 20..31), and limb-front runes.
    draw.rectangle((9, 11, 11, 12), fill=eye)
    draw.rectangle((13, 11, 15, 12), fill=eye)
    for x, y in ((23, 21), (22, 22), (23, 22), (24, 22), (23, 23),
                 (22, 24), (24, 24), (21, 25), (25, 25),
                 (6, 21), (10, 53), (42, 21), (26, 53)):
        draw.rectangle((x, y, x + 1, y + 1), fill=rune)
    # Outer-layer crown and shoulder seams remain in their standard 64x64 UV regions.
    draw.rectangle((40, 8, 47, 9), fill=rune)
    draw.rectangle((40, 16, 47, 16), fill=eye)
    draw.rectangle((20, 36, 27, 37), fill=rune)
    return image


def spawn_egg(base: str, shade: str, highlight: str, rune: str) -> Image.Image:
    """Paint one crisp vanilla-scale egg with an alignment rune instead of borrowing another mob."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    rows = {
        1: (7, 8), 2: (5, 10), 3: (4, 11), 4: (3, 12),
        5: (2, 13), 6: (2, 13), 7: (2, 13), 8: (2, 13),
        9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (4, 11),
        13: (5, 10), 14: (7, 8),
    }
    for y, (left, right) in rows.items():
        for x in range(left, right + 1):
            edge = x == left or x == right or y in {1, 14}
            color = shade if edge or x + y > 20 else base
            draw.point((x, y), fill=color)
    for x, y in ((5, 4), (6, 3), (4, 6), (10, 10), (11, 8)):
        draw.point((x, y), fill=highlight)
    # A compact forked rune stays legible at GUI scale and distinguishes custom eggs.
    for x, y in ((8, 5), (7, 6), (8, 6), (9, 6), (8, 7), (8, 8),
                 (7, 9), (8, 9), (9, 9), (7, 10), (9, 10)):
        draw.point((x, y), fill=rune)
    return image


def main() -> None:
    """Write reproducible semantic skins and self-contained operator spawn eggs."""
    OUTPUT.mkdir(parents=True, exist_ok=True)
    ITEM_OUTPUT.mkdir(parents=True, exist_ok=True)
    darkness_skin().save(OUTPUT / "darkness_player.png", format="PNG", optimize=True)
    test_actor_skin().save(OUTPUT / "test_actor.png", format="PNG", optimize=True)

    herald_skin(False).save(OUTPUT / "dark_herald.png", format="PNG", optimize=True)
    herald_skin(True).save(OUTPUT / "light_herald.png", format="PNG", optimize=True)
    eggs = {
        "darkness_creature_spawn_egg": ("#13091d", "#030105", "#713d8c", "#d38cff"),
        "power_test_actor_spawn_egg": ("#27303d", "#10151d", "#62dffc", "#ffd05c"),
        "radiant_sentinel_spawn_egg": ("#fff3c4", "#b78d2e", "#ffffff", "#69dff7"),
        "first_vessel_spawn_egg": ("#24102e", "#08040d", "#8e5ab0", "#e7ddff"),
        "dark_herald_spawn_egg": ("#210b2d", "#050108", "#7b3597", "#ec68ff"),
        "light_herald_spawn_egg": ("#fff1b0", "#9f7424", "#ffffff", "#70eaff"),
    }
    for name, palette in eggs.items():
        spawn_egg(*palette).save(ITEM_OUTPUT / f"{name}.png", format="PNG", optimize=True)


if __name__ == "__main__":
    main()
