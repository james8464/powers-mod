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
        self.git(repository, "add", "clock.txt")
        self.git(repository, "commit", "-qm", "base")
        base_sha = self.git(repository, "rev-parse", "HEAD")
        (repository / "clock.txt").write_text("implementation\n", encoding="utf-8")
        (repository / "lease.txt").write_text("owned\n", encoding="utf-8")
        self.git(repository, "add", "clock.txt", "lease.txt")
        self.git(repository, "commit", "-qm", "implementation")
        implementation_sha = self.git(repository, "rev-parse", "HEAD")
        return repository, base_sha, implementation_sha

    @staticmethod
    def git(repository: Path, *arguments: str) -> str:
        return subprocess.check_output(
            ["git", *arguments], cwd=repository, text=True).strip()

    def make_fixture(self, parent: Path) -> tuple[Path, Path]:
        repository, base_sha, implementation_sha = self.make_repository(parent)
        root = parent / "evidence"
        root.mkdir()
        rows = [
            self.row(implementation_sha, "admin-preservation",
                     {"acquired": False, "leaseActive": False, "vanillaFrozen": True}),
            self.row(implementation_sha, "external-supersession",
                     {"leaseActive": False, "superseded": True, "vanillaFrozen": True}),
            self.row(implementation_sha, "crystal-control-deadline",
                     {"clock": "CONTROL", "duration": 1200}, 1200, 0),
            self.row(implementation_sha, "world-managers-paused",
                     {"celestialPaused": True, "channelsPaused": True,
                      "energyMutated": False, "realmPaused": True,
                      "worldAdvanced": False}),
            self.row(implementation_sha, "projectile-pause-resume",
                     {"frozenDistanceSquared": 0.0,
                      "resumedDistanceSquared": 0.25}, 4, 4),
            self.row(implementation_sha, "lifecycle-cleanup",
                     {"leaseActive": False, "matchingOwner": True,
                      "vanillaFrozen": False}),
        ]
        encoded_rows = [json.dumps(row, separators=(",", ":")) for row in rows]
        (root / "temporal-assertions.jsonl").write_text(
            "\n".join(encoded_rows) + "\n", encoding="utf-8")
        self.write_json(root / "build-metadata.json", {
            "schemaVersion": 2, "task": "INT-008", "baseSha": base_sha,
            "implementationSha": implementation_sha, "result": "PENDING",
            "gameTests": 161, "junitTests": 1825, "pythonTests": 226,
        })
        inventory = []
        for relative in ("clock.txt", "lease.txt"):
            blob = subprocess.check_output(
                ["git", "show", f"{implementation_sha}:{relative}"], cwd=repository)
            inventory.append(f"{hashlib.sha256(blob).hexdigest()}  {relative}")
        (root / "source-inventory.txt").write_text(
            "\n".join(sorted(inventory)) + "\n", encoding="utf-8")
        (root / "logs").mkdir()
        (root / "logs/gametest.log").write_text(
            "\n".join("INT008_TEMPORAL " + row for row in encoded_rows)
            + "\nAll 161 required tests passed :)\n", encoding="utf-8")
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
                VERIFY.validate(root, repository)

    def test_complete_exact_sha_evidence_passes(self):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.make_fixture(Path(raw))
            result = VERIFY.validate(root, repository)
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
            rows[0] = "f" * 64 + rows[0][64:]
            (root / "source-inventory.txt").write_text("\n".join(sorted(rows)) + "\n")
        self.mutate(alter_digest, "Git source inventory")
        self.mutate(lambda root: (root / "source-inventory.txt").write_text(
            (root / "source-inventory.txt").read_text().splitlines()[0] + "\n"),
                    "Git source inventory")

    def test_log_rows_must_equal_jsonl_rows_byte_for_byte(self):
        def drift_log(root):
            path = root / "logs/gametest.log"
            path.write_text(path.read_text().replace('"controlTicks":1200',
                                                     '"controlTicks":1199', 1))
        self.mutate(drift_log, "log rows")


if __name__ == "__main__":
    unittest.main()
