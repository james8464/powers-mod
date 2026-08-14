#!/usr/bin/env python3
"""Validates pinned compatibility artifacts and assembles isolated Fabric run dirs."""

from __future__ import annotations

import argparse
from datetime import date
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
from typing import Any
from urllib.parse import urlsplit


REQUIRED_ARTIFACT_FIELDS = (
    "projectId", "versionId", "version", "releaseChannel", "sides",
    "sourceUrl", "downloadUrl", "filename", "size", "sha256", "license",
    "redistribution", "retrieved",
)


class CompatibilityError(ValueError):
    pass


SAFE_ID = re.compile(r"[a-z0-9_]{1,64}")
HEX_256 = re.compile(r"[0-9a-f]{64}")
UUID = re.compile(r"(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\b")
IPV4 = re.compile(r"(?<![\w.])(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}(?::\d{1,5})?")
# A bracketed timestamp such as ``[23:32:19]`` is not an IPv6 address. Require
# either IPv6 compression (``::``) or at least four hextets in bracketed form;
# unbracketed full addresses require all eight hextets.
IPV6 = re.compile(
    r"(?<!\w)(?:"
    r"\[(?=[0-9a-fA-F:]*::|(?:[0-9a-fA-F]{1,4}:){3})[0-9a-fA-F:]+\](?::\d{1,5})?"
    r"|(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
    r"|[0-9a-fA-F]*::[0-9a-fA-F:]*)"
    r"(?!\w)")
LOCALHOST = re.compile(r"(?i)\blocalhost(?::\d{1,5})?\b")
HOME_PATH = re.compile(r"/(?:Users|home)/[^/\s]+")
SEED = re.compile(r"(?i)(\bseed\s*[:=]\s*)-?\d+")
SECRET = re.compile(r"(?i)(\b(?:password|secret|token|api[_-]?key)\b\s*[:=]\s*)[^\s]+")


