#!/usr/bin/env python3
"""Capture the VFX-011 locator and hidden Darkness root with two real clients."""

from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import queue
import signal
import subprocess
import threading
import time


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "build" / "vfx-011-two-client"
PORT = 25_571
JAVA = Path(os.environ["JAVA_HOME"]) / "bin" / "java"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stop_group(process: subprocess.Popen, timeout: int = 20) -> None:
    if process.poll() is not None:
        return
    os.killpg(process.pid, signal.SIGTERM)
    try:
        process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait(timeout=timeout)


def client_command(game_dir: Path, username: str, role: str, script: Path | None) -> list[str]:
    command = [
        str(JAVA), "-Xms256m", "-Xmx1g", f"-Dpowers.qa.role={role}",
        f"-Dpowers.qa.server=127.0.0.1:{PORT}",
    ]
    if script is not None:
        command.append(f"-Dpowers.qa.script={script}")
    command.extend([
        f"-Dfabric.dli.config={ROOT / '.gradle/loom-cache/launch.cfg'}",
        "-Dfabric.dli.env=client",
        "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient",
        f"@{ROOT / 'build/loom-cache/argFiles/runClient'}", "-XstartOnFirstThread",
        "--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED",
        "net.fabricmc.devlaunchinjector.Main", "--username", username,
        "--gameDir", str(game_dir), "--width", "1280", "--height", "720",
    ])
    return command


def launch_client(username: str, role: str, script: Path | None, suffix: str):
    game_dir = OUTPUT / "clients" / f"{username}-{suffix}"
    game_dir.mkdir(parents=True, exist_ok=True)
    (game_dir / "options.txt").write_text(
        "guiScale:2\nmaxFps:30\nrenderDistance:4\nsimulationDistance:5\n"
        "particles:0\ngraphicsPreset:\"custom\"\ntutorialStep:none\nchatVisibility:2\n",
        encoding="utf-8")
    log_path = OUTPUT / "logs" / f"{username}-{suffix}.log"
    handle = log_path.open("wb")
    process = subprocess.Popen(client_command(game_dir, username, role, script), cwd=game_dir,
                               stdout=handle, stderr=subprocess.STDOUT, start_new_session=True)
    return process, handle, log_path, game_dir


def wait_log(path: Path, marker: str, timeout: float) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if path.is_file() and marker in path.read_text(encoding="utf-8", errors="replace"):
            return
        time.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for {marker!r} in {path}")


def wait_log_count(path: Path, marker: str, count: int, timeout: float) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if path.is_file() and path.read_text(encoding="utf-8", errors="replace").count(marker) >= count:
            return
        time.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for {count} occurrences of {marker!r} in {path}")


def wait_screenshots(game_dir: Path, count: int, timeout: float) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        images = sorted((game_dir / "screenshots").glob("*.png"))
        if len(images) >= count and all(path.stat().st_size > 0 for path in images):
            return
        time.sleep(0.1)
    raise TimeoutError(f"Timed out waiting for {count} complete screenshots in {game_dir}")


def copy_screenshots(game_dir: Path, prefix: str) -> list[dict[str, str]]:
    rows = []
    for index, source in enumerate(sorted((game_dir / "screenshots").glob("*.png"))):
        target = OUTPUT / "screenshots" / f"{prefix}-{index:02d}.png"
        target.write_bytes(source.read_bytes())
        rows.append({"file": target.name, "sha256": sha(target)})
    return rows


