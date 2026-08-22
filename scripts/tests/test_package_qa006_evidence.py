#!/usr/bin/env python3

import gzip
import importlib.util
import io
import json
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))
SPEC = importlib.util.spec_from_file_location(
    "package_qa006_evidence", ROOT / "scripts/package_qa006_evidence.py")
PACKAGE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(PACKAGE)


class Qa006EvidencePackagerTest(unittest.TestCase):
    def make_run(self, root: Path, *, cycles: int = 288) -> Path:
        source = root / "restart-soak"
        server_logs = source / "server-logs"
        client_logs = source / "client-logs"
        server_logs.mkdir(parents=True)
        client_logs.mkdir()
        rows = []
        for cycle in range(1, cycles + 1):
            mode = "sigterm" if cycle % 12 == 0 else "clean"
            exit_code = 143 if mode == "sigterm" else 0
            server_name = f"server-logs/cycle-{cycle:04d}.log"
            client_name = f"client-logs/cycle-{cycle:04d}.log"
            server = "\n".join((
                "Source=file:/Users/private/.gradle/example.jar",
                "Done (1.0s)! For help, type help",
                "SoakClient[/127.0.0.1:54321] logged in",
                "SoakClient joined the game",
                f"POWERS_SOAK_VERIFY cycle={cycle} passed=true detail=recoveredRuin=true",
                f"POWERS_SOAK_SEED cycle={cycle} passed=true detail=systems=8",
                f"POWERS_SOAK_SETTLED cycle={cycle} passed=true detail=clean",
                f"POWERS_SOAK_STATUS cycle={cycle} passed=true detail=travelResolved=true; rollover=false",
                f"POWERS_SOAK_ROLLOVER cycle={cycle} passed=true detail=persistedRuin=true",
                f"POWERS_SOAK_STATUS cycle={cycle} passed=true detail=travelResolved=true; rollover=true",
                "proxies=0; travelLoads=0; celestialEvents=1; forcedChunks=0",
                "Saved the game",
                "SoakClient left the game",
                *(("BUILD SUCCESSFUL",) if mode == "clean" else ()),
            )) + "\n"
            client = "\n".join((
                "Connecting to 127.0.0.1:25565",
                "QA client role=restart-soak executed ACTIVATE [0] at connected tick 160",
            )) + "\n"
            (source / server_name).write_text(server)
            (source / client_name).write_text(client)
            rows.append({
                "cycle": cycle,
                "ready": True,
                "client_connected": True,
                "client_disconnected": True,
                "startup_verified": True,
                "seeded": True,
                "settled": True,
                "status_verified": True,
                "rollover_seeded": True,
                "clean_diagnostics": True,
                "client_ability_actions": 1,
                "connected_workload_seconds": 270.0,
                "shutdown_mode": mode,
                "exit_code": exit_code,
                "error_lines": [],
                "passed": True,
                "server_log": f"build/restart-soak/{server_name}",
                "client_log": f"build/restart-soak/{client_name}",
                "seconds": 300.0,
            })
        report = {
            "schema": 3,
            "git_commit": "a" * 40,
            "requested_hours": 24.0,
            "cycle_seconds": 300,
            "requested_cycles": 288,
            "completed_cycles": cycles,
            "connected_workload_seconds": cycles * 270.0,
            "elapsed_seconds": 86_500.0,
            "acceptance_window_started_epoch": 1_700_000_000.0,
            "cycles": rows,
            "status": "passed" if cycles == 288 else "running",
            "passed": cycles == 288,
            "failure": "",
            "runtime": "build/restart-soak/runtime",
        }
        (source / "restart-soak-report.json").write_text(json.dumps(report) + "\n")
        return source

    def test_partial_run_is_rejected_before_packaging(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root, cycles=287)
            with self.assertRaisesRegex(ValueError, "288 completed cycles"):
                PACKAGE.package_run(source, root / "evidence")

    def test_missing_lifecycle_marker_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            log = source / "server-logs/cycle-0042.log"
            log.write_text(log.read_text().replace("SoakClient left the game\n", ""))
            with self.assertRaisesRegex(ValueError, "client disconnect"):
                PACKAGE.package_run(source, root / "evidence")

    def test_real_shape_sigterm_cycle_does_not_require_gradle_footer(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            signal_log = source / "server-logs/cycle-0012.log"
            self.assertNotIn("BUILD SUCCESSFUL", signal_log.read_text())
            PACKAGE.package_run(source, root / "evidence")

    def test_report_schema_and_cadence_cannot_be_weakened(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            report_path = source / "restart-soak-report.json"
            report = json.loads(report_path.read_text())
            report["cycle_seconds"] = 60
            report["operatorNotes"] = "/Users/private/secret"
            report_path.write_text(json.dumps(report) + "\n")
            with self.assertRaisesRegex(ValueError, "unknown report field"):
                PACKAGE.package_run(source, root / "evidence")
            del report["operatorNotes"]
            report_path.write_text(json.dumps(report) + "\n")
            with self.assertRaisesRegex(ValueError, "300-second cadence"):
                PACKAGE.package_run(source, root / "evidence")

    def test_per_cycle_duration_and_connected_total_are_exact(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            report_path = source / "restart-soak-report.json"
            report = json.loads(report_path.read_text())
            report["cycles"][200]["seconds"] = 299.9
            report_path.write_text(json.dumps(report) + "\n")
            with self.assertRaisesRegex(ValueError, "full 300-second boundary"):
                PACKAGE.package_run(source, root / "evidence")
            report["cycles"][200]["seconds"] = 300.0
            report["connected_workload_seconds"] += 1.0
            report_path.write_text(json.dumps(report) + "\n")
            with self.assertRaisesRegex(ValueError, "connected workload total"):
                PACKAGE.package_run(source, root / "evidence")

    def test_short_connected_workload_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            report_path = source / "restart-soak-report.json"
            report = json.loads(report_path.read_text())
            report["cycles"][41]["connected_workload_seconds"] = 269.999
            report["connected_workload_seconds"] = round(sum(
                row["connected_workload_seconds"] for row in report["cycles"]), 3)
            report_path.write_text(json.dumps(report) + "\n")
            with self.assertRaisesRegex(ValueError, "270-second connected workload"):
                PACKAGE.package_run(source, root / "evidence")

    def test_second_status_and_coherent_final_diagnostics_are_required(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            log = source / "server-logs/cycle-0042.log"
            text = log.read_text()
            second = "POWERS_SOAK_STATUS cycle=42 passed=true detail=travelResolved=true; rollover=true\n"
            log.write_text(text.replace(second, ""))
            with self.assertRaisesRegex(ValueError, "ordered lifecycle"):
                PACKAGE.package_run(source, root / "evidence")

            source = self.make_run(root / "again")
            log = source / "server-logs/cycle-0042.log"
            text = log.read_text().replace(
                "proxies=0; travelLoads=0; celestialEvents=1; forcedChunks=0",
                "proxies=0\ntravelLoads=0\ncelestialEvents=1\nforcedChunks=0")
            log.write_text(text)
            with self.assertRaisesRegex(ValueError, "coherent final diagnostics"):
                PACKAGE.package_run(source, root / "evidence-again")

    def test_symlinked_log_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            log = source / "server-logs/cycle-0001.log"
            target = root / "external.log"
            target.write_text(log.read_text())
            log.unlink()
            log.symlink_to(target)
            with self.assertRaisesRegex(ValueError, "unsafe"):
                PACKAGE.package_run(source, root / "evidence")

    def test_archive_is_deterministic_complete_and_privacy_sanitized(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            first = root / "first"
            second = root / "second"
            PACKAGE.package_run(source, first)
            PACKAGE.package_run(source, second)
            archive = first / "restart-soak-logs.tar.gz"
            self.assertEqual(archive.read_bytes(), (second / archive.name).read_bytes())
            with tarfile.open(fileobj=io.BytesIO(gzip.decompress(archive.read_bytes()))) as bundle:
                members = bundle.getmembers()
                self.assertEqual(576, len(members))
                text = b"".join(bundle.extractfile(member).read() for member in members)
            self.assertNotIn(b"/Users/", text)
            self.assertNotIn(b"127.0.0.1", text)
            self.assertIn(b"<HOME>", text)
            self.assertIn(b"<LOOPBACK>", text)
            index = json.loads((first / "logs-index.json").read_text())
            self.assertEqual(576, len(index["logs"]))
            self.assertEqual(288, index["serverLogs"])
            self.assertEqual(288, index["clientLogs"])
            checksums = (first / "SHA256SUMS").read_text()
            for name in PACKAGE.OWNED_OUTPUTS - {"SHA256SUMS"}:
                self.assertIn(name, checksums)

    def test_unexpected_output_file_is_never_deleted_or_overwritten(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            destination = root / "evidence"
            destination.mkdir()
            (destination / "reviewer-notes.txt").write_text("owned externally")
            with self.assertRaisesRegex(ValueError, "unexpected output"):
                PACKAGE.package_run(source, destination)
            self.assertEqual("owned externally", (destination / "reviewer-notes.txt").read_text())

    def test_published_bundle_is_collectively_rechecked(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = self.make_run(root)
            destination = root / "evidence"
            original = PACKAGE.write_bytes_atomic

            def mutate_after_checksums(path, data):
                result = original(path, data)
                if path.name == "SHA256SUMS":
                    (path.parent / "README.md").write_text("mutated after publication\n")
                return result

            PACKAGE.write_bytes_atomic = mutate_after_checksums
            try:
                with self.assertRaisesRegex(ValueError, "published evidence changed"):
                    PACKAGE.package_run(source, destination)
            finally:
                PACKAGE.write_bytes_atomic = original


if __name__ == "__main__":
    unittest.main()
