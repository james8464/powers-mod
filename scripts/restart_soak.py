#!/usr/bin/env python3
"""Run isolated dedicated-server restart cycles with one real Fabric client."""

from __future__ import annotations

import argparse
import json
import math
import os
from pathlib import Path
import queue
import shutil
import signal
import subprocess
import sys
import threading
import time
from typing import TextIO


ROOT = Path(__file__).resolve().parents[1]
SOAK_ROOT = (ROOT / "build" / "restart-soak").resolve()
READY_MARKER = "Done ("
CLIENT_JOINED_MARKER = "SoakClient joined the game"
CLIENT_LEFT_MARKER = "SoakClient left the game"
REQUIRED_DIAGNOSTIC_MARKERS = (
    "forcedChunks=0",
    "proxies=0",
    "travelLoads=0",
    "celestialEvents=1",
)
PHASE_MARKERS = {
    "startup_verified": "POWERS_SOAK_VERIFY",
    "seeded": "POWERS_SOAK_SEED",
    "settled": "POWERS_SOAK_SETTLED",
    "rollover_seeded": "POWERS_SOAK_ROLLOVER",
}


def shutdown_mode(cycle: int) -> str:
    """Use one recoverable flushed SIGTERM cycle per hour at five-minute cadence."""
    if cycle < 1:
        raise ValueError("cycle must be positive")
    return "sigterm" if cycle % 12 == 0 else "clean"


def accepted_window_start(current_start: float, passed: bool, now: float) -> float:
    """A failed cycle begins a new uninterrupted acceptance window."""
    return current_start if passed else now


def required_cycle_count(duration_seconds: float, cycle_seconds: int) -> int:
    """Require complete workload windows rather than accepting partial final cycles."""
    return math.ceil(duration_seconds / cycle_seconds)


