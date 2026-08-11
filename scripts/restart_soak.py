#!/usr/bin/env python3
"""Run an isolated dedicated world through repeated clean restarts and diagnostics."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import selectors
import signal
import subprocess
import sys
import time


ROOT = Path(__file__).resolve().parents[1]
SOAK_ROOT = (ROOT / "build" / "restart-soak").resolve()
READY_MARKER = "Done ("
REQUIRED_CLEAN_MARKERS = (
    "forcedChunks=0",
    "proxies=0",
    "travelLoads=0",
    "celestialEvents=0",
)


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--hours", type=float, default=24.0)
    parser.add_argument("--cycle-seconds", type=int, default=300)
    parser.add_argument("--boot-timeout", type=int, default=180)
    return parser.parse_args()


def checked_settings(args: argparse.Namespace) -> tuple[float, int, int]:
    if not 0.01 <= args.hours <= 168.0:
        raise ValueError("--hours must be between 0.01 and 168")
    if not 10 <= args.cycle_seconds <= 3600:
        raise ValueError("--cycle-seconds must be between 10 and 3600")
    if not 30 <= args.boot_timeout <= 600:
        raise ValueError("--boot-timeout must be between 30 and 600")
    return args.hours * 3600.0, args.cycle_seconds, args.boot_timeout


def send(process: subprocess.Popen[str], command: str) -> None:
    if process.stdin is None:
        raise RuntimeError("Dedicated server stdin closed")
    process.stdin.write(command + "\n")
    process.stdin.flush()


def one_cycle(runtime: Path, cycle_seconds: int, boot_timeout: int, index: int) -> dict[str, object]:
    started = time.monotonic()
    process = subprocess.Popen(
        [
            "./gradlew", "runServer", "--no-daemon", "--console=plain",
            f"-PpowersRunDir={runtime}",
        ],
        cwd=ROOT,
        env=os.environ.copy(),
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
        start_new_session=True,
    )
    assert process.stdout is not None
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    lines: list[str] = []
    ready_at: float | None = None
    diagnose_sent = False
    stopped = False
    try:
        while True:
            now = time.monotonic()
            if process.poll() is not None:
                break
            for key, _ in selector.select(timeout=0.5):
                line = key.fileobj.readline()
                if not line:
                    continue
                lines.append(line.rstrip())
                sys.stdout.write(f"[{index:04d}] {line}")
                if READY_MARKER in line and ready_at is None:
                    ready_at = now
            if ready_at is None and now - started > boot_timeout:
                raise TimeoutError("Dedicated server did not become ready")
            if ready_at is not None and not diagnose_sent and now - ready_at >= cycle_seconds:
                send(process, "powers diagnose")
                diagnose_sent = True
            if diagnose_sent and not stopped and now - ready_at >= cycle_seconds + 2:
                send(process, "save-all flush")
                send(process, "stop")
                stopped = True
            if stopped and now - ready_at > cycle_seconds + 60:
                raise TimeoutError("Dedicated server did not stop cleanly")
        exit_code = process.wait(timeout=10)
        output = "\n".join(lines)
        clean = exit_code == 0 and ready_at is not None and all(
            marker in output for marker in REQUIRED_CLEAN_MARKERS
        )
        return {
            "cycle": index,
            "seconds": round(time.monotonic() - started, 3),
            "exit_code": exit_code,
            "ready": ready_at is not None,
            "clean_diagnostics": clean,
            "error_lines": [line for line in lines if "[Server thread/ERROR]" in line],
        }
    finally:
        selector.close()
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait(timeout=15)


def main() -> int:
    duration_seconds, cycle_seconds, boot_timeout = checked_settings(arguments())
    SOAK_ROOT.mkdir(parents=True, exist_ok=True)
    runtime = (SOAK_ROOT / "runtime").resolve()
    if SOAK_ROOT not in runtime.parents:
        raise RuntimeError("Refusing a non-isolated restart-soak directory")
    runtime.mkdir(parents=True, exist_ok=True)
    (runtime / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    properties = runtime / "server.properties"
    if not properties.exists():
        properties.write_text("online-mode=false\nlevel-name=world\n", encoding="utf-8")

    began = time.monotonic()
    cycles: list[dict[str, object]] = []
    failure = ""
    index = 1
    try:
        while time.monotonic() - began < duration_seconds:
            result = one_cycle(runtime, cycle_seconds, boot_timeout, index)
            cycles.append(result)
            if not result["clean_diagnostics"] or result["error_lines"]:
                failure = f"cycle {index} failed clean-state validation"
                break
            index += 1
    except (OSError, RuntimeError, TimeoutError, ValueError) as error:
        failure = str(error)

    report = {
        "schema": 1,
        "requested_hours": duration_seconds / 3600.0,
        "cycle_seconds": cycle_seconds,
        "elapsed_seconds": round(time.monotonic() - began, 3),
        "cycles": cycles,
        "passed": not failure and time.monotonic() - began >= duration_seconds,
        "failure": failure,
        "runtime": str(runtime.relative_to(ROOT)),
    }
    report_path = SOAK_ROOT / "restart-soak-report.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Restart-soak report: {report_path}")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
