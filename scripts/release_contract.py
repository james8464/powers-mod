#!/usr/bin/env python3

import hashlib
import ipaddress
import json
import os
import re
import secrets
import stat
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from types import MappingProxyType
from typing import Mapping
from urllib.parse import urlsplit


SCHEMA_VERSION = 1
HEAD_BINDING = "@HEAD"
ID_PATTERN = re.compile(r"[a-z0-9]+(?:[._-][a-z0-9]+)*\Z")
COMMIT_PATTERN = re.compile(r"[0-9a-f]{40}\Z")
SHA256_PATTERN = re.compile(r"[0-9a-f]{64}\Z")
REPOSITORY_PATTERN = re.compile(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+\Z")
ENVIRONMENT_ALLOWLIST = frozenset({
    "JAVA_HOME",
    "JAVA_VERSION",
    "GRADLE_USER_HOME",
    "POWERS_TEST_RUN_ID",
    "GITHUB_RUN_ID",
    "GITHUB_RUN_ATTEMPT",
    "GITHUB_SHA",
})
EVIDENCE_VALIDATORS = MappingProxyType({
    "junit": "junit-xml",
    "fabric-log": "fabric-log",
    "restart-soak": "restart-soak",
    "profiles": "real-client-profiles",
    "compatibility": "compatibility",
    "packet-fault": "packet-fault",
    "manual": "manual-review",
    "visual": "visual-review",
    "migration": "migration",
    "manifest": "manifest",
    "four-client": "four-client",
    "github-ci": "github-ci",
    "limitations": "limitations",
})
MAX_JSON_BYTES = 16 * 1024 * 1024


class ReleaseContractError(ValueError):
    pass


@dataclass(frozen=True, slots=True)
class Gate:
    id: str
    argv: tuple[str, ...]
    validator: str


@dataclass(frozen=True, slots=True)
class EvidenceRequirement:
    id: str
    kind: str
    validator: str


@dataclass(frozen=True, slots=True)
class ArtifactRequirement:
    id: str
    path_template: str


@dataclass(frozen=True, slots=True)
class GateCatalogue:
    schema_version: int
    repository: str
    output_root: str
    environment_allowlist: tuple[str, ...]
    commands: tuple[Gate, ...]
    evidence: tuple[EvidenceRequirement, ...]
    artifacts: tuple[ArtifactRequirement, ...]
    source_urls: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class CommandReceipt:
    gate_id: str
    commit: str
    argv: tuple[str, ...]
    environment: Mapping[str, str]
    started_at: str
    ended_at: str
    duration_seconds: float
    exit_code: int
    log_path: str
    log_size: int
    log_sha256: str
    catalogue_sha256: str


@dataclass(frozen=True, slots=True)
class EvidenceRow:
    id: str
    kind: str
    validator: str
    path: str
    sha256: str
    size: int
    commit: str
    producer: tuple[str, ...]
    result: Mapping[str, object]
    limitations: tuple[str, ...]


@dataclass(frozen=True, slots=True)
class EvidenceManifest:
    schema_version: int
    commit: str
    rows: tuple[EvidenceRow, ...]


def _is_plain_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def _object(value: object, label: str) -> dict[str, object]:
    if not isinstance(value, dict) or not all(isinstance(key, str) for key in value):
        raise ReleaseContractError(f"{label}: expected object")
    return value


def _exact_keys(value: dict[str, object], required: set[str], optional: set[str], label: str) -> None:
    missing = required - value.keys()
    unknown = value.keys() - required - optional
    if missing:
        raise ReleaseContractError(f"{label}: missing {sorted(missing)[0]}")
    if unknown:
        raise ReleaseContractError(f"{label}: unknown catalogue field {sorted(unknown)[0]}")


def _identifier(value: object, label: str) -> str:
    if not isinstance(value, str) or not ID_PATTERN.fullmatch(value):
        raise ReleaseContractError(f"{label}: invalid identifier")
    return value


def _commit(value: object, label: str, *, allow_head: bool = False) -> str:
    if value == HEAD_BINDING and allow_head:
        return HEAD_BINDING
    if not isinstance(value, str) or not COMMIT_PATTERN.fullmatch(value):
        raise ReleaseContractError(f"{label}: invalid commit")
    return value


def _sha256(value: object, label: str) -> str:
    if not isinstance(value, str) or not SHA256_PATTERN.fullmatch(value):
        raise ReleaseContractError(f"{label}: invalid sha256")
    return value


def _relative_path(value: object, label: str, *, template: bool = False) -> str:
    if not isinstance(value, str) or not value or "\\" in value or "\x00" in value:
        raise ReleaseContractError(f"{label}: invalid path")
    candidate = PurePosixPath(value)
    if candidate.is_absolute() or not candidate.parts or any(part in ("", ".", "..") for part in candidate.parts):
        raise ReleaseContractError(f"{label}: invalid path")
    if not template and ("{" in value or "}" in value):
        raise ReleaseContractError(f"{label}: invalid path")
    if template:
        scrubbed = value.replace("{version}", "1.0.0")
        if "{" in scrubbed or "}" in scrubbed:
            raise ReleaseContractError(f"{label}: invalid pathTemplate")
    return value


def _string_vector(value: object, label: str) -> tuple[str, ...]:
    if not isinstance(value, list) or not value:
        raise ReleaseContractError(f"{label}: expected nonempty argv")
    if not all(isinstance(item, str) and item and "\x00" not in item for item in value):
        raise ReleaseContractError(f"{label}: expected string argv")
    return tuple(value)


def _read_json(path: Path) -> object:
    try:
        with path.open("rb") as source:
            data = source.read(MAX_JSON_BYTES + 1)
    except (OSError, ValueError) as error:
        raise ReleaseContractError(f"{path}: cannot read JSON: {error}") from error
    if len(data) > MAX_JSON_BYTES:
        raise ReleaseContractError(f"{path}: JSON exceeds {MAX_JSON_BYTES} bytes")
    try:
        return json.loads(data.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ReleaseContractError(f"{path}: invalid JSON: {error}") from error


def _source_url(value: object) -> str:
    if not isinstance(value, str):
        raise ReleaseContractError("sourceUrl: expected string")
    parsed = urlsplit(value)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
        raise ReleaseContractError("sourceUrl: HTTPS without credentials required")
    path_parts = {part.lower() for part in PurePosixPath(parsed.path).parts}
    if "latest" in path_parts or parsed.query or parsed.fragment:
        raise ReleaseContractError("sourceUrl: mutable URL is forbidden")
    return value


def load_catalogue(path: Path) -> GateCatalogue:
    data = _object(_read_json(path), "catalogue")
    required = {
        "schemaVersion", "repository", "outputRoot", "environmentAllowlist",
        "commands", "evidence", "artifacts",
    }
    _exact_keys(data, required, {"sourceUrls"}, "catalogue")
    if not _is_plain_int(data["schemaVersion"]) or data["schemaVersion"] != SCHEMA_VERSION:
        raise ReleaseContractError("catalogue schemaVersion: expected 1")
    repository = data["repository"]
    if not isinstance(repository, str) or not REPOSITORY_PATTERN.fullmatch(repository):
        raise ReleaseContractError("catalogue repository: expected owner/name")
    output_root = _relative_path(data["outputRoot"], "catalogue outputRoot")

    raw_environment = data["environmentAllowlist"]
    if not isinstance(raw_environment, list) or not all(isinstance(item, str) for item in raw_environment):
        raise ReleaseContractError("catalogue environment: expected string list")
    environment = tuple(raw_environment)
    if len(set(environment)) != len(environment) or not set(environment) <= ENVIRONMENT_ALLOWLIST:
        raise ReleaseContractError("catalogue environment: unknown or duplicate name")

    raw_commands = data["commands"]
    if not isinstance(raw_commands, list) or not raw_commands:
        raise ReleaseContractError("catalogue commands: expected nonempty list")
    commands: list[Gate] = []
    command_ids: set[str] = set()
    for index, raw in enumerate(raw_commands):
        item = _object(raw, f"command[{index}]")
        _exact_keys(item, {"id", "argv", "validator"}, set(), f"command[{index}]")
        identifier = _identifier(item["id"], "command id")
        if identifier in command_ids:
            raise ReleaseContractError(f"duplicate command id: {identifier}")
        command_ids.add(identifier)
        if item["validator"] != "command-receipt":
            raise ReleaseContractError(f"command {identifier}: unknown validator")
        commands.append(Gate(identifier, _string_vector(item["argv"], f"command {identifier} argv"), "command-receipt"))

    raw_evidence = data["evidence"]
    if not isinstance(raw_evidence, list) or not raw_evidence:
        raise ReleaseContractError("catalogue evidence: expected nonempty list")
    evidence: list[EvidenceRequirement] = []
    evidence_ids: set[str] = set()
    for index, raw in enumerate(raw_evidence):
        item = _object(raw, f"evidence[{index}]")
        _exact_keys(item, {"id", "kind", "validator"}, set(), f"evidence[{index}]")
        identifier = _identifier(item["id"], "evidence id")
        if identifier in evidence_ids:
            raise ReleaseContractError(f"duplicate evidence id: {identifier}")
        evidence_ids.add(identifier)
        kind = item["kind"]
        if not isinstance(kind, str) or kind not in EVIDENCE_VALIDATORS:
            raise ReleaseContractError(f"evidence kind: {kind!r}")
        expected_validator = EVIDENCE_VALIDATORS[kind]
        if item["validator"] != expected_validator:
            raise ReleaseContractError(f"evidence validator: expected {expected_validator}")
        evidence.append(EvidenceRequirement(identifier, kind, expected_validator))

    raw_artifacts = data["artifacts"]
    if not isinstance(raw_artifacts, list) or not raw_artifacts:
        raise ReleaseContractError("catalogue artifacts: expected nonempty list")
    artifacts: list[ArtifactRequirement] = []
    artifact_ids: set[str] = set()
    for index, raw in enumerate(raw_artifacts):
        item = _object(raw, f"artifact[{index}]")
        _exact_keys(item, {"id", "pathTemplate"}, set(), f"artifact[{index}]")
        identifier = _identifier(item["id"], "artifact id")
        if identifier in artifact_ids:
            raise ReleaseContractError(f"duplicate artifact id: {identifier}")
        artifact_ids.add(identifier)
        path_template = _relative_path(item["pathTemplate"], "artifact pathTemplate", template=True)
        artifacts.append(ArtifactRequirement(identifier, path_template))

    raw_urls = data.get("sourceUrls", [])
    if not isinstance(raw_urls, list):
        raise ReleaseContractError("sourceUrls: expected list")
    source_urls = tuple(_source_url(value) for value in raw_urls)
    return GateCatalogue(
        SCHEMA_VERSION, repository, output_root, tuple(environment), tuple(commands),
        tuple(evidence), tuple(artifacts), source_urls)


def load_evidence_manifest(path: Path) -> EvidenceManifest:
    data = _object(_read_json(path), "evidence manifest")
    _exact_keys(data, {"schemaVersion", "commit", "rows"}, set(), "evidence manifest")
    if not _is_plain_int(data["schemaVersion"]) or data["schemaVersion"] != SCHEMA_VERSION:
        raise ReleaseContractError("evidence manifest schemaVersion: expected 1")
    commit = _commit(data["commit"], "evidence manifest commit", allow_head=True)
    if commit != HEAD_BINDING:
        raise ReleaseContractError("evidence manifest commit must use @HEAD")
    raw_rows = data["rows"]
    if not isinstance(raw_rows, list) or not raw_rows:
        raise ReleaseContractError("evidence manifest rows: expected nonempty list")
    rows: list[EvidenceRow] = []
    identifiers: set[str] = set()
    required = {
        "id", "kind", "validator", "path", "sha256", "size", "commit",
        "producer", "result", "limitations",
    }
    for index, raw in enumerate(raw_rows):
        item = _object(raw, f"evidence row[{index}]")
        _exact_keys(item, required, set(), f"evidence row[{index}]")
        identifier = _identifier(item["id"], "evidence id")
        if identifier in identifiers:
            raise ReleaseContractError(f"duplicate evidence id: {identifier}")
        identifiers.add(identifier)
        kind = item["kind"]
        if not isinstance(kind, str) or kind not in EVIDENCE_VALIDATORS:
            raise ReleaseContractError(f"evidence kind: {kind!r}")
        validator = item["validator"]
        if validator != EVIDENCE_VALIDATORS[kind]:
            raise ReleaseContractError(f"evidence validator: expected {EVIDENCE_VALIDATORS[kind]}")
        row_commit = _commit(
            item["commit"], f"evidence {identifier} commit", allow_head=True)
        if row_commit != commit:
            raise ReleaseContractError(f"evidence {identifier} commit: manifest mismatch")
        if not _is_plain_int(item["size"]) or item["size"] <= 0:
            raise ReleaseContractError(f"evidence {identifier} size: expected positive integer")
        result = _object(item["result"], f"evidence {identifier} result")
        limitations = item["limitations"]
        if not isinstance(limitations, list) or not all(isinstance(value, str) and value.strip() for value in limitations):
            raise ReleaseContractError(f"evidence {identifier} limitations: expected nonblank string list")
        rows.append(EvidenceRow(
            identifier,
            kind,
            validator,
            _relative_path(item["path"], f"evidence {identifier} path"),
            _sha256(item["sha256"], f"evidence {identifier} sha256"),
            item["size"],
            row_commit,
            _string_vector(item["producer"], f"evidence {identifier} producer"),
            MappingProxyType(dict(result)),
            tuple(limitations),
        ))
    return EvidenceManifest(SCHEMA_VERSION, commit, tuple(rows))


def _safe_directory(path: Path) -> Path:
    absolute = path.absolute()
    try:
        supplied = absolute.lstat()
        resolved = absolute.resolve(strict=True)
        canonical = resolved.lstat()
    except OSError as error:
        raise ReleaseContractError(f"unsafe directory {absolute}: {error}") from error
    if (stat.S_ISLNK(supplied.st_mode) or not stat.S_ISDIR(supplied.st_mode)
            or stat.S_ISLNK(canonical.st_mode) or not stat.S_ISDIR(canonical.st_mode)):
        raise ReleaseContractError(f"unsafe directory {absolute}")
    return absolute


def safe_regular_file(root: Path, relative: str) -> Path:
    root = _safe_directory(root)
    clean = _relative_path(relative, "file")
    parts = PurePosixPath(clean).parts
    current = root
    for part in parts[:-1]:
        current /= part
        try:
            info = current.lstat()
        except OSError as error:
            raise ReleaseContractError(f"unsafe path {relative}: {error}") from error
        if stat.S_ISLNK(info.st_mode) or not stat.S_ISDIR(info.st_mode):
            raise ReleaseContractError(f"unsafe path {relative}")
    target = current / parts[-1]
    try:
        info = target.lstat()
    except OSError as error:
        raise ReleaseContractError(f"unsafe file {relative}: {error}") from error
    if stat.S_ISLNK(info.st_mode) or not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
        raise ReleaseContractError(f"unsafe file {relative}")
    return target


def sha256_file(path: Path) -> str:
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    descriptor = os.open(path, flags)
    try:
        before = os.fstat(descriptor)
        if not stat.S_ISREG(before.st_mode) or before.st_nlink != 1:
            raise ReleaseContractError(f"unsafe file {path}")
        digest = hashlib.sha256()
        while True:
            block = os.read(descriptor, 1024 * 1024)
            if not block:
                break
            digest.update(block)
        after = os.fstat(descriptor)
        current = path.lstat()
        if ((before.st_dev, before.st_ino, before.st_size, before.st_mtime_ns)
                != (after.st_dev, after.st_ino, after.st_size, after.st_mtime_ns)
                or (after.st_dev, after.st_ino) != (current.st_dev, current.st_ino)):
            raise ReleaseContractError(f"file changed while hashing: {path}")
        return digest.hexdigest()
    finally:
        os.close(descriptor)


def write_bytes_atomic(path: Path, data: bytes) -> str:
    if not isinstance(data, bytes):
        raise TypeError("data must be bytes")
    parent = _safe_directory(path.parent)
    if path.name in ("", ".", "..") or "/" in path.name or "\x00" in path.name:
        raise ReleaseContractError(f"unsafe output name: {path.name!r}")
    directory_descriptor = os.open(
        parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0))
    temporary = f".{path.name}.{secrets.token_hex(12)}.tmp"
    file_descriptor = None
    try:
        file_descriptor = os.open(
            temporary,
            os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0),
            0o600,
            dir_fd=directory_descriptor,
        )
        view = memoryview(data)
        while view:
            written = os.write(file_descriptor, view)
            if written <= 0:
                raise OSError("short atomic write")
            view = view[written:]
        os.fsync(file_descriptor)
        os.close(file_descriptor)
        file_descriptor = None
        os.replace(
            temporary, path.name,
            src_dir_fd=directory_descriptor, dst_dir_fd=directory_descriptor)
        os.fsync(directory_descriptor)
    except BaseException:
        if file_descriptor is not None:
            os.close(file_descriptor)
        try:
            os.unlink(temporary, dir_fd=directory_descriptor)
        except FileNotFoundError:
            pass
        raise
    finally:
        os.close(directory_descriptor)
    expected = hashlib.sha256(data).hexdigest()
    actual = sha256_file(parent / path.name)
    if actual != expected:
        raise ReleaseContractError(f"atomic output rehash mismatch: {path}")
    return actual


