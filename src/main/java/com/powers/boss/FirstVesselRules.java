package com.powers.boss;

/** Pure phase, scaling, channel, and performance limits for the boss. */
public final class FirstVesselRules {
	public static final int MAX_CANDIDATES = 24;
	public static final float BASE_HEALTH = 5_000.0F;

	private FirstVesselRules() {
	}

	public static FirstVesselPhase phase(double healthRatio) {
		if (healthRatio > 0.70) return FirstVesselPhase.AWAKENING;
		if (healthRatio > 0.35) return FirstVesselPhase.UNBOUND;
		return FirstVesselPhase.LAST_COVENANT;
	}

	/** 75% per extra nearby player, capped at four times base health. */
	public static double playerScale(int nearbyPlayers) {
		return Math.min(4.0, 1.0 + Math.max(0, nearbyPlayers - 1) * 0.75);
	}

	public static boolean shouldBeginReconstitution(double healthRatio, boolean alreadyUsed) {
		return healthRatio < 0.50 && !alreadyUsed;
	}

	/** Eight percent of maximum health breaks the 5-second ritual. */
	public static boolean channelInterrupted(float damageDuringChannel, float maximumHealth) {
		return damageDuringChannel >= maximumHealth * 0.08F;
	}

	public static boolean planningTick(int tickCount) {
		return Math.floorMod(tickCount, 10) == 0;
	}

	public static int castInterval(FirstVesselPhase phase) {
		return switch (phase) {
			case AWAKENING -> 40;
			case UNBOUND -> 28;
			case LAST_COVENANT -> 18;
		};
	}
}
