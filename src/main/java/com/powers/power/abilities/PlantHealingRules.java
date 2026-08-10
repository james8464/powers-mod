package com.powers.power.abilities;

/** Pure boundary rules for the crouch healing aura. */
public final class PlantHealingRules {
	private static final double RADIUS_SQUARED = 4.0;

	private PlantHealingRules() {
	}

	public static boolean withinAura(double distanceSquared) {
		return Double.isFinite(distanceSquared)
				&& distanceSquared >= 0.0 && distanceSquared <= RADIUS_SQUARED;
	}
}
