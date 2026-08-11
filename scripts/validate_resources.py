#!/usr/bin/env python3
"""Strict, dependency-free resource audit for the POWERS release build."""

from __future__ import annotations

import argparse
import json
import re
import struct
import sys
from pathlib import Path

IDENTIFIER = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
CURRENT_FILE = ""
REQUIRED_UI_TEXTURES = {
    "textures/gui/energy_symbols.png": (27, 45),
    "textures/gui/power_slot.png": (30, 30),
    "textures/gui/power_slot_active.png": (30, 30),
    "textures/gui/teleport_panel.png": (256, 192),
    "textures/gui/locator_panel.png": (240, 224),
    "textures/gui/rank_maze/light_panel.png": (512, 256),
    "textures/gui/rank_maze/dark_panel.png": (512, 256),
    "textures/gui/advancements/backgrounds/radiant_path.png": (256, 256),
    "textures/gui/advancements/backgrounds/shadow_path.png": (256, 256),
    "textures/mob_effect/exhaustion.png": (18, 18),
    "textures/mob_effect/amethyst_poisoning.png": (18, 18),
    "textures/particle/mote.png": (16, 16),
    "textures/particle/shard.png": (16, 16),
    "textures/particle/glyph.png": (16, 16),
    "textures/particle/ribbon.png": (16, 16),
    "textures/particle/spark.png": (16, 16),
    "textures/particle/eclipse.png": (16, 16),
    "textures/particle/root.png": (16, 16),
    "textures/particle/fracture.png": (16, 16),
}
REQUIRED_MAGIC_SOUNDS = {
    "rune_hum", "crystal_resonate", "amethyst_fracture", "time_suspend",
    "time_release", "rift_open", "rift_close", "soul_tether", "light_chorus",
    "dark_whisper", "ward_impact", "rank_awaken", "interaction_clash",
}


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"{CURRENT_FILE}: duplicate JSON key {key!r}")
        result[key] = value
    return result


def read_json(path: Path):
    global CURRENT_FILE
    CURRENT_FILE = str(path)
    with path.open("r", encoding="utf-8") as source:
        return json.load(source, object_pairs_hook=unique_object)


def local_resource(root: Path, identifier: str, category: str, suffix: str) -> Path | None:
    if not identifier.startswith("powers:"):
        return None
    path = identifier.split(":", 1)[1]
    prefix = category + "/"
    if path.startswith(prefix):
        path = path[len(prefix):]
    return root / "assets" / "powers" / category / (path + suffix)


