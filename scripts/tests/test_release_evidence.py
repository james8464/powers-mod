#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "scripts"
sys.path.insert(0, str(SCRIPTS))
from release_contract import EvidenceRow, ReleaseContractError

MODULE_PATH = SCRIPTS / "release_evidence.py"
SPEC = importlib.util.spec_from_file_location("release_evidence", MODULE_PATH)
EVIDENCE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(EVIDENCE)


COMMIT = "a" * 40


class ReleaseEvidenceTest(unittest.TestCase):
    def row(
            self, path: Path, kind: str, validator: str,
            result: dict[str, object], limitations: tuple[str, ...] = ()) -> EvidenceRow:
        data = path.read_bytes()
        return EvidenceRow(
            kind + "-fixture", kind, validator, path.name,
            hashlib.sha256(data).hexdigest(), len(data), COMMIT,
            ("fixture", kind), result, limitations)

    def write_json(self, path: Path, value: object) -> None:
        path.write_text(json.dumps(value), encoding="utf-8")

    def validate(self, row: EvidenceRow, path: Path) -> dict[str, object]:
        return EVIDENCE.validate_evidence(row, path, COMMIT)

    def test_junit_requires_consistent_zero_failure_error_and_skip_totals(self):
        good = (
            '<testsuites tests="2" failures="0" errors="0" skipped="0">'
            '<testsuite name="one" tests="1" failures="0" errors="0" skipped="0"/>'
            '<testsuite name="two" tests="1" failures="0" errors="0" skipped="0"/>'
            '</testsuites>')
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "junit.xml"
            path.write_text(good, encoding="utf-8")
            row = self.row(path, "junit", "junit-xml", {
                "tests": 2, "failures": 0, "errors": 0, "skipped": 0,
            })
            self.assertEqual(2, self.validate(row, path)["tests"])

            for replacement, expected in (
                    (('skipped="0"', 'skipped="1"', 1), "skipped"),
                    (('failures="0"', 'failures="1"', 0), "failures"),
                    (('tests="2"', 'tests="3"', 0), "inconsistent")):
                old, new, occurrence = replacement
                broken = good.replace(old, new, occurrence + 1)
                path.write_text(broken, encoding="utf-8")
                bad = self.row(path, "junit", "junit-xml", row.result)
                with self.assertRaisesRegex(ReleaseContractError, expected):
                    self.validate(bad, path)
            path.write_text("<testsuites>", encoding="utf-8")
            with self.assertRaisesRegex(ReleaseContractError, "XML"):
                self.validate(self.row(path, "junit", "junit-xml", row.result), path)

    def test_fabric_log_requires_exact_total_success_marker_and_no_errors(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "gametest.log"
            good = "Running 127 tests\nAll 127 required tests passed :)\nBUILD SUCCESSFUL\n"
            path.write_text(good, encoding="utf-8")
            result = {"requiredTests": 127}
            accepted = self.validate(
                self.row(path, "fabric-log", "fabric-log", result), path)
            self.assertEqual(127, accepted["requiredTests"])
            for suffix, expected in (
                    ("1 required tests failed\n", "failed required"),
                    ("[Server thread/ERROR] broken\n", "server error"),
                    ("", "required-test success marker")):
                content = good if suffix else good.replace("All 127 required tests passed :)\n", "")
                path.write_text(content + suffix, encoding="utf-8")
                with self.assertRaisesRegex(ReleaseContractError, expected):
                    self.validate(self.row(path, "fabric-log", "fabric-log", result), path)
            path.write_text(
                good.replace("Running 127 tests", "Running 126 tests"), encoding="utf-8")
            with self.assertRaisesRegex(ReleaseContractError, "exact required-test total"):
                self.validate(self.row(path, "fabric-log", "fabric-log", result), path)

    def soak(self) -> dict[str, object]:
        cycles = []
        for number in range(1, 289):
            cycles.append({
                "cycle": number,
                "shutdown_mode": "sigterm" if number % 12 == 0 else "clean",
                "exit_code": 143 if number % 12 == 0 else 0,
                "ready": True,
                "client_connected": True,
                "client_disconnected": True,
                "startup_verified": True,
                "seeded": True,
                "settled": True,
                "status_verified": True,
                "rollover_seeded": True,
                "clean_diagnostics": True,
                "error_lines": [],
            })
        return {
            "schema": 3,
            "git_commit": COMMIT,
            "requested_hours": 24.0,
            "cycle_seconds": 300,
            "requested_cycles": 288,
            "completed_cycles": 288,
            "connected_workload_seconds": 77_760.0,
            "elapsed_seconds": 86_400.0,
            "cycles": cycles,
            "status": "passed",
            "passed": True,
            "failure": "",
        }

    def test_restart_soak_requires_final_24_hour_cycle_and_lifecycle_contract(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "soak.json"
            good = self.soak()
            self.write_json(path, good)
            result = {"expectedCycles": 288, "minimumSeconds": 86_400}
            accepted = self.validate(
                self.row(path, "restart-soak", "restart-soak", result), path)
            self.assertEqual(288, accepted["completedCycles"])
            cases = (
                ("elapsed_seconds", 86_399, "86400"),
                ("passed", False, "passed"),
                ("failure", "timeout", "failure"),
                ("completed_cycles", 287, "completed cycles"),
                ("git_commit", "b" * 40, "commit"),
            )
            for field, value, expected in cases:
                with self.subTest(field=field):
                    broken = json.loads(json.dumps(good))
                    broken[field] = value
                    self.write_json(path, broken)
                    with self.assertRaisesRegex(ReleaseContractError, expected):
                        self.validate(self.row(path, "restart-soak", "restart-soak", result), path)
            for field in ("client_disconnected", "clean_diagnostics", "rollover_seeded"):
                with self.subTest(field=field):
                    broken = json.loads(json.dumps(good))
                    broken["cycles"][11][field] = False
                    self.write_json(path, broken)
                    with self.assertRaisesRegex(ReleaseContractError, field):
                        self.validate(self.row(path, "restart-soak", "restart-soak", result), path)

    def test_real_client_profiles_require_10_50_100_for_1800_seconds(self):
        good = {
            "schemaVersion": 1,
            "commit": COMMIT,
            "passed": True,
            "profiles": [
                {"clients": clients, "durationSeconds": 1800, "actorType": "real-client", "errors": 0}
                for clients in (10, 50, 100)
            ],
        }
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "profiles.json"
            result = {"clients": [10, 50, 100], "minimumDurationSeconds": 1800}
            self.write_json(path, good)
            accepted = self.validate(
                self.row(path, "profiles", "real-client-profiles", result), path)
            self.assertEqual([10, 50, 100], accepted["clients"])
            for mutation, expected in (
                    ((1, "durationSeconds", 1799), "duration"),
                    ((0, "actorType", "embedded"), "real-client"),
                    ((2, "errors", 1), "errors")):
                index, field, value = mutation
                broken = json.loads(json.dumps(good))
                broken["profiles"][index][field] = value
                self.write_json(path, broken)
                with self.assertRaisesRegex(ReleaseContractError, expected):
                    self.validate(self.row(path, "profiles", "real-client-profiles", result), path)
            broken = json.loads(json.dumps(good))
            broken["profiles"].pop()
            self.write_json(path, broken)
            with self.assertRaisesRegex(ReleaseContractError, "10/50/100"):
                self.validate(self.row(path, "profiles", "real-client-profiles", result), path)

    def test_compatibility_requires_exact_artifacts_counts_and_limitations(self):
        artifacts = [{
            "id": "sodium", "versionId": "0.8.6", "size": 123,
            "sha256": "c" * 64,
        }]
        good = {
            "schemaVersion": 1,
            "commit": COMMIT,
            "passed": True,
            "requiredTests": 115,
            "passedTests": 115,
            "artifacts": artifacts,
            "limitations": ["Microphone audio was not captured."],
        }
        result = {"requiredTests": 115, "artifacts": artifacts}
        limitations = ("Microphone audio was not captured.",)
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "compat.json"
            self.write_json(path, good)
            accepted = self.validate(
                self.row(path, "compatibility", "compatibility", result, limitations), path)
            self.assertEqual(115, accepted["passedTests"])
            for field, value, expected in (
                    ("passedTests", 114, "test count"),
                    ("commit", "d" * 40, "commit"),
                    ("artifacts", [], "artifacts"),
                    ("limitations", [], "limitations")):
                broken = json.loads(json.dumps(good))
                broken[field] = value
                self.write_json(path, broken)
                with self.assertRaisesRegex(ReleaseContractError, expected):
                    self.validate(
                        self.row(path, "compatibility", "compatibility", result, limitations), path)

    def review_bundle(self, directory: Path, *, metadata_source: str = "client-emitted") -> Path:
        raw = directory / "raw"
        raw.mkdir(exist_ok=True)
        frame = raw / "frame.png"
        frame.write_bytes(b"retained raw frame")
        path = directory / "review.json"
        self.write_json(path, {
            "schemaVersion": 1,
            "commit": COMMIT,
            "passed": True,
            "decisions": [{
                "id": "frame",
                "decision": "PASS",
                "rawPath": "raw/frame.png",
                "sha256": hashlib.sha256(frame.read_bytes()).hexdigest(),
                "metadataSource": metadata_source,
                "runtime": {
                    "width": 1280,
                    "height": 720,
                    "requestedGuiScale": 4,
                    "effectiveGuiScale": 3,
                    "mipmapLevel": 4,
                    "reducedMotion": False,
                    "graphicsMode": "fancy",
                    "renderDistance": 12,
                    "resourcePacks": ["vanilla", "powers"],
                    "gameTime": 6000,
                    "weather": "clear",
                },
            }],
        })
        return path

    def test_manual_and_visual_review_require_retained_digest_bound_raw_bytes(self):
        for kind, validator in (("manual", "manual-review"), ("visual", "visual-review")):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as raw:
                directory = Path(raw)
                path = self.review_bundle(directory)
                result = {"decisions": 1}
                accepted = self.validate(self.row(path, kind, validator, result), path)
                self.assertEqual(1, accepted["decisions"])
                (directory / "raw/frame.png").write_bytes(b"changed")
                with self.assertRaisesRegex(ReleaseContractError, "raw SHA-256"):
                    self.validate(self.row(path, kind, validator, result), path)

                path = self.review_bundle(directory, metadata_source="reconstructed")
                with self.assertRaisesRegex(ReleaseContractError, "client-emitted"):
                    self.validate(self.row(path, kind, validator, result), path)
                path = self.review_bundle(directory)
                bundle = json.loads(path.read_text(encoding="utf-8"))
                bundle["decisions"][0].pop("rawPath")
                self.write_json(path, bundle)
                with self.assertRaisesRegex(ReleaseContractError, "rawPath"):
                    self.validate(self.row(path, kind, validator, result), path)

    def test_review_validation_carries_the_exact_retained_raw_snapshot(self):
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            path = self.review_bundle(directory)
            row = self.row(
                path, "visual", "visual-review", {"decisions": 1})
            snapshots = []
            accepted = EVIDENCE.validate_evidence(
                row, path, COMMIT, raw_snapshots=snapshots)
            self.assertEqual(1, accepted["decisions"])
            self.assertEqual(1, len(snapshots))
            (directory / "raw/frame.png").write_bytes(b"replacement frame")
            with self.assertRaisesRegex(ReleaseContractError, "changed after validation"):
                EVIDENCE.recheck_regular_snapshot(snapshots[0])

    def test_generic_typed_families_require_exact_commit_and_passed_state(self):
        fixtures = (
            ("packet-fault", "packet-fault", {"profiles": 6, "clientConverged": True}),
            ("migration", "migration", {"cases": 3}),
            ("manifest", "manifest", {"entries": 970, "stale": 0}),
            ("four-client", "four-client", {"clients": 4, "joined": 4, "disconnected": 4}),
            ("github-ci", "github-ci", {"conclusion": "success"}),
        )
        for kind, validator, typed in fixtures:
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as raw:
                path = Path(raw) / "typed.json"
                data = {"schemaVersion": 1, "commit": COMMIT, "passed": True, **typed}
                self.write_json(path, data)
                accepted = self.validate(self.row(path, kind, validator, typed), path)
                self.assertTrue(accepted["passed"])
                data["passed"] = False
                self.write_json(path, data)
                with self.assertRaisesRegex(ReleaseContractError, "passed"):
                    self.validate(self.row(path, kind, validator, typed), path)
                data["passed"] = True
                data["commit"] = "b" * 40
                self.write_json(path, data)
                with self.assertRaisesRegex(ReleaseContractError, "commit"):
                    self.validate(self.row(path, kind, validator, typed), path)

    def test_limitations_are_stable_nonblank_and_preserved_verbatim(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "limitations.json"
            data = {
                "schemaVersion": 1,
                "commit": COMMIT,
                "limitations": [{"id": "voice-audio", "text": "Microphone audio was not captured."}],
            }
            self.write_json(path, data)
            limitations = ("Microphone audio was not captured.",)
            accepted = self.validate(self.row(
                path, "limitations", "limitations", {"count": 1}, limitations), path)
            self.assertEqual(list(limitations), accepted["limitations"])
            for field, value in (("id", "../bad"), ("text", " ")):
                broken = json.loads(json.dumps(data))
                broken["limitations"][0][field] = value
                self.write_json(path, broken)
                with self.assertRaises(ReleaseContractError):
                    self.validate(self.row(
                        path, "limitations", "limitations", {"count": 1}, limitations), path)

    def test_outer_identity_is_checked_before_dispatch(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "typed.json"
            self.write_json(path, {
                "schemaVersion": 1, "commit": COMMIT, "passed": True,
                "cases": 1,
            })
            row = self.row(path, "migration", "migration", {"cases": 1})
            with self.assertRaisesRegex(ReleaseContractError, "expected commit"):
                EVIDENCE.validate_evidence(row, path, "b" * 40)
            path.write_text("changed", encoding="utf-8")
            with self.assertRaisesRegex(ReleaseContractError, "(size|SHA-256)"):
                self.validate(row, path)

    def test_head_token_resolves_only_against_verified_expected_commit(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "migration.json"
            self.write_json(path, {
                "schemaVersion": 1, "commit": "@HEAD", "passed": True, "cases": 1,
            })
            data = path.read_bytes()
            row = EvidenceRow(
                "migration-fixture", "migration", "migration", path.name,
                hashlib.sha256(data).hexdigest(), len(data), "@HEAD",
                ("fixture",), {"cases": 1}, ())
            accepted = EVIDENCE.validate_evidence(row, path, COMMIT)
            self.assertTrue(accepted["passed"])

    def test_privacy_is_checked_on_the_exact_semantically_validated_bytes(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "migration.json"
            self.write_json(path, {
                "schemaVersion": 1,
                "commit": COMMIT,
                "passed": True,
                "cases": 1,
                "diagnostic": "/Users/alice/private.log",
            })
            row = self.row(path, "migration", "migration", {"cases": 1})
            with self.assertRaisesRegex(ReleaseContractError, "unowned absolute path"):
                self.validate(row, path)


if __name__ == "__main__":
    unittest.main()