def write_json_atomic(path: Path, value: object) -> str:
    try:
        encoded = (json.dumps(
            value, ensure_ascii=False, allow_nan=False, sort_keys=True,
            separators=(",", ":")) + "\n").encode("utf-8")
    except (TypeError, ValueError) as error:
        raise ReleaseContractError(f"cannot encode canonical JSON: {error}") from error
    return write_bytes_atomic(path, encoded)


_UUID = re.compile(r"(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b")
_BEARER = re.compile(r"(?i)\b(?:authorization\s*:\s*bearer|bearer)\s+[A-Za-z0-9._~+/=-]{8,}")
_TOKEN = re.compile(r"(?i)\b(?:token|password|passwd|secret)\s*[=:]\s*\S{8,}")
_CREDENTIAL_URL = re.compile(r"https?://[^\s/@:]+:[^\s/@]+@")
_ABSOLUTE_PATH = re.compile(r"(?<![A-Za-z0-9_.-])/(?:Users|home|private|var/folders|tmp)/[^\s]+")
_IPV4 = re.compile(r"(?<![0-9.])(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?![0-9.])")
_ALLOWED_IPV4_NETWORKS = tuple(ipaddress.ip_network(value) for value in (
    "0.0.0.0/32", "10.0.0.0/8", "127.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"))


def validate_packaged_text(text: str) -> None:
    if not isinstance(text, str):
        raise ReleaseContractError("packaged text must be a string")
    for label, pattern in (
            ("bearer credential", _BEARER),
            ("credential", _TOKEN),
            ("credentialed URL", _CREDENTIAL_URL),
            ("UUID", _UUID),
            ("unowned absolute path", _ABSOLUTE_PATH)):
        if pattern.search(text):
            raise ReleaseContractError(f"packaged text contains {label}")
    for match in _IPV4.finditer(text):
        try:
            address = ipaddress.ip_address(match.group(0))
        except ValueError:
            raise ReleaseContractError("packaged text contains malformed IP address")
        if not any(address in network for network in _ALLOWED_IPV4_NETWORKS):
            raise ReleaseContractError("packaged text contains public IP address")
