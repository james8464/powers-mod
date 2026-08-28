#!/usr/bin/env python3
"""Verify the exact VFX-006 integrated-client gallery and semantic manifest."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import uuid
from pathlib import Path, PurePosixPath

from PIL import Image


EXPECTED_SIZE = (1280, 720)
POSES = ("INVOKE", "PROJECT", "CHANNEL", "RELEASE")
STYLES = ("SHADOW", "RADIANT", "DARKNESS", "HERALD_LIGHT", "HERALD_DARK",
          "FIRST_VESSEL")
ENTITY_TYPES = {
    "SHADOW": "powers:shadow_companion",
    "RADIANT": "powers:radiant_sentinel",
    "DARKNESS": "powers:darkness_creature",
    "HERALD_LIGHT": "powers:light_herald",
    "HERALD_DARK": "powers:dark_herald",
    "FIRST_VESSEL": "powers:first_vessel",
}
HANDS = {"NONE", "LEFT", "RIGHT", "BOTH"}
LIFECYCLE_SCENARIOS = (
    "latency", "late_tracking", "interruption", "expiry", "reconnect",
    "entity_id_reuse", "locomotion_walk",
)
ANGLE_FIELDS = (
    "headX", "headY", "bodyX", "bodyY", "leftArmX", "leftArmY", "leftArmZ",
    "rightArmX", "rightArmY", "rightArmZ",
)
ANGLE_LIMITS = {
    "headX": 0.25, "headY": 0.25, "bodyX": 0.35, "bodyY": 0.35,
    "leftArmX": 1.25, "leftArmY": 1.25, "leftArmZ": 1.25,
    "rightArmX": 1.25, "rightArmY": 1.25, "rightArmZ": 1.25,
}
SHA_PATTERN = re.compile(r"[0-9a-f]{40}")
HEX64_PATTERN = re.compile(r"[0-9a-f]{64}")
PRIVATE_MARKERS = ("/Users/", "\\Users\\", ".worktrees/", "file://")
REQUIRED_FIELDS = {
    "schemaVersion", "implementationSha", "captureId", "scenario", "entityType",
    "entityId", "entityUuid", "resolvedEntityUuid", "sequence", "pose", "style",
    "hand", "authoritativeStartTick", "durationTicks", "receiptTick", "captureTick",
    "reducedMotion", "active", "progress", "angles", "imagePath", "sha256",
}


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _safe_relative(value: object) -> str:
    if not isinstance(value, str) or not value or any(marker in value for marker in PRIVATE_MARKERS):
        raise ValueError("path privacy violation")
    path = PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts or path.name != value:
        raise ValueError("path privacy violation")
    return value


def _uuid(value: object, label: str) -> uuid.UUID:
    try:
        parsed = uuid.UUID(str(value))
    except (ValueError, AttributeError) as error:
        raise ValueError(f"invalid {label}") from error
    if parsed.int == 0:
        raise ValueError(f"invalid {label}")
    return parsed


def _read_rows(manifest: Path) -> list[dict]:
    if not manifest.is_file() or manifest.is_symlink():
        raise ValueError("missing canonical manifest")
    text = manifest.read_text(encoding="utf-8")
    if any(marker in text for marker in PRIVATE_MARKERS):
        raise ValueError("path privacy violation")
    rows = []
    for number, line in enumerate(text.splitlines(), 1):
        try:
            row = json.loads(line)
        except json.JSONDecodeError as error:
            raise ValueError(f"invalid manifest JSON line {number}") from error
        if not isinstance(row, dict) or set(row) != REQUIRED_FIELDS:
            raise ValueError(f"manifest schema mismatch line {number}")
        rows.append(row)
    return rows


def _validate_row(root: Path, row: dict, implementation_sha: str | None) -> tuple[str, str]:
    if row["schemaVersion"] != 1:
        raise ValueError("unsupported manifest schema")
    sha = row["implementationSha"]
    if not isinstance(sha, str) or SHA_PATTERN.fullmatch(sha) is None:
        raise ValueError("invalid implementation SHA")
    if implementation_sha is not None and sha != implementation_sha:
        raise ValueError("implementation identity mismatch")
    if not isinstance(row["captureId"], str) or not row["captureId"].startswith("vfx006-"):
        raise ValueError("invalid capture identity")
    scenario = row["scenario"]
    if scenario != "gallery" and scenario not in LIFECYCLE_SCENARIOS:
        raise ValueError("invalid scenario")
    style = row["style"]
    pose = row["pose"]
    if style not in STYLES or pose not in POSES or row["hand"] not in HANDS:
        raise ValueError("invalid pose vocabulary")
    if row["entityType"] != ENTITY_TYPES[style]:
        raise ValueError("entity/style identity mismatch")
    entity_uuid = _uuid(row["entityUuid"], "entity UUID")
    resolved_uuid = _uuid(row["resolvedEntityUuid"], "resolved entity UUID")
    if not isinstance(row["entityId"], int) or row["entityId"] < 0:
        raise ValueError("invalid entity ID")
    if not isinstance(row["sequence"], int) or row["sequence"] < 1:
        raise ValueError("invalid sequence")
    timing = (row["authoritativeStartTick"], row["receiptTick"], row["captureTick"])
    if any(not isinstance(value, int) or value < 0 for value in timing):
        raise ValueError("invalid lifecycle timing")
    if not isinstance(row["durationTicks"], int) or not 1 <= row["durationTicks"] <= 120:
        raise ValueError("invalid duration")
    if row["receiptTick"] < row["authoritativeStartTick"] - 5:
        raise ValueError("invalid receipt timing")
    if row["captureTick"] < row["receiptTick"]:
        raise ValueError("invalid capture timing")
    progress = row["progress"]
    if not isinstance(progress, (int, float)) or not math.isfinite(progress) or not 0 <= progress <= 1:
        raise ValueError("invalid pose progress")
    if not isinstance(row["active"], bool) or not isinstance(row["reducedMotion"], bool):
        raise ValueError("invalid mode metadata")
    angles = row["angles"]
    if not isinstance(angles, dict) or set(angles) != set(ANGLE_FIELDS):
        raise ValueError("angle schema mismatch")
    for field, limit in ANGLE_LIMITS.items():
        value = angles[field]
        if not isinstance(value, (int, float)) or not math.isfinite(value) or abs(value) > limit + 1e-9:
            raise ValueError(f"angle bounds violation: {field}")
        if not row["active"] and abs(value) > 1e-9:
            raise ValueError("inactive lifecycle has non-zero angles")
    if scenario == "gallery" and not row["active"]:
        raise ValueError("gallery pose is inactive")
    if scenario == "gallery" and not 0.2 <= progress <= 0.75:
        raise ValueError("gallery capture is outside authored hold")
    if scenario in {"latency", "late_tracking"} and (not row["active"] or progress < 0.25):
        raise ValueError(f"{scenario} did not preserve authoritative age")
    if scenario == "latency" and row["receiptTick"] <= row["authoritativeStartTick"]:
        raise ValueError("latency receipt was not delayed")
    if scenario in {"latency", "late_tracking"}:
        expected = min(1.0, max(0.0, (row["captureTick"]
                                     - row["authoritativeStartTick"]) / row["durationTicks"]))
        if not math.isclose(progress, expected, rel_tol=0.0, abs_tol=1e-9):
            raise ValueError("authoritative progress mismatch")
    if scenario == "locomotion_walk" and (not row["active"] or not 0.2 <= progress <= 0.75):
        raise ValueError("locomotion capture is outside authored hold")
    if scenario in {"interruption", "expiry", "reconnect", "entity_id_reuse"} and row["active"]:
        raise ValueError(f"{scenario} did not clear pose state")
    if scenario == "entity_id_reuse":
        if entity_uuid == resolved_uuid:
            raise ValueError("entity-ID reuse identity mismatch")
    elif entity_uuid != resolved_uuid:
        raise ValueError("entity identity mismatch")
    image_name = _safe_relative(row["imagePath"])
    image_path = root / "screenshots" / image_name
    if not image_path.is_file() or image_path.is_symlink():
        raise ValueError("missing screenshot")
    if not isinstance(row["sha256"], str) or HEX64_PATTERN.fullmatch(row["sha256"]) is None:
        raise ValueError("invalid image checksum")
    if _sha256(image_path) != row["sha256"]:
        raise ValueError("image checksum mismatch")
    try:
        with Image.open(image_path) as image:
            image.verify()
        with Image.open(image_path) as image:
            if image.format != "PNG" or image.size != EXPECTED_SIZE:
                raise ValueError("screenshot must be decoded PNG at 1280x720")
    except (OSError, SyntaxError) as error:
        raise ValueError("screenshot must be decoded PNG at 1280x720") from error
    return sha, image_name


def validate(root: Path) -> dict:
    root = root.resolve()
    rows = _read_rows(root / "capture-manifest.jsonl")
    if not rows:
        raise ValueError("empty VFX-006 gallery")
    implementation_sha = rows[0]["implementationSha"]
    captures: set[str] = set()
    images: set[str] = set()
    gallery_keys: set[tuple[str, str, bool]] = set()
    lifecycle: set[str] = set()
    semantic_ids: set[tuple[str, int]] = set()
    for row in rows:
        sha, image_name = _validate_row(root, row, implementation_sha)
        if sha != implementation_sha:
            raise ValueError("implementation identity mismatch")
        if row["captureId"] in captures or image_name in images:
            raise ValueError("duplicate capture identity")
        captures.add(row["captureId"])
        images.add(image_name)
        semantic_id = (row["entityUuid"], row["sequence"])
        if semantic_id in semantic_ids:
            raise ValueError("duplicate semantic identity")
        semantic_ids.add(semantic_id)
        if row["scenario"] == "gallery":
            gallery_keys.add((row["style"], row["pose"], row["reducedMotion"]))
        else:
            if row["scenario"] in lifecycle:
                raise ValueError("duplicate lifecycle scenario")
            lifecycle.add(row["scenario"])
    expected_gallery = {(style, pose, reduced) for style in STYLES for pose in POSES
                        for reduced in (False, True)}
    missing = expected_gallery - gallery_keys
    if missing:
        missing_poses = [pose for pose in POSES if any(key[1] == pose for key in missing)]
        if missing_poses:
            raise ValueError(f"missing pose coverage: {missing_poses[0]}")
        raise ValueError("missing gallery coverage")
    if gallery_keys != expected_gallery:
        raise ValueError("noncanonical gallery coverage")
    if lifecycle != set(LIFECYCLE_SCENARIOS):
        raise ValueError("missing lifecycle scenario coverage")
    screenshots = root / "screenshots"
    actual = {path.name for path in screenshots.iterdir()
              if path.is_file() and not path.is_symlink()}
    if actual != images or any(path.is_symlink() for path in screenshots.iterdir()):
        raise ValueError("screenshot inventory mismatch")
    return {
        "schemaVersion": 1,
        "implementationSha": implementation_sha,
        "rowCount": len(rows),
        "galleryCount": len(gallery_keys),
        "lifecycleCount": len(lifecycle),
        "normalCount": sum(1 for row in rows if row["scenario"] == "gallery"
                           and not row["reducedMotion"]),
        "reducedCount": sum(1 for row in rows if row["scenario"] == "gallery"
                            and row["reducedMotion"]),
        "poses": list(POSES),
        "styles": list(STYLES),
        "lifecycleScenarios": list(LIFECYCLE_SCENARIOS),
        "maximumObservedAngles": {
            field: max(abs(float(row["angles"][field])) for row in rows)
            for field in ANGLE_FIELDS
        },
        "images": [{"captureId": row["captureId"], "imagePath": row["imagePath"],
                    "sha256": row["sha256"]}
                   for row in sorted(rows, key=lambda item: item["captureId"])],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    options = parser.parse_args()
    result = validate(options.root)
    output = options.output or options.root / "capture-verification.json"
    output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"VFX-006 gallery passed: {result['rowCount']} rows; "
          f"{result['galleryCount']} gallery; {result['lifecycleCount']} lifecycle")


if __name__ == "__main__":
    main()
