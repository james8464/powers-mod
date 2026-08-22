#!/usr/bin/env python3
"""Validate and retain one complete QA-006 restart-soak evidence bundle."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
import math
import re
import tarfile
from pathlib import Path

from release_contract import (
    ReleaseContractError,
    read_regular_snapshot,
    recheck_regular_snapshot,
    validate_packaged_text,
    write_bytes_atomic,
)


EXPECTED_CYCLES = 288
MINIMUM_SECONDS = 86_400.0
MAX_REPORT_BYTES = 16 * 1024 * 1024
MAX_LOG_BYTES = 8 * 1024 * 1024
OWNED_OUTPUTS = frozenset({
    "README.md",
    "SHA256SUMS",
    "logs-index.json",
    "restart-soak-logs.tar.gz",
    "restart-soak-report.json",
    "validation-summary.json",
})
REQUIRED_PREDICATES = (
    "ready",
    "client_connected",
    "client_disconnected",
    "startup_verified",
    "seeded",
    "settled",
    "status_verified",
    "rollover_seeded",
    "clean_diagnostics",
)
DIAGNOSTIC_MARKERS = (
    "forcedChunks=0",
    "proxies=0",
    "travelLoads=0",
    "celestialEvents=1",
)
_HOME_PATH = re.compile(r"(?:file:)?/(?:Users|home)/[^\s\])]+")
_LOOPBACK = re.compile(r"/?(?:127\.0\.0\.1|localhost)(?::\d+|,\s*\d+)?")
REPORT_FIELDS = frozenset({
    "schema", "git_commit", "requested_hours", "cycle_seconds",
    "requested_cycles", "completed_cycles", "connected_workload_seconds",
    "elapsed_seconds", "acceptance_window_started_epoch", "cycles", "status",
    "passed", "failure", "runtime",
})
CYCLE_FIELDS = frozenset({
    "cycle", "seconds", "connected_workload_seconds", "ready", "client_connected",
    "client_disconnected", "startup_verified", "seeded", "settled",
    "status_verified", "rollover_seeded", "clean_diagnostics",
    "client_ability_actions", "shutdown_mode", "exit_code", "error_lines", "passed",
    "server_log", "client_log",
})


def canonical_json(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False,
                       allow_nan=False) + "\n").encode("utf-8")


def sanitize_log(text: str) -> str:
    sanitized = _HOME_PATH.sub("<HOME>", text)
    sanitized = re.sub(
        r"(?:file:)?/(?:private|var/folders|tmp)/[^\s\])]+", "<LOCAL_PATH>", sanitized)
    sanitized = _LOOPBACK.sub("<LOOPBACK>", sanitized)
    sanitized = re.sub(r"[ \t]+(?=\r?$)", "", sanitized, flags=re.MULTILINE)
    validate_packaged_text(sanitized)
    return sanitized


def _plain_number(value: object) -> bool:
    return (isinstance(value, (int, float)) and not isinstance(value, bool)
            and math.isfinite(float(value)))


def _report(source: Path):
    try:
        snapshot = read_regular_snapshot(
            source, "restart-soak-report.json", maximum_bytes=MAX_REPORT_BYTES)
        data = json.loads(snapshot.content)
    except (ReleaseContractError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ValueError(f"unsafe or invalid restart-soak report: {error}") from error
    if not isinstance(data, dict):
        raise ValueError("restart-soak report must be an object")
    unknown = set(data) - REPORT_FIELDS
    missing = REPORT_FIELDS - set(data)
    if unknown:
        raise ValueError(f"unknown report field: {sorted(unknown)[0]}")
    if missing:
        raise ValueError(f"missing report field: {sorted(missing)[0]}")
    try:
        validate_packaged_text(snapshot.content.decode("utf-8"))
    except (UnicodeDecodeError, ReleaseContractError) as error:
        raise ValueError(f"restart-soak report privacy validation failed: {error}") from error
    if data.get("cycle_seconds") != 300:
        raise ValueError("QA-006 requires the exact 300-second cadence")
    exact = (
        data.get("schema") == 3
        and data.get("status") == "passed"
        and data.get("passed") is True
        and data.get("failure") == ""
        and data.get("requested_hours") == 24.0
        and data.get("requested_cycles") == EXPECTED_CYCLES
        and data.get("completed_cycles") == EXPECTED_CYCLES
        and _plain_number(data.get("elapsed_seconds"))
        and float(data["elapsed_seconds"]) >= MINIMUM_SECONDS
    )
    if not exact:
        raise ValueError("QA-006 requires exactly 288 completed cycles and 24 accepted hours")
    commit = data.get("git_commit")
    if not isinstance(commit, str) or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("restart-soak report has no exact implementation commit")
    cycles = data.get("cycles")
    if not isinstance(cycles, list) or len(cycles) != EXPECTED_CYCLES:
        raise ValueError("QA-006 requires exactly 288 completed cycles")
    return snapshot, data, cycles


def _relative_log(row: dict[str, object], key: str, expected: str) -> str:
    value = row.get(key)
    prefix = "build/restart-soak/"
    if not isinstance(value, str) or not value.startswith(prefix):
        raise ValueError(f"cycle log path is not owned: {value!r}")
    relative = value.removeprefix(prefix)
    if relative != expected:
        raise ValueError(f"cycle log path mismatch: {relative} != {expected}")
    return relative


def _validate_cycle(row: object, cycle: int) -> tuple[str, str]:
    if not isinstance(row, dict) or row.get("cycle") != cycle or row.get("passed") is not True:
        raise ValueError(f"cycle {cycle}: identity/pass contract failed")
    unknown = set(row) - CYCLE_FIELDS
    missing = CYCLE_FIELDS - set(row)
    if unknown:
        raise ValueError(f"cycle {cycle}: unknown field {sorted(unknown)[0]}")
    if missing:
        raise ValueError(f"cycle {cycle}: missing field {sorted(missing)[0]}")
    if (not _plain_number(row.get("seconds"))
            or float(row["seconds"]) < 300.0):
        raise ValueError(f"cycle {cycle}: full 300-second boundary is missing")
    if (not _plain_number(row.get("connected_workload_seconds"))
            or float(row["connected_workload_seconds"]) < 270.0
            or float(row["connected_workload_seconds"]) > float(row["seconds"])):
        raise ValueError(f"cycle {cycle}: 270-second connected workload contract failed")
    for predicate in REQUIRED_PREDICATES:
        if row.get(predicate) is not True:
            raise ValueError(f"cycle {cycle}: {predicate} must be true")
    actions = row.get("client_ability_actions")
    if not isinstance(actions, int) or isinstance(actions, bool) or actions <= 0:
        raise ValueError(f"cycle {cycle}: client ability actions must be positive")
    if row.get("error_lines") != []:
        raise ValueError(f"cycle {cycle}: error lines are not empty")
    signal_boundary = cycle % 12 == 0
    expected_mode = "sigterm" if signal_boundary else "clean"
    expected_exit = 143 if signal_boundary else 0
    if row.get("shutdown_mode") != expected_mode or row.get("exit_code") != expected_exit:
        raise ValueError(f"cycle {cycle}: shutdown boundary mismatch")
    server = _relative_log(row, "server_log", f"server-logs/cycle-{cycle:04d}.log")
    client = _relative_log(row, "client_log", f"client-logs/cycle-{cycle:04d}.log")
    return server, client


def _read_log(source: Path, relative: str):
    try:
        return read_regular_snapshot(source, relative, maximum_bytes=MAX_LOG_BYTES)
    except ReleaseContractError as error:
        raise ValueError(f"unsafe soak log {relative}: {error}") from error


def _ordered_server_contract(text: str, cycle: int, mode: str) -> None:
    lines = text.splitlines()
    markers = (
        ("server boot marker", lambda line: "Done (" in line),
        ("client join marker", lambda line: "SoakClient joined the game" in line),
        ("startup verification", lambda line: (
            f"POWERS_SOAK_VERIFY cycle={cycle} passed=true" in line)),
        ("seed phase", lambda line: (
            f"POWERS_SOAK_SEED cycle={cycle} passed=true" in line)),
        ("settled phase", lambda line: (
            f"POWERS_SOAK_SETTLED cycle={cycle} passed=true" in line)),
        ("pre-rollover status", lambda line: (
            f"POWERS_SOAK_STATUS cycle={cycle} passed=true" in line
            and "rollover=false" in line)),
        ("rollover phase", lambda line: (
            f"POWERS_SOAK_ROLLOVER cycle={cycle} passed=true" in line)),
        ("post-rollover status", lambda line: (
            f"POWERS_SOAK_STATUS cycle={cycle} passed=true" in line
            and "rollover=true" in line)),
        ("coherent final diagnostics", lambda line: all(
            marker in line for marker in DIAGNOSTIC_MARKERS)),
        ("flushed save", lambda line: "Saved the game" in line),
        ("client disconnect", lambda line: "SoakClient left the game" in line),
    )
    cursor = 0
    for label, predicate in markers:
        for index in range(cursor, len(lines)):
            if predicate(lines[index]):
                cursor = index + 1
                break
        else:
            if label == "coherent final diagnostics":
                raise ValueError(f"cycle {cycle}: coherent final diagnostics missing")
            raise ValueError(f"cycle {cycle}: ordered lifecycle missing {label}")
    if mode == "clean" and not any("BUILD SUCCESSFUL" in line for line in lines[cursor:]):
        raise ValueError(f"cycle {cycle}: clean shutdown build footer missing")


def _validated_logs(source: Path, cycles: list[object]):
    snapshots = []
    entries = []
    archive_rows = []
    for cycle, raw_row in enumerate(cycles, 1):
        row = raw_row if isinstance(raw_row, dict) else {}
        server_relative, client_relative = _validate_cycle(raw_row, cycle)
        for kind, relative in (("server", server_relative), ("client", client_relative)):
            snapshot = _read_log(source, relative)
            try:
                raw_text = snapshot.content.decode("utf-8")
            except UnicodeDecodeError as error:
                raise ValueError(f"cycle {cycle}: {kind} log is not UTF-8") from error
            if kind == "server":
                _ordered_server_contract(raw_text, cycle, str(row["shutdown_mode"]))
                if ("[Server thread/ERROR]" in raw_text
                        or "[ServerMain/ERROR]" in raw_text):
                    raise ValueError(f"cycle {cycle}: server error marker present")
            else:
                if raw_text.count("executed ACTIVATE") != row["client_ability_actions"]:
                    raise ValueError(f"cycle {cycle}: client action count mismatch")
            if "passed=false" in raw_text or "BUILD FAILED" in raw_text:
                raise ValueError(f"cycle {cycle}: negative marker in {kind} log")
            sanitized = sanitize_log(raw_text).encode("utf-8")
            member = f"{kind}/cycle-{cycle:04d}.log"
            entries.append({
                "cycle": cycle,
                "kind": kind,
                "member": member,
                "rawSha256": snapshot.sha256,
                "rawSize": snapshot.size,
                "sanitizedSha256": hashlib.sha256(sanitized).hexdigest(),
                "sanitizedSize": len(sanitized),
            })
            snapshots.append(snapshot)
            archive_rows.append((member, sanitized))
    return snapshots, entries, archive_rows


def _archive(rows: list[tuple[str, bytes]]) -> bytes:
    stream = io.BytesIO()
    with tarfile.open(fileobj=stream, mode="w", format=tarfile.PAX_FORMAT) as bundle:
        for name, content in rows:
            info = tarfile.TarInfo(name)
            info.size = len(content)
            info.mode = 0o644
            info.mtime = 0
            info.uid = 0
            info.gid = 0
            info.uname = ""
            info.gname = ""
            bundle.addfile(info, io.BytesIO(content))
    return gzip.compress(stream.getvalue(), compresslevel=9, mtime=0)


def _check_output(destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    unexpected = sorted(path.name for path in destination.iterdir()
                        if path.name not in OWNED_OUTPUTS)
    if unexpected:
        raise ValueError(f"unexpected output file: {unexpected[0]}")
    for path in destination.iterdir():
        if path.is_symlink() or not path.is_file():
            raise ValueError(f"unsafe output path: {path.name}")


def _verify_published_outputs(destination: Path, outputs: dict[str, bytes]) -> None:
    _check_output(destination)
    if {path.name for path in destination.iterdir()} != OWNED_OUTPUTS:
        raise ValueError("published evidence inventory is incomplete")
    snapshots = []
    for name, expected in sorted(outputs.items()):
        try:
            snapshot = read_regular_snapshot(
                destination, name, maximum_bytes=max(len(expected), 1))
        except ReleaseContractError as error:
            raise ValueError(f"published evidence changed: {name}: {error}") from error
        if snapshot.content != expected:
            raise ValueError(f"published evidence changed: {name}")
        snapshots.append(snapshot)
    _check_output(destination)
    for snapshot in snapshots:
        try:
            recheck_regular_snapshot(snapshot)
        except ReleaseContractError as error:
            raise ValueError(
                f"published evidence changed: {snapshot.relative}: {error}") from error


def package_run(source: Path, destination: Path) -> dict[str, object]:
    source = source.absolute()
    destination = destination.absolute()
    report_snapshot, report, cycles = _report(source)
    log_snapshots, entries, archive_rows = _validated_logs(source, cycles)
    connected_total = round(sum(float(row["connected_workload_seconds"])
                                for row in cycles), 3)
    if (not _plain_number(report.get("connected_workload_seconds"))
            or float(report["connected_workload_seconds"]) != connected_total):
        raise ValueError("restart-soak connected workload total is inconsistent")
    archive = _archive(archive_rows)
    archive_sha = hashlib.sha256(archive).hexdigest()
    index = {
        "schema": 1,
        "implementationCommit": report["git_commit"],
        "serverLogs": EXPECTED_CYCLES,
        "clientLogs": EXPECTED_CYCLES,
        "logs": entries,
    }
    sigterm_boundaries = sum(row.get("shutdown_mode") == "sigterm" for row in cycles)
    total_actions = sum(int(row["client_ability_actions"]) for row in cycles)
    summary = {
        "schema": 1,
        "status": "passed",
        "implementationCommit": report["git_commit"],
        "requestedHours": report["requested_hours"],
        "completedCycles": report["completed_cycles"],
        "elapsedSeconds": report["elapsed_seconds"],
        "connectedWorkloadSeconds": report["connected_workload_seconds"],
        "sigtermBoundaries": sigterm_boundaries,
        "clientAbilityActions": total_actions,
        "logCount": len(entries),
        "archiveSha256": archive_sha,
        "privacy": "absolute home paths and loopback endpoints replaced in retained logs",
        "releaseLimitation": "the final release commit still requires its own complete 24-hour rerun",
    }
    readme = f"""# QA-006 restart/reconnect soak evidence

