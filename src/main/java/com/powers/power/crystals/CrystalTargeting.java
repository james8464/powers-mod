package com.powers.power.crystals;

/** Pure range policy shared by crystal area effects. */
public final class CrystalTargeting {
	private CrystalTargeting() {
	}

	public static boolean withinRadius(double distanceSquared, double radius) {
		return Double.isFinite(distanceSquared) && Double.isFinite(radius)
				&& radius >= 0.0 && distanceSquared <= radius * radius;
	}
}
