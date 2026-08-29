#!/usr/bin/env python3

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

from scripts.tests import test_verify_int008_temporal as verifier_fixture


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "package_int008_evidence", ROOT / "scripts/package_int008_evidence.py")
PACKAGE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(PACKAGE)


class Int008EvidencePackagerTest(unittest.TestCase):
    def fixture(self, parent):
        builder = verifier_fixture.Int008TemporalVerifierTest(
            methodName="test_complete_exact_sha_evidence_passes")
        return builder.make_fixture(parent)

    @staticmethod
    def base_sha(root):
        return json.loads((root / "build-metadata.json").read_text())["baseSha"]

    def test_archive_bytes_inventories_and_checksums_are_deterministic(self):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.fixture(Path(raw))
            first, second = Path(raw) / "first.tar.gz", Path(raw) / "second.tar.gz"
            base_sha = self.base_sha(root)
            PACKAGE.package(root, first, repository, base_sha)
            PACKAGE.package(root, second, repository, base_sha)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            self.assertTrue(PACKAGE.verify(root, repository, base_sha))
            self.assertTrue(PACKAGE.verify_archive(first, repository, base_sha))
            inventory = (root / "archive-inventory.txt").read_text().splitlines()
            self.assertEqual(sorted(inventory), inventory)

    def test_semantically_false_evidence_is_not_archived(self):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.fixture(Path(raw))
            path = root / "temporal-assertions.jsonl"
            rows = [json.loads(line) for line in path.read_text().splitlines()]
            rows[3]["facts"]["worldAdvanced"] = True
            path.write_text("".join(
                json.dumps(row, separators=(",", ":")) + "\n" for row in rows),
                encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "world manager"):
                PACKAGE.package(root, Path(raw) / "archive.tar.gz", repository,
                                self.base_sha(root))

    def test_tampering_privacy_symlinks_and_crlf_are_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            root, repository = self.fixture(Path(raw))
            base_sha = self.base_sha(root)
            PACKAGE.package(root, Path(raw) / "archive.tar.gz", repository, base_sha)
            (root / "README.md").write_text("changed\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "checksum"):
                PACKAGE.verify(root, repository, base_sha)
        for content, message in ((b"/Users/private\n", "privacy"),
                                 (b"unsafe\r\n", "LF")):
            with tempfile.TemporaryDirectory() as raw:
                root = Path(raw)
                (root / "README.md").write_bytes(content)
                with self.assertRaisesRegex(ValueError, message):
                    PACKAGE.package(root, root.parent / "archive.tar.gz", root)
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "README.md").write_text("safe\n", encoding="utf-8")
            (root / "link").symlink_to(root / "README.md")
            with self.assertRaisesRegex(ValueError, "symlink"):
                PACKAGE.package(root, root.parent / "archive.tar.gz", root)


if __name__ == "__main__":
    unittest.main()
