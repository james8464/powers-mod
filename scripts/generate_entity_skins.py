#!/usr/bin/env python3
"""Generate valid 64x64 player-UV boss skins and the vanilla-scale energy atlas."""

from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ENTITY_DIR = ROOT / "src/main/resources/assets/powers/textures/entity"
GUI_DIR = ROOT / "src/main/resources/assets/powers/textures/gui"


def rect(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: str) -> None:
    """Pillow rectangles include their final pixel; resource UV boxes do not."""
    x0, y0, x1, y1 = box
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=color)


def pixel_noise(image: Image.Image, box: tuple[int, int, int, int], shades: tuple[str, ...], seed: int) -> None:
    """Add deterministic one-pixel material variation without breaking UV islands."""
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            value = (x * 17 + y * 31 + seed * 13) % 29
            if value < len(shades):
                image.putpixel((x, y), tuple(bytes.fromhex(shades[value][1:])) + (255,))


def base_skin(palette: dict[str, str]) -> Image.Image:
    """Paint every standard wide-arm player face; transparent pixels remain overlay-only."""
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    # Head, torso, right leg, right arm, left leg, left arm base UV faces.
    groups = (
        ((8, 0, 16, 8), (16, 0, 24, 8), (0, 8, 8, 16), (8, 8, 16, 16), (16, 8, 24, 16), (24, 8, 32, 16)),
        ((20, 16, 28, 20), (28, 16, 36, 20), (16, 20, 20, 32), (20, 20, 28, 32), (28, 20, 32, 32), (32, 20, 40, 32)),
        ((4, 16, 8, 20), (8, 16, 12, 20), (0, 20, 4, 32), (4, 20, 8, 32), (8, 20, 12, 32), (12, 20, 16, 32)),
        ((44, 16, 48, 20), (48, 16, 52, 20), (40, 20, 44, 32), (44, 20, 48, 32), (48, 20, 52, 32), (52, 20, 56, 32)),
        ((20, 48, 24, 52), (24, 48, 28, 52), (16, 52, 20, 64), (20, 52, 24, 64), (24, 52, 28, 64), (28, 52, 32, 64)),
        ((36, 48, 40, 52), (40, 48, 44, 52), (32, 52, 36, 64), (36, 52, 40, 64), (40, 52, 44, 64), (44, 52, 48, 64)),
    )
    for index, faces in enumerate(groups):
        for face in faces:
            rect(draw, face, palette["cloth"] if index else palette["skin"])
            pixel_noise(image, face, (palette["shade"], palette["glint"]), index + 1)
    return image


def first_vessel() -> Image.Image:
    image = base_skin({
        "skin": "#120a19", "cloth": "#160d20", "shade": "#0a0710", "glint": "#261234"
    })
    draw = ImageDraw.Draw(image)
    # Hollow silver-violet eyes and an ancient vertical face fracture.
    rect(draw, (9, 10, 11, 11), "#ede4ff")
    rect(draw, (13, 10, 15, 11), "#ede4ff")
    for x, y in ((12, 8), (12, 9), (11, 10), (12, 11), (11, 12), (11, 13), (10, 14)):
        draw.point((x, y), fill="#a86dff")
    # Robe sigil and asymmetric cracks across torso/limbs.
    for x, y in ((23, 21), (24, 22), (23, 23), (22, 24), (23, 25), (24, 26), (23, 27),
                 (5, 23), (6, 24), (5, 25), (45, 22), (46, 23), (45, 24),
                 (21, 55), (22, 56), (21, 57), (37, 56), (38, 57), (37, 58)):
        draw.point((x, y), fill="#7f3fc5")
    rect(draw, (20, 30, 28, 32), "#08050c")
    # Outer hood/crown and layered shoulder mantle use standard overlay UVs.
    rect(draw, (40, 8, 48, 16), "#08050c")
    rect(draw, (41, 8, 42, 12), "#7d6b94")
    rect(draw, (46, 8, 47, 12), "#7d6b94")
    rect(draw, (20, 36, 28, 48), "#0b0710")
    rect(draw, (16, 36, 20, 40), "#33203f")
    rect(draw, (28, 36, 32, 40), "#33203f")
    for x, y in ((23, 38), (24, 39), (23, 40), (22, 41), (23, 42), (24, 43)):
        draw.point((x, y), fill="#b384ff")
    return image


def radiant_sentinel() -> Image.Image:
    image = base_skin({
        "skin": "#fff7dc", "cloth": "#e8e1c5", "shade": "#c8b87e", "glint": "#ffffff"
    })
    draw = ImageDraw.Draw(image)
    # Luminous eyes and sun-mark face.
    rect(draw, (9, 10, 11, 11), "#ffffff")
    rect(draw, (13, 10, 15, 11), "#ffffff")
    draw.point((12, 8), fill="#ffd35a")
    draw.point((11, 9), fill="#ffd35a")
    draw.point((13, 9), fill="#ffd35a")
    # Gold breastplate sun and pale-blue light channels.
    rect(draw, (20, 20, 28, 22), "#9e7a24")
    for x, y in ((23, 22), (24, 22), (22, 23), (25, 23), (23, 24), (24, 24),
                 (22, 25), (25, 25), (23, 26), (24, 26), (23, 27), (24, 27)):
        draw.point((x, y), fill="#ffd45f")
    for x, y in ((5, 22), (5, 23), (6, 24), (5, 25), (46, 22), (46, 23), (45, 24), (46, 25),
                 (22, 54), (22, 55), (21, 56), (38, 54), (38, 55), (37, 56)):
        draw.point((x, y), fill="#8de7ff")
    # Halo/circlet and raised armour overlay.
    rect(draw, (40, 8, 48, 9), "#ffe281")
    rect(draw, (40, 9, 41, 13), "#d6ad38")
    rect(draw, (47, 9, 48, 13), "#d6ad38")
    rect(draw, (20, 36, 28, 48), "#fff9df")
    rect(draw, (16, 36, 20, 40), "#d4ab38")
    rect(draw, (28, 36, 32, 40), "#d4ab38")
    for x, y in ((23, 38), (24, 38), (22, 39), (25, 39), (23, 40), (24, 40)):
        draw.point((x, y), fill="#fff6a8")
    return image


def main() -> None:
    ENTITY_DIR.mkdir(parents=True, exist_ok=True)
    GUI_DIR.mkdir(parents=True, exist_ok=True)
    first_vessel().save(ENTITY_DIR / "first_vessel.png")
    radiant_sentinel().save(ENTITY_DIR / "radiant_sentinel.png")


if __name__ == "__main__":
    main()
