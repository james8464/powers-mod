#!/usr/bin/env python3

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "restart_soak", ROOT / "scripts/restart_soak.py")
SOAK = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(SOAK)


class RestartSoakPolicyTest(unittest.TestCase):
    def test_hourly_cycle_uses_flushed_sigterm(self):
        self.assertEqual("clean", SOAK.shutdown_mode(1))
        self.assertEqual("clean", SOAK.shutdown_mode(11))
        self.assertEqual("sigterm", SOAK.shutdown_mode(12))
        self.assertEqual("sigterm", SOAK.shutdown_mode(24))

    def test_failure_resets_the_acceptance_window(self):
        self.assertEqual(120.0, SOAK.accepted_window_start(120.0, True, 50.0))
        self.assertEqual(50.0, SOAK.accepted_window_start(120.0, False, 50.0))

    def test_client_identity_and_script_are_explicit(self):
        command = SOAK.client_command(
            Path("/jdk/bin/java"), Path("/launch.cfg"), Path("/runClient"),
            Path("/runtime/client"), Path("/restart-soak-client.tsv"))
        rendered = " ".join(str(part) for part in command)
        self.assertIn("SoakClient", rendered)
        self.assertIn("powers.qa.server=127.0.0.1:25565", rendered)
        self.assertIn("powers.qa.script=/restart-soak-client.tsv", rendered)
        self.assertIn("--gameDir /runtime/client", rendered)

    def test_each_reconnect_gets_a_deterministic_open_summoning_arena(self):
        commands = SOAK.arena_setup_commands()
        self.assertEqual(
            "execute in minecraft:overworld run teleport SoakClient 0.5 100 0.5 0 0",
            commands[0])
        self.assertIn("minecraft:air", commands[1])
        self.assertIn("minecraft:stone", commands[2])

    def test_duration_is_an_exact_number_of_complete_cycles(self):
        self.assertEqual(288, SOAK.required_cycle_count(24 * 3600, 300))
        self.assertEqual(4, SOAK.required_cycle_count(36, 10))
        self.assertEqual(30, SOAK.rollover_lead_seconds(300))
        self.assertEqual(5, SOAK.rollover_lead_seconds(10))

    def test_each_cycle_waits_for_its_full_wall_clock_boundary(self):
        self.assertEqual(18.0, SOAK.cycle_boundary_wait_seconds(100.0, 382.0, 300))
        self.assertEqual(0.0, SOAK.cycle_boundary_wait_seconds(100.0, 405.0, 300))

    def test_release_cannot_pass_before_the_requested_wall_duration(self):
        self.assertFalse(SOAK.acceptance_passed("", 288, 288, 86_399.99, 86_400.0))
        self.assertTrue(SOAK.acceptance_passed("", 288, 288, 86_400.0, 86_400.0))
        self.assertFalse(SOAK.acceptance_passed("failed", 288, 288, 86_500.0, 86_400.0))

    def test_connected_workload_is_summed_from_observed_intervals(self):
        cycles = [
            {"connected_workload_seconds": 269.5},
            {"connected_workload_seconds": 270.25},
        ]
        self.assertEqual(539.75, SOAK.total_connected_seconds(cycles))

    def test_acceptance_requires_every_lifecycle_marker_and_expected_exit(self):
        result = {
            "ready": True,
            "client_connected": True,
            "startup_verified": True,
            "seeded": True,
            "settled": True,
            "status_verified": True,
            "rollover_seeded": True,
            "client_ability_actions": 2,
            "shutdown_mode": "clean",
            "exit_code": 0,
            "error_lines": [],
        }
        self.assertTrue(SOAK.cycle_passed(result))
        result["settled"] = False
        self.assertFalse(SOAK.cycle_passed(result))
        result["settled"] = True
        result["shutdown_mode"] = "sigterm"
        result["exit_code"] = -int(SOAK.signal.SIGTERM)
        self.assertTrue(SOAK.cycle_passed(result))
        result["exit_code"] = 3
        self.assertFalse(SOAK.cycle_passed(result))
        result["exit_code"] = -int(SOAK.signal.SIGTERM)
        result["client_ability_actions"] = 0
        self.assertFalse(SOAK.cycle_passed(result))

    def test_negative_machine_marker_fails_fast(self):
        lines = [
            "ordinary output",
            "POWERS_SOAK_VERIFY cycle=2 passed=false detail=locked",
        ]
        self.assertEqual(lines[1], SOAK.failed_phase(lines, 2))
        self.assertEqual("", SOAK.failed_phase(lines, 3))

    def test_quiet_output_keeps_only_lifecycle_evidence(self):
        self.assertTrue(SOAK.should_echo("POWERS_SOAK_SEED cycle=1 passed=true", True))
        self.assertTrue(SOAK.should_echo("SoakClient joined the game", True))
        self.assertFalse(SOAK.should_echo("Loaded 1605 recipes", True))
        self.assertTrue(SOAK.should_echo("Loaded 1605 recipes", False))


if __name__ == "__main__":
    unittest.main()
