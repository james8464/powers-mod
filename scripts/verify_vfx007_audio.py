#!/usr/bin/env python3
"""Strictly verify one exact-SHA VFX-007 production audio evidence package."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
from pathlib import Path, PurePosixPath

from PIL import Image


CUES = (
    "rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
    "celestial_ring", "beam_ring", "boss_impact_ring", "time_release",
    "rift_open", "rift_close", "soul_tether", "light_chorus",
    "dark_whisper", "ward_impact", "rank_awaken", "interaction_clash",
)
LAYERS = ("near", "mid", "far")
WORLD_CUES = {"celestial_ring", "boss_impact_ring", "light_chorus", "dark_whisper",
              "rank_awaken"}
INTIMATE_CUES = {"rune_hum", "crystal_resonate"}
RADII = {"intimate": (8.0, 28.0, 72.0), "standard": (12.0, 48.0, 128.0),
         "world": (20.0, 96.0, 256.0)}
SHA = re.compile(r"[0-9a-f]{40}")
DIMENSION = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")
HEX64 = re.compile(r"[0-9a-f]{64}")
PRIVATE_MARKERS = (b"/Users/", b"\\Users\\", b".worktrees/", b"file://",
                   b"james8464")
AUDIT_FIELDS = {
    "schemaVersion", "cue", "layer", "distance", "obstructed", "effectiveGain",
    "result", "subtitleKey", "reducedTinnitus", "dimension", "eventId",
    "implementationSha",
}
INDEX_FIELDS = {"schemaVersion", "implementationSha", "burstEventIds",
                "ordinaryCelestialEventId", "reducedCelestialEventId",
                "lifecycleEventIds"}


def _duplicates(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def _constant(value):
    raise ValueError(f"JSON number must be finite: {value}")


def _loads(text: str, label: str):
    try:
        return json.loads(text, object_pairs_hook=_duplicates, parse_constant=_constant)
    except json.JSONDecodeError as error:
        raise ValueError(f"invalid JSON: {label}") from error


def _read_json(path: Path):
    if not path.is_file() or path.is_symlink():
        raise ValueError(f"missing regular evidence file: {path.name}")
    data = path.read_bytes()
    _privacy(data, path.name)
    if b"\r" in data:
        raise ValueError(f"text must use normalized LF: {path.name}")
    return _loads(data.decode("utf-8"), path.name)


def _read_rows(path: Path) -> list[dict]:
    if not path.is_file() or path.is_symlink():
        raise ValueError("missing audio audit")
    data = path.read_bytes()
    _privacy(data, path.name)
    if b"\r" in data:
        raise ValueError("audio audit must use normalized LF")
    rows = []
    for number, line in enumerate(data.decode("utf-8").splitlines(), 1):
        value = _loads(line, f"audio row {number}")
        if not isinstance(value, dict):
            raise ValueError(f"audio row {number} is not an object")
        rows.append(value)
    return rows


def _privacy(data: bytes, label: str) -> None:
    if any(marker in data for marker in PRIVATE_MARKERS):
        raise ValueError(f"privacy violation: {label}")


def _finite(value, label: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{label} must be finite")
    parsed = float(value)
    if not math.isfinite(parsed):
        raise ValueError(f"{label} must be finite")
    return parsed


def _safe_name(value, label: str) -> str:
    if not isinstance(value, str) or not value:
        raise ValueError(f"invalid {label}")
    path = PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts or path.name != value:
        raise ValueError(f"invalid {label}")
    return value


def _profile(cue: str) -> str:
    if cue in INTIMATE_CUES:
        return "intimate"
    if cue in WORLD_CUES:
        return "world"
    return "standard"


def _raw_layer(cue: str, distance: float) -> str | None:
    near, mid, far = RADII[_profile(cue)]
    if distance < 0.0 or distance > far:
        return None
    if distance <= near:
        return "near"
    if distance <= mid:
        return "mid"
    return "far"


def _validate_row(row: dict, implementation_sha: str) -> None:
    if set(row) != AUDIT_FIELDS or row["schemaVersion"] != 1:
        raise ValueError("audio audit schema mismatch")
    cue, layer = row["cue"], row["layer"]
    if cue not in CUES or layer not in LAYERS:
        raise ValueError("invalid audio vocabulary")
    if row["implementationSha"] != implementation_sha:
        raise ValueError("implementation identity mismatch")
    if row["subtitleKey"] != f"subtitles.powers.{cue}":
        raise ValueError("subtitle identity mismatch")
    if not isinstance(row["obstructed"], bool) or not isinstance(row["reducedTinnitus"], bool):
        raise ValueError("audio flags must be booleans")
    if row["reducedTinnitus"] and cue != "celestial_ring":
        raise ValueError("reduced tinnitus selected for a non-Celestial cue")
    if row["result"] not in {"admitted", "dropped"}:
        raise ValueError("invalid admission result")
    if not isinstance(row["eventId"], int) or isinstance(row["eventId"], bool) \
            or row["eventId"] < 0:
        raise ValueError("invalid event ID")
    if not isinstance(row["dimension"], str) or DIMENSION.fullmatch(row["dimension"]) is None:
        raise ValueError("invalid dimension")
    distance = _finite(row["distance"], "distance")
    gain = _finite(row["effectiveGain"], "effective gain")
    raw = _raw_layer(cue, distance)
    if raw is None:
        raise ValueError("distance exceeds authored radius")
    expected = {"near": "mid", "mid": "far", "far": "far"}[raw] \
        if row["obstructed"] else raw
    if row["result"] == "admitted" and layer != expected:
        raise ValueError("distance/layer mismatch")
    if row["result"] == "admitted" and not 0.0 < gain <= 0.90:
        raise ValueError("admitted effective gain out of range")
    if row["result"] == "dropped" and gain != 0.0:
        raise ValueError("dropped event retained effective gain")


def _validate_metrics(metrics: dict) -> None:
    required = {"schemaVersion", "assetCount", "errors", "cues",
                "reducedCelestialHighBandRatio", "reducedCelestialLowMidEnergy"}
    if set(metrics) != required or metrics["schemaVersion"] != 1 \
            or metrics["assetCount"] != 51 or metrics["errors"] != []:
        raise ValueError("audio metrics report is not a clean 51-asset PASS")
    if not isinstance(metrics["cues"], list) \
            or {entry.get("cue") for entry in metrics["cues"] if isinstance(entry, dict)} != set(CUES):
        raise ValueError("audio metrics cue coverage mismatch")
    for entry in metrics["cues"]:
        if set(entry) != {"cue", "effectiveRms", "peak", "nearCentroid", "farCentroid"}:
            raise ValueError("audio metrics schema mismatch")
        values = entry["effectiveRms"]
        if not isinstance(values, dict) or set(values) != set(LAYERS):
            raise ValueError("audio metrics layer mismatch")
        near, mid, far = (_finite(values[layer], "RMS") for layer in LAYERS)
        if not near > mid > far > 0.0:
            raise ValueError("audio metrics RMS ordering mismatch")
        if not 0.0 < _finite(entry["peak"], "peak") <= 0.7071:
            raise ValueError("audio metrics peak mismatch")
        if _finite(entry["farCentroid"], "far centroid") \
                > _finite(entry["nearCentroid"], "near centroid") * 0.80:
            raise ValueError("audio metrics centroid mismatch")
    if not 0.0 <= _finite(metrics["reducedCelestialHighBandRatio"], "reduced ratio") <= 0.30 \
            or _finite(metrics["reducedCelestialLowMidEnergy"], "warning energy") <= 0.0:
        raise ValueError("audio metrics reduced-Celestial mismatch")


def validate(root: Path) -> dict:
    root = root.resolve()
    if not root.is_dir() or root.is_symlink():
        raise ValueError("evidence root must be a directory")
    for path in root.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"symlink is forbidden: {path.name}")
        if path.is_file():
            _privacy(path.read_bytes(), path.name)

    index = _read_json(root / "capture-index.json")
    if not isinstance(index, dict) or set(index) != INDEX_FIELDS or index["schemaVersion"] != 1:
        raise ValueError("capture index schema mismatch")
    implementation_sha = index["implementationSha"]
    if not isinstance(implementation_sha, str) or SHA.fullmatch(implementation_sha) is None:
        raise ValueError("invalid implementation SHA")
    rows = _read_rows(root / "audio-audit.jsonl")
    if not 1 <= len(rows) <= 128:
        raise ValueError("audio audit row bound violated")
    for row in rows:
        _validate_row(row, implementation_sha)
    by_id = {}
    for row in rows:
        if row["eventId"] in by_id:
            raise ValueError("duplicate event ID")
        by_id[row["eventId"]] = row

    burst_ids = index["burstEventIds"]
    lifecycle = index["lifecycleEventIds"]
    if not isinstance(burst_ids, list) or len(burst_ids) != 9 or len(set(burst_ids)) != 9:
        raise ValueError("burst index mismatch")
    if not isinstance(lifecycle, dict) or set(lifecycle) != {"reload", "reconnect", "dimension"}:
        raise ValueError("lifecycle index mismatch")
    special_ids = set(burst_ids) | set(lifecycle.values()) | {index["reducedCelestialEventId"]}
    if any(event not in by_id for event in special_ids | {index["ordinaryCelestialEventId"]}):
        raise ValueError("capture index references missing event")
    open_rows = [row for row in rows if row["eventId"] not in special_ids
                 and not row["obstructed"] and not row["reducedTinnitus"]
                 and row["result"] == "admitted"]
    if {(row["cue"], row["layer"]) for row in open_rows} \
            != {(cue, layer) for cue in CUES for layer in LAYERS} or len(open_rows) != 48:
        raise ValueError("open coverage must contain exactly 16 cues x 3 layers")
    wall_rows = [row for row in rows if row["obstructed"] and row["result"] == "admitted"]
    if {row["cue"] for row in wall_rows} != set(CUES) or len(wall_rows) != 16:
        raise ValueError("wall coverage must contain exactly all 16 cues")
    burst = [by_id[event] for event in burst_ids]
    if sum(row["result"] == "admitted" for row in burst) != 4 \
            or sum(row["result"] == "dropped" for row in burst) != 5:
        raise ValueError("burst cap proof must show four admitted and five dropped")
    ordinary = by_id[index["ordinaryCelestialEventId"]]
    reduced = by_id[index["reducedCelestialEventId"]]
    if ordinary["cue"] != "celestial_ring" or ordinary["reducedTinnitus"] \
            or reduced["cue"] != "celestial_ring" or not reduced["reducedTinnitus"] \
            or ordinary["result"] != "admitted" or reduced["result"] != "admitted":
        raise ValueError("ordinary/reduced Celestial coverage mismatch")
    if any(by_id[event]["result"] != "admitted" for event in lifecycle.values()):
        raise ValueError("lifecycle event was not admitted after reset")
    if by_id[lifecycle["dimension"]]["dimension"] \
            == by_id[lifecycle["reload"]]["dimension"]:
        raise ValueError("dimension lifecycle did not cross dimensions")

    subtitles = _read_json(root / "subtitles.json")
    if not isinstance(subtitles, dict) or set(subtitles) \
            != {"schemaVersion", "implementationSha", "captures"} \
            or subtitles["schemaVersion"] != 1 \
            or subtitles["implementationSha"] != implementation_sha:
        raise ValueError("subtitle manifest schema mismatch")
    captures = subtitles["captures"]
    if not isinstance(captures, list) or len(captures) != 16 \
            or {capture.get("subtitleKey") for capture in captures if isinstance(capture, dict)} \
            != {f"subtitles.powers.{cue}" for cue in CUES}:
        raise ValueError("subtitle coverage mismatch")
    image_names = set()
    for capture in captures:
        if set(capture) != {"subtitleKey", "imagePath", "sha256"}:
            raise ValueError("subtitle capture schema mismatch")
        name = _safe_name(capture["imagePath"], "subtitle image")
        path = root / "screenshots" / name
        if name in image_names or not path.is_file() or path.is_symlink() \
                or not isinstance(capture["sha256"], str) \
                or HEX64.fullmatch(capture["sha256"]) is None \
                or hashlib.sha256(path.read_bytes()).hexdigest() != capture["sha256"]:
            raise ValueError("subtitle screenshot identity/checksum mismatch")
        image_names.add(name)
        with Image.open(path) as image:
            if image.format != "PNG" or image.size not in {(1280, 720), (2560, 1440)}:
                raise ValueError("subtitle screenshot must be a 1280x720 capture at 1x or 2x")
    screenshots = root / "screenshots"
    actual_images = {path.name for path in screenshots.iterdir() if path.is_file()}
    if actual_images != image_names:
        raise ValueError("subtitle screenshot inventory mismatch")

    _validate_metrics(_read_json(root / "audio-metrics.json"))
    spectrogram = _read_json(root / "spectrogram-summary.json")
    if not isinstance(spectrogram, dict) or set(spectrogram) \
            != {"schemaVersion", "implementationSha", "cues"} \
            or spectrogram["schemaVersion"] != 1 \
            or spectrogram["implementationSha"] != implementation_sha \
            or {entry.get("cue") for entry in spectrogram["cues"]} != set(CUES):
        raise ValueError("spectrogram summary mismatch")
    metadata = _read_json(root / "build-metadata.json")
    if not isinstance(metadata, dict) or metadata.get("schemaVersion") != 1 \
            or metadata.get("implementationSha") != implementation_sha \
            or metadata.get("result") not in {"PENDING", "PASS"}:
        raise ValueError("build metadata mismatch")
    readme = (root / "README.md").read_text(encoding="utf-8")
    if "no microphone recording" not in readme.lower():
        raise ValueError("evidence limitations must state no microphone recording")

    return {"schemaVersion": 1, "implementationSha": implementation_sha,
            "rowCount": len(rows), "openCount": len(open_rows),
            "wallCount": len(wall_rows), "burstCount": len(burst),
            "subtitleCount": len(captures), "lifecycleScenarios": sorted(lifecycle)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--output", type=Path)
    options = parser.parse_args()
    result = validate(options.root)
    output = options.output or options.root / "verification-report.json"
    output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"VFX-007 audio passed: {result['rowCount']} rows; "
          f"{result['openCount']} open; {result['wallCount']} wall")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
