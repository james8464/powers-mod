#!/usr/bin/env python3
"""Sanitize owned VFX-011 text evidence without dropping diagnostic lines."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"
TEXT_GLOBS = ("logs/*.log", "two-client/logs/*.log")
PRIVATE = re.compile(r"/(?:Users|home)/[^/\s]+|(?:127\.0\.0\.1|localhost):\d+")


def sanitized(text: str) -> str:
    text = re.sub(r"/(?:Users|home)/[^/\s]+", "<HOME>", text)
    text = re.sub(r"(?:127\.0\.0\.1|localhost):\d+", "<LOOPBACK>", text)
    return re.sub(r"[ \t]+(?=\r?$)", "", text, flags=re.MULTILINE)


def owned_files() -> list[Path]:
    return sorted(path for pattern in TEXT_GLOBS for path in EVIDENCE.glob(pattern))


def scan_files() -> list[Path]:
    return sorted(path for path in EVIDENCE.rglob("*")
                  if path.is_file() and path.suffix != ".png")


def refresh_receipt() -> None:
    path = EVIDENCE / "two-client/receipt.json"
    receipt = json.loads(path.read_text())
    for entry in receipt["logs"]:
        owned = EVIDENCE / "two-client/logs" / Path(entry["file"]).name
        entry["file"] = owned.name
        entry["sha256"] = hashlib.sha256(owned.read_bytes()).hexdigest()
    option_names = {
        "VfxObserver-idle": "VfxObserver-idle-options.txt",
        "VfxPrimary-darkness": "VfxPrimary-darkness-options.txt",
        "VfxPrimary-locator": "VfxPrimary-locator-options.txt",
    }
    for entry in receipt["options"]:
        recorded = Path(entry["file"])
        if recorded.name in option_names.values():
            name = recorded.name
        else:
            name = option_names[recorded.parent.name]
        owned = EVIDENCE / "two-client/options" / name
        entry["file"] = owned.name
        entry["sha256"] = hashlib.sha256(owned.read_bytes()).hexdigest()
    path.write_text(json.dumps(receipt, indent=2) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    offenders = []
    for path in owned_files():
        before = path.read_text(errors="surrogateescape")
        after = sanitized(before)
        if args.check:
            if before != after or PRIVATE.search(before):
                offenders.append(path.relative_to(ROOT).as_posix())
        else:
            if before.count("\n") != after.count("\n"):
                raise SystemExit(f"sanitizer changed line count: {path}")
            if before != after:
                path.write_text(after, errors="surrogateescape")
    if args.check:
        offenders.extend(path.relative_to(ROOT).as_posix() for path in scan_files()
                         if PRIVATE.search(path.read_text(errors="surrogateescape")))
        offenders = sorted(set(offenders))
        if offenders:
            raise SystemExit("unsanitized evidence: " + ", ".join(offenders))
        print(f"privacy scan passed: {len(scan_files())} files")
    else:
        refresh_receipt()
        print(f"sanitized {len(owned_files())} files and refreshed receipt")


if __name__ == "__main__":
    main()
