package com.powers.forge;

import java.util.Locale;

/** Maps documented runestone families to the four Crucible XP tiers. */
public final class CrucibleRuneRules {
	private CrucibleRuneRules() {
	}

	public static int xpFor(String itemPath) {
		if (itemPath == null) return 0;
		String id = itemPath.toLowerCase(Locale.ROOT);
		if (!id.contains("runestone")) return 0;
		if (id.contains("inscribed_large") || id.contains("active_3")) return 675;
		if (id.contains("large") || id.contains("active_2") || id.contains("inscribed_medium")) return 225;
		if (id.contains("tiny") || id.contains("small") || id.contains("frigid")
				|| id.contains("active_1") || id.contains("inscribed_small")) return 75;
		return 25;
	}
}
