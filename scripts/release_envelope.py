#!/usr/bin/env python3

import argparse
import hashlib
import io
import json
import os
import re
import stat
import subprocess
import sys
import zipfile
from collections.abc import Mapping
from pathlib import Path, PurePosixPath

from release_contract import (
    COMMIT_PATTERN,
    GateCatalogue,
    ReleaseContractError,
    load_catalogue,
    load_evidence_manifest,
    read_regular_snapshot,
    recheck_regular_snapshot,
    safe_regular_file,
    sha256_file,
    write_bytes_atomic,
)
from release_evidence import validate_evidence
from release_gate import verify_receipt


DEFAULT_PLAN = Path("docs/superpowers/plans/2026-08-12-stages-1-8-completion.md")
DEFAULT_BACKLOG = Path("docs/planning/IMPROVEMENT_BACKLOG.md")
OUTPUT_NAMES = ("release-envelope.json", "release-envelope.md", "SHA256SUMS")
REQUIRED_LEDGER_IDS = (
    "QA-001", "PERF-001", "QA-005", "QA-006", "PRG-001", "PERF-005",
    "PERF-006", "COR-020", "PERF-012", "PERF-014", "PERF-016",
    "PERF-015", "PERF-013", "NET-007", "NET-009", "NET-010", "NET-011",
    "QA-009", "QA-010", "QA-016", "PERF-010", "UX-004", "VFX-011",
    "VFX-009", "VFX-004", "VFX-005", "VFX-006", "VFX-007", "INT-008",
    "INT-009", "INT-010", "INT-011", "INT-007", "INT-012", "INT-014",
    "PWR-004", "PWR-006", "PWR-011", "PWR-014", "PWR-015", "PWR-022",
    "PWR-023", "PWR-024", "SPL-004", "SPL-005", "SPL-007", "SPL-009",
    "SPL-011", "CRY-003", "CRY-006", "CRY-007", "PRG-003", "PRG-004",
    "PRG-009", "ART-003", "ART-006", "ART-014", "ART-016", "ART-020",
    "SHD-011", "SHD-013", "SHD-008", "SHD-009", "SHD-010", "SHD-014",
    "SHD-016", "WRLD-008", "MOB-006", "WRLD-015", "MOB-007", "MOB-014",
    "MOB-015",
)
REQUIRED_FINAL_ACCEPTANCE = (
    "Regenerate the complete `QA-005` checklist on the final commit.",
    "Rerun final 10/50/100-player 30-minute profiles and the complete 24-hour restart soak.",
    "Pass `./gradlew clean check pitest verifyScreenshots verifyVisualGoldens saveMigrationCorpus syntheticSoak --rerun-tasks --no-daemon`.",
    "Pass complete Fabric server/client GameTests, dedicated-server reload/save/restart, compatibility, packet-fault, and four-client campaign gates.",
    "Verify asset, sound, resource, documentation, migration, source-quality, and exact-audit manifests.",
    "Build the final JAR/report, publish GitHub Actions provenance with `actions/attest@v4`, and verify it using `gh attestation verify`.",
    "Confirm only `main` exists, the worktree is clean, local/remote SHAs match, and GitHub Actions is green.",
    "Remove `QA-001` only after every statement above is proven on the same final commit.",
)


def _git(repo_root: Path, *arguments: str) -> str:
    try:
        result = subprocess.run(
            ["git", *arguments], cwd=repo_root, capture_output=True,
            text=True, check=True, shell=False)
    except (OSError, subprocess.CalledProcessError) as error:
        detail = error.stderr.strip() if isinstance(error, subprocess.CalledProcessError) else str(error)
        raise ReleaseContractError(f"git {' '.join(arguments)} failed: {detail}") from error
    return result.stdout.strip()


