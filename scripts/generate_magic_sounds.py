#!/usr/bin/env python3
"""Generate restrained original magic cues and encode them as Minecraft Vorbis assets."""

from pathlib import Path
import math
import random
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "src/main/resources/assets/powers/sounds/magic"
RATE = 44_100
DURATION = 5.0


def ringing_sample(time: float, rng: random.Random) -> float:
    """Tinnitus-like high partials over a short sub-bass impact and decaying air."""
    attack = min(1.0, time / 0.025)
    decay = math.exp(-time * 0.38)
    high = math.sin(2.0 * math.pi * 7_420.0 * time)
    shimmer = math.sin(2.0 * math.pi * (3_710.0 + 18.0 * math.sin(time * 1.7)) * time)
    rumble = math.sin(2.0 * math.pi * 54.0 * time) * math.exp(-time * 7.0)
    air = (rng.random() * 2.0 - 1.0) * math.exp(-time * 1.4)
    return attack * (0.22 * high * decay + 0.075 * shimmer * decay + 0.34 * rumble + 0.018 * air)


def main() -> None:
	OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
	for name, duration, seed, pitch in (
		("celestial_ring", 5.0, 0xCE1E57, 1.0),
		("beam_ring", 0.85, 0xBEA7, 1.18),
		("boss_impact_ring", 1.35, 0xB055, 0.82),
	):
		rng = random.Random(seed)
		samples = np.empty(int(RATE * duration), dtype=np.float32)
		for index in range(len(samples)):
			time = index / RATE
			value = ringing_sample(time * pitch, rng) * math.exp(-time / duration)
			samples[index] = max(-0.92, min(0.92, value))
		sf.write(OUTPUT_DIR / f"{name}.ogg", samples, RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    main()
