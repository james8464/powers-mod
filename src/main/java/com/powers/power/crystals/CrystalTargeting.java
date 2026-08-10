package com.powers.power.crystals;

/** Pure range and traveller-selection policy shared by crystal effects. */
public final class CrystalTargeting {
	public enum JourneyTarget {
		CASTER,
		AIMED_PLAYER
	}

	private CrystalTargeting() {
	}

	public static boolean withinRadius(double distanceSquared, double radius) {
		return Double.isFinite(distanceSquared) && Double.isFinite(radius)
				&& radius >= 0.0 && distanceSquared <= radius * radius;
	}

	/** Empty aim falls back to the caster so a mindscape crystal always opens a journey. */
	public static JourneyTarget journeyTarget(boolean crouching, boolean hasPlayerTarget) {
		return !crouching && hasPlayerTarget ? JourneyTarget.AIMED_PLAYER : JourneyTarget.CASTER;
	}
}
