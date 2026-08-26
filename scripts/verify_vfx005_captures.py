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
NEAR_FOREGROUND_ROI = (430, 220, 850, 610)
WALL_FOREGROUND_ROI = (240, 0, 1040, 600)
BODY_IDENTITY_ROI = (620, 385, 660, 510)
TOAST_ROI = (962, 0, 1280, 320)
PIXEL_DELTA = 12
MIN_FOREGROUND_PIXELS = 18
BODY_RENDER_PALETTE = frozenset({
    (15, 17, 21), (16, 17, 21), (24, 26, 32), (24, 26, 33), (24, 27, 33),
    (25, 27, 33), (25, 27, 34), (37, 41, 51), (38, 42, 52), (39, 43, 53),
    (51, 56, 69),
})
EXPECTED_BODY_IDENTITY_PIXELS = 4_704
MIN_BODY_RETENTION = 0.90
MIN_BODY_ROW_COVERAGE = 113
MIN_BODY_COLUMN_COVERAGE = 36
MIN_REDUCED_OUTLINE_JACCARD = 0.82
MAX_BACKGROUND_DRIFT_PIXELS = 256
EXPECTED_ROW_COUNT = 56
BASE_EPOCH = 1_000_002
DIMENSION_EPOCH = 1_000_004
RECONNECT_EPOCH = 2_000_002
REQUIRED_KEYS = {
    "captureId", "category", "powerId", "alignment", "distance",
    "reducedMotion", "particles", "reloadRevision", "epoch", "imagePath",
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
    if rows != _canonical_rows():
        raise ValueError("canonical gallery IDs, paths, ordering, or metadata drifted")


def _canonical_rows() -> list[dict[str, Any]]:
    specs: list[tuple[str, str, str, int, bool, str, int, int]] = [
        ("baseline", "size_shift", "radiant", 96, False, "all", 0, BASE_EPOCH),
    ]
    specs.extend(("far_normal", power_id, "radiant", 96, False, "all", 0, BASE_EPOCH)
                 for power_id in POWER_IDS)
    specs.extend(("far_reduced", power_id, "radiant", 96, True, "minimal", 0, BASE_EPOCH)
                 for power_id in POWER_IDS)
    specs.extend(("alignment_variant", power_id, "darkness", 96, False, "all", 0, BASE_EPOCH)
                 for power_id in ALIGNMENT_VARIANT_IDS)
    specs.extend([
        ("near", "flight", "radiant", 8, False, "all", 0, BASE_EPOCH),
        ("wall_baseline", "forcefield", "radiant", 96, False, "all", 0, BASE_EPOCH),
        ("wall", "forcefield", "radiant", 96, False, "all", 0, BASE_EPOCH),
        ("minimal_particles", "starfall", "radiant", 96, False, "minimal", 0, BASE_EPOCH),
        ("post_reload", "void_beam", "radiant", 96, False, "all", 1, BASE_EPOCH),
        ("post_dimension", "time_freeze", "radiant", 96, False, "all", 1, DIMENSION_EPOCH),
        ("post_reconnect", "double_health", "radiant", 96, False, "all", 1, RECONNECT_EPOCH),
    ])
    rows: list[dict[str, Any]] = []
    for index, (category, power_id, alignment, distance, reduced,
                particles, revision, epoch) in enumerate(specs):
        capture_id = f"vfx005-{category}-{power_id}"
        rows.append({"captureId": capture_id, "category": category, "powerId": power_id,
                     "alignment": alignment, "distance": distance, "reducedMotion": reduced,
                     "particles": particles, "reloadRevision": revision, "epoch": epoch,
                     "imagePath": f"{index:04d}_{capture_id}.png"})
    return rows


def _open_capture(screenshots: Path, row: dict[str, Any]) -> Image.Image:
    path = screenshots / row["imagePath"]
    try:
        resolved = path.resolve(strict=True)
    except FileNotFoundError as error:
        raise ValueError(f"missing capture {row['imagePath']}") from error
    if path.is_symlink() or resolved.parent != screenshots.resolve():
        raise ValueError(f"capture escapes screenshot directory: {row['imagePath']}")
    with Image.open(resolved) as opened:
        if opened.format != "PNG":
            raise ValueError(f"capture must be an actual decoded PNG: {row['imagePath']}")
        if opened.size != EXPECTED_SIZE:
            raise ValueError(f"capture must be exact 1280x720: {row['imagePath']} is {opened.size}")
        return opened.convert("RGB")


def _mask(image: Image.Image, baseline: Image.Image,
          roi: tuple[int, int, int, int] = SILHOUETTE_ROI) -> frozenset[int]:
    difference = ImageChops.difference(image.crop(roi), baseline.crop(roi)).convert("RGB")
    return frozenset(index for index, pixel in enumerate(difference.get_flattened_data())
                     if max(pixel) >= PIXEL_DELTA)


def _changed_pixel_count(difference: Image.Image, threshold: int) -> int:
    red, green, blue = difference.convert("RGB").split()
    maximum = ImageChops.lighter(ImageChops.lighter(red, green), blue)
    return sum(maximum.histogram()[threshold:])


def _outside_changed_pixels(image: Image.Image, baseline: Image.Image,
                            excluded: tuple[int, int, int, int]) -> int:
    difference = ImageChops.difference(image, baseline).convert("RGB")
    x0, y0, x1, y1 = excluded
    regions = ((0, 0, EXPECTED_SIZE[0], y0),
               (0, y1, EXPECTED_SIZE[0], EXPECTED_SIZE[1]),
               (0, y0, x0, y1),
               (x1, y0, EXPECTED_SIZE[0], y1))
    return sum(_changed_pixel_count(difference.crop(region), PIXEL_DELTA)
               for region in regions if region[0] < region[2] and region[1] < region[3])


def _allowed_foreground(row: dict[str, Any]) -> tuple[int, int, int, int]:
    if row["category"] == "near":
        return NEAR_FOREGROUND_ROI
    if row["category"] in {"wall_baseline", "wall"}:
        return WALL_FOREGROUND_ROI
    return SILHOUETTE_ROI


def _toast_pixels(image: Image.Image) -> int:
    toast_region = image.crop(TOAST_ROI).convert("RGB")
    width, height = toast_region.size
    comparisons = (
        ImageChops.difference(toast_region.crop((1, 0, width, height)),
                              toast_region.crop((0, 0, width - 1, height))),
        ImageChops.difference(toast_region.crop((0, 1, width, height)),
                              toast_region.crop((0, 0, width, height - 1))),
    )
    return int(any(max(channel_max for _, channel_max in difference.getextrema())
                   >= PIXEL_DELTA for difference in comparisons))


def _jaccard(first: frozenset[int], second: frozenset[int]) -> float:
    union = first | second
    return 1.0 if not union else len(first & second) / len(union)


def validate(screenshots: Path, manifest: Path) -> dict[str, Any]:
    screenshots = screenshots.resolve()
    if not screenshots.is_dir():
        raise ValueError("screenshots directory does not exist")
    rows = _load_manifest(manifest)
    _validate_inventory(rows)
    referenced = {row["imagePath"] for row in rows}
    entries = list(screenshots.iterdir())
    actual = {entry.name for entry in entries}
    if actual != referenced or any(entry.is_symlink() or not entry.is_file() for entry in entries):
        raise ValueError("screenshot inventory must exactly match canonical manifest paths")
    images = {row["captureId"]: _open_capture(screenshots, row) for row in rows}
    for row in rows:
        # The explicit wall-occlusion scene legitimately fills the toast reserve; its
        # two full-frame captures are instead required to match pixel-for-pixel below.
        if row["category"] in {"wall_baseline", "wall"}:
            continue
        if _toast_pixels(images[row["captureId"]]) != 0:
            raise ValueError(f"toast or overlay present: {row['captureId']}")
    baseline_row = _one(rows, "baseline")
    baseline = images[baseline_row["captureId"]]
    normal_anchor = images[_one(rows, "far_normal", "size_shift")["captureId"]]
    row_background_pixels: dict[str, int] = {}
    for row in rows:
        reference = normal_anchor if row["category"] == "baseline" else baseline
        changed = _outside_changed_pixels(images[row["captureId"]], reference,
                                          _allowed_foreground(row))
        if changed > MAX_BACKGROUND_DRIFT_PIXELS:
            raise ValueError(f"background mismatch: {row['captureId']} has {changed} "
                             "changed pixels outside allowed foreground")
        row_background_pixels[row["captureId"]] = changed
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
    lifecycle_background_pixels: dict[str, int] = {}
    for category in CONTINUITY_ROWS:
        row = _one(rows, category)
        normal_row = _one(rows, "far_normal", row["powerId"])
        score = _jaccard(normal_masks[row["powerId"]], masks[row["captureId"]])
        if score < MIN_REDUCED_OUTLINE_JACCARD:
            raise ValueError(f"lifecycle outline mismatch: {category} jaccard={score:.4f}")
        lifecycle_scores[category] = round(score, 6)
        background_pixels = _outside_changed_pixels(images[row["captureId"]],
                                                    images[normal_row["captureId"]],
                                                    SILHOUETTE_ROI)
        if background_pixels > MAX_BACKGROUND_DRIFT_PIXELS:
            raise ValueError(f"lifecycle background mismatch: {category} has "
                             f"{background_pixels} changed pixels outside silhouette ROI")
        lifecycle_background_pixels[category] = background_pixels

    near = images[_one(rows, "near")["captureId"]]
    if _mask(near, baseline, CROSSHAIR_ROI):
        raise ValueError("crosshair intrusion: near silhouette changed the protected center ROI")
    body_crop = near.crop(BODY_IDENTITY_ROI)
    body_positions = frozenset(
        (index % body_crop.width, index // body_crop.width)
        for index, pixel in enumerate(body_crop.get_flattened_data())
        if pixel in BODY_RENDER_PALETTE)
    body_retention = len(body_positions) / EXPECTED_BODY_IDENTITY_PIXELS
    body_rows = len({y for _, y in body_positions})
    body_columns = len({x for x, _ in body_positions})
    if (body_retention < MIN_BODY_RETENTION or body_rows < MIN_BODY_ROW_COVERAGE
            or body_columns < MIN_BODY_COLUMN_COVERAGE):
        raise ValueError(f"near body obstruction: retention={body_retention:.4f}, "
                         f"rows={body_rows}, columns={body_columns}")
    wall_baseline = images[_one(rows, "wall_baseline")["captureId"]]
    wall = images[_one(rows, "wall")["captureId"]]
    wall_difference = ImageChops.difference(wall, wall_baseline).convert("RGB")
    wall_pixels = _changed_pixel_count(wall_difference, 1)
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
            "maximumBackgroundDriftPixels": max(row_background_pixels.values()),
            "maximumLifecycleBackgroundDriftPixels": max(lifecycle_background_pixels.values()),
            "nearBodyIdentityPixels": len(body_positions),
            "nearBodyRetention": round(body_retention, 6),
            "nearBodyRowCoverage": body_rows,
            "nearBodyColumnCoverage": body_columns,
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
