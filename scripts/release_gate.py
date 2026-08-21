#!/usr/bin/env python3

import argparse
import hashlib
import json
import os
import re
import secrets
import signal
import stat
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
    read_regular_snapshot,
    validate_packaged_text,
)


DEFAULT_MAX_LOG_BYTES = 64 * 1024 * 1024
CATALOGUE_RELATIVE = "config/release/qa-001-gates.json"
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


def _open_receipt_directory(
        catalogue: GateCatalogue, receipt_dir: Path, repo_root: Path,
        *, create: bool) -> tuple[Path, int]:
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
    descriptor = os.open(
        repo_root, os.O_RDONLY | os.O_DIRECTORY | getattr(os, "O_NOFOLLOW", 0))
    current = descriptor
    try:
        for part in (*PurePosixPath(catalogue.output_root).parts, "receipts"):
            try:
                child = os.open(
                    part,
                    os.O_RDONLY | os.O_DIRECTORY | getattr(os, "O_NOFOLLOW", 0),
                    dir_fd=current)
            except FileNotFoundError:
                if not create:
                    raise ReleaseContractError(
                        f"unsafe receipt directory {expected}: missing") from None
                try:
                    os.mkdir(part, mode=0o700, dir_fd=current)
                    child = os.open(
                        part,
                        os.O_RDONLY | os.O_DIRECTORY
                        | getattr(os, "O_NOFOLLOW", 0), dir_fd=current)
                except OSError as error:
                    raise ReleaseContractError(
                        f"unsafe receipt directory {expected}: {error}") from error
            except OSError as error:
                raise ReleaseContractError(
                    f"unsafe receipt directory {expected}: {error}") from error
            if current != descriptor:
                os.close(current)
            current = child
        if current == descriptor:
            raise ReleaseContractError("receipt directory cannot equal repository root")
        os.close(descriptor)
        return expected, current
    except BaseException:
        if current != descriptor:
            os.close(current)
        os.close(descriptor)
        raise


def _assert_receipt_directory_unchanged(
        catalogue: GateCatalogue, receipt_dir: Path, repo_root: Path,
        held_descriptor: int) -> None:
    try:
        _, current = _open_receipt_directory(
            catalogue, receipt_dir, repo_root, create=False)
    except ReleaseContractError as error:
        raise ReleaseContractError("receipt directory changed during gate") from error
    try:
        held = os.fstat(held_descriptor)
        reopened = os.fstat(current)
        if (held.st_dev, held.st_ino) != (reopened.st_dev, reopened.st_ino):
            raise ReleaseContractError("receipt directory changed during gate")
    finally:
        os.close(current)


def _path_exists_at(directory: int, name: str) -> bool:
    try:
        os.stat(name, dir_fd=directory, follow_symlinks=False)
        return True
    except FileNotFoundError:
        return False


def _snapshot_at(
        directory: int, name: str, maximum_bytes: int
) -> tuple[bytes, int, str, int, int, int]:
    descriptor = os.open(
        name, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0), dir_fd=directory)
    try:
        before = os.fstat(descriptor)
        if (not stat.S_ISREG(before.st_mode) or before.st_nlink != 1
                or before.st_size > maximum_bytes):
            raise ReleaseContractError(f"unsafe gate output: {name}")
        blocks: list[bytes] = []
        digest = hashlib.sha256()
        total = 0
        while block := os.read(descriptor, min(1024 * 1024, maximum_bytes + 1 - total)):
            total += len(block)
            if total > maximum_bytes:
                raise ReleaseContractError(f"gate output exceeds {maximum_bytes} bytes")
            blocks.append(block)
            digest.update(block)
        after = os.fstat(descriptor)
        current = os.stat(name, dir_fd=directory, follow_symlinks=False)
        identity = (before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns)
        if (identity != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns)
                or (after.st_dev, after.st_ino) != (current.st_dev, current.st_ino)
                or total != after.st_size):
            raise ReleaseContractError(f"gate output changed while reading: {name}")
        return (
            b"".join(blocks), total, digest.hexdigest(), after.st_dev,
            after.st_ino, after.st_mtime_ns)
    finally:
        os.close(descriptor)


