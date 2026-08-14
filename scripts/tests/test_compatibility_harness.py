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
                "sourceUrl": "https://example.invalid/project/official-project",
                "downloadUrl": "https://example.invalid/file.jar",
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
                "--run-dir", str(run_directory))

            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual(b"pinned jar", (run_directory / "mods" / "fixture.jar").read_bytes())
            self.assertEqual("eula=true\n", (run_directory / "eula.txt").read_text(encoding="utf-8"))

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


if __name__ == "__main__":
    unittest.main()
