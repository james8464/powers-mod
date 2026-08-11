#!/usr/bin/env python3
"""Generate the exhaustive purpose/acquisition catalogue from registry sources."""

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LANG = json.loads((ROOT / "src/main/resources/assets/powers/lang/en_us.json").read_text())


def imported_textures() -> list[str]:
    source = (ROOT / "src/main/java/com/powers/ImportedPackItems.java").read_text()
    body = re.search(r'private static final String\[\] TEXTURES = """(.*?)"""\.split', source, re.S).group(1)
    return [value.strip() for value in body.split(",") if value.strip()]


def weapons() -> list[str]:
    source = (ROOT / "src/main/java/com/powers/PowersWeapons.java").read_text()
    return re.findall(r'new WeaponDef\("([a-z0-9_]+)"', source)


def imported_role(texture: str) -> tuple[str, str, str]:
    if texture == "device_miniportal_active" or texture == "artifact_runestone_back" or "runestone_overlay_" in texture:
        return "Compatibility asset alias", "Hidden; retained only for old saves and model composition", "Deferred/hidden"
    if texture.startswith("food_"):
        return "Provision", "Edible food; cooked and smoked forms restore more hunger", "Themed mob/block loot and village provision salvage; processed variants may also be cooked"
    if texture.startswith("book_grimoire"):
        return "Grimoire", "Selects and channels the documented spells of one school", "Stronghold library/corridor discovery"
    if texture.startswith("book_"):
        return "Lore", "Archaeological grimoire fragment interpreted by Shadow", "Stronghold discovery"
    if "runestone" in texture:
        return "Runestone", "Finite tiered energy restoration and Arcane Crucible infusion", "Crafting upgrade chain plus dungeon, ancient-city, and trial rewards"
    if "ring" in texture or "amulet" in texture:
        return "Attunement", "Passive energy restoration and resistance while carried", "Thematic archaeology and structure salvage"
    if "soulstone" in texture or "soulmatrix" in texture:
        return "Soul vessel", "Bounded nonlethal soul drain; the Matrix also stores passive energy", "Nether-fortress and archaeology salvage"
    if "heart" in texture:
        return "Heart relic", "Vitality, nature, or necromantic healing", "Settlement and archaeology salvage"
    if "ritualdagger" in texture:
        return "Ritual catalyst", "Trades health to amplify the next grimoire ritual", "Woodland-mansion and archaeology salvage"
    if "philosopherstone" in texture:
        return "Transmuter", "Controlled stone/deepslate/netherrack/end-stone transmutation", "Rare woodland-mansion and archaeology salvage"
    if "lodestone" in texture:
        return "Travel relic", "Binds a safe same-dimension destination for a Miniportal", "Desert-pyramid and archaeology salvage"
    if texture == "device_miniportal":
        return "Travel relic", "Two safe same-dimension trips; dropped amethyst restores both charges", "Guaranteed First Vessel drop and rare ruined-portal salvage"
    if "flute" in texture:
        return "Command relic", "Recalls, heals, and aligns nearby player-shaped guardians", "Jungle-temple and archaeology salvage"
    if texture.startswith("magic_essence_") or texture.startswith("blood_salts"):
        return "Arcane catalyst", "Named spell-school and Arcane Crucible reagent", "Buried-treasure, fortress, and archaeology salvage"
    return "Relic or lore catalyst", "Arcane Crucible reagent or memory fragment interpreted by Shadow", "Thematic archaeology and structure salvage"


def row(item: str, name: str, family: str, purpose: str, acquisition: str) -> str:
    return f"| `{item}` | {name} | {family} | {purpose} | {acquisition} |"


def render() -> str:
    rows: list[str] = []
    crystals = ["rainbow_crystal", "red_crystal", "orange_crystal", "yellow_crystal",
                "green_crystal", "blue_crystal", "indigo_crystal", "violet_crystal",
                "light_crystal", "dark_crystal"]
    for item in crystals:
        rows.append(row(f"powers:{item}", LANG.get(f"item.powers.{item}", item), "Crystal",
                        "Selects the crystal powers documented in README",
                        "Story acquisition intentionally deferred; no crafting recipe; operator testing only"))
    rows.append(row("powers:infected_rainbow_crystal", "Legacy Fractured Crystal", "Compatibility alias",
                    "Inert; Rainbow appearance now derives from holder alignment", "Hidden; old-save compatibility only"))

    blocks = {
        "darkness": ("Living force", "Spreading Darkness realm matter", "Dark Realm generation, Blight Ground, and invasion scars"),
        "pure_light": ("Living force", "Spreading Pure Light realm matter", "Light Realm generation, consecration, and invasion scars"),
        "amethyst_ward": ("Counterplay", "Powered dampening and force-containment ceremony", "Crafting recipe"),
        "arcane_crucible": ("Forge", "Atomic staged magical weapon conversion", "Crafting recipe"),
        "light_memory_obelisk": ("Realm structure", "Light memory landmark core", "Generated in the Light Realm; not a survival drop"),
        "dark_memory_obelisk": ("Realm structure", "Dark memory landmark core", "Generated in the Dark Realm; not a survival drop"),
    }
    for item, (family, purpose, acquisition) in blocks.items():
        rows.append(row(f"powers:{item}", LANG.get(f"block.powers.{item}", item), family, purpose, acquisition))

    eggs = ["darkness_creature", "power_test_actor", "radiant_sentinel", "first_vessel", "dark_herald", "light_herald"]
    for entity in eggs:
        item = f"{entity}_spawn_egg"
        rows.append(row(f"powers:{item}", LANG.get(f"item.powers.{item}", item), "Testing tool",
                        f"Spawns {LANG.get(f'entity.powers.{entity}', entity)} for controlled testing",
                        "Creative/operator testing only"))

    for weapon in weapons():
        name = LANG.get(f"item.powers.{weapon}", weapon.replace("_", " ").title())
        if weapon == "lycanbane":
            rows.append(row("powers:lycanbane", name, "Mythic Darkness artifact",
                            "All routed powers plus exactly three Shadow rites", "Guaranteed Dark Herald drop"))
        elif weapon == "heavenly_partisan":
            rows.append(row("powers:heavenly_partisan", name, "Mythic Light artifact",
                            "Curated radiant dominions and routed powers", "Guaranteed Light Herald drop"))
        else:
            rows.append(row(f"powers:{weapon}", name, "Fantasy weapon",
                            "One of twelve named combat archetypes; tooltip identifies its proc",
                            "Low-chance archetype-themed chest or mob loot"))

    for texture in imported_textures():
        item = "imported_" + texture.replace(".", "_")
        family, purpose, acquisition = imported_role(texture)
        rows.append(row(f"powers:{item}", LANG.get(f"item.powers.{item}", item), family, purpose, acquisition))

    return """# Item purpose and acquisition catalogue

This generated table names every registered gameplay item family, including hidden save aliases. “Deferred” is intentional for crystals and story-gated objects; it does not mean an undocumented missing recipe. Ordinary imported items receive additive loot and never replace vanilla loot tables.

| Registry ID | Name | Family | Purpose | Survival acquisition/status |
|---|---|---|---|---|
""" + "\n".join(rows) + "\n"


def main() -> None:
    target = ROOT / "docs/gameplay/item-catalogue.md"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(render(), encoding="utf-8")


if __name__ == "__main__":
    main()