def _publish_noreplace(directory: int, source: str, target: str) -> None:
    try:
        os.link(
            source, target, src_dir_fd=directory, dst_dir_fd=directory,
            follow_symlinks=False)
    except FileExistsError as error:
        raise ReleaseContractError(
            f"accepted gate output already exists: {target}") from error
    os.unlink(source, dir_fd=directory)
    os.fsync(directory)


def _write_json_exclusive_at(directory: int, name: str, value: object) -> None:
    try:
        data = (json.dumps(
            value, ensure_ascii=False, allow_nan=False, sort_keys=True,
            separators=(",", ":")) + "\n").encode("utf-8")
    except (TypeError, ValueError) as error:
        raise ReleaseContractError(f"cannot encode canonical receipt: {error}") from error
    temporary = f".{name}.{secrets.token_hex(12)}.tmp"
    descriptor = -1
    try:
        descriptor = os.open(
            temporary,
            os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0),
            0o600, dir_fd=directory)
        view = memoryview(data)
        while view:
            written = os.write(descriptor, view)
            if written <= 0:
                raise OSError("short receipt write")
            view = view[written:]
        os.fsync(descriptor)
        os.close(descriptor)
        descriptor = -1
        _publish_noreplace(directory, temporary, name)
    finally:
        if descriptor >= 0:
            os.close(descriptor)
        try:
            os.unlink(temporary, dir_fd=directory)
        except FileNotFoundError:
            pass


_CHILD_ENVIRONMENT = frozenset({
    "CI", "DISPLAY", "GITHUB_ACTIONS", "GITHUB_RUN_ATTEMPT", "GITHUB_RUN_ID",
    "GITHUB_SHA", "GRADLE_USER_HOME", "HOME", "JAVA_HOME", "JAVA_VERSION",
    "LANG", "LC_ALL", "PATH", "POWERS_TEST_RUN_ID", "RUNNER_TEMP", "TMPDIR",
})


def _child_environment(environment: Mapping[str, str]) -> dict[str, str]:
    child: dict[str, str] = {}
    for name in sorted(_CHILD_ENVIRONMENT):
        value = environment.get(name)
        if value is not None:
            if not isinstance(value, str) or "\x00" in value:
                raise ReleaseContractError(f"invalid child environment value: {name}")
            child[name] = value
    if "PATH" not in child:
        raise ReleaseContractError("child environment requires PATH")
    return child


