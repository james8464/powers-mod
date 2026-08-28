import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "scripts" / "generate_layered_magic_sounds.py"
VALIDATOR = ROOT / "scripts" / "validate_layered_audio.py"
MASTER_DIR = ROOT / "src" / "main" / "resources" / "assets" / "powers" / "sounds" / "magic"


class LayeredAudioValidationTest(unittest.TestCase):
    def test_committed_bank_meets_inventory_loudness_spectrum_and_comfort_contract(self):
        result = subprocess.run(
            ["python3", str(VALIDATOR), "--root", str(ROOT), "--json"],
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr or result.stdout)
        report = json.loads(result.stdout)

        self.assertEqual(1, report["schemaVersion"])
        self.assertEqual(51, report["assetCount"])
        self.assertEqual([], report["errors"])
        self.assertEqual(16, len(report["cues"]))
        for cue in report["cues"]:
            rms = cue["effectiveRms"]
            self.assertGreater(rms["near"], rms["mid"], cue["cue"])
            self.assertGreater(rms["mid"], rms["far"], cue["cue"])
            self.assertLessEqual(cue["peak"], 0.7071, cue["cue"])
            self.assertLessEqual(cue["farCentroid"], cue["nearCentroid"] * 0.80, cue["cue"])
        self.assertLessEqual(report["reducedCelestialHighBandRatio"], 0.30)
        self.assertGreater(report["reducedCelestialLowMidEnergy"], 0.000001)

    def test_generator_reproduces_identical_layer_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = Path(directory)
            fixture_masters = fixture / "src" / "main" / "resources" / "assets" / "powers" / "sounds" / "magic"
            fixture_masters.mkdir(parents=True)
            for master in sorted(MASTER_DIR.glob("*.ogg")):
                shutil.copy2(master, fixture_masters / master.name)

            first = self.run_generator(fixture)
            first_hashes = self.hashes(fixture_masters / "layered")
            second = self.run_generator(fixture)
            second_hashes = self.hashes(fixture_masters / "layered")

            self.assertEqual(0, first.returncode, first.stderr or first.stdout)
            self.assertEqual(0, second.returncode, second.stderr or second.stdout)
            self.assertEqual(51, len(first_hashes))
            self.assertEqual(first_hashes, second_hashes)

    @staticmethod
    def run_generator(root: Path):
        return subprocess.run(
            ["python3", str(GENERATOR), "--root", str(root)],
            check=False,
            capture_output=True,
            text=True,
        )

    @staticmethod
    def hashes(directory: Path):
        return {
            path.name: hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted(directory.glob("*.ogg"))
        }


if __name__ == "__main__":
    unittest.main()
