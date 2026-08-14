import importlib.util
from pathlib import Path
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "quest_telemetry_campaign.py"
SPEC = importlib.util.spec_from_file_location("quest_telemetry_campaign", SCRIPT)
CAMPAIGN = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CAMPAIGN)


class QuestTelemetryCampaignHarnessTest(unittest.TestCase):
    def test_each_alignment_uses_ten_distinct_real_client_names(self):
        light = CAMPAIGN.client_names("LIGHT")
        dark = CAMPAIGN.client_names("DARK")
        self.assertEqual(10, len(light))
        self.assertEqual(10, len(set(light)))
        self.assertEqual(10, len(dark))
        self.assertTrue(all(name.startswith("QuestLight") for name in light))
        self.assertTrue(all(name.startswith("QuestDark") for name in dark))

    def test_publication_requires_all_twenty_ten_sample_rows(self):
        lines = []
        for alignment in ("LIGHT", "DARK"):
            for level in range(1, 11):
                lines.append(f"prefix {alignment};{level};10;1200;1500;pilgrimage")
        rows = CAMPAIGN.parse_telemetry_rows(lines)
        self.assertTrue(CAMPAIGN.publication_ready(rows))
        rows[("DARK", 10)]["samples"] = 9
        self.assertFalse(CAMPAIGN.publication_ready(rows))

    def test_client_command_keeps_game_directories_isolated(self):
        command = CAMPAIGN.client_command(
            Path("/jdk/bin/java"), Path("/launch.cfg"), Path("/args"),
            Path("/client-7"), "QuestLight7")
        rendered = " ".join(map(str, command))
        self.assertIn("--username QuestLight7", rendered)
        self.assertIn("--gameDir /client-7", rendered)
        self.assertIn("powers.qa.role=quest-telemetry", rendered)
        self.assertIn("powers.qa.server=127.0.0.1:25566", rendered)

    def test_client_options_use_valid_minimum_distances(self):
        options = CAMPAIGN.client_options()
        self.assertIn("renderDistance:2\n", options)
        self.assertIn("simulationDistance:5\n", options)

    def test_only_offline_profile_key_errors_are_expected(self):
        expected, unexpected = CAMPAIGN.classify_client_errors([
            "[Download-1/ERROR] (Minecraft) Failed to retrieve profile key pair",
            "[Render thread/ERROR] (powers) Packet handler crashed",
        ])
        self.assertEqual(1, len(expected))
        self.assertEqual(1, len(unexpected))
        self.assertIn("Packet handler crashed", unexpected[0])


if __name__ == "__main__":
    unittest.main()
