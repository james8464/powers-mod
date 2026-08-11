#!/usr/bin/env python3
"""Generate the exact 3×5 nine-pixel energy atlas used by the survival HUD."""

from pathlib import Path

from PIL import Image


OUTPUT = Path("src/main/resources/assets/powers/textures/gui/energy_symbols.png")
SIZE = 9
OUTLINE = {
	(4, 0), (3, 1), (4, 1), (5, 1),
	(2, 2), (6, 2),
	(1, 3), (7, 3), (1, 4), (7, 4), (1, 5), (7, 5),
	(2, 6), (6, 6), (3, 7), (5, 7), (4, 8),
}
INTERIOR = {
	(3, 2), (4, 2), (5, 2),
	(2, 3), (3, 3), (4, 3), (5, 3), (6, 3),
	(2, 4), (3, 4), (4, 4), (5, 4), (6, 4),
	(2, 5), (3, 5), (4, 5), (5, 5), (6, 5),
	(3, 6), (4, 6), (5, 6), (4, 7),
}

# Vanilla-scale black edge, unfilled recess, main fill, lower shade, glint, state rune.
PALETTES = (
	((20, 24, 26, 255), (48, 55, 57, 255), (65, 218, 209, 255), (31, 145, 154, 255), (207, 255, 240, 255), (255, 208, 74, 255)),
	((28, 22, 29, 255), (55, 48, 57, 255), (119, 114, 124, 255), (77, 70, 82, 255), (211, 207, 216, 255), (158, 83, 173, 255)),
	((28, 18, 35, 255), (54, 37, 64, 255), (181, 100, 218, 255), (111, 53, 151, 255), (246, 211, 255, 255), (94, 224, 255, 255)),
	((14, 10, 21, 255), (36, 23, 49, 255), (108, 43, 146, 255), (61, 20, 91, 255), (224, 144, 255, 255), (25, 13, 38, 255)),
	((15, 23, 35, 255), (29, 49, 65, 255), (66, 169, 214, 255), (34, 103, 151, 255), (210, 248, 255, 255), (146, 105, 223, 255)),
)


def draw_symbol(image: Image.Image, column: int, row: int, fill: int) -> None:
    """Draw empty (0), left-half (1), or full (2) in one atlas cell."""
    shadow, empty, main, shade, shine, accent = PALETTES[row]
    x0, y0 = column * SIZE, row * SIZE
    for x, y in OUTLINE:
        image.putpixel((x0 + x, y0 + y), shadow)
    for x, y in INTERIOR:
        selected = fill == 2 or (fill == 1 and x <= 4)
        colour = shade if selected and (x >= 5 or y >= 6) else main
        image.putpixel((x0 + x, y0 + y), colour if selected else empty)

    # A two-pixel upper-left glint matches vanilla hearts, armour, and hunger shading.
    if fill:
        image.putpixel((x0 + 3, y0 + 2), shine)
        if fill == 2:
            image.putpixel((x0 + 2, y0 + 3), shine)

    # State-specific hairline rune: gold normal, amethyst crack, void seam, soul tether.
    if row == 0 and fill:
        image.putpixel((x0 + 5, y0 + 5), accent)
    elif row == 1:
        for x, y in ((4, 2), (3, 3), (4, 4), (3, 5), (4, 6)):
            image.putpixel((x0 + x, y0 + y), accent)
    elif row == 2:
        for x, y in ((5, 2), (4, 3), (5, 4), (4, 5)):
            image.putpixel((x0 + x, y0 + y), accent)
    elif row == 3 and fill:
        image.putpixel((x0 + 4, y0 + 3), accent)
        image.putpixel((x0 + 4, y0 + 4), accent)
    elif row == 4 and fill:
        image.putpixel((x0 + 5, y0 + 4), accent)


def generate_atlas() -> Image.Image:
    """Return the deterministic atlas for the aggregate UI generator and tests."""
    atlas = Image.new("RGBA", (SIZE * 3, SIZE * len(PALETTES)), (0, 0, 0, 0))
    for row in range(len(PALETTES)):
        for column, fill in enumerate((0, 1, 2)):
            draw_symbol(atlas, column, row, fill)
    return atlas


def main() -> None:
    atlas = generate_atlas()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(OUTPUT, optimize=True)


if __name__ == "__main__":
    main()
