#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "scripts"
sys.path.insert(0, str(SCRIPTS))
from release_contract import ReleaseContractError, load_catalogue
from release_gate import run_gate

MODULE_PATH = SCRIPTS / "release_envelope.py"
SPEC = importlib.util.spec_from_file_location("release_envelope", MODULE_PATH)
ENVELOPE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(ENVELOPE)


class ReleaseEnvelopeTest(unittest.TestCase):
    PLAN = Path("docs/superpowers/plans/selected.md")
    BACKLOG = Path("docs/planning/IMPROVEMENT_BACKLOG.md")

    def git(self, repo: Path, *arguments: str) -> str:
        result = subprocess.run(
            ["git", *arguments], cwd=repo, capture_output=True, text=True, check=True)
        return result.stdout.strip()

    def fixture(
            self, directory: Path, *, final: bool = True
    ) -> tuple[Path, Path, Path, str]:
        origin = directory / "origin.git"
        subprocess.run(["git", "init", "--bare", str(origin)], check=True,
                       capture_output=True, text=True)
        repo = directory / "repo"
        repo.mkdir()
        self.git(repo, "init", "-b", "main")
        self.git(repo, "config", "user.email", "qa@example.invalid")
        self.git(repo, "config", "user.name", "QA Fixture")
        self.git(repo, "remote", "add", "origin", str(origin))

        (repo / self.PLAN.parent).mkdir(parents=True)
        (repo / self.BACKLOG.parent).mkdir(parents=True)
        (repo / self.PLAN).write_text(
            "# Selected\n\n- [x] final row\n" if final else
            "# Selected\n\n- [ ] QA-001 remains open\n", encoding="utf-8")
        (repo / self.BACKLOG).write_text(
            "# Backlog\n" if final else
            "# Backlog\n\n| QA-001 | Guarantee | P0 | Envelope | Report |\n",
            encoding="utf-8")
        (repo / ".gitignore").write_text("/build/\n", encoding="utf-8")
        (repo / "gradle.properties").write_text(
            "mod_version=1.2.3\n"
            "minecraft_version=26.2\n"
            "loader_version=0.19.3\n",
            encoding="utf-8")
        (repo / "fixture.py").write_text("print('gate passed')\n", encoding="utf-8")

        evidence_directory = repo / "docs/evidence"
        evidence_directory.mkdir(parents=True)
        junit = evidence_directory / "junit.xml"
        junit.write_text(
            '<testsuites tests="1" failures="0" errors="0" skipped="0">'
            '<testsuite name="unit" tests="1" failures="0" errors="0" skipped="0"/>'
            '</testsuites>', encoding="utf-8")
        evidence = repo / "config/release/qa-001-evidence.json"
        evidence.parent.mkdir(parents=True)
        evidence.write_text(json.dumps({
            "schemaVersion": 1,
            "commit": "@HEAD",
            "rows": [{
                "id": "jvm-tests",
                "kind": "junit",
                "validator": "junit-xml",
                "path": "docs/evidence/junit.xml",
                "sha256": hashlib.sha256(junit.read_bytes()).hexdigest(),
                "size": junit.stat().st_size,
                "commit": "@HEAD",
                "producer": ["python3", "fixture.py"],
                "result": {"tests": 1, "failures": 0, "errors": 0, "skipped": 0},
                "limitations": [],
            }],
        }), encoding="utf-8")
        catalogue = repo / "config/release/qa-001-gates.json"
        catalogue.write_text(json.dumps({
            "schemaVersion": 1,
            "repository": "james8464/powers-mod",
            "outputRoot": "build/release-envelope",
            "environmentAllowlist": ["GITHUB_SHA"],
            "commands": [{
                "id": "unit",
                "argv": ["python3", "fixture.py"],
                "validator": "command-receipt",
            }],
            "evidence": [{
                "id": "jvm-tests", "kind": "junit", "validator": "junit-xml",
            }],
            "artifacts": [
                {"id": "runtime-jar", "pathTemplate": "build/libs/powers-{version}.jar"},
                {"id": "sources-jar", "pathTemplate": "build/libs/powers-{version}-sources.jar"},
            ],
        }), encoding="utf-8")
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-m", "release fixture")
        self.git(repo, "push", "-u", "origin", "main")
        commit = self.git(repo, "rev-parse", "HEAD")
        return repo, catalogue, evidence, commit

    def artifacts(self, repo: Path) -> tuple[Path, Path]:
        output = repo / "build/libs"
        output.mkdir(parents=True, exist_ok=True)
        runtime = output / "powers-1.2.3.jar"
        sources = output / "powers-1.2.3-sources.jar"
        with zipfile.ZipFile(runtime, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nImplementation-Version: 1.2.3\n")
            archive.writestr("fabric.mod.json", json.dumps({"id": "powers", "version": "1.2.3"}))
            archive.writestr("com/powers/Main.class", b"bytecode")
        with zipfile.ZipFile(sources, "w") as archive:
            archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nImplementation-Version: 1.2.3\n")
            archive.writestr("com/powers/Main.java", "package com.powers; class Main {}\n")
        return runtime, sources

    def receipts(self, repo: Path, catalogue_path: Path, commit: str) -> Path:
        directory = repo / "build/release-envelope/receipts"
        run_gate(
            load_catalogue(catalogue_path), "unit", directory, repo, commit,
            {**os.environ, "GITHUB_SHA": commit}, catalogue_path=catalogue_path)
        return directory

    def test_repository_validation_requires_exact_clean_single_main_and_final_ledger(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, _, _, commit = self.fixture(Path(raw))
            state = ENVELOPE.validate_repository(
                repo, commit, True, self.PLAN, self.BACKLOG, "build/release-envelope")
            self.assertTrue(state["accepted"])
            self.assertEqual("main", state["branch"])
            self.assertEqual(commit, state["commit"])
            self.assertEqual(["main"], state["localBranches"])
            self.assertEqual(["origin/main"], state["remoteBranches"])

        with tempfile.TemporaryDirectory() as raw:
            repo, _, _, commit = self.fixture(Path(raw), final=False)
            state = ENVELOPE.validate_repository(
                repo, commit, False, self.PLAN, self.BACKLOG, "build/release-envelope")
            self.assertFalse(state["accepted"])
            with self.assertRaisesRegex(ReleaseContractError, "open checkbox"):
                ENVELOPE.validate_repository(
                    repo, commit, True, self.PLAN, self.BACKLOG, "build/release-envelope")

    def test_repository_validation_rejects_branch_remote_and_worktree_drift(self):
        mutations = ("dirty", "untracked", "wrong-branch", "extra-local", "remote-behind", "extra-remote")
        for mutation in mutations:
            with self.subTest(mutation=mutation), tempfile.TemporaryDirectory() as raw:
                repo, _, _, commit = self.fixture(Path(raw))
                expected = commit
                if mutation == "dirty":
                    (repo / self.PLAN).write_text("changed\n", encoding="utf-8")
                    message = "worktree"
                elif mutation == "untracked":
                    (repo / "untracked.txt").write_text("new\n", encoding="utf-8")
                    message = "worktree"
                elif mutation == "wrong-branch":
                    self.git(repo, "switch", "-c", "feature")
                    message = "branch"
                elif mutation == "extra-local":
                    self.git(repo, "branch", "extra")
                    message = "local branches"
                elif mutation == "remote-behind":
                    self.git(repo, "commit", "--allow-empty", "-m", "local only")
                    expected = self.git(repo, "rev-parse", "HEAD")
                    message = "origin/main"
                else:
                    self.git(repo, "push", "origin", "HEAD:refs/heads/extra")
                    message = "remote branches"
                with self.assertRaisesRegex(ReleaseContractError, message):
                    ENVELOPE.validate_repository(
                        repo, expected, True, self.PLAN, self.BACKLOG,
                        "build/release-envelope")

    def test_receipt_set_is_exact_and_reverified(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, _, commit = self.fixture(Path(raw))
            directory = self.receipts(repo, catalogue_path, commit)
            catalogue = load_catalogue(catalogue_path)
            receipts = ENVELOPE.validate_receipts(
                catalogue, directory, repo, commit, catalogue_path)
            self.assertEqual(["unit"], [receipt.gate_id for receipt in receipts])

            shutil.copy2(directory / "unit.json", directory / "duplicate.json")
            with self.assertRaisesRegex(ReleaseContractError, "unexpected receipt"):
                ENVELOPE.validate_receipts(
                    catalogue, directory, repo, commit, catalogue_path)
            (directory / "duplicate.json").unlink()
            (directory / "unit.json").unlink()
            with self.assertRaisesRegex(ReleaseContractError, "missing receipt"):
                ENVELOPE.validate_receipts(
                    catalogue, directory, repo, commit, catalogue_path)

    def test_receipt_directory_cannot_escape_through_symlink(self):
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            repo, catalogue_path, _, commit = self.fixture(directory)
            receipts = self.receipts(repo, catalogue_path, commit)
            external = directory / "external-receipts"
            receipts.rename(external)
            receipts.symlink_to(external, target_is_directory=True)
            with self.assertRaisesRegex(ReleaseContractError, "receipt directory"):
                ENVELOPE.validate_receipts(
                    load_catalogue(catalogue_path), receipts, repo, commit, catalogue_path)

    def test_artifacts_require_exact_gradle_version_names_and_jar_content(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, _, _ = self.fixture(Path(raw))
            runtime, sources = self.artifacts(repo)
            accepted = ENVELOPE.validate_artifacts(
                repo, load_catalogue(catalogue_path), runtime, sources)
            self.assertEqual(["runtime-jar", "sources-jar"], [row["id"] for row in accepted])
            self.assertTrue(all(len(row["sha256"]) == 64 for row in accepted))

            wrong = runtime.with_name("powers-wrong.jar")
            shutil.copy2(runtime, wrong)
            with self.assertRaisesRegex(ReleaseContractError, "artifact path"):
                ENVELOPE.validate_artifacts(
                    repo, load_catalogue(catalogue_path), wrong, sources)
            with zipfile.ZipFile(runtime, "w") as archive:
                archive.writestr("fabric.mod.json", json.dumps({"id": "powers", "version": "9.9.9"}))
                archive.writestr("com/powers/Main.class", b"bytecode")
            with self.assertRaisesRegex(ReleaseContractError, "version"):
                ENVELOPE.validate_artifacts(
                    repo, load_catalogue(catalogue_path), runtime, sources)

    def test_final_build_is_canonical_deterministic_and_checksum_complete(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, evidence, commit = self.fixture(Path(raw))
            receipts = self.receipts(repo, catalogue, commit)
            runtime, sources = self.artifacts(repo)
            output = repo / "build/release-envelope"
            arguments = dict(
                repo_root=repo,
                expected_sha=commit,
                catalogue_path=catalogue,
                evidence_path=evidence,
                receipts_dir=receipts,
                runtime_jar=runtime,
                sources_jar=sources,
                output_dir=output,
                mode="final",
                created_at="2026-08-21T12:00:00Z",
                github_run_id="123",
                github_run_attempt="1",
                plan_path=self.PLAN,
                backlog_path=self.BACKLOG,
            )
            paths = ENVELOPE.build_envelope(**arguments)
            first = {name: path.read_bytes() for name, path in paths.items()}
            value = json.loads(first["json"])
            self.assertTrue(value["accepted"])
            self.assertEqual(commit, value["commit"])
            self.assertEqual("1.2.3", value["version"])
            self.assertEqual(["unit"], [gate["id"] for gate in value["gates"]])
            self.assertEqual(["jvm-tests"], [row["id"] for row in value["evidence"]])
            self.assertIn("gh attestation verify", first["markdown"].decode("utf-8"))

            checksum_lines = first["checksums"].decode("utf-8").splitlines()
            self.assertEqual(5, len(checksum_lines))
            for line in checksum_lines:
                digest, relative = line.split("  ", 1)
                self.assertEqual(digest, hashlib.sha256((repo / relative).read_bytes()).hexdigest())

            second_paths = ENVELOPE.build_envelope(**arguments)
            second = {name: path.read_bytes() for name, path in second_paths.items()}
            self.assertEqual(first, second)

    def test_preflight_never_emits_an_accepted_envelope(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, evidence, commit = self.fixture(Path(raw), final=False)
            receipts = self.receipts(repo, catalogue, commit)
            runtime, sources = self.artifacts(repo)
            output = repo / "build/release-envelope"
            result = ENVELOPE.build_envelope(
                repo, commit, catalogue, evidence, receipts, runtime, sources, output,
                "preflight", "2026-08-21T12:00:00Z", "", "",
                self.PLAN, self.BACKLOG)
            self.assertEqual({}, result)
            self.assertFalse((output / "release-envelope.json").exists())

    def test_output_failure_cleans_partial_envelope(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, evidence, commit = self.fixture(Path(raw))
            receipts = self.receipts(repo, catalogue, commit)
            runtime, sources = self.artifacts(repo)
            output = repo / "build/release-envelope"
            original = ENVELOPE.write_bytes_atomic
            calls = 0

            def fail_second(path: Path, data: bytes) -> str:
                nonlocal calls
                calls += 1
                if calls == 2:
                    raise OSError("interrupted output")
                return original(path, data)

            with mock.patch.object(ENVELOPE, "write_bytes_atomic", side_effect=fail_second):
                with self.assertRaisesRegex(OSError, "interrupted output"):
                    ENVELOPE.build_envelope(
                        repo, commit, catalogue, evidence, receipts, runtime, sources, output,
                        "final", "2026-08-21T12:00:00Z", "123", "1",
                        self.PLAN, self.BACKLOG)
            for name in ("release-envelope.json", "release-envelope.md", "SHA256SUMS"):
                self.assertFalse((output / name).exists())


if __name__ == "__main__":
    unittest.main()
