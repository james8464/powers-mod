package com.powers.forge;

import com.powers.item.ArtifactRoleCatalogue;

import java.util.Locale;

/** Maps documented runestone families to the four Crucible XP tiers. */
public final class CrucibleRuneRules {
	private CrucibleRuneRules() {
	}

	public static int xpFor(String itemPath) {
		if (itemPath == null) return 0;
		String id = itemPath.toLowerCase(Locale.ROOT);
		// Archaeology and spell-school reagents are deliberately below the rarest
		// inscribed runes, but now have a concrete progression use.
		if (id.contains("emperyeanjewel")) return 175;
		if (id.contains("malignember")) return 125;
		if (id.contains("sacred_dust")) return 100;
		if (id.contains("soul_dust") || id.contains("blood_dust")) return 50;
		if (id.contains("blood_salts")) return 45;
		if (id.contains("fossil")) return 35;
		if (id.contains("ammolite") || id.contains("blackpearl")
				|| id.contains("bloodstone") || id.contains("oddstone")) return 60;
		if (id.contains("runestone")) {
			if (id.contains("inscribed_large") || id.contains("active_3")) return 675;
			if (id.contains("large") || id.contains("active_2") || id.contains("inscribed_medium")) return 225;
			if (id.contains("tiny") || id.contains("small") || id.contains("frigid")
					|| id.contains("active_1") || id.contains("inscribed_small")) return 75;
			return 25;
		}
		return switch (ArtifactRoleCatalogue.role(id)) {
			case CELESTIAL_FOCUS -> 100;
			case DESTRUCTIVE_FOCUS -> 125;
			case ARCANE_ENERGY_DUST -> 50;
			case RITUAL_CONTAINER -> 30;
			case ARCHAEOLOGY -> 35;
			case ARCANE_CATALYST, LORE_FRAGMENT -> 25;
			default -> 0;
		};
	}
}
