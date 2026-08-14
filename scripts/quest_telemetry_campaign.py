#!/usr/bin/env python3
"""Run ten Light and ten Darkness human-cadence sessions with real Fabric clients."""

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
OUTPUT = (ROOT / "build" / "quest-telemetry-campaign").resolve()
READY_MARKER = "Done ("
CAMPAIGN_PORT = 25_566
PHASE_TICKS = {"LIGHT": 700_000, "DARK": 635_000}
ROW = re.compile(r"\b(LIGHT|DARK);(10|[1-9]);(\d+);(\d+);(\d+);([a-z0-9_,.-]+)")
CLIENT_ERROR = re.compile(r"(?:^|\])[^\n]*\bERROR\b")
EXPECTED_OFFLINE_CLIENT_ERRORS = ("Failed to retrieve profile key pair",)


def client_names(alignment: str) -> list[str]:
    prefix = "QuestLight" if alignment.upper() == "LIGHT" else "QuestDark"
    return [f"{prefix}{index}" for index in range(1, 11)]


def client_options() -> str:
    return (
        "maxFps:10\nrenderDistance:2\nsimulationDistance:5\n"
        "entityDistanceScaling:0.5\nparticles:2\ngraphicsMode:fast\n"
    )


def classify_client_errors(lines: list[str]) -> tuple[list[str], list[str]]:
    errors = [line for line in lines if CLIENT_ERROR.search(line)]
    expected = [line for line in errors
                if any(marker in line for marker in EXPECTED_OFFLINE_CLIENT_ERRORS)]
    unexpected = [line for line in errors if line not in expected]
    return expected, unexpected


def client_command(java: Path, launch: Path, arguments_file: Path,
                   game_directory: Path, username: str) -> list[str]:
    return [
        str(java), "-Xms128m", "-Xmx512m",
        "-Dpowers.qa.role=quest-telemetry",
        f"-Dpowers.qa.server=127.0.0.1:{CAMPAIGN_PORT}",
        f"-Dfabric.dli.config={launch}", "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient",
        f"@{arguments_file}", "-XstartOnFirstThread",
        "--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED",
        "net.fabricmc.devlaunchinjector.Main", "--username", username,
        "--gameDir", str(game_directory), "--width", "320", "--height", "240",
    ]


def parse_telemetry_rows(lines: list[str]) -> dict[tuple[str, int], dict[str, object]]:
    rows: dict[tuple[str, int], dict[str, object]] = {}
    for line in lines:
        match = ROW.search(line)
        if not match:
            continue
        alignment, level, samples, median, p90, routes = match.groups()
        rows[(alignment, int(level))] = {
            "alignment": alignment, "level": int(level), "samples": int(samples),
            "median_ticks": int(median), "p90_ticks": int(p90),
            "routes": routes.split(","),
        }
    return rows


def publication_ready(rows: dict[tuple[str, int], dict[str, object]]) -> bool:
    return len(rows) == 20 and all(
        int(rows[(alignment, level)]["samples"]) >= 10
        for alignment in ("LIGHT", "DARK") for level in range(1, 11)
    )


def send(process: subprocess.Popen[str], command: str) -> None:
    if process.stdin is None:
        raise RuntimeError("Dedicated server stdin closed")
    process.stdin.write(command + "\n")
    process.stdin.flush()


def enqueue(stream: TextIO, destination: queue.Queue[str]) -> None:
    for line in stream:
        destination.put(line)


def stop_group(process: subprocess.Popen[object], timeout: int = 20) -> None:
    if process.poll() is not None:
        return
    os.killpg(process.pid, signal.SIGTERM)
    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait(timeout=timeout)


def prepare_launch() -> tuple[Path, Path, Path]:
    subprocess.run(["./gradlew", "classes", "clientClasses", "configureClientLaunch",
                    "--no-daemon", "--console=plain"], cwd=ROOT, check=True)
    java_home = os.environ.get("JAVA_HOME", "")
    java = Path(java_home) / "bin" / "java" if java_home else Path(shutil.which("java") or "")
    launch = ROOT / ".gradle" / "loom-cache" / "launch.cfg"
    arguments_file = ROOT / "build" / "loom-cache" / "argFiles" / "runClient"
    if not arguments_file.is_file():
        bootstrap = subprocess.Popen(
            ["./gradlew", "runClient", "--rerun", "--no-daemon", "--console=plain"],
            cwd=ROOT, stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT,
            start_new_session=True)
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline and not arguments_file.is_file() \
                and bootstrap.poll() is None:
            time.sleep(0.1)
        stop_group(bootstrap)
    for required in (java, launch, arguments_file):
        if not required.is_file():
            raise RuntimeError(f"Missing client launch input: {required}")
    return java.resolve(), launch.resolve(), arguments_file.resolve()


def drain(source: queue.Queue[str], lines: list[str], log: TextIO) -> None:
    while True:
        try:
            line = source.get_nowait()
        except queue.Empty:
            return
        lines.append(line.rstrip())
        log.write(line)
        log.flush()


def wait_for(predicate, server: subprocess.Popen[str], source: queue.Queue[str],
             lines: list[str], log: TextIO, timeout: float, reason: str) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        drain(source, lines, log)
        if predicate(lines):
            return
        if server.poll() is not None:
            raise RuntimeError(f"Dedicated server exited while waiting for {reason}")
        time.sleep(0.05)
    raise TimeoutError(f"Timed out waiting for {reason}")


