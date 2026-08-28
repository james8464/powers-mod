#!/usr/bin/env python3
"""Derive deterministic near, mid, far, and reduced-tinnitus OGG layers."""

from __future__ import annotations

import argparse
import hashlib
import math
from pathlib import Path
import struct

import numpy as np
from scipy import signal
import soundfile as sf


CUES = (
    "rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
    "celestial_ring", "beam_ring", "boss_impact_ring", "time_release",
    "rift_open", "rift_close", "soul_tether", "light_chorus",
    "dark_whisper", "ward_impact", "rank_awaken", "interaction_clash",
)
LAYERS = {
    "near": (15_000.0, 0.88, 2.0),
    "mid": (7_000.0, 0.50, 8.0),
    "far": (2_800.0, 0.24, 18.0),
}
RATE = 44_100
MAX_PEAK = 0.707


def soften_transient(samples: np.ndarray, attack_ms: float) -> np.ndarray:
    attack = max(1, round(RATE * attack_ms / 1_000.0))
    envelope = np.ones(len(samples), dtype=np.float64)
    envelope[:attack] = np.linspace(0.0, 1.0, attack, endpoint=True)
    return samples * envelope


def lowpass(samples: np.ndarray, cutoff: float) -> np.ndarray:
    sos = signal.butter(4, cutoff, btype="lowpass", fs=RATE, output="sos")
    return signal.sosfilt(sos, samples)


def limit(samples: np.ndarray) -> np.ndarray:
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    if peak > MAX_PEAK:
        samples = samples * (MAX_PEAK / peak)
    return samples.astype(np.float32)


def ordinary_layer(master: np.ndarray, name: str) -> np.ndarray:
    cutoff, gain, attack_ms = LAYERS[name]
    source = master
    if name == "far":
        slowed = signal.resample_poly(master, 100, 72)
        source = np.pad(slowed[:len(master)], (0, max(0, len(master) - len(slowed))))
    return limit(soften_transient(lowpass(source, cutoff), attack_ms) * gain)


def reduced_celestial_master(master: np.ndarray) -> np.ndarray:
    absolute = np.abs(master)
    window = max(1, round(RATE * 0.025))
    envelope = np.convolve(absolute, np.ones(window) / window, mode="same")
    maximum = float(np.max(envelope))
    if maximum > 0.0:
        envelope /= maximum
    time = np.arange(len(master), dtype=np.float64) / RATE
    warning = 0.30 * np.sin(2.0 * math.pi * 220.0 * time)
    warning += 0.14 * np.sin(2.0 * math.pi * 880.0 * time)
    warning += 0.06 * np.sin(2.0 * math.pi * 1_320.0 * time)
    return warning * envelope


def ogg_crc_table() -> tuple[int, ...]:
    table = []
    for value in range(256):
        remainder = value << 24
        for _ in range(8):
            remainder = ((remainder << 1) ^ 0x04C11DB7) & 0xFFFFFFFF \
                if remainder & 0x80000000 else (remainder << 1) & 0xFFFFFFFF
        table.append(remainder)
    return tuple(table)


CRC_TABLE = ogg_crc_table()


def ogg_crc(page: bytearray) -> int:
    value = 0
    for byte in page:
        value = ((value << 8) & 0xFFFFFFFF) ^ CRC_TABLE[((value >> 24) & 0xFF) ^ byte]
    return value


def canonicalize_ogg(path: Path, identity: str) -> None:
    data = bytearray(path.read_bytes())
    serial = int.from_bytes(hashlib.sha256(identity.encode("utf-8")).digest()[:4], "little")
    offset = 0
    while offset < len(data):
        if data[offset:offset + 4] != b"OggS" or offset + 27 > len(data):
            raise ValueError(f"Malformed generated OGG page in {path}")
        segment_count = data[offset + 26]
        header_end = offset + 27 + segment_count
        if header_end > len(data):
            raise ValueError(f"Truncated generated OGG lacing table in {path}")
        page_end = header_end + sum(data[offset + 27:header_end])
        if page_end > len(data):
            raise ValueError(f"Truncated generated OGG body in {path}")
        data[offset + 14:offset + 18] = serial.to_bytes(4, "little")
        data[offset + 22:offset + 26] = b"\x00\x00\x00\x00"
        checksum = ogg_crc(data[offset:page_end])
        data[offset + 22:offset + 26] = struct.pack("<I", checksum)
        offset = page_end
    path.write_bytes(data)


def write_ogg(path: Path, samples: np.ndarray, identity: str) -> None:
    sf.write(path, samples, RATE, format="OGG", subtype="VORBIS")
    canonicalize_ogg(path, identity)


def generate(root: Path) -> int:
    masters = root / "src" / "main" / "resources" / "assets" / "powers" / "sounds" / "magic"
    output = masters / "layered"
    output.mkdir(parents=True, exist_ok=True)
    expected = set()
    reduced_master = None
    for cue in CUES:
        samples, rate = sf.read(masters / f"{cue}.ogg", dtype="float64", always_2d=False)
        if rate != RATE or samples.ndim != 1 or not np.isfinite(samples).all():
            raise ValueError(f"{cue}.ogg must be finite mono {RATE} Hz audio")
        for layer in LAYERS:
            name = f"{cue}_{layer}.ogg"
            expected.add(name)
            write_ogg(output / name, ordinary_layer(samples, layer), name)
        if cue == "celestial_ring":
            reduced_master = reduced_celestial_master(samples)
    assert reduced_master is not None
    for layer in LAYERS:
        name = f"celestial_ring_reduced_{layer}.ogg"
        expected.add(name)
        write_ogg(output / name, ordinary_layer(reduced_master, layer), name)
    for stale in output.glob("*.ogg"):
        if stale.name not in expected:
            stale.unlink()
    return len(expected)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    count = generate(args.root.resolve())
    print(f"Generated {count} deterministic layered magic sounds")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
