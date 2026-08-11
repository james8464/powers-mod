#!/usr/bin/env python3
"""Replace texture-file labels with concise in-world English item names."""

import json
from pathlib import Path


LANG = Path("src/main/resources/assets/powers/lang/en_us.json")


def title(words: list[str]) -> str:
    return " ".join(word.capitalize() for word in words if word)


def food_name(parts: list[str]) -> str:
    special = {
        "apple_green": "Green Apple",
        "apple_wormy": "Worm-Eaten Apple",
        "apple_wormy_2": "Rotten Worm-Eaten Apple",
        "bread_big": "Large Loaf",
        "coconut_normal": "Coconut",
        "coconut_opened": "Opened Coconut",
        "coconut_straw": "Coconut with Straw",
        "stew_sweetpod": "Sweetpod Stew",
    }
    raw = "_".join(parts)
    if raw in special:
        return special[raw]
    adjectives = {"raw": "Raw", "cooked": "Cooked", "smoked": "Smoked", "salted": "Salted"}
    modifier = next((part for part in parts if part in adjectives), None)
    nouns = [part for part in parts if part != modifier]
    if nouns and nouns[0] == "slab":
        nouns = nouns[1:] + ["slab"]
    return (adjectives[modifier] + " " if modifier else "") + title(nouns)


def artifact_name(parts: list[str]) -> str:
    raw = "_".join(parts)
    special = {
        "philosopherstone": "Philosopher's Stone",
        "ritualdagger": "Ritual Dagger",
        "soulmatrix": "Soul Matrix",
        "woodheart": "Heart of the Wildwood",
        "ghoul_heart": "Ghoul Heart",
        "beating_heart": "Living Heart",
        "heart_mechanism": "Clockwork Heart",
        "dark_bone_figurine": "Dark Bone Figurine",
        "bonefigurine": "Bone Figurine",
        "trilobitefossil": "Trilobite Fossil",
        "trilobite_fossil": "Trilobite Fossil",
        "emperyeanjewel": "Empyrean Jewel",
        "malignember": "Malign Ember",
        "blackpearl": "Black Pearl",
        "smallpot": "Small Ritual Pot",
    }
    if raw in special:
        return special[raw]
    if parts and parts[0] == "soulstone":
        modifiers = [word.capitalize() for word in parts[1:]]
        return " ".join(modifiers + ["Soulstone"])
    if "runestone" in parts:
        modifiers = [word.capitalize() for word in parts if word not in {"runestone", "overlay", "back"}]
        return " ".join(modifiers + ["Runestone"]) or "Runestone"
    if parts and parts[-1] in {"active", "inert", "animated"}:
        return title([parts[-1]] + parts[:-1])
    return title(parts)


def normalized(texture: str) -> str:
    parts = texture.split("_")
    family, rest = parts[0], parts[1:]
    if family == "food":
        return food_name(rest)
    if family == "artifact":
        return artifact_name(rest)
    if family == "device":
        return "Active Miniportal" if rest[-1:] == ["active"] else "Miniportal"
    if family == "magic" and rest[:1] == ["essence"]:
        return title(rest[1:-1] + ["Essence", rest[-1]])
    if family == "blood" and rest[:1] == ["salts"]:
        return "Blood Salts"
    if family == "book":
        grimoire_names = {
            "grimoire_abyssal": "The Abyssal Codex",
            "grimoire_blight": "The Blighted Testament",
            "grimoire_celestial": "The Celestial Grimoire",
            "grimoire_deep": "The Deepbound Grimoire",
            "grimoire_infernal": "The Infernal Ledger",
            "grimoire_recolor": "Unbound Grimoire",
            "grimoire_wild": "The Verdant Canticle",
            "grimoire_recolor_overlay_abyssal": "Abyssal Grimoire Leaf",
            "grimoire_recolor_overlay_blight": "Blighted Grimoire Leaf",
            "grimoire_recolor_overlay_celestial": "Celestial Grimoire Leaf",
            "grimoire_recolor_overlay_deep": "Deepbound Grimoire Leaf",
            "grimoire_recolor_overlay_infernal": "Infernal Grimoire Leaf",
            "grimoire_recolor_overlay_wild": "Wild Grimoire Leaf",
        }
        raw = "_".join(rest)
        if raw in grimoire_names:
            return grimoire_names[raw]
        if rest == ["page", "written"]:
            return "Written Grimoire Page"
        if rest == ["tattered"]:
            return "Tattered Grimoire"
    return title(parts)


def main() -> None:
    language = json.loads(LANG.read_text(encoding="utf-8"))
    prefix = "item.powers.imported_"
    for key in list(language):
        if key.startswith(prefix):
            language[key] = normalized(key[len(prefix):])
    LANG.write_text(json.dumps(language, indent="\t", ensure_ascii=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
