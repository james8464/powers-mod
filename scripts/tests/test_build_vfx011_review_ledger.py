#!/usr/bin/env python3

import subprocess
import sys
import unittest
import csv
import importlib.util
import hashlib
import json
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / "docs/verification/evidence/2026-08-21-vfx-011"
FRESH_EVIDENCE = ROOT / "docs/verification/evidence/2026-08-24-vfx-011"


def load_module(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def runtime_options():
    return {
        "physicalWidth": 1280, "physicalHeight": 720, "requestedGuiScale": 2,
        "effectiveGuiScale": 2, "mipLevel": 0, "particles": "ALL",
        "screenEffectScale": 1.0, "reducedMotion": False, "renderDistance": 12,
        "graphicsMode": "FANCY", "resourcePacks": ["vanilla", "fabric"],
        "gameTime": 6000, "weather": "clear",
    }


class Vfx011EvidenceTest(unittest.TestCase):
    def test_current_bundle_truthfully_waits_for_fresh_raw_capture(self):
        metadata = json.loads((EVIDENCE / "build-metadata.json").read_text())
        self.assertEqual("PENDING_FRESH_RAW_CAPTURE", metadata["status"])
        self.assertNotIn("acceptedClientMetadata", metadata)
        self.assertNotIn("integratedOptions", metadata)
        self.assertFalse((EVIDENCE / "captures.jsonl").exists())
        self.assertFalse((EVIDENCE / "integrated-options.txt").exists())
        self.assertFalse((ROOT / "scripts/rebuild_vfx011_capture_metadata.py").exists())

    def test_packager_rehashes_raw_bytes_and_rejects_metadata_mismatch(self):
        module = load_module("package_vfx011_evidence")
        with tempfile.TemporaryDirectory() as temporary:
            screenshots = Path(temporary)
            source = screenshots / "frame.png"
            source.write_bytes(b"actual raw screenshot bytes")
            row = {"screenshot": source.name, "screenshotSha256": "0" * 64,
                   "runtimeOptions": runtime_options()}
            with self.assertRaisesRegex(ValueError, "raw screenshot digest mismatch"):
                module.validate_raw_screenshots([row], screenshots)

    def test_packager_overwrites_and_rehashes_a_stale_content_address(self):
        module = load_module("package_vfx011_evidence")
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            screenshots = root / "screenshots"
            raw = root / "raw"
            screenshots.mkdir()
            raw.mkdir()
            source = screenshots / "frame.png"
            source.write_bytes(b"fresh accepted screenshot")
            sha256 = hashlib.sha256(source.read_bytes()).hexdigest()
            destination = raw / f"{sha256}.png"
            destination.write_bytes(b"corrupt stale content")
            row = {"screenshot": source.name, "screenshotSha256": sha256,
                   "runtimeOptions": runtime_options()}
            module.retain_raw_screenshots([row], screenshots, raw)
            self.assertEqual(sha256, hashlib.sha256(destination.read_bytes()).hexdigest())

    def test_packager_requires_client_emitted_digest_and_runtime_options(self):
        module = load_module("package_vfx011_evidence")
        with tempfile.TemporaryDirectory() as temporary:
            captures = Path(temporary) / "captures.jsonl"
            captures.write_text(json.dumps({"screenshot": "frame.png", "captureIds": ["one"]}) + "\n")
            with self.assertRaisesRegex(ValueError, "client-emitted"):
                module.load_rows(captures)

    def test_packager_requires_complete_actual_runtime_options(self):
        module = load_module("package_vfx011_evidence")
        with tempfile.TemporaryDirectory() as temporary:
            captures = Path(temporary) / "captures.jsonl"
            row = {"screenshot": "frame.png", "captureIds": ["one"],
                   "screenshotSha256": "a" * 64, "runtimeOptions": runtime_options()}
            del row["runtimeOptions"]["graphicsMode"]
            captures.write_text(json.dumps(row) + "\n")
            with self.assertRaisesRegex(ValueError, "client-emitted"):
                module.load_rows(captures)

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

    def test_historical_client_rows_cannot_claim_visual_acceptance(self):
        rows = list(csv.DictReader(
            (EVIDENCE / "review-decisions.tsv").read_text().splitlines(), delimiter="\t"))
        screenshots = [row for row in rows if row["kind"] == "historical_client_digest"]
        pages = [row for row in rows if row["kind"] == "client_page"]
        self.assertEqual(971, len(screenshots))
        self.assertEqual({"PENDING_RAW_RECAPTURE"}, {row["verdict"] for row in screenshots})
        self.assertEqual(49, len(pages))
        self.assertEqual({"LIMITED"}, {row["verdict"] for row in pages})

    def test_fresh_bundle_uses_retained_raw_decisions_and_is_cli_selectable(self):
        module = load_module("build_vfx011_review_ledger")
        decisions = module.load_decisions(FRESH_EVIDENCE / "review-decisions.tsv")
        expected = module.expected_decision_keys(FRESH_EVIDENCE)
        self.assertEqual(expected, set(decisions))
        raw = [row for key, row in decisions.items() if key[0] == "client_raw"]
        self.assertEqual(971, len(raw))
        self.assertNotIn("PENDING_RAW_RECAPTURE", {row["verdict"] for row in raw})
        result = subprocess.run(
            [sys.executable, "scripts/build_vfx011_review_ledger.py", "--evidence",
             str(FRESH_EVIDENCE), "--check"], cwd=ROOT, capture_output=True, text=True,
            check=False)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_fresh_checksums_bind_every_owned_file(self):
        checksum_path = FRESH_EVIDENCE / "SHA256SUMS"
        rows = [line.split("  ", 1) for line in checksum_path.read_text().splitlines()]
        bound = {path: digest for digest, path in rows}
        retained = list((FRESH_EVIDENCE / "client-raw").glob("*.png"))
        self.assertEqual(971, len(retained))
        owned = [path for path in FRESH_EVIDENCE.rglob("*")
                 if path.is_file() and path != checksum_path]
        owned.extend((
            ROOT / "docs/quality/vfx-011-asset-audit.json",
            ROOT / "docs/quality/vfx-011-reviewed-exceptions.json",
        ))
        owned.extend((ROOT / "docs/quality/vfx-011-asset-pages").glob("*.png"))
        self.assertEqual({path.relative_to(ROOT).as_posix() for path in owned}, set(bound))
        for path in owned:
            relative = path.relative_to(ROOT).as_posix()
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), bound[relative])

    def test_names_cannot_infer_a_repair_or_pass(self):
        source = (ROOT / "scripts/build_vfx011_review_ledger.py").read_text()
        self.assertNotIn("client_verdict", source)
        self.assertNotIn("capture_id.startswith", source)


if __name__ == "__main__":
    unittest.main()
