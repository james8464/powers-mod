#!/usr/bin/env python3

import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/release-envelope.yml"
CATALOGUE = ROOT / "config/release/qa-001-gates.json"


class ReleaseWorkflowTest(unittest.TestCase):
    def text(self) -> str:
        return WORKFLOW.read_text(encoding="utf-8")

    def test_workflow_is_manual_only_with_exact_inputs_and_sha_checkout(self):
        text = self.text()
        on_block = text[text.index("on:"):text.index("permissions:")]
        self.assertIn("workflow_dispatch:", on_block)
        for forbidden in ("push:", "pull_request:", "schedule:", "workflow_call:"):
            self.assertNotIn(forbidden, on_block)
        self.assertRegex(on_block, r"release_sha:\n\s+description:")
        self.assertRegex(on_block, r"evidence_manifest:\n\s+description:")
        self.assertIn("ref: ${{ inputs.release_sha }}", text)
        self.assertIn("persist-credentials: false", text)
        self.assertIn("fetch-depth: 0", text)
        self.assertIn('[[ "$RELEASE_SHA" =~ ^[0-9a-f]{40}$ ]]', text)
        self.assertIn('test "$EVIDENCE_MANIFEST" = "config/release/qa-001-evidence.json"', text)
        self.assertIn('git switch -C main "$RELEASE_SHA"', text)
        self.assertIn('test "$(git rev-parse refs/remotes/origin/main)" = "$RELEASE_SHA"', text)
        self.assertIn('>"$RUNNER_TEMP/powers-xvfb.log"', text)
        self.assertNotIn(">build-xvfb.log", text)

    def test_permissions_and_actions_are_minimal_and_pinned_by_major_contract(self):
        text = self.text()
        permission_block = text[text.index("permissions:"):text.index("jobs:")]
        self.assertEqual(1, permission_block.count("contents: read"))
        self.assertEqual(1, permission_block.count("id-token: write"))
        self.assertEqual(1, permission_block.count("attestations: write"))
        for forbidden in ("contents: write", "packages: write", "actions: write", "pull-requests: write"):
            self.assertNotIn(forbidden, permission_block)
        for action in (
                "actions/checkout@v6", "actions/setup-java@v5",
                "gradle/actions/setup-gradle@v4", "actions/attest@v4",
                "actions/upload-artifact@v6"):
            self.assertIn(action, text)
        self.assertIn("java-version: '25'", text)

    def test_every_declared_gate_is_run_through_release_gate(self):
        text = self.text()
        catalogue = json.loads(CATALOGUE.read_text(encoding="utf-8"))
        for gate in catalogue["commands"]:
            self.assertEqual(1, text.count(f"--gate {gate['id']} "))
            self.assertRegex(
                text,
                rf"scripts/release_gate\.py \\\n\s+--catalogue "
                rf"config/release/qa-001-gates\.json --gate {gate['id']} ")
        self.assertIn("scripts/release_envelope.py \\", text)
        self.assertIn("--mode final", text)
        self.assertIn('"$EVIDENCE_MANIFEST"', text)

    def test_attestation_names_four_exact_subjects_and_never_mutates_release_state(self):
        text = self.text()
        self.assertIn("create-storage-record: false", text)
        subject_block = text[text.index("subject-path: |"):text.index("create-storage-record:")]
        expected = (
            "build/libs/powers-${{ steps.version.outputs.value }}.jar",
            "build/libs/powers-${{ steps.version.outputs.value }}-sources.jar",
            "build/release-envelope/release-envelope.json",
            "build/release-envelope/release-envelope.md",
        )
        for path in expected:
            self.assertEqual(1, subject_block.count(path))
        self.assertNotIn("*", subject_block)
        self.assertIn("path: |\n            build/release-envelope", text)
        lowered = text.lower()
        for forbidden in (
                "git push", "git tag", "gh release", "create-release",
                "softprops/action-gh-release", "packages: write"):
            self.assertNotIn(forbidden, lowered)

    def test_workflow_contract_has_no_placeholder_or_mutable_action_reference(self):
        text = self.text()
        for marker in ("TODO", "TBD", "PLACEHOLDER", "@main", "@master", "@latest"):
            self.assertNotIn(marker, text)
        self.assertNotRegex(text, r"uses:\s+[^\s]+@(?!v\d)\S+")
        self.assertNotIn("curl ", text)
        self.assertNotIn("wget ", text)


if __name__ == "__main__":
    unittest.main()
