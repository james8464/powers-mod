#!/usr/bin/env python3
"""Capture PERF-005 before/after FX traffic through one real Fabric client."""

from __future__ import annotations

import json
import os
from pathlib import Path
import queue
import re
import shutil
import signal
import subprocess
import threading
import time
from typing import TextIO


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build" / "perf-005-fx-capture"
PORT = 25_567
CAPTURE = re.compile(
    r"POWERS_FX_CAPTURE passed=(true|false) attemptedPackets=(\d+) "
    r"deliveredPackets=(\d+) attemptedBytes=(\d+) deliveredBytes=(\d+) "
    r"packetReduction=([0-9.]+) byteReduction=([0-9.]+)")
EXPECTED_CLIENT_ERRORS = ("Failed to retrieve profile key pair",)
COLLISION_FILTER = (
    "powers-gametest:fx_coalescing_game_tests_"
    "duplicate_visual_updates_leave_beam_collision_authoritative"
)


def parse_capture(line: str) -> dict[str, int | float | bool]:
    match = CAPTURE.search(line)
    if match is None:
        raise ValueError("Missing POWERS_FX_CAPTURE marker")
    passed, attempted_packets, delivered_packets, attempted_bytes, delivered_bytes, \
        packet_reduction, byte_reduction = match.groups()
    return {
        "passed": passed == "true",
        "attempted_packets": int(attempted_packets),
        "delivered_packets": int(delivered_packets),
        "attempted_bytes": int(attempted_bytes),
        "delivered_bytes": int(delivered_bytes),
        "packet_reduction_percent": float(packet_reduction),
        "byte_reduction_percent": float(byte_reduction),
    }


def accepted(capture: dict[str, int | float | bool]) -> bool:
    return bool(capture["passed"]) \
        and float(capture["packet_reduction_percent"]) >= 25.0 \
        and float(capture["byte_reduction_percent"]) >= 25.0


def unexpected_client_errors(lines: list[str]) -> list[str]:
    errors = [line for line in lines if "ERROR" in line]
    return [line for line in errors
            if not any(expected in line for expected in EXPECTED_CLIENT_ERRORS)
            and not ("MinecraftClientHttpException[type=HTTP_ERROR, status=401" in line
                     and "path=/player/certificates" in line)]


def stop_group(process: subprocess.Popen[object], timeout: int = 20) -> None:
    if process.poll() is not None:
        return
    os.killpg(process.pid, signal.SIGTERM)
    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait(timeout=timeout)


def enqueue(stream: TextIO, destination: queue.Queue[str]) -> None:
    for line in stream:
        destination.put(line)


def drain(source: queue.Queue[str], lines: list[str], log: TextIO) -> None:
    while True:
        try:
            line = source.get_nowait()
        except queue.Empty:
            return
        lines.append(line.rstrip())
        log.write(line)
        log.flush()


def wait_for(predicate, process: subprocess.Popen[str], source: queue.Queue[str],
             lines: list[str], log: TextIO, timeout: float, reason: str) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        drain(source, lines, log)
        if predicate(lines):
            return
        if process.poll() is not None:
            raise RuntimeError(f"Server exited while waiting for {reason}")
        time.sleep(0.05)
    raise TimeoutError(f"Timed out waiting for {reason}")


def send(process: subprocess.Popen[str], command: str) -> None:
    if process.stdin is None:
        raise RuntimeError("Dedicated server stdin closed")
    process.stdin.write(command + "\n")
    process.stdin.flush()


def prepare_launch() -> tuple[Path, Path, Path]:
    subprocess.run(["./gradlew", "classes", "clientClasses", "configureClientLaunch",
                    "--no-daemon", "--console=plain"], cwd=ROOT, check=True)
    java_home = os.environ.get("JAVA_HOME", "")
    java = Path(java_home) / "bin" / "java" if java_home else Path(shutil.which("java") or "")
    launch = ROOT / ".gradle" / "loom-cache" / "launch.cfg"
    arguments = ROOT / "build" / "loom-cache" / "argFiles" / "runClient"
    for required in (java, launch, arguments):
        if not required.is_file():
            raise RuntimeError(f"Missing client launch input: {required}")
    return java.resolve(), launch.resolve(), arguments.resolve()


