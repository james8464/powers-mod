#!/usr/bin/env python3
"""Generate deed-controlled progression advancements deterministically."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ADVANCEMENT_ROOT = ROOT / "src/main/resources/data/powers/advancement"
DARKNESS_LEVELS = (
    ("coal_block", "01_embers"),
    ("bone_block", "02_nether_foundations"),
    ("iron_golem_spawn_egg", "03_black_stone"),
    ("wolf_spawn_egg", "04_poisons"),
    ("villager_spawn_egg", "05_dark_barters"),
    ("soul_lantern", "06_fortress"),
    ("wither_rose", "07_wither_court"),
    ("crying_obsidian", "08_end_warding"),
    ("echo_shard", "09_end_city"),
    ("dragon_head", "10_ascension"),
)
SKILL_LEVELS = (
    ("amethyst_shard", "01_shard"),
    ("powers:rainbow_crystal", "02_crystal_bread"),
    ("powers:amethyst_ward", "03_ward_grimoire"),
    ("powers:imported_artifact_runestone_frigid", "04_rune_realm"),
    ("netherite_sword", "05_realms_fight"),
    ("diamond_sword", "06_nether_hunts"),
    ("powers:light_crystal", "07_end_secrets"),
    ("ender_eye", "08_crystal_travel"),
    ("nether_star", "09_deep_dark"),
    ("dragon_head", "10_capstone"),
)


def advancement(path: str, criterion: str, level: int, icon: str,
                translation: str) -> dict[str, object]:
    """Build one server-counter advancement in its visible, ordered chain."""
    parent = f"powers:{path}_root" if level == 1 else f"powers:{path}/level_{level - 1:02d}"
    key = f"advancement.powers.{path}.{translation}"
    # Unqualified icons are vanilla; imported and custom icons declare their namespace.
    icon_id = icon if ":" in icon else f"minecraft:{icon}"
    return {
        "parent": parent,
        "display": {
            "icon": {"id": icon_id},
            "title": {"translate": f"{key}.title"},
            "description": {"translate": f"{key}.description"},
            "frame": "challenge" if level == 10 else "task",
            "show_toast": True,
            "announce_to_chat": False,
            "hidden": False,
        },
        "criteria": {criterion: {"trigger": "minecraft:impossible"}},
    }


def main() -> None:
    """Rewrite both generated paths using stable ordering and tabs."""
    for path, criterion, levels in (
            ("darkness", "deed", DARKNESS_LEVELS),
            ("skill", "mastery", SKILL_LEVELS)):
        output = ADVANCEMENT_ROOT / path
        output.mkdir(parents=True, exist_ok=True)
        for level, (icon, translation) in enumerate(levels, start=1):
            target = output / f"level_{level:02d}.json"
            data = advancement(path, criterion, level, icon, translation)
            target.write_text(json.dumps(data, indent="\t") + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
