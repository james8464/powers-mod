#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import os
import re
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
    PLAN = Path("docs/superpowers/plans/2026-08-12-stages-1-8-completion.md")
    BACKLOG = Path("docs/planning/IMPROVEMENT_BACKLOG.md")

    def test_checked_in_programme_ledger_matches_locked_release_identity(self):
        text = (ROOT / self.PLAN).read_text(encoding="utf-8")
        ledger_section = text.split("## Decisions ledger", 1)[0]
        identifiers = []
        for line in ledger_section.splitlines():
            if re.match(r"^- \[[ x]\] ", line):
                identifiers.extend(re.findall(r"`([A-Z]+-\d+)`", line))
        self.assertEqual(ENVELOPE.REQUIRED_LEDGER_IDS, tuple(identifiers))
        final_section = text.split("## Final acceptance", 1)[1]
        final_rows = tuple(re.findall(r"(?m)^- \[[ x]\] (.+)$", final_section))
        self.assertEqual(ENVELOPE.REQUIRED_FINAL_ACCEPTANCE, final_rows)

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
        ledger = []
        for identifier in ENVELOPE.REQUIRED_LEDGER_IDS:
            checked = final or identifier != "QA-001"
            ledger.append(f"- [{'x' if checked else ' '}] `{identifier}`: fixture")
        acceptance = [
            f"- [x] {value}" for value in ENVELOPE.REQUIRED_FINAL_ACCEPTANCE]
        (repo / self.PLAN).write_text(
            "# Selected\n\n" + "\n".join(ledger)
            + "\n\n## Decisions ledger\n\nFixture.\n\n## Final acceptance\n\n"
            + "\n".join(acceptance) + "\n", encoding="utf-8")
        backlog_rows = ["| COR-018 | Guarantee | P2 | Deferred | Evidence |"]
        if not final:
            backlog_rows.append("| QA-001 | Guarantee | P0 | Envelope | Report |")
        (repo / self.BACKLOG).write_text(
            "# Backlog\n\n| ID | Kind | Priority | Improvement | Acceptance |\n"
            "| --- | --- | --- | --- | --- |\n" + "\n".join(backlog_rows) + "\n",
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
        mutations = (
            "dirty", "untracked", "wrong-branch", "extra-local", "remote-behind",
            "extra-remote", "hidden-extra-remote")
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
                elif mutation == "extra-remote":
                    self.git(repo, "push", "origin", "HEAD:refs/heads/extra")
                    message = "remote branches"
                else:
                    self.git(repo, "config", "remote.origin.fetch",
                             "+refs/heads/main:refs/remotes/origin/main")
                    self.git(repo, "push", "origin", "HEAD:refs/heads/hidden")
                    self.git(repo, "update-ref", "-d", "refs/remotes/origin/hidden")
                    message = "remote branches"
                with self.assertRaisesRegex(ReleaseContractError, message):
                    ENVELOPE.validate_repository(
                        repo, expected, True, self.PLAN, self.BACKLOG,
                        "build/release-envelope")

    def test_repository_validation_rejects_truncated_governance_files(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, _, _, _ = self.fixture(Path(raw))
            (repo / self.PLAN).write_text("# Selected\n", encoding="utf-8")
            (repo / self.BACKLOG).write_text("# Backlog\n", encoding="utf-8")
            self.git(repo, "add", str(self.PLAN), str(self.BACKLOG))
            self.git(repo, "commit", "-m", "truncate governance")
            self.git(repo, "push", "origin", "main")
            commit = self.git(repo, "rev-parse", "HEAD")
            with self.assertRaisesRegex(ReleaseContractError, "programme ledger"):
                ENVELOPE.validate_repository(
                    repo, commit, True, self.PLAN, self.BACKLOG,
                    "build/release-envelope")

    def test_final_repository_rejects_any_selected_id_left_in_backlog(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, _, _, _ = self.fixture(Path(raw))
            backlog = repo / self.BACKLOG
            backlog.write_text(
                backlog.read_text(encoding="utf-8")
                + "| VFX-004 | Presentation | P1 | Still active | Evidence |\n",
                encoding="utf-8")
            self.git(repo, "add", str(self.BACKLOG))
            self.git(repo, "commit", "-m", "leave selected row active")
            self.git(repo, "push", "origin", "main")
            commit = self.git(repo, "rev-parse", "HEAD")

            with self.assertRaisesRegex(
                    ReleaseContractError, "selected backlog row remains"):
                ENVELOPE.validate_repository(
                    repo, commit, True, self.PLAN, self.BACKLOG,
                    "build/release-envelope")

    def test_receipt_set_is_exact_and_reverified(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, _, commit = self.fixture(Path(raw))
            directory = self.receipts(repo, catalogue_path, commit)
            catalogue = load_catalogue(catalogue_path)
            receipts = ENVELOPE.validate_receipts(
                catalogue, directory, repo, commit, catalogue_path)
            self.assertEqual(["unit"], [receipt.gate_id for receipt in receipts])

            rogue = directory / "private.running"
            rogue.write_text("secret\n", encoding="utf-8")
            with self.assertRaisesRegex(ReleaseContractError, "unexpected receipt output"):
                ENVELOPE.validate_receipts(
                    catalogue, directory, repo, commit, catalogue_path)
            rogue.unlink()

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

    def test_artifact_validation_carries_recheckable_exact_source_snapshots(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue_path, _, _ = self.fixture(Path(raw))
            runtime, sources = self.artifacts(repo)
            snapshots = []
            accepted = ENVELOPE.validate_artifacts(
                repo, load_catalogue(catalogue_path), runtime, sources,
                source_snapshots=snapshots)
            self.assertEqual(3, len(snapshots))
            self.assertEqual(
                {row["sha256"] for row in accepted},
                {snapshot.sha256 for snapshot in snapshots
                 if snapshot.relative.startswith("build/libs/")})
            runtime.write_bytes(b"replacement")
            runtime_snapshot = next(
                snapshot for snapshot in snapshots
                if snapshot.relative.endswith("powers-1.2.3.jar"))
            with self.assertRaisesRegex(ReleaseContractError, "changed after validation"):
                ENVELOPE.recheck_regular_snapshot(runtime_snapshot)

    def test_final_builder_pins_all_committed_control_paths(self):
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            repo, catalogue, evidence, commit = self.fixture(directory)
            receipts = self.receipts(repo, catalogue, commit)
            runtime, sources = self.artifacts(repo)
            output = repo / "build/release-envelope"
            external_catalogue = directory / "catalogue.json"
            external_catalogue.write_bytes(catalogue.read_bytes())
            with self.assertRaisesRegex(ReleaseContractError, "catalogue path"):
                ENVELOPE.build_envelope(
                    repo, commit, external_catalogue, evidence, receipts,
                    runtime, sources, output, "final", "2026-08-21T12:00:00Z",
                    "123", "1", self.PLAN, self.BACKLOG)

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
            self.assertEqual(6, len(checksum_lines))
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

    def test_final_builder_rejects_every_unowned_output_entry(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, evidence, commit = self.fixture(Path(raw))
            receipts = self.receipts(repo, catalogue, commit)
            runtime, sources = self.artifacts(repo)
            output = repo / "build/release-envelope"
            (output / "private.txt").write_text("not owned\n", encoding="utf-8")
            with self.assertRaisesRegex(ReleaseContractError, "unexpected release output"):
                ENVELOPE.build_envelope(
                    repo, commit, catalogue, evidence, receipts, runtime, sources,
                    output, "final", "2026-08-21T12:00:00Z", "123", "1",
                    self.PLAN, self.BACKLOG)

    def test_generated_subject_swap_cannot_be_checksummed_or_accepted(self):
        with tempfile.TemporaryDirectory() as raw:
            repo, catalogue, evidence, commit = self.fixture(Path(raw))
            receipts = self.receipts(repo, catalogue, commit)
            runtime, sources = self.artifacts(repo)
            output = repo / "build/release-envelope"
            original = ENVELOPE.write_bytes_atomic

            def swap_json(path: Path, data: bytes) -> str:
                digest = original(path, data)
                if path.name == "release-envelope.json":
                    path.write_text('{"accepted":false}\n', encoding="utf-8")
                return digest

            with mock.patch.object(
                    ENVELOPE, "write_bytes_atomic", side_effect=swap_json):
                with self.assertRaisesRegex(
                        ReleaseContractError, "generated output changed"):
                    ENVELOPE.build_envelope(
                        repo, commit, catalogue, evidence, receipts, runtime,
                        sources, output, "final", "2026-08-21T12:00:00Z",
                        "123", "1", self.PLAN, self.BACKLOG)
            for name in ENVELOPE.OUTPUT_NAMES:
                self.assertFalse((output / name).exists())


if __name__ == "__main__":
    unittest.main()
