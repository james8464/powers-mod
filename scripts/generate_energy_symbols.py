#!/usr/bin/env python3
"""Generate the exact 3×5 nine-pixel energy atlas used by the survival HUD."""

from pathlib import Path

from PIL import Image


OUTPUT = Path("src/main/resources/assets/powers/textures/gui/energy_symbols.png")
SIZE = 9
OUTLINE = {
    (4, 0), (2, 1), (3, 1), (4, 1), (5, 1), (6, 1),
    (1, 2), (2, 2), (6, 2), (7, 2),
    (1, 3), (7, 3), (1, 4), (7, 4), (1, 5), (7, 5),
    (1, 6), (2, 6), (6, 6), (7, 6),
    (2, 7), (3, 7), (4, 7), (5, 7), (6, 7), (4, 8),
}
INTERIOR = {
    (3, 2), (4, 2), (5, 2),
    (2, 3), (3, 3), (4, 3), (5, 3), (6, 3),
    (2, 4), (3, 4), (4, 4), (5, 4), (6, 4),
    (2, 5), (3, 5), (4, 5), (5, 5), (6, 5),
    (3, 6), (4, 6), (5, 6),
}

# shadow, empty interior, main fill, highlight, fracture accent
PALETTES = (
    ((24, 28, 31, 255), (52, 59, 61, 255), (72, 221, 219, 255), (210, 255, 241, 255), (255, 211, 92, 255)),
    ((31, 25, 32, 255), (54, 47, 56, 255), (122, 117, 126, 255), (211, 207, 216, 255), (148, 91, 161, 255)),
    ((31, 21, 39, 255), (54, 39, 65, 255), (180, 104, 219, 255), (246, 210, 255, 255), (116, 227, 255, 255)),
    ((17, 12, 25, 255), (37, 24, 51, 255), (112, 47, 150, 255), (225, 144, 255, 255), (32, 19, 45, 255)),
    ((18, 26, 39, 255), (31, 52, 68, 255), (71, 174, 217, 255), (211, 249, 255, 255), (139, 103, 218, 255)),
)


def draw_symbol(image: Image.Image, column: int, row: int, fill: int) -> None:
    """Draw empty (0), left-half (1), or full (2) in one atlas cell."""
    shadow, empty, main, shine, accent = PALETTES[row]
    x0, y0 = column * SIZE, row * SIZE
    for x, y in OUTLINE:
        image.putpixel((x0 + x, y0 + y), shadow)
    for x, y in INTERIOR:
        selected = fill == 2 or (fill == 1 and x <= 4)
        image.putpixel((x0 + x, y0 + y), main if selected else empty)

    # A two-pixel upper-left glint keeps the glyph readable at vanilla scale.
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
