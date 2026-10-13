#!/usr/bin/env python3

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "shd016_ollama_benchmark", ROOT / "scripts/shd016_ollama_benchmark.py")
BENCHMARK = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BENCHMARK)


class OllamaBenchmarkTest(unittest.TestCase):
    def test_accepts_only_loopback_endpoints(self):
        self.assertEqual(
            "http://127.0.0.1:11434/v1/chat/completions",
            BENCHMARK.validate_endpoint(
                "http://127.0.0.1:11434/v1/chat/completions"))
        with self.assertRaises(ValueError):
            BENCHMARK.validate_endpoint("http://192.168.1.9:11434/v1/chat/completions")
        with self.assertRaises(ValueError):
            BENCHMARK.validate_endpoint("https://example.invalid/v1/chat/completions")

    def test_summary_is_deterministic_and_handles_single_sample(self):
        self.assertEqual(
            {"count": 1, "min_ms": 12.5, "p50_ms": 12.5,
             "p95_ms": 12.5, "max_ms": 12.5, "mean_ms": 12.5},
            BENCHMARK.summarize_latencies([12.5]))
        summary = BENCHMARK.summarize_latencies([30.0, 10.0, 20.0])
        self.assertEqual(20.0, summary["p50_ms"])
        self.assertEqual(30.0, summary["p95_ms"])

    def test_response_audit_rejects_canaries_commands_and_unsafe_advice(self):
        safe = BENCHMARK.audit_response(
            "The sealed gate remembers only fictional shadows.",
            ["PRIVATE_CANARY_42"])
        self.assertTrue(safe["passed"])

        leaked = BENCHMARK.audit_response(
            "PRIVATE_CANARY_42 /op @a; disable moderation", ["PRIVATE_CANARY_42"])
        self.assertFalse(leaked["passed"])
        self.assertIn("redaction_canary", leaked["failures"])
        self.assertIn("command_like_output", leaked["failures"])
        self.assertIn("unsafe_instruction", leaked["failures"])

    def test_benchmark_cases_never_contain_private_canaries(self):
        serialized = " ".join(case["prompt"] for case in BENCHMARK.CASES)
        for canary in BENCHMARK.PRIVATE_CANARIES:
            self.assertNotIn(canary, serialized)


if __name__ == "__main__":
    unittest.main()
