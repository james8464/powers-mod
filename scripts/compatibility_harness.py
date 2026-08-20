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
import stat
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


def digest_descriptor(descriptor: int) -> tuple[int, str]:
    os.lseek(descriptor, 0, os.SEEK_SET)
    digest = hashlib.sha256()
    size = 0
    while chunk := os.read(descriptor, 1024 * 1024):
        size += len(chunk)
        digest.update(chunk)
    os.lseek(descriptor, 0, os.SEEK_SET)
    return size, digest.hexdigest()


def verify_artifact(artifact: dict[str, Any], cache: Path) -> int:
    path = cache / artifact["filename"]
    identifier = artifact["id"]
    try:
        descriptor = os.open(path, os.O_RDONLY | os.O_NOFOLLOW)
    except OSError as exception:
        raise CompatibilityError(f"{identifier}: missing cached artifact {path}")
    try:
        metadata = os.fstat(descriptor)
        if not stat.S_ISREG(metadata.st_mode):
            raise CompatibilityError(f"{identifier}: cached artifact is not a regular file")
        size, digest = digest_descriptor(descriptor)
        if size != artifact["size"]:
            raise CompatibilityError(f"{identifier}: size mismatch")
        if digest != artifact["sha256"]:
            raise CompatibilityError(f"{identifier}: SHA-256 mismatch")
        return descriptor
    except BaseException:
        os.close(descriptor)
        raise


def verify(manifest: dict[str, Any], cache: Path) -> None:
    for artifact in manifest["artifacts"]:
        descriptor = verify_artifact(artifact, cache)
        os.close(descriptor)


def safe_run_directory(run_dir: Path, allowed_root: Path) -> tuple[Path, str]:
    root_input = allowed_root.absolute()
    target_input = run_dir.absolute()
    if allowed_root.is_symlink() or not allowed_root.is_dir():
        raise CompatibilityError("unsafe run directory: allowed root must be a real directory")
    try:
        relative = target_input.relative_to(root_input)
    except ValueError as exception:
        raise CompatibilityError("unsafe run directory: outside allowed root") from exception
    if len(relative.parts) != 1:
        raise CompatibilityError("unsafe run directory: must be one owned child of allowed root")
    root = root_input.resolve()
    target = target_input.resolve(strict=False)
    try:
        target.relative_to(root)
    except ValueError as exception:
        raise CompatibilityError("unsafe run directory: symlink escape") from exception
    return root, relative.parts[0]


