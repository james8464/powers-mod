package com.powers.power.crystals;

/** Normalizes persisted convergence selections with bounded wraparound. */
public final class CrystalModeState {
	private CrystalModeState() {
	}

	public static int current(int stored, int modeCount) {
		if (modeCount < 1) throw new IllegalArgumentException("A crystal needs at least one mode");
		return Math.floorMod(stored, modeCount);
	}

	public static int advance(int stored, int modeCount) {
		return com.powers.power.AbilityArithmetic.nextMode(current(stored, modeCount), modeCount);
	}
}
