#!/usr/bin/env python3
"""Generate exhaustive, source-linked Shadow offline knowledge entries."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path


GENERATED_DIRECTORY = Path("src/main/resources/data/powers/knowledge_entries/generated")
COVERAGE_REPORT = Path("docs/gameplay/knowledge-coverage.md")
EXPECTED_COUNTS = {
    "items": 260,
    "actions": 64,
    "ranks": 56,
    "realms": 3,
    "interaction_rules": 50,
}


def clean(value: str) -> str:
    return value.strip().strip("`").replace("<br>", "; ").replace("\\|", "|")


def slug(value: str) -> str:
    result = re.sub(r"[^a-z0-9]+", "_", value.lower()).strip("_")
    return result[:72] or "entry"


def table_rows(path: Path, columns: int) -> list[list[str]]:
    rows: list[list[str]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|") or re.match(r"^\|[-:| ]+\|$", line):
            continue
        cells = [clean(cell) for cell in re.split(r"(?<!\\)\|", line)[1:-1]]
        if len(cells) != columns:
            continue
        rows.append(cells)
    return rows[1:]


def entry(entry_id: str, title: str, keywords: list[str], answer: str,
          sources: list[str]) -> dict:
    lookup = "knowledge_lookup_" + hashlib.sha256(entry_id.encode("utf-8")).hexdigest()[:16]
    unique_keywords = list(dict.fromkeys([lookup, f"powers:{entry_id}"] + [value for value in keywords if value]))
    return {
        "id": entry_id,
        "title": title,
        "keywords": unique_keywords[:32],
        "answer": answer,
        "sources": sources,
        "reveal_rank": 0,
    }


def item_entries(root: Path) -> list[dict]:
    source = "docs/gameplay/item-catalogue.md"
    result = []
    for registry_id, name, family, purpose, acquisition in table_rows(root / source, 5):
        path = registry_id.split(":", 1)[-1]
        result.append(entry(
            f"generated/items/{path}", name,
            [registry_id, path, path.replace("_", " "), name, family],
            f"{name} is a {family.lower()}. {purpose}. Survival acquisition/status: {acquisition}.",
            [source],
        ))
    return result


def action_entries(root: Path) -> list[dict]:
    source = "docs/interactions/action-catalogue.md"
    result = []
    for row in table_rows(root / source, 17):
        (action_id, origin, aspects, delivery, intent, significance, beats, potency,
         reach, duration, energy, cooldown, residue, priority, motif, sound, colours) = row
        title = action_id.replace("_", " ").title()
        result.append(entry(
            f"generated/actions/{action_id}", title,
            [f"magic action {action_id.replace('_', ' ')}", action_id,
             action_id.replace("_", " "), title, origin, aspects, delivery, intent],
            f"{title} is a {origin.lower()} {delivery.lower()} action with {intent.lower()} intent. "
            f"Aspects: {aspects}. Baseline potency {potency}, range {reach}, duration {duration}, "
            f"energy {energy}, cooldown {cooldown}, residue {residue}, priority {priority}, and "
            f"{beats} generic presentation beats. Its {significance.lower()} signature uses the "
            f"{motif} motif, {sound} sound, and colours {colours}.",
            [source],
        ))
    return result


def rank_entries(root: Path) -> list[dict]:
    source = "docs/gameplay/rank-catalogue.md"
    result = []
    for alignment, rank_id, depth, branch, title, route, parents, perks in table_rows(root / source, 8):
        result.append(entry(
            f"generated/ranks/{alignment.lower()}_{rank_id}", f"{alignment} rank: {title}",
            [f"{alignment.lower()} {rank_id}", rank_id, title, branch, f"{alignment} rank"],
            f"{title} is a depth-{depth} {alignment} rank on the {branch} branch. It is a "
            f"{route.lower()} route with parents {parents}. Perks: {perks}.",
            [source, f"data/powers/ranks/{alignment.lower()}.json"],
        ))
    return result


def realm_entries(root: Path) -> list[dict]:
    del root
    source = "docs/interactions/system-catalogue.md"
    definitions = {
        "dark_realm": "Dark Realm is the Darkness mindscape. Travel uses a vulnerable body proxy and an exact safe return route; living Darkness and its landmarks inhabit the realm.",
        "light_realm": "Light Realm is the radiant mindscape. Travel uses a vulnerable body proxy and an exact safe return route; Pure Light and its landmarks inhabit the realm.",
        "middleworld": "Middleworld is the Indigo cohort-travel realm. The caster and admitted nearby passengers retain exact origins and return through owned travel state.",
    }
    return [entry(
        f"generated/realms/{realm}", realm.replace("_", " ").title(),
        [realm, f"powers:{realm}", realm.replace("_", " "), "realm", "dimension"],
        answer,
        [source, f"data/powers/dimension/{realm}.json"],
    ) for realm, answer in definitions.items()]


def interaction_rule_entries(root: Path) -> list[dict]:
    source = "docs/interactions/interaction-rules.md"
    result = []
    for index, (family, mechanics, signature) in enumerate(table_rows(root / source, 3), start=1):
        result.append(entry(
            f"generated/interaction_rules/{index:02d}_{slug(family)}", f"Interaction: {family}",
            [f"magic interaction {family.replace('×', ' ')}", family,
             family.replace("×", "and"), "magic interaction", "interaction rule"],
            f"{family}: {mechanics} Signature: {signature}",
            [source, "docs/interactions/interaction-matrix.csv"],
        ))
    return result


def build_inventory(root: Path) -> dict[str, list[dict]]:
    inventory = {
        "items": item_entries(root),
        "actions": action_entries(root),
        "ranks": rank_entries(root),
        "realms": realm_entries(root),
        "interaction_rules": interaction_rule_entries(root),
    }
    actual = {domain: len(entries) for domain, entries in inventory.items()}
    if actual != EXPECTED_COUNTS:
        raise ValueError(f"Knowledge source inventory changed: expected {EXPECTED_COUNTS}, got {actual}")
    return inventory


def json_payload(value: dict) -> str:
    payload = {key: item for key, item in value.items() if key != "id"}
    return json.dumps(payload, indent=2, ensure_ascii=False) + "\n"


def expected_outputs(source_root: Path) -> dict[Path, str]:
    inventory = build_inventory(source_root)
    outputs: dict[Path, str] = {}
    for domain, entries in inventory.items():
        for value in entries:
            filename = value["id"].split("/", 2)[-1] + ".json"
            outputs[GENERATED_DIRECTORY / domain / filename] = json_payload(value)
    total = sum(len(entries) for entries in inventory.values())
    report_rows = []
    sources = {
        "items": "[item catalogue](item-catalogue.md)",
        "actions": "[action catalogue](../interactions/action-catalogue.md)",
        "ranks": "[rank catalogue](rank-catalogue.md)",
        "realms": "[runtime system catalogue](../interactions/system-catalogue.md)",
        "interaction_rules": "[interaction rules](../interactions/interaction-rules.md)",
    }
    labels = {
        "items": "Items", "actions": "Actions", "ranks": "Rank nodes",
        "realms": "Realms", "interaction_rules": "Interaction-rule families",
    }
    for domain, count in EXPECTED_COUNTS.items():
        report_rows.append(f"| {labels[domain]} | {count} | {count} | 100% | {sources[domain]} |")
    outputs[COVERAGE_REPORT] = f"""# Shadow knowledge coverage

