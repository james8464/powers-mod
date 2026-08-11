#!/usr/bin/env python3

import json
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/generate_rank_docs.py"


class GenerateRankDocsTest(unittest.TestCase):
    def test_generates_both_registries_and_check_mode_rejects_stale_output(self):
        with tempfile.TemporaryDirectory() as directory:
            project = Path(directory)
            ranks = project / "src/main/resources/data/powers/ranks"
            ranks.mkdir(parents=True)
            (ranks / "light.json").write_text(json.dumps([
                {"id": "legacy_0", "depth": 0, "branch": "origin", "title": "Dormant",
                 "parents": [], "canonical": True,
                 "perks": [{"type": "ENERGY_CAPACITY", "amount": 0.01, "actionOrAspect": ""}]},
                {"id": "might_1", "depth": 1, "branch": "might", "title": "Ember-Blooded",
                 "parents": ["legacy_0"], "canonical": False,
                 "perks": [{"type": "POWER_DAMAGE", "amount": 0.06, "actionOrAspect": "fire"}]},
            ]), encoding="utf-8")
            (ranks / "darkness.json").write_text(json.dumps([
                {"id": "legacy_0", "depth": 0, "branch": "abyss", "title": "Unmarked",
                 "parents": [], "canonical": True,
                 "perks": [{"type": "STEALTH", "amount": 0.018, "actionOrAspect": ""}]},
            ]), encoding="utf-8")

            generated = subprocess.run(
                ["python3", str(SCRIPT), "--root", str(project)],
                capture_output=True, text=True, check=False)
            self.assertEqual(0, generated.returncode, generated.stderr)
            target = project / "docs/gameplay/rank-catalogue.md"
            self.assertEqual(
                """# Rank catalogue

This generated appendix is sourced from the Light and Darkness rank registries.

| Alignment | ID | Depth | Branch | Title | Route | Parents | Perks |
|---|---|---:|---|---|---|---|---|
| Light | `legacy_0` | 0 | `origin` | Dormant | Canonical | — | `ENERGY_CAPACITY +1%` |
| Light | `might_1` | 1 | `might` | Ember-Blooded | Optional | `legacy_0` | `POWER_DAMAGE +6% (fire)` |
| Darkness | `legacy_0` | 0 | `abyss` | Unmarked | Canonical | — | `STEALTH +1.8%` |
""",
                target.read_text(encoding="utf-8"),
            )

            current = subprocess.run(
                ["python3", str(SCRIPT), "--root", str(project), "--check"],
                capture_output=True, text=True, check=False)
            self.assertEqual(0, current.returncode, current.stderr)

            target.write_text("stale\n", encoding="utf-8")
            stale = subprocess.run(
                ["python3", str(SCRIPT), "--root", str(project), "--check"],
                capture_output=True, text=True, check=False)
            self.assertNotEqual(0, stale.returncode)
            self.assertEqual("stale\n", target.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
