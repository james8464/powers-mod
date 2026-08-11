#!/usr/bin/env python3

import subprocess
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class AuditNonItemAssetsTest(unittest.TestCase):
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