def launch_clients(alignment: str, inputs: tuple[Path, Path, Path]) \
        -> list[tuple[subprocess.Popen[bytes], object]]:
    launched = []
    for username in client_names(alignment):
        directory = OUTPUT / "clients" / username
        directory.mkdir(parents=True, exist_ok=True)
        (directory / "options.txt").write_text(client_options(), encoding="utf-8")
        log_handle = (OUTPUT / "client-logs" / f"{username}.log").open("wb")
        process = subprocess.Popen(
            client_command(*inputs, directory, username), cwd=directory,
            stdout=log_handle, stderr=subprocess.STDOUT, start_new_session=True)
        launched.append((process, log_handle))
    return launched


def stop_clients(clients: list[tuple[subprocess.Popen[bytes], object]]) -> None:
    for process, _ in clients:
        stop_group(process)
    for _, handle in clients:
        handle.close()


def run_phase(alignment: str, server: subprocess.Popen[str], source: queue.Queue[str],
              lines: list[str], log: TextIO, inputs: tuple[Path, Path, Path]) -> dict[str, object]:
    clients = launch_clients(alignment, inputs)
    names = client_names(alignment)
    try:
        wait_for(lambda output: all(any(f"{name} joined the game" in line for line in output)
                                    for name in names), server, source, lines, log, 240,
                 f"ten {alignment} clients")
        send(server, f"powers testing quest-campaign start {alignment.lower()}")
        wait_for(lambda output: any("POWERS_QUEST_CAMPAIGN_START passed=true" in line
                                    and f"alignment={alignment}" in line for line in output),
                 server, source, lines, log, 30, f"{alignment} campaign start")
        started = time.monotonic()
        send(server, f"tick sprint {PHASE_TICKS[alignment]}")
        wait_for(lambda output: any(f"POWERS_QUEST_CAMPAIGN alignment={alignment} passed=true"
                                    in line for line in output), server, source, lines, log, 900,
                 f"{alignment} rank-ten completion")
        send(server, "powers testing quest-campaign status")
        wait_for(lambda output: any("POWERS_QUEST_CAMPAIGN_STATUS passed=true" in line
                                    and f"alignment={alignment}; finished=true" in line
                                    for line in output), server, source, lines, log, 60,
                 f"{alignment} completion status")
        return {"alignment": alignment, "clients": names,
                "wall_seconds": round(time.monotonic() - started, 3),
                "human_equivalent_ticks": PHASE_TICKS[alignment]}
    finally:
        if server.poll() is None:
            send(server, "powers testing quest-campaign clear")
            time.sleep(1.0)
            drain(source, lines, log)
        stop_clients(clients)


def main() -> int:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    (OUTPUT / "client-logs").mkdir(parents=True)
    runtime = OUTPUT / "runtime"
    runtime.mkdir()
    (runtime / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (runtime / "server.properties").write_text(
        f"online-mode=false\nserver-port={CAMPAIGN_PORT}\nlevel-name=world\nmax-players=20\nview-distance=4\n"
        "simulation-distance=4\ndifficulty=peaceful\nspawn-monsters=false\nspawn-animals=false\n",
        encoding="utf-8")
    inputs = prepare_launch()
    commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    process = subprocess.Popen(
        ["./gradlew", "runServer", "--no-daemon", "--console=plain",
         f"-PpowersRunDir={runtime}"], cwd=ROOT, env=os.environ.copy(),
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, bufsize=1, start_new_session=True)
    if process.stdout is None:
        raise RuntimeError("Dedicated server stdout closed")
    source: queue.Queue[str] = queue.Queue()
    threading.Thread(target=enqueue, args=(process.stdout, source), daemon=True).start()
    lines: list[str] = []
    server_log_path = OUTPUT / "server.log"
    started = time.monotonic()
    phases: list[dict[str, object]] = []
    try:
        with server_log_path.open("w", encoding="utf-8") as log:
            wait_for(lambda output: any(READY_MARKER in line for line in output),
                     process, source, lines, log, 180, "server readiness")
            phases.append(run_phase("LIGHT", process, source, lines, log, inputs))
            phases.append(run_phase("DARK", process, source, lines, log, inputs))
            send(process, "powers testing quest-telemetry")
            wait_for(lambda output: len(parse_telemetry_rows(output)) == 20,
                     process, source, lines, log, 60, "twenty telemetry rows")
            send(process, "save-all flush")
            time.sleep(1.0)
            send(process, "stop")
            process.wait(timeout=60)
            drain(source, lines, log)
    finally:
        stop_group(process)
    rows = parse_telemetry_rows(lines)
    server_errors = [line for line in lines if "/ERROR]" in line or "BUILD FAILED" in line]
    client_lines = []
    for client_log in sorted((OUTPUT / "client-logs").glob("*.log")):
        client_lines.extend(client_log.read_text(encoding="utf-8", errors="replace").splitlines())
    expected_client_errors, unexpected_client_errors = classify_client_errors(client_lines)
    errors = server_errors + unexpected_client_errors
    report = {
        "schema": 1, "commit": commit, "minecraft": "26.2",
        "fabric_clients": 20, "sessions_per_alignment": 10,
        "cadence_basis": "server game ticks at authored human-equivalent intervals",
        "wall_seconds": round(time.monotonic() - started, 3), "phases": phases,
        "rows": [rows[key] for key in sorted(rows)],
        "server_error_lines": server_errors,
        "expected_offline_client_error_count": len(expected_client_errors),
        "unexpected_client_error_lines": unexpected_client_errors,
        "error_lines": errors,
        "passed": process.returncode == 0 and not errors and publication_ready(rows),
    }
    report_path = OUTPUT / "quest-telemetry-report.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Quest telemetry report: {report_path}")
    print(f"passed={report['passed']} rows={len(rows)} commit={commit}")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
