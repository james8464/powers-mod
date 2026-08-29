#!/usr/bin/env python3

import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/verify_int008_checkout.py"


class Int008CheckoutVerifierTest(unittest.TestCase):
    def test_accepts_only_the_checked_out_exact_commit(self):
        with tempfile.TemporaryDirectory() as raw:
            repository = Path(raw)
            subprocess.run(["git", "init", "-q"], cwd=repository, check=True)
            subprocess.run(["git", "config", "user.name", "INT-008 Test"],
                           cwd=repository, check=True)
            subprocess.run(["git", "config", "user.email", "int008@example.invalid"],
                           cwd=repository, check=True)
            (repository / "tracked.txt").write_text("first\n", encoding="utf-8")
            subprocess.run(["git", "add", "tracked.txt"], cwd=repository, check=True)
            subprocess.run(["git", "commit", "-qm", "fixture"], cwd=repository, check=True)
            head = subprocess.check_output(
                ["git", "rev-parse", "HEAD"], cwd=repository, text=True).strip()

            accepted = subprocess.run(
                ["python3", str(SCRIPT), "--repository", str(repository),
                 "--expected", head], capture_output=True, text=True)
            self.assertEqual(0, accepted.returncode, accepted.stderr)
            rejected = subprocess.run(
                ["python3", str(SCRIPT), "--repository", str(repository),
                 "--expected", "a" * 40], capture_output=True, text=True)
            self.assertNotEqual(0, rejected.returncode)
            self.assertIn("does not match checked-out HEAD", rejected.stderr)

            (repository / "tracked.txt").write_text("dirty\n", encoding="utf-8")
            dirty = subprocess.run(
                ["python3", str(SCRIPT), "--repository", str(repository),
                 "--expected", head], capture_output=True, text=True)
            self.assertNotEqual(0, dirty.returncode)
            self.assertIn("working tree is not clean", dirty.stderr)


if __name__ == "__main__":
    unittest.main()