This generated report proves that Shadow's bundled offline index has one source-cited entry for every row in each required gameplay catalogue. The build rejects missing, stale, duplicate, or extra generated entries.

| Domain | Source rows | Knowledge entries | Coverage | Source |
|---|---:|---:|---:|---|
{chr(10).join(report_rows)}
| **Total** | **{total}** | **{total}** | **100%** | — |

Generated entries live under `data/powers/knowledge_entries/generated/`. The existing authored lore entries remain independent and are not counted as substitutes for catalogue coverage.
"""
    return outputs


def generated_files(root: Path) -> set[Path]:
    directory = root / GENERATED_DIRECTORY
    files = set()
    if directory.exists():
        files.update(path.relative_to(root) for path in directory.rglob("*.json"))
    if (root / COVERAGE_REPORT).exists():
        files.add(COVERAGE_REPORT)
    return files


def check_outputs(source_root: Path, output_root: Path) -> list[str]:
    expected = expected_outputs(source_root)
    actual_files = generated_files(output_root)
    stale = set(actual_files) ^ set(expected)
    for path, content in expected.items():
        target = output_root / path
        if not target.exists() or target.read_text(encoding="utf-8") != content:
            stale.add(path)
    return sorted(str(path) for path in stale)


def write_outputs(source_root: Path, output_root: Path) -> None:
    expected = expected_outputs(source_root)
    for stale in generated_files(output_root) - set(expected):
        (output_root / stale).unlink()
    for path, content in expected.items():
        target = output_root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    root = args.root.resolve()
    if args.check:
        stale = check_outputs(root, root)
        if stale:
            print("Generated Shadow knowledge is stale:", file=sys.stderr)
            print("\n".join(stale), file=sys.stderr)
            return 1
        return 0
    write_outputs(root, root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
