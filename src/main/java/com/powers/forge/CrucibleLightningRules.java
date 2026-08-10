package com.powers.forge;

/** Item-power damage and energy rules; ranks deliberately never enter this calculation. */
public final class CrucibleLightningRules {
	private CrucibleLightningRules() {
	}

	public static int energyCost(int level) {
		int bounded = Math.clamp(level, 0, CrucibleXpRules.MAX_LEVEL);
		return 12 + (bounded + 3) / 4;
	}

	public static float damage(int level, boolean opposedFaction, boolean playerTarget) {
		int bounded = Math.clamp(level, 0, CrucibleXpRules.MAX_LEVEL);
		float raw = 18.0F + 5.0F * bounded + (bounded * bounded) / 3
				+ (opposedFaction ? 12.0F : 0.0F);
		return capDamage(raw, playerTarget);
	}

	public static float capDamage(float amount, boolean playerTarget) {
		if (!Float.isFinite(amount) || amount <= 0.0F) return 0.0F;
		return Math.min(amount, playerTarget ? 120.0F : 1_200.0F);
	}

	public static int cooldownTicks() {
		return 0;
	}
}
