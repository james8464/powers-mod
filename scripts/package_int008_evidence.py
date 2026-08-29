#!/usr/bin/env python3
"""Build and verify deterministic metadata and tar.gz bytes for INT-008 evidence."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import importlib.util
import io
import tarfile
import tempfile
from pathlib import Path


GENERATED = {"SHA256SUMS", "evidence-inventory.txt", "archive-inventory.txt"}
TEXT_SUFFIXES = {".json", ".jsonl", ".md", ".txt", ".log", ".csv"}
PRIVATE_MARKERS = (b"/Users/", b"\\Users\\", b".worktrees/", b"file://",
                   b"james8464")
VERIFY_SPEC = importlib.util.spec_from_file_location(
    "verify_int008_temporal_for_package",
    Path(__file__).resolve().with_name("verify_int008_temporal.py"))
VERIFY_INT008 = importlib.util.module_from_spec(VERIFY_SPEC)
assert VERIFY_SPEC.loader is not None
VERIFY_SPEC.loader.exec_module(VERIFY_INT008)


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


def _validate_files(root: Path, paths: list[Path]) -> None:
    for path in paths:
        data = path.read_bytes()
        if any(marker in data for marker in PRIVATE_MARKERS):
            raise ValueError(f"privacy leak: {path.relative_to(root)}")
        if path.suffix.lower() in TEXT_SUFFIXES:
            if b"\r" in data:
                raise ValueError(f"text must use normalized LF: {path.relative_to(root)}")
            try:
                data.decode("utf-8")
            except UnicodeDecodeError as error:
                raise ValueError(f"text must be UTF-8: {path.relative_to(root)}") from error


def _metadata(root: Path, payload: list[Path]) -> list[Path]:
    payload_names = [path.relative_to(root).as_posix() for path in payload]
    all_names = sorted(payload_names + list(GENERATED))
    inventory = "".join(f"{name}\n" for name in all_names)
    (root / "evidence-inventory.txt").write_text(inventory, encoding="utf-8")
    (root / "archive-inventory.txt").write_text(inventory, encoding="utf-8")
    checksum_paths = sorted(payload + [root / "archive-inventory.txt",
                                       root / "evidence-inventory.txt"],
                            key=lambda path: path.relative_to(root).as_posix())
    checksums = "".join(f"{_sha256(path)}  {path.relative_to(root).as_posix()}\n"
                        for path in checksum_paths)
    (root / "SHA256SUMS").write_text(checksums, encoding="utf-8")
    return sorted(payload + [root / name for name in GENERATED],
                  key=lambda path: path.relative_to(root).as_posix())


def _archive(root: Path, paths: list[Path], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("wb") as raw:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0,
                           compresslevel=9) as zipped:
            with tarfile.open(fileobj=zipped, mode="w", format=tarfile.GNU_FORMAT) as archive:
                for path in paths:
                    data = path.read_bytes()
                    info = tarfile.TarInfo(path.relative_to(root).as_posix())
                    info.size, info.mtime, info.mode = len(data), 0, 0o644
                    info.uid = info.gid = 0
                    info.uname = info.gname = ""
                    archive.addfile(info, io.BytesIO(data))


def package(root: Path, output: Path, repository: Path) -> dict:
    root = root.resolve()
    if not root.is_dir() or root.is_symlink():
        raise ValueError("evidence root must be a directory")
    payload = _payload(root)
    _validate_files(root, payload)
    VERIFY_INT008.validate(root, repository)
    paths = _metadata(root, payload)
    _validate_files(root, paths)
    _archive(root, paths, output.resolve())
    verify(root, repository)
    verify_archive(output.resolve(), repository)
    return {"fileCount": len(paths),
            "files": [path.relative_to(root).as_posix() for path in paths],
            "archiveSha256": _sha256(output.resolve())}


def verify(root: Path, repository: Path) -> bool:
    root = root.resolve()
    payload = _payload(root)
    _validate_files(root, payload)
    expected_names = sorted([path.relative_to(root).as_posix() for path in payload]
                            + list(GENERATED))
    expected_inventory = "".join(f"{name}\n" for name in expected_names)
    for name in ("evidence-inventory.txt", "archive-inventory.txt"):
        path = root / name
        if not path.is_file() or path.read_text(encoding="utf-8") != expected_inventory:
            raise ValueError(f"inventory mismatch: {name}")
    checksum_paths = sorted(payload + [root / "archive-inventory.txt",
                                       root / "evidence-inventory.txt"],
                            key=lambda path: path.relative_to(root).as_posix())
    expected_checksums = "".join(
        f"{_sha256(path)}  {path.relative_to(root).as_posix()}\n" for path in checksum_paths)
    checksum_file = root / "SHA256SUMS"
    if not checksum_file.is_file() \
            or checksum_file.read_text(encoding="utf-8") != expected_checksums:
        raise ValueError("checksum mismatch")
    _validate_files(root, payload + [root / name for name in GENERATED])
    VERIFY_INT008.validate(root, repository)
    return True


def verify_archive(archive_path: Path, repository: Path) -> bool:
    archive_path = archive_path.resolve()
    if not archive_path.is_file() or archive_path.is_symlink():
        raise ValueError("archive must be a regular file")
    with tempfile.TemporaryDirectory() as raw:
        root = Path(raw)
        with tarfile.open(archive_path, mode="r:gz") as archive:
            members = archive.getmembers()
            names = [member.name for member in members]
            if names != sorted(names) or len(names) != len(set(names)):
                raise ValueError("archive members must be sorted and unique")
            for member in members:
                relative = Path(member.name)
                if member.name.startswith("/") or ".." in relative.parts \
                        or not member.isfile():
                    raise ValueError("unsafe archive member")
                if member.mtime != 0 or member.mode != 0o644 or member.uid != 0 \
                        or member.gid != 0 or member.uname or member.gname:
                    raise ValueError("non-deterministic archive metadata")
                source = archive.extractfile(member)
                if source is None:
                    raise ValueError("unreadable archive member")
                destination = root / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(source.read())
        inventory = (root / "archive-inventory.txt").read_text(encoding="utf-8").splitlines()
        if names != inventory:
            raise ValueError("archive inventory does not match members")
        verify(root, repository)
    return True


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    options = parser.parse_args()
    result = package(options.root, options.output, options.repository)
    print(f"INT-008 evidence packaged: {result['fileCount']} files; "
          f"sha256={result['archiveSha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
