#!/usr/bin/env python3
"""Generate or verify the exact-version Java source audit manifest."""

from __future__ import annotations

import argparse
import hashlib
import re
from pathlib import Path


SOURCE_ROOTS = (Path("src/main/java"), Path("src/client/java"))
OUTPUT = Path("docs/quality/code-audit.md")
PUBLIC_TYPE = re.compile(
    r"(?m)^public\s+(?:(?:final|abstract|sealed|non-sealed)\s+)*(?:class|interface|record|enum)\s+"
)
JAVADOC = re.compile(r"/\*\*(.*?)\*/", re.DOTALL)

PACKAGE_OWNERSHIP = {
    "ai": "Per-level per-tick immutable perception snapshots; replaced each tick and cleared on server stop.",
    "magic.runtime": "Bounded per-server cast residues; cleared on disconnect, respawn, and stop.",
    "magic": "Immutable process-wide catalogue and pure deterministic resolver.",
    "network": "Registered once; packet input is validated and executed on the server thread.",
    "player": "Persistent player attachments plus bounded per-session identity caches.",
    "power.abilities": "Ability-local transient state; lifecycle hooks clear owner and server state.",
    "power.crystals": "Crystal selection and ability-local transient state; cleared at lifecycle edges.",
    "power.state": "Indexed entity ownership; explicit release and server-stop cleanup.",
    "power.travel": "Stateless validation against loaded server world state.",
    "spell": "Server-owned channels and fields; ticked and cleared by the server lifecycle.",
    "realm": "Server-owned mindscape sessions and layout state; restored and cleared explicitly.",
    "fx": "Per-tick particle budgets and stateless presentation helpers.",
    "client.screen": "Client screen instance state; server revalidates every submitted request.",
    "client": "Client-only synchronized mirrors and rendering state.",
    "mixin": "No independent ownership; delegates narrow hooks to server policy.",
}

PURE_VISUAL_SCAR_FILES = {
    "ClientVisualScarState.java",
    "ScarFxProtocolRules.java",
    "VisualScarDeliveryRules.java",
    "VisualScarDeliveryModel.java",
    "VisualScarExpiryIndex.java",
    "VisualScarGeometry.java",
    "VisualScarLedgerRules.java",
    "VisualScarMotifGeometry.java",
    "VisualScarPresentation.java",
    "VisualScarRevalidationRing.java",
    "VisualScarRequestQueue.java",
    "VisualScarRules.java",
}


def package_suffix(path: Path) -> str:
    text = path.as_posix()
    marker = "/com/powers/"
    if marker not in text:
        return ""
    suffix = text.split(marker, 1)[1].rsplit("/", 1)[0] if "/" in text.split(marker, 1)[1] else ""
    return suffix.replace("/", ".")


def ownership(path: Path) -> str:
    if path.name in PURE_VISUAL_SCAR_FILES:
        return "Pure bounded value/state models; runtime owners supply lifecycle and thread confinement."
    package = package_suffix(path)
    for prefix, description in sorted(PACKAGE_OWNERSHIP.items(), key=lambda item: -len(item[0])):
        if package == prefix or package.startswith(prefix + "."):
            return description
    return "Registered immutable definitions or lifecycle-owned server state; see type contract."


def authority(path: Path) -> str:
    text = path.as_posix()
    package = package_suffix(path)
    if text.startswith("src/client/"):
        return "Client render/input thread; presentation only."
    if path.name in PURE_VISUAL_SCAR_FILES:
        return "Pure Java logic; no Minecraft, Fabric, world, network, or render dependency."
    if package == "magic" or package in {"hud", "progression"}:
        return "Pure/immutable logic; callable from tests, mutations remain server-owned."
    return "Server-authoritative; mutable Minecraft state is touched on the server thread."


def first_javadoc(source: str) -> str:
    public_type = PUBLIC_TYPE.search(source)
    match = None
    if public_type:
        candidates = [candidate for candidate in JAVADOC.finditer(source, 0, public_type.start())]
        if candidates:
            candidate = candidates[-1]
            between = source[candidate.end():public_type.start()]
            if not between.strip() or all(
                line.lstrip().startswith("@") for line in between.splitlines() if line.strip()
            ):
                match = candidate
    if match is None:
        match = JAVADOC.search(source)
    if not match:
        return "Internal implementation documented by its package and call-site contracts."
    lines = []
    for raw in match.group(1).splitlines():
        line = re.sub(r"^\s*\*\s?", "", raw).strip()
        if line and not line.startswith("@"):
            lines.append(line)
    summary = " ".join(lines)
    summary = re.sub(r"\{@(?:link|code)\s+([^}]+)}", r"\1", summary)
    summary = re.sub(r"<[^>]+>", "", summary)
    sentence = summary.split(". ", 1)[0].rstrip(".") + "." if summary else "Documented source unit."
    return sentence.replace("|", "\\|")


def contract(source: str, path: Path) -> str:
    if path.name == "package-info.java":
        return "Package boundary and responsibility contract."
    if PUBLIC_TYPE.search(source):
        return "Public API; behavior, validation, and invariants documented in source."
    return "Package-private implementation; exposed only through documented owning APIs."


def source_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for source_root in SOURCE_ROOTS:
        directory = root / source_root
        if directory.exists():
            files.extend(path for path in directory.rglob("*.java") if path.is_file())
    return sorted(files, key=lambda path: path.relative_to(root).as_posix())


def render(root: Path) -> str:
    rows = []
    for path in source_files(root):
        source = path.read_text(encoding="utf-8")
        relative = path.relative_to(root).as_posix()
        digest = hashlib.sha256(source.encode("utf-8")).hexdigest()[:12]
        lines = len(source.splitlines())
        rows.append(
            f"| `{relative}` | {lines} | `{digest}` | {first_javadoc(source)} | "
            f"{contract(source, path)} | {ownership(path)} | {authority(path)} | "
            "Tracked exact version; semantic review requires tests and human/code-review evidence. |"
        )

    header = """# Java source audit

This manifest pins the exact SHA-256 prefix and line count of every production Java source included in the quality pass. A matching digest proves file identity only; it is not evidence of semantic line-by-line review. Regenerate it after an intentional source change. CI separately rejects missing, extra, stale, wildcard-import, debug, unfinished, undocumented, or oversized source units, while behavior is supported by focused tests and explicit review evidence.

| Source | Lines | SHA-256 | Responsibility | Public contract | Ownership / lifecycle | Thread / authority | Findings and resolution |
|---|---:|---|---|---|---|---|---|
"""
    return header + "\n".join(rows) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    expected = render(root)
    destination = root / OUTPUT
    if args.check:
        if not destination.exists() or destination.read_text(encoding="utf-8") != expected:
            raise SystemExit("Java source audit is stale; run scripts/audit_java_sources.py")
        return 0
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(expected, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
