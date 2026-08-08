package com.powers.power;

/** Small deterministic calculations shared by abilities and their regression tests. */
public final class AbilityArithmetic {
	private AbilityArithmetic() {
	}

	public static int pulseCount(int durationTicks, int intervalTicks) {
		if (durationTicks <= 0 || intervalTicks <= 0) return 0;
		return (durationTicks + intervalTicks - 1) / intervalTicks;
	}

	public static int afterPulse(int remainingTicks, int intervalTicks) {
		return Math.max(0, remainingTicks - Math.max(1, intervalTicks));
	}

	public static int drainStep(int remainingEnergy, int ticksRemaining) {
		if (remainingEnergy <= 0 || ticksRemaining <= 0) return 0;
		return (remainingEnergy + ticksRemaining - 1) / ticksRemaining;
	}

	public static double[] endpoint(double x, double y, double z,
			double dx, double dy, double dz, double range) {
		return new double[] {x + dx * range, y + dy * range, z + dz * range};
	}

	public static int nextMode(int current, int modeCount) {
		if (modeCount <= 0) throw new IllegalArgumentException("modeCount must be positive");
		return Math.floorMod(current + 1, modeCount);
	}

	public static boolean costsEnergy(boolean selectionAction) {
		return !selectionAction;
	}
}
