#!/usr/bin/env python3
"""Generate the twelve bounded authored realm templates and their Java piece catalogue."""

from __future__ import annotations

import gzip
import struct
from collections import OrderedDict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
STRUCTURES = ROOT / "src/main/resources/data/powers/structure/realm"
CATALOGUE = ROOT / "src/main/java/com/powers/realm/RealmLandmarkTemplates.java"
MAX_PIECE_BLOCKS = 128
MAX_SITE_BLOCKS = 2_048
SITES = ("archive", "labyrinth", "shrine", "settlement", "font", "herald_court")


@dataclass(frozen=True)
class Block:
    x: int
    y: int
    z: int
    name: str
    properties: tuple[tuple[str, str], ...] = ()
    nbt: dict[str, object] | None = None


def palette(alignment: str) -> dict[str, str]:
    if alignment == "light":
        return {"floor": "powers:pure_light", "wall": "minecraft:quartz_bricks",
                "accent": "minecraft:gold_block", "lamp": "minecraft:sea_lantern",
                "shelves": "minecraft:chiseled_bookshelf", "core": "minecraft:sea_lantern",
                "hazard": "minecraft:powder_snow"}
    return {"floor": "powers:darkness", "wall": "minecraft:polished_blackstone_bricks",
            "accent": "minecraft:crying_obsidian", "lamp": "minecraft:soul_lantern",
            "shelves": "minecraft:bookshelf", "core": "minecraft:crying_obsidian",
            "hazard": "minecraft:magma_block"}


