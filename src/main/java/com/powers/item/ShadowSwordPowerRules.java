package com.powers.item;

/** Pure damage caps and progression gates for the sword's unique powers. */
public final class ShadowSwordPowerRules {
	public static final double SINGULARITY_RADIUS = 48.0;
	public static final double BEAM_RANGE = 128.0;

	private ShadowSwordPowerRules() {
	}

	public static float singularityDamage(float maximumHealth) {
		return Math.min(300.0F, 80.0F + Math.max(0.0F, maximumHealth) * 0.10F);
	}

	public static float annihilationDamage(float maximumHealth) {
		return Math.min(500.0F, 140.0F + Math.max(0.0F, maximumHealth) * 0.25F);
	}

	public static int requiredRank(String abilityId) {
		return switch (abilityId) {
			case "abyssal_singularity" -> 3;
			case "annihilation_beam" -> 7;
			case "nightfall_dominion" -> 10;
			default -> 1;
		};
	}
}
