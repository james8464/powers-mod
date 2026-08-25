#!/usr/bin/env python3
"""Regression coverage for the VFX-009 committed-evidence privacy gate."""

from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class SanitizeVfx009EvidenceTest(unittest.TestCase):
    def test_committed_evidence_has_no_absolute_home_paths(self) -> None:
        result = subprocess.run(
            [sys.executable, "scripts/sanitize_vfx009_evidence.py", "--check"],
            cwd=ROOT, capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
