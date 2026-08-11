package com.powers.power.crystals;

/** Fixed, rank-independent Yellow Crystal body scales. */
public final class CrystalSizeShiftRules {
	public static final double SMALL_SCALE = 0.0625;
	public static final double GIANT_SCALE = 10.0;

	private CrystalSizeShiftRules() {
	}

	/** SCALE uses an additive-total modifier around the vanilla base value of one. */
	public static double modifierFor(double scale) {
		return Math.clamp(scale, SMALL_SCALE, GIANT_SCALE) - 1.0;
	}
}
