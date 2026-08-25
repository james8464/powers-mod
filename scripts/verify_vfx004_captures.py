#!/usr/bin/env python3
"""Validate the exact VFX-004 30-cell gallery and opaque-wall render proof."""

from __future__ import annotations

import argparse
import hashlib
import json
import statistics
from pathlib import Path

from PIL import Image


IMPACTS = ("beam", "slam", "thunderclap", "ice", "fire")
MATERIALS = ("stone", "earth", "wood", "metal", "sand", "cold")
ROI = (570, 210, 710, 280)
EXPECTED_SIZE = (1280, 720)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def capture(screenshots: Path, suffix: str) -> Path:
    matches = sorted(screenshots.glob(f"*_{suffix}.png"))
    if len(matches) != 1:
        raise ValueError(f"expected one {suffix} capture, found {len(matches)}")
    if matches[0].is_symlink():
        raise ValueError(f"symlinked capture is not accepted: {matches[0]}")
    return matches[0]


def roi_metrics(path: Path) -> tuple[float, int, int]:
    with Image.open(path) as opened:
        if opened.size != EXPECTED_SIZE:
            raise ValueError(f"unexpected capture size {opened.size}: {path.name}")
        pixels = list(opened.convert("RGB").crop(ROI).get_flattened_data())
    luma = [(red * 299 + green * 587 + blue * 114) / 1000
            for red, green, blue in pixels]
    saturated = sum(1 for red, green, blue in pixels
                    if max(red, green, blue) - min(red, green, blue) > 45
                    and max(red, green, blue) > 150)
    return statistics.pstdev(luma), saturated, len(set(pixels))


def validate(screenshots: Path) -> dict:
    screenshots = screenshots.resolve()
    rows = []
    for impact in IMPACTS:
        for material in MATERIALS:
            capture_id = f"vfx004-scar-matrix-{impact}-{material}"
            path = capture(screenshots, capture_id)
            deviation, saturated, colors = roi_metrics(path)
            if deviation < 5.0 or colors < 30:
                raise ValueError(f"matrix ROI is blank or unrendered: {capture_id}")
            rows.append({"captureId": capture_id, "file": path.name,
                         "sha256": digest(path), "roiLumaDeviation": round(deviation, 3),
                         "roiSaturatedPixels": saturated, "roiUniqueColors": colors})
    hashes = [row["sha256"] for row in rows]
    if len(set(hashes)) != len(hashes):
        raise ValueError("matrix contains duplicate frames; transition/fade contamination suspected")

    visible = capture(screenshots, "vfx004-scar-visible-front")
    occluded = capture(screenshots, "vfx004-scar-occluded-wall")
    post_reload = capture(screenshots, "vfx004-scar-post-resource-reload")
    visible_metrics = roi_metrics(visible)
    occluded_metrics = roi_metrics(occluded)
    roi_metrics(post_reload)
    if visible_metrics[1] < 500:
        raise ValueError("front proof lacks the expected saturated fire/cold scar pixels")
    if occluded_metrics[1] > 10 or occluded_metrics[1] * 20 >= visible_metrics[1]:
        raise ValueError("scar-colored pixels remain in the opaque-wall ROI")
    return {"schema": 1, "matrixRows": rows,
            "visibleFront": {"file": visible.name, "sha256": digest(visible),
                             "roiSaturatedPixels": visible_metrics[1]},
            "occludedWall": {"file": occluded.name, "sha256": digest(occluded),
                              "roiSaturatedPixels": occluded_metrics[1]},
            "postResourceReload": {"file": post_reload.name, "sha256": digest(post_reload)}}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--screenshots", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = validate(args.screenshots)
    rendered = json.dumps(result, indent=2) + "\n"
    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    print(f"VFX-004 captures passed: {len(result['matrixRows'])} matrix rows; "
          f"front={result['visibleFront']['roiSaturatedPixels']} saturated pixels; "
          f"wall={result['occludedWall']['roiSaturatedPixels']}")


if __name__ == "__main__":
    main()
