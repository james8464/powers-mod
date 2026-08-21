#!/usr/bin/env python3

import argparse
import json
import os
import re
import signal
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from types import MappingProxyType
from typing import Mapping

from release_contract import (
    COMMIT_PATTERN,
    CommandReceipt,
    Gate,
    GateCatalogue,
    ReleaseContractError,
    load_catalogue,
    safe_regular_file,
    sha256_file,
    write_json_atomic,
)


DEFAULT_MAX_LOG_BYTES = 64 * 1024 * 1024
RECEIPT_KEYS = frozenset({
    "schemaVersion", "gateId", "commit", "argv", "environment", "startedAt",
    "endedAt", "durationSeconds", "exitCode", "logPath", "logSize",
    "logSha256", "catalogueSha256",
})


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def _git_head(repo_root: Path) -> str:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=repo_root,
            capture_output=True, text=True, check=True, shell=False)
    except (OSError, subprocess.CalledProcessError) as error:
        raise ReleaseContractError(f"cannot resolve repository HEAD: {error}") from error
    head = result.stdout.strip()
    if not COMMIT_PATTERN.fullmatch(head):
        raise ReleaseContractError(f"repository HEAD is not a full commit: {head!r}")
    return head


def _gate(catalogue: GateCatalogue, gate_id: str) -> Gate:
    for value in catalogue.commands:
        if value.id == gate_id:
            return value
    raise ReleaseContractError(f"unknown gate: {gate_id}")


def _ensure_receipt_directory(
        catalogue: GateCatalogue, receipt_dir: Path, repo_root: Path) -> Path:
    repo_root = repo_root.absolute()
    try:
        root_info = repo_root.lstat()
    except OSError as error:
        raise ReleaseContractError(f"unsafe repository root: {error}") from error
    if not repo_root.is_dir() or repo_root.is_symlink():
        raise ReleaseContractError("unsafe repository root")
    expected = repo_root / catalogue.output_root / "receipts"
    if receipt_dir.absolute() != expected:
        raise ReleaseContractError(f"receipt directory must be {expected}")
    current = repo_root
    for part in (*PurePosixPath(catalogue.output_root).parts, "receipts"):
        current /= part
        try:
            info = current.lstat()
        except FileNotFoundError:
            current.mkdir(mode=0o700)
            info = current.lstat()
        except OSError as error:
            raise ReleaseContractError(f"unsafe receipt directory {current}: {error}") from error
        if not current.is_dir() or current.is_symlink():
            raise ReleaseContractError(f"unsafe receipt directory {current}")
    return expected


def _write_process_log(
        gate: Gate, repo_root: Path, environment: Mapping[str, str],
        running_log: Path, max_log_bytes: int) -> tuple[int, bool]:
    if not isinstance(max_log_bytes, int) or isinstance(max_log_bytes, bool) or max_log_bytes <= 0:
        raise ReleaseContractError("max log bytes must be a positive integer")
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
    descriptor = os.open(running_log, flags, 0o600)
    process = None
    oversized = False
    written = 0
    try:
        process = subprocess.Popen(
            list(gate.argv), cwd=repo_root, env=dict(environment), shell=False,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=0)
        assert process.stdout is not None
        while True:
            block = process.stdout.read(64 * 1024)
            if not block:
                break
            remaining = max_log_bytes + 1 - written
            if remaining > 0:
                accepted = block[:remaining]
                view = memoryview(accepted)
                while view:
                    count = os.write(descriptor, view)
                    if count <= 0:
                        raise OSError("short log write")
                    view = view[count:]
                written += len(accepted)
            if written > max_log_bytes or len(block) > remaining:
                oversized = True
                process.terminate()
        return_code = process.wait()
        os.fsync(descriptor)
        return return_code, oversized
    except BaseException:
        if process is not None and process.poll() is None:
            process.kill()
            process.wait()
        raise
    finally:
        if process is not None and process.stdout is not None:
            process.stdout.close()
        os.close(descriptor)


