#!/usr/bin/env python3
"""Build a bounded, traceable VFX-011 client evidence bundle from an exact gallery run."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


TILE_W = 256
TILE_H = 164
IMAGE_H = 144
COLUMNS = 5
ROWS = 4
REPRESENTATIVES = (
    "model/item/solstice/gui",
    "model/item/solstice/firstperson_righthand",
    "model/item/solstice/thirdperson_righthand",
    "item/light_herald_spawn_egg/gui",
    "entity/light_herald/front",
    "entity/dark_herald/front",
    "entity/first_vessel/front",
    "entity/radiant_sentinel/front",
    "entity/shadow/wide/overlay/front",
    "entity/shadow/slim/overlay/front",
    "screen/artifact_catalogue/selected/scale4/normal/wide",
    "screen/grimoire_index/preview/scale4/normal/wide",
    "screen/artifact_catalogue/hover/scale2/normal/wide",
    "hud/energy/normal/half10/normal",
    "hud/combination/vanilla_mount/normal",
    "gameplay/first_person/solstice",
    "gameplay/third_person/solstice",
    "boss/light_herald/progress72",
    "boss/dark_herald/progress38",
    "boss/first_vessel/last_covenant/progress14",
)
RUNTIME_OPTION_TYPES = {
    "physicalWidth": int,
    "physicalHeight": int,
    "requestedGuiScale": int,
    "effectiveGuiScale": int,
    "mipLevel": int,
    "particles": str,
    "screenEffectScale": (int, float),
    "reducedMotion": bool,
    "renderDistance": int,
    "graphicsMode": str,
    "resourcePacks": list,
    "gameTime": int,
    "weather": str,
}


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_rows(path: Path) -> list[dict]:
    rows = [json.loads(line) for line in path.read_text().splitlines() if line.strip()]
    for row in rows:
        emitted_digest = row.get("screenshotSha256")
        runtime_options = row.get("runtimeOptions")
        valid_runtime = isinstance(runtime_options, dict) and set(runtime_options) == set(RUNTIME_OPTION_TYPES)
        if valid_runtime:
            valid_runtime = all(isinstance(runtime_options[key], expected)
                                and (expected is bool or not isinstance(runtime_options[key], bool))
                                for key, expected in RUNTIME_OPTION_TYPES.items())
        if (not isinstance(emitted_digest, str) or len(emitted_digest) != 64
                or any(character not in "0123456789abcdef" for character in emitted_digest)
                or not valid_runtime):
            raise ValueError("capture metadata lacks client-emitted screenshot digest/runtime options")
    screenshots = [row["screenshot"] for row in rows]
    if len(screenshots) != len(set(screenshots)):
        raise ValueError("duplicate screenshot in capture metadata")
    capture_ids = [capture_id for row in rows for capture_id in row["captureIds"]]
    if len(capture_ids) != len(set(capture_ids)):
        raise ValueError("duplicate capture ID in capture metadata")
    return rows


def validate_raw_screenshots(rows: list[dict], screenshots: Path) -> None:
    for row in rows:
        source = screenshots / row["screenshot"]
        if not source.is_file():
            raise FileNotFoundError(source)
        actual = digest(source)
        if actual != row["screenshotSha256"]:
            raise ValueError(f"raw screenshot digest mismatch: {row['screenshot']}")


def retain_raw_screenshots(rows: list[dict], screenshots: Path, raw: Path) -> list[str]:
    raw.mkdir(parents=True, exist_ok=True)
    lines = ["screenshot\tsha256\tcontent_path"]
    for row in rows:
        sha256 = row["screenshotSha256"]
        content_name = f"{sha256}.png"
        content_path = raw / content_name
        shutil.copyfile(screenshots / row["screenshot"], content_path)
        if digest(content_path) != sha256:
            raise ValueError(f"retained raw screenshot digest mismatch: {row['screenshot']}")
        lines.append(f"{row['screenshot']}\t{sha256}\tclient-raw/{content_name}")
    return lines


def build(captures: Path, screenshots: Path, output: Path,
          implementation_commit: str, jar: Path) -> None:
    rows = load_rows(captures)
    if len(rows) != 971:
        raise ValueError(f"expected exact 971-row gallery run, found {len(rows)}")
    validate_raw_screenshots(rows, screenshots)
    if (len(implementation_commit) != 40
            or any(character not in "0123456789abcdef" for character in implementation_commit)):
        raise ValueError("implementation commit must be an exact lowercase SHA-1")
    if not jar.is_file():
        raise FileNotFoundError(jar)
    output.mkdir(parents=True, exist_ok=True)
    sheets = output / "client-contact-sheets"
    representatives = output / "representative-full-resolution"
    raw = output / "client-raw"
    sheets.mkdir(exist_ok=True)
    representatives.mkdir(exist_ok=True)
    raw.mkdir(exist_ok=True)
    for directory in (sheets, representatives, raw):
        for stale in directory.iterdir():
            if stale.is_file():
                stale.unlink()

    emitted_metadata = output / "client-emitted-captures.jsonl"
    shutil.copyfile(captures, emitted_metadata)
    raw_lines = retain_raw_screenshots(rows, screenshots, raw)
    raw_index = output / "client-raw-index.tsv"
    raw_index.write_text("\n".join(raw_lines) + "\n")

    font = ImageFont.load_default()
    index_lines = ["capture_id\tscreenshot\tpage\tslot\tx\ty\twidth\theight\tsha256"]
    per_page = COLUMNS * ROWS
    for page_start in range(0, len(rows), per_page):
        page_number = page_start // per_page
        page_name = f"client-{page_number:03d}.png"
        page = Image.new("RGB", (COLUMNS * TILE_W, ROWS * TILE_H), (22, 22, 26))
        draw = ImageDraw.Draw(page)
        for local, row in enumerate(rows[page_start:page_start + per_page]):
            source = screenshots / row["screenshot"]
            if not source.is_file():
                raise FileNotFoundError(source)
            with Image.open(source) as opened:
                image = opened.convert("RGB")
                image.thumbnail((TILE_W, IMAGE_H), Image.Resampling.LANCZOS)
            x = local % COLUMNS * TILE_W
            y = local // COLUMNS * TILE_H
            page.paste(image, (x + (TILE_W - image.width) // 2, y + (IMAGE_H - image.height) // 2))
            label = f"{page_start + local:04d} {row['screenshot'][:34]}"
            draw.rectangle((x, y + IMAGE_H, x + TILE_W - 1, y + TILE_H - 1), fill=(0, 0, 0))
            draw.text((x + 3, y + IMAGE_H + 3), label, font=font, fill=(255, 255, 255))
            for capture_id in row["captureIds"]:
                index_lines.append("\t".join((capture_id, row["screenshot"], page_name, str(local),
                        str(x), str(y), str(TILE_W), str(TILE_H), digest(source))))
        page.save(sheets / page_name, format="PNG", optimize=False, compress_level=9)

    (output / "client-capture-index.tsv").write_text("\n".join(index_lines) + "\n")
    by_id = {capture_id: row for row in rows for capture_id in row["captureIds"]}
    representative_lines = ["capture_id\tscreenshot\tsha256"]
    for capture_id in REPRESENTATIVES:
        if capture_id not in by_id:
            raise ValueError(f"missing representative capture ID: {capture_id}")
        row = by_id[capture_id]
        source = screenshots / row["screenshot"]
        target = representatives / row["screenshot"]
        shutil.copyfile(source, target)
        representative_lines.append(f"{capture_id}\t{target.name}\t{digest(target)}")
    (output / "representative-index.tsv").write_text("\n".join(representative_lines) + "\n")
    receipt = {
        "schema": 1,
        "implementationCommit": implementation_commit,
        "jar": {"file": jar.name, "sha256": digest(jar)},
        "clientEmittedMetadata": {
            "file": emitted_metadata.name, "sha256": digest(emitted_metadata), "rows": len(rows)},
        "rawScreenshots": {
            "index": raw_index.name, "indexSha256": digest(raw_index), "rows": len(rows),
            "uniqueContentFiles": len(list(raw.glob("*.png"))),
        },
    }
    (output / "client-run-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--captures", type=Path, required=True)
    parser.add_argument("--screenshots", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--implementation-commit", required=True)
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args()
    build(args.captures, args.screenshots, args.output, args.implementation_commit, args.jar)


if __name__ == "__main__":
    main()