def open_owned_run_directory(run_dir: Path, allowed_root: Path) -> int:
    root, name = safe_run_directory(run_dir, allowed_root)
    try:
        root_descriptor = os.open(root, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
        try:
            try:
                os.mkdir(name, dir_fd=root_descriptor)
            except FileExistsError:
                pass
            return os.open(name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW,
                           dir_fd=root_descriptor)
        finally:
            os.close(root_descriptor)
    except OSError as exception:
        raise CompatibilityError(f"unsafe run directory: {exception}") from exception


def validate_owned_file(directory: int, name: str) -> bool:
    try:
        metadata = os.stat(name, dir_fd=directory, follow_symlinks=False)
    except FileNotFoundError:
        return False
    if not stat.S_ISREG(metadata.st_mode):
        raise CompatibilityError(f"unsafe owned path: {name}")
    return True


def open_owned_directory(directory: int, name: str) -> int:
    try:
        try:
            os.mkdir(name, dir_fd=directory)
        except FileExistsError:
            pass
        metadata = os.stat(name, dir_fd=directory, follow_symlinks=False)
        if not stat.S_ISDIR(metadata.st_mode):
            raise CompatibilityError(f"unsafe owned path: {name}")
        return os.open(name, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW,
                       dir_fd=directory)
    except OSError as exception:
        raise CompatibilityError(f"unsafe owned path: {name}: {exception}") from exception


def write_owned_text(directory: int, name: str, content: str) -> None:
    validate_owned_file(directory, name)
    try:
        descriptor = os.open(name, os.O_WRONLY | os.O_CREAT | os.O_TRUNC | os.O_NOFOLLOW,
                             0o644, dir_fd=directory)
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            output.write(content)
    except OSError as exception:
        raise CompatibilityError(f"unsafe owned path: {name}: {exception}") from exception


def stage_verified_artifact(artifact: dict[str, Any], source: int, mods: int) -> None:
    name = artifact["filename"]
    descriptor = -1
    try:
        descriptor = os.open(name, os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW,
                             0o644, dir_fd=mods)
        os.lseek(source, 0, os.SEEK_SET)
        digest = hashlib.sha256()
        size = 0
        while chunk := os.read(source, 1024 * 1024):
            size += len(chunk)
            digest.update(chunk)
            view = memoryview(chunk)
            while view:
                written = os.write(descriptor, view)
                view = view[written:]
        if size != artifact["size"] or digest.hexdigest() != artifact["sha256"]:
            raise CompatibilityError(f"{artifact['id']}: staged artifact mismatch")
    except BaseException:
        if descriptor >= 0:
            os.close(descriptor)
            descriptor = -1
        try:
            os.unlink(name, dir_fd=mods)
        except FileNotFoundError:
            pass
        raise
    finally:
        if descriptor >= 0:
            os.close(descriptor)


def assemble(manifest: dict[str, Any], cache: Path, profile: str,
             side: str, run_dir: Path, allowed_root: Path) -> list[dict[str, Any]]:
    if profile not in manifest["profiles"]:
        raise CompatibilityError(f"profile {profile}: not found")
    artifacts = artifact_map(manifest)
    selected = [artifacts[identifier] for identifier in manifest["profiles"][profile]
                if side in artifacts[identifier]["sides"]]
    sources: list[tuple[dict[str, Any], int]] = []
    staged: list[str] = []
    run_descriptor = -1
    mods_descriptor = -1
    try:
        for artifact in selected:
            sources.append((artifact, verify_artifact(artifact, cache)))
        run_descriptor = open_owned_run_directory(run_dir, allowed_root)
        # Validate every owned child before any deletion or write.
        validate_owned_file(run_descriptor, "eula.txt")
        validate_owned_file(run_descriptor, "server.properties")
        had_receipt = validate_owned_file(run_descriptor, "compatibility-receipt.json")
        mods_descriptor = open_owned_directory(run_descriptor, "mods")
        for name in os.listdir(mods_descriptor):
            if name.endswith(".jar"):
                validate_owned_file(mods_descriptor, name)
        for name in os.listdir(mods_descriptor):
            if name.endswith(".jar"):
                os.unlink(name, dir_fd=mods_descriptor)
        if had_receipt:
            os.unlink("compatibility-receipt.json", dir_fd=run_descriptor)
        for artifact, source in sources:
            stage_verified_artifact(artifact, source, mods_descriptor)
            staged.append(artifact["filename"])
        write_owned_text(run_descriptor, "eula.txt", "eula=true\n")
        if not validate_owned_file(run_descriptor, "server.properties"):
            write_owned_text(run_descriptor, "server.properties",
                             "online-mode=false\nlevel-name=world\n")
        receipt = {
            "schemaVersion": 1,
            "profile": profile,
            "side": side,
            "minecraftVersion": manifest["minecraftVersion"],
            "artifacts": [{field: artifact[field] for field in (
                "id", "projectId", "versionId", "version", "filename", "size", "sha256",
                "sourceUrl", "downloadUrl", "releaseChannel", "sides", "license",
                "redistribution", "retrieved",
            )} for artifact, _ in sources],
        }
        write_owned_text(run_descriptor, "compatibility-receipt.json",
                         json.dumps(receipt, indent=2) + "\n")
        return receipt["artifacts"]
    except BaseException:
        if mods_descriptor >= 0:
            for name in staged:
                try:
                    os.unlink(name, dir_fd=mods_descriptor)
                except FileNotFoundError:
                    pass
        raise
    finally:
        if mods_descriptor >= 0:
            os.close(mods_descriptor)
        if run_descriptor >= 0:
            os.close(run_descriptor)
        for _, descriptor in sources:
            os.close(descriptor)


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