def validate_repository(
        repo_root: Path,
        expected_sha: str,
        final_mode: bool,
        plan_path: Path = DEFAULT_PLAN,
        backlog_path: Path = DEFAULT_BACKLOG,
        output_root: str = ".release-envelope",
        *,
        plan_content: bytes | None = None,
        backlog_content: bytes | None = None) -> dict[str, object]:
    repo_root = repo_root.absolute()
    if (not isinstance(expected_sha, str) or not COMMIT_PATTERN.fullmatch(expected_sha)):
        raise ReleaseContractError("expected SHA must be 40 lowercase hexadecimal characters")
    try:
        root_info = repo_root.lstat()
    except OSError as error:
        raise ReleaseContractError(f"repository root is unavailable: {error}") from error
    if repo_root.is_symlink() or not repo_root.is_dir():
        raise ReleaseContractError("repository root is unsafe")
    _git(repo_root, "fetch", "--prune", "origin")
    branch = _git(repo_root, "symbolic-ref", "--short", "HEAD")
    if branch != "main":
        raise ReleaseContractError(f"branch must be main, found {branch}")
    head = _git(repo_root, "rev-parse", "HEAD")
    if head != expected_sha:
        raise ReleaseContractError(f"HEAD mismatch: {head} != {expected_sha}")
    origin = _git(repo_root, "rev-parse", "refs/remotes/origin/main")
    if origin != expected_sha:
        raise ReleaseContractError(f"origin/main mismatch: {origin} != {expected_sha}")
    local_branches = sorted(filter(None, _git(
        repo_root, "for-each-ref", "--format=%(refname:short)", "refs/heads").splitlines()))
    if local_branches != ["main"]:
        raise ReleaseContractError(f"local branches must be exactly main: {local_branches}")
    remote_rows = _git(repo_root, "ls-remote", "--heads", "origin").splitlines()
    remote_heads: list[tuple[str, str]] = []
    for line in remote_rows:
        fields = line.split()
        if len(fields) != 2 or not COMMIT_PATTERN.fullmatch(fields[0]):
            raise ReleaseContractError("remote branches returned malformed identity")
        remote_heads.append((fields[1], fields[0]))
    if remote_heads != [("refs/heads/main", expected_sha)]:
        raise ReleaseContractError(f"remote branches must be exactly origin/main: {remote_heads}")
    remote_branches = ["origin/main"]
    dirty = []
    output_prefix = output_root.rstrip("/") + "/"
    for line in _git(repo_root, "status", "--porcelain=v1", "--untracked-files=all").splitlines():
        path = line[3:] if len(line) >= 4 else line
        if line.startswith("?? ") and path.startswith(output_prefix):
            continue
        dirty.append(line)
    if dirty:
        raise ReleaseContractError(f"worktree/index is not clean: {dirty[0]}")

    if plan_content is None:
        plan_content = read_regular_snapshot(
            repo_root, plan_path.as_posix(), maximum_bytes=16 * 1024 * 1024).content
    if backlog_content is None:
        backlog_content = read_regular_snapshot(
            repo_root, backlog_path.as_posix(), maximum_bytes=16 * 1024 * 1024).content
    try:
        plan_text = plan_content.decode("utf-8")
        backlog_text = backlog_content.decode("utf-8")
    except UnicodeDecodeError as error:
        raise ReleaseContractError("programme governance files must be UTF-8") from error
    ledger_section = plan_text.split("## Decisions ledger", 1)[0]
    ledger_ids: list[str] = []
    for line in ledger_section.splitlines():
        if re.match(r"^- \[[ x]\] ", line):
            ledger_ids.extend(re.findall(r"`([A-Z]+-\d+)`", line))
    if tuple(ledger_ids) != REQUIRED_LEDGER_IDS:
        raise ReleaseContractError("programme ledger identity is incomplete or reordered")
    final_section = plan_text.split("## Final acceptance", 1)
    final_rows = () if len(final_section) != 2 else tuple(
        match.group(1) for match in re.finditer(
            r"(?m)^- \[[ x]\] (.+)$", final_section[1]))
    if final_rows != REQUIRED_FINAL_ACCEPTANCE:
        raise ReleaseContractError("programme ledger final acceptance contract is incomplete")
    if not re.search(r"(?m)^\| [A-Z]+-\d+ \|", backlog_text):
        raise ReleaseContractError("programme backlog has no registered rows")
    active_backlog_ids = set(re.findall(
        r"(?m)^\|\s*([A-Z]+-\d+)\s*\|", backlog_text))
    if final_mode:
        if re.search(r"(?m)^- \[ \] ", plan_text):
            raise ReleaseContractError("selected completion plan contains an open checkbox")
        remaining_selected = sorted(
            active_backlog_ids.intersection(REQUIRED_LEDGER_IDS))
        if remaining_selected:
            raise ReleaseContractError(
                "selected backlog row remains: " + remaining_selected[0])
    return {
        "accepted": bool(final_mode),
        "branch": branch,
        "commit": head,
        "originMain": origin,
        "localBranches": local_branches,
        "remoteBranches": remote_branches,
        "worktreeClean": True,
    }


