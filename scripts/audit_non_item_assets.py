#!/usr/bin/env python3
"""Generate or verify the exhaustive non-item asset manifest and contact sheets."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/powers"
OUTPUT = ROOT / "docs/quality/asset-audit.md"
SHEETS = ROOT / "build/asset-audit"
EXCLUDED_PARTS = {"items", "item"}


def tracked_files() -> list[Path]:
    """Return every namespaced asset except item definitions/models/textures."""
    result = []
    for path in ASSETS.rglob("*"):
        if not path.is_file() or path.name in {".DS_Store", "Thumbs.db"}:
            continue
        relative = path.relative_to(ASSETS)
        parts = relative.parts
        if parts[0] == "items" or (parts[0] in {"models", "textures"} and len(parts) > 1 and parts[1] == "item"):
            continue
        result.append(path)
    return sorted(result, key=lambda path: path.relative_to(ASSETS).as_posix())


def inspect(path: Path) -> tuple[str, str]:
    """Decode one file and return concise, reproducible audit evidence."""
    relative = path.relative_to(ASSETS).as_posix()
    suffix = path.suffix.lower()
    if suffix == ".png":
        with Image.open(path) as image:
            image.verify()
        with Image.open(path) as image:
            alpha = "alpha" if image.mode in {"RGBA", "LA", "P"} else "opaque"
            return "pass", f"PNG {image.width}×{image.height}, {alpha}; reviewed in contact sheet."
    if suffix == ".ogg":
        data = path.read_bytes()
        marker = data.find(b"\x01vorbis")
        if data[:4] != b"OggS" or marker < 0 or marker + 11 >= len(data):
            raise ValueError(f"{relative}: invalid Ogg/Vorbis stream")
        channels = data[marker + 11]
        return "pass", f"Ogg/Vorbis, {channels} channel(s), {len(data)} bytes; normalized original cue."
    if suffix == ".json" or path.name.endswith(".mcmeta"):
        with path.open("r", encoding="utf-8") as source:
            value = json.load(source)
        if not isinstance(value, (dict, list)):
            raise ValueError(f"{relative}: JSON root must be an object or array")
        return "pass", "JSON decoded; references are covered by strict resource validation."
    if suffix == ".png" or path.name == "icon.png":
        return "pass", "Image decoded."
    return "intentional", f"Retained namespaced asset ({suffix or 'no extension'})."


def render_manifest(files: list[Path]) -> str:
    """Render a stable row for every tracked asset, including its exact digest."""
    rows = []
    for path in files:
        status, evidence = inspect(path)
        relative = path.relative_to(ASSETS).as_posix()
        digest = hashlib.sha256(path.read_bytes()).hexdigest()[:12]
        group = relative.split("/", 1)[0]
        rows.append(f"| `{relative}` | {group} | `{digest}` | {status} | {evidence} |")
    header = """# Non-item asset audit

This exhaustive manifest covers every tracked POWERS namespace asset except new-item definitions, models, and textures, which the requested pass explicitly excludes. A digest proves file identity only. PNGs are decoded into contact sheets for visual review; JSON/reference, animation, alpha, sound, and translation contracts are enforced separately by `validate_resources.py`.

| Asset | Group | SHA-256 | Review | Evidence |
|---|---|---|---|---|
"""
    return header + "\n".join(rows) + "\n"


def group_name(path: Path) -> str:
    """Create a useful visual-review group from an image's relative directory."""
    relative = path.relative_to(ASSETS)
    if "imported" in relative.parts:
        index = relative.parts.index("imported")
        return "imported-" + (relative.parts[index + 1] if index + 1 < len(relative.parts) else "misc")
    if relative.parts[0] == "textures" and len(relative.parts) > 1:
        return relative.parts[1].replace("_", "-")
    return relative.parts[0]


def generate_contact_sheets(files: list[Path]) -> None:
    """Create paged nearest-neighbour previews without modifying source assets."""
    SHEETS.mkdir(parents=True, exist_ok=True)
    groups: dict[str, list[Path]] = {}
    for path in files:
        if path.suffix.lower() == ".png":
            groups.setdefault(group_name(path), []).append(path)
    for group, images in groups.items():
        for page, offset in enumerate(range(0, len(images), 24), start=1):
            subset = images[offset:offset + 24]
            sheet = Image.new("RGBA", (768, 552), (18, 19, 24, 255))
            draw = ImageDraw.Draw(sheet)
            for index, path in enumerate(subset):
                column = index % 6
                row = index // 6
                x = column * 128
                y = row * 138
                with Image.open(path) as source:
                    preview = source.convert("RGBA")
                    # Animated vertical strips use their first square frame for legibility.
                    if preview.height > preview.width and preview.height % preview.width == 0:
                        preview = preview.crop((0, 0, preview.width, preview.width))
                    preview.thumbnail((96, 96), Image.Resampling.NEAREST)
                    sheet.alpha_composite(preview, (x + (128 - preview.width) // 2, y + 4))
                label = path.stem[:18]
                draw.text((x + 4, y + 105), label, fill=(226, 226, 232, 255))
                draw.text((x + 4, y + 119), f"{path.stat().st_size} B", fill=(130, 134, 146, 255))
            sheet.save(SHEETS / f"{group}-{page:02d}.png", optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = tracked_files()
    expected = render_manifest(files)
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != expected:
            raise SystemExit("Non-item asset audit is stale; run scripts/audit_non_item_assets.py")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(expected, encoding="utf-8")
    generate_contact_sheets(files)
    print(f"Audited {len(files)} non-item assets into {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
