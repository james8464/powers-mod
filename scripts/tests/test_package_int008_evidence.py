#!/usr/bin/env python3

import importlib.util
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "package_int008_evidence", ROOT / "scripts/package_int008_evidence.py")
PACKAGE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(PACKAGE)


class Int008EvidencePackagerTest(unittest.TestCase):
    def test_archive_bytes_inventories_and_checksums_are_deterministic(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw) / "evidence"
            root.mkdir()
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "logs").mkdir()
            (root / "logs/server.log").write_text("PASS\n", encoding="utf-8")
            first, second = Path(raw) / "first.tar.gz", Path(raw) / "second.tar.gz"
            PACKAGE.package(root, first)
            PACKAGE.package(root, second)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            self.assertTrue(PACKAGE.verify(root))
            inventory = (root / "archive-inventory.txt").read_text().splitlines()
            self.assertEqual(sorted(inventory), inventory)

    def test_tampering_privacy_symlinks_and_crlf_are_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw) / "evidence"
            root.mkdir()
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            PACKAGE.package(root, Path(raw) / "archive.tar.gz")
            (root / "README.md").write_text("changed\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "checksum"):
                PACKAGE.verify(root)
        for content, message in ((b"/Users/private\n", "privacy"),
                                 (b"unsafe\r\n", "LF")):
            with tempfile.TemporaryDirectory() as raw:
                root = Path(raw)
                (root / "README.md").write_bytes(content)
                with self.assertRaisesRegex(ValueError, message):
                    PACKAGE.package(root, root.parent / "archive.tar.gz")
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "link").symlink_to(root / "README.md")
            with self.assertRaisesRegex(ValueError, "symlink"):
                PACKAGE.package(root, root.parent / "archive.tar.gz")


if __name__ == "__main__":
    unittest.main()
