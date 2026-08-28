#!/usr/bin/env python3

import importlib.util
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "package_vfx007_evidence", ROOT / "scripts/package_vfx007_evidence.py")
PACKAGE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(PACKAGE)


class Vfx007EvidencePackagerTest(unittest.TestCase):
    def test_archive_bytes_inventories_and_checksums_are_deterministic(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw) / "evidence"
            root.mkdir()
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "logs").mkdir()
            (root / "logs" / "client.log").write_text("PASS\n", encoding="utf-8")
            first = Path(raw) / "first.tar.gz"
            second = Path(raw) / "second.tar.gz"

            PACKAGE.package(root, first)
            PACKAGE.package(root, second)

            self.assertEqual(first.read_bytes(), second.read_bytes())
            self.assertTrue(PACKAGE.verify(root))
            inventory = (root / "archive-inventory.txt").read_text().splitlines()
            self.assertEqual(sorted(inventory), inventory)
            self.assertIn("SHA256SUMS", inventory)
            self.assertNotIn("SHA256SUMS", (root / "SHA256SUMS").read_text())

    def test_checksum_tampering_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            PACKAGE.package(root, root.parent / "archive.tar.gz")
            (root / "README.md").write_text("changed\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "checksum"):
                PACKAGE.verify(root)

    def test_private_content_and_symlinks_are_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("/Users/private\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "privacy"):
                PACKAGE.package(root, root.parent / "archive.tar.gz")
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "link").symlink_to(root / "README.md")
            with self.assertRaisesRegex(ValueError, "symlink"):
                PACKAGE.package(root, root.parent / "archive.tar.gz")

    def test_crlf_text_is_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_bytes(b"unsafe\r\n")
            with self.assertRaisesRegex(ValueError, "LF"):
                PACKAGE.package(root, root.parent / "archive.tar.gz")


if __name__ == "__main__":
    unittest.main()
