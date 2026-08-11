package com.powers.spell;

/** Pure targeting and integrity limits for Hearth Sanctuary. */
public final class HearthSanctuaryRules {
	public static final double RADIUS = 3.0;
	public static final int MAX_TARGETS = 32;
	public static final float INTEGRITY = 40.0F;

	private HearthSanctuaryRules() {
	}

	public static boolean withinRadius(double distanceSquared) {
		return Double.isFinite(distanceSquared) && distanceSquared >= 0.0
				&& distanceSquared <= RADIUS * RADIUS;
	}
}
