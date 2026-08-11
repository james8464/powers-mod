package com.powers.item;

/** Human-readable family purpose and survival source for every visible import. */
public record ImportedItemPurpose(String role, String acquisition) {
	public static ImportedItemPurpose describe(String texture) {
		if (texture == null || texture.isBlank()) return new ImportedItemPurpose(
				"Invalid import", "Not obtainable");
		if (texture.startsWith("food_")) return new ImportedItemPurpose(
				"Edible provisions with cooked/smoked variants restoring more hunger",
				"Creature, foliage, crop, fishing, or village provision loot");
		if (texture.startsWith("book_grimoire")) return new ImportedItemPurpose(
				"Grimoire that selects and channels its documented spell school",
				"Stronghold corridor and library discoveries");
		if (texture.contains("runestone")) return new ImportedItemPurpose(
				"Finite tiered magical-energy restoration or Crucible infusion",
				"Dungeon, ancient-site, or trial-chamber reward loot");
		ImportedArtifactKind kind = ImportedArtifactRules.kind(texture);
		String role = switch (kind) {
			case ATTUNEMENT -> "Ring or amulet attunement that passively restores energy and resistance";
			case SOUL_VESSEL -> "Bounded soul drain, with Soul Matrix passive energy storage";
			case RITUAL_CATALYST -> "Health-paid catalyst that amplifies the next grimoire ritual";
			case HEART_RELIC -> "Vitality, nature, or necromantic healing relic";
			case TRANSMUTER -> "Controlled Philosopher's Stone block transmutation";
			case TRAVEL_RELIC -> "Lodestone binding or two-charge same-dimension Miniportal travel";
			case COMMAND_RELIC -> "Flute that recalls, heals, and aligns nearby guardian creatures";
			case ARCANE_CATALYST -> "Arcane Crucible reagent for a named magical school";
			case LORE_RELIC -> "Archaeological memory fragment that Shadow can interpret";
			case NONE -> "Visible material, provision, or story curio";
		};
		return new ImportedItemPurpose(role,
				"Thematic archaeology, structure, boss, or settlement loot");
	}
}
