#!/usr/bin/env python3
"""Capture exact-SHA VFX-007 audio evidence with a dedicated server and real clients."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import queue
import shutil
import signal
import subprocess
import threading
import time


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT / "build" / "vfx007-audio-capture"
PORT = 25_572
CUES = (
    "rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
    "celestial_ring", "beam_ring", "boss_impact_ring", "time_release",
    "rift_open", "rift_close", "soul_tether", "light_chorus",
    "dark_whisper", "ward_impact", "rank_awaken", "interaction_clash",
)
PROFILE = {
    "rune_hum": "intimate", "crystal_resonate": "intimate",
    "celestial_ring": "world", "boss_impact_ring": "world",
    "light_chorus": "world", "dark_whisper": "world", "rank_awaken": "world",
}
DISTANCES = {
    "intimate": {"near": 4, "mid": 16, "far": 50},
    "standard": {"near": 6, "mid": 30, "far": 90},
    "world": {"near": 10, "mid": 60, "far": 160},
}
AUDIT_MARKER = "powers_layered_audio_audit "


def render_primary_scenario() -> str:
    lines = [
        "70\tcommand\tgamemode creative @s",
        "80\tcommand\ttp @s 0 200 0",
        "81\tcommand\tfill -2 199 -2 2 199 0 stone",
        "90\tlook\t0 0",
        "100\tcommand\tfill -2 198 1 2 204 170 air",
        "180\taudio_comfort\tordinary",
    ]
    tick = 200
    for cue in CUES:
        profile = PROFILE.get(cue, "standard")
        for layer in ("near", "mid", "far"):
            distance = DISTANCES[profile][layer]
            lines.append(f"{tick}\taudio_emit\t{cue} {distance} open")
            lines.append(f"{tick}\taudio_assert\t{layer} admitted")
            if layer == "near":
                lines.append(f"{tick + 1}\tscreenshot\taudio-subtitle-{cue}")
            tick += 4
    lines.append(f"{tick}\tcommand\tfill -2 198 2 2 204 2 stone")
    tick += 20
    for cue in CUES:
        distance = DISTANCES[PROFILE.get(cue, "standard")]["near"]
        lines.append(f"{tick}\taudio_emit\t{cue} {distance} wall")
        lines.append(f"{tick}\taudio_assert\tmid admitted")
        tick += 4
    lines.append(f"{tick}\tcommand\tfill -2 198 1 2 204 170 air")
    tick += 20
    for distance in range(1, 10):
        lines.append(f"{tick}\taudio_emit\tinteraction_clash {distance} open")
    tick += 10
    lines.append(f"{tick}\taudio_comfort\treduced")
    lines.append(f"{tick + 1}\taudio_emit\tcelestial_ring 10 open")
    lines.append(f"{tick + 1}\taudio_assert\tnear admitted")
    tick += 10
    lines.append(f"{tick}\taudio_reload\tnow")
    tick += 100
    lines.append(f"{tick}\taudio_emit\trune_hum 4 open")
    lines.append(f"{tick}\taudio_assert\tnear admitted")
    tick += 10
    lines.append(f"{tick}\tcommand\texecute in minecraft:the_nether run tp @s 0 200 0")
    tick += 100
    lines.append(f"{tick}\tlook\t0 0")
    lines.append(f"{tick + 1}\taudio_emit\trune_hum 4 open")
    lines.append(f"{tick + 1}\taudio_assert\tnear admitted")
    return "".join(line + "\n" for line in lines)


def render_reconnect_scenario() -> str:
    return ("80\tcommand\texecute in minecraft:overworld run tp @s 0 200 0\n"
            "140\tlook\t0 0\n"
            "160\taudio_emit\trune_hum 4 open\n"
            "160\taudio_assert\tnear admitted\n")


def extract_audit_rows(log: str) -> list[dict]:
    rows = []
    for line in log.splitlines():
        if AUDIT_MARKER not in line:
            continue
        value = json.loads(line.split(AUDIT_MARKER, 1)[1])
        if not isinstance(value, dict) or value.get("schemaVersion") != 1:
            raise ValueError("client emitted a non-schema-1 audit row")
        rows.append(value)
    return rows


def resolve_argument_file(directory: Path) -> Path:
    candidate = directory / "runClient"
    if candidate.is_file() and candidate.stat().st_size > 0:
        return candidate
    raise RuntimeError(f"Missing generated runClient argument file in {directory}")


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _stop_group(process: subprocess.Popen, timeout: int = 20) -> None:
    if process.poll() is not None:
        return
    os.killpg(process.pid, signal.SIGTERM)
    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait(timeout=timeout)


def _wait_log(path: Path, marker: str, timeout: float) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if path.is_file() and marker in path.read_text(encoding="utf-8", errors="replace"):
            return
        time.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for client marker: {marker}")


def _wait_screenshots(game_dir: Path, count: int, timeout: float) -> list[Path]:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        images = sorted((game_dir / "screenshots").glob("*.png"))
        if len(images) >= count and all(path.stat().st_size > 0 for path in images):
            return images
        time.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for {count} screenshots")


def _client_command(java: Path, launch: Path, arguments: Path, game_dir: Path,
                    script: Path, username: str, role: str, implementation_sha: str) -> list[str]:
    return [
        str(java), "-Xms256m", "-Xmx1g", f"-Dpowers.qa.role={role}",
        f"-Dpowers.qa.server=127.0.0.1:{PORT}", f"-Dpowers.qa.script={script}",
        f"-Dpowers.vfx007.implementationSha={implementation_sha}",
        f"-Dfabric.dli.config={launch}", "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient",
        f"@{arguments}", "-XstartOnFirstThread", "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED", "net.fabricmc.devlaunchinjector.Main",
        "--username", username, "--gameDir", str(game_dir), "--width", "1280",
        "--height", "720",
    ]


def _launch_client(output: Path, inputs: tuple[Path, Path, Path], script: Path,
                   username: str, role: str, implementation_sha: str):
    java, launch, arguments = inputs
    game_dir = output / "clients" / role
    game_dir.mkdir(parents=True)
    (game_dir / "options.txt").write_text(
        "guiScale:2\nmaxFps:30\nrenderDistance:6\nsimulationDistance:5\n"
        "particles:0\ngraphicsPreset:\"custom\"\ntutorialStep:none\n"
        "chatVisibility:2\nshowSubtitles:true\n",
        encoding="utf-8")
    log_path = output / "raw" / f"{role}.log"
    handle = log_path.open("wb")
    process = subprocess.Popen(
        _client_command(java, launch, arguments, game_dir, script, username, role,
                        implementation_sha),
        cwd=game_dir, stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
    return process, handle, log_path, game_dir


def _prepare_launch() -> tuple[Path, Path, Path]:
    subprocess.run(["./gradlew", "classes", "clientClasses", "configureClientLaunch",
                    "--no-daemon", "--console=plain"], cwd=ROOT, check=True)
    java_home = os.environ.get("JAVA_HOME", "")
    java = Path(java_home) / "bin" / "java"
    launch = ROOT / ".gradle" / "loom-cache" / "launch.cfg"
    argument_directory = ROOT / "build" / "loom-cache" / "argFiles"
    try:
        arguments = resolve_argument_file(argument_directory)
    except RuntimeError:
        bootstrap = subprocess.Popen(
            ["./gradlew", "runClient", "--rerun", "--no-daemon", "--console=plain"],
            cwd=ROOT, env=os.environ.copy(), stdout=subprocess.DEVNULL,
            stderr=subprocess.STDOUT, start_new_session=True)
        try:
            deadline = time.monotonic() + 120
            while time.monotonic() < deadline:
                try:
                    arguments = resolve_argument_file(argument_directory)
                    time.sleep(0.1)
                    break
                except RuntimeError:
                    if bootstrap.poll() is not None:
                        raise RuntimeError("Loom exited before creating the runClient argument file")
                    time.sleep(0.05)
            else:
                raise TimeoutError("Loom did not create the runClient argument file")
        finally:
            _stop_group(bootstrap)
    for path in (java, launch):
        if not path.is_file():
            raise RuntimeError(f"Missing launch input: {path}")
    return java.resolve(), launch.resolve(), arguments.resolve()


def _write_json(path: Path, value) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def _write_evidence(output: Path, implementation_sha: str, rows: list[dict],
                    screenshot_sources: list[Path]) -> Path:
    evidence = output / "evidence"
    screenshots = evidence / "screenshots"
    logs = evidence / "logs"
    screenshots.mkdir(parents=True)
    logs.mkdir()
    (evidence / "audio-audit.jsonl").write_text(
        "".join(json.dumps(row, sort_keys=True, separators=(",", ":")) + "\n" for row in rows),
        encoding="utf-8")
    open_rows, wall_rows = rows[:48], rows[48:64]
    burst, reduced, reload_row, dimension_row, reconnect_row = (
        rows[64:73], rows[73], rows[74], rows[75], rows[76])
    _write_json(evidence / "capture-index.json", {
        "schemaVersion": 1, "implementationSha": implementation_sha,
        "burstEventIds": [row["eventId"] for row in burst],
        "ordinaryCelestialEventId": next(row["eventId"] for row in open_rows
                                          if row["cue"] == "celestial_ring"
                                          and row["layer"] == "near"),
        "reducedCelestialEventId": reduced["eventId"],
        "lifecycleEventIds": {"reload": reload_row["eventId"],
                              "dimension": dimension_row["eventId"],
                              "reconnect": reconnect_row["eventId"]},
    })
    captures = []
    for index, (cue, source) in enumerate(zip(CUES, screenshot_sources, strict=True)):
        target = screenshots / f"{index:02d}-{cue}.png"
        shutil.copyfile(source, target)
        captures.append({"subtitleKey": f"subtitles.powers.{cue}",
                         "imagePath": target.name, "sha256": _sha256(target)})
    _write_json(evidence / "subtitles.json", {
        "schemaVersion": 1, "implementationSha": implementation_sha,
        "captures": captures,
    })
    raw_metrics = subprocess.check_output(
        ["python3", "scripts/validate_layered_audio.py", "--json"], cwd=ROOT, text=True)
    metrics = json.loads(raw_metrics)
    _write_json(evidence / "audio-metrics.json", metrics)
    _write_json(evidence / "spectrogram-summary.json", {
        "schemaVersion": 1, "implementationSha": implementation_sha,
        "cues": [{"cue": entry["cue"], "nearCentroid": entry["nearCentroid"],
                  "farCentroid": entry["farCentroid"]} for entry in metrics["cues"]],
    })
    _write_json(evidence / "build-metadata.json", {
        "schemaVersion": 1, "implementationSha": implementation_sha,
        "captureCommand": "JAVA_HOME=<java25> python3 scripts/vfx007_audio_capture.py",
        "minecraft": "26.2", "java": "25", "dedicatedServer": True,
        "realClientProcesses": 2, "result": "PENDING",
        "coverage": {"rows": len(rows), "open": len(open_rows), "wall": len(wall_rows),
                     "burst": len(burst), "subtitles": len(captures)},
    })
    (logs / "client-audit.log").write_text(
        "".join(json.dumps(row, sort_keys=True) + "\n" for row in rows), encoding="utf-8")
    (logs / "server-summary.log").write_text(
        "dedicated server started\nprimary real client connected\n"
        "reconnect real client connected\ndedicated server stopped cleanly\n", encoding="utf-8")
    (evidence / "README.md").write_text(
        "# VFX-007 layered audio evidence\n\n"
        "This package retains production client audit decisions, subtitle screenshots, and "
        "source-file metrics from the exact implementation SHA. No microphone recording is "
        "used as source-faithful proof; decoded asset metrics and the production mixer audit "
        "are the authoritative evidence.\n",
        encoding="utf-8")
    return evidence


def capture(output: Path) -> Path:
    if output.exists():
        raise RuntimeError(f"Refusing to overwrite capture output: {output}")
    (output / "raw").mkdir(parents=True)
    runtime = output / "runtime"
    runtime.mkdir()
    (runtime / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (runtime / "server.properties").write_text(
        f"online-mode=false\nserver-port={PORT}\nlevel-name=world\nmax-players=2\n"
        "view-distance=6\nsimulation-distance=5\ndifficulty=peaceful\n",
        encoding="utf-8")
    primary_script = output / "primary.tsv"
    reconnect_script = output / "reconnect.tsv"
    primary_script.write_text(render_primary_scenario(), encoding="utf-8")
    reconnect_script.write_text(render_reconnect_scenario(), encoding="utf-8")
    implementation_sha = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    if subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip():
        raise RuntimeError("Capture requires a clean exact-SHA worktree")
    inputs = _prepare_launch()
    server_log_path = output / "raw" / "server.log"
    server_log = server_log_path.open("w", encoding="utf-8")
    server = subprocess.Popen(
        ["./gradlew", "runServer", "--no-daemon", "--console=plain",
         f"-PpowersRunDir={runtime}"], cwd=ROOT, env=os.environ.copy(), stdin=subprocess.PIPE,
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
        start_new_session=True)
    lines: queue.Queue[str] = queue.Queue()

    def drain() -> None:
        assert server.stdout is not None
        for line in server.stdout:
            server_log.write(line)
            server_log.flush()
            lines.put(line)

    threading.Thread(target=drain, daemon=True).start()
    clients = []
    try:
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            try:
                if "Done (" in lines.get(timeout=0.2):
                    break
            except queue.Empty:
                pass
        else:
            raise TimeoutError("Dedicated server did not become ready")
        primary = _launch_client(output, inputs, primary_script, "AudioPrimary",
                                 "vfx007-primary", implementation_sha)
        clients.append(primary)
        _wait_log(primary[2], "connected as AudioPrimary", 180)
        assert server.stdin is not None
        server.stdin.write("op AudioPrimary\n")
        server.stdin.flush()
        _wait_log(primary[2], "executed AUDIO_ASSERT [near admitted] at connected tick 727", 240)
        images = _wait_screenshots(primary[3], 17, 60)
        scripted_images = images[1:17]
        _stop_group(primary[0])
        primary[1].close()
        clients.remove(primary)
        reconnect = _launch_client(output, inputs, reconnect_script, "AudioPrimary",
                                   "vfx007-reconnect", implementation_sha)
        clients.append(reconnect)
        _wait_log(reconnect[2], "connected as AudioPrimary", 180)
        server.stdin.write("op AudioPrimary\n")
        server.stdin.flush()
        _wait_log(reconnect[2], "executed AUDIO_ASSERT [near admitted] at connected tick 160", 120)
        _stop_group(reconnect[0])
        reconnect[1].close()
        clients.remove(reconnect)
        server.stdin.write("save-all flush\nstop\n")
        server.stdin.flush()
        server.wait(timeout=60)
        primary_text = primary[2].read_text(encoding="utf-8", errors="replace")
        reconnect_text = reconnect[2].read_text(encoding="utf-8", errors="replace")
        rows = extract_audit_rows(primary_text) + extract_audit_rows(reconnect_text)
        if len(rows) != 77:
            raise RuntimeError(f"Expected 77 production audit rows, found {len(rows)}")
        unexpected = [line for text in (primary_text, reconnect_text)
                      for line in text.splitlines() if "/ERROR]" in line
                      and "Failed to retrieve profile key pair" not in line]
        if unexpected or server.returncode != 0:
            raise RuntimeError(f"Capture runtime errors: {unexpected}")
        return _write_evidence(output, implementation_sha, rows, scripted_images)
    finally:
        for process, handle, _, _ in clients:
            _stop_group(process)
            handle.close()
        _stop_group(server)
        server_log.close()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    options = parser.parse_args()
    evidence = capture(options.output.resolve())
    print(f"VFX-007 production capture complete: {evidence}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
