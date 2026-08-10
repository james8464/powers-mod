package com.powers.item;

/** Data-only mapping from imported rune artwork to useful recharge tiers. */
public final class RuneTierRules {
	private RuneTierRules() {
	}

	public static int energyFor(String texture) {
		if (texture.contains("bound_runestone_active_3")) return 600;
		if (texture.contains("bound_runestone_active_2")) return 350;
		if (texture.contains("bound_runestone_active_1")) return 200;
		if (texture.contains("inscribed_large")) return 600;
		if (texture.contains("inscribed_medium")) return 350;
		if (texture.contains("inscribed_small")) return 200;
		if (texture.contains("inscribed_tiny")) return 100;
		if (texture.endsWith("dark_large")) return 400;
		if (texture.endsWith("dark_medium") || texture.endsWith("frigid")) return 250;
		if (texture.endsWith("dark_small")) return 125;
		if (texture.endsWith("dark_tiny")) return 60;
		if (texture.contains("inert")) return 40;
		return 75;
	}

	public static int cooldownTicks(int energy) {
		return Math.clamp(60 + Math.max(0, energy) / 4, 60, 300);
	}
}
