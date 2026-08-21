#!/usr/bin/env python3

import hashlib
import importlib.util
import io
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
HARNESS = ROOT / "scripts/compatibility_harness.py"
SPEC = importlib.util.spec_from_file_location("compatibility_harness", HARNESS)
HARNESS_MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(HARNESS_MODULE)


class CompatibilityHarnessTest(unittest.TestCase):
    def fixture(self, directory: Path, content: bytes = b"pinned jar") -> tuple[Path, Path]:
        cache = directory / "cache"
        cache.mkdir()
        manifest = directory / "manifest.json"
        manifest.write_text(json.dumps({
            "schemaVersion": 1,
            "minecraftVersion": "26.2",
            "loader": "fabric",
            "profiles": {"renderer": ["fixture"]},
            "artifacts": [{
                "id": "fixture",
                "projectId": "official-project",
                "versionId": "official-version",
                "version": "1.0.0",
                "releaseChannel": "release",
                "sides": ["client"],
                "sourceUrl": "https://modrinth.com/mod/official-project/version/official-version",
                "downloadUrl": "https://cdn.modrinth.com/data/official-project/versions/official-version/fixture.jar",
                "filename": "fixture.jar",
                "size": len(content),
                "sha256": hashlib.sha256(content).hexdigest(),
                "license": "Test-only",
                "redistribution": "Do not redistribute",
                "retrieved": "2026-08-14",
            }],
        }), encoding="utf-8")
        return manifest, cache

    def run_harness(self, *arguments: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, "-B", str(HARNESS), *arguments],
            cwd=ROOT, capture_output=True, text=True, check=False)

    def test_fetch_acquires_only_exact_pinned_bytes_and_reuses_verified_cache(self):
        class Response(io.BytesIO):
            status = 200

            def __init__(self, content: bytes, url: str):
                super().__init__(content)
                self.url = url

            def geturl(self) -> str:
                return self.url

        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest_path, cache = self.fixture(directory)
            manifest = HARNESS_MODULE.load_manifest(manifest_path)
            url = manifest["artifacts"][0]["downloadUrl"]
            opens: list[str] = []

            def open_exact(request):
                opens.append(request.full_url)
                return Response(b"pinned jar", request.full_url)

            fetched = HARNESS_MODULE.fetch(manifest, cache, opener=open_exact)
            self.assertEqual(["fixture"], fetched)
            self.assertEqual([url], opens)
            self.assertEqual(b"pinned jar", (cache / "fixture.jar").read_bytes())
            HARNESS_MODULE.verify(manifest, cache)

            reused = HARNESS_MODULE.fetch(
                manifest, cache,
                opener=lambda _request: self.fail("verified cache must not redownload"))
            self.assertEqual([], reused)

    def test_fetch_rejects_wrong_bytes_without_replacing_existing_cache(self):
        class Response(io.BytesIO):
            status = 200

            def __init__(self, content: bytes, url: str):
                super().__init__(content)
                self.url = url

            def geturl(self) -> str:
                return self.url

        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest_path, cache = self.fixture(directory)
            target = cache / "fixture.jar"
            target.write_bytes(b"old-invalid")
            manifest = HARNESS_MODULE.load_manifest(manifest_path)
            with self.assertRaisesRegex(
                    HARNESS_MODULE.CompatibilityError,
                    "downloaded artifact mismatch"):
                HARNESS_MODULE.fetch(
                    manifest, cache,
                    opener=lambda request: Response(b"wrong bytes", request.full_url))
            self.assertEqual(b"old-invalid", target.read_bytes())
            self.assertEqual([target], list(cache.iterdir()))

    def test_fetch_rejects_symlinked_intermediate_cache_parent(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest_path, unused_cache = self.fixture(directory)
            unused_cache.rmdir()
            repository = directory / "repo"
            repository.mkdir()
            external = directory / "external"
            external.mkdir()
            (repository / ".compatibility-cache").symlink_to(
                external, target_is_directory=True)
            manifest = HARNESS_MODULE.load_manifest(manifest_path)

            with self.assertRaisesRegex(
                    HARNESS_MODULE.CompatibilityError, "unsafe cache directory"):
                HARNESS_MODULE.fetch(
                    manifest, repository / ".compatibility-cache/net-011",
                    allowed_root=repository,
                    opener=lambda _request: self.fail("unsafe cache must not download"))

            self.assertEqual([], list(external.iterdir()))

    def test_verify_rejects_hash_mismatched_cached_artifact(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            manifest, cache = self.fixture(Path(raw_directory))
            (cache / "fixture.jar").write_bytes(b"tampered!!")

            result = self.run_harness(
                "verify", "--manifest", str(manifest), "--cache", str(cache))

            self.assertEqual(1, result.returncode)
            self.assertIn("fixture: SHA-256 mismatch", result.stderr)

    def test_assemble_copies_only_artifacts_valid_for_requested_side(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest, cache = self.fixture(directory)
            (cache / "fixture.jar").write_bytes(b"pinned jar")
            run_directory = directory / "isolated-client"

            result = self.run_harness(
                "assemble", "--manifest", str(manifest), "--cache", str(cache),
                "--profile", "renderer", "--side", "client",
                "--run-dir", str(run_directory), "--allowed-root", str(directory))

            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual(b"pinned jar", (run_directory / "mods" / "fixture.jar").read_bytes())
            self.assertEqual("eula=true\n", (run_directory / "eula.txt").read_text(encoding="utf-8"))
            receipt = json.loads((run_directory / "compatibility-receipt.json").read_text())
            self.assertEqual("official-project", receipt["artifacts"][0]["projectId"])
            self.assertEqual("official-version", receipt["artifacts"][0]["versionId"])
            self.assertEqual(len(b"pinned jar"), receipt["artifacts"][0]["size"])
            self.assertEqual(hashlib.sha256(b"pinned jar").hexdigest(),
                             receipt["artifacts"][0]["sha256"])

    def test_manifest_rejects_missing_authoritative_source_metadata(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest, cache = self.fixture(directory)
            data = json.loads(manifest.read_text(encoding="utf-8"))
            del data["artifacts"][0]["sourceUrl"]
            manifest.write_text(json.dumps(data), encoding="utf-8")

            result = self.run_harness(
                "verify", "--manifest", str(manifest), "--cache", str(cache))

            self.assertEqual(1, result.returncode)
            self.assertIn("fixture: missing sourceUrl", result.stderr)

    def test_manifest_rejects_malformed_types_urls_hashes_sizes_and_profiles(self):
        invalid = (
            ("id", "../fixture", "invalid id"),
            ("size", True, "invalid size"),
            ("size", 0, "invalid size"),
            ("sha256", "G" * 64, "invalid sha256"),
            ("sourceUrl", "http://modrinth.com/mod/official-project/version/official-version", "invalid sourceUrl"),
            ("sourceUrl", "https://modrinth.com/mod/wrong/version/official-version", "invalid sourceUrl"),
            ("downloadUrl", "https://user:pass@cdn.modrinth.com/data/official-project/versions/official-version/file.jar", "invalid downloadUrl"),
            ("downloadUrl", "https://cdn.modrinth.com/data/official-project/versions/wrong/file.jar", "invalid downloadUrl"),
        )
        for field, value, expected in invalid:
            with self.subTest(field=field, value=value), tempfile.TemporaryDirectory() as raw_directory:
                directory = Path(raw_directory)
                manifest, cache = self.fixture(directory)
                data = json.loads(manifest.read_text(encoding="utf-8"))
                data["artifacts"][0][field] = value
                manifest.write_text(json.dumps(data), encoding="utf-8")
                result = self.run_harness("verify", "--manifest", str(manifest), "--cache", str(cache))
                self.assertEqual(1, result.returncode)
                self.assertIn(expected, result.stderr)

        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest, cache = self.fixture(directory)
            data = json.loads(manifest.read_text(encoding="utf-8"))
            data["profiles"] = {"../escape": ["fixture", "fixture"]}
            manifest.write_text(json.dumps(data), encoding="utf-8")
            result = self.run_harness("verify", "--manifest", str(manifest), "--cache", str(cache))
            self.assertEqual(1, result.returncode)
            self.assertIn("invalid profile", result.stderr)

    def test_assemble_rejects_outside_equal_and_symlinked_run_directories(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest, cache = self.fixture(directory)
            (cache / "fixture.jar").write_bytes(b"pinned jar")
            allowed = directory / "runs"
            allowed.mkdir()
            outside = directory / "outside"
            outside.mkdir()
            symlink = allowed / "linked"
            symlink.symlink_to(outside, target_is_directory=True)
            for target in (directory / "escape", allowed, symlink / "child"):
                with self.subTest(target=target):
                    result = self.run_harness(
                        "assemble", "--manifest", str(manifest), "--cache", str(cache),
                        "--profile", "renderer", "--side", "client",
                        "--run-dir", str(target), "--allowed-root", str(allowed))
                    self.assertEqual(1, result.returncode)
                    self.assertIn("unsafe run directory", result.stderr)

    def test_assemble_rejects_symlinked_owned_children_without_touching_targets(self):
        for child, target_content in (
                ("mods", b"external jar"),
                ("eula.txt", b"external eula"),
                ("server.properties", b"external properties"),
                ("compatibility-receipt.json", b"external receipt")):
            with self.subTest(child=child), tempfile.TemporaryDirectory() as raw_directory:
                directory = Path(raw_directory)
                manifest, cache = self.fixture(directory)
                (cache / "fixture.jar").write_bytes(b"pinned jar")
                run_directory = directory / "isolated-client"
                run_directory.mkdir()
                external = directory / "external"
                if child == "mods":
                    external.mkdir()
                    target = external / "existing.jar"
                    target.write_bytes(target_content)
                else:
                    external.write_bytes(target_content)
                (run_directory / child).symlink_to(
                    external, target_is_directory=child == "mods")

                result = self.run_harness(
                    "assemble", "--manifest", str(manifest), "--cache", str(cache),
                    "--profile", "renderer", "--side", "client",
                    "--run-dir", str(run_directory), "--allowed-root", str(directory))

                self.assertEqual(1, result.returncode)
                self.assertIn("unsafe owned path", result.stderr)
                if child == "mods":
                    self.assertEqual(target_content, target.read_bytes())
                else:
                    self.assertEqual(target_content, external.read_bytes())

    def test_owned_text_replaces_hardlinks_without_mutating_external_inode(self):
        for name in ("eula.txt", "server.properties", "compatibility-receipt.json"):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as raw_directory:
                directory = Path(raw_directory)
                owned = directory / "owned"
                owned.mkdir()
                external = directory / "external"
                external.write_bytes(b"external content")
                os.link(external, owned / name)
                external_inode = external.stat().st_ino
                directory_descriptor = os.open(
                    owned, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
                try:
                    HARNESS_MODULE.write_owned_text(
                        directory_descriptor, name, "generated content\n")
                finally:
                    os.close(directory_descriptor)

                self.assertEqual(b"external content", external.read_bytes())
                self.assertEqual("generated content\n", (owned / name).read_text())
                self.assertEqual(external_inode, external.stat().st_ino)
                self.assertNotEqual(external_inode, (owned / name).stat().st_ino)

    def test_assemble_replaces_generated_hardlinks_without_mutating_external_inode(self):
        for name in ("eula.txt", "compatibility-receipt.json"):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as raw_directory:
                directory = Path(raw_directory)
                manifest, cache = self.fixture(directory)
                (cache / "fixture.jar").write_bytes(b"pinned jar")
                run_directory = directory / "isolated-client"
                run_directory.mkdir()
                external = directory / "external"
                external.write_bytes(b"external content")
                os.link(external, run_directory / name)
                external_inode = external.stat().st_ino

                result = self.run_harness(
                    "assemble", "--manifest", str(manifest), "--cache", str(cache),
                    "--profile", "renderer", "--side", "client",
                    "--run-dir", str(run_directory), "--allowed-root", str(directory))

                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(b"external content", external.read_bytes())
                self.assertEqual(external_inode, external.stat().st_ino)
                self.assertNotEqual(external_inode, (run_directory / name).stat().st_ino)

    def test_owned_text_removes_exclusive_temporary_file_when_replace_fails(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            owned = Path(raw_directory) / "owned"
            owned.mkdir()
            directory_descriptor = os.open(
                owned, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
            try:
                with mock.patch.object(HARNESS_MODULE.os, "replace",
                                       side_effect=OSError("simulated replace failure")):
                    with self.assertRaisesRegex(
                            HARNESS_MODULE.CompatibilityError,
                            "simulated replace failure"):
                        HARNESS_MODULE.write_owned_text(
                            directory_descriptor, "eula.txt", "eula=true\n")
            finally:
                os.close(directory_descriptor)

            self.assertEqual([], list(owned.iterdir()))

    def test_assemble_uses_verified_open_source_when_cache_path_is_swapped(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest_path, cache = self.fixture(directory)
            source = cache / "fixture.jar"
            source.write_bytes(b"pinned jar")
            run_directory = directory / "isolated-client"
            manifest = HARNESS_MODULE.load_manifest(manifest_path)
            original_verify = HARNESS_MODULE.verify_artifact

            def verify_then_swap(artifact, artifact_cache):
                verified = original_verify(artifact, artifact_cache)
                source.rename(cache / "verified-original.jar")
                source.write_bytes(b"tampered!!")
                return verified

            with mock.patch.object(HARNESS_MODULE, "verify_artifact",
                                   side_effect=verify_then_swap):
                HARNESS_MODULE.assemble(
                    manifest, cache, "renderer", "client", run_directory, directory)

            self.assertEqual(b"pinned jar", (run_directory / "mods" / "fixture.jar").read_bytes())

    def test_staged_descriptor_mismatch_fails_and_removes_partial_destination(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            manifest_path, _ = self.fixture(directory)
            artifact = HARNESS_MODULE.load_manifest(manifest_path)["artifacts"][0]
            source = directory / "wrong.jar"
            source.write_bytes(b"tampered!!")
            mods = directory / "mods"
            mods.mkdir()
            source_descriptor = os.open(source, os.O_RDONLY | os.O_NOFOLLOW)
            mods_descriptor = os.open(mods, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
            try:
                with self.assertRaisesRegex(HARNESS_MODULE.CompatibilityError,
                                            "staged artifact mismatch"):
                    HARNESS_MODULE.stage_verified_artifact(
                        artifact, source_descriptor, mods_descriptor)
            finally:
                os.close(source_descriptor)
                os.close(mods_descriptor)
            self.assertFalse((mods / "fixture.jar").exists())

    def test_sanitizer_redacts_identity_network_uuid_home_and_seed_deterministically(self):
        with tempfile.TemporaryDirectory() as raw_directory:
            directory = Path(raw_directory)
            source = directory / "raw.log"
            first = directory / "first.log"
            second = directory / "second.log"
            source.write_text(
                "[23:32:19] [INFO] Player James joined 127.0.0.1:25565 uuid="
                "123e4567-e89b-12d3-a456-426614174000 at /Users/james/private\n"
                "[INFO] endpoints [::1]:24454 fe80::1 localhost:25565\n"
                "[WARN] seed: 987654321\n[INFO] management-server-secret=private-token\n"
                "[ERROR] retained diagnostic\n", encoding="utf-8")
            arguments = ("sanitize", "--input", str(source), "--identity", "James")
            one = self.run_harness(*arguments, "--output", str(first))
            two = self.run_harness(*arguments, "--output", str(second))
            self.assertEqual(0, one.returncode, one.stderr)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            sanitized = first.read_text(encoding="utf-8")
            self.assertNotIn("James", sanitized)
            self.assertNotIn("127.0.0.1", sanitized)
            self.assertNotIn("123e4567", sanitized)
            self.assertNotIn("987654321", sanitized)
            self.assertNotIn("::1", sanitized)
            self.assertNotIn("localhost:25565", sanitized)
            self.assertNotIn("private-token", sanitized)
            self.assertIn("[WARN] seed: <redacted>", sanitized)
            self.assertIn("[ERROR] retained diagnostic", sanitized)
            self.assertIn("[23:32:19]", sanitized)


if __name__ == "__main__":
    unittest.main()