def validate_receipts(
        catalogue: GateCatalogue,
        receipts_dir: Path,
        repo_root: Path,
        expected_sha: str,
        catalogue_path: Path,
        *,
        source_snapshots: list[object] | None = None) -> list[object]:
    expected_directory = (repo_root / catalogue.output_root / "receipts").absolute()
    if receipts_dir.absolute() != expected_directory:
        raise ReleaseContractError(f"receipt directory must be {expected_directory}")
    try:
        directory_info = receipts_dir.lstat()
    except OSError as error:
        raise ReleaseContractError(f"receipt directory is unavailable: {error}") from error
    if receipts_dir.is_symlink() or not receipts_dir.is_dir():
        raise ReleaseContractError("receipt directory is unsafe")
    try:
        entries = list(receipts_dir.iterdir())
    except OSError as error:
        raise ReleaseContractError(f"cannot inspect receipt directory: {error}") from error
    expected_json = {f"{gate.id}.json" for gate in catalogue.commands}
    expected_logs = {f"{gate.id}.log" for gate in catalogue.commands}
    actual_names = {entry.name for entry in entries}
    unexpected_outputs = actual_names - expected_json - expected_logs
    if unexpected_outputs:
        raise ReleaseContractError(
            f"unexpected receipt output: {sorted(unexpected_outputs)[0]}")
    actual_json = actual_names & expected_json
    missing = expected_json - actual_json
    unexpected = actual_json - expected_json
    if missing:
        raise ReleaseContractError(f"missing receipt: {sorted(missing)[0]}")
    if unexpected:
        raise ReleaseContractError(f"unexpected receipt: {sorted(unexpected)[0]}")
    receipts = []
    for gate in sorted(catalogue.commands, key=lambda value: value.id):
        path = safe_regular_file(receipts_dir, f"{gate.id}.json")
        receipts.append(verify_receipt(
            path, catalogue, repo_root, expected_sha,
            catalogue_path=catalogue_path,
            source_snapshots=source_snapshots))
    return receipts


def validate_output_inventory(
        output_dir: Path, catalogue: GateCatalogue) -> None:
    try:
        info = output_dir.lstat()
    except FileNotFoundError:
        return
    except OSError as error:
        raise ReleaseContractError(f"release output is unavailable: {error}") from error
    if output_dir.is_symlink() or not stat.S_ISDIR(info.st_mode):
        raise ReleaseContractError("release output root is unsafe")
    allowed = {"receipts", *OUTPUT_NAMES}
    for entry in output_dir.iterdir():
        if entry.name not in allowed:
            raise ReleaseContractError(f"unexpected release output: {entry.name}")
        entry_info = entry.lstat()
        if entry.is_symlink():
            raise ReleaseContractError(f"unsafe release output: {entry.name}")
        if entry.name == "receipts":
            if not stat.S_ISDIR(entry_info.st_mode):
                raise ReleaseContractError("unsafe release output: receipts")
        elif not stat.S_ISREG(entry_info.st_mode) or entry_info.st_nlink != 1:
            raise ReleaseContractError(f"unsafe release output: {entry.name}")