def main() -> int:
    if OUTPUT.exists():
        raise RuntimeError(f"Refusing to overwrite existing evidence: {OUTPUT}")
    for path in (OUTPUT / "logs", OUTPUT / "screenshots", OUTPUT / "runtime"):
        path.mkdir(parents=True)
    runtime = OUTPUT / "runtime"
    (runtime / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (runtime / "server.properties").write_text(
        f"online-mode=false\nserver-port={PORT}\nlevel-name=world\nmax-players=4\n"
        "view-distance=4\nsimulation-distance=4\ndifficulty=peaceful\n",
        encoding="utf-8")
    locator_script = OUTPUT / "locator.tsv"
    locator_script.write_text(
        "160\tcommand\tpowers testing vfx locator-entity\n"
        "190\tclean\tui\n"
        "200\tscreenshot\tlocator_entity_two_clients\n"
        "220\tclose\tscreen\n"
        "240\tcommand\tpowers testing vfx advancement-dark\n", encoding="utf-8")
    dark_script = OUTPUT / "darkness.tsv"
    dark_script.write_text(
        "100\tkey\tadvancements on\n101\tkey\tadvancements off\n"
        "140\tclean\tui\n"
        "150\tscreenshot\tdarkness_advancement_root\n", encoding="utf-8")

    subprocess.run(["./gradlew", "classes", "clientClasses", "configureClientLaunch",
                    "--no-daemon", "--console=plain"], cwd=ROOT, check=True)
    server_log_path = OUTPUT / "logs" / "server.log"
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
    screenshots: list[dict[str, str]] = []
    try:
        deadline = time.monotonic() + 180
        ready = False
        while time.monotonic() < deadline:
            try:
                if "Done (" in lines.get(timeout=0.2):
                    ready = True
                    break
            except queue.Empty:
                pass
        if not ready:
            raise TimeoutError("Dedicated server did not become ready")
        observer = launch_client("VfxObserver", "vfx-observer", None, "idle")
        primary = launch_client("VfxPrimary", "vfx-primary", locator_script, "locator")
        clients.extend((observer, primary))
        wait_log(observer[2], "connected as VfxObserver", 120)
        wait_log(primary[2], "connected as VfxPrimary", 120)
        assert server.stdin is not None
        server.stdin.write("op VfxPrimary\n")
        server.stdin.flush()
        wait_log(primary[2], "executed SCREENSHOT [locator_entity_two_clients]", 120)
        wait_screenshots(primary[3], 2, 30)
        wait_log(primary[2], "executed COMMAND [powers testing vfx advancement-dark]", 60)
        stop_group(primary[0])
        primary[1].close()
        clients.remove(primary)
        wait_log_count(server_log_path, "VfxPrimary left the game", 1, 30)
        screenshots.append(copy_screenshots(primary[3], "locator")[-1])

        dark = launch_client("VfxPrimary", "vfx-primary-dark", dark_script, "darkness")
        clients.append(dark)
        wait_log(dark[2], "executed SCREENSHOT [darkness_advancement_root]", 120)
        wait_screenshots(dark[3], 2, 30)
        screenshots.append(copy_screenshots(dark[3], "darkness")[-1])
        for client in (dark, observer):
            stop_group(client[0])
            client[1].close()
            clients.remove(client)
        wait_log_count(server_log_path, "VfxPrimary left the game", 2, 30)
        wait_log(server_log_path, "VfxObserver left the game", 30)
        server.stdin.write("save-all flush\nstop\n")
        server.stdin.flush()
        server.wait(timeout=60)
    finally:
        for process, handle, _, _ in clients:
            stop_group(process)
            handle.close()
        stop_group(server)
        server_log.close()

    expected = screenshots
    logs = sorted((OUTPUT / "logs").glob("*.log"))
    log_text = {path.name: path.read_text(encoding="utf-8", errors="replace") for path in logs}
    expected_client_error = "Failed to retrieve profile key pair"
    unexpected_errors = [line for name, text in log_text.items() for line in text.splitlines()
                         if "/ERROR]" in line and expected_client_error not in line]
    server_text = log_text["server.log"]
    locator_text = log_text["VfxPrimary-locator.log"]
    darkness_text = log_text["VfxPrimary-darkness.log"]
    markers = {
        "observerJoined": "VfxObserver joined the game" in server_text,
        "primaryJoinedTwice": server_text.count("VfxPrimary joined the game") >= 2,
        "primaryLeftTwice": server_text.count("VfxPrimary left the game") >= 2,
        "locatorCommand": "executed COMMAND [powers testing vfx locator-entity]" in locator_text,
        "locatorState": "QA VFX proof locator visiblePlayers=[VfxObserver]" in locator_text,
        "darkCommand": "executed COMMAND [powers testing vfx advancement-dark]" in locator_text,
        "darkState": "QA VFX proof selectedRoot=powers:darkness_root opposingRootLoaded=false"
                     in darkness_text,
    }
    options = sorted((OUTPUT / "clients").glob("*/options.txt"))
    required_options = ("guiScale:2", "simulationDistance:5", 'graphicsPreset:"custom"',
                        "tutorialStep:none", "chatVisibility:2")
    options_exact = all(all(value in path.read_text(encoding="utf-8")
                            for value in required_options) for path in options)
    java_line = subprocess.check_output([str(JAVA), "-version"], stderr=subprocess.STDOUT,
                                        text=True).splitlines()[0]
    receipt = {
        "schema": 1,
        "commit": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT,
                                          text=True).strip(),
        "java": java_line,
        "minecraft": "26.2", "server": "dedicated/offline", "realClients": 2,
        "screenshots": expected,
        "logs": [{"file": path.name, "sha256": sha(path)} for path in logs],
        "options": [{"file": str(path.relative_to(OUTPUT)), "sha256": sha(path)}
                    for path in options],
        "markers": markers,
        "unexpectedErrorLines": unexpected_errors,
        "passed": server.returncode == 0 and len(expected) == 2
                  and all(row["sha256"] != hashlib.sha256(b"").hexdigest() for row in expected)
                  and java_line.startswith('openjdk version "25.') and options_exact
                  and all(markers.values()) and not unexpected_errors,
    }
    (OUTPUT / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, indent=2))
    return 0 if receipt["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