def _captured_environment(
        catalogue: GateCatalogue, environment: Mapping[str, str]) -> dict[str, str]:
    captured: dict[str, str] = {}
    for name in sorted(catalogue.environment_allowlist):
        if name in environment:
            value = environment[name]
            if not isinstance(value, str) or "\x00" in value:
                raise ReleaseContractError(f"invalid environment value: {name}")
            captured[name] = value
    return captured


def _receipt_value(
        gate: Gate, commit: str, captured: dict[str, str], started_at: str,
        ended_at: str, duration: float, log_path: str, log_size: int,
        log_sha256: str, catalogue_sha256: str) -> dict[str, object]:
    return {
        "schemaVersion": 1,
        "gateId": gate.id,
        "commit": commit,
        "argv": list(gate.argv),
        "environment": captured,
        "startedAt": started_at,
        "endedAt": ended_at,
        "durationSeconds": round(duration, 6),
        "exitCode": 0,
        "logPath": log_path,
        "logSize": log_size,
        "logSha256": log_sha256,
        "catalogueSha256": catalogue_sha256,
    }


def run_gate(
        catalogue: GateCatalogue,
        gate_id: str,
        receipt_dir: Path,
        repo_root: Path,
        expected_sha: str,
        environment: Mapping[str, str],
        *,
        catalogue_path: Path,
        max_log_bytes: int = DEFAULT_MAX_LOG_BYTES) -> Path:
    gate = _gate(catalogue, gate_id)
    if not COMMIT_PATTERN.fullmatch(expected_sha):
        raise ReleaseContractError("expected SHA must be 40 lowercase hexadecimal characters")
    head = _git_head(repo_root)
    if head != expected_sha:
        raise ReleaseContractError(f"HEAD does not match expected SHA: {head} != {expected_sha}")
    catalogue_sha256 = sha256_file(catalogue_path)
    captured = _captured_environment(catalogue, environment)
    directory = _ensure_receipt_directory(catalogue, receipt_dir, repo_root)
    running_log = directory / f".{gate.id}.{os.getpid()}.{time.monotonic_ns()}.running"
    accepted_log = directory / f"{gate.id}.log"
    failed_log = directory / f"{gate.id}.failed.log"
    accepted_receipt = directory / f"{gate.id}.json"
    failed_receipt = directory / f"{gate.id}.failed.json"

    started_at = _utc_now()
    started = time.monotonic()
    return_code, oversized = _write_process_log(
        gate, repo_root, environment, running_log, max_log_bytes)
    duration = time.monotonic() - started
    ended_at = _utc_now()
    if oversized or return_code != 0:
        os.replace(running_log, failed_log)
        error = (
            f"log exceeded {max_log_bytes} bytes" if oversized else
            f"terminated by signal {-return_code}" if return_code < 0 else
            f"exit code {return_code}")
        failure = {
            "schemaVersion": 1,
            "accepted": False,
            "gateId": gate.id,
            "commit": head,
            "argv": list(gate.argv),
            "startedAt": started_at,
            "endedAt": ended_at,
            "durationSeconds": round(duration, 6),
            "exitCode": return_code,
            "error": error,
            "logPath": failed_log.relative_to(repo_root).as_posix(),
        }
        write_json_atomic(failed_receipt, failure)
        raise ReleaseContractError(f"gate {gate.id} failed: {error}")

    os.replace(running_log, accepted_log)
    with accepted_log.open("rb") as source:
        os.fsync(source.fileno())
    log_size = accepted_log.stat().st_size
    log_sha256 = sha256_file(accepted_log)
    value = _receipt_value(
        gate, head, captured, started_at, ended_at, duration,
        accepted_log.relative_to(repo_root).as_posix(), log_size, log_sha256,
        catalogue_sha256)
    write_json_atomic(accepted_receipt, value)
    return accepted_receipt


def _read_receipt(path: Path) -> dict[str, object]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ReleaseContractError(f"invalid receipt {path}: {error}") from error
    if not isinstance(value, dict) or set(value) != RECEIPT_KEYS:
        raise ReleaseContractError(f"invalid receipt fields: {path}")
    return value


