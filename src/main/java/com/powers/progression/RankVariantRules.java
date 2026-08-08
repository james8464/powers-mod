package com.powers.progression;

/** Pure bounded consequences shared by named rank-variant runtime effects. */
public final class RankVariantRules {
	private RankVariantRules() {
	}

	/** True Sight overrides only a failed realm-veil rank/path gate. */
	public static boolean mayPierceRealmVeil(boolean ordinaryAccess, boolean trueSight) {
		return ordinaryAccess || trueSight;
	}

	/** Returns whether Dark Resurgence is in its inclusive quarter-energy emergency band. */
	public static boolean darknessEmergency(int energy, int capacity) {
		return capacity > 0 && (long) Math.max(0, energy) * 4L <= capacity;
	}

	/** Applies the 1.5x affinity bonus, or 2x while the player is in the emergency band. */
	public static int darknessRefill(int rankedBase, int energy, int capacity, boolean darkResurgence) {
		if (rankedBase <= 0) return 0;
		if (!darkResurgence) return rankedBase;
		long scaled = darknessEmergency(energy, capacity)
				? (long) rankedBase * 2L
				: ((long) rankedBase * 3L + 1L) / 2L;
		return (int) Math.min(Integer.MAX_VALUE, scaled);
	}
}
