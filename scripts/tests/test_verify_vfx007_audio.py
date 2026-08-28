#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "verify_vfx007_audio", ROOT / "scripts/verify_vfx007_audio.py")
VERIFY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(VERIFY)

CUES = (
    "rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
    "celestial_ring", "beam_ring", "boss_impact_ring", "time_release",
    "rift_open", "rift_close", "soul_tether", "light_chorus",
    "dark_whisper", "ward_impact", "rank_awaken", "interaction_clash",
)
SHA = "a" * 40
DISTANCES = {
    "intimate": {"near": 4.0, "mid": 16.0, "far": 50.0},
    "standard": {"near": 6.0, "mid": 30.0, "far": 90.0},
    "world": {"near": 10.0, "mid": 60.0, "far": 160.0},
}
PROFILE = {
    "rune_hum": "intimate", "crystal_resonate": "intimate",
    "celestial_ring": "world", "boss_impact_ring": "world",
    "light_chorus": "world", "dark_whisper": "world", "rank_awaken": "world",
}


class Vfx007AudioVerifierTest(unittest.TestCase):
    def make_fixture(self, root: Path) -> Path:
        rows = []
        event = 1
        open_ids = {}
        for cue in CUES:
            profile = PROFILE.get(cue, "standard")
            for layer in ("near", "mid", "far"):
                rows.append(self.row(event, cue, layer, DISTANCES[profile][layer], False,
                                     "admitted", False))
                open_ids[(cue, layer)] = event
                event += 1
        for cue in CUES:
            profile = PROFILE.get(cue, "standard")
            rows.append(self.row(event, cue, "mid", DISTANCES[profile]["near"], True,
                                 "admitted", False))
            event += 1
        burst_ids = []
        for index in range(9):
            rows.append(self.row(event, "interaction_clash", "near", 1.0, False,
                                 "admitted" if index < 4 else "dropped", False))
            burst_ids.append(event)
            event += 1
        reduced_id = event
        rows.append(self.row(event, "celestial_ring", "near", 10.0, False,
                             "admitted", True))
        event += 1
        lifecycle = {}
        for scenario in ("reload", "reconnect", "dimension"):
            lifecycle[scenario] = event
            row = self.row(event, "rune_hum", "near", 4.0, False,
                           "admitted", False)
            if scenario == "dimension":
                row["dimension"] = "minecraft:the_nether"
            rows.append(row)
            event += 1
        self.write_jsonl(root / "audio-audit.jsonl", rows)
        self.write_json(root / "capture-index.json", {
            "schemaVersion": 1,
            "implementationSha": SHA,
            "burstEventIds": burst_ids,
            "ordinaryCelestialEventId": open_ids[("celestial_ring", "near")],
            "reducedCelestialEventId": reduced_id,
            "lifecycleEventIds": lifecycle,
        })
        self.write_json(root / "audio-metrics.json", {
            "schemaVersion": 1, "assetCount": 51, "errors": [],
            "cues": [{"cue": cue, "effectiveRms": {"near": 0.3, "mid": 0.2,
                       "far": 0.1}, "peak": 0.5, "nearCentroid": 1000.0,
                      "farCentroid": 700.0} for cue in CUES],
            "reducedCelestialHighBandRatio": 0.2,
            "reducedCelestialLowMidEnergy": 1.0,
        })
        self.write_json(root / "spectrogram-summary.json", {
            "schemaVersion": 1, "implementationSha": SHA,
            "cues": [{"cue": cue, "nearCentroid": 1000.0,
                      "farCentroid": 700.0} for cue in CUES],
        })
        screenshots = root / "screenshots"
        screenshots.mkdir()
        captures = []
        for index, cue in enumerate(CUES):
            path = screenshots / f"{index:02d}-{cue}.png"
            Image.new("RGB", (1280, 720), (20 + index, 30, 40)).save(path)
            captures.append({"subtitleKey": f"subtitles.powers.{cue}",
                             "imagePath": path.name,
                             "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
        self.write_json(root / "subtitles.json", {
            "schemaVersion": 1, "implementationSha": SHA, "captures": captures,
        })
        self.write_json(root / "build-metadata.json", {
            "schemaVersion": 1, "implementationSha": SHA, "result": "PENDING",
        })
        (root / "README.md").write_text(
            "# VFX-007 evidence\n\nNo microphone recording is used as source-faithful proof.\n",
            encoding="utf-8")
        return root

    @staticmethod
    def row(event, cue, layer, distance, obstructed, result, reduced):
        return {"schemaVersion": 1, "cue": cue, "layer": layer,
                "distance": distance, "obstructed": obstructed,
                "effectiveGain": 0.0 if result == "dropped" else 0.4,
                "result": result, "subtitleKey": f"subtitles.powers.{cue}",
                "reducedTinnitus": reduced, "dimension": "minecraft:overworld",
                "eventId": event, "implementationSha": SHA}

    @staticmethod
    def write_json(path, value):
        path.write_text(json.dumps(value, sort_keys=True) + "\n", encoding="utf-8")

    @staticmethod
    def write_jsonl(path, rows):
        path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
                        encoding="utf-8")

    def mutate(self, operation, message):
        with tempfile.TemporaryDirectory() as raw:
            root = self.make_fixture(Path(raw))
            operation(root)
            with self.assertRaisesRegex(ValueError, message):
                VERIFY.validate(root)

    def test_complete_production_evidence_passes(self):
        with tempfile.TemporaryDirectory() as raw:
            result = VERIFY.validate(self.make_fixture(Path(raw)))
            self.assertEqual(77, result["rowCount"])
            self.assertEqual(48, result["openCount"])
            self.assertEqual(16, result["wallCount"])
            self.assertEqual(16, result["subtitleCount"])

    def test_missing_open_layer_is_rejected(self):
        def remove(root):
            rows = [json.loads(line) for line in (root / "audio-audit.jsonl").read_text().splitlines()]
            rows.pop(0)
            self.write_jsonl(root / "audio-audit.jsonl", rows)
        self.mutate(remove, "open coverage")

    def test_burst_must_demonstrate_the_group_cap(self):
        def admit_all(root):
            rows = [json.loads(line) for line in (root / "audio-audit.jsonl").read_text().splitlines()]
            index = json.loads((root / "capture-index.json").read_text())
            for row in rows:
                if row["eventId"] in index["burstEventIds"]:
                    row["result"], row["effectiveGain"] = "admitted", 0.4
            self.write_jsonl(root / "audio-audit.jsonl", rows)
        self.mutate(admit_all, "burst cap")

    def test_lifecycle_scenario_is_required(self):
        def remove(root):
            index = json.loads((root / "capture-index.json").read_text())
            del index["lifecycleEventIds"]["reconnect"]
            self.write_json(root / "capture-index.json", index)
        self.mutate(remove, "lifecycle")

    def test_mixed_implementation_identity_is_rejected(self):
        def mix(root):
            rows = [json.loads(line) for line in (root / "audio-audit.jsonl").read_text().splitlines()]
            rows[-1]["implementationSha"] = "b" * 40
            self.write_jsonl(root / "audio-audit.jsonl", rows)
        self.mutate(mix, "implementation")

    def test_duplicate_json_key_is_rejected(self):
        def duplicate(root):
            path = root / "build-metadata.json"
            path.write_text('{"schemaVersion":1,"schemaVersion":1}\n', encoding="utf-8")
        self.mutate(duplicate, "duplicate JSON key")

    def test_nonfinite_gain_is_rejected(self):
        def nonfinite(root):
            text = (root / "audio-audit.jsonl").read_text()
            (root / "audio-audit.jsonl").write_text(
                text.replace('"effectiveGain": 0.4', '"effectiveGain": NaN', 1),
                encoding="utf-8")
        self.mutate(nonfinite, "finite")

    def test_private_identity_is_rejected(self):
        def leak(root):
            (root / "README.md").write_text("player=james8464 /Users/private\n", encoding="utf-8")
        self.mutate(leak, "privacy")

    def test_metric_errors_are_rejected(self):
        def error(root):
            metrics = json.loads((root / "audio-metrics.json").read_text())
            metrics["errors"] = ["bad asset"]
            self.write_json(root / "audio-metrics.json", metrics)
        self.mutate(error, "metrics")


if __name__ == "__main__":
    unittest.main()
