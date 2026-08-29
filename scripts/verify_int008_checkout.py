#!/usr/bin/env python3
"""Fail closed unless an INT-008 capture SHA is the repository's checked-out HEAD."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


SHA = re.compile(r"[0-9a-f]{40}")


def verify(repository: Path, expected: str) -> str:
    repository = repository.resolve()
    if SHA.fullmatch(expected) is None:
        raise ValueError("expected SHA must be 40 lowercase hexadecimal characters")
    try:
        head = subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=repository, text=True,
            stderr=subprocess.PIPE).strip()
    except (OSError, subprocess.CalledProcessError) as error:
        raise ValueError("repository HEAD could not be resolved") from error
    if head != expected:
        raise ValueError(f"INT-008 capture SHA {expected} does not match checked-out HEAD {head}")
    status = subprocess.check_output(
        ["git", "status", "--porcelain", "--untracked-files=all"],
        cwd=repository, text=True).strip()
    if status:
        raise ValueError("INT-008 capture working tree is not clean")
    return head


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", type=Path, required=True)
    parser.add_argument("--expected", required=True)
    options = parser.parse_args()
    try:
        head = verify(options.repository, options.expected)
    except ValueError as error:
        print(error, file=sys.stderr)
        return 1
    print(f"INT-008 checkout verified: {head}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
