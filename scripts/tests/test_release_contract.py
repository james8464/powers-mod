#!/usr/bin/env python3

import importlib.util
import json
import os
import stat
import tempfile
import unittest
from dataclasses import FrozenInstanceError
from pathlib import Path
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts/release_contract.py"
SPEC = importlib.util.spec_from_file_location("release_contract", MODULE_PATH)
CONTRACT = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(CONTRACT)


class ReleaseContractTest(unittest.TestCase):
    def catalogue_data(self) -> dict[str, object]:
        return {
            "schemaVersion": 1,
            "repository": "james8464/powers-mod",
            "outputRoot": "build/release-envelope",
            "environmentAllowlist": ["JAVA_HOME", "GITHUB_SHA"],
            "commands": [{
                "id": "unit",
                "argv": ["python3", "-B", "fixture.py"],
                "validator": "command-receipt",
            }],
            "evidence": [{
                "id": "tests",
                "kind": "junit",
                "validator": "junit-xml",
            }],
            "artifacts": [{
                "id": "runtime-jar",
                "pathTemplate": "build/libs/powers-{version}.jar",
            }],
        }

    def write_json(self, path: Path, value: object) -> None:
        path.write_text(json.dumps(value), encoding="utf-8")

    def test_catalogue_loads_closed_immutable_models(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "catalogue.json"
            self.write_json(path, self.catalogue_data())

            catalogue = CONTRACT.load_catalogue(path)

            self.assertIsInstance(catalogue, CONTRACT.GateCatalogue)
            self.assertEqual(1, catalogue.schema_version)
            self.assertEqual("unit", catalogue.commands[0].id)
            self.assertEqual(("python3", "-B", "fixture.py"), catalogue.commands[0].argv)
            self.assertIsInstance(catalogue.commands[0], CONTRACT.Gate)
            with self.assertRaises(FrozenInstanceError):
                catalogue.commands[0].id = "changed"
            self.assertTrue(hasattr(CONTRACT, "CommandReceipt"))
            self.assertTrue(hasattr(CONTRACT, "EvidenceRow"))

    def test_catalogue_rejects_unknown_or_malformed_contracts(self):
        mutations = (
            ("schemaVersion", 2, "schemaVersion"),
            ("repository", "owner", "repository"),
            ("outputRoot", "/tmp/output", "outputRoot"),
            ("environmentAllowlist", ["PATH"], "environment"),
        )
        for field, value, expected in mutations:
            with self.subTest(field=field), tempfile.TemporaryDirectory() as raw:
                path = Path(raw) / "catalogue.json"
                data = self.catalogue_data()
                data[field] = value
                self.write_json(path, data)
                with self.assertRaisesRegex(CONTRACT.ReleaseContractError, expected):
                    CONTRACT.load_catalogue(path)

        cases = []
        data = self.catalogue_data()
        data["extra"] = True
        cases.append((data, "unknown catalogue field"))
        data = self.catalogue_data()
        data["commands"][0]["id"] = "../unit"
        cases.append((data, "command id"))
        data = self.catalogue_data()
        data["commands"][0]["argv"] = "python3 fixture.py"
        cases.append((data, "argv"))
        data = self.catalogue_data()
        data["commands"][0]["argv"] = []
        cases.append((data, "argv"))
        data = self.catalogue_data()
        data["commands"][0]["argv"] = ["python3", 7]
        cases.append((data, "argv"))
        data = self.catalogue_data()
        data["commands"][0]["validator"] = "dynamic.module"
        cases.append((data, "validator"))
        data = self.catalogue_data()
        data["commands"].append(dict(data["commands"][0]))
        cases.append((data, "duplicate command"))
        data = self.catalogue_data()
        data["evidence"][0]["kind"] = "unknown"
        cases.append((data, "evidence kind"))
        data = self.catalogue_data()
        data["evidence"][0]["validator"] = "dynamic"
        cases.append((data, "evidence validator"))
        data = self.catalogue_data()
        data["artifacts"][0]["pathTemplate"] = "../outside.jar"
        cases.append((data, "pathTemplate"))

        for data, expected in cases:
            with self.subTest(expected=expected), tempfile.TemporaryDirectory() as raw:
                path = Path(raw) / "catalogue.json"
                self.write_json(path, data)
                with self.assertRaisesRegex(CONTRACT.ReleaseContractError, expected):
                    CONTRACT.load_catalogue(path)

    def test_catalogue_rejects_mutable_urls_and_credentialed_urls(self):
        for url in (
                "http://example.invalid/artifact",
                "https://user:pass@example.invalid/artifact",
                "https://example.invalid/latest/artifact"):
            with self.subTest(url=url), tempfile.TemporaryDirectory() as raw:
                path = Path(raw) / "catalogue.json"
                data = self.catalogue_data()
                data["sourceUrls"] = [url]
                with self.assertRaisesRegex(CONTRACT.ReleaseContractError, "sourceUrl"):
                    self.write_json(path, data)
                    CONTRACT.load_catalogue(path)

    def test_evidence_manifest_is_strict_and_immutable(self):
        commit = "@HEAD"
        digest = "b" * 64
        data = {
            "schemaVersion": 1,
            "commit": commit,
            "rows": [{
                "id": "unit-tests",
                "kind": "junit",
                "validator": "junit-xml",
                "path": "evidence/junit.xml",
                "sha256": digest,
                "size": 42,
                "commit": commit,
                "producer": ["./gradlew", "test"],
                "result": {"tests": 1, "failures": 0},
                "limitations": [],
            }],
        }
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "evidence.json"
            self.write_json(path, data)
            manifest = CONTRACT.load_evidence_manifest(path)
            self.assertEqual(commit, manifest.commit)
            self.assertIsInstance(manifest.rows[0], CONTRACT.EvidenceRow)
            self.assertEqual(("./gradlew", "test"), manifest.rows[0].producer)
            with self.assertRaises(FrozenInstanceError):
                manifest.rows[0].size = 7

            for field, value, expected in (
                    ("path", "../junit.xml", "path"),
                    ("sha256", "B" * 64, "sha256"),
                    ("size", True, "size"),
                    ("commit", "short", "commit"),
                    ("producer", "./gradlew test", "producer")):
                broken = json.loads(json.dumps(data))
                broken["rows"][0][field] = value
                self.write_json(path, broken)
                with self.assertRaisesRegex(CONTRACT.ReleaseContractError, expected):
                    CONTRACT.load_evidence_manifest(path)

    def test_regular_snapshot_rejects_intermediate_symlinks_and_rechecks_identity(self):
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            root = directory / "root"
            root.mkdir()
            owned = root / "owned"
            owned.mkdir()
            source = owned / "evidence.txt"
            source.write_text("accepted bytes\n", encoding="utf-8")

            snapshot = CONTRACT.read_regular_snapshot(
                root, "owned/evidence.txt", maximum_bytes=1024)
            self.assertEqual(b"accepted bytes\n", snapshot.content)
            CONTRACT.recheck_regular_snapshot(snapshot)

            source.write_text("changed bytes\n", encoding="utf-8")
            with self.assertRaisesRegex(CONTRACT.ReleaseContractError, "changed after validation"):
                CONTRACT.recheck_regular_snapshot(snapshot)

            external = directory / "external"
            external.mkdir()
            (external / "evidence.txt").write_text("outside\n", encoding="utf-8")
            (root / "linked").symlink_to(external, target_is_directory=True)
            with self.assertRaisesRegex(CONTRACT.ReleaseContractError, "unsafe path"):
                CONTRACT.read_regular_snapshot(
                    root, "linked/evidence.txt", maximum_bytes=1024)

    def test_committed_evidence_manifest_accepts_only_head_binding_token(self):
        digest = "b" * 64
        base = {
            "schemaVersion": 1,
            "commit": "@HEAD",
            "rows": [{
                "id": "unit-tests",
                "kind": "junit",
                "validator": "junit-xml",
                "path": "evidence/junit.xml",
                "sha256": digest,
                "size": 42,
                "commit": "@HEAD",
                "producer": ["./gradlew", "test"],
                "result": {"tests": 1, "failures": 0},
                "limitations": [],
            }],
        }
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "evidence.json"
            self.write_json(path, base)
            manifest = CONTRACT.load_evidence_manifest(path)
            self.assertEqual("@HEAD", manifest.commit)
            self.assertEqual("@HEAD", manifest.rows[0].commit)
            for top, row, expected in (
                    ("@MAIN", "@MAIN", "commit"),
                    ("@HEAD", "a" * 40, "manifest mismatch"),
                    ("a" * 40, "@HEAD", "must use @HEAD"),
                    ("a" * 40, "a" * 40, "must use @HEAD")):
                broken = json.loads(json.dumps(base))
                broken["commit"] = top
                broken["rows"][0]["commit"] = row
                self.write_json(path, broken)
                with self.assertRaisesRegex(CONTRACT.ReleaseContractError, expected):
                    CONTRACT.load_evidence_manifest(path)

    def test_safe_regular_file_rejects_escape_links_hardlinks_and_special_files(self):
        with tempfile.TemporaryDirectory() as raw:
            base = Path(raw)
            root = base / "root"
            root.mkdir()
            regular = root / "regular.txt"
            regular.write_text("accepted", encoding="utf-8")
            self.assertEqual(regular, CONTRACT.safe_regular_file(root, "regular.txt"))

            outside = base / "outside.txt"
            outside.write_text("external", encoding="utf-8")
            (root / "linked.txt").symlink_to(outside)
            (root / "linked-dir").symlink_to(base, target_is_directory=True)
            hardlink = root / "hardlink.txt"
            os.link(outside, hardlink)
            fifo = root / "pipe"
            os.mkfifo(fifo)

            for relative in (
                    "../outside.txt", "/etc/passwd", "linked.txt",
                    "linked-dir/outside.txt", "hardlink.txt", "pipe", "."):
                with self.subTest(relative=relative), self.assertRaises(CONTRACT.ReleaseContractError):
                    CONTRACT.safe_regular_file(root, relative)
            self.assertEqual("external", outside.read_text(encoding="utf-8"))

            linked_root = base / "linked-root"
            linked_root.symlink_to(root, target_is_directory=True)
            with self.assertRaises(CONTRACT.ReleaseContractError):
                CONTRACT.safe_regular_file(linked_root, "regular.txt")

    def test_atomic_write_is_canonical_replaces_inode_and_preserves_hardlink_target(self):
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            target = directory / "value.json"
            external = directory / "external.json"
            external.write_bytes(b"external\n")
            os.link(external, target)
            old_inode = target.stat().st_ino

            digest = CONTRACT.write_json_atomic(target, {"z": 1, "a": [2, 1]})

            expected = b'{"a":[2,1],"z":1}\n'
            self.assertEqual(expected, target.read_bytes())
            self.assertEqual(b"external\n", external.read_bytes())
            self.assertEqual(old_inode, external.stat().st_ino)
            self.assertNotEqual(old_inode, target.stat().st_ino)
            self.assertEqual(CONTRACT.sha256_file(target), digest)
            self.assertEqual([], list(directory.glob(".value.json.*.tmp")))

    def test_atomic_write_cleans_exclusive_temporary_after_replace_failure(self):
        with tempfile.TemporaryDirectory() as raw:
            target = Path(raw) / "value.bin"
            with mock.patch.object(CONTRACT.os, "replace", side_effect=OSError("replace failed")):
                with self.assertRaises(OSError):
                    CONTRACT.write_bytes_atomic(target, b"new bytes")
            self.assertFalse(target.exists())
            self.assertEqual([], list(Path(raw).glob(".value.bin.*.tmp")))

    def test_privacy_check_rejects_sensitive_or_unowned_text(self):
        rejected = (
            "Authorization: Bearer abcdefghijklmnop",
            "https://user:secret@example.invalid/path",
            "home=/Users/alice/project",
            "uuid=123e4567-e89b-12d3-a456-426614174000",
            "client=203.0.113.8",
            "client=[2001:4860:4860::8888]:443",
            "token=ghp_abcdefghijklmnopqrstuvwxyz123456",
            "source=/private/tmp/foreign.log",
        )
        for value in rejected:
            with self.subTest(value=value), self.assertRaises(CONTRACT.ReleaseContractError):
                CONTRACT.validate_packaged_text(value)
        CONTRACT.validate_packaged_text(
            "path=docs/verification/report.md sha256=" + "a" * 64)


if __name__ == "__main__":
    unittest.main()
