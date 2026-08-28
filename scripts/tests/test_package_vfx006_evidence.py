#!/usr/bin/env python3

import importlib.util
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "package_vfx006_evidence", ROOT / "scripts/package_vfx006_evidence.py")
PACKAGE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(PACKAGE)


class Vfx006EvidencePackagerTest(unittest.TestCase):
    def test_package_is_deterministic_and_verifiable(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "logs").mkdir()
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "logs" / "check.log").write_text("BUILD SUCCESSFUL\n", encoding="utf-8")
            first = PACKAGE.package(root)
            second = PACKAGE.package(root)
            self.assertEqual(first, second)
            self.assertTrue(PACKAGE.verify(root))
            self.assertIn("README.md", (root / "evidence-inventory.txt").read_text())

    def test_privacy_leak_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("/Users/james/private\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "privacy leak"):
                PACKAGE.package(root)

    def test_stale_packaged_checksum_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            PACKAGE.package(root)
            (root / "README.md").write_text("changed\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "checksum mismatch"):
                PACKAGE.verify(root)

    def test_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "link").symlink_to(root / "README.md")
            with self.assertRaisesRegex(ValueError, "symlink"):
                PACKAGE.package(root)


if __name__ == "__main__":
    unittest.main()
