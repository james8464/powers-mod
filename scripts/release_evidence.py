#!/usr/bin/env python3

import hashlib
import json
import os
import re
import stat
import xml.etree.ElementTree as ElementTree
from pathlib import Path
from typing import Callable, Mapping

from release_contract import (
    COMMIT_PATTERN,
    EVIDENCE_VALIDATORS,
    ID_PATTERN,
    SHA256_PATTERN,
    EvidenceRow,
    ReleaseContractError,
    safe_regular_file,
    sha256_file,
)


MAX_EVIDENCE_BYTES = 256 * 1024 * 1024
RUNTIME_FIELDS = frozenset({
    "width", "height", "requestedGuiScale", "effectiveGuiScale", "mipmapLevel",
    "reducedMotion", "graphicsMode", "renderDistance", "resourcePacks",
    "gameTime", "weather",
})


def _plain_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _number(value: object) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _read_bytes(path: Path) -> bytes:
    descriptor = None
    try:
        descriptor = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        before = os.fstat(descriptor)
    except OSError as error:
        raise ReleaseContractError(f"cannot open evidence {path}: {error}") from error
    if (not stat.S_ISREG(before.st_mode) or before.st_nlink != 1
            or before.st_size <= 0 or before.st_size > MAX_EVIDENCE_BYTES):
        os.close(descriptor)
        raise ReleaseContractError(f"evidence size outside 1..{MAX_EVIDENCE_BYTES}: {path}")
    try:
        blocks: list[bytes] = []
        total = 0
        while True:
            block = os.read(descriptor, min(1024 * 1024, MAX_EVIDENCE_BYTES + 1 - total))
            if not block:
                break
            blocks.append(block)
            total += len(block)
            if total > MAX_EVIDENCE_BYTES:
                raise ReleaseContractError(f"evidence exceeds {MAX_EVIDENCE_BYTES} bytes: {path}")
        after = os.fstat(descriptor)
        current = path.lstat()
        if ((before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns)
                != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns)
                or (after.st_dev, after.st_ino) != (current.st_dev, current.st_ino)
                or total != after.st_size):
            raise ReleaseContractError(f"evidence changed while reading: {path}")
        return b"".join(blocks)
    finally:
        os.close(descriptor)


def _text(path: Path, content: bytes) -> str:
    try:
        return content.decode("utf-8")
    except UnicodeDecodeError as error:
        raise ReleaseContractError(f"evidence is not UTF-8: {path}") from error


def _json(path: Path, content: bytes) -> dict[str, object]:
    try:
        value = json.loads(_text(path, content))
    except json.JSONDecodeError as error:
        raise ReleaseContractError(f"invalid evidence JSON {path}: {error}") from error
    if not isinstance(value, dict) or not all(isinstance(key, str) for key in value):
        raise ReleaseContractError(f"evidence JSON must be an object: {path}")
    return value


def _exact_commit(data: Mapping[str, object], expected_commit: str, label: str) -> None:
    if data.get("commit") != expected_commit:
        raise ReleaseContractError(f"{label}: commit mismatch")


def _passed(data: Mapping[str, object], expected_commit: str, label: str) -> None:
    _exact_commit(data, expected_commit, label)
    if data.get("schemaVersion") != 1:
        raise ReleaseContractError(f"{label}: schemaVersion mismatch")
    if data.get("passed") is not True:
        raise ReleaseContractError(f"{label}: passed must be true")


def _expected_int(result: Mapping[str, object], name: str, minimum: int = 0) -> int:
    value = result.get(name)
    if not _plain_int(value) or value < minimum:
        raise ReleaseContractError(f"result {name}: expected integer >= {minimum}")
    return value


