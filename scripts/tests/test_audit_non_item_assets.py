#!/usr/bin/env python3

import subprocess
import sys
import unittest
import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class AuditNonItemAssetsTest(unittest.TestCase):
    def test_manifest_never_promotes_decode_or_contact_sheets_to_visual_pass(self):
        spec = importlib.util.spec_from_file_location(
            "audit_non_item_assets", ROOT / "scripts/audit_non_item_assets.py")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        manifest = module.render_manifest(module.tracked_files())
        self.assertNotIn("reviewed in contact sheet", manifest.lower())
        self.assertNotIn("| pass |", manifest.lower())
        self.assertIn("integrity-only", manifest.lower())

    def test_check_mode_needs_only_the_python_standard_library(self):
        result = subprocess.run(
            [sys.executable, "-S", "scripts/audit_non_item_assets.py", "--check"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
