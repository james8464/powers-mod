#!/usr/bin/env python3
"""Generate the source-backed Light and Darkness rank appendix."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


ALIGNMENTS = (("Light", "light.json"), ("Darkness", "darkness.json"))

MECHANIC_NAMES = {
    "POWER_DAMAGE": "Potency multiplier",
    "HEALING": "Healing multiplier",
    "CONTROL": "Control-strength multiplier",
    "RANGE": "Reach multiplier",
    "DURATION": "Duration multiplier",
    "ENERGY_CAPACITY": "Energy-capacity multiplier",
    "ENERGY_REGEN": "Energy-regeneration multiplier",
    "ENERGY_COST_REDUCTION": "Energy-cost reduction",
    "COOLDOWN_REDUCTION": "Cooldown reduction",
    "RESISTANCE": "Damage-resistance multiplier",
    "MOVEMENT": "Movement-speed multiplier",
    "WARD_INTEGRITY": "Ward-integrity multiplier",
    "STEALTH": "Stealth-strength multiplier",
    "REVEAL": "Reveal-strength multiplier",
    "SOUL": "Soul-force multiplier",
    "SUMMON": "Summon-force multiplier",
    "KNOCKBACK": "Knockback multiplier",
    "INTERACTION_PRIORITY": "Interaction-priority bonus",
    "BACKLASH_REDUCTION": "Backlash reduction",
}
REDUCTIONS = {"ENERGY_COST_REDUCTION", "COOLDOWN_REDUCTION", "BACKLASH_REDUCTION"}
ADDITIONS = {"INTERACTION_PRIORITY"}
MECHANIC_CAPS = {
    "POWER_DAMAGE": 0.40,
    "HEALING": 0.40,
    "CONTROL": 0.40,
    "RANGE": 0.35,
    "DURATION": 0.35,
    "ENERGY_CAPACITY": 0.50,
    "ENERGY_REGEN": 0.40,
    "ENERGY_COST_REDUCTION": 0.25,
    "COOLDOWN_REDUCTION": 0.25,
    "RESISTANCE": 0.20,
    "MOVEMENT": 0.35,
    "WARD_INTEGRITY": 0.50,
    "STEALTH": 0.35,
    "REVEAL": 0.35,
    "SOUL": 0.40,
    "SUMMON": 0.35,
    "KNOCKBACK": 0.35,
    "INTERACTION_PRIORITY": 0.25,
    "BACKLASH_REDUCTION": 0.35,
}


def mechanic_text(perk: dict) -> str:
    name = MECHANIC_NAMES[str(perk["type"])]
    aspect = str(perk.get("actionOrAspect", ""))
    return f"{name} (`{aspect}`)" if aspect else name


def format_number(value: float) -> str:
    return f"{value:.6f}".rstrip("0").rstrip(".")


def execute(perk: dict, focused: bool) -> float:
    mechanic = str(perk["type"])
    amount = min(MECHANIC_CAPS[mechanic],
                 float(perk["amount"]) * (1.5 if focused else 1.0))
    if mechanic in REDUCTIONS:
        return 100.0 * (1.0 - amount)
    if mechanic in ADDITIONS:
        return 100.0 + amount
    return 100.0 * (1.0 + amount)


def proof_text(perk: dict) -> str:
    active = format_number(execute(perk, False))
    focused = format_number(execute(perk, True))
    return f"`100 → {active} → {focused}`"


def render(root: Path, expected_nodes: int) -> str:
    ranks = root / "src/main/resources/data/powers/ranks"
    rows: list[str] = []
    identities: set[tuple[str, str]] = set()
    for alignment, filename in ALIGNMENTS:
        nodes = json.loads((ranks / filename).read_text(encoding="utf-8"))
        for node in sorted(nodes, key=lambda value: (int(value["depth"]), str(value["id"]))):
            identity = (alignment, str(node["id"]))
            if identity in identities:
                raise ValueError(f"Duplicate rank node: {alignment} {node['id']}")
            identities.add(identity)
            if not node["perks"]:
                raise ValueError(f"Rank node has no executable mechanic: {alignment} {node['id']}")
            parents = ", ".join(f"`{parent}`" for parent in node["parents"]) or "—"
            mechanics = "<br>".join(mechanic_text(perk) for perk in node["perks"])
            proofs = "<br>".join(proof_text(perk) for perk in node["perks"])
            route = "Canonical" if node["canonical"] else "Optional"
            title = str(node["title"]).replace("|", "\\|")
            rows.append(
                f"| {alignment} | `{node['id']}` | {node['depth']} | `{node['branch']}` | "
                f"{title} | {route} | {parents} | {mechanics} | {proofs} |"
            )
    if len(identities) != expected_nodes:
        raise ValueError(f"Expected {expected_nodes} rank nodes, found {len(identities)}")
    return """# Rank catalogue

This generated appendix is sourced from the Light and Darkness rank registries. Every row names
the executable numerical mechanic and proves its ordinary/focused output from a baseline of 100.

| Alignment | ID | Depth | Branch | Title | Route | Parents | Mechanic | Profile proof (base → active → focused) |
|---|---|---:|---|---|---|---|---|---|
""" + "\n".join(rows) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--expected-nodes", type=int, default=56)
    args = parser.parse_args()
    root = args.root.resolve()
    target = root / "docs/gameplay/rank-catalogue.md"
    expected = render(root, args.expected_nodes)
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
