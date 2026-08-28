#!/usr/bin/env python3

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "vfx007_audio_capture", ROOT / "scripts/vfx007_audio_capture.py")
CAPTURE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(CAPTURE)


class Vfx007AudioCaptureTest(unittest.TestCase):
    def test_primary_scenario_covers_open_wall_burst_comfort_reload_dimension_and_subtitles(self):
        lines = CAPTURE.render_primary_scenario().splitlines()
        creative = lines.index("70\tcommand\tgamemode creative @s")
        teleport = lines.index("80\tcommand\ttp @s 0 200 0")
        self.assertLess(creative, teleport)
        emits = [line for line in lines if "\taudio_emit\t" in line]
        self.assertEqual(77 - 1, len(emits))  # reconnect is captured by the second real client
        self.assertEqual(48, sum(line.endswith(" open") and "interaction_clash 1 open" not in line
                                 for line in emits[:48]))
        self.assertEqual(16, sum(line.endswith(" wall") for line in emits))
        self.assertEqual(16, sum("\tscreenshot\taudio-subtitle-" in line for line in lines))
        self.assertIn("audio_reload\tnow", CAPTURE.render_primary_scenario())
        self.assertIn("audio_comfort\treduced", CAPTURE.render_primary_scenario())
        self.assertIn("minecraft:the_nether", CAPTURE.render_primary_scenario())

    def test_reconnect_scenario_emits_one_post_join_row(self):
        text = CAPTURE.render_reconnect_scenario()
        self.assertEqual(1, text.count("\taudio_emit\t"))
        self.assertIn("rune_hum 4 open", text)

    def test_audit_extraction_keeps_only_schema_one_json_rows(self):
        first = {"schemaVersion": 1, "eventId": 1}
        second = {"schemaVersion": 1, "eventId": 2}
        log = ("noise\n[INFO] powers_layered_audio_audit " + json.dumps(first) + "\n"
               "identity=AudioCapture\n[INFO] powers_layered_audio_audit "
               + json.dumps(second) + "\n")
        self.assertEqual([first, second], CAPTURE.extract_audit_rows(log))

    def test_launch_argument_resolution_rejects_gametest_classpath(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            (root / "runGameTest").write_text("client classpath\n", encoding="utf-8")
            with self.assertRaisesRegex(RuntimeError, "runClient"):
                CAPTURE.resolve_argument_file(root)

    def test_launch_argument_resolution_accepts_only_run_client(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            expected = root / "runClient"
            expected.write_text("full LWJGL client classpath\n", encoding="utf-8")
            self.assertEqual(expected, CAPTURE.resolve_argument_file(root))


if __name__ == "__main__":
    unittest.main()