def _validate_junit(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    try:
        root = ElementTree.fromstring(content)
    except ElementTree.ParseError as error:
        raise ReleaseContractError(f"JUnit XML is malformed: {error}") from error
    if root.tag != "testsuites":
        raise ReleaseContractError("JUnit XML root must be testsuites")

    def count(element: ElementTree.Element, name: str) -> int:
        raw = element.attrib.get(name)
        try:
            value = int(raw) if raw is not None else -1
        except ValueError as error:
            raise ReleaseContractError(f"JUnit {name} count is invalid") from error
        if value < 0:
            raise ReleaseContractError(f"JUnit {name} count is invalid")
        return value

    names = ("tests", "failures", "errors", "skipped")
    totals = {name: count(root, name) for name in names}
    if totals["failures"] != 0:
        raise ReleaseContractError("JUnit failures must be zero")
    if totals["errors"] != 0:
        raise ReleaseContractError("JUnit errors must be zero")
    if totals["skipped"] != 0:
        raise ReleaseContractError("JUnit skipped tests must be zero")
    suites = list(root.findall("testsuite"))
    if not suites:
        raise ReleaseContractError("JUnit XML contains no test suites")
    summed = {name: sum(count(suite, name) for suite in suites) for name in names}
    if summed != totals:
        raise ReleaseContractError("JUnit totals are inconsistent with suites")
    expected = {name: _expected_int(row.result, name) for name in names}
    if totals != expected:
        raise ReleaseContractError("JUnit totals do not match evidence result")
    return dict(totals)


def _validate_fabric(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    text = _text(path, content)
    required = _expected_int(row.result, "requiredTests", 1)
    if re.search(r"\b(?:required tests? failed|failed required tests?)\b", text, re.IGNORECASE):
        raise ReleaseContractError("Fabric log contains failed required tests")
    if re.search(r"\[(?:Server thread|ServerMain)/ERROR\]", text):
        raise ReleaseContractError("Fabric log contains server error")
    if f"All {required} required tests passed :)" not in text:
        raise ReleaseContractError("Fabric required-test success marker is missing")
    running = {int(value) for value in re.findall(r"\bRunning (\d+) tests\b", text)}
    if running != {required}:
        raise ReleaseContractError("Fabric exact required-test total is missing or ambiguous")
    return {"requiredTests": required, "passedTests": required}


def _validate_soak(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    data = _json(path, content)
    if data.get("schema") != 3:
        raise ReleaseContractError("restart soak schema must be 3")
    if data.get("git_commit") != expected_commit:
        raise ReleaseContractError("restart soak commit mismatch")
    if data.get("passed") is not True or data.get("status") != "passed":
        raise ReleaseContractError("restart soak passed/status contract failed")
    if data.get("failure") != "":
        raise ReleaseContractError("restart soak failure must be empty")
    expected_cycles = _expected_int(row.result, "expectedCycles", 1)
    minimum_seconds = _expected_int(row.result, "minimumSeconds", 86_400)
    if data.get("requested_cycles") != expected_cycles:
        raise ReleaseContractError("restart soak requested cycles mismatch")
    if data.get("completed_cycles") != expected_cycles:
        raise ReleaseContractError("restart soak completed cycles mismatch")
    if not _number(data.get("requested_hours")) or data["requested_hours"] < 24:
        raise ReleaseContractError("restart soak requested hours are below 24")
    if not _number(data.get("elapsed_seconds")) or data["elapsed_seconds"] < minimum_seconds:
        raise ReleaseContractError(f"restart soak elapsed time is below {minimum_seconds}")
    cycles = data.get("cycles")
    if not isinstance(cycles, list) or len(cycles) != expected_cycles:
        raise ReleaseContractError("restart soak cycle list is incomplete")
    predicates = (
        "ready", "client_connected", "client_disconnected", "startup_verified",
        "seeded", "settled", "status_verified", "rollover_seeded",
        "clean_diagnostics",
    )
    for expected_number, raw_cycle in enumerate(cycles, 1):
        if not isinstance(raw_cycle, dict) or raw_cycle.get("cycle") != expected_number:
            raise ReleaseContractError(f"restart soak cycle {expected_number}: identity mismatch")
        for predicate in predicates:
            if raw_cycle.get(predicate) is not True:
                raise ReleaseContractError(
                    f"restart soak cycle {expected_number}: {predicate} must be true")
        if raw_cycle.get("error_lines") != []:
            raise ReleaseContractError(f"restart soak cycle {expected_number}: error_lines not empty")
        scheduled_signal = expected_number % 12 == 0
        expected_mode = "sigterm" if scheduled_signal else "clean"
        expected_exit = 143 if scheduled_signal else 0
        if (raw_cycle.get("shutdown_mode") != expected_mode
                or raw_cycle.get("exit_code") != expected_exit):
            raise ReleaseContractError(
                f"restart soak cycle {expected_number}: shutdown boundary mismatch")
    return {
        "completedCycles": expected_cycles,
        "elapsedSeconds": data["elapsed_seconds"],
        "sigtermBoundaries": expected_cycles // 12,
    }


def _validate_profiles(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    data = _json(path, content)
    _passed(data, expected_commit, "real-client profiles")
    expected_clients = row.result.get("clients")
    if expected_clients != [10, 50, 100]:
        raise ReleaseContractError("profile result must require 10/50/100 clients")
    minimum = _expected_int(row.result, "minimumDurationSeconds", 1800)
    profiles = data.get("profiles")
    if not isinstance(profiles, list) or len(profiles) != 3:
        raise ReleaseContractError("real-client profiles must contain 10/50/100 rows")
    by_clients: dict[int, dict[str, object]] = {}
    for profile in profiles:
        if not isinstance(profile, dict) or not _plain_int(profile.get("clients")):
            raise ReleaseContractError("real-client profile row is invalid")
        clients = profile["clients"]
        if clients in by_clients:
            raise ReleaseContractError("real-client profile client count is duplicated")
        by_clients[clients] = profile
    if sorted(by_clients) != [10, 50, 100]:
        raise ReleaseContractError("real-client profiles must contain 10/50/100 rows")
    for clients, profile in by_clients.items():
        if profile.get("actorType") != "real-client":
            raise ReleaseContractError(f"profile {clients}: real-client actor required")
        if not _number(profile.get("durationSeconds")) or profile["durationSeconds"] < minimum:
            raise ReleaseContractError(f"profile {clients}: duration below {minimum}")
        if profile.get("errors") != 0:
            raise ReleaseContractError(f"profile {clients}: errors must be zero")
    return {"clients": [10, 50, 100], "minimumDurationSeconds": minimum}


def _validate_compatibility(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    data = _json(path, content)
    _passed(data, expected_commit, "compatibility")
    required = _expected_int(row.result, "requiredTests", 1)
    if data.get("requiredTests") != required or data.get("passedTests") != required:
        raise ReleaseContractError("compatibility test count mismatch")
    expected_artifacts = row.result.get("artifacts")
    if not isinstance(expected_artifacts, list) or not expected_artifacts:
        raise ReleaseContractError("compatibility expected artifacts are missing")
    if data.get("artifacts") != expected_artifacts:
        raise ReleaseContractError("compatibility artifacts mismatch")
    for artifact in expected_artifacts:
        if (not isinstance(artifact, dict)
                or not isinstance(artifact.get("id"), str)
                or not isinstance(artifact.get("versionId"), str)
                or not _plain_int(artifact.get("size")) or artifact["size"] <= 0
                or not isinstance(artifact.get("sha256"), str)
                or not SHA256_PATTERN.fullmatch(artifact["sha256"])):
            raise ReleaseContractError("compatibility artifacts are malformed")
    limitations = data.get("limitations")
    if limitations != list(row.limitations):
        raise ReleaseContractError("compatibility limitations mismatch")
    return {
        "requiredTests": required,
        "passedTests": required,
        "artifacts": expected_artifacts,
        "limitations": limitations,
    }


def _validate_runtime(runtime: object, identifier: str) -> None:
    if not isinstance(runtime, dict) or set(runtime) != RUNTIME_FIELDS:
        raise ReleaseContractError(f"review {identifier}: runtime metadata is incomplete")
    integer_fields = (
        "width", "height", "requestedGuiScale", "effectiveGuiScale", "mipmapLevel",
        "renderDistance", "gameTime",
    )
    if not all(_plain_int(runtime.get(name)) and runtime[name] >= 0 for name in integer_fields):
        raise ReleaseContractError(f"review {identifier}: runtime numeric metadata is invalid")
    if runtime["width"] <= 0 or runtime["height"] <= 0:
        raise ReleaseContractError(f"review {identifier}: runtime window is invalid")
    if not isinstance(runtime.get("reducedMotion"), bool):
        raise ReleaseContractError(f"review {identifier}: reducedMotion is invalid")
    if runtime.get("graphicsMode") not in ("fast", "fancy", "fabulous"):
        raise ReleaseContractError(f"review {identifier}: graphicsMode is invalid")
    if runtime.get("weather") not in ("clear", "rain", "thunder"):
        raise ReleaseContractError(f"review {identifier}: weather is invalid")
    packs = runtime.get("resourcePacks")
    if not isinstance(packs, list) or not packs or not all(isinstance(item, str) and item for item in packs):
        raise ReleaseContractError(f"review {identifier}: resourcePacks are invalid")


def _validate_review(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    data = _json(path, content)
    _passed(data, expected_commit, f"{row.kind} review")
    decisions = data.get("decisions")
    expected_count = _expected_int(row.result, "decisions", 1)
    if not isinstance(decisions, list) or len(decisions) != expected_count:
        raise ReleaseContractError(f"{row.kind} review decision count mismatch")
    identifiers: set[str] = set()
    for decision in decisions:
        if not isinstance(decision, dict):
            raise ReleaseContractError(f"{row.kind} review decision is invalid")
        identifier = decision.get("id")
        if not isinstance(identifier, str) or not ID_PATTERN.fullmatch(identifier) or identifier in identifiers:
            raise ReleaseContractError(f"{row.kind} review decision id is invalid")
        identifiers.add(identifier)
        if decision.get("decision") not in ("PASS", "REPAIRED"):
            raise ReleaseContractError(f"review {identifier}: decision is not accepted")
        if decision.get("metadataSource") != "client-emitted":
            raise ReleaseContractError(f"review {identifier}: client-emitted metadata required")
        raw_path = decision.get("rawPath")
        if not isinstance(raw_path, str):
            raise ReleaseContractError(f"review {identifier}: rawPath is required")
        raw = safe_regular_file(path.parent, raw_path)
        expected_digest = decision.get("sha256")
        if not isinstance(expected_digest, str) or not SHA256_PATTERN.fullmatch(expected_digest):
            raise ReleaseContractError(f"review {identifier}: raw SHA-256 is invalid")
        if sha256_file(raw) != expected_digest:
            raise ReleaseContractError(f"review {identifier}: raw SHA-256 mismatch")
        _validate_runtime(decision.get("runtime"), identifier)
    return {"decisions": expected_count, "decisionIds": sorted(identifiers)}


def _validate_generic(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    data = _json(path, content)
    _passed(data, expected_commit, row.kind)
    for key, expected in row.result.items():
        if data.get(key) != expected:
            raise ReleaseContractError(f"{row.kind}: typed field {key} mismatch")
    if row.kind == "packet-fault":
        if data.get("profiles") != 6 or data.get("clientConverged") is not True:
            raise ReleaseContractError("packet-fault: six converged profiles required")
    elif row.kind == "migration":
        if not _plain_int(data.get("cases")) or data["cases"] <= 0:
            raise ReleaseContractError("migration: positive cases required")
    elif row.kind == "manifest":
        if (not _plain_int(data.get("entries")) or data["entries"] <= 0
                or data.get("stale") != 0):
            raise ReleaseContractError("manifest: positive entries and zero stale rows required")
    elif row.kind == "four-client":
        clients = data.get("clients")
        if (not _plain_int(clients) or clients < 4
                or data.get("joined") != clients or data.get("disconnected") != clients):
            raise ReleaseContractError("four-client: four joined/disconnected clients required")
    elif row.kind == "github-ci" and data.get("conclusion") != "success":
        raise ReleaseContractError("github-ci: conclusion must be success")
    return dict(data)


def _validate_limitations(
        row: EvidenceRow, path: Path, expected_commit: str,
        content: bytes) -> dict[str, object]:
    data = _json(path, content)
    if data.get("schemaVersion") != 1:
        raise ReleaseContractError("limitations schemaVersion mismatch")
    _exact_commit(data, expected_commit, "limitations")
    limitations = data.get("limitations")
    expected_count = _expected_int(row.result, "count")
    if not isinstance(limitations, list) or len(limitations) != expected_count:
        raise ReleaseContractError("limitations count mismatch")
    identifiers: set[str] = set()
    texts: list[str] = []
    for limitation in limitations:
        if not isinstance(limitation, dict):
            raise ReleaseContractError("limitation row is invalid")
        identifier = limitation.get("id")
        text = limitation.get("text")
        if not isinstance(identifier, str) or not ID_PATTERN.fullmatch(identifier) or identifier in identifiers:
            raise ReleaseContractError("limitation id is invalid or duplicated")
        if not isinstance(text, str) or not text.strip():
            raise ReleaseContractError(f"limitation {identifier}: text is blank")
        identifiers.add(identifier)
        texts.append(text)
    if texts != list(row.limitations):
        raise ReleaseContractError("limitations do not match manifest verbatim")
    return {"count": len(texts), "limitations": texts}


VALIDATORS: Mapping[
    str, Callable[[EvidenceRow, Path, str, bytes], dict[str, object]]
] = {
    "junit-xml": _validate_junit,
    "fabric-log": _validate_fabric,
    "restart-soak": _validate_soak,
    "real-client-profiles": _validate_profiles,
    "compatibility": _validate_compatibility,
    "manual-review": _validate_review,
    "visual-review": _validate_review,
    "packet-fault": _validate_generic,
    "migration": _validate_generic,
    "manifest": _validate_generic,
    "four-client": _validate_generic,
    "github-ci": _validate_generic,
    "limitations": _validate_limitations,
}


def validate_evidence(
        row: EvidenceRow, path: Path, expected_commit: str) -> dict[str, object]:
    if not isinstance(expected_commit, str) or not COMMIT_PATTERN.fullmatch(expected_commit):
        raise ReleaseContractError("expected commit is not a full lowercase SHA")
    if row.commit != expected_commit:
        raise ReleaseContractError(f"evidence {row.id}: expected commit mismatch")
    expected_validator = EVIDENCE_VALIDATORS.get(row.kind)
    if expected_validator is None or row.validator != expected_validator or row.validator not in VALIDATORS:
        raise ReleaseContractError(f"evidence {row.id}: validator is not registered")
    content = _read_bytes(path)
    if len(content) != row.size:
        raise ReleaseContractError(f"evidence {row.id}: size mismatch")
    if hashlib.sha256(content).hexdigest() != row.sha256:
        raise ReleaseContractError(f"evidence {row.id}: SHA-256 mismatch")
    return VALIDATORS[row.validator](row, path, expected_commit, content)
