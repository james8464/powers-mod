package com.powers.spell;

/** Deterministic, idempotent migration from the released 21-spell page order. */
public final class SpellSelectionMigration {
	private SpellSelectionMigration() {
	}

	public static int canonicalIndex(String grimoireKey, int savedIndex) {
		if (grimoireKey == null || savedIndex < 0) return 0;
		return switch (grimoireKey) {
			case "book_grimoire_celestial" -> savedIndex <= 3 ? savedIndex : 0;
			case "book_grimoire_deep", "book_grimoire_infernal" -> 0;
			case "book_grimoire_blight" -> savedIndex <= 1 ? savedIndex : 0;
			case "book_grimoire_wild" -> savedIndex <= 2 ? savedIndex : 0;
			case "book_grimoire_abyssal" -> savedIndex == 0 ? 0
					: savedIndex <= 3 ? 1 : 0;
			default -> 0;
		};
	}
}
