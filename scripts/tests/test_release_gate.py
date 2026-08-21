#!/usr/bin/env python3

import importlib.util
import hashlib
import json
import os
import signal
import subprocess
import sys
import tempfile
import threading
import time
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
        catalogue = repo / "config/release/qa-001-gates.json"
        catalogue.parent.mkdir(parents=True)
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
        self.run_git(repo, "add", "fixture.py", "config/release/qa-001-gates.json")
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
                    GATE_MODULE, "_write_json_exclusive_at",
                    side_effect=OSError("receipt failure")):
                with self.assertRaisesRegex(OSError, "receipt failure"):
                    GATE_MODULE.run_gate(
                        catalogue, "unit", self.receipt_directory(repo), repo, commit,
                        self.environment(commit), catalogue_path=catalogue_path)
            self.assertFalse((self.receipt_directory(repo) / "unit.json").exists())

    def test_prior_accepted_receipt_and_log_are_never_overwritten(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, first_commit = self.fixture(Path(raw))
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            receipt = GATE_MODULE.run_gate(
                catalogue, "unit", self.receipt_directory(repo), repo, first_commit,
                self.environment(first_commit), catalogue_path=catalogue_path)
            original_receipt = receipt.read_bytes()
            original_log = (receipt.parent / "unit.log").read_bytes()

            self.run_git(repo, "commit", "--allow-empty", "-m", "next candidate")
            second_commit = self.run_git(repo, "rev-parse", "HEAD")
            with self.assertRaisesRegex(
                    GATE_MODULE.ReleaseContractError,
                    "accepted gate output already exists"):
                GATE_MODULE.run_gate(
                    catalogue, "unit", self.receipt_directory(repo), repo,
                    second_commit, self.environment(second_commit),
                    catalogue_path=catalogue_path)

            self.assertEqual(original_receipt, receipt.read_bytes())
            self.assertEqual(original_log, (receipt.parent / "unit.log").read_bytes())

    def test_concurrent_gate_cannot_share_or_overwrite_reserved_namespace(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(
                Path(raw), "import time\ntime.sleep(0.4)\nprint('done')\n")
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            errors: list[BaseException] = []

            def first_run() -> None:
                try:
                    GATE_MODULE.run_gate(
                        catalogue, "unit", self.receipt_directory(repo), repo,
                        commit, self.environment(commit),
                        catalogue_path=catalogue_path)
                except BaseException as error:
                    errors.append(error)

            thread = threading.Thread(target=first_run)
            thread.start()
            lock = self.receipt_directory(repo) / ".unit.lock"
            deadline = time.monotonic() + 2
            while not lock.exists() and time.monotonic() < deadline:
                time.sleep(0.01)
            self.assertTrue(lock.exists())
            with self.assertRaisesRegex(
                    GATE_MODULE.ReleaseContractError, "namespace is already reserved"):
                GATE_MODULE.run_gate(
                    catalogue, "unit", self.receipt_directory(repo), repo,
                    commit, self.environment(commit),
                    catalogue_path=catalogue_path)
            self.assertTrue(lock.exists(), "failed contender removed owner lock")
            with self.assertRaisesRegex(
                    GATE_MODULE.ReleaseContractError, "namespace is already reserved"):
                GATE_MODULE.run_gate(
                    catalogue, "unit", self.receipt_directory(repo), repo,
                    commit, self.environment(commit),
                    catalogue_path=catalogue_path)
            thread.join(timeout=3)
            self.assertFalse(thread.is_alive())
            self.assertEqual([], errors)
            self.assertTrue((self.receipt_directory(repo) / "unit.json").is_file())

    def test_receipt_parent_swap_never_redirects_gate_output(self):
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            repo, catalogue_path, commit = self.fixture(directory)
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            original_writer = GATE_MODULE._write_process_log
            moved = directory / "held-receipts"
            external = directory / "external"
            external.mkdir()

            def swap_then_write(*arguments, **keywords):
                receipts = self.receipt_directory(repo)
                receipts.rename(moved)
                receipts.symlink_to(external, target_is_directory=True)
                return original_writer(*arguments, **keywords)

            with mock.patch.object(
                    GATE_MODULE, "_write_process_log", side_effect=swap_then_write):
                with self.assertRaisesRegex(
                        GATE_MODULE.ReleaseContractError, "receipt directory changed"):
                    GATE_MODULE.run_gate(
                        catalogue, "unit", self.receipt_directory(repo), repo,
                        commit, self.environment(commit),
                        catalogue_path=catalogue_path)
            self.assertEqual([], list(external.iterdir()))

    def test_private_success_log_never_becomes_accepted_output(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(
                Path(raw), "print('/Users/private/workspace')\n")
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            with self.assertRaisesRegex(
                    GATE_MODULE.ReleaseContractError,
                    "packaged text contains unowned absolute path"):
                GATE_MODULE.run_gate(
                    catalogue, "unit", self.receipt_directory(repo), repo,
                    commit, self.environment(commit),
                    catalogue_path=catalogue_path)
            directory = self.receipt_directory(repo)
            self.assertFalse((directory / "unit.json").exists())
            self.assertFalse((directory / "unit.log").exists())

    def test_private_allowlisted_environment_is_rejected_before_execution(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw))
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            environment = self.environment(commit)
            environment["JAVA_VERSION"] = "/home/private/jdk"
            with self.assertRaisesRegex(
                    GATE_MODULE.ReleaseContractError,
                    "packaged text contains unowned absolute path"):
                GATE_MODULE.run_gate(
                    catalogue, "unit", self.receipt_directory(repo), repo,
                    commit, environment, catalogue_path=catalogue_path)
            self.assertFalse((self.receipt_directory(repo) / "unit.json").exists())

    def test_bare_ambient_secret_is_not_available_to_gate_process(self):
        source = (
            "import os\n"
            "print(os.environ.get('RELEASE_SECRET', 'not-present'))\n")
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw), source)
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            receipt = GATE_MODULE.run_gate(
                catalogue, "unit", self.receipt_directory(repo), repo, commit,
                self.environment(commit), catalogue_path=catalogue_path)
            log = (receipt.parent / "unit.log").read_text(encoding="utf-8")
            self.assertIn("not-present", log)
            self.assertNotIn("must-not-be-captured", log)

    def test_receipt_verification_privacy_checks_the_exact_hashed_log(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, commit = self.fixture(Path(raw))
            catalogue = GATE_MODULE.load_catalogue(catalogue_path)
            receipt = GATE_MODULE.run_gate(
                catalogue, "unit", self.receipt_directory(repo), repo, commit,
                self.environment(commit), catalogue_path=catalogue_path)
            value = json.loads(receipt.read_text(encoding="utf-8"))
            log = repo / value["logPath"]
            log.write_text("/Users/private/workspace\n", encoding="utf-8")
            value["logSize"] = log.stat().st_size
            value["logSha256"] = hashlib.sha256(log.read_bytes()).hexdigest()
            receipt.write_text(json.dumps(value), encoding="utf-8")

            with self.assertRaisesRegex(
                    GATE_MODULE.ReleaseContractError,
                    "packaged text contains unowned absolute path"):
                GATE_MODULE.verify_receipt(
                    receipt, catalogue, repo, commit,
                    catalogue_path=catalogue_path)


if __name__ == "__main__":
    unittest.main()