def _write_process_log(
        gate: Gate, repo_root: Path, environment: Mapping[str, str],
        directory: int, running_name: str,
        max_log_bytes: int) -> tuple[int, bool, int, str, int, int]:
    if not isinstance(max_log_bytes, int) or isinstance(max_log_bytes, bool) or max_log_bytes <= 0:
        raise ReleaseContractError("max log bytes must be a positive integer")
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
    descriptor = os.open(running_name, flags, 0o600, dir_fd=directory)
    process = None
    oversized = False
    written = 0
    digest = hashlib.sha256()
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
                digest.update(accepted)
            if written > max_log_bytes or len(block) > remaining:
                oversized = True
                process.terminate()
        return_code = process.wait()
        os.fsync(descriptor)
        metadata = os.fstat(descriptor)
        return (
            return_code, oversized, written, digest.hexdigest(),
            metadata.st_dev, metadata.st_ino)
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
    expected_catalogue = (repo_root / CATALOGUE_RELATIVE).absolute()
    if catalogue_path.absolute() != expected_catalogue:
        raise ReleaseContractError(f"catalogue path must be {expected_catalogue}")
    catalogue_snapshot = read_regular_snapshot(
        repo_root, CATALOGUE_RELATIVE, maximum_bytes=16 * 1024 * 1024)
    if load_catalogue(
            catalogue_path, content=catalogue_snapshot.content) != catalogue:
        raise ReleaseContractError("catalogue object does not match committed bytes")
    catalogue_sha256 = catalogue_snapshot.sha256
    captured = _captured_environment(catalogue, environment)
    validate_packaged_text(json.dumps(captured, sort_keys=True))
    child_environment = _child_environment(environment)
    directory_path, directory = _open_receipt_directory(
        catalogue, receipt_dir, repo_root, create=True)
    running_name = f".{gate.id}.{os.getpid()}.{time.monotonic_ns()}.running"
    lock_name = f".{gate.id}.lock"
    accepted_log_name = f"{gate.id}.log"
    failed_log_name = f"{gate.id}.failed.log"
    accepted_receipt_name = f"{gate.id}.json"
    failed_receipt_name = f"{gate.id}.failed.json"
    lock_descriptor = -1
    lock_acquired = False
    try:
        try:
            lock_descriptor = os.open(
                lock_name,
                os.O_WRONLY | os.O_CREAT | os.O_EXCL
                | getattr(os, "O_NOFOLLOW", 0),
                0o600, dir_fd=directory)
        except FileExistsError as error:
            raise ReleaseContractError(
                f"gate namespace is already reserved for {gate.id}") from error
        lock_acquired = True
        os.write(lock_descriptor, (expected_sha + "\n").encode("ascii"))
        os.fsync(lock_descriptor)
        os.fsync(directory)
        if (_path_exists_at(directory, accepted_log_name)
                or _path_exists_at(directory, accepted_receipt_name)):
            raise ReleaseContractError(
                f"accepted gate output already exists for {gate.id}; refusing overwrite")

        started_at = _utc_now()
        started = time.monotonic()
        (return_code, oversized, written_size, written_digest,
         written_device, written_inode) = _write_process_log(
            gate, repo_root, child_environment, directory, running_name,
            max_log_bytes)
        duration = time.monotonic() - started
        ended_at = _utc_now()
        _assert_receipt_directory_unchanged(
            catalogue, receipt_dir, repo_root, directory)
        if oversized or return_code != 0:
            _publish_noreplace(directory, running_name, failed_log_name)
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
                "logPath": (directory_path / failed_log_name).relative_to(
                    repo_root).as_posix(),
            }
            _write_json_exclusive_at(directory, failed_receipt_name, failure)
            raise ReleaseContractError(f"gate {gate.id} failed: {error}")

        try:
            running_snapshot = _snapshot_at(
                directory, running_name, max_log_bytes + 1)
            if ((running_snapshot[1], running_snapshot[2],
                 running_snapshot[3], running_snapshot[4])
                    != (written_size, written_digest,
                        written_device, written_inode)):
                raise ReleaseContractError(
                    f"gate {gate.id} log changed after process capture")
            try:
                validate_packaged_text(running_snapshot[0].decode("utf-8"))
            except UnicodeDecodeError as error:
                raise ReleaseContractError(
                    f"gate {gate.id} log is not UTF-8") from error
        except BaseException:
            try:
                os.unlink(running_name, dir_fd=directory)
            except FileNotFoundError:
                pass
            raise
        _publish_noreplace(directory, running_name, accepted_log_name)
        accepted_snapshot = _snapshot_at(
            directory, accepted_log_name, max_log_bytes + 1)
        if (accepted_snapshot[0:5] != running_snapshot[0:5]):
            os.unlink(accepted_log_name, dir_fd=directory)
            raise ReleaseContractError(
                f"gate {gate.id} log changed during acceptance")
        value = _receipt_value(
            gate, head, captured, started_at, ended_at, duration,
            (directory_path / accepted_log_name).relative_to(repo_root).as_posix(),
            accepted_snapshot[1], accepted_snapshot[2], catalogue_sha256)
        try:
            _write_json_exclusive_at(directory, accepted_receipt_name, value)
        except BaseException:
            try:
                os.unlink(accepted_log_name, dir_fd=directory)
            except FileNotFoundError:
                pass
            raise
        _assert_receipt_directory_unchanged(
            catalogue, receipt_dir, repo_root, directory)
        return directory_path / accepted_receipt_name
    finally:
        if lock_acquired:
            owner = os.fstat(lock_descriptor)
            try:
                current = os.stat(
                    lock_name, dir_fd=directory, follow_symlinks=False)
            except FileNotFoundError:
                current = None
            if (current is not None
                    and (current.st_dev, current.st_ino)
                    == (owner.st_dev, owner.st_ino)):
                os.unlink(lock_name, dir_fd=directory)
                os.fsync(directory)
        if lock_descriptor >= 0:
            os.close(lock_descriptor)
        try:
            os.unlink(running_name, dir_fd=directory)
        except FileNotFoundError:
            pass
        os.close(directory)