def validate_evidence_manifest(
        catalogue: GateCatalogue,
        evidence_path: Path,
        repo_root: Path,
        expected_sha: str,
        *,
        manifest=None,
        source_snapshots: list[object] | None = None) -> list[dict[str, object]]:
    manifest = manifest or load_evidence_manifest(evidence_path)
    requirements = {row.id: row for row in catalogue.evidence}
    rows = {row.id: row for row in manifest.rows}
    missing = requirements.keys() - rows.keys()
    unexpected = rows.keys() - requirements.keys()
    if missing:
        raise ReleaseContractError(f"missing evidence row: {sorted(missing)[0]}")
    if unexpected:
        raise ReleaseContractError(f"unexpected evidence row: {sorted(unexpected)[0]}")
    validated = []
    for identifier in sorted(requirements):
        requirement = requirements[identifier]
        row = rows[identifier]
        if row.kind != requirement.kind or row.validator != requirement.validator:
            raise ReleaseContractError(f"evidence {identifier}: catalogue contract mismatch")
        snapshot = read_regular_snapshot(
            repo_root, row.path, maximum_bytes=256 * 1024 * 1024)
        if source_snapshots is not None:
            source_snapshots.append(snapshot)
        path = repo_root / row.path
        typed = validate_evidence(
            row, path, expected_sha, content=snapshot.content,
            raw_snapshots=source_snapshots, raw_root=repo_root,
            raw_prefix=PurePosixPath(row.path).parent)
        validated.append({
            "id": row.id,
            "kind": row.kind,
            "validator": row.validator,
            "path": row.path,
            "sha256": row.sha256,
            "size": row.size,
            "commit": expected_sha,
            "producer": list(row.producer),
            "result": _jsonable(typed),
            "limitations": list(row.limitations),
        })
    return validated


def _properties(path: Path, *, content: bytes | None = None) -> dict[str, str]:
    values: dict[str, str] = {}
    try:
        text = path.read_text(encoding="utf-8") if content is None else content.decode("utf-8")
    except UnicodeDecodeError as error:
        raise ReleaseContractError("gradle.properties: expected UTF-8") from error
    for number, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if "=" not in stripped:
            raise ReleaseContractError(f"gradle.properties:{number}: expected key=value")
        key, value = stripped.split("=", 1)
        if not key or not value or key in values:
            raise ReleaseContractError(f"gradle.properties:{number}: invalid or duplicate property")
        values[key] = value
    for required in ("mod_version", "minecraft_version", "loader_version"):
        if required not in values:
            raise ReleaseContractError(f"gradle.properties: missing {required}")
    return values


def _safe_zip(content: bytes, label: Path) -> zipfile.ZipFile:
    try:
        archive = zipfile.ZipFile(io.BytesIO(content), "r")
        bad = archive.testzip()
    except (OSError, zipfile.BadZipFile) as error:
        raise ReleaseContractError(f"artifact is not a valid ZIP/JAR: {label}: {error}") from error
    if bad is not None:
        archive.close()
        raise ReleaseContractError(f"artifact contains corrupt member: {bad}")
    for name in archive.namelist():
        candidate = Path(name)
        if candidate.is_absolute() or ".." in candidate.parts:
            archive.close()
            raise ReleaseContractError(f"artifact contains unsafe member: {name}")
    return archive