def rollover_lead_seconds(cycle_seconds: int) -> int:
    """Reserve enough time to persist and inspect the next restart's recovery fixture."""
    return min(30, max(5, cycle_seconds // 2))


def cycle_boundary_wait_seconds(started: float, now: float, cycle_seconds: int) -> float:
    """Keep successful restart cycles on their declared wall-clock cadence."""
    return max(0.0, started + cycle_seconds - now)


def acceptance_passed(failure: str, completed_cycles: int, requested_cycles: int,
                      elapsed_seconds: float, required_seconds: float) -> bool:
    """Never accept a nominal cycle count before its full wall duration elapsed."""
    return (not failure and completed_cycles == requested_cycles
            and elapsed_seconds >= required_seconds)


def total_connected_seconds(cycles: list[dict[str, object]]) -> float:
    """Report measured client workload instead of inferring it from cycle labels."""
    return round(sum(float(cycle.get("connected_workload_seconds", 0.0))
                     for cycle in cycles), 3)


def cycle_passed(result: dict[str, object]) -> bool:
    """Evaluate one cycle without allowing an expected SIGTERM to mask missing proof."""
    required = (
        "ready", "client_connected", "startup_verified", "seeded", "settled",
        "status_verified", "rollover_seeded",
    )
    mode = str(result.get("shutdown_mode", ""))
    code = result.get("exit_code")
    expected_exit = code == 0 if mode == "clean" else code in (
        0, -int(signal.SIGTERM), 128 + int(signal.SIGTERM))
    return (all(result.get(field) is True for field in required)
            and int(result.get("client_ability_actions", 0)) > 0
            and expected_exit and not result.get("error_lines"))


def client_command(java: Path, launch_config: Path, argument_file: Path,
                   game_directory: Path, script: Path) -> list[str]:
    """Build one explicit rendered development-client reconnect command."""
    return [
        str(java), "-Xms128m", "-Xmx640m",
        "-Dpowers.qa.role=restart-soak",
        "-Dpowers.qa.server=127.0.0.1:25565",
        f"-Dpowers.qa.script={script}",
        f"-Dfabric.dli.config={launch_config}",
        "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient",
        f"@{argument_file}", "-XstartOnFirstThread",
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED",
        "net.fabricmc.devlaunchinjector.Main", "--username", "SoakClient",
        "--gameDir", str(game_directory), "--width", "320", "--height", "240",
    ]


def arena_setup_commands() -> tuple[str, str, str]:
    """Reset each reconnect to an open, loaded arena before spawning test entities."""
    return (
        "execute in minecraft:overworld run teleport SoakClient 0.5 100 0.5 0 0",
        "execute at SoakClient run fill ~-12 ~ ~-12 ~12 ~8 ~12 minecraft:air",
        "execute at SoakClient run fill ~-12 ~-1 ~-12 ~12 ~-1 ~12 minecraft:stone",
    )


def should_echo(line: str, quiet: bool) -> bool:
    """Keep long acceptance runs readable without removing their stored raw logs."""
    if not quiet:
        return True
    return any(marker in line for marker in (
        "POWERS_SOAK_", READY_MARKER, CLIENT_JOINED_MARKER, CLIENT_LEFT_MARKER,
        "Saved the game", "/ERROR]", "BUILD FAILED", "Restart-soak report:",
    ))


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--hours", type=float, default=24.0)
    parser.add_argument("--cycle-seconds", type=int, default=300)
    parser.add_argument("--boot-timeout", type=int, default=180)
    parser.add_argument("--cycles", type=int)
    parser.add_argument("--reset-runtime", action="store_true")
    parser.add_argument("--quiet", action="store_true")
    return parser.parse_args()


def checked_settings(args: argparse.Namespace) -> tuple[float, int, int, int]:
    if not 0.01 <= args.hours <= 168.0:
        raise ValueError("--hours must be between 0.01 and 168")
    if not 10 <= args.cycle_seconds <= 3600:
        raise ValueError("--cycle-seconds must be between 10 and 3600")
    if not 30 <= args.boot_timeout <= 600:
        raise ValueError("--boot-timeout must be between 30 and 600")
    duration = args.hours * 3600.0
    cycles = args.cycles if args.cycles is not None else required_cycle_count(
        duration, args.cycle_seconds)
    if cycles < 1 or cycles > 10_000:
        raise ValueError("--cycles must be between 1 and 10000")
    if args.cycles is not None:
        duration = cycles * args.cycle_seconds
    return duration, args.cycle_seconds, args.boot_timeout, cycles


def send(process: subprocess.Popen[str], command: str) -> None:
    if process.stdin is None:
        raise RuntimeError("Dedicated server stdin closed")
    process.stdin.write(command + "\n")
    process.stdin.flush()


def enqueue_output(stream: TextIO, destination: queue.Queue[str]) -> None:
    """Copy complete text lines from one process without retaining a second log."""
    for line in stream:
        destination.put(line)


def stop_process_group(process: subprocess.Popen[object], timeout: int = 20) -> None:
    """Stop only the explicitly launched process group and bound escalation time."""
    if process.poll() is not None:
        return
    os.killpg(process.pid, signal.SIGTERM)
    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait(timeout=timeout)


def prepare_client_launch() -> tuple[Path, Path, Path]:
    """Compile the exact development client once before the acceptance clock starts."""
    subprocess.run(
        ["./gradlew", "classes", "clientClasses", "configureClientLaunch",
         "--no-daemon", "--console=plain"],
        cwd=ROOT, env=os.environ.copy(), check=True,
    )
    java_home = os.environ.get("JAVA_HOME", "")
    java = Path(java_home) / "bin" / "java" if java_home else Path(
        shutil.which("java") or "")
    launch = ROOT / ".gradle" / "loom-cache" / "launch.cfg"
    arguments_file = ROOT / "build" / "loom-cache" / "argFiles" / "runClient"
    for required in (java, launch, arguments_file):
        if not required.is_file():
            raise RuntimeError(f"Missing client launch input: {required}")
    return java.resolve(), launch.resolve(), arguments_file.resolve()


def phase_seen(lines: list[str], marker: str, cycle: int, occurrence: int = 1) -> bool:
    needle = f"{marker} cycle={cycle} passed=true"
    return sum(needle in line for line in lines) >= occurrence


def failed_phase(lines: list[str], cycle: int) -> str:
    """Return the first authoritative negative marker for the active cycle."""
    needle = f"cycle={cycle} passed=false"
    return next((line for line in lines if "POWERS_SOAK_" in line and needle in line), "")


def one_cycle(runtime: Path, cycle_seconds: int, boot_timeout: int, index: int,
              launch_inputs: tuple[Path, Path, Path], script: Path,
              quiet: bool) -> dict[str, object]:
    """Execute one connected workload, settle owners, persist Ruin, and restart."""
    started = time.monotonic()
    mode = shutdown_mode(index)
    process = subprocess.Popen(
        ["./gradlew", "runServer", "--no-daemon", "--console=plain",
         f"-PpowersRunDir={runtime}"],
        cwd=ROOT, env=os.environ.copy(), stdin=subprocess.PIPE,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
        start_new_session=True,
    )
    if process.stdout is None:
        raise RuntimeError("Dedicated server stdout closed")
    output_queue: queue.Queue[str] = queue.Queue()
    reader = threading.Thread(
        target=enqueue_output, args=(process.stdout, output_queue),
        name=f"restart-soak-server-{index}", daemon=True,
    )
    reader.start()
    lines: list[str] = []
    ready_at: float | None = None
    connected_at: float | None = None
    client: subprocess.Popen[bytes] | None = None
    client_log_handle = None
    setup_sent = False
    pre_shutdown_sent = False
    shutdown_sent = False
    client_left = False
    workload_ended_at: float | None = None
    client_logs = SOAK_ROOT / "client-logs"
    server_logs = SOAK_ROOT / "server-logs"
    client_logs.mkdir(parents=True, exist_ok=True)
    server_logs.mkdir(parents=True, exist_ok=True)
    client_game = SOAK_ROOT / "client-runtime"
    client_game.mkdir(parents=True, exist_ok=True)
    client_log = client_logs / f"cycle-{index:04d}.log"
    server_log = server_logs / f"cycle-{index:04d}.log"
    try:
        while process.poll() is None:
            now = time.monotonic()
            while True:
                try:
                    line = output_queue.get_nowait()
                except queue.Empty:
                    break
                stripped = line.rstrip()
                lines.append(stripped)
                if should_echo(line, quiet):
                    sys.stdout.write(f"[{index:04d}] {line}")
                    sys.stdout.flush()
                if READY_MARKER in line and ready_at is None:
                    ready_at = now
                    java, launch, argument_file = launch_inputs
                    client_log_handle = client_log.open("wb")
                    client = subprocess.Popen(
                        client_command(java, launch, argument_file, client_game, script),
                        cwd=ROOT, env=os.environ.copy(), stdout=client_log_handle,
                        stderr=subprocess.STDOUT, start_new_session=True,
                    )
                if CLIENT_JOINED_MARKER in line and connected_at is None:
                    connected_at = now
                if CLIENT_LEFT_MARKER in line:
                    client_left = True
            negative = failed_phase(lines, index)
            if negative:
                raise RuntimeError(f"negative lifecycle marker: {negative}")
            if ready_at is None and now - started > boot_timeout:
                raise TimeoutError("Dedicated server did not become ready")
            if ready_at is not None and connected_at is None and now - ready_at > boot_timeout:
                raise TimeoutError("SoakClient did not connect")
            if connected_at is not None and not setup_sent:
                for command in arena_setup_commands():
                    send(process, command)
                send(process, "execute as SoakClient run powers testing on")
                send(process, f"powers testing soak verify {index}")
                send(process, f"powers testing soak seed {index}")
                setup_sent = True
            if (connected_at is not None and not pre_shutdown_sent
                    and now - connected_at >= cycle_seconds - rollover_lead_seconds(cycle_seconds)):
                if not phase_seen(lines, PHASE_MARKERS["settled"], index):
                    raise RuntimeError("Seeded owners did not settle before rollover")
                send(process, f"powers testing soak status {index}")
                send(process, f"powers testing soak rollover {index}")
                send(process, f"powers testing soak status {index}")
                send(process, "powers diagnose")
                send(process, "powers diagnose export")
                send(process, "save-all flush")
                pre_shutdown_sent = True
            second_status = phase_seen(lines, "POWERS_SOAK_STATUS", index, 2)
            saved = any("Saved the game" in line for line in lines)
            if pre_shutdown_sent and second_status and saved and not shutdown_sent:
                if client is not None:
                    workload_ended_at = time.monotonic()
                    stop_process_group(client)
                if mode == "clean":
                    send(process, "stop")
                else:
                    os.killpg(process.pid, signal.SIGTERM)
                shutdown_sent = True
            if shutdown_sent and now - (connected_at or started) > cycle_seconds + 90:
                raise TimeoutError("Dedicated server did not stop after flushed shutdown")
            time.sleep(0.05)
        exit_code = process.wait(timeout=20)
        reader.join(timeout=2)
        while not output_queue.empty():
            line = output_queue.get_nowait()
            lines.append(line.rstrip())
            if CLIENT_LEFT_MARKER in line:
                client_left = True
        output = "\n".join(lines)
        result: dict[str, object] = {
            "cycle": index,
            "shutdown_mode": mode,
            "exit_code": exit_code,
            "ready": ready_at is not None,
            "client_connected": connected_at is not None,
            "client_disconnected": client_left,
            "startup_verified": phase_seen(lines, PHASE_MARKERS["startup_verified"], index),
            "seeded": phase_seen(lines, PHASE_MARKERS["seeded"], index),
            "settled": phase_seen(lines, PHASE_MARKERS["settled"], index),
            "status_verified": phase_seen(lines, "POWERS_SOAK_STATUS", index, 2),
            "rollover_seeded": phase_seen(lines, PHASE_MARKERS["rollover_seeded"], index),
            "clean_diagnostics": all(marker in output for marker in REQUIRED_DIAGNOSTIC_MARKERS),
            "error_lines": [line for line in lines if "[Server thread/ERROR]" in line
                            or "[ServerMain/ERROR]" in line],
            "server_log": str(server_log.relative_to(ROOT)),
            "client_log": str(client_log.relative_to(ROOT)),
            "connected_workload_seconds": round(max(0.0,
                    (workload_ended_at or time.monotonic()) - (connected_at or time.monotonic())), 3),
        }
        result["client_ability_actions"] = client_log.read_text(
            encoding="utf-8", errors="replace").count("executed ACTIVATE")
        result["passed"] = cycle_passed(result) and result["clean_diagnostics"] is True
        server_log.write_text(output + "\n", encoding="utf-8")
        boundary_wait = cycle_boundary_wait_seconds(started, time.monotonic(), cycle_seconds)
        if boundary_wait > 0.0:
            time.sleep(boundary_wait)
        result["seconds"] = round(time.monotonic() - started, 3)
        return result
    finally:
        if client is not None:
            stop_process_group(client)
        if client_log_handle is not None:
            client_log_handle.close()
        stop_process_group(process)
        reader.join(timeout=2)


def prepare_runtime(reset: bool) -> Path:
    runtime = (SOAK_ROOT / "runtime").resolve()
    if SOAK_ROOT not in runtime.parents:
        raise RuntimeError("Refusing a non-isolated restart-soak directory")
    if reset and runtime.exists():
        shutil.rmtree(runtime)
    runtime.mkdir(parents=True, exist_ok=True)
    (runtime / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    properties = runtime / "server.properties"
    if not properties.exists():
        properties.write_text(
            "online-mode=false\nlevel-name=world\nview-distance=8\nsimulation-distance=6\n",
            encoding="utf-8",
        )
    return runtime


def main() -> int:
    args = arguments()
    duration_seconds, cycle_seconds, boot_timeout, requested_cycles = checked_settings(args)
    SOAK_ROOT.mkdir(parents=True, exist_ok=True)
    runtime = prepare_runtime(args.reset_runtime)
    launch_inputs = prepare_client_launch()
    script = (ROOT / "scripts" / "restart-soak-client.tsv").resolve()
    if not script.is_file():
        raise RuntimeError(f"Missing restart workload script: {script}")

    began = time.monotonic()
    acceptance_window = time.time()
    cycles: list[dict[str, object]] = []
    failure = ""
    try:
        for index in range(1, requested_cycles + 1):
            result = one_cycle(runtime, cycle_seconds, boot_timeout, index, launch_inputs,
                               script, args.quiet)
            cycles.append(result)
            passed = result["passed"] is True
            acceptance_window = accepted_window_start(acceptance_window, passed, time.time())
            if not passed:
                failure = f"cycle {index} failed connected lifecycle validation"
                break
    except (OSError, RuntimeError, TimeoutError, ValueError,
            subprocess.CalledProcessError) as error:
        failure = str(error)
        acceptance_window = accepted_window_start(acceptance_window, False, time.time())

    completed_cycles = sum(result.get("passed") is True for result in cycles)
    elapsed_seconds = round(time.monotonic() - began, 3)
    report = {
        "schema": 2,
        "git_commit": subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True,
            capture_output=True, check=True).stdout.strip(),
        "requested_hours": duration_seconds / 3600.0,
        "cycle_seconds": cycle_seconds,
        "requested_cycles": requested_cycles,
        "completed_cycles": completed_cycles,
        "connected_workload_seconds": total_connected_seconds(cycles),
        "elapsed_seconds": elapsed_seconds,
        "acceptance_window_started_epoch": acceptance_window,
        "cycles": cycles,
        "passed": acceptance_passed(failure, completed_cycles, requested_cycles,
                                    elapsed_seconds, duration_seconds),
        "failure": failure,
        "runtime": str(runtime.relative_to(ROOT)),
    }
    report_path = SOAK_ROOT / "restart-soak-report.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Restart-soak report: {report_path}")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
