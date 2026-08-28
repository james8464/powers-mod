#!/usr/bin/env python3
"""Create and verify deterministic inventory/checksum metadata for VFX-006 evidence."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


GENERATED = {"SHA256SUMS", "evidence-inventory.txt", "archive-inventory.txt"}
PRIVATE_MARKERS = (b"/Users/", b"\\Users\\", b".worktrees/", b"file://")


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _payload(root: Path) -> list[Path]:
    paths = []
    for path in root.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"symlink is forbidden: {path.relative_to(root)}")
        if path.is_file() and path.name not in GENERATED and path.name != ".DS_Store":
            paths.append(path)
    return sorted(paths, key=lambda path: path.relative_to(root).as_posix())


def _privacy(paths: list[Path]) -> None:
    for path in paths:
        data = path.read_bytes()
        if any(marker in data for marker in PRIVATE_MARKERS):
            raise ValueError(f"privacy leak: {path.name}")


def package(root: Path) -> dict:
    root = root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    paths = _payload(root)
    _privacy(paths)
    relative = [path.relative_to(root).as_posix() for path in paths]
    checksums = [f"{_sha256(path)}  {name}" for path, name in zip(paths, relative)]
    inventory = "".join(f"{name}\n" for name in relative)
    digest_text = "".join(f"{line}\n" for line in checksums)
    (root / "evidence-inventory.txt").write_text(inventory, encoding="utf-8")
    (root / "archive-inventory.txt").write_text(inventory, encoding="utf-8")
    (root / "SHA256SUMS").write_text(digest_text, encoding="utf-8")
    return {"fileCount": len(paths), "files": relative, "checksums": checksums}


def verify(root: Path) -> bool:
    root = root.resolve()
    paths = _payload(root)
    _privacy(paths)
    relative = [path.relative_to(root).as_posix() for path in paths]
    expected_inventory = "".join(f"{name}\n" for name in relative)
    for name in ("evidence-inventory.txt", "archive-inventory.txt"):
        path = root / name
        if not path.is_file() or path.read_text(encoding="utf-8") != expected_inventory:
            raise ValueError(f"inventory mismatch: {name}")
    expected_checksums = "".join(
        f"{_sha256(path)}  {name}\n" for path, name in zip(paths, relative))
    checksums = root / "SHA256SUMS"
    if not checksums.is_file() or checksums.read_text(encoding="utf-8") != expected_checksums:
        raise ValueError("checksum mismatch")
    return True


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--verify", action="store_true")
    options = parser.parse_args()
    if options.verify:
        verify(options.root)
        print("VFX-006 evidence package verified")
    else:
        result = package(options.root)
        verify(options.root)
        print(f"VFX-006 evidence packaged: {result['fileCount']} files")


if __name__ == "__main__":
    main()
