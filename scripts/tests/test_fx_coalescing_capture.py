from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts" / "fx_coalescing_capture.py"


def load_module():
    spec = importlib.util.spec_from_file_location("fx_coalescing_capture", SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError("Could not load FX capture harness")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FxCoalescingCaptureHarnessTest(unittest.TestCase):
    def test_parses_connected_capture_and_enforces_both_reduction_gates(self):
        module = load_module()
        accepted = module.parse_capture(
            "POWERS_FX_CAPTURE passed=true attemptedPackets=64 deliveredPackets=1 "
            "attemptedBytes=3520 deliveredBytes=55 packetReduction=98.438 "
            "byteReduction=98.438")
        rejected = module.parse_capture(
            "POWERS_FX_CAPTURE passed=false attemptedPackets=4 deliveredPackets=3 "
            "attemptedBytes=400 deliveredBytes=301 packetReduction=25.000 "
            "byteReduction=24.750")

        self.assertTrue(module.accepted(accepted))
        self.assertFalse(module.accepted(rejected))

    def test_uses_a_real_scripted_client_and_the_physical_collision_gametest(self):
        source = SCRIPT.read_text(encoding="utf-8")
        module = load_module()
        self.assertIn("powers.qa.server=127.0.0.1:25567", source)
        self.assertIn("powers testing fx-capture 64", source)
        self.assertIn("fx_coalescing_game_tests_duplicate_visual_updates",
                      module.COLLISION_FILTER)
        self.assertIn("collision_gametest_passed", source)


if __name__ == "__main__":
    unittest.main()
