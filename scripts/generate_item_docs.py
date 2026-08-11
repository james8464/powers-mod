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
    if (texture == "device_miniportal_active" or texture == "artifact_runestone_back"
            or "runestone_overlay_" in texture or texture in {
                "book_grimoire_infernal", "book_grimoire_recolor_overlay_infernal",
                "artifact_trilobitefossil"}):
        return "Compatibility asset alias", "Hidden; retained only for old saves and model composition", "Deferred/hidden"
    if texture == "food_wisdomfruit":
        return ("Provision", "Edible food; cooked and smoked forms restore more hunger",
                "3.5% additive drop from Archivist realm-memory caches")
    if texture.startswith("food_"):
        return "Provision", "Edible food; cooked and smoked forms restore more hunger", "Themed mob/block loot and village provision salvage; processed variants may also be cooked"
    if texture.startswith("book_grimoire"):
        return "Grimoire", "Selects and channels the documented spells of one school", "Stronghold library/corridor discovery"
    if texture.startswith("book_"):
        return "Lore", "Archaeological grimoire fragment interpreted by Shadow", "Stronghold discovery"
    if "runestone" in texture:
        return "Runestone", "Finite tiered energy restoration and Arcane Crucible infusion", "Crafting upgrade chain plus dungeon, ancient-city, and trial rewards"
    if "ring" in texture or "amulet" in texture:
        rates = {
            "artifact_corroded_copper_ring": 1, "artifact_plain_copper_ring": 1,
            "artifact_emerald_ring": 2, "artifact_amulet": 2,
            "artifact_diamond_ring": 3,
        }
        return "Attunement", f"Restores {rates.get(texture, 1)} energy per second; carried attunements jointly cap at 6 and grant bounded resistance", "Thematic archaeology and structure salvage"
    if "soulstone" in texture or "soulmatrix" in texture:
        capacities = {"small": 200, "medium": 400, "large": 800}
        capacity = 1600 if "soulmatrix" in texture else next(
            (value for size, value in capacities.items() if size in texture), 200)
        initial = "starts empty" if "inert" in texture else "starts charged"
        return "Energy reservoir", f"Stores {capacity} magic energy ({initial}); sneak-use stores and use releases up to 100, and casts atomically draw shortfalls", "Nether-fortress and archaeology salvage"
    if "heart" in texture:
        purpose = {
            "artifact_beating_heart": "Active healing plus passive regeneration",
            "artifact_woodheart": "Strong active healing plus passive regeneration",
            "artifact_ghoul_heart": "Trades weaker healing for active and passive energy restoration",
            "artifact_heart_mechanism": "Raises a timed clockwork absorption ward",
        }.get(texture, "Specialised vitality restoration")
        return "Heart relic", purpose, "Settlement and archaeology salvage"
    if "bloodstone" in texture:
        return "Death ward", "Arms one five-minute lethal-damage ward; the ward is consumed to prevent a legal death", "Thematic archaeology and structure salvage"
    if "ritualdagger" in texture:
        return "Energy catalyst", "Directly sacrifices 4 health to restore 80 magic energy", "Woodland-mansion and archaeology salvage"
    if "philosopherstone" in texture:
        return "Transmuter", "Controlled stone/deepslate/netherrack/end-stone transmutation", "Rare woodland-mansion and archaeology salvage"
    if "lodestone" in texture:
        return "Travel relic", "Binds a safe same-dimension destination for a Miniportal", "Desert-pyramid and archaeology salvage"
    if texture == "device_miniportal":
        return "Travel relic", "Two safe same-dimension trips; dropped amethyst restores both charges", "Guaranteed First Vessel drop and rare ruined-portal salvage"
    if "flute" in texture:
        return "Command relic", "Recalls, heals, and aligns nearby player-shaped guardians", "Jungle-temple and archaeology salvage"
    if "emperyeanjewel" in texture:
        return "Consent seal", "Overrides every player-consent gate for one 40-energy surcharge; safe zones and server policy still win", "Rare stronghold and archaeology salvage"
    if "malignember" in texture:
        return "Destructive focus", "Reduces explicit destructive magic activation costs by 20%, never below 1", "Nether-fortress and archaeology salvage"
    if texture in {"artifact_star", "artifact_star_animated", "artifact_ammolite"}:
        return "Celestial focus", "Passively restores 1 energy per second and provides bounded Arcane Crucible infusion", "Thematic archaeology and structure salvage"
    if "bowl" in texture or "smallpot" in texture or "dripping_orb" in texture:
        return "Ritual container", "Provides a bounded Arcane Crucible infusion catalyst", "Thematic archaeology and structure salvage"
    if "fossil" in texture:
        return "Archaeology", "Provides a bounded archaeology-themed Arcane Crucible infusion", "Thematic archaeology and structure salvage"
    if texture.startswith("magic_essence_") or texture.startswith("blood_salts"):
        return "Arcane energy dust", "A magic-energy-themed Arcane Crucible infusion reagent; it is not a second resource", "Buried-treasure, fortress, and archaeology salvage"
    return "Relic or lore catalyst", "Arcane Crucible reagent or memory fragment interpreted by Shadow", "Thematic archaeology and structure salvage"


def row(item: str, name: str, family: str, purpose: str, acquisition: str) -> str:
    return f"| `{item}` | {name} | {family} | {purpose} | {acquisition} |"


def render() -> str:
    rows: list[str] = []
    crystals = ["rainbow_crystal", "red_crystal", "orange_crystal", "yellow_crystal",
                "green_crystal", "blue_crystal", "indigo_crystal", "violet_crystal",
                "light_crystal", "dark_crystal"]
    for item in crystals:
        acquisition = ("No crafting recipe; operator testing or Shadow's full-energy "
                       "60-second manifestation rite" if item == "dark_crystal" else
                       "Story acquisition intentionally deferred; no crafting recipe; operator testing only")
        rows.append(row(f"powers:{item}", LANG.get(f"item.powers.{item}", item), "Crystal",
                        "Selects the crystal powers documented in README",
                        acquisition))
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
