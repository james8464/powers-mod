#!/usr/bin/env python3
"""Synthesize POWERS' original, normalized mono Vorbis magic sound bank."""

from __future__ import annotations

import math
from pathlib import Path

import numpy as np
import soundfile as sf


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/powers/sounds/magic"
SAMPLE_RATE = 44_100
SOUNDS = {
    "rune_hum": (0.85, 96.0, 1.50, 0.08),
    "crystal_resonate": (1.10, 246.0, 2.01, 0.02),
    "amethyst_fracture": (0.72, 174.0, 1.31, 0.18),
    "time_suspend": (1.25, 82.0, 0.52, 0.05),
    "time_release": (0.78, 116.0, 2.42, 0.08),
    "rift_open": (1.18, 58.0, 1.88, 0.11),
    "rift_close": (0.84, 132.0, 0.63, 0.10),
    "soul_tether": (1.25, 147.0, 1.01, 0.04),
    "light_chorus": (1.42, 220.0, 2.50, 0.015),
    "dark_whisper": (1.34, 71.0, 0.77, 0.09),
    "ward_impact": (0.64, 112.0, 1.72, 0.22),
    "rank_awaken": (1.55, 164.0, 2.00, 0.03),
    "interaction_clash": (0.82, 104.0, 1.41, 0.20),
}


def envelope(time: np.ndarray, duration: float) -> np.ndarray:
    """Apply short attack and exponential decay to prevent clicks and clipping."""
    attack = np.clip(time / 0.035, 0.0, 1.0)
    release = np.clip((duration - time) / 0.16, 0.0, 1.0)
    return attack * release * np.exp(-time * 0.72)


def synthesize(name: str, duration: float, root: float, sweep: float, grit: float) -> np.ndarray:
    """Build one distinct ancient-cosmic cue from harmonics, sweep, and seeded dust."""
    count = round(duration * SAMPLE_RATE)
    time = np.arange(count, dtype=np.float64) / SAMPLE_RATE
    rng = np.random.default_rng(sum((index + 1) * ord(char) for index, char in enumerate(name)))
    phase = 2.0 * math.pi * root * (time + (sweep - 1.0) * time * time / (2.0 * duration))
    signal = 0.52 * np.sin(phase)
    signal += 0.24 * np.sin(phase * 1.5 + 0.45)
    signal += 0.13 * np.sin(phase * 2.01 + np.sin(time * math.tau * 2.0) * 0.8)
    signal += grit * rng.normal(0.0, 0.65, count) * np.exp(-time * 4.5)
    # A slow tremolo makes the bank feel ritualistic instead of electronic.
    signal *= 0.78 + 0.22 * np.sin(time * math.tau * (2.0 + root % 5) + 0.7)
    signal *= envelope(time, duration)
    peak = float(np.max(np.abs(signal)))
    if peak > 0.0:
        signal *= 0.72 / peak
    return signal.astype(np.float32)


def main() -> None:
    """Regenerate all mono OGG/Vorbis files deterministically."""
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, recipe in SOUNDS.items():
        sf.write(OUTPUT / f"{name}.ogg", synthesize(name, *recipe), SAMPLE_RATE,
                 format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    main()