def verify_receipt(
        receipt_path: Path,
        catalogue: GateCatalogue,
        repo_root: Path,
        expected_sha: str,
        *,
        catalogue_path: Path) -> CommandReceipt:
    value = _read_receipt(receipt_path)
    if value["schemaVersion"] != 1:
        raise ReleaseContractError("receipt schemaVersion mismatch")
    gate_id = value["gateId"]
    if not isinstance(gate_id, str):
        raise ReleaseContractError("receipt gateId is invalid")
    gate = _gate(catalogue, gate_id)
    if value["commit"] != expected_sha:
        raise ReleaseContractError(f"receipt {gate_id} commit mismatch")
    if value["argv"] != list(gate.argv):
        raise ReleaseContractError(f"receipt {gate_id} argv mismatch")
    if value["exitCode"] != 0:
        raise ReleaseContractError(f"receipt {gate_id} exit code mismatch")
    environment = value["environment"]
    if (not isinstance(environment, dict)
            or not all(isinstance(key, str) and isinstance(item, str)
                       for key, item in environment.items())
            or not set(environment) <= set(catalogue.environment_allowlist)):
        raise ReleaseContractError(f"receipt {gate_id} environment mismatch")
    catalogue_digest = sha256_file(catalogue_path)
    if value["catalogueSha256"] != catalogue_digest:
        raise ReleaseContractError(f"receipt {gate_id} catalogue SHA-256 mismatch")
    log_path = value["logPath"]
    if not isinstance(log_path, str):
        raise ReleaseContractError(f"receipt {gate_id} log path mismatch")
    log = safe_regular_file(repo_root, log_path)
    if not isinstance(value["logSize"], int) or isinstance(value["logSize"], bool):
        raise ReleaseContractError(f"receipt {gate_id} log size mismatch")
    if log.stat().st_size != value["logSize"]:
        raise ReleaseContractError(f"receipt {gate_id} log size mismatch")
    if sha256_file(log) != value["logSha256"]:
        raise ReleaseContractError(f"receipt {gate_id} log SHA-256 mismatch")
    duration = value["durationSeconds"]
    if not isinstance(duration, (int, float)) or isinstance(duration, bool) or duration < 0:
        raise ReleaseContractError(f"receipt {gate_id} duration mismatch")
    for field in ("startedAt", "endedAt"):
        if not isinstance(value[field], str) or not value[field].endswith("Z"):
            raise ReleaseContractError(f"receipt {gate_id} {field} mismatch")
    return CommandReceipt(
        gate_id,
        expected_sha,
        gate.argv,
        MappingProxyType(dict(sorted(environment.items()))),
        value["startedAt"],
        value["endedAt"],
        float(duration),
        0,
        log_path,
        value["logSize"],
        value["logSha256"],
        catalogue_digest,
    )


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Run one declared QA-001 release gate.")
    parser.add_argument("--catalogue", type=Path, required=True)
    parser.add_argument("--gate", required=True)
    parser.add_argument("--receipt-dir", type=Path, required=True)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--expected-sha", required=True)
    parser.add_argument("--max-log-bytes", type=int, default=DEFAULT_MAX_LOG_BYTES)
    parser.add_argument("--dry-run", action="store_true")
    return parser


def main(arguments: list[str] | None = None) -> int:
    options = _parser().parse_args(arguments)
    try:
        catalogue = load_catalogue(options.catalogue)
        gate = _gate(catalogue, options.gate)
        if not COMMIT_PATTERN.fullmatch(options.expected_sha):
            raise ReleaseContractError("expected SHA must be 40 lowercase hexadecimal characters")
        head = _git_head(options.repo_root)
        if head != options.expected_sha:
            raise ReleaseContractError(
                f"HEAD does not match expected SHA: {head} != {options.expected_sha}")
        if options.dry_run:
            print(json.dumps(list(gate.argv), separators=(",", ":")))
            return 0
        run_gate(
            catalogue, gate.id, options.receipt_dir, options.repo_root,
            options.expected_sha, os.environ, catalogue_path=options.catalogue,
            max_log_bytes=options.max_log_bytes)
        return 0
    except (OSError, ReleaseContractError) as error:
        print(f"release_gate: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
