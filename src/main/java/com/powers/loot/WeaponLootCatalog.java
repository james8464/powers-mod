package com.powers.loot;

import com.powers.item.FantasyWeaponArchetype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Distributes every ordinary fantasy weapon across low-chance survival discoveries. */
final class WeaponLootCatalog {
	private WeaponLootCatalog() {
	}

	static List<LootDropGroup> groups(List<String> weaponIds) {
		Map<String, List<String>> byTable = new LinkedHashMap<>();
		for (String id : weaponIds) {
			String table = table(FantasyWeaponArchetype.from(id));
			byTable.computeIfAbsent(table, ignored -> new ArrayList<>()).add("powers:" + id);
		}
		return byTable.entrySet().stream().map(entry -> new LootDropGroup(
				entry.getKey(), 0.025F, 1, 1, entry.getValue())).toList();
	}

	private static String table(FantasyWeaponArchetype archetype) {
		return switch (archetype) {
			case FROST -> "minecraft:chests/shipwreck_treasure";
			case SWIFT -> "minecraft:chests/igloo_chest";
			case REAPER, ABYSSAL -> "minecraft:entities/wither_skeleton";
			case CRUSHER, GUARDIAN -> "minecraft:chests/village/village_weaponsmith";
			case BERSERKER -> "minecraft:chests/pillager_outpost";
			case ARCANE -> "minecraft:entities/evoker";
			case VITAL -> "minecraft:entities/witch";
			case RADIANT -> "minecraft:chests/underwater_ruin_big";
			case HUNTER -> "minecraft:entities/vindicator";
			case PIERCER -> "minecraft:chests/village/village_toolsmith";
		};
	}
}
