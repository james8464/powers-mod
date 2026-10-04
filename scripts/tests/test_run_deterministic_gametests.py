#!/usr/bin/env python3

import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "run_deterministic_gametests", ROOT / "scripts/run_deterministic_gametests.py")
RUNNER = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(RUNNER)


class DeterministicGameTestRunnerTest(unittest.TestCase):
    def test_discovers_sorted_server_gametests_and_ignores_client_sources(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            server = root / "src/gametest/java/com/example/AlphaGameTests.java"
            client = root / "src/gametest/java/com/example/ClientGameTests.java"
            server.parent.mkdir(parents=True)
            server.write_text("""
                package com.example;
                import net.fabricmc.fabric.api.gametest.v1.GameTest;
                public final class AlphaGameTests {
                    @GameTest public void zebraCase() {}
                    @GameTest public void alphaCase() {}
                }
                """, encoding="utf-8")
            manifest = root / "src/gametest/resources/fabric.mod.json"
            manifest.parent.mkdir(parents=True)
            manifest.write_text("""
                {"entrypoints": {"fabric-gametest": [
                  "com.example.AlphaGameTests"
                ], "fabric-client-gametest": []}}
                """, encoding="utf-8")
            client.write_text("""
                import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
                public final class ClientGameTests implements FabricClientGameTest {}
                """, encoding="utf-8")

            self.assertEqual([
				"minecraft:always_pass",
                "powers-gametest:alpha_game_tests_alpha_case",
                "powers-gametest:alpha_game_tests_zebra_case",
            ], RUNNER.discover_test_ids(root))

    def test_repository_inventory_matches_the_registered_167_server_tests(self):
        self.assertEqual(167, len(RUNNER.discover_test_ids(ROOT)))

    def test_identifier_conversion_matches_fabric_lower_to_upper_boundaries(self):
        self.assertEqual("p1acceptance_game_tests",
                         RUNNER.snake_case("P1AcceptanceGameTests"))
        self.assertEqual("accepts_aplayer", RUNNER.snake_case("acceptsAPlayer"))

    def test_duplicate_discovered_ids_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/gametest/java/com/example/DuplicateGameTests.java"
            source.parent.mkdir(parents=True)
            source.write_text("""
                import net.fabricmc.fabric.api.gametest.v1.GameTest;
                public final class DuplicateGameTests {
                    @GameTest public void sameCase() {}
                    @GameTest public void sameCase() {}
                }
                """, encoding="utf-8")
            manifest = root / "src/gametest/resources/fabric.mod.json"
            manifest.parent.mkdir(parents=True)
            manifest.write_text("""
                {"entrypoints": {"fabric-gametest": [
                  "com.example.DuplicateGameTests"
                ], "fabric-client-gametest": []}}
                """, encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate"):
                RUNNER.discover_test_ids(root)

    def test_failure_stops_following_children_and_retains_raw_log(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            logs = root / "logs"
            first = subprocess.CompletedProcess([], 1, "raw failure", "")
            with mock.patch.object(RUNNER.subprocess, "run", return_value=first) as run:
                with self.assertRaisesRegex(RuntimeError, "first_case"):
                    RUNNER.run_tests(root, ["powers-gametest:first_case",
                                            "powers-gametest:second_case"], logs)

            self.assertEqual(1, run.call_count)
            self.assertEqual("raw failure", (logs / "001-first_case.log").read_text())

    def test_success_returns_complete_sorted_inventory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            logs = root / "logs"
            success = subprocess.CompletedProcess([], 0, "raw success", "")
            with mock.patch.object(RUNNER.subprocess, "run", return_value=success):
                passed = RUNNER.run_tests(root, ["powers-gametest:z_case",
                                                 "powers-gametest:a_case"], logs)

            self.assertEqual(["powers-gametest:a_case", "powers-gametest:z_case"], passed)
            self.assertTrue((logs / "001-a_case.log").is_file())
            self.assertTrue((logs / "002-z_case.log").is_file())

    def test_gradle_contract_registers_authoritative_task_and_check_dependency(self):
        build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        self.assertIn('tasks.register("runDeterministicGameTests", Exec)', build)
        self.assertIn('dependsOn("runDeterministicGameTests")', build)
        self.assertIn('dependency != "runGameTest"', build)
        self.assertIn('deterministicGameTestFilter', build)


if __name__ == "__main__":
    unittest.main()
