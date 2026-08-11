package com.powers.loot;

import com.powers.ImportedItemRules;
import com.powers.ImportedPackItems;

import java.util.ArrayList;
import java.util.List;

/** Ensures every visible imported item has at least one non-command survival source. */
final class ImportedSurvivalCatalog {
	private ImportedSurvivalCatalog() {
	}

	static List<LootDropGroup> groups() {
		List<String> provisions = new ArrayList<>();
		List<String> grimoires = new ArrayList<>();
		List<String> runes = new ArrayList<>();
		List<String> relics = new ArrayList<>();
		for (String texture : ImportedPackItems.textureIds()) {
			if (ImportedItemRules.isLegacyAssetLayer(texture)) continue;
			String id = "imported_" + texture.replace('.', '_');
			if (texture.startsWith("food_")) provisions.add(id);
			else if (texture.startsWith("book_")) grimoires.add(id);
			else if (texture.contains("runestone")) runes.add(id);
			else relics.add(id);
		}
		return List.of(
				new LootDropGroup("minecraft:chests/village/village_plains_house",
						0.22F, 1, 1, provisions),
				new LootDropGroup("minecraft:chests/stronghold_corridor",
						0.12F, 1, 1, grimoires),
				new LootDropGroup("minecraft:chests/trial_chambers/reward_common",
						0.10F, 1, 1, runes),
				new LootDropGroup("minecraft:archaeology/desert_well",
						0.08F, 1, 1, relics));
	}
}
