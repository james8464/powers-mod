#!/usr/bin/env python3
"""Generate deterministic two-tone pixel-art frames for the rank maze UI."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/powers/textures/gui/rank_maze"
BASE_SIZE = (256, 128)
FINAL_SIZE = (512, 256)


def generate(name: str, palette: dict[str, tuple[int, int, int, int]], dark: bool) -> None:
    image = Image.new("RGBA", BASE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    # A translucent, quiet field keeps the world visible without sacrificing node readability.
    draw.rectangle((4, 4, 251, 123), fill=palette["field"])
    for y in range(8, 120, 8):
        for x in range(8, 248, 8):
            if (x // 8 + y // 8) % 2 == 0:
                draw.rectangle((x, y, x + 7, y + 7), fill=palette["tile"])

    # Chunky nested frame and stepped corners, inspired by the approved concept pass.
    draw.rectangle((2, 2, 253, 125), outline=palette["shadow"], width=2)
    draw.rectangle((4, 4, 251, 123), outline=palette["edge"], width=2)
    draw.rectangle((7, 7, 248, 120), outline=palette["mid"], width=1)
    for corner_x in (4, 233):
        for corner_y in (4, 105):
            draw.rectangle((corner_x, corner_y, corner_x + 18, corner_y + 18),
                           fill=palette["corner"], outline=palette["edge"], width=2)
            diamond(draw, corner_x + 9, corner_y + 9, 5, palette["bright"])
            diamond(draw, corner_x + 9, corner_y + 9, 2, palette["shadow"])

    # Border-only labyrinth paths keep the graph's central playfield uncluttered.
    draw.line([(25, 8), (25, 13), (38, 13), (38, 9), (54, 9), (54, 14),
               (71, 14), (71, 10), (91, 10)], fill=palette["mid"], width=2)
    draw.line([(164, 10), (184, 10), (184, 14), (201, 14), (201, 9),
               (218, 9), (218, 14), (230, 14)], fill=palette["mid"], width=2)
    draw.line([(25, 119), (25, 114), (43, 114), (43, 118), (61, 118),
               (61, 113), (88, 113)], fill=palette["mid"], width=2)
    draw.line([(167, 114), (194, 114), (194, 118), (213, 118),
               (213, 113), (230, 113)], fill=palette["mid"], width=2)

    # A small identity rune leaves the advancement backgrounds visually independent.
    if dark:
        draw.polygon([(128, 5), (122, 11), (122, 17), (128, 23),
                      (134, 17), (134, 11)], fill=palette["corner"], outline=palette["edge"])
        draw.arc((124, 8, 135, 19), 70, 290, fill=palette["bright"], width=2)
    else:
        diamond(draw, 128, 14, 9, palette["corner"])
        draw.rectangle((127, 7, 129, 21), fill=palette["bright"])
        draw.rectangle((121, 13, 135, 15), fill=palette["bright"])
        diamond(draw, 128, 14, 3, palette["edge"])

    OUTPUT.mkdir(parents=True, exist_ok=True)
    image.resize(FINAL_SIZE, Image.Resampling.NEAREST).save(OUTPUT / f"{name}.png")


def diamond(draw: ImageDraw.ImageDraw, center_x: int, center_y: int,
            radius: int, color: tuple[int, int, int, int]) -> None:
    for offset_y in range(-radius, radius + 1):
        half = radius - abs(offset_y)
        draw.line((center_x - half, center_y + offset_y,
                   center_x + half, center_y + offset_y), fill=color)


generate("light_panel", {
    "field": (14, 17, 24, 182),
    "tile": (24, 25, 28, 182),
    "shadow": (52, 43, 30, 244),
    "edge": (246, 222, 151, 255),
    "mid": (157, 132, 77, 230),
    "bright": (255, 247, 205, 255),
    "corner": (78, 65, 39, 245),
}, dark=False)

generate("dark_panel", {
    "field": (10, 6, 15, 188),
    "tile": (18, 9, 24, 188),
    "shadow": (22, 10, 31, 248),
    "edge": (151, 86, 190, 255),
    "mid": (92, 48, 116, 235),
    "bright": (221, 173, 246, 255),
    "corner": (45, 22, 59, 248),
}, dark=True)
