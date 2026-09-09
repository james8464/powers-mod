import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/generate_knowledge_docs.py"


def load_generator():
    spec = importlib.util.spec_from_file_location("generate_knowledge_docs", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class GenerateKnowledgeDocsTest(unittest.TestCase):
    def test_inventory_reaches_exact_source_backed_coverage(self):
        generator = load_generator()
        inventory = generator.build_inventory(ROOT)

        self.assertEqual(
            {"items": 260, "actions": 64, "ranks": 56, "realms": 3, "interaction_rules": 50},
            {domain: len(entries) for domain, entries in inventory.items()},
        )
        self.assertEqual(433, sum(len(entries) for entries in inventory.values()))
        for entries in inventory.values():
            for entry in entries:
                self.assertTrue(entry["sources"])
                self.assertTrue(all(source.startswith(("docs/", "data/")) for source in entry["sources"]))

    def test_generated_entries_respect_runtime_parser_bounds_and_are_unique(self):
        generator = load_generator()
        inventory = generator.build_inventory(ROOT)
        entries = [entry for values in inventory.values() for entry in values]

        self.assertEqual(len(entries), len({entry["id"] for entry in entries}))
        for entry in entries:
            self.assertLessEqual(len("powers:" + entry["id"]), 96)
            self.assertLessEqual(len(entry["title"]), 128)
            self.assertLessEqual(len(entry["answer"]), 4096)
            self.assertLessEqual(len(entry["keywords"]), 32)
            self.assertLessEqual(len(entry["sources"]), 16)
            self.assertTrue(all(0 < len(keyword) <= 96 for keyword in entry["keywords"]))
            self.assertTrue(all(0 < len(source) <= 256 for source in entry["sources"]))

    def test_write_and_check_rejects_stale_or_extra_generated_files(self):
        generator = load_generator()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            generator.write_outputs(ROOT, root)
            self.assertEqual([], generator.check_outputs(ROOT, root))

            generated = root / generator.GENERATED_DIRECTORY
            first = sorted(generated.rglob("*.json"))[0]
            first.write_text("{}\n", encoding="utf-8")
            self.assertIn(str(first.relative_to(root)), generator.check_outputs(ROOT, root))

            generator.write_outputs(ROOT, root)
            extra = generated / "extra.json"
            extra.write_text(json.dumps({"unexpected": True}) + "\n", encoding="utf-8")
            self.assertIn(str(extra.relative_to(root)), generator.check_outputs(ROOT, root))


if __name__ == "__main__":
    unittest.main()
