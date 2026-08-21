import importlib.util
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("audit_java_sources", ROOT / "scripts" / "audit_java_sources.py")
AUDIT = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(AUDIT)


class AuditJavaSourcesTest(unittest.TestCase):
    def test_manifest_responsibility_uses_public_type_contract(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/main/java/com/example/Example.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                """package com.example;

/** Internal helper note. */
final class Helper { }

/** Authoritative public contract. */
public final class Example { }
""",
                encoding="utf-8",
            )

            rendered = AUDIT.render(root)

            self.assertIn("Authoritative public contract.", rendered)
            self.assertNotIn("Internal helper note.", rendered)


if __name__ == "__main__":
    unittest.main()
