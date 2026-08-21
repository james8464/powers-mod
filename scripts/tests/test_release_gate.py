#!/usr/bin/env python3

import importlib.util
import json
import os
import signal
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "scripts"
sys.path.insert(0, str(SCRIPTS))
MODULE_PATH = SCRIPTS / "release_gate.py"
SPEC = importlib.util.spec_from_file_location("release_gate", MODULE_PATH)
GATE_MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GATE_MODULE)


class ReleaseGateTest(unittest.TestCase):
    def run_git(self, repo: Path, *arguments: str) -> str:
        result = subprocess.run(
            ["git", *arguments], cwd=repo, capture_output=True, text=True, check=True)
        return result.stdout.strip()

    def fixture(self, directory: Path, source: str | None = None) -> tuple[Path, Path, str]:
        repo = directory / "repo"
        repo.mkdir()
        fixture = source or (
            "import os, sys\n"
            "print('args=' + ','.join(sys.argv[1:]))\n"
            "print('java=' + os.environ.get('JAVA_VERSION', 'missing'))\n")
        (repo / "fixture.py").write_text(fixture, encoding="utf-8")
        catalogue = repo / "catalogue.json"
        catalogue.write_text(json.dumps({
            "schemaVersion": 1,
            "repository": "james8464/powers-mod",
            "outputRoot": "build/release-envelope",
            "environmentAllowlist": ["JAVA_VERSION", "GITHUB_SHA"],
            "commands": [{
                "id": "unit",
                "argv": ["python3", "-B", "fixture.py", "hello"],
                "validator": "command-receipt",
            }],
            "evidence": [{
                "id": "tests", "kind": "junit", "validator": "junit-xml",
            }],
            "artifacts": [{
                "id": "runtime-jar", "pathTemplate": "build/libs/powers-{version}.jar",
            }],
        }), encoding="utf-8")
        self.run_git(repo, "init", "-b", "main")
        self.run_git(repo, "config", "user.email", "qa@example.invalid")
        self.run_git(repo, "config", "user.name", "QA Fixture")
        self.run_git(repo, "add", "fixture.py", "catalogue.json")
        self.run_git(repo, "commit", "-m", "fixture")
        return repo, catalogue, self.run_git(repo, "rev-parse", "HEAD")

    def receipt_directory(self, repo: Path) -> Path:
        return repo / "build/release-envelope/receipts"

    def environment(self, commit: str) -> dict[str, str]:
        value = dict(os.environ)
        value.update({
            "JAVA_VERSION": "25",
            "GITHUB_SHA": commit,
            "RELEASE_SECRET": "must-not-be-captured",
        })
        return value

    def test_run_gate_executes_exact_argv_and_writes_verified_receipt(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw))
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            receipt_path = GATE_MODULE.run_gate(
                catalogue, "unit", self.receipt_directory(repo), repo, commit,
                self.environment(commit), catalogue_path=catalogue_path)

            receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
            self.assertEqual("unit", receipt["gateId"])
            self.assertEqual(commit, receipt["commit"])
            self.assertEqual(["python3", "-B", "fixture.py", "hello"], receipt["argv"])
            self.assertEqual({"GITHUB_SHA": commit, "JAVA_VERSION": "25"}, receipt["environment"])
            self.assertNotIn("RELEASE_SECRET", receipt["environment"])
            self.assertEqual(0, receipt["exitCode"])
            self.assertGreaterEqual(receipt["durationSeconds"], 0)
            self.assertRegex(receipt["startedAt"], r"^\d{4}-\d\d-\d\dT.*Z$")
            self.assertRegex(receipt["endedAt"], r"^\d{4}-\d\d-\d\dT.*Z$")
            log = repo / receipt["logPath"]
            self.assertIn("args=hello", log.read_text(encoding="utf-8"))
            self.assertIn("java=25", log.read_text(encoding="utf-8"))
            verified = GATE_MODULE.verify_receipt(
                receipt_path, catalogue, repo, commit, catalogue_path=catalogue_path)
            self.assertEqual("unit", verified.gate_id)
            self.assertEqual(log.stat().st_size, verified.log_size)

    def test_dry_run_prints_canonical_argv_without_receipt_or_execution(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, commit = self.fixture(Path(raw), "raise SystemExit(91)\n")
            result = subprocess.run([
                sys.executable, "-B", str(MODULE_PATH),
                "--catalogue", str(catalogue), "--gate", "unit",
                "--receipt-dir", str(self.receipt_directory(repo)),
                "--repo-root", str(repo), "--expected-sha", commit, "--dry-run",
            ], cwd=repo, capture_output=True, text=True, check=False)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual(
                '["python3","-B","fixture.py","hello"]\n', result.stdout)
            self.assertFalse(self.receipt_directory(repo).exists())

    def test_undeclared_gate_and_mismatched_head_fail_before_execution(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, commit = self.fixture(Path(raw))
            for gate, expected_sha, expected in (
                    ("missing", commit, "unknown gate"),
                    ("unit", "f" * 40, "HEAD does not match")):
                with self.subTest(gate=gate):
                    result = subprocess.run([
                        sys.executable, "-B", str(MODULE_PATH),
                        "--catalogue", str(catalogue), "--gate", gate,
                        "--receipt-dir", str(self.receipt_directory(repo)),
                        "--repo-root", str(repo), "--expected-sha", expected_sha,
                    ], cwd=repo, capture_output=True, text=True, check=False)
                    self.assertEqual(1, result.returncode)
                    self.assertIn(expected, result.stderr)
                    self.assertFalse((self.receipt_directory(repo) / f"{gate}.json").exists())

    def test_nonzero_and_signal_exit_never_write_accepted_receipt(self):
        sources = (
            ("raise SystemExit(7)\n", "exit code 7"),
            ("import os, signal\nos.kill(os.getpid(), signal.SIGTERM)\n", "signal 15"),
        )
        for source, expected in sources:
            with self.subTest(expected=expected), tempfile.TemporaryDirectory() as raw:
                repo, catalogue_path, commit = self.fixture(Path(raw), source)
                catalogue = GATE_MODULE.load_catalogue(catalogue_path)
                with self.assertRaisesRegex(GATE_MODULE.ReleaseContractError, expected):
                    GATE_MODULE.run_gate(
                        catalogue, "unit", self.receipt_directory(repo), repo, commit,
                        self.environment(commit), catalogue_path=catalogue_path)
                directory = self.receipt_directory(repo)
                self.assertFalse((directory / "unit.json").exists())
                self.assertTrue((directory / "unit.failed.json").is_file())
                self.assertTrue((directory / "unit.failed.log").is_file())

    def test_oversized_log_is_failed_without_unbounded_memory_capture(self):
        source = "import sys\nsys.stdout.write('x' * 5000)\n"
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw), source)
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            with self.assertRaisesRegex(GATE_MODULE.ReleaseContractError, "log exceeded 1024 bytes"):
                GATE_MODULE.run_gate(
                    catalogue, "unit", self.receipt_directory(repo), repo, commit,
                    self.environment(commit), catalogue_path=catalogue_path,
                    max_log_bytes=1024)
            self.assertFalse((self.receipt_directory(repo) / "unit.json").exists())
            self.assertLessEqual((self.receipt_directory(repo) / "unit.failed.log").stat().st_size, 1025)

    def test_mutated_log_invalidates_an_existing_receipt(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw))
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            receipt = GATE_MODULE.run_gate(
                catalogue, "unit", self.receipt_directory(repo), repo, commit,
                self.environment(commit), catalogue_path=catalogue_path)
            parsed = json.loads(receipt.read_text(encoding="utf-8"))
            (repo / parsed["logPath"]).write_text("tampered\n", encoding="utf-8")
            with self.assertRaisesRegex(GATE_MODULE.ReleaseContractError, "log (size|SHA-256) mismatch"):
                GATE_MODULE.verify_receipt(
                    receipt, catalogue, repo, commit, catalogue_path=catalogue_path)

    def test_atomic_receipt_failure_leaves_no_accepted_receipt(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw))
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            with mock.patch.object(
                    GATE_MODULE, "write_json_atomic", side_effect=OSError("receipt failure")):
                with self.assertRaisesRegex(OSError, "receipt failure"):
                    GATE_MODULE.run_gate(
                        catalogue, "unit", self.receipt_directory(repo), repo, commit,
                        self.environment(commit), catalogue_path=catalogue_path)
            self.assertFalse((self.receipt_directory(repo) / "unit.json").exists())


if __name__ == "__main__":
    unittest.main()
