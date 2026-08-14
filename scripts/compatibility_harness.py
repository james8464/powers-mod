#!/usr/bin/env python3
"""Validates pinned compatibility artifacts and assembles isolated Fabric run dirs."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys
from typing import Any


REQUIRED_ARTIFACT_FIELDS = (
    "projectId", "versionId", "version", "releaseChannel", "sides",
    "sourceUrl", "downloadUrl", "filename", "size", "sha256", "license",
    "redistribution", "retrieved",
)


class CompatibilityError(ValueError):
    pass


def load_manifest(path: Path) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exception:
        raise CompatibilityError(f"manifest: {exception}") from exception
    if data.get("schemaVersion") != 1:
        raise CompatibilityError("manifest: schemaVersion must be 1")
    if data.get("minecraftVersion") != "26.2" or data.get("loader") != "fabric":
        raise CompatibilityError("manifest: expected Minecraft 26.2 with Fabric")
    artifacts = data.get("artifacts")
    profiles = data.get("profiles")
    if not isinstance(artifacts, list) or not isinstance(profiles, dict):
        raise CompatibilityError("manifest: artifacts and profiles are required")
    identifiers: set[str] = set()
    for artifact in artifacts:
        identifier = artifact.get("id", "artifact") if isinstance(artifact, dict) else "artifact"
        if not isinstance(artifact, dict):
            raise CompatibilityError(f"{identifier}: artifact must be an object")
        if identifier in identifiers:
            raise CompatibilityError(f"{identifier}: duplicate id")
        identifiers.add(identifier)
        for field in REQUIRED_ARTIFACT_FIELDS:
            if field not in artifact or artifact[field] in (None, "", []):
                raise CompatibilityError(f"{identifier}: missing {field}")
        filename = artifact["filename"]
        if Path(filename).name != filename or not filename.endswith(".jar"):
            raise CompatibilityError(f"{identifier}: unsafe filename")
        if artifact["sides"] not in (["client"], ["server"], ["client", "server"]):
            raise CompatibilityError(f"{identifier}: invalid sides")
        digest = artifact["sha256"]
        if not isinstance(digest, str) or len(digest) != 64:
            raise CompatibilityError(f"{identifier}: invalid sha256")
    for profile, members in profiles.items():
        if not isinstance(members, list) or any(member not in identifiers for member in members):
            raise CompatibilityError(f"profile {profile}: unknown artifact")
    return data


def artifact_map(manifest: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {artifact["id"]: artifact for artifact in manifest["artifacts"]}


def verify_artifact(artifact: dict[str, Any], cache: Path) -> Path:
    path = cache / artifact["filename"]
    identifier = artifact["id"]
    if not path.is_file():
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


def assemble(manifest: dict[str, Any], cache: Path, profile: str,
             side: str, run_dir: Path) -> list[str]:
    if profile not in manifest["profiles"]:
        raise CompatibilityError(f"profile {profile}: not found")
    artifacts = artifact_map(manifest)
    selected = [artifacts[identifier] for identifier in manifest["profiles"][profile]
                if side in artifacts[identifier]["sides"]]
    sources = [(artifact, verify_artifact(artifact, cache)) for artifact in selected]
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
        "artifacts": [artifact["id"] for artifact, _ in sources],
    }
    (run_dir / "compatibility-receipt.json").write_text(
        json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    return receipt["artifacts"]


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
    return result


def main(arguments: list[str] | None = None) -> int:
    options = parser().parse_args(arguments)
    try:
        manifest = load_manifest(options.manifest)
        if options.command == "verify":
            verify(manifest, options.cache)
            print(f"Verified {len(manifest['artifacts'])} pinned compatibility artifacts")
        else:
            selected = assemble(
                manifest, options.cache, options.profile, options.side, options.run_dir)
            print(f"Assembled {options.profile}/{options.side}: {', '.join(selected)}")
        return 0
    except CompatibilityError as exception:
        print(exception, file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
