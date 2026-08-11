package com.powers.entity;

/** Pure tactical state selection for player-shaped aligned guardians. */
public final class GuardianTactics {
	public static final int NAVIGATION_INTERVAL = 10;
	public static final int COVER_SEARCH_INTERVAL = 20;
	public static final int MAX_COVER_CANDIDATES = 125;

	public enum Stance {
		IDLE,
		ADVANCE,
		MELEE,
		RANGED,
		SEEK_COVER,
		RETREAT
	}

	private GuardianTactics() {
	}

	/** Chooses one readable stance from bounded authoritative combat facts. */
	public static Stance choose(double distance, double healthFraction,
			boolean lineOfSight, boolean targetAlive) {
		if (!targetAlive) return Stance.IDLE;
		double safeDistance = Double.isFinite(distance) ? Math.max(0.0, distance) : 12.0;
		double safeHealth = Double.isFinite(healthFraction) ? Math.clamp(healthFraction, 0.0, 1.0) : 1.0;
		if (safeHealth <= 0.25) return Stance.RETREAT;
		if (safeHealth <= 0.5 && lineOfSight) return Stance.SEEK_COVER;
		if (!lineOfSight || safeDistance > 18.0) return Stance.ADVANCE;
		if (safeDistance <= 4.0) return Stance.MELEE;
		return Stance.RANGED;
	}
}