def authored(alignment: str, site: str) -> list[Block]:
    """Create one deliberately authored layout; this runs only during asset generation."""
    p = palette(alignment)
    blocks: OrderedDict[tuple[int, int, int], Block] = OrderedDict()

    def put(x: int, y: int, z: int, name: str, properties: dict[str, str] | None = None,
            nbt: dict[str, object] | None = None) -> None:
        blocks[(x, y, z)] = Block(x, y, z, name,
                                   tuple(sorted((properties or {}).items())), nbt)

    def platform(radius: int) -> None:
        for x in range(-radius, radius + 1):
            for z in range(-radius, radius + 1):
                if x * x + z * z <= radius * radius:
                    put(x, 0, z, p["floor"])

    if site == "archive":
        platform(9)
        for x in range(-7, 8):
            for z in range(-5, 6):
                put(x, 1, z, p["floor"])
                edge = abs(x) == 7 or abs(z) == 5
                for y in range(2, 6):
                    if edge and not (z == -5 and abs(x) <= 1 and y <= 3): put(x, y, z, p["wall"])
                if abs(x) <= 6 and z in (-4, 4) and x % 2 == 0: put(x, 2, z, p["shelves"])
                if (x + z) % 3 == 0: put(x, 6, z, p["wall"])
        for x in (-6, 6):
            for z in (-4, 4): put(x, 5, z, p["lamp"])
        put(0, 2, 3, "minecraft:lectern", {"facing": "north", "has_book": "false"})
    elif site == "labyrinth":
        for x in range(-10, 11):
            for z in range(-10, 11):
                put(x, 0, z, p["floor"])
                boundary = abs(x) == 10 or abs(z) == 10
                inner = ((x % 2 == 0 and z % 2 == 0)
                         or (x % 4 == 0 and (z + x * 3) % 7 != 0)
                         or (z % 4 == 0 and (x - z * 5) % 7 != 0))
                if (boundary or inner) and not (z == -10 and abs(x) <= 1) and not (abs(x) <= 1 and abs(z) <= 1):
                    put(x, 1, z, p["wall"]); put(x, 2, z, p["wall"])
        for x in (-8, 0, 8):
            put(x, 1, 8 if x else -7, "minecraft:lever", {"face": "floor", "facing": "north", "powered": "false"})
    elif site == "shrine":
        platform(8)
        for radius, y in ((6, 1), (4, 2), (2, 3)):
            for x in range(-radius, radius + 1):
                for z in range(-radius, radius + 1): put(x, y, z, p["accent"] if y == 3 else p["wall"])
        for x in (-6, 6):
            for z in (-6, 6):
                for y in range(1, 7): put(x, y, z, p["wall"])
                put(x, 7, z, p["lamp"])
        for x, z in ((0, -5), (5, 0), (0, 5), (-5, 0)):
            put(x, 1, z, "minecraft:chiseled_copper")
    elif site == "settlement":
        platform(10)
        for ox, oz in ((-6, -4), (6, -4), (0, 6)):
            for x in range(-3, 4):
                for z in range(-3, 4):
                    put(ox + x, 1, oz + z, p["floor"])
                    for y in range(2, 5):
                        if (abs(x) == 3 or abs(z) == 3) and not (z == 3 and x == 0 and y <= 3):
                            put(ox + x, y, oz + z, p["wall"])
                    put(ox + x, 5 + max(abs(x), abs(z)) // 3, oz + z, p["accent"])
            put(ox, 3, oz, p["lamp"])
        put(0, 1, 0, "minecraft:bell", {"attachment": "floor", "facing": "north", "powered": "false"})
    elif site == "font":
        platform(10)
        for x in range(-7, 8):
            for z in range(-7, 8):
                distance = x * x + z * z
                if distance <= 45: put(x, 1, z, p["wall"] if distance >= 32 else p["hazard"])
                if distance == 49: put(x, 2, z, p["accent"])
        for y in range(2, 8): put(0, y, 0, p["accent"])
        put(0, 8, 0, p["lamp"])
        for x, z in ((3, 0), (-3, 0), (0, 3), (0, -3)): put(x, 2, z, "minecraft:amethyst_block")
    else:
        platform(12)
        for x in range(-12, 13):
            for z in range(-12, 13):
                distance = x * x + z * z
                if 121 <= distance <= 144:
                    put(x, 1, z, p["wall"])
                    if (x + z) % 3 == 0: put(x, 2, z, p["wall"])
        for x, z in ((-8, -8), (-8, 8), (8, -8), (8, 8)):
            for y in range(1, 9): put(x, y, z, p["accent"])
            put(x, 9, z, p["lamp"])
        put(0, 1, -6, "minecraft:heavy_core")

    put(0, 1, 0, p["core"])
    # Each site has a real, deterministic reward container and an authored interaction clue.
    put(2, 1, 2, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": "false"},
        {"LootTable": "powers:chests/realm_memory", "LootTableSeed": 31 + SITES.index(site)})
    put(-2, 1, 2, "minecraft:stone_button", {"face": "floor", "facing": "north", "powered": "false"})
    result = list(blocks.values())
    if not 1 <= len(result) <= MAX_SITE_BLOCKS:
        raise ValueError(f"{alignment}/{site} has {len(result)} blocks")
    return result


def write_name(stream, name: str) -> None:
    encoded = name.encode("utf-8"); stream.extend(struct.pack(">H", len(encoded))); stream.extend(encoded)


def tag_header(stream, tag: int, name: str) -> None:
    stream.append(tag); write_name(stream, name)


def write_string_payload(stream, value: str) -> None:
    write_name(stream, value)


def write_compound_payload(stream, values: dict[str, object]) -> None:
    for name, value in values.items():
        if isinstance(value, str):
            tag_header(stream, 8, name); write_string_payload(stream, value)
        elif isinstance(value, int):
            tag_header(stream, 4, name); stream.extend(struct.pack(">q", value))
        else:
            raise TypeError(value)
    stream.append(0)


def structure_nbt(blocks: list[Block]) -> bytes:
    min_x = min(block.x for block in blocks); min_y = min(block.y for block in blocks); min_z = min(block.z for block in blocks)
    normalized = [Block(b.x - min_x, b.y - min_y, b.z - min_z, b.name, b.properties, b.nbt) for b in blocks]
    size = (max(b.x for b in normalized) + 1, max(b.y for b in normalized) + 1, max(b.z for b in normalized) + 1)
    states: list[tuple[str, tuple[tuple[str, str], ...]]] = []
    for block in normalized:
        key = (block.name, block.properties)
        if key not in states: states.append(key)
    out = bytearray((10, 0, 0))
    tag_header(out, 3, "DataVersion"); out.extend(struct.pack(">i", 0))
    tag_header(out, 11, "size"); out.extend(struct.pack(">i", 3)); out.extend(struct.pack(">iii", *size))
    tag_header(out, 9, "palette"); out.append(10); out.extend(struct.pack(">i", len(states)))
    for name, properties in states:
        tag_header(out, 8, "Name"); write_string_payload(out, name)
        if properties:
            tag_header(out, 10, "Properties")
            for key, value in properties:
                tag_header(out, 8, key); write_string_payload(out, value)
            out.append(0)
        out.append(0)
    tag_header(out, 9, "blocks"); out.append(10); out.extend(struct.pack(">i", len(normalized)))
    for block in normalized:
        tag_header(out, 9, "pos"); out.append(3); out.extend(struct.pack(">i", 3)); out.extend(struct.pack(">iii", block.x, block.y, block.z))
        tag_header(out, 3, "state"); out.extend(struct.pack(">i", states.index((block.name, block.properties))))
        if block.nbt:
            tag_header(out, 10, "nbt"); write_compound_payload(out, block.nbt)
        out.append(0)
    tag_header(out, 9, "entities"); out.append(10); out.extend(struct.pack(">i", 0)); out.append(0)
    return gzip.compress(bytes(out), mtime=0)


def main() -> None:
    STRUCTURES.mkdir(parents=True, exist_ok=True)
    java_rows: list[str] = []
    for alignment in ("light", "dark"):
        for site_index, site in enumerate(SITES, start=1):
            blocks = sorted(authored(alignment, site), key=lambda b: (b.y, b.x, b.z))
            pieces: list[str] = []
            for piece_index, start in enumerate(range(0, len(blocks), MAX_PIECE_BLOCKS)):
                piece = blocks[start:start + MAX_PIECE_BLOCKS]
                name = f"{alignment}/{site}/piece_{piece_index:02d}"
                target = STRUCTURES / f"{name}.nbt"
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(structure_nbt(piece))
                min_x = min(block.x for block in piece); min_y = min(block.y for block in piece); min_z = min(block.z for block in piece)
                pieces.append(f'new Piece(PowersMod.id("realm/{name}"), {len(piece)}, {min_x}, {min_y}, {min_z})')
            site_id = f"{alignment}_memory_{site_index}"
            java_rows.append(f'\t\tMap.entry("{site_id}", List.of(\n\t\t\t\t' + ",\n\t\t\t\t".join(pieces) + "))")

    source = """package com.powers.realm;

import com.powers.PowersMod;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/** Generated bounded piece catalogue for the twelve authored realm structures. */
public final class RealmLandmarkTemplates {
\tpublic static final int MAX_SITE_BLOCKS = 2_048;
\tpublic static final int MAX_PIECE_BLOCKS = 128;
\tprivate static final Map<String, List<Piece>> PIECES = Map.ofEntries(
""" + ",\n".join(java_rows) + """
\t);

\tprivate RealmLandmarkTemplates() {
\t}

\t/** Ordered pieces for one stable MemorySite identifier. */
\tpublic static List<Piece> pieces(String siteId) {
\t\treturn PIECES.getOrDefault(siteId, List.of());
\t}

\t/** One structure-template piece and its placement offset from site centre/floor. */
\tpublic record Piece(Identifier template, int blocks, int offsetX, int offsetY, int offsetZ) {
\t\tpublic Piece {
\t\t\tif (template == null || blocks < 1 || blocks > MAX_PIECE_BLOCKS) {
\t\t\t\tthrow new IllegalArgumentException("Invalid realm template piece");
\t\t\t}
\t\t}
\t}
}
"""
    CATALOGUE.write_text(source, encoding="utf-8")


if __name__ == "__main__":
    main()
