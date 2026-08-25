#!/usr/bin/env python3
"""Sanitize and checksum the complete retained VFX-009 evidence bundle."""

from __future__ import annotations

import argparse
import hashlib
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-25-vfx-009"
PRIVATE = re.compile(r"/(?:Users|home)/[^/\s]+")
TEXT_SUFFIXES = {".json", ".jsonl", ".log", ".md", ".properties", ".tsv", ".txt"}


def text_files(evidence: Path) -> list[Path]:
    return sorted(path for path in evidence.rglob("*")
                  if path.is_file() and path.suffix in TEXT_SUFFIXES)


def sanitize(text: str) -> str:
    return PRIVATE.sub("<HOME>", text)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def manifest_files(manifest: Path, evidence: Path) -> list[Path]:
    root = evidence if manifest.name == "EVIDENCE-SHA256SUMS" else manifest.parent
    return sorted((path for path in root.rglob("*") if path.is_file() and path != manifest),
                  key=lambda path: path.relative_to(ROOT).as_posix())


def refresh_manifests(evidence: Path) -> None:
    manifests = sorted(evidence.rglob("SHA256SUMS"))
    for manifest in manifests:
        manifest.write_text("".join(
            f"{digest(path)}  {path.relative_to(ROOT).as_posix()}\n"
            for path in manifest_files(manifest, evidence)), encoding="utf-8")
    root_manifest = evidence / "EVIDENCE-SHA256SUMS"
    root_manifest.write_text("".join(
        f"{digest(path)}  {path.relative_to(ROOT).as_posix()}\n"
        for path in manifest_files(root_manifest, evidence)), encoding="utf-8")


def verify_manifests(evidence: Path) -> list[str]:
    failures: list[str] = []
    manifests = [*sorted(evidence.rglob("SHA256SUMS")), evidence / "EVIDENCE-SHA256SUMS"]
    for manifest in manifests:
        actual = "".join(
            f"{digest(path)}  {path.relative_to(ROOT).as_posix()}\n"
            for path in manifest_files(manifest, evidence))
        if not manifest.is_file() or manifest.read_text(encoding="utf-8") != actual:
            failures.append(manifest.relative_to(ROOT).as_posix())
    return failures


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--evidence", type=Path, default=EVIDENCE)
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    files = text_files(evidence)
    offenders = [path.relative_to(ROOT).as_posix() for path in files
                 if PRIVATE.search(path.read_text(encoding="utf-8"))]
    if args.check:
        stale = verify_manifests(evidence)
        if offenders or stale:
            raise SystemExit("VFX-009 evidence check failed: " + ", ".join(offenders + stale))
        print(f"privacy and checksums passed: {len(files)} text files")
        return
    for path in files:
        before = path.read_text(encoding="utf-8")
        after = sanitize(before)
        if before.count("\n") != after.count("\n"):
            raise SystemExit(f"sanitizer changed line count: {path}")
        if before != after:
            path.write_text(after, encoding="utf-8")
    refresh_manifests(evidence)
    print(f"sanitized {len(offenders)} files; refreshed local and root manifests")


if __name__ == "__main__":
    main()
