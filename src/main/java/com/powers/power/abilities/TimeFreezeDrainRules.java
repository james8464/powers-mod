package com.powers.power.abilities;

/** Cost contract for taking ownership of the entire server clock. */
public final class TimeFreezeDrainRules {
	private static final double CAPACITY_FRACTION_PER_SECOND = 0.15;
	private static final int MINIMUM_PER_SECOND = 40;

	private TimeFreezeDrainRules() {
	}

	public static int energyPerSecond(int capacity) {
		return Math.max(MINIMUM_PER_SECOND,
				(int) Math.ceil(Math.max(0, capacity) * CAPACITY_FRACTION_PER_SECOND));
	}
}