def load_manifest(path: Path) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise CompatibilityError(f"manifest: {exception}") from exception
    if not isinstance(data, dict):
        raise CompatibilityError("manifest: root must be an object")
    if type(data.get("schemaVersion")) is not int or data.get("schemaVersion") != 1:
        raise CompatibilityError("manifest: schemaVersion must be 1")
    if data.get("minecraftVersion") != "26.2" or data.get("loader") != "fabric":
        raise CompatibilityError("manifest: expected Minecraft 26.2 with Fabric")
    artifacts = data.get("artifacts")
    profiles = data.get("profiles")
    if not isinstance(artifacts, list) or not artifacts or not isinstance(profiles, dict) or not profiles:
        raise CompatibilityError("manifest: artifacts and profiles are required")
    identifiers: set[str] = set()
    for artifact in artifacts:
        identifier = artifact.get("id", "artifact") if isinstance(artifact, dict) else "artifact"
        if not isinstance(artifact, dict):
            raise CompatibilityError(f"{identifier}: artifact must be an object")
        if not isinstance(identifier, str) or not SAFE_ID.fullmatch(identifier):
            raise CompatibilityError(f"{identifier}: invalid id")
        if identifier in identifiers:
            raise CompatibilityError(f"{identifier}: duplicate id")
        identifiers.add(identifier)
        for field in REQUIRED_ARTIFACT_FIELDS:
            if field not in artifact or artifact[field] in (None, "", []):
                raise CompatibilityError(f"{identifier}: missing {field}")
        filename = artifact["filename"]
        if not isinstance(filename, str) or Path(filename).name != filename or not filename.endswith(".jar"):
            raise CompatibilityError(f"{identifier}: unsafe filename")
        if artifact["sides"] not in (["client"], ["server"], ["client", "server"]):
            raise CompatibilityError(f"{identifier}: invalid sides")
        digest = artifact["sha256"]
        if not isinstance(digest, str) or not HEX_256.fullmatch(digest):
            raise CompatibilityError(f"{identifier}: invalid sha256")
        if type(artifact["size"]) is not int or artifact["size"] <= 0:
            raise CompatibilityError(f"{identifier}: invalid size")
        for field in ("projectId", "versionId", "version", "license", "redistribution"):
            if not isinstance(artifact[field], str):
                raise CompatibilityError(f"{identifier}: invalid {field}")
        if artifact["releaseChannel"] not in ("release", "beta", "alpha"):
            raise CompatibilityError(f"{identifier}: invalid releaseChannel")
        try:
            date.fromisoformat(artifact["retrieved"])
        except (TypeError, ValueError) as exception:
            raise CompatibilityError(f"{identifier}: invalid retrieved") from exception
        for field in ("sourceUrl", "downloadUrl"):
            value = artifact[field]
            parsed = urlsplit(value) if isinstance(value, str) else None
            if (parsed is None or parsed.scheme != "https" or not parsed.hostname
                    or parsed.username is not None or parsed.password is not None
                    or parsed.fragment):
                raise CompatibilityError(f"{identifier}: invalid {field}")
        source = urlsplit(artifact["sourceUrl"])
        source_parts = source.path.strip("/").split("/")
        if (source.hostname not in ("modrinth.com", "www.modrinth.com")
                or len(source_parts) != 4 or source_parts[0] not in ("mod", "plugin")
                or source_parts[1] != artifact["projectId"] or source_parts[2] != "version"
                or source_parts[3] != artifact["versionId"]):
            raise CompatibilityError(f"{identifier}: invalid sourceUrl")
        download = urlsplit(artifact["downloadUrl"])
        download_parts = download.path.strip("/").split("/")
        if (download.hostname != "cdn.modrinth.com" or len(download_parts) < 5
                or download_parts[:4] != ["data", artifact["projectId"], "versions",
                                          artifact["versionId"]]):
            raise CompatibilityError(f"{identifier}: invalid downloadUrl")
    for profile, members in profiles.items():
        if not isinstance(profile, str) or not SAFE_ID.fullmatch(profile):
            raise CompatibilityError(f"profile {profile}: invalid profile")
        if (not isinstance(members, list) or not members or len(set(members)) != len(members)
                or any(not isinstance(member, str) or member not in identifiers for member in members)):
            raise CompatibilityError(f"profile {profile}: unknown artifact")
    return data


