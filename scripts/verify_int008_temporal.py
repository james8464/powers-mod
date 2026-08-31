#!/usr/bin/env python3
"""Strictly verify one exact-SHA INT-008 temporal-ownership evidence directory."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


SHA = re.compile(r"[0-9a-f]{40}")
SOURCE_ROW = re.compile(
    r"[ADMT] (?:[0-9a-f]{64}|-) (?:[0-9a-f]{64}|-) [A-Za-z0-9_./@+-]+")
INT008_BASE_SHA = "98b181671b1514a3695ccb8f1ba1985092bce3dd"
POST_CAPTURE_PREFIX = "docs/verification/evidence/2026-08-29-int-008/"
PRIVATE_MARKERS = (b"/Users/", b"\\Users\\", b".worktrees/", b"file://",
                   b"james8464")
CASES = {
    "admin-preservation",
    "external-supersession",
    "crystal-control-deadline",
    "world-managers-paused",
    "projectile-pause-resume",
    "lifecycle-cleanup",
}
ROW_FIELDS = {"schemaVersion", "implementationSha", "case", "result",
              "controlTicks", "worldTicks", "facts"}
FACT_FIELDS = {
    "admin-preservation": {"acquired", "leaseActive", "vanillaFrozen"},
    "external-supersession": {"leaseActive", "superseded", "vanillaFrozen"},
    "crystal-control-deadline": {"activeAt1199", "clock", "duration",
                                 "releasedAt1200", "worldTicksParked"},
    "world-managers-paused": {"celestialPaused", "channelsPaused",
                              "energyMutated", "externalFreeze", "fieldsPaused",
                              "heraldCadencePaused", "ownedFreeze", "realmPaused",
                              "worldAdvanced"},
    "projectile-pause-resume": {"frozenDistanceSquared", "resumedDistanceSquared"},
    "lifecycle-cleanup": {"dampeningReleased", "deathReleased",
                          "disconnectReleased", "leaseActive",
                          "mismatchedSourcePreserved", "shadowLossReleased",
                          "shutdownReleased", "vanillaFrozen"},
}
SUMMARY_FIELDS = {"schemaVersion", "implementationSha", "framework", "tests",
                  "failures", "errors", "skipped"}


def _duplicates(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def _constant(value):
    raise ValueError(f"JSON number must be finite: {value}")


def _loads(text: str, label: str):
    try:
        return json.loads(text, object_pairs_hook=_duplicates, parse_constant=_constant)
    except json.JSONDecodeError as error:
        raise ValueError(f"invalid JSON: {label}") from error


def _privacy(data: bytes, label: str) -> None:
    if any(marker in data for marker in PRIVATE_MARKERS):
        raise ValueError(f"privacy violation: {label}")


def _read(path: Path) -> bytes:
    if not path.is_file() or path.is_symlink():
        raise ValueError(f"missing regular evidence file: {path.name}")
    data = path.read_bytes()
    _privacy(data, path.name)
    if b"\r" in data:
        raise ValueError(f"text must use normalized LF: {path.name}")
    return data


def _read_json(path: Path):
    return _loads(_read(path).decode("utf-8"), path.name)


def _read_rows(path: Path) -> list[dict]:
    rows = []
    for number, line in enumerate(_read(path).decode("utf-8").splitlines(), 1):
        value = _loads(line, f"temporal row {number}")
        if not isinstance(value, dict):
            raise ValueError(f"temporal row {number} is not an object")
        rows.append(value)
    return rows


def _git(repository: Path, *arguments: str, text: bool = True):
    try:
        return subprocess.check_output(
            ["git", *arguments], cwd=repository, text=text,
            stderr=subprocess.PIPE)
    except (OSError, subprocess.CalledProcessError) as error:
        raise ValueError(f"Git verification failed: {' '.join(arguments)}") from error


def _verify_git(repository: Path, base_sha: str, implementation_sha: str,
                inventory_lines: list[str], expected_base_sha: str) -> None:
    repository = repository.resolve()
    if not repository.is_dir():
        raise ValueError("Git repository is missing")
    for sha, label in ((base_sha, "base"), (implementation_sha, "implementation")):
        if SHA.fullmatch(sha) is None:
            raise ValueError(f"invalid {label} SHA")
        resolved = _git(repository, "rev-parse", f"{sha}^{{commit}}").strip()
        if resolved != sha:
            raise ValueError(f"unresolved {label} SHA")
    if base_sha != expected_base_sha:
        raise ValueError("build metadata does not use the immutable INT-008 base")
    try:
        subprocess.run(["git", "merge-base", "--is-ancestor", base_sha,
                        implementation_sha], cwd=repository, check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        subprocess.run(["git", "merge-base", "--is-ancestor", implementation_sha,
                        "HEAD"], cwd=repository, check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    except (OSError, subprocess.CalledProcessError) as error:
        raise ValueError("implementation ancestry mismatch") from error
    late_paths = _git(repository, "diff", "--name-only",
                      f"{implementation_sha}..HEAD").splitlines()
    if any(not path.startswith(POST_CAPTURE_PREFIX) for path in late_paths):
        raise ValueError("post-capture changes are not evidence-package-only")
    changes = _git(repository, "diff", "--name-status", "--no-renames",
                   f"{base_sha}..{implementation_sha}").splitlines()
    expected = []
    for change in changes:
        parts = change.split("\t")
        if len(parts) != 2 or parts[0] not in {"A", "D", "M", "T"}:
            raise ValueError("unsupported Git source change")
        status, relative = parts
        if not re.fullmatch(r"[A-Za-z0-9_./@+-]+", relative):
            raise ValueError("unsafe Git source path")
        old_digest = "-" if status == "A" else hashlib.sha256(
            _git(repository, "show", f"{base_sha}:{relative}", text=False)).hexdigest()
        new_digest = "-" if status == "D" else hashlib.sha256(
            _git(repository, "show", f"{implementation_sha}:{relative}", text=False)).hexdigest()
        expected.append(f"{status} {old_digest} {new_digest} {relative}")
    if inventory_lines != sorted(expected):
        raise ValueError("Git source inventory does not match the exact changed-file set")


def _nonnegative_int(value, label: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value < 0:
        raise ValueError(f"{label} must be a non-negative integer")
    return value


def _finite(value, label: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{label} must be finite")
    value = float(value)
    if not math.isfinite(value) or value < 0.0:
        raise ValueError(f"{label} must be finite and non-negative")
    return value


def _validate_summary(path: Path, implementation_sha: str, framework: str,
                      expected_tests: int) -> None:
    summary = _read_json(path)
    if not isinstance(summary, dict) or set(summary) != SUMMARY_FIELDS \
            or summary["schemaVersion"] != 1 \
            or summary["implementationSha"] != implementation_sha \
            or summary["framework"] != framework \
            or _nonnegative_int(summary["tests"], f"{framework} summary tests") \
            != expected_tests \
            or any(_nonnegative_int(summary[field], f"{framework} summary {field}") != 0
                   for field in ("failures", "errors", "skipped")):
        raise ValueError(f"{framework} test summary mismatch")


def _junit_capture(root: Path) -> tuple[tuple[int, int, int, int], int, str]:
    directory = root / "logs/junit"
    paths = sorted(directory.glob("TEST-*.xml")) if directory.is_dir() else []
    if not paths:
        raise ValueError("JUnit raw results are missing")
    totals = [0, 0, 0, 0]
    inventory = []
    for path in paths:
        data = _read(path)
        inventory.append(f"{hashlib.sha256(data).hexdigest()}  {path.name}\n")
        try:
            suite = ET.fromstring(data)
        except ET.ParseError as error:
            raise ValueError("JUnit raw results are malformed") from error
        if suite.tag != "testsuite":
            raise ValueError("JUnit raw results must contain test suites")
        for index, field in enumerate(("tests", "failures", "errors", "skipped")):
            try:
                value = int(suite.attrib.get(field, "0"))
            except ValueError as error:
                raise ValueError("JUnit raw results contain invalid totals") from error
            totals[index] += _nonnegative_int(value, f"JUnit raw {field}")
    digest = hashlib.sha256("".join(inventory).encode("utf-8")).hexdigest()
    return tuple(totals), len(paths), digest


def _validate_aggregate_receipts(output: str, implementation_sha: str,
        junit_tests: int, junit_files: int, junit_digest: str) -> None:
    preflight = f"INT-008 aggregate preflight verified: {implementation_sha}; clean=true"
    postflight = f"INT-008 aggregate postflight verified: {implementation_sha}; clean=true"
    junit = (f"INT-008 JUnit capture verified: files={junit_files}; "
             f"tests={junit_tests}; sha256={junit_digest}")
    if output.count(preflight) != 1 or output.count(postflight) != 1:
        raise ValueError("aggregate checkout receipts do not match the implementation SHA")
    if output.count(junit) != 1:
        raise ValueError("JUnit capture digest does not match the retained raw inventory")
    build = output.find("BUILD SUCCESSFUL")
    if not (0 <= output.find(preflight) < build < output.find(postflight)
            < output.find(junit)):
        raise ValueError("aggregate checkout receipts are not ordered around the full gate")
    gate = output[output.find(preflight):build]
    required_tasks = {
        "auditJavaSources", "auditNonItemAssets", "compileJava", "compileClientJava",
        "compileExampleExtensionJava", "compileGametestJava", "runGameTest",
        "compileTestJava", "test", "testPythonScripts", "validatePowerResources",
        "verifyItemDocs", "verifyMagicDocs", "verifyRankDocs", "verifyVfxAssetAudit", "check",
    }
    executed = re.findall(r"(?m)^> Task :([A-Za-z]+)$", gate)
    if any(executed.count(task) != 1 for task in required_tasks) \
            or "All 167 required tests passed" not in gate:
        raise ValueError("aggregate execution does not prove the complete rerun gate")


def _python_total(root: Path) -> int:
    output = _read(root / "logs/aggregate-check.log").decode("utf-8")
    matches = re.findall(r"(?m)^Ran ([0-9]+) tests? in [0-9.]+s$", output)
    if len(matches) != 1 or not re.search(r"(?m)^OK$", output) \
            or "BUILD SUCCESSFUL" not in output or "BUILD FAILED" in output:
        raise ValueError("Python raw results do not prove a green aggregate run")
    return int(matches[0])


def _validate_facts(case: str, facts: dict) -> None:
    if not isinstance(facts, dict) or set(facts) != FACT_FIELDS[case]:
        raise ValueError(f"{case} fact schema mismatch")
    if case != "projectile-pause-resume":
        for field, value in facts.items():
            expected_type = (str if field == "clock" else int if field == "duration" else bool)
            if type(value) is not expected_type:
                raise ValueError(f"{case}.{field} fact type mismatch")
    if case == "admin-preservation" and facts != {
            "acquired": False, "leaseActive": False, "vanillaFrozen": True}:
        raise ValueError("administrator preservation proof failed")
    if case == "external-supersession" and facts != {
            "leaseActive": False, "superseded": True, "vanillaFrozen": True}:
        raise ValueError("external supersession proof failed")
    if case == "crystal-control-deadline" and facts != {
            "activeAt1199": True, "clock": "CONTROL", "duration": 1200,
            "releasedAt1200": True, "worldTicksParked": True}:
        raise ValueError("control deadline proof failed")
    if case == "world-managers-paused" and facts != {
            "celestialPaused": True, "channelsPaused": True,
            "energyMutated": False, "externalFreeze": True,
            "fieldsPaused": True, "heraldCadencePaused": True, "ownedFreeze": True,
            "realmPaused": True, "worldAdvanced": False}:
        raise ValueError("world manager pause proof failed")
    if case == "projectile-pause-resume":
        if _finite(facts["frozenDistanceSquared"], "frozen distance") > 1.0E-8 \
                or _finite(facts["resumedDistanceSquared"], "resumed distance") <= 0.01:
            raise ValueError("projectile pause/resume proof failed")
    if case == "lifecycle-cleanup" and facts != {
            "dampeningReleased": True, "deathReleased": True,
            "disconnectReleased": True, "leaseActive": False,
            "mismatchedSourcePreserved": True, "shadowLossReleased": True,
            "shutdownReleased": True, "vanillaFrozen": False}:
        raise ValueError("lifecycle cleanup proof failed")


def validate(root: Path, repository: Path,
             expected_base_sha: str = INT008_BASE_SHA) -> dict:
    root = root.resolve()
    if not root.is_dir() or root.is_symlink():
        raise ValueError("evidence root must be a directory")
    for path in root.rglob("*"):
        if path.is_symlink():
            raise ValueError(f"symlink is forbidden: {path.name}")
        if path.is_file():
            _privacy(path.read_bytes(), path.name)

    metadata = _read_json(root / "build-metadata.json")
    expected_metadata = {"schemaVersion", "task", "baseSha", "implementationSha", "result",
                         "gameTests", "junitTests", "pythonTests"}
    if not isinstance(metadata, dict) or set(metadata) != expected_metadata \
            or metadata["schemaVersion"] != 2 or metadata["task"] != "INT-008" \
            or metadata["result"] not in {"PENDING", "PASS"}:
        raise ValueError("build metadata schema mismatch")
    implementation_sha = metadata["implementationSha"]
    if not isinstance(implementation_sha, str) or SHA.fullmatch(implementation_sha) is None:
        raise ValueError("invalid implementation SHA")
    base_sha = metadata["baseSha"]
    if not isinstance(base_sha, str) or SHA.fullmatch(base_sha) is None:
        raise ValueError("invalid base SHA")
    if _nonnegative_int(metadata["gameTests"], "GameTest count") != 167 \
            or _nonnegative_int(metadata["junitTests"], "JUnit count") < 1825 \
            or _nonnegative_int(metadata["pythonTests"], "Python count") < 1:
        raise ValueError("build totals mismatch")
    _validate_summary(root / "logs/junit-summary.json", implementation_sha, "JUnit",
                      metadata["junitTests"])
    _validate_summary(root / "logs/python-summary.json", implementation_sha, "Python",
                      metadata["pythonTests"])
    junit_totals, junit_files, junit_digest = _junit_capture(root)
    if junit_totals != (metadata["junitTests"], 0, 0, 0):
        raise ValueError("JUnit raw results do not match build metadata")
    if _python_total(root) != metadata["pythonTests"]:
        raise ValueError("Python raw results do not match build metadata")
    aggregate_output = _read(root / "logs/aggregate-check.log").decode("utf-8")
    _validate_aggregate_receipts(aggregate_output, implementation_sha,
                                 metadata["junitTests"], junit_files, junit_digest)

    rows = _read_rows(root / "temporal-assertions.jsonl")
    if len(rows) != len(CASES) or {row.get("case") for row in rows} != CASES:
        raise ValueError("temporal case coverage mismatch")
    for row in rows:
        if set(row) != ROW_FIELDS or row["schemaVersion"] != 2 or row["result"] != "PASS":
            raise ValueError("temporal assertion schema mismatch")
        if row["implementationSha"] != implementation_sha:
            raise ValueError("implementation identity mismatch")
        _nonnegative_int(row["controlTicks"], "control ticks")
        _nonnegative_int(row["worldTicks"], "world ticks")
        control, world = row["controlTicks"], row["worldTicks"]
        if (row["case"] == "crystal-control-deadline" and (control, world) != (1200, 0)) \
                or (row["case"] == "world-managers-paused" and world != 0) \
                or (row["case"] == "projectile-pause-resume"
                    and (world < 4 or control - world < 4)):
            raise ValueError(f"{row['case']} measured clock proof failed")
        _validate_facts(row["case"], row["facts"])

    inventory_lines = _read(root / "source-inventory.txt").decode("utf-8").splitlines()
    if not inventory_lines or inventory_lines != sorted(inventory_lines) \
            or len(inventory_lines) != len(set(inventory_lines)) \
            or any(SOURCE_ROW.fullmatch(line) is None for line in inventory_lines):
        raise ValueError("source inventory must be sorted, unique, and digest-bound")
    _verify_git(repository, base_sha, implementation_sha, inventory_lines,
                expected_base_sha)
    log = _read(root / "logs/gametest.log").decode("utf-8")
    if f"INT-008 checkout verified: {implementation_sha}" not in log \
            or "BUILD SUCCESSFUL" not in log or "BUILD FAILED" in log:
        raise ValueError("GameTest checkout verification is missing or failed")
    if "All 167 required tests passed" not in log \
            or any(case not in log for case in CASES):
        raise ValueError("GameTest log coverage mismatch")
    row_lines = _read(root / "temporal-assertions.jsonl").decode("utf-8").splitlines()
    log_rows = [line.split("INT008_TEMPORAL ", 1)[1]
                for line in log.splitlines() if "INT008_TEMPORAL " in line]
    if log_rows != row_lines:
        raise ValueError("GameTest log rows do not byte-match temporal JSONL rows")
    _read(root / "README.md")
    return {"implementationSha": implementation_sha, "caseCount": len(rows),
            "result": metadata["result"]}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    options = parser.parse_args()
    result = validate(options.root, options.repository)
    print(f"INT-008 evidence verified: {result['caseCount']} cases; "
          f"sha={result['implementationSha']}; result={result['result']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
