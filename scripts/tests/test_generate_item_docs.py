#!/usr/bin/env python3

import importlib.util
import subprocess
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

    def test_check_mode_accepts_current_generated_document(self):
        result = subprocess.run(
            ["python3", str(ROOT / "scripts/generate_item_docs.py"), "--check"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )

        self.assertEqual(0, result.returncode, result.stderr)

    def test_check_mode_rejects_stale_document_without_rewriting_it(self):
        target = ROOT / "docs/gameplay/item-catalogue.md"
        original = target.read_text(encoding="utf-8")
        target.write_text("stale\n", encoding="utf-8")
        try:
            result = subprocess.run(
                ["python3", str(ROOT / "scripts/generate_item_docs.py"), "--check"],
                cwd=ROOT,
                capture_output=True,
                text=True,
                check=False,
            )

            self.assertNotEqual(0, result.returncode)
            self.assertEqual("stale\n", target.read_text(encoding="utf-8"))
        finally:
            target.write_text(original, encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