def artifact_map(manifest: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {artifact["id"]: artifact for artifact in manifest["artifacts"]}


def verify_artifact(artifact: dict[str, Any], cache: Path) -> Path:
    path = cache / artifact["filename"]
    identifier = artifact["id"]
    if path.is_symlink() or not path.is_file():
        raise CompatibilityError(f"{identifier}: missing cached artifact {path}")
    if path.stat().st_size != artifact["size"]:
        raise CompatibilityError(f"{identifier}: size mismatch")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != artifact["sha256"]:
        raise CompatibilityError(f"{identifier}: SHA-256 mismatch")
    return path


def verify(manifest: dict[str, Any], cache: Path) -> None:
    for artifact in manifest["artifacts"]:
        verify_artifact(artifact, cache)


def safe_run_directory(run_dir: Path, allowed_root: Path) -> Path:
    root_input = allowed_root.absolute()
    target_input = run_dir.absolute()
    if allowed_root.is_symlink() or not allowed_root.is_dir():
        raise CompatibilityError("unsafe run directory: allowed root must be a real directory")
    try:
        relative = target_input.relative_to(root_input)
    except ValueError as exception:
        raise CompatibilityError("unsafe run directory: outside allowed root") from exception
    if not relative.parts:
        raise CompatibilityError("unsafe run directory: cannot equal allowed root")
    cursor = root_input
    for part in relative.parts:
        cursor = cursor / part
        if cursor.is_symlink():
            raise CompatibilityError("unsafe run directory: symlink component")
    root = root_input.resolve()
    target = target_input.resolve(strict=False)
    try:
        target.relative_to(root)
    except ValueError as exception:
        raise CompatibilityError("unsafe run directory: symlink escape") from exception
    return target


def assemble(manifest: dict[str, Any], cache: Path, profile: str,
             side: str, run_dir: Path, allowed_root: Path) -> list[dict[str, Any]]:
    if profile not in manifest["profiles"]:
        raise CompatibilityError(f"profile {profile}: not found")
    artifacts = artifact_map(manifest)
    selected = [artifacts[identifier] for identifier in manifest["profiles"][profile]
                if side in artifacts[identifier]["sides"]]
    sources = [(artifact, verify_artifact(artifact, cache)) for artifact in selected]
    run_dir = safe_run_directory(run_dir, allowed_root)
    mods = run_dir / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    for existing in mods.glob("*.jar"):
        existing.unlink()
    for artifact, source in sources:
        shutil.copy2(source, mods / artifact["filename"])
    (run_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    properties = run_dir / "server.properties"
    if not properties.exists():
        properties.write_text("online-mode=false\nlevel-name=world\n", encoding="utf-8")
    receipt = {
        "schemaVersion": 1,
        "profile": profile,
        "side": side,
        "minecraftVersion": manifest["minecraftVersion"],
        "artifacts": [{field: artifact[field] for field in (
            "id", "projectId", "versionId", "version", "filename", "size", "sha256",
            "sourceUrl", "downloadUrl", "releaseChannel", "sides", "license", "redistribution",
            "retrieved",
        )} for artifact, _ in sources],
    }
    (run_dir / "compatibility-receipt.json").write_text(
        json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    return receipt["artifacts"]


def sanitize(source: Path, output: Path, identities: list[str]) -> None:
    if source.resolve() == output.resolve():
        raise CompatibilityError("sanitize: input and output must differ")
    try:
        content = source.read_text(encoding="utf-8")
    except OSError as exception:
        raise CompatibilityError(f"sanitize: {exception}") from exception
    content = UUID.sub("<uuid>", content)
    content = IPV4.sub("<network>", content)
    content = IPV6.sub("<network>", content)
    content = LOCALHOST.sub("<network>", content)
    content = HOME_PATH.sub("<home>", content)
    content = SEED.sub(r"\1<redacted>", content)
    content = SECRET.sub(r"\1<redacted>", content)
    for identity in sorted(set(identities), key=lambda value: (-len(value), value.casefold())):
        if not identity or len(identity) > 128 or any(character in identity for character in "\r\n"):
            raise CompatibilityError("sanitize: invalid identity")
        content = re.sub(re.escape(identity), "<player>", content, flags=re.IGNORECASE)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(content, encoding="utf-8")


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser()
    subcommands = result.add_subparsers(dest="command", required=True)
    for name in ("verify", "assemble"):
        command = subcommands.add_parser(name)
        command.add_argument("--manifest", type=Path, required=True)
        command.add_argument("--cache", type=Path, required=True)
        if name == "assemble":
            command.add_argument("--profile", required=True)
            command.add_argument("--side", choices=("client", "server"), required=True)
            command.add_argument("--run-dir", type=Path, required=True)
            command.add_argument("--allowed-root", type=Path, required=True)
    command = subcommands.add_parser("sanitize")
    command.add_argument("--input", type=Path, required=True)
    command.add_argument("--output", type=Path, required=True)
    command.add_argument("--identity", action="append", default=[])
    return result


def main(arguments: list[str] | None = None) -> int:
    options = parser().parse_args(arguments)
    try:
        if options.command == "sanitize":
            sanitize(options.input, options.output, options.identity)
            print(f"Sanitized {options.input} -> {options.output}")
            return 0
        manifest = load_manifest(options.manifest)
        if options.command == "verify":
            verify(manifest, options.cache)
            print(f"Verified {len(manifest['artifacts'])} pinned compatibility artifacts")
        else:
            selected = assemble(
                manifest, options.cache, options.profile, options.side, options.run_dir,
                options.allowed_root)
            print(f"Assembled {options.profile}/{options.side}: "
                  f"{', '.join(artifact['id'] for artifact in selected)}")
        return 0
    except CompatibilityError as exception:
        print(exception, file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
