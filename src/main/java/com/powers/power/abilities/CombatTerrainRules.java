package com.powers.power.abilities;

/** Pure rank-to-work budgets for persistent offensive terrain scars. */
public final class CombatTerrainRules {
	private CombatTerrainRules() {
	}

	public static int craterBudget(int rank) {
		return 16 + clampRank(rank) * 8;
	}

	public static int thunderclapBudget(int rank) {
		return 8 + clampRank(rank) * 4;
	}

	public static int rayBudget(int rank) {
		return 1 + (clampRank(rank) * 7 + 9) / 10;
	}

	public static double craterRadius(int rank) {
		return 1.5 + clampRank(rank) * 0.45;
	}

	public static float maximumHardness(int rank) {
		return 2.5F + clampRank(rank) * 0.75F;
	}

	/** Applies a server minimum without allowing either source to escape the authored ladder. */
	public static int effectiveTier(int authoredTier, int configuredMinimum) {
		return Math.max(clampRank(authoredTier), clampRank(configuredMinimum));
	}

	/** Enforces the administrator's hard per-cast mutation ceiling. */
	public static int cappedBudget(int requested, int configuredMaximum) {
		return Math.min(Math.max(0, requested), Math.max(1, configuredMaximum));
	}

	private static int clampRank(int rank) {
		return Math.clamp(rank, 0, 10);
	}
}
