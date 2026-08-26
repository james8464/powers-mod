#!/usr/bin/env python3
"""Verify the deterministic real-client VFX-005 silhouette acceptance gallery."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

from PIL import Image, ImageChops


POWER_IDS = (
    "size_shift", "time_shift", "flight", "starfall", "void_beam", "fireball",
    "lightning_strike", "thunderclap", "speed_burst", "telekinesis", "energy_beam",
    "super_speed", "breezy_bash", "invisibility", "time_freeze", "forcefield",
    "gravity_displacement", "vessel_possession", "astral_projection", "energy_drain",
    "ice_manipulation", "plant_healing_acceleration", "double_health",
)
ALIGNMENT_VARIANT_IDS = ("flight", "forcefield")
EXPECTED_SIZE = (1280, 720)
SILHOUETTE_ROI = (430, 220, 850, 590)
CROSSHAIR_ROI = (624, 344, 657, 377)
PIXEL_DELTA = 12
MIN_FOREGROUND_PIXELS = 18
MIN_REDUCED_OUTLINE_JACCARD = 0.82
EXPECTED_ROW_COUNT = 56
REQUIRED_KEYS = {
    "captureId", "category", "powerId", "alignment", "distance",
    "reducedMotion", "particles", "reloadRevision", "epoch", "imagePath",
}
SPECIAL_ROWS = {
    "baseline": ("size_shift", 96, False, "all"),
    "near": ("flight", 8, False, "all"),
    "wall_baseline": ("forcefield", 96, False, "all"),
    "wall": ("forcefield", 96, False, "all"),
    "minimal_particles": ("starfall", 96, False, "minimal"),
    "post_reload": ("void_beam", 96, False, "all"),
    "post_dimension": ("time_freeze", 96, False, "all"),
    "post_reconnect": ("double_health", 96, False, "all"),
}
CONTINUITY_ROWS = ("minimal_particles", "post_reload", "post_dimension", "post_reconnect")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _load_manifest(path: Path) -> list[dict[str, Any]]:
    if not path.is_file() or path.is_symlink():
        raise ValueError("manifest must be one regular JSONL file")
    rows: list[dict[str, Any]] = []
    for line_number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip():
            raise ValueError(f"blank manifest line {line_number}")
        try:
            row = json.loads(raw)
        except json.JSONDecodeError as error:
            raise ValueError(f"invalid manifest JSON line {line_number}: {error}") from error
        if not isinstance(row, dict) or set(row) != REQUIRED_KEYS:
            raise ValueError(f"manifest line {line_number} has wrong fields")
        _validate_row_types(row, line_number)
        rows.append(row)
    return rows


def _validate_row_types(row: dict[str, Any], line_number: int) -> None:
    for key in ("captureId", "category", "powerId", "alignment", "particles", "imagePath"):
        if not isinstance(row[key], str) or not row[key]:
            raise ValueError(f"manifest line {line_number} has invalid {key}")
    if row["powerId"] not in POWER_IDS:
        raise ValueError(f"manifest line {line_number} has unknown powerId")
    if row["alignment"] not in {"radiant", "darkness"}:
        raise ValueError(f"manifest line {line_number} has invalid alignment")
    if row["particles"] not in {"all", "minimal"}:
        raise ValueError(f"manifest line {line_number} has invalid particles")
    if type(row["reducedMotion"]) is not bool:
        raise ValueError(f"manifest line {line_number} has invalid reducedMotion")
    for key in ("distance", "reloadRevision", "epoch"):
        if type(row[key]) is not int or row[key] < 0:
            raise ValueError(f"manifest line {line_number} has invalid {key}")
    image = Path(row["imagePath"])
    if image.is_absolute() or len(image.parts) != 1 or image.suffix.lower() != ".png":
        raise ValueError(f"manifest line {line_number} has unsafe imagePath")


def _one(rows: list[dict[str, Any]], category: str,
         power_id: str | None = None) -> dict[str, Any]:
    matches = [row for row in rows if row["category"] == category
               and (power_id is None or row["powerId"] == power_id)]
    if len(matches) != 1:
        raise ValueError(f"required gallery rows: expected one {category}/{power_id}, found {len(matches)}")
    return matches[0]


def _validate_inventory(rows: list[dict[str, Any]]) -> None:
    if len(rows) != EXPECTED_ROW_COUNT:
        raise ValueError(f"required gallery rows: expected {EXPECTED_ROW_COUNT}, found {len(rows)}")
    capture_ids = [row["captureId"] for row in rows]
    paths = [row["imagePath"] for row in rows]
    if len(set(capture_ids)) != len(capture_ids) or len(set(paths)) != len(paths):
        raise ValueError("required gallery rows contain duplicate capture IDs or image paths")
    for category, reduced in (("far_normal", False), ("far_reduced", True)):
        actual = [row for row in rows if row["category"] == category]
        if len(actual) != len(POWER_IDS) or {row["powerId"] for row in actual} != set(POWER_IDS):
            raise ValueError(f"required gallery rows: {category} must cover all 23 powers")
        for row in actual:
            if (row["distance"], row["reducedMotion"], row["alignment"]) != (96, reduced, "radiant"):
                raise ValueError(f"required gallery rows: invalid {category}/{row['powerId']} contract")
            expected_particles = "minimal" if reduced else "all"
            if row["particles"] != expected_particles:
                raise ValueError(f"required gallery rows: invalid particles for {category}")
    variants = [row for row in rows if row["category"] == "alignment_variant"]
    if len(variants) != len(ALIGNMENT_VARIANT_IDS) or {
            row["powerId"] for row in variants} != set(ALIGNMENT_VARIANT_IDS):
        raise ValueError("required gallery rows: alignment variants are incomplete")
    if any((row["alignment"], row["distance"], row["reducedMotion"])
           != ("darkness", 96, False) for row in variants):
        raise ValueError("required gallery rows: alignment variants have invalid settings")
    for category, expected in SPECIAL_ROWS.items():
        row = _one(rows, category)
        observed = (row["powerId"], row["distance"], row["reducedMotion"], row["particles"])
        if observed != expected or row["alignment"] != "radiant":
            raise ValueError(f"required gallery rows: invalid {category} contract")
    if _one(rows, "post_reload")["reloadRevision"] < 1:
        raise ValueError("required gallery rows: reload revision was not advanced")
    base_epoch = _one(rows, "baseline")["epoch"]
    dimension_epoch = _one(rows, "post_dimension")["epoch"]
    reconnect_epoch = _one(rows, "post_reconnect")["epoch"]
    if not base_epoch < dimension_epoch < reconnect_epoch:
        raise ValueError("required gallery rows: dimension/reconnect epochs did not advance")


def _open_capture(screenshots: Path, row: dict[str, Any]) -> Image.Image:
    path = screenshots / row["imagePath"]
    try:
        resolved = path.resolve(strict=True)
    except FileNotFoundError as error:
        raise ValueError(f"missing capture {row['imagePath']}") from error
    if path.is_symlink() or resolved.parent != screenshots.resolve():
        raise ValueError(f"capture escapes screenshot directory: {row['imagePath']}")
    with Image.open(resolved) as opened:
        if opened.size != EXPECTED_SIZE:
            raise ValueError(f"capture must be exact 1280x720: {row['imagePath']} is {opened.size}")
        return opened.convert("RGB")


def _mask(image: Image.Image, baseline: Image.Image,
          roi: tuple[int, int, int, int] = SILHOUETTE_ROI) -> frozenset[int]:
    difference = ImageChops.difference(image.crop(roi), baseline.crop(roi)).convert("RGB")
    width = difference.width
    return frozenset(index for index, pixel in enumerate(difference.get_flattened_data())
                     if max(pixel) >= PIXEL_DELTA and index % width >= 0)


def _jaccard(first: frozenset[int], second: frozenset[int]) -> float:
    union = first | second
    return 1.0 if not union else len(first & second) / len(union)


def validate(screenshots: Path, manifest: Path) -> dict[str, Any]:
    screenshots = screenshots.resolve()
    if not screenshots.is_dir():
        raise ValueError("screenshots directory does not exist")
    rows = _load_manifest(manifest)
    _validate_inventory(rows)
    images = {row["captureId"]: _open_capture(screenshots, row) for row in rows}
    baseline_row = _one(rows, "baseline")
    baseline = images[baseline_row["captureId"]]
    masks: dict[str, frozenset[int]] = {}
    for row in rows:
        if row["category"] in {"baseline", "wall_baseline", "wall"}:
            continue
        mask = _mask(images[row["captureId"]], baseline)
        if len(mask) < MIN_FOREGROUND_PIXELS:
            raise ValueError(f"blank foreground: {row['captureId']} has {len(mask)} changed pixels")
        masks[row["captureId"]] = mask

    normal_masks = {
        power_id: masks[_one(rows, "far_normal", power_id)["captureId"]]
        for power_id in POWER_IDS
    }
    for index, first_id in enumerate(POWER_IDS):
        for second_id in POWER_IDS[index + 1:]:
            if normal_masks[first_id] == normal_masks[second_id]:
                raise ValueError(f"duplicate monochrome masks: {first_id} and {second_id}")

    outline_scores: dict[str, float] = {}
    for power_id in POWER_IDS:
        reduced = masks[_one(rows, "far_reduced", power_id)["captureId"]]
        score = _jaccard(normal_masks[power_id], reduced)
        if score < MIN_REDUCED_OUTLINE_JACCARD:
            raise ValueError(f"reduced outline mismatch: {power_id} jaccard={score:.4f}")
        outline_scores[power_id] = round(score, 6)

    lifecycle_scores: dict[str, float] = {}
    for category in CONTINUITY_ROWS:
        row = _one(rows, category)
        score = _jaccard(normal_masks[row["powerId"]], masks[row["captureId"]])
        if score < MIN_REDUCED_OUTLINE_JACCARD:
            raise ValueError(f"lifecycle outline mismatch: {category} jaccard={score:.4f}")
        lifecycle_scores[category] = round(score, 6)

    near = images[_one(rows, "near")["captureId"]]
    if _mask(near, baseline, CROSSHAIR_ROI):
        raise ValueError("crosshair intrusion: near silhouette changed the protected center ROI")
    wall_baseline = images[_one(rows, "wall_baseline")["captureId"]]
    wall = images[_one(rows, "wall")["captureId"]]
    wall_pixels = len(_mask(wall, wall_baseline))
    if wall_pixels != 0:
        raise ValueError(f"wall leakage: {wall_pixels} silhouette-region pixels remain")

    output_rows = []
    for row in sorted(rows, key=lambda item: item["captureId"]):
        path = screenshots / row["imagePath"]
        output_rows.append({"captureId": row["captureId"], "category": row["category"],
                            "imagePath": row["imagePath"], "sha256": _sha256(path),
                            "foregroundPixels": len(masks.get(row["captureId"], frozenset()))})
    return {"schemaVersion": 1, "rowCount": len(rows), "farNormalCount": 23,
            "farReducedCount": 23, "pairwiseComparisons": 253,
            "minimumReducedOutlineJaccard": min(outline_scores.values()),
            "minimumLifecycleOutlineJaccard": min(lifecycle_scores.values()),
            "wallLeakagePixels": wall_pixels, "crosshairIntrusionPixels": 0,
            "rows": output_rows}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--screenshots", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    options = parser.parse_args()
    result = validate(options.screenshots, options.manifest)
    rendered = json.dumps(result, indent=2, sort_keys=True) + "\n"
    options.output.write_text(rendered, encoding="utf-8")
    print("VFX-005 captures passed: "
          f"{result['rowCount']} rows; 253 pairwise masks; "
          f"reduced>={result['minimumReducedOutlineJaccard']:.4f}; "
          "crosshair=0; wall=0")


if __name__ == "__main__":
    main()
