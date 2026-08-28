#!/usr/bin/env python3
"""Quantitatively validate the committed VFX-007 layered audio bank."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import soundfile as sf


CUES = (
    "rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
    "celestial_ring", "beam_ring", "boss_impact_ring", "time_release",
    "rift_open", "rift_close", "soul_tether", "light_chorus",
    "dark_whisper", "ward_impact", "rank_awaken", "interaction_clash",
)
LAYERS = ("near", "mid", "far")
RATE = 44_100


def read_audio(path: Path, errors: list[str]) -> np.ndarray:
    try:
        samples, rate = sf.read(path, dtype="float64", always_2d=False)
    except Exception as error:
        errors.append(f"{path}: {error}")
        return np.zeros(1, dtype=np.float64)
    if rate != RATE:
        errors.append(f"{path}: expected {RATE} Hz, found {rate}")
    if samples.ndim != 1:
        errors.append(f"{path}: expected mono audio")
        samples = samples[:, 0]
    if not np.isfinite(samples).all():
        errors.append(f"{path}: samples must be finite")
        samples = np.nan_to_num(samples)
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    if peak > 0.7071:
        errors.append(f"{path}: peak {peak:.6f} exceeds 0.7071")
    return samples


def rms(samples: np.ndarray) -> float:
    return float(np.sqrt(np.mean(np.square(samples)))) if len(samples) else 0.0


def spectrum(samples: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    if not len(samples):
        return np.zeros(1), np.zeros(1)
    windowed = samples * np.hanning(len(samples))
    power = np.square(np.abs(np.fft.rfft(windowed)))
    frequency = np.fft.rfftfreq(len(samples), 1.0 / RATE)
    return frequency, power


def centroid(samples: np.ndarray) -> float:
    frequency, power = spectrum(samples)
    total = float(power.sum())
    return float((frequency * power).sum() / total) if total > 0.0 else 0.0


def band_energy(samples: np.ndarray, lower: float, upper: float) -> float:
    frequency, power = spectrum(samples)
    selected = power[(frequency >= lower) & (frequency <= upper)]
    return float(selected.sum())


def expected_entries() -> dict[str, tuple[str, str]]:
    entries = {}
    for cue in CUES:
        for layer in LAYERS:
            entries[f"{cue}.{layer}"] = (
                f"powers:magic/layered/{cue}_{layer}", f"subtitles.powers.{cue}")
    for layer in LAYERS:
        entries[f"celestial_ring.reduced.{layer}"] = (
            f"powers:magic/layered/celestial_ring_reduced_{layer}",
            "subtitles.powers.celestial_ring")
    return entries


def validate(root: Path) -> dict:
    errors: list[str] = []
    assets = root / "src" / "main" / "resources" / "assets" / "powers"
    audio_dir = assets / "sounds" / "magic" / "layered"
    actual_files = {path.name for path in audio_dir.glob("*.ogg")}
    expected_files = {
        f"{cue}_{layer}.ogg" for cue in CUES for layer in LAYERS
    } | {f"celestial_ring_reduced_{layer}.ogg" for layer in LAYERS}
    if actual_files != expected_files:
        errors.append(f"layered asset inventory mismatch: missing={sorted(expected_files - actual_files)}, "
                      f"extra={sorted(actual_files - expected_files)}")

    sounds = json.loads((assets / "sounds.json").read_text(encoding="utf-8"))
    lang = json.loads((assets / "lang" / "en_us.json").read_text(encoding="utf-8"))
    for event, (resource, subtitle) in expected_entries().items():
        definition = sounds.get(event)
        if definition != {"subtitle": subtitle, "sounds": [resource]}:
            errors.append(f"sounds.json event {event!r} does not resolve exactly to {resource!r}/{subtitle!r}")
    for cue in CUES:
        key = f"subtitles.powers.{cue}"
        if not isinstance(lang.get(key), str) or not lang[key].strip():
            errors.append(f"missing non-empty subtitle {key}")

    cues = []
    loaded = {}
    for cue in CUES:
        layers = {}
        for layer in LAYERS:
            layers[layer] = read_audio(audio_dir / f"{cue}_{layer}.ogg", errors)
        loaded[cue] = layers
        values = {layer: rms(samples) for layer, samples in layers.items()}
        if not values["near"] > values["mid"] > values["far"] > 0.0:
            errors.append(f"{cue}: RMS must decrease near > mid > far, found {values}")
        near_centroid = centroid(layers["near"])
        far_centroid = centroid(layers["far"])
        if near_centroid > 0.0 and far_centroid > near_centroid * 0.80:
            errors.append(f"{cue}: far centroid {far_centroid:.3f} is not 20% below near {near_centroid:.3f}")
        cues.append({
            "cue": cue,
            "effectiveRms": values,
            "peak": max(float(np.max(np.abs(value))) for value in layers.values()),
            "nearCentroid": near_centroid,
            "farCentroid": far_centroid,
        })

    reduced = {
        layer: read_audio(audio_dir / f"celestial_ring_reduced_{layer}.ogg", errors)
        for layer in LAYERS
    }
    ordinary_high = band_energy(loaded.get("celestial_ring", {}).get("near", np.zeros(1)), 4_000, 12_000)
    reduced_high = band_energy(reduced["near"], 4_000, 12_000)
    ratio = reduced_high / ordinary_high if ordinary_high > 0.0 else 1.0
    low_mid = band_energy(reduced["near"], 100, 2_000)
    if ratio > 0.30:
        errors.append(f"reduced Celestial high-band ratio {ratio:.6f} exceeds 0.30")
    if low_mid <= 0.000001:
        errors.append("reduced Celestial must retain measurable 100-2000 Hz warning energy")

    return {
        "schemaVersion": 1,
        "assetCount": len(actual_files),
        "errors": errors,
        "cues": cues,
        "reducedCelestialHighBandRatio": ratio,
        "reducedCelestialLowMidEnergy": low_mid,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    report = validate(args.root.resolve())
    if args.json:
        print(json.dumps(report, sort_keys=True))
    elif report["errors"]:
        print("\n".join(report["errors"]))
    else:
        print(f"Layered audio validation passed ({report['assetCount']} assets)")
    return 1 if report["errors"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