def _read_receipt(
        path: Path, repo_root: Path,
        source_snapshots: list[object] | None = None) -> dict[str, object]:
    try:
        relative = path.absolute().relative_to(repo_root.absolute()).as_posix()
    except ValueError as error:
        raise ReleaseContractError(f"receipt outside repository: {path}") from error
    snapshot = read_regular_snapshot(
        repo_root, relative, maximum_bytes=16 * 1024 * 1024)
    if source_snapshots is not None:
        source_snapshots.append(snapshot)
    try:
        text = snapshot.content.decode("utf-8")
        validate_packaged_text(text)
        value = json.loads(text)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
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
        catalogue_path: Path,
        source_snapshots: list[object] | None = None) -> CommandReceipt:
    value = _read_receipt(receipt_path, repo_root, source_snapshots)
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
    expected_catalogue = (repo_root / CATALOGUE_RELATIVE).absolute()
    if catalogue_path.absolute() != expected_catalogue:
        raise ReleaseContractError(f"catalogue path must be {expected_catalogue}")
    catalogue_snapshot = read_regular_snapshot(
        repo_root, CATALOGUE_RELATIVE, maximum_bytes=16 * 1024 * 1024)
    if source_snapshots is not None:
        source_snapshots.append(catalogue_snapshot)
    if load_catalogue(
            catalogue_path, content=catalogue_snapshot.content) != catalogue:
        raise ReleaseContractError("catalogue object does not match committed bytes")
    catalogue_digest = catalogue_snapshot.sha256
    if value["catalogueSha256"] != catalogue_digest:
        raise ReleaseContractError(f"receipt {gate_id} catalogue SHA-256 mismatch")
    log_path = value["logPath"]
    if not isinstance(log_path, str):
        raise ReleaseContractError(f"receipt {gate_id} log path mismatch")
    log_snapshot = read_regular_snapshot(
        repo_root, log_path, maximum_bytes=DEFAULT_MAX_LOG_BYTES + 1)
    if source_snapshots is not None:
        source_snapshots.append(log_snapshot)
    try:
        validate_packaged_text(log_snapshot.content.decode("utf-8"))
    except UnicodeDecodeError as error:
        raise ReleaseContractError(f"receipt {gate_id} log is not UTF-8") from error
    if not isinstance(value["logSize"], int) or isinstance(value["logSize"], bool):
        raise ReleaseContractError(f"receipt {gate_id} log size mismatch")
    if log_snapshot.size != value["logSize"]:
        raise ReleaseContractError(f"receipt {gate_id} log size mismatch")
    if log_snapshot.sha256 != value["logSha256"]:
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
        expected_catalogue = (options.repo_root / CATALOGUE_RELATIVE).absolute()
        if options.catalogue.absolute() != expected_catalogue:
            raise ReleaseContractError(f"catalogue path must be {expected_catalogue}")
        catalogue_snapshot = read_regular_snapshot(
            options.repo_root, CATALOGUE_RELATIVE,
            maximum_bytes=16 * 1024 * 1024)
        catalogue = load_catalogue(
            options.catalogue, content=catalogue_snapshot.content)
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
