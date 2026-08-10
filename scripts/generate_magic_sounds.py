#!/usr/bin/env python3
"""Generate restrained original magic cues and encode them as Minecraft Vorbis assets."""

from pathlib import Path
import math
import random
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/powers/sounds/magic/celestial_ring.ogg"
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
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    rng = random.Random(0xCE1E57)
    samples = np.empty(int(RATE * DURATION), dtype=np.float32)
    for index in range(int(RATE * DURATION)):
        value = max(-0.92, min(0.92, ringing_sample(index / RATE, rng)))
        samples[index] = value
    sf.write(OUTPUT, samples, RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
    main()
