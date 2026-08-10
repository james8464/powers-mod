package com.powers.power.crystals;

/** Pure two-state policy for the useful Space-Time crystal modes. */
public final class SpaceTimeModeRules {
	public enum Mode { ACCELERATE, FREEZE }

	private SpaceTimeModeRules() {
	}

	public static int count() {
		return Mode.values().length;
	}

	public static Mode mode(int index) {
		return Mode.values()[Math.floorMod(index, count())];
	}

	public static int next(int index) {
		return Math.floorMod(index + 1, count());
	}
}
