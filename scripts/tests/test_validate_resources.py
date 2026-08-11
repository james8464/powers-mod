#!/usr/bin/env python3

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "validate_resources", ROOT / "scripts/validate_resources.py")
VALIDATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VALIDATOR)


class ValidateResourceReferencesTest(unittest.TestCase):
    def test_reports_local_reference_gaps_and_cycles_deterministically(self):
        fixture = ROOT / "scripts/tests/fixtures/resource_graph_invalid"

        errors = VALIDATOR.validate(fixture)
        self.assertEqual(errors, VALIDATOR.validate(fixture))

        expected = [
            "recipe cycle: powers:a -> powers:b -> powers:a",
            "loot table cycle: powers:chests/a -> powers:chests/b -> powers:chests/a",
            "tag cycle: powers:item/a -> powers:item/b -> powers:item/a",
            "missing local item powers:missing_item",
            "missing local tag powers:item/missing_recipe_tag",
            "missing local loot table powers:chests/missing",
            "missing local tag powers:item/missing",
        ]
        for message in expected:
            self.assertTrue(any(message in error for error in errors), message)


if __name__ == "__main__":
    unittest.main()