def validate_artifacts(
        repo_root: Path,
        catalogue: GateCatalogue,
        runtime_jar: Path,
        sources_jar: Path,
        *,
        source_snapshots: list[object] | None = None,
        properties_snapshot=None) -> list[dict[str, object]]:
    properties_snapshot = properties_snapshot or read_regular_snapshot(
        repo_root, "gradle.properties", maximum_bytes=1024 * 1024)
    if source_snapshots is not None and properties_snapshot not in source_snapshots:
        source_snapshots.append(properties_snapshot)
    properties = _properties(
        repo_root / "gradle.properties", content=properties_snapshot.content)
    version = properties["mod_version"]
    supplied = {"runtime-jar": runtime_jar.absolute(), "sources-jar": sources_jar.absolute()}
    requirements = {artifact.id: artifact for artifact in catalogue.artifacts}
    if set(requirements) != set(supplied):
        raise ReleaseContractError("catalogue must declare runtime-jar and sources-jar exactly")
    accepted = []
    for identifier in ("runtime-jar", "sources-jar"):
        expected_relative = requirements[identifier].path_template.replace("{version}", version)
        expected = (repo_root / expected_relative).absolute()
        if supplied[identifier] != expected:
            raise ReleaseContractError(
                f"artifact path mismatch for {identifier}: {supplied[identifier]} != {expected}")
        snapshot = read_regular_snapshot(
            repo_root, expected_relative, maximum_bytes=512 * 1024 * 1024)
        if source_snapshots is not None:
            source_snapshots.append(snapshot)
        with _safe_zip(snapshot.content, expected) as archive:
            names = set(archive.namelist())
            if identifier == "runtime-jar":
                if "fabric.mod.json" not in names or not any(name.endswith(".class") for name in names):
                    raise ReleaseContractError("runtime artifact lacks Fabric metadata or classes")
                try:
                    metadata = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
                except (UnicodeDecodeError, json.JSONDecodeError) as error:
                    raise ReleaseContractError("runtime artifact fabric.mod.json is invalid") from error
                if not isinstance(metadata, dict) or metadata.get("version") != version:
                    raise ReleaseContractError("runtime artifact version mismatch")
            elif not any(name.endswith(".java") for name in names):
                raise ReleaseContractError("sources artifact contains no Java sources")
        accepted.append({
            "id": identifier,
            "path": expected_relative,
            "size": snapshot.size,
            "sha256": snapshot.sha256,
        })
    return accepted


def _jsonable(value: object) -> object:
    if isinstance(value, Mapping):
        return {str(key): _jsonable(item) for key, item in sorted(value.items())}
    if isinstance(value, (tuple, list)):
        return [_jsonable(item) for item in value]
    if isinstance(value, (str, int, float, bool)) or value is None:
        return value
    raise ReleaseContractError(f"value is not JSON-compatible: {type(value).__name__}")


def _canonical(value: object) -> bytes:
    try:
        return (json.dumps(
            _jsonable(value), ensure_ascii=False, allow_nan=False, sort_keys=True,
            separators=(",", ":")) + "\n").encode("utf-8")
    except (TypeError, ValueError) as error:
        raise ReleaseContractError(f"cannot render canonical envelope JSON: {error}") from error


def _markdown(envelope: dict[str, object]) -> bytes:
    lines = [
        "# QA-001 Exact-Build Release Envelope",
        "",
        f"- Accepted: `{str(envelope['accepted']).lower()}`",
        f"- Repository: `{envelope['repository']}`",
        f"- Branch: `{envelope['branch']}`",
        f"- Commit: `{envelope['commit']}`",
        f"- Version: `{envelope['version']}`",
        f"- Minecraft: `{envelope['minecraftVersion']}`",
        f"- Fabric Loader: `{envelope['fabricLoaderVersion']}`",
        f"- Java: `{envelope['javaVersion']}`",
        "",
        "## Automated gates",
        "",
        "| Gate | Exit | Log SHA-256 |",
        "|---|---:|---|",
    ]
    for gate in envelope["gates"]:
        lines.append(f"| `{gate['id']}` | {gate['exitCode']} | `{gate['logSha256']}` |")
    lines += ["", "## Evidence", "", "| Evidence | Kind | SHA-256 |", "|---|---|---|"]
    for evidence in envelope["evidence"]:
        lines.append(f"| `{evidence['id']}` | `{evidence['kind']}` | `{evidence['sha256']}` |")
    lines += ["", "## Artifacts", ""]
    for artifact in envelope["artifacts"]:
        lines.append(f"- `{artifact['path']}` — `{artifact['sha256']}`")
    lines += ["", "## Accepted limitations", ""]
    limitations = envelope["limitations"]
    lines.extend(f"- {value}" for value in limitations)
    if not limitations:
        lines.append("- None.")
    lines += ["", "## Reproduction", ""]
    lines.extend(f"- `{' '.join(command)}`" for command in envelope["reproductionCommands"])
    lines += ["", "## Attestation verification", ""]
    lines.extend(f"- `{command}`" for command in envelope["attestationVerifyCommands"])
    return ("\n".join(lines) + "\n").encode("utf-8")


