#!/usr/bin/env python3
"""Run every registered server GameTest in its own fresh Fabric server process."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[1]
SERVER_GAMETEST_IMPORT = "net.fabricmc.fabric.api.gametest.v1.GameTest"
CLASS_PATTERN = re.compile(r"\bclass\s+([A-Za-z_][A-Za-z0-9_]*)")
METHOD_PATTERN = re.compile(
    r"@GameTest(?:\s*\([^)]*\))?(?:(?!@GameTest).)*?\bvoid\s+"
    r"([A-Za-z_][A-Za-z0-9_]*)\s*\(",
    re.DOTALL)
CAMEL_BOUNDARY = re.compile(r"([a-z])([A-Z])")
SERVER_ENTRYPOINTS_PATTERN = re.compile(
    r'"fabric-gametest"\s*:\s*\[(.*?)\]\s*,\s*"fabric-client-gametest"',
    re.DOTALL)
ENTRYPOINT_CLASS_PATTERN = re.compile(r'"(com\.powers\.[A-Za-z0-9_.]+|com\.example\.[A-Za-z0-9_.]+)"')


def snake_case(value: str) -> str:
    """Match Fabric's lower-snake GameTest identifier convention."""
    return CAMEL_BOUNDARY.sub(r"\1_\2", value).lower()


def registered_server_classes(root: Path) -> list[str]:
    """Read the ordinary server entrypoints without enabling opt-in test profiles."""
    manifest = root / "src/gametest/resources/fabric.mod.json"
    text = manifest.read_text(encoding="utf-8")
    section = SERVER_ENTRYPOINTS_PATTERN.search(text)
    if section is None:
        raise ValueError("fabric-gametest entrypoint list is missing")
    classes = ENTRYPOINT_CLASS_PATTERN.findall(section.group(1))
    if not classes:
        raise ValueError("fabric-gametest entrypoint list is empty")
    duplicates = sorted({name for name in classes if classes.count(name) > 1})
    if duplicates:
        raise ValueError(f"duplicate server GameTest entrypoints: {', '.join(duplicates)}")
    return classes


def discover_test_ids(root: Path) -> list[str]:
    """Return every registered server GameTest identifier in stable source order."""
    source_root = root / "src/gametest/java"
    discovered: list[str] = []
    for class_name in sorted(registered_server_classes(root)):
        source = source_root / (class_name.replace(".", "/") + ".java")
        if not source.is_file():
            candidates = list(source_root.rglob(class_name.rsplit(".", 1)[1] + ".java"))
            if len(candidates) != 1:
                raise ValueError(f"registered server GameTest source is missing: {source}")
            source = candidates[0]
        text = source.read_text(encoding="utf-8")
        if SERVER_GAMETEST_IMPORT not in text:
            raise ValueError(f"registered server GameTest has no server annotation import: {source}")
        class_match = CLASS_PATTERN.search(text)
        if class_match is None:
            raise ValueError(f"server GameTest source has no class: {source}")
        class_name = snake_case(class_match.group(1))
        for method in METHOD_PATTERN.findall(text):
            discovered.append(f"powers-gametest:{class_name}_{snake_case(method)}")
    # Minecraft's standard test registry contributes this one required bootstrap
    # test in addition to Fabric's annotated entrypoints.
    discovered.append("minecraft:always_pass")
    if not discovered:
        raise ValueError("discovered zero server GameTests")
    duplicates = sorted({test_id for test_id in discovered if discovered.count(test_id) > 1})
    if duplicates:
        raise ValueError(f"duplicate server GameTest IDs: {', '.join(duplicates)}")
    return sorted(discovered)


def log_path(log_directory: Path, index: int, test_id: str) -> Path:
    """Give each raw Gradle transcript a stable, shell-safe file name."""
    name = test_id.split(":", 1)[-1]
    return log_directory / f"{index:03d}-{name}.log"


def run_tests(root: Path, test_ids: list[str], log_directory: Path) -> list[str]:
    """Run exact filters sequentially, retaining raw output and stopping on failure."""
    if not test_ids:
        raise ValueError("no GameTests were requested")
    ordered = sorted(test_ids)
    log_directory.mkdir(parents=True, exist_ok=True)
    if any(log_directory.iterdir()):
        raise ValueError(f"evidence directory is not empty: {log_directory}")
    for index, test_id in enumerate(ordered, start=1):
        command = [
            "./gradlew", "runGameTest", f"-PgameTestFilter={test_id}",
            "--rerun-tasks", "--no-daemon", "--console=plain",
        ]
        result = subprocess.run(
            command, cwd=root, env=os.environ.copy(), text=True,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
        raw_output = result.stdout or ""
        log_path(log_directory, index, test_id).write_text(raw_output, encoding="utf-8")
        if result.returncode != 0:
            raise RuntimeError(
                f"{test_id} failed with exit {result.returncode}; raw log: "
                f"{log_path(log_directory, index, test_id)}")
        counts = re.findall(r"All (\d+) required tests passed", raw_output)
        if counts != ["1"] or "BUILD SUCCESSFUL" not in raw_output:
            raise RuntimeError(f"{test_id} lacks single-test success proof; raw log: "
                               f"{log_path(log_directory, index, test_id)}")
    return ordered


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--log-dir", type=Path)
    parser.add_argument("--test-id", action="append", dest="test_ids")
    parser.add_argument("--list", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = arguments()
    root = args.root.resolve()
    discovered = discover_test_ids(root)
    requested = discovered if args.test_ids is None else sorted(args.test_ids)
    unknown = sorted(set(requested).difference(discovered))
    if unknown:
        raise ValueError(f"requested unregistered GameTests: {', '.join(unknown)}")
    if args.list:
        print("\n".join(requested))
        return 0
    if args.log_dir is None:
        parent = root / "build/deterministic-gametests"
        parent.mkdir(parents=True, exist_ok=True)
        log_directory = Path(tempfile.mkdtemp(prefix="run-", dir=parent))
    else:
        log_directory = args.log_dir
    print(f"Raw GameTest evidence: {log_directory}", flush=True)
    passed = run_tests(root, requested, log_directory.resolve())
    print(f"All {len(passed)} deterministic GameTests passed:")
    print("\n".join(passed))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, RuntimeError, ValueError) as error:
        print(f"deterministic GameTest gate failed: {error}", file=sys.stderr)
        raise SystemExit(1)
