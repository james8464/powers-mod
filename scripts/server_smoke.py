#!/usr/bin/env python3
"""Boots the dedicated server, waits for readiness, then requests a clean stop."""

from __future__ import annotations

import os
from pathlib import Path
import selectors
import subprocess
import sys
import time


ROOT = Path(__file__).resolve().parents[1]
READY = "Done ("
TIMEOUT_SECONDS = 180


def main() -> int:
    run = ROOT / "run"
    run.mkdir(exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    process = subprocess.Popen(
        ["./gradlew", "runServer", "--no-daemon", "--console=plain"],
        cwd=ROOT,
        env=os.environ.copy(),
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    assert process.stdout is not None
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    deadline = time.monotonic() + TIMEOUT_SECONDS
    ready = False
    try:
        while time.monotonic() < deadline:
            if process.poll() is not None:
                break
            for key, _ in selector.select(timeout=1.0):
                line = key.fileobj.readline()
                if not line:
                    continue
                sys.stdout.write(line)
                if READY in line:
                    ready = True
                    assert process.stdin is not None
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    break
            if ready:
                return process.wait(timeout=60)
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=15)
        return process.returncode or 1
    finally:
        selector.close()
        if process.poll() is None:
            process.kill()


if __name__ == "__main__":
    raise SystemExit(main())
