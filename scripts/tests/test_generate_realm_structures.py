#!/usr/bin/env python3

import importlib.util
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "generate_realm_structures", ROOT / "scripts/generate_realm_structures.py")
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


class GenerateRealmStructuresTest(unittest.TestCase):
    def test_landmark_cores_use_vanilla_alignment_blocks(self):
        self.assertEqual("minecraft:sea_lantern", GENERATOR.palette("light")["core"])
        self.assertEqual("minecraft:crying_obsidian", GENERATOR.palette("dark")["core"])


if __name__ == "__main__":
    unittest.main()