def walk_models(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key == "model" and isinstance(child, str):
                yield child
            yield from walk_models(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_models(child)


def walk_strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from walk_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_strings(child)


def namespaced_id(path: Path, base: Path) -> str:
    return "powers:" + path.relative_to(base).with_suffix("").as_posix()


def graph_cycles(graph: dict[str, set[str]], label: str) -> list[str]:
    errors: list[str] = []
    state: dict[str, int] = {}
    stack: list[str] = []
    reported: set[frozenset[str]] = set()

    def visit(node: str) -> None:
        state[node] = 1
        stack.append(node)
        for target in sorted(graph.get(node, ())):
            if target not in graph:
                continue
            if state.get(target, 0) == 0:
                visit(target)
            elif state.get(target) == 1:
                start = stack.index(target)
                cycle = stack[start:] + [target]
                identity = frozenset(cycle[:-1])
                if identity not in reported:
                    reported.add(identity)
                    errors.append(f"{label} cycle: {' -> '.join(cycle)}")
        stack.pop()
        state[node] = 2

    for node in sorted(graph):
        if state.get(node, 0) == 0:
            visit(node)
    return errors


def ingredient_tokens(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for child in value:
            yield from ingredient_tokens(child)
    elif isinstance(value, dict):
        item = value.get("item")
        tag = value.get("tag")
        if isinstance(item, str):
            yield item
        if isinstance(tag, str):
            yield "#" + tag.removeprefix("#")
        if item is None and tag is None:
            for child in value.values():
                yield from ingredient_tokens(child)


def tag_reference(category: str, identifier: str) -> str:
    namespace, path = identifier.removeprefix("#").split(":", 1)
    if path.startswith(category + "/"):
        return f"{namespace}:{path}"
    return f"{namespace}:{category}/{path}"


def validate_data_references(root: Path, parsed: dict[Path, object]) -> list[str]:
    errors: list[str] = []
    data = root / "data" / "powers"
    items = root / "assets" / "powers" / "items"
    blockstates = root / "assets" / "powers" / "blockstates"

    recipe_base = data / "recipe"
    recipe_paths = sorted(recipe_base.rglob("*.json"))
    recipe_ids = {path: namespaced_id(path, recipe_base) for path in recipe_paths}
    produced_by: dict[str, set[str]] = {}
    recipe_dependencies: dict[str, set[str]] = {identifier: set() for identifier in recipe_ids.values()}
    recipe_tokens: dict[Path, list[str]] = {}
    for path in recipe_paths:
        value = parsed.get(path, {})
        if not isinstance(value, dict):
            continue
        result = value.get("result", {})
        result_id = result.get("id") if isinstance(result, dict) else result
        if isinstance(result_id, str) and result_id.startswith("powers:"):
            produced_by.setdefault(result_id, set()).add(recipe_ids[path])
            item_path = items / (result_id.split(":", 1)[1] + ".json")
            if not item_path.is_file():
                errors.append(f"{path}: missing local item {result_id}")
        tokens: list[str] = []
        for key in ("ingredient", "ingredients"):
            tokens.extend(ingredient_tokens(value.get(key, [])))
        key_map = value.get("key", {})
        if isinstance(key_map, dict):
            for ingredient in key_map.values():
                tokens.extend(ingredient_tokens(ingredient))
        recipe_tokens[path] = tokens
        for token in tokens:
            if token.startswith("powers:"):
                item_path = items / (token.split(":", 1)[1] + ".json")
                if not item_path.is_file():
                    errors.append(f"{path}: missing local item {token}")
            elif token.startswith("#powers:"):
                reference = tag_reference("item", token)
                tag_path = data / "tags" / (reference.split(":", 1)[1] + ".json")
                if not tag_path.is_file():
                    errors.append(f"{path}: missing local tag {reference}")

    for path, tokens in recipe_tokens.items():
        dependencies = recipe_dependencies[recipe_ids[path]]
        for token in tokens:
            dependencies.update(produced_by.get(token, ()))
    errors.extend(graph_cycles(recipe_dependencies, "recipe"))

    loot_base = data / "loot_table"
    loot_paths = sorted(loot_base.rglob("*.json"))
    loot_ids = {path: namespaced_id(path, loot_base) for path in loot_paths}
    loot_by_id = {identifier: path for path, identifier in loot_ids.items()}
    loot_graph: dict[str, set[str]] = {identifier: set() for identifier in loot_ids.values()}

    def inspect_loot(path: Path, value) -> None:
        if isinstance(value, dict):
            entry_type = value.get("type")
            identifier = value.get("name", value.get("value"))
            if isinstance(identifier, str) and identifier.startswith("powers:"):
                if entry_type == "minecraft:item":
                    item_path = items / (identifier.split(":", 1)[1] + ".json")
                    if not item_path.is_file():
                        errors.append(f"{path}: missing local item {identifier}")
                elif entry_type == "minecraft:loot_table":
                    loot_graph[loot_ids[path]].add(identifier)
                    if identifier not in loot_by_id:
                        errors.append(f"{path}: missing local loot table {identifier}")
                elif entry_type == "minecraft:tag":
                    reference = tag_reference("item", identifier)
                    tag_path = data / "tags" / (reference.split(":", 1)[1] + ".json")
                    if not tag_path.is_file():
                        errors.append(f"{path}: missing local tag {reference}")
            for child in value.values():
                inspect_loot(path, child)
        elif isinstance(value, list):
            for child in value:
                inspect_loot(path, child)

    for path in loot_paths:
        inspect_loot(path, parsed.get(path, {}))
    errors.extend(graph_cycles(loot_graph, "loot table"))

    tags_base = data / "tags"
    tag_paths = sorted(tags_base.rglob("*.json"))
    tag_ids = {path: namespaced_id(path, tags_base) for path in tag_paths}
    tags_by_id = {identifier: path for path, identifier in tag_ids.items()}
    tag_graph: dict[str, set[str]] = {identifier: set() for identifier in tag_ids.values()}
    for path in tag_paths:
        relative = path.relative_to(tags_base)
        category = relative.parts[0]
        value = parsed.get(path, {})
        values = value.get("values", []) if isinstance(value, dict) else []
        for entry in values if isinstance(values, list) else []:
            required = True
            if isinstance(entry, dict):
                required = entry.get("required", True) is not False
                entry = entry.get("id")
            if not isinstance(entry, str) or not entry.startswith(("powers:", "#powers:")):
                continue
            if entry.startswith("#"):
                reference = tag_reference(category, entry)
                tag_graph[tag_ids[path]].add(reference)
                if required and reference not in tags_by_id:
                    errors.append(f"{path}: missing local tag {reference}")
            elif required and category == "item":
                item_path = items / (entry.split(":", 1)[1] + ".json")
                if not item_path.is_file():
                    errors.append(f"{path}: missing local item {entry}")
            elif required and category == "block":
                block_path = blockstates / (entry.split(":", 1)[1] + ".json")
                if not block_path.is_file():
                    errors.append(f"{path}: missing local block {entry}")
            elif required and category == "damage_type":
                damage_path = data / "damage_type" / (entry.split(":", 1)[1] + ".json")
                if not damage_path.is_file():
                    errors.append(f"{path}: missing local damage type {entry}")
    errors.extend(graph_cycles(tag_graph, "tag"))
    return errors


def validate(root: Path) -> list[str]:
    errors: list[str] = []
    parsed = {}
    png_dimensions = {}
    png_color_types = {}
    for path in sorted(root.rglob("*")):
        if path.name in {".DS_Store", "Thumbs.db"}:
            errors.append(f"forbidden metadata file: {path}")
        if path.suffix == ".json" or path.name.endswith(".mcmeta"):
            try:
                parsed[path] = read_json(path)
            except Exception as error:
                errors.append(str(error))
        if path.suffix == ".png":
            try:
                data = path.read_bytes()
                if data[:8] != PNG_SIGNATURE or len(data) < 24:
                    raise ValueError("invalid PNG signature/header")
                width, height = struct.unpack(">II", data[16:24])
                if width <= 0 or height <= 0 or width > 16_384 or height > 16_384:
                    raise ValueError(f"invalid dimensions {width}x{height}")
                png_dimensions[path] = (width, height)
                png_color_types[path] = data[25]
            except Exception as error:
                errors.append(f"{path}: {error}")

    assets = root / "assets" / "powers"
    lang_path = assets / "lang" / "en_us.json"
    lang = parsed.get(lang_path, {})
    errors.extend(validate_data_references(root, parsed))

    for relative, expected_dimensions in REQUIRED_UI_TEXTURES.items():
        path = assets / relative
        actual_dimensions = png_dimensions.get(path)
        if actual_dimensions != expected_dimensions:
            errors.append(f"{path}: expected {expected_dimensions[0]}x{expected_dimensions[1]} RGBA PNG, "
                          f"found {actual_dimensions!r}")
        if path in png_color_types and png_color_types[path] not in {4, 6}:
            errors.append(f"{path}: custom GUI/effect art must retain an alpha channel")
    for effect in ("exhaustion", "amethyst_poisoning"):
        if f"effect.powers.{effect}" not in lang:
            errors.append(f"missing en_us effect translation: effect.powers.{effect}")

    sounds_path = assets / "sounds.json"
    sounds = parsed.get(sounds_path, {})
    if not isinstance(sounds, dict):
        errors.append(f"{sounds_path}: expected a JSON object")
        sounds = {}
    missing_sounds = sorted(REQUIRED_MAGIC_SOUNDS - set(sounds))
    if missing_sounds:
        errors.append(f"{sounds_path}: missing semantic sounds {missing_sounds}")
    for sound_id in sorted(REQUIRED_MAGIC_SOUNDS):
        ogg = assets / "sounds" / "magic" / f"{sound_id}.ogg"
        try:
            data = ogg.read_bytes()
            marker = data.find(b"\x01vorbis")
            if data[:4] != b"OggS" or marker < 0:
                raise ValueError("expected Ogg/Vorbis audio")
            if marker + 11 >= len(data) or data[marker + 11] != 1:
                raise ValueError("magic sound must be mono")
        except Exception as error:
            errors.append(f"{ogg}: {error}")

    particles = assets / "particles"
    for particle in ("mote", "shard", "glyph", "ribbon", "spark", "eclipse", "root", "fracture"):
        definition = parsed.get(particles / f"{particle}.json", {})
        expected = f"powers:{particle}"
        textures = definition.get("textures", []) if isinstance(definition, dict) else []
        if expected not in textures:
            errors.append(f"{particles / (particle + '.json')}: missing texture {expected}")
    mod_metadata = parsed.get(root / "fabric.mod.json", {})
    contact = mod_metadata.get("contact", {}) if isinstance(mod_metadata, dict) else {}
    for field in ("homepage", "sources", "issues"):
        if not isinstance(contact, dict) or not contact.get(field):
            errors.append(f"fabric.mod.json: missing contact.{field}")

    for path in sorted((assets / "items").glob("*.json")):
        data = parsed.get(path)
        if not isinstance(data, dict):
            continue
        for model in walk_models(data):
            if not IDENTIFIER.fullmatch(model):
                errors.append(f"{path}: invalid model identifier {model!r}")
                continue
            target = local_resource(root, model, "models", ".json")
            if target is not None and not target.exists():
                errors.append(f"{path}: missing model {target}")
        stem = path.stem
        if f"item.powers.{stem}" not in lang and f"block.powers.{stem}" not in lang:
            errors.append(f"{path}: missing en_us item/block translation")

    for path in sorted((assets / "blockstates").glob("*.json")):
        data = parsed.get(path)
        for model in walk_models(data):
            if not IDENTIFIER.fullmatch(model):
                errors.append(f"{path}: invalid block model identifier {model!r}")
                continue
            target = local_resource(root, model, "models", ".json")
            if target is not None and not target.exists():
                errors.append(f"{path}: missing block model {target}")
        if f"block.powers.{path.stem}" not in lang:
            errors.append(f"{path}: missing en_us block translation")

    for path in sorted((assets / "models").rglob("*.json")):
        data = parsed.get(path)
        if not isinstance(data, dict):
            continue
        parent = data.get("parent")
        if isinstance(parent, str):
            if not IDENTIFIER.fullmatch(parent):
                errors.append(f"{path}: invalid parent identifier {parent!r}")
            target = local_resource(root, parent, "models", ".json")
            if target is not None and not target.exists():
                errors.append(f"{path}: missing parent model {target}")
        textures = data.get("textures", {})
        if isinstance(textures, dict):
            for texture in textures.values():
                if not isinstance(texture, str) or texture.startswith("#"):
                    continue
                if not IDENTIFIER.fullmatch(texture):
                    errors.append(f"{path}: invalid texture identifier {texture!r}")
                    continue
                target = local_resource(root, texture, "textures", ".png")
                if target is not None and not target.exists():
                    errors.append(f"{path}: missing texture {target}")
                if (path.is_relative_to(assets / "models" / "block")
                        and texture.startswith("powers:")
                        and not texture.removeprefix("powers:").startswith("block/")):
                    errors.append(f"{path}: block texture {texture!r} is outside the stitched block atlas")

    for path in sorted((root / "data" / "powers" / "advancement").rglob("*.json")):
        data = parsed.get(path, {})
        background = data.get("display", {}).get("background") if isinstance(data, dict) else None
        if isinstance(background, str) and background.startswith("powers:"):
            target = local_resource(root, background, "textures", ".png")
            if target is not None and not target.exists():
                errors.append(f"{path}: missing advancement background {target}")

    for path in sorted(assets.rglob("*.png.mcmeta")):
        texture = path.with_suffix("")
        dimensions = png_dimensions.get(texture)
        data = parsed.get(path, {})
        animation = data.get("animation", {}) if isinstance(data, dict) else {}
        if not dimensions or not isinstance(animation, dict):
            continue
        frame_time = animation.get("frametime", 1)
        if not isinstance(frame_time, int) or frame_time < 1:
            errors.append(f"{path}: animation frametime must be a positive integer")
        width, height = dimensions
        frames = animation.get("frames")
        if height > width and height % width == 0 and isinstance(frames, list):
            frame_count = height // width
            referenced = {
                frame.get("index") if isinstance(frame, dict) else frame
                for frame in frames
            }
            unused = sorted(set(range(frame_count)) - {index for index in referenced if isinstance(index, int)})
            if unused:
                errors.append(f"{path}: unused animation frames {unused}")

    minecraft_loot = root / "data" / "minecraft" / "loot_table"
    if minecraft_loot.exists() and any(minecraft_loot.rglob("*.json")):
        errors.append("vanilla loot-table overrides are forbidden; use additive LootTableEvents")

    dimensions = root / "data" / "powers" / "dimension"
    biomes = root / "data" / "powers" / "worldgen" / "biome"
    for path in sorted(dimensions.glob("*.json")):
        data = parsed.get(path, {})
        generator = data.get("generator", {}) if isinstance(data, dict) else {}
        settings = generator.get("settings", {}) if isinstance(generator, dict) else {}
        biome_source = generator.get("biome_source", {}) if isinstance(generator, dict) else {}
        biome = (settings.get("biome") if isinstance(settings, dict) else None) or (
            biome_source.get("biome") if isinstance(biome_source, dict) else None
        )
        expected = f"powers:{path.stem}"
        if biome != expected:
            errors.append(f"{path}: expected a distinct {expected} biome, found {biome!r}")
        if not (biomes / f"{path.stem}.json").exists():
            errors.append(f"{path}: missing worldgen biome {biomes / (path.stem + '.json')}")

    for path in sorted((root / "data" / "powers" / "recipe").glob("*.json")):
        data = parsed.get(path, {})
        result = data.get("result", {}) if isinstance(data, dict) else {}
        result_id = result.get("id", "") if isinstance(result, dict) else str(result)
        if result_id.startswith("powers:") and "crystal" in result_id:
            errors.append(f"{path}: crystal recipes are intentionally unreleased")

    if "item.powers.crystal_frostcythe" in lang:
        errors.append("stale typo translation item.powers.crystal_frostcythe")
    for stale_key in (
        "power.powers.feral_roar",
        "power.powers.feral_roar.description",
        "power.powers.dimensional_anchor",
        "power.powers.dimensional_anchor.description",
    ):
        if stale_key in lang:
            errors.append(f"stale translation {stale_key}")
    if any(key.startswith("grimoire.celestial.low_xp.") for key in lang):
        errors.append("stale XP-priced celestial grimoire messages")
    stale_quartz = "minecraft:block/quartz_pillar"
    for path, data in parsed.items():
        if path.suffix == ".json" and stale_quartz in walk_strings(data):
            errors.append(f"{path}: stale vanilla texture {stale_quartz}; use quartz_pillar_side")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True, type=Path)
    args = parser.parse_args()
    errors = validate(args.root.resolve())
    if errors:
        print("Resource validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("POWERS resource validation passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
