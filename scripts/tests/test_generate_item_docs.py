#!/usr/bin/env python3

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "generate_item_docs", ROOT / "scripts/generate_item_docs.py")
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GENERATOR)


class GenerateItemDocsTest(unittest.TestCase):
    def test_wisdom_fruit_names_its_only_survival_source(self):
        document = GENERATOR.render()

        self.assertIn(
            "| `powers:imported_food_wisdomfruit` | Wisdomfruit | Provision | "
            "Edible food; cooked and smoked forms restore more hunger | "
            "3.5% additive drop from Archivist realm-memory caches |",
            document,
        )


if __name__ == "__main__":
    unittest.main()
