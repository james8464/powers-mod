#!/usr/bin/env python3
"""Convenience wrapper for the canonical Java magic-document generator."""

from __future__ import annotations

import argparse
import os
import subprocess
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    task = "verifyMagicDocs" if args.check else "generateMagicDocs"
    environment = os.environ.copy()
    environment.setdefault("JAVA_HOME", "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home")
    return subprocess.call([str(root / "gradlew"), "-q", task], cwd=root, env=environment)


if __name__ == "__main__":
    raise SystemExit(main())
