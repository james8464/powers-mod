#!/usr/bin/env python3

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "verify_int008_temporal", ROOT / "scripts/verify_int008_temporal.py")
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)
SHA = "a" * 40


class Int008TemporalVerifierTest(unittest.TestCase):
    def make_fixture(self, root: Path) -> Path:
        rows = [
            self.row("admin-preservation", {"acquired": False, "leaseActive": False,
                                             "vanillaFrozen": True}),
            self.row("external-supersession", {"leaseActive": False, "superseded": True,
                                                "vanillaFrozen": True}),
            self.row("crystal-control-deadline", {"clock": "CONTROL", "duration": 1200}),
            self.row("world-managers-paused", {"celestialPaused": True,
                                                "channelsPaused": True,
                                                "energyMutated": False,
                                                "realmPaused": True,
                                                "worldAdvanced": False}),
            self.row("projectile-pause-resume", {"frozenDistanceSquared": 0.0,
                                                  "resumedDistanceSquared": 0.25}, 4, 4),
            self.row("lifecycle-cleanup", {"leaseActive": False,
                                           "matchingOwner": True,
                                           "vanillaFrozen": False}),
        ]
        self.write_jsonl(root / "temporal-assertions.jsonl", rows)
        self.write_json(root / "build-metadata.json", {
            "schemaVersion": 2, "task": "INT-008", "implementationSha": SHA,
            "result": "PENDING", "gameTests": 161, "junitTests": 1825,
            "pythonTests": 224,
        })
        (root / "source-inventory.txt").write_text(
            "0" * 64 + "  src/main/java/com/powers/power/state/GlobalTimeStopManager.java\n"
            + "1" * 64 + "  src/main/java/com/powers/time/TemporalClocks.java\n",
            encoding="utf-8")
        (root / "logs").mkdir()
        (root / "logs/gametest.log").write_text(
            "\n".join(row["case"] for row in rows)
            + "\nAll 161 required tests passed :)\n", encoding="utf-8")
        (root / "README.md").write_text("# INT-008 evidence\n", encoding="utf-8")
        return root

    @staticmethod
    def row(case, facts, control=0, world=0):
        return {"schemaVersion": 2, "implementationSha": SHA, "case": case,
                "result": "PASS", "controlTicks": control,
                "worldTicks": world, "facts": facts}

    @staticmethod
    def write_json(path, value):
        path.write_text(json.dumps(value, sort_keys=True) + "\n", encoding="utf-8")

    @staticmethod
    def write_jsonl(path, rows):
        path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
                        encoding="utf-8")

    def mutate(self, operation, message):
        with tempfile.TemporaryDirectory() as raw:
            root = self.make_fixture(Path(raw))
            operation(root)
            with self.assertRaisesRegex(ValueError, message):
                VERIFY.validate(root)

    def test_complete_exact_sha_evidence_passes(self):
        with tempfile.TemporaryDirectory() as raw:
            result = VERIFY.validate(self.make_fixture(Path(raw)))
            self.assertEqual(6, result["caseCount"])
            self.assertEqual(SHA, result["implementationSha"])

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
            self.write_jsonl(root / "temporal-assertions.jsonl", rows)
        self.mutate(mix, "implementation")

    def test_false_temporal_fact_is_rejected(self):
        def drift(root):
            rows = [json.loads(line) for line in
                    (root / "temporal-assertions.jsonl").read_text().splitlines()]
            rows[3]["facts"]["worldAdvanced"] = True
            self.write_jsonl(root / "temporal-assertions.jsonl", rows)
        self.mutate(drift, "world manager")

    def test_private_content_and_duplicate_keys_are_rejected(self):
        self.mutate(lambda root: (root / "README.md").write_text("/Users/private\n"),
                    "privacy")
        self.mutate(lambda root: (root / "build-metadata.json").write_text(
            '{"schemaVersion":2,"schemaVersion":2}\n'), "duplicate JSON key")

    def test_unsorted_source_inventory_is_rejected(self):
        def reverse(root):
            path = root / "source-inventory.txt"
            path.write_text("\n".join(reversed(path.read_text().splitlines())) + "\n")
        self.mutate(reverse, "source inventory")


if __name__ == "__main__":
    unittest.main()
