package com.powers.power.abilities;

/** Cost contract for taking ownership of the entire server clock. */
public final class TimeFreezeDrainRules {
	private static final double CAPACITY_FRACTION_PER_SECOND = 0.15;
	private static final int MINIMUM_PER_SECOND = 40;
	private static final double LOW_TPS_WARNING_MSPT = 50.0;

	private TimeFreezeDrainRules() {
	}

	/** Authoritative pre-activation drain, safe duration, and advisory load state. */
	public record Forecast(int energyPerSecond, int safeSeconds,
			boolean lowTpsWarning) {
	}

	public static int energyPerSecond(int capacity) {
		return Math.max(MINIMUM_PER_SECOND,
				(int) Math.ceil(Math.max(0, capacity) * CAPACITY_FRACTION_PER_SECOND));
	}

	/** Forecasts full payable drain intervals; MSPT can warn but never refuses. */
	public static Forecast forecast(int energy, int capacity, double mspt) {
		int drain = energyPerSecond(capacity);
		int boundedCapacity = Math.max(0, capacity);
		int boundedEnergy = Math.clamp(energy, 0, boundedCapacity);
		return new Forecast(drain, boundedEnergy / drain,
				Double.isFinite(mspt) && mspt > LOW_TPS_WARNING_MSPT);
	}
}
