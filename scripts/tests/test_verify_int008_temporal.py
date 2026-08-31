#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "verify_int008_temporal", ROOT / "scripts/verify_int008_temporal.py")
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)


class Int008TemporalVerifierTest(unittest.TestCase):
    def make_repository(self, root: Path) -> tuple[Path, str, str]:
        repository = root / "repository"
        repository.mkdir()
        self.git(repository, "init", "-q")
        self.git(repository, "config", "user.name", "INT-008 Test")
        self.git(repository, "config", "user.email", "int008@example.invalid")
        (repository / "clock.txt").write_text("base\n", encoding="utf-8")
        (repository / "removed.txt").write_text("deleted by implementation\n", encoding="utf-8")
        self.git(repository, "add", "clock.txt", "removed.txt")
        self.git(repository, "commit", "-qm", "base")
        base_sha = self.git(repository, "rev-parse", "HEAD")
        (repository / "clock.txt").write_text("implementation\n", encoding="utf-8")
        (repository / "lease.txt").write_text("owned\n", encoding="utf-8")
        (repository / "removed.txt").unlink()
        self.git(repository, "add", "-A")
        self.git(repository, "commit", "-qm", "implementation")
        implementation_sha = self.git(repository, "rev-parse", "HEAD")
        return repository, base_sha, implementation_sha

    @staticmethod
    def git(repository: Path, *arguments: str) -> str:
        return subprocess.check_output(
            ["git", *arguments], cwd=repository, text=True).strip()

    def make_fixture(self, parent: Path) -> tuple[Path, Path]:
        repository, base_sha, implementation_sha = self.make_repository(parent)
        self.base_sha = base_sha
        root = parent / "evidence"
        root.mkdir()
        rows = [
            self.row(implementation_sha, "admin-preservation",
                     {"acquired": False, "leaseActive": False, "vanillaFrozen": True}),
            self.row(implementation_sha, "external-supersession",
                     {"leaseActive": False, "superseded": True, "vanillaFrozen": True}),
            self.row(implementation_sha, "crystal-control-deadline",
                     {"activeAt1199": True, "clock": "CONTROL", "duration": 1200,
                      "releasedAt1200": True, "worldTicksParked": True}, 1200, 0),
            self.row(implementation_sha, "world-managers-paused",
                     {"celestialPaused": True, "channelsPaused": True,
                      "energyMutated": False, "externalFreeze": True,
                      "fieldsPaused": True, "ownedFreeze": True,
                      "heraldCadencePaused": True, "realmPaused": True,
                      "worldAdvanced": False}),
            self.row(implementation_sha, "projectile-pause-resume",
                     {"frozenDistanceSquared": 0.0,
                      "resumedDistanceSquared": 0.25}, 8, 4),
            self.row(implementation_sha, "lifecycle-cleanup",
                     {"dampeningReleased": True, "deathReleased": True,
                      "disconnectReleased": True, "leaseActive": False,
                      "mismatchedSourcePreserved": True, "shadowLossReleased": True,
                      "shutdownReleased": True,
                      "vanillaFrozen": False}),
        ]
        encoded_rows = [json.dumps(row, separators=(",", ":")) for row in rows]
        (root / "temporal-assertions.jsonl").write_text(
            "\n".join(encoded_rows) + "\n", encoding="utf-8")
        self.write_json(root / "build-metadata.json", {
            "schemaVersion": 2, "task": "INT-008", "baseSha": base_sha,
            "implementationSha": implementation_sha, "result": "PENDING",
            "gameTests": 166, "junitTests": 1825, "pythonTests": 226,
        })
        old_clock = subprocess.check_output(
            ["git", "show", f"{base_sha}:clock.txt"], cwd=repository)
        new_clock = subprocess.check_output(
            ["git", "show", f"{implementation_sha}:clock.txt"], cwd=repository)
        lease = subprocess.check_output(
            ["git", "show", f"{implementation_sha}:lease.txt"], cwd=repository)
        removed = subprocess.check_output(
            ["git", "show", f"{base_sha}:removed.txt"], cwd=repository)
        inventory = [
            f"M {hashlib.sha256(old_clock).hexdigest()} {hashlib.sha256(new_clock).hexdigest()} clock.txt",
            f"A - {hashlib.sha256(lease).hexdigest()} lease.txt",
            f"D {hashlib.sha256(removed).hexdigest()} - removed.txt",
        ]
        (root / "source-inventory.txt").write_text(
            "\n".join(sorted(inventory)) + "\n", encoding="utf-8")
        (root / "logs").mkdir()
        (root / "logs/junit").mkdir()
        first_xml = '<testsuite name="first" tests="1800" failures="0" errors="0" skipped="0"/>\n'
        second_xml = '<testsuite name="second" tests="25" failures="0" errors="0" skipped="0"/>\n'
        (root / "logs/junit/TEST-first.xml").write_text(first_xml, encoding="utf-8")
        (root / "logs/junit/TEST-second.xml").write_text(second_xml, encoding="utf-8")
        junit_inventory = "".join(sorted((
            f"{hashlib.sha256(first_xml.encode()).hexdigest()}  TEST-first.xml\n",
            f"{hashlib.sha256(second_xml.encode()).hexdigest()}  TEST-second.xml\n",
        )))
        junit_digest = hashlib.sha256(junit_inventory.encode()).hexdigest()
        (root / "logs/aggregate-check.log").write_text(
            f"INT-008 aggregate preflight verified: {implementation_sha}; clean=true\n"
            "> Task :auditJavaSources\n> Task :auditNonItemAssets\n"
            "> Task :compileJava\n> Task :compileClientJava\n"
            "> Task :compileExampleExtensionJava\n> Task :compileGametestJava\n"
            "> Task :runGameTest\nAll 166 required tests passed :)\n"
            "> Task :compileTestJava\n> Task :test\n> Task :testPythonScripts\n"
            "> Task :validatePowerResources\n> Task :verifyItemDocs\n"
            "> Task :verifyMagicDocs\n> Task :verifyRankDocs\n"
            "> Task :verifyVfxAssetAudit\n> Task :check\n"
            "Ran 226 tests in 1.000s\n\nOK\nBUILD SUCCESSFUL in 2m\n"
            "24 actionable tasks: 24 executed\n"
            f"INT-008 aggregate postflight verified: {implementation_sha}; clean=true\n"
            f"INT-008 JUnit capture verified: files=2; tests=1825; sha256={junit_digest}\n",
            encoding="utf-8")
        self.write_json(root / "logs/junit-summary.json", {
            "schemaVersion": 1, "implementationSha": implementation_sha,
            "framework": "JUnit", "tests": 1825, "failures": 0,
            "errors": 0, "skipped": 0,
        })
        self.write_json(root / "logs/python-summary.json", {
            "schemaVersion": 1, "implementationSha": implementation_sha,
            "framework": "Python", "tests": 226, "failures": 0,
            "errors": 0, "skipped": 0,
        })
        (root / "logs/gametest.log").write_text(
            f"INT-008 checkout verified: {implementation_sha}\n"
            + "\n".join("INT008_TEMPORAL " + row for row in encoded_rows)
            + "\nAll 166 required tests passed :)\nBUILD SUCCESSFUL in 1m\n",
            encoding="utf-8")
        (root / "README.md").write_text("# INT-008 evidence\n", encoding="utf-8")
        return root, repository

    @staticmethod
    def row(sha, case, facts, control=0, world=0):
        return {"schemaVersion": 2, "implementationSha": sha, "case": case,
                "result": "PASS", "controlTicks": control,
                "worldTicks": world, "facts": facts}

    @staticmethod
    def write_json(path, value):
        path.write_text(json.dumps(value, sort_keys=True) + "\n", encoding="utf-8")

    def mutate(self, operation, message):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.make_fixture(Path(raw))
            operation(root)
            with self.assertRaisesRegex(ValueError, message):
                VERIFY.validate(root, repository, self.base_sha)

    def test_complete_exact_sha_evidence_passes(self):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.make_fixture(Path(raw))
            result = VERIFY.validate(root, repository, self.base_sha)
            self.assertEqual(6, result["caseCount"])

    def test_missing_case_is_rejected(self):
        def remove(root):
            rows = (root / "temporal-assertions.jsonl").read_text().splitlines()
            (root / "temporal-assertions.jsonl").write_text("\n".join(rows[:-1]) + "\n")
        self.mutate(remove, "case coverage")

    def test_mixed_sha_is_rejected(self):
        def mix(root):
            rows = [json.loads(line) for line in
                    (root / "temporal-assertions.jsonl").read_text().splitlines()]
            rows[-1]["implementationSha"] = "b" * 40
            (root / "temporal-assertions.jsonl").write_text(
                "".join(json.dumps(row, separators=(",", ":")) + "\n" for row in rows))
        self.mutate(mix, "implementation")

    def test_false_temporal_fact_is_rejected(self):
        def drift(root):
            rows = [json.loads(line) for line in
                    (root / "temporal-assertions.jsonl").read_text().splitlines()]
            rows[3]["facts"]["worldAdvanced"] = True
            (root / "temporal-assertions.jsonl").write_text(
                "".join(json.dumps(row, separators=(",", ":")) + "\n" for row in rows))
        self.mutate(drift, "world manager")

    def test_private_content_and_duplicate_keys_are_rejected(self):
        self.mutate(lambda root: (root / "README.md").write_text("/Users/private\n"),
                    "privacy")
        self.mutate(lambda root: (root / "build-metadata.json").write_text(
            '{"schemaVersion":2,"schemaVersion":2}\n'), "duplicate JSON key")

    def test_git_blob_digest_and_changed_set_are_enforced(self):
        def alter_digest(root):
            rows = (root / "source-inventory.txt").read_text().splitlines()
            rows[0] = rows[0].replace(rows[0].split()[1], "f" * 64, 1)
            (root / "source-inventory.txt").write_text("\n".join(sorted(rows)) + "\n")
        self.mutate(alter_digest, "Git source inventory")
        self.mutate(lambda root: (root / "source-inventory.txt").write_text(
            (root / "source-inventory.txt").read_text().splitlines()[0] + "\n"),
                    "Git source inventory")

    def test_base_is_pinned_and_deleted_blobs_are_required(self):
        def shorten_base(root):
            path = root / "build-metadata.json"
            metadata = json.loads(path.read_text())
            metadata["baseSha"] = metadata["implementationSha"]
            self.write_json(path, metadata)
        self.mutate(shorten_base, "immutable INT-008 base")

        def omit_deletion(root):
            rows = [row for row in (root / "source-inventory.txt").read_text().splitlines()
                    if not row.startswith("D ")]
            (root / "source-inventory.txt").write_text("\n".join(rows) + "\n")
        self.mutate(omit_deletion, "Git source inventory")

    def test_post_capture_code_is_rejected_but_evidence_only_commit_is_allowed(self):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.make_fixture(Path(raw))
            evidence_note = repository / "docs/verification/evidence/2026-08-29-int-008/note.txt"
            evidence_note.parent.mkdir(parents=True)
            evidence_note.write_text("retained\n", encoding="utf-8")
            self.git(repository, "add", ".")
            self.git(repository, "commit", "-qm", "evidence")
            VERIFY.validate(root, repository, self.base_sha)

            (repository / "production.txt").write_text("late code\n", encoding="utf-8")
            self.git(repository, "add", "production.txt")
            self.git(repository, "commit", "-qm", "late production")
            with self.assertRaisesRegex(ValueError, "post-capture"):
                VERIFY.validate(root, repository, self.base_sha)

    def test_log_rows_must_equal_jsonl_rows_byte_for_byte(self):
        def drift_log(root):
            path = root / "logs/gametest.log"
            path.write_text(path.read_text().replace('"controlTicks":1200',
                                                     '"controlTicks":1199', 1))
        self.mutate(drift_log, "log rows")

    def test_metadata_totals_must_exactly_match_machine_readable_summaries(self):
        def drift_metadata(root):
            path = root / "build-metadata.json"
            metadata = json.loads(path.read_text())
            metadata["junitTests"] += 1
            self.write_json(path, metadata)
        self.mutate(drift_metadata, "test summary mismatch")

        def drift_summary(root):
            path = root / "logs/python-summary.json"
            summary = json.loads(path.read_text())
            summary["tests"] -= 1
            self.write_json(path, summary)
        self.mutate(drift_summary, "test summary mismatch")

    def test_test_summaries_require_exact_sha_and_zero_nonpasses(self):
        def mixed_sha(root):
            path = root / "logs/junit-summary.json"
            summary = json.loads(path.read_text())
            summary["implementationSha"] = "b" * 40
            self.write_json(path, summary)
        self.mutate(mixed_sha, "test summary mismatch")

        def failed_test(root):
            path = root / "logs/python-summary.json"
            summary = json.loads(path.read_text())
            summary["failures"] = 1
            self.write_json(path, summary)
        self.mutate(failed_test, "test summary mismatch")

    def test_totals_are_recomputed_from_retained_machine_outputs(self):
        def drift_junit_xml(root):
            path = root / "logs/junit/TEST-second.xml"
            path.write_text(path.read_text().replace('tests="25"', 'tests="24"'),
                            encoding="utf-8")
        self.mutate(drift_junit_xml, "JUnit raw results")

        def drift_python_output(root):
            path = root / "logs/aggregate-check.log"
            path.write_text(path.read_text().replace("Ran 226 tests", "Ran 225 tests"),
                            encoding="utf-8")
        self.mutate(drift_python_output, "Python raw results")

        def remove_checkout_proof(root):
            path = root / "logs/gametest.log"
            path.write_text("\n".join(path.read_text().splitlines()[1:]) + "\n",
                            encoding="utf-8")
        self.mutate(remove_checkout_proof, "checkout verification")

    def test_aggregate_requires_matching_clean_preflight_and_postflight_receipts(self):
        def wrong_preflight(root):
            path = root / "logs/aggregate-check.log"
            path.write_text(path.read_text().replace(
                "aggregate preflight verified: ",
                "aggregate preflight verified: " + "b" * 40 + "; ignored=" , 1),
                encoding="utf-8")
        self.mutate(wrong_preflight, "aggregate checkout")

        def missing_postflight(root):
            path = root / "logs/aggregate-check.log"
            lines = [line for line in path.read_text().splitlines()
                     if "aggregate postflight verified" not in line]
            path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        self.mutate(missing_postflight, "aggregate checkout")

    def test_aggregate_binds_the_exact_raw_junit_inventory(self):
        def preserve_totals_but_change_xml(root):
            path = root / "logs/junit/TEST-first.xml"
            path.write_text(path.read_text().replace('name="first"', 'name="altered"'),
                            encoding="utf-8")
        self.mutate(preserve_totals_but_change_xml, "JUnit capture digest")

    def test_aggregate_requires_executed_full_gate_and_gametests(self):
        for removed in ("> Task :runGameTest", "> Task :test", "> Task :check",
                        "> Task :auditJavaSources", "All 166 required tests passed :)"):
            with self.subTest(removed=removed):
                def strip(root):
                    path = root / "logs/aggregate-check.log"
                    path.write_text("\n".join(line for line in path.read_text().splitlines()
                                               if line != removed) + "\n")
                self.mutate(strip, "aggregate execution")

    def test_aggregate_rejects_cached_or_skipped_gate_tasks(self):
        for suffix in (" UP-TO-DATE", " FROM-CACHE", " SKIPPED"):
            with self.subTest(suffix=suffix):
                def cached(root):
                    path = root / "logs/aggregate-check.log"
                    path.write_text(path.read_text().replace("> Task :test\n",
                                                            "> Task :test" + suffix + "\n"))
                self.mutate(cached, "aggregate execution")

    def test_matching_transcript_and_rows_cannot_contradict_measured_clocks(self):
        for case, control, world in (("crystal-control-deadline", 1, 0),
                                     ("crystal-control-deadline", 1200, 99),
                                     ("world-managers-paused", 0, 1),
                                     ("projectile-pause-resume", 4, 4),
                                     ("projectile-pause-resume", 8, 0)):
            with self.subTest(case=case, control=control, world=world):
                def contradict(root):
                    path = root / "temporal-assertions.jsonl"
                    lines = path.read_text().splitlines()
                    log_path = root / "logs/gametest.log"
                    log = log_path.read_text()
                    for index, line in enumerate(lines):
                        row = json.loads(line)
                        if row["case"] != case:
                            continue
                        row.update(controlTicks=control, worldTicks=world)
                        lines[index] = json.dumps(row, separators=(",", ":"))
                        log = log.replace(line, lines[index])
                    path.write_text("\n".join(lines) + "\n")
                    log_path.write_text(log)
                self.mutate(contradict, "measured clock")

    def test_matching_numeric_facts_cannot_impersonate_booleans_or_integers(self):
        cases = (("admin-preservation", "acquired", 0),
                 ("admin-preservation", "vanillaFrozen", 1),
                 ("external-supersession", "superseded", 1.0),
                 ("crystal-control-deadline", "activeAt1199", 1),
                 ("crystal-control-deadline", "duration", 1200.0),
                 ("world-managers-paused", "energyMutated", 0),
                 ("lifecycle-cleanup", "deathReleased", 1))
        for case, field, value in cases:
            with self.subTest(case=case, field=field, value=value):
                def impersonate(root):
                    path = root / "temporal-assertions.jsonl"
                    lines = path.read_text().splitlines()
                    log_path = root / "logs/gametest.log"
                    log = log_path.read_text()
                    for index, line in enumerate(lines):
                        row = json.loads(line)
                        if row["case"] != case:
                            continue
                        row["facts"][field] = value
                        lines[index] = json.dumps(row, separators=(",", ":"))
                        log = log.replace(line, lines[index])
                    path.write_text("\n".join(lines) + "\n")
                    log_path.write_text(log)
                self.mutate(impersonate, "fact type")


if __name__ == "__main__":
    unittest.main()