def client_command(inputs: tuple[Path, Path, Path], game_dir: Path,
                   scenario: Path) -> list[str]:
    java, launch, arguments = inputs
    return [
        str(java), "-Xms128m", "-Xmx512m",
        "-Dpowers.qa.role=perf-005",
        "-Dpowers.qa.server=127.0.0.1:25567",
        f"-Dpowers.qa.script={scenario}",
        f"-Dfabric.dli.config={launch}", "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient",
        f"@{arguments}", "-XstartOnFirstThread",
        "--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED",
        "net.fabricmc.devlaunchinjector.Main", "--username", "FxCapture",
        "--gameDir", str(game_dir), "--width", "320", "--height", "240",
    ]


def run_collision_gametest() -> None:
    subprocess.run(["./gradlew", "runGameTest", f"-PgameTestFilter={COLLISION_FILTER}",
                    "--no-daemon", "--console=plain"], cwd=ROOT, check=True)


def main() -> int:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    runtime = OUTPUT / "runtime"
    game_dir = OUTPUT / "client"
    runtime.mkdir(parents=True)
    game_dir.mkdir(parents=True)
    (runtime / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (runtime / "server.properties").write_text(
        f"online-mode=false\nserver-port={PORT}\nlevel-name=world\nmax-players=2\n"
        "view-distance=4\nsimulation-distance=4\ndifficulty=peaceful\n",
        encoding="utf-8")
    (game_dir / "options.txt").write_text(
        "maxFps:10\nrenderDistance:2\nsimulationDistance:5\nparticles:2\ngraphicsMode:fast\n",
        encoding="utf-8")
    scenario = OUTPUT / "scenario.tsv"
    scenario.write_text("100\tCOMMAND\tpowers testing fx-capture 64\n",
                        encoding="utf-8")
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT,
                                     text=True).strip()
    started = time.monotonic()
    run_collision_gametest()
    inputs = prepare_launch()
    server = subprocess.Popen(
        ["./gradlew", "runServer", "--no-daemon", "--console=plain",
         f"-PpowersRunDir={runtime}"], cwd=ROOT, env=os.environ.copy(),
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, bufsize=1, start_new_session=True)
    if server.stdout is None:
        raise RuntimeError("Dedicated server stdout closed")
    source: queue.Queue[str] = queue.Queue()
    threading.Thread(target=enqueue, args=(server.stdout, source), daemon=True).start()
    lines: list[str] = []
    client: subprocess.Popen[bytes] | None = None
    try:
        with (OUTPUT / "server.log").open("w", encoding="utf-8") as server_log:
            wait_for(lambda output: any("Done (" in line for line in output),
                     server, source, lines, server_log, 180, "server readiness")
            with (OUTPUT / "client.log").open("wb") as client_log:
                client = subprocess.Popen(client_command(inputs, game_dir, scenario),
                                          cwd=game_dir, stdout=client_log,
                                          stderr=subprocess.STDOUT, start_new_session=True)
                wait_for(lambda output: any("FxCapture joined the game" in line for line in output),
                         server, source, lines, server_log, 180, "real client connection")
                send(server, "op FxCapture")
                wait_for(lambda output: any("POWERS_FX_CAPTURE" in line for line in output),
                         server, source, lines, server_log, 60, "FX capture marker")
                send(server, "save-all flush")
                time.sleep(0.5)
                send(server, "stop")
                server.wait(timeout=60)
                drain(source, lines, server_log)
    finally:
        if client is not None:
            stop_group(client)
        stop_group(server)
    marker = next(line for line in lines if "POWERS_FX_CAPTURE" in line)
    capture = parse_capture(marker)
    client_lines = (OUTPUT / "client.log").read_text(
        encoding="utf-8", errors="replace").splitlines()
    client_errors = unexpected_client_errors(client_lines)
    server_errors = [line for line in lines if "/ERROR]" in line or "BUILD FAILED" in line]
    report = {
        "schema": 1,
        "commit": commit,
        "minecraft": "26.2",
        "fabric_clients": 1,
        "connected_client": "FxCapture",
        "collision_gametest_passed": True,
        "traffic": capture,
        "server_error_lines": server_errors,
        "unexpected_client_error_lines": client_errors,
        "wall_seconds": round(time.monotonic() - started, 3),
        "passed": server.returncode == 0 and accepted(capture)
                  and not server_errors and not client_errors,
    }
    (OUTPUT / "fx-coalescing-report.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
