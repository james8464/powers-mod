#!/usr/bin/env python3
"""Run the exact VFX-011 client gate and retain its terminal result and capture receipt."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from pathlib import Path

from package_vfx011_evidence import load_rows, validate_raw_screenshots


ROOT = Path(__file__).resolve().parents[1]
COMMAND = (
    "./gradlew", "runClientGameTest", "-Pvfx011ClientOnly", "--rerun-tasks",
    "--no-daemon", "--console=plain",
)
CAPTURES = ROOT / "build/run/clientGameTest/vfx-011-gallery/captures.jsonl"
SCREENSHOTS = ROOT / "build/run/clientGameTest/screenshots"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sanitize(text: str) -> str:
    text = re.sub(r"/(?:Users|home)/[^/\s]+", "<HOME>", text)
    return re.sub(r"(?:127\.0\.0\.1|localhost):\d+", "<LOOPBACK>", text)


def validate_commit(commit: str) -> None:
    if len(commit) != 40 or any(character not in "0123456789abcdef" for character in commit):
        raise ValueError("implementation commit must be an exact lowercase SHA-1")


def capture_summary(captures: Path = CAPTURES, screenshots: Path = SCREENSHOTS) -> dict:
    rows = load_rows(captures)
    capture_ids = [capture_id for row in rows for capture_id in row["captureIds"]]
    if len(rows) != 971 or len({row["screenshot"] for row in rows}) != 971:
        raise ValueError("exact client gate must emit 971 unique screenshot rows")
    if len(capture_ids) != 9_034 or len(set(capture_ids)) != 9_034:
        raise ValueError("exact client gate must emit 9,034 unique capture IDs")
    validate_raw_screenshots(rows, screenshots)
    return {
        "metadataSha256": digest(captures),
        "rows": len(rows),
        "uniqueScreenshots": len({row["screenshot"] for row in rows}),
        "verifiedDigests": len(rows),
        "captureIds": len(capture_ids),
        "uniqueCaptureIds": len(set(capture_ids)),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--transcript", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--implementation-commit", required=True)
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args()
    validate_commit(args.implementation_commit)
    if not args.jar.is_file():
        raise FileNotFoundError(args.jar)
    args.transcript.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    with args.transcript.open("w") as transcript:
        process = subprocess.Popen(
            COMMAND, cwd=ROOT, env=os.environ.copy(), stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, text=True, bufsize=1)
        assert process.stdout is not None
        for line in process.stdout:
            clean = sanitize(line)
            transcript.write(clean)
            transcript.flush()
            sys.stdout.write(clean)
            sys.stdout.flush()
        exit_code = process.wait()
        marker = f"VFX011_CLIENT_COMMAND_EXIT={exit_code}\n"
        transcript.write(marker)
        transcript.flush()
        sys.stdout.write(marker)
    receipt = {
        "schema": 1,
        "result": "FAIL" if exit_code else "PENDING_VALIDATION",
        "command": list(COMMAND),
        "exitCode": exit_code,
        "implementationCommit": args.implementation_commit,
        "jar": {"file": args.jar.name, "sha256": digest(args.jar)},
        "transcript": {
            "file": args.transcript.resolve().relative_to(args.receipt.resolve().parent).as_posix(),
            "sha256": digest(args.transcript),
        },
    }
    if exit_code == 0:
        terminal = args.transcript.read_text(errors="replace")
        if "BUILD SUCCESSFUL" not in terminal:
            receipt["result"] = "INVALID"
            receipt["validationError"] = "terminal transcript lacks BUILD SUCCESSFUL"
            args.receipt.write_text(json.dumps(receipt, indent=2) + "\n")
            raise SystemExit(2)
        try:
            summary = capture_summary()
        except Exception as error:
            receipt["result"] = "INVALID"
            receipt["validationError"] = str(error)
            args.receipt.write_text(json.dumps(receipt, indent=2) + "\n")
            raise
        receipt.update({
            "result": "PASS",
            "clientEmittedMetadata": {
                "file": "captures.jsonl", "sha256": summary["metadataSha256"],
                "rows": summary["rows"],
            },
            "rawScreenshots": {
                "rows": summary["rows"],
                "uniqueScreenshots": summary["uniqueScreenshots"],
                "verifiedDigests": summary["verifiedDigests"],
            },
            "captureIds": {
                "rows": summary["captureIds"], "unique": summary["uniqueCaptureIds"],
            },
        })
        print("VFX011_CLIENT_CAPTURE_SUMMARY "
              f"rows={summary['rows']} uniqueScreenshots={summary['uniqueScreenshots']} "
              f"captureIds={summary['captureIds']} verifiedRawDigests={summary['verifiedDigests']}")
    args.receipt.write_text(json.dumps(receipt, indent=2) + "\n")
    raise SystemExit(exit_code)


if __name__ == "__main__":
    main()
