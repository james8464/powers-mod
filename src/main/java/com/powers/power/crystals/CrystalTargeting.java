package com.powers.power.crystals;

/** Pure range and traveller-selection policy shared by crystal effects. */
public final class CrystalTargeting {
	private CrystalTargeting() {
	}

	public static boolean withinRadius(double distanceSquared, double radius) {
		return Double.isFinite(distanceSquared) && Double.isFinite(radius)
				&& radius >= 0.0 && distanceSquared <= radius * radius;
	}
}
