package com.powers.power.abilities;

/** Pure forward-cone geometry for the boss-scale Thunderclap shockwave. */
public final class ThunderclapRules {
	private static final double MINIMUM_DOT = Math.cos(Math.toRadians(70.0));

	private ThunderclapRules() {
	}

	public static boolean inCone(double offsetX, double offsetZ,
			double lookX, double lookZ, double range) {
		double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
		if (distanceSquared > range * range) return false;
		if (distanceSquared <= 1.0E-8) return true;
		double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
		if (lookLength <= 1.0E-8) return false;
		double dot = (offsetX * lookX + offsetZ * lookZ)
				/ (Math.sqrt(distanceSquared) * lookLength);
		return dot >= MINIMUM_DOT;
	}
}
