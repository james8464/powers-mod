#!/usr/bin/env python3

import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
HARNESS = ROOT / "scripts/compatibility_harness.py"


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
