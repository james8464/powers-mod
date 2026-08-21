#!/usr/bin/env python3
"""Rebuild the accepted VFX-011 metadata from its immutable capture index."""

from __future__ import annotations

import csv
import hashlib
import json
import re
from collections import OrderedDict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"
EXPECTED_SHA256 = "d80fbd866a7b99312dba938cf6a6d9cfe86fd902af70303e8d6d2e0a24eb82f6"


def source_key(capture_id: str) -> str:
    return re.sub(r"^configuration/(?:entity/)?mip[0-4]/(?:normal|reduced)/", "", capture_id)


def row_for(screenshot: str, capture_ids: list[str]) -> dict:
    mip_match = re.search(r"-mip([0-4])-", screenshot)
    mip = int(mip_match.group(1)) if mip_match else 4
    reduced = "-reduced" in screenshot
    if "-screen-" in screenshot:
        requested_scale = int(re.search(r"-scale([1-4])-", screenshot).group(1))
        # A 720px window clamps Minecraft's effective scale to three even when
        # the acceptance fixture requests scale four; the requested value stays
        # independently traceable in the capture ID.
        scale = min(requested_scale, 3)
        width = 960 if screenshot.endswith("-narrow.png") else 1280
        camera = "screen"
    else:
        scale = 3 if "-items-" in screenshot or "-entities-" in screenshot else 2
        width = 1280
        if "-items-" in screenshot:
            camera = re.search(r"-(?:evidence|configuration)-(.+?)-mip", screenshot).group(1)
        elif "-entities-" in screenshot:
            camera = "entity-" + re.search(r"-(front|back|left|right|equipped)\.png$", screenshot).group(1)
        elif "-hud-" in screenshot:
            camera = "hud"
        elif "-gameplay-first_person-" in screenshot:
            camera = "first-person"
        elif "-gameplay-third_person-" in screenshot:
            camera = "third-person"
        elif "-boss-" in screenshot:
            camera = "boss-overlay"
        else:
            raise ValueError(f"unclassified capture: {screenshot}")
    if "-configuration-" in screenshot or "-entities-config-" in screenshot:
        background = ("light", "dark", "checker")[mip % 3]
    else:
        background = "checker"
    return {
        "screenshot": screenshot,
        "captureIds": capture_ids,
        "sourceKeys": [source_key(capture_id) for capture_id in capture_ids],
        "physicalWidth": width,
        "physicalHeight": 720,
        "guiScale": scale,
        "mipLevel": mip,
        "reducedMotion": reduced,
        "background": background,
        "camera": camera,
        "gameTime": 6000,
        "weather": "clear",
    }


def main() -> None:
    grouped: OrderedDict[str, list[str]] = OrderedDict()
    with (EVIDENCE / "client-capture-index.tsv").open(newline="") as handle:
        for row in csv.DictReader(handle, delimiter="\t"):
            grouped.setdefault(row["screenshot"], []).append(row["capture_id"])
    payload = "".join(json.dumps(row_for(name, ids), separators=(",", ":")) + "\n"
                      for name, ids in grouped.items()).encode()
    actual = hashlib.sha256(payload).hexdigest()
    if len(grouped) != 971 or actual != EXPECTED_SHA256:
        raise SystemExit(f"reconstruction mismatch: rows={len(grouped)} sha256={actual}")
    (EVIDENCE / "captures.jsonl").write_bytes(payload)
    print(f"wrote {len(grouped)} rows sha256={actual}")


if __name__ == "__main__":
    main()
