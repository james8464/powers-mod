#!/usr/bin/env python3

import subprocess
import sys
import unittest
import importlib.util
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"


class Vfx011EvidenceTest(unittest.TestCase):
    def test_accepted_capture_metadata_is_durable_and_exact(self):
        captures = EVIDENCE / "captures.jsonl"
        self.assertTrue(captures.is_file())
        self.assertEqual(
            "d80fbd866a7b99312dba938cf6a6d9cfe86fd902af70303e8d6d2e0a24eb82f6",
            hashlib.sha256(captures.read_bytes()).hexdigest())
        rows = [json.loads(line) for line in captures.read_text().splitlines()]
        self.assertEqual(971, len(rows))
        metadata_ids = [capture_id for row in rows for capture_id in row["captureIds"]]
        index_ids = [line.split("\t", 1)[0]
                     for line in (EVIDENCE / "client-capture-index.tsv").read_text().splitlines()[1:]]
        self.assertEqual(index_ids, metadata_ids)
        self.assertTrue(all(row["sourceKeys"] for row in rows))

    def test_integrated_options_are_exact_and_source_bound(self):
        options = EVIDENCE / "integrated-options.txt"
        self.assertTrue(options.is_file())
        values = dict(line.split("=", 1) for line in options.read_text().splitlines() if "=" in line)
        source = ROOT / values["source"]
        self.assertEqual(values["sourceSha256"], hashlib.sha256(source.read_bytes()).hexdigest())
        self.assertEqual("0,1,2,3,4", values["mipmapLevels"])
        self.assertEqual("ALL", values["normal.particles"])
        self.assertEqual("MINIMAL", values["reduced.particles"])
        self.assertEqual("1280x720,960x720", values["physicalWindows"])
        self.assertEqual("1,2,3,4", values["guiScales"])

    def test_committed_evidence_is_privacy_sanitized(self):
        result = subprocess.run(
            [sys.executable, "scripts/sanitize_vfx011_evidence.py", "--check"],
            cwd=ROOT, capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        private = ("/Users/", "/home/", "127.0.0.1:", "localhost:")
        for path in EVIDENCE.rglob("*"):
            if path.is_file() and path.suffix != ".png":
                text = path.read_text(errors="replace")
                self.assertFalse(any(marker in text for marker in private), path)

    def test_sanitizer_update_is_idempotent(self):
        receipt = EVIDENCE / "two-client/receipt.json"
        before = receipt.read_bytes()
        result = subprocess.run(
            [sys.executable, "scripts/sanitize_vfx011_evidence.py"],
            cwd=ROOT, capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(before, receipt.read_bytes())

    def test_two_client_receipt_hashes_sanitized_owned_files(self):
        receipt = json.loads((EVIDENCE / "two-client/receipt.json").read_text())
        for category, directory, key in (
                ("logs", "logs", "file"), ("options", "options", "file")):
            for entry in receipt[category]:
                name = Path(entry[key]).name
                owned = EVIDENCE / "two-client" / directory / name
                self.assertTrue(owned.is_file(), owned)
                self.assertEqual(entry["sha256"], hashlib.sha256(owned.read_bytes()).hexdigest())

    def test_checksums_bind_every_owned_evidence_file(self):
        checksum_path = EVIDENCE / "SHA256SUMS"
        rows = [line.split("  ", 1) for line in checksum_path.read_text().splitlines()]
        bound = {path: digest for digest, path in rows}
        required = {
            path.relative_to(ROOT).as_posix()
            for path in EVIDENCE.rglob("*") if path.is_file() and path != checksum_path
        }
        required.update({
            "docs/quality/vfx-011-asset-audit.json",
            "docs/quality/vfx-011-reviewed-exceptions.json",
            *(path.relative_to(ROOT).as_posix()
              for path in (ROOT / "docs/quality/vfx-011-asset-pages").glob("*.png")),
        })
        self.assertEqual(required, set(bound))
        for relative, expected in bound.items():
            self.assertEqual(expected, hashlib.sha256((ROOT / relative).read_bytes()).hexdigest(), relative)

    def test_review_ledger_is_complete_and_deterministic(self):
        result = subprocess.run(
            [sys.executable, "scripts/build_vfx011_review_ledger.py", "--check"],
            cwd=ROOT, capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_representatives_are_twenty_ids_backed_by_fifteen_unique_images(self):
        rows = (EVIDENCE / "representative-index.tsv").read_text().splitlines()
        self.assertEqual(21, len(rows))
        self.assertEqual(15, len({row.split("\t")[1] for row in rows[1:]}))
        self.assertEqual(15, len(list((EVIDENCE / "representative-full-resolution").glob("*.png"))))

    def test_decisions_are_explicit_digest_bound_and_complete(self):
        spec = importlib.util.spec_from_file_location(
            "build_vfx011_review_ledger", ROOT / "scripts/build_vfx011_review_ledger.py")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        decisions = module.load_decisions(EVIDENCE / "review-decisions.tsv")
        expected = module.expected_decision_keys()
        self.assertEqual(expected, set(decisions))
        removed = dict(decisions)
        removed.pop(next(iter(removed)))
        with self.assertRaisesRegex(ValueError, "missing"):
            module.validate_decisions(removed, expected)

    def test_names_cannot_infer_a_repair_or_pass(self):
        source = (ROOT / "scripts/build_vfx011_review_ledger.py").read_text()
        self.assertNotIn("client_verdict", source)
        self.assertNotIn("capture_id.startswith", source)


if __name__ == "__main__":
    unittest.main()