This bundle retains the accepted schema-3 report and all {len(entries)} server/client logs for the
24-hour, {EXPECTED_CYCLES}-cycle restart soak at implementation commit
`{report['git_commit']}`. Every cycle proves readiness, connected client work, observed disconnect,
startup/seed/settle/status/rollover phases, clean owner diagnostics, and the expected clean or hourly
SIGTERM boundary. The run recorded {total_actions} client ability activations and
{sigterm_boundaries} flushed SIGTERM boundaries.

`restart-soak-logs.tar.gz` is deterministic and contains privacy-sanitized UTF-8 logs. The exact raw
and retained hashes/sizes are recorded per member in `logs-index.json`; private home paths and dynamic
loopback endpoints are replaced without removing diagnostic lines. `SHA256SUMS` covers every other
file in this directory.

This evidence closes the QA-006 work unit only. The selected programme's final acceptance still
requires a new complete 24-hour soak on the exact final release commit.
""".encode("utf-8")
    outputs = {
        "README.md": readme,
        "logs-index.json": canonical_json(index),
        "restart-soak-logs.tar.gz": archive,
        "restart-soak-report.json": report_snapshot.content,
        "validation-summary.json": canonical_json(summary),
    }
    for snapshot in (report_snapshot, *log_snapshots):
        try:
            recheck_regular_snapshot(snapshot)
        except ReleaseContractError as error:
            raise ValueError(f"source changed during packaging: {snapshot.relative}") from error
    _check_output(destination)
    checksums = "".join(
        f"{hashlib.sha256(content).hexdigest()}  {name}\n"
        for name, content in sorted(outputs.items()))
    outputs["SHA256SUMS"] = checksums.encode("utf-8")
    for name, content in sorted(outputs.items()):
        if name == "SHA256SUMS":
            continue
        try:
            write_bytes_atomic(destination / name, content)
        except ReleaseContractError as error:
            raise ValueError(f"could not write owned evidence {name}: {error}") from error
    try:
        write_bytes_atomic(destination / "SHA256SUMS", outputs["SHA256SUMS"])
    except ReleaseContractError as error:
        raise ValueError(f"could not write owned evidence SHA256SUMS: {error}") from error
    _verify_published_outputs(destination, outputs)
    return summary


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    options = parser.parse_args()
    summary = package_run(options.source, options.output)
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    main()
