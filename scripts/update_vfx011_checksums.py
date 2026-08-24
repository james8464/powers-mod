#!/usr/bin/env python3
"""Regenerate the complete VFX-011 evidence checksum inventory."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"
CHECKSUMS = EVIDENCE / "SHA256SUMS"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=Path, default=EVIDENCE)
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    checksums = evidence / "SHA256SUMS"
    files = [path for path in evidence.rglob("*") if path.is_file() and path != checksums]
    files.extend((
        ROOT / "docs/quality/vfx-011-asset-audit.json",
        ROOT / "docs/quality/vfx-011-reviewed-exceptions.json",
    ))
    files.extend((ROOT / "docs/quality/vfx-011-asset-pages").glob("*.png"))
    files = sorted(files, key=lambda path: path.relative_to(ROOT).as_posix())
    checksums.write_text("".join(
        f"{digest(path)}  {path.relative_to(ROOT).as_posix()}\n" for path in files))
    print(f"bound {len(files)} files")


if __name__ == "__main__":
    main()
