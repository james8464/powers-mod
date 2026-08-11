#!/usr/bin/env python3
"""Generate the source-backed Light and Darkness rank appendix."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


ALIGNMENTS = (("Light", "light.json"), ("Darkness", "darkness.json"))


def perk_text(perk: dict) -> str:
    amount = f"{float(perk['amount']) * 100:g}%"
    aspect = str(perk.get("actionOrAspect", ""))
    suffix = f" ({aspect})" if aspect else ""
    return f"`{perk['type']} +{amount}{suffix}`"


def render(root: Path) -> str:
    ranks = root / "src/main/resources/data/powers/ranks"
    rows: list[str] = []
    for alignment, filename in ALIGNMENTS:
        nodes = json.loads((ranks / filename).read_text(encoding="utf-8"))
        for node in sorted(nodes, key=lambda value: (int(value["depth"]), str(value["id"]))):
            parents = ", ".join(f"`{parent}`" for parent in node["parents"]) or "—"
            perks = "<br>".join(perk_text(perk) for perk in node["perks"])
            route = "Canonical" if node["canonical"] else "Optional"
            title = str(node["title"]).replace("|", "\\|")
            rows.append(
                f"| {alignment} | `{node['id']}` | {node['depth']} | `{node['branch']}` | "
                f"{title} | {route} | {parents} | {perks} |"
            )
    return """# Rank catalogue

This generated appendix is sourced from the Light and Darkness rank registries.

| Alignment | ID | Depth | Branch | Title | Route | Parents | Perks |
|---|---|---:|---|---|---|---|---|
""" + "\n".join(rows) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    root = args.root.resolve()
    target = root / "docs/gameplay/rank-catalogue.md"
    expected = render(root)
    if args.check:
        actual = target.read_text(encoding="utf-8") if target.exists() else None
        if actual != expected:
            print(f"Generated rank documentation is stale: {target}", file=sys.stderr)
            return 1
        return 0
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(expected, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