def _receipt_rows(receipts: list[object], repo_root: Path, receipts_dir: Path) -> list[dict[str, object]]:
    rows = []
    for receipt in receipts:
        receipt_path = receipts_dir / f"{receipt.gate_id}.json"
        rows.append({
            "id": receipt.gate_id,
            "commit": receipt.commit,
            "argv": list(receipt.argv),
            "environment": dict(receipt.environment),
            "startedAt": receipt.started_at,
            "endedAt": receipt.ended_at,
            "durationSeconds": receipt.duration_seconds,
            "exitCode": receipt.exit_code,
            "logPath": receipt.log_path,
            "logSize": receipt.log_size,
            "logSha256": receipt.log_sha256,
            "receiptPath": receipt_path.relative_to(repo_root).as_posix(),
            "receiptSha256": sha256_file(receipt_path),
            "catalogueSha256": receipt.catalogue_sha256,
        })
    return sorted(rows, key=lambda value: value["id"])


def _cleanup_outputs(output_dir: Path) -> None:
    for name in OUTPUT_NAMES:
        try:
            (output_dir / name).unlink()
        except FileNotFoundError:
            pass


def build_envelope(
        repo_root: Path,
        expected_sha: str,
        catalogue_path: Path,
        evidence_path: Path,
        receipts_dir: Path,
        runtime_jar: Path,
        sources_jar: Path,
        output_dir: Path,
        mode: str,
        created_at: str,
        github_run_id: str,
        github_run_attempt: str,
        plan_path: Path = DEFAULT_PLAN,
        backlog_path: Path = DEFAULT_BACKLOG) -> dict[str, Path]:
    if mode not in ("preflight", "final"):
        raise ReleaseContractError("mode must be preflight or final")
    expected_catalogue = (repo_root / "config/release/qa-001-gates.json").absolute()
    expected_evidence = (repo_root / "config/release/qa-001-evidence.json").absolute()
    expected_plan = (repo_root / DEFAULT_PLAN).absolute()
    expected_backlog = (repo_root / DEFAULT_BACKLOG).absolute()
    if catalogue_path.absolute() != expected_catalogue:
        raise ReleaseContractError(f"catalogue path must be {expected_catalogue}")
    if evidence_path.absolute() != expected_evidence:
        raise ReleaseContractError(f"evidence path must be {expected_evidence}")
    supplied_plan = (repo_root / plan_path).absolute() if not plan_path.is_absolute() else plan_path.absolute()
    supplied_backlog = ((repo_root / backlog_path).absolute()
                        if not backlog_path.is_absolute() else backlog_path.absolute())
    if supplied_plan != expected_plan:
        raise ReleaseContractError(f"plan path must be {expected_plan}")
    if supplied_backlog != expected_backlog:
        raise ReleaseContractError(f"backlog path must be {expected_backlog}")
    source_snapshots: list[object] = []
    catalogue_snapshot = read_regular_snapshot(
        repo_root, "config/release/qa-001-gates.json",
        maximum_bytes=16 * 1024 * 1024)
    evidence_index_snapshot = read_regular_snapshot(
        repo_root, "config/release/qa-001-evidence.json",
        maximum_bytes=16 * 1024 * 1024)
    properties_snapshot = read_regular_snapshot(
        repo_root, "gradle.properties", maximum_bytes=1024 * 1024)
    plan_snapshot = read_regular_snapshot(
        repo_root, DEFAULT_PLAN.as_posix(), maximum_bytes=16 * 1024 * 1024)
    backlog_snapshot = read_regular_snapshot(
        repo_root, DEFAULT_BACKLOG.as_posix(), maximum_bytes=16 * 1024 * 1024)
    source_snapshots.extend((
        catalogue_snapshot, evidence_index_snapshot, properties_snapshot,
        plan_snapshot, backlog_snapshot))
    catalogue = load_catalogue(
        catalogue_path, content=catalogue_snapshot.content)
    evidence_manifest = load_evidence_manifest(
        evidence_path, content=evidence_index_snapshot.content)
    expected_output = (repo_root / catalogue.output_root).absolute()
    if output_dir.absolute() != expected_output:
        raise ReleaseContractError(f"output directory must be {expected_output}")
    validate_output_inventory(output_dir, catalogue)
    repository = validate_repository(
        repo_root, expected_sha, mode == "final", plan_path, backlog_path,
        catalogue.output_root, plan_content=plan_snapshot.content,
        backlog_content=backlog_snapshot.content)
    receipts = validate_receipts(
        catalogue, receipts_dir, repo_root, expected_sha, catalogue_path,
        source_snapshots=source_snapshots)
    evidence = validate_evidence_manifest(
        catalogue, evidence_path, repo_root, expected_sha,
        manifest=evidence_manifest, source_snapshots=source_snapshots)
    artifacts = validate_artifacts(
        repo_root, catalogue, runtime_jar, sources_jar,
        source_snapshots=source_snapshots,
        properties_snapshot=properties_snapshot)
    if mode == "preflight":
        return {}
    if not re.fullmatch(r"\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d+)?Z", created_at):
        raise ReleaseContractError("created-at must be an explicit UTC timestamp")
    if not github_run_id.isdigit() or not github_run_attempt.isdigit():
        raise ReleaseContractError("final mode requires numeric GitHub run identity")
    properties = _properties(
        repo_root / "gradle.properties", content=properties_snapshot.content)
    receipt_rows = _receipt_rows(receipts, repo_root, receipts_dir)
    limitations = sorted({
        limitation for row in evidence for limitation in row["limitations"]})
    artifact_paths = {row["id"]: row["path"] for row in artifacts}
    json_relative = f"{catalogue.output_root}/release-envelope.json"
    markdown_relative = f"{catalogue.output_root}/release-envelope.md"
    subject_paths = (
        artifact_paths["runtime-jar"], artifact_paths["sources-jar"],
        json_relative, markdown_relative,
    )
    envelope = {
        "schemaVersion": 1,
        "accepted": True,
        "repository": catalogue.repository,
        "branch": repository["branch"],
        "commit": expected_sha,
        "originMain": repository["originMain"],
        "version": properties["mod_version"],
        "minecraftVersion": properties["minecraft_version"],
        "fabricLoaderVersion": properties["loader_version"],
        "javaVersion": "25",
        "createdAt": created_at,
        "github": {"runId": github_run_id, "runAttempt": github_run_attempt},
        "catalogueSha256": catalogue_snapshot.sha256,
        "evidenceIndexSha256": evidence_index_snapshot.sha256,
        "repositoryState": repository,
        "gates": receipt_rows,
        "evidence": evidence,
        "artifacts": artifacts,
        "limitations": limitations,
        "reproductionCommands": [list(gate.argv) for gate in catalogue.commands],
        "attestationVerifyCommands": [
            f"gh attestation verify {path} --repo {catalogue.repository}"
            for path in subject_paths
        ],
    }
    json_bytes = _canonical(envelope)
    markdown_bytes = _markdown(envelope)
    output_dir.mkdir(parents=True, exist_ok=True)
    json_path = output_dir / "release-envelope.json"
    markdown_path = output_dir / "release-envelope.md"
    checksums_path = output_dir / "SHA256SUMS"
    try:
        write_bytes_atomic(json_path, json_bytes)
        write_bytes_atomic(markdown_path, markdown_bytes)
        generated_snapshots = []
        for path, expected_content in (
                (json_path, json_bytes), (markdown_path, markdown_bytes)):
            snapshot = read_regular_snapshot(
                repo_root, path.relative_to(repo_root).as_posix(),
                maximum_bytes=64 * 1024 * 1024)
            if snapshot.content != expected_content:
                raise ReleaseContractError(
                    f"generated output changed: {path.name}")
            generated_snapshots.append(snapshot)
        checksum_paths = [
            json_path, markdown_path, runtime_jar, sources_jar,
            *(receipts_dir / f"{gate.id}.json" for gate in catalogue.commands),
            *(receipts_dir / f"{gate.id}.log" for gate in catalogue.commands),
        ]
        checksum_lines = []
        for path in sorted(checksum_paths, key=lambda value: value.relative_to(repo_root).as_posix()):
            relative = path.relative_to(repo_root).as_posix()
            checksum_lines.append(f"{sha256_file(path)}  {relative}")
        checksums_bytes = ("\n".join(checksum_lines) + "\n").encode("utf-8")
        write_bytes_atomic(checksums_path, checksums_bytes)
        checksums_snapshot = read_regular_snapshot(
            repo_root, checksums_path.relative_to(repo_root).as_posix(),
            maximum_bytes=64 * 1024 * 1024)
        if checksums_snapshot.content != checksums_bytes:
            raise ReleaseContractError("generated output changed: SHA256SUMS")
        generated_snapshots.append(checksums_snapshot)
        for artifact in artifacts:
            if sha256_file(repo_root / artifact["path"]) != artifact["sha256"]:
                raise ReleaseContractError(f"artifact changed during packaging: {artifact['id']}")
        for snapshot in source_snapshots:
            recheck_regular_snapshot(snapshot)
        for receipt in receipt_rows:
            if sha256_file(repo_root / receipt["receiptPath"]) != receipt["receiptSha256"]:
                raise ReleaseContractError(f"receipt changed during packaging: {receipt['id']}")
        validate_output_inventory(output_dir, catalogue)
        for snapshot in generated_snapshots:
            recheck_regular_snapshot(snapshot)
    except BaseException:
        _cleanup_outputs(output_dir)
        raise
    return {"json": json_path, "markdown": markdown_path, "checksums": checksums_path}


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Build the QA-001 exact-build release envelope.")
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--expected-sha", required=True)
    parser.add_argument("--catalogue", type=Path, required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--receipts", type=Path, required=True)
    parser.add_argument("--runtime-jar", type=Path, required=True)
    parser.add_argument("--sources-jar", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--mode", choices=("preflight", "final"), required=True)
    parser.add_argument("--created-at", default="")
    parser.add_argument("--github-run-id", default="")
    parser.add_argument("--github-run-attempt", default="")
    parser.add_argument("--plan", type=Path, default=DEFAULT_PLAN)
    parser.add_argument("--backlog", type=Path, default=DEFAULT_BACKLOG)
    return parser


def main(arguments: list[str] | None = None) -> int:
    options = _parser().parse_args(arguments)
    try:
        build_envelope(
            options.repo_root, options.expected_sha, options.catalogue,
            options.evidence, options.receipts, options.runtime_jar,
            options.sources_jar, options.output, options.mode,
            options.created_at, options.github_run_id, options.github_run_attempt,
            options.plan, options.backlog)
        return 0
    except (OSError, ReleaseContractError) as error:
        print(f"release_envelope: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
