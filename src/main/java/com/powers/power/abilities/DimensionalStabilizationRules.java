package com.powers.power.abilities;

/** Pure fail-closed policy for extending already-existing local dimensional state. */
public final class DimensionalStabilizationRules {
	public static final int MAX_EXTENSION_TICKS = 600;
	public static final int MAX_HORIZON_TICKS = 1_200;

	public enum Refusal { NONE, MISSING, UNLOADED, CROSS_DIMENSION, OUT_OF_RANGE, PROTECTED }

	private DimensionalStabilizationRules() {
	}

	public static boolean localBody(boolean loaded, boolean recognizedBody) {
		return loaded && recognizedBody;
	}

	public static Decision existingRoute(boolean exists, boolean originLoaded,
			boolean destinationLoaded, boolean sameDimension, boolean withinLocalRange,
			boolean originLegal, boolean destinationLegal) {
		if (!exists) return new Decision(false, Refusal.MISSING);
		if (!originLoaded || !destinationLoaded) return new Decision(false, Refusal.UNLOADED);
		if (!sameDimension) return new Decision(false, Refusal.CROSS_DIMENSION);
		if (!withinLocalRange) return new Decision(false, Refusal.OUT_OF_RANGE);
		if (!originLegal || !destinationLegal) return new Decision(false, Refusal.PROTECTED);
		return new Decision(true, Refusal.NONE);
	}

	/** Extends a lease while retaining an absolute finite horizon from this interaction. */
	public static long extendedDeadline(long now, long currentDeadline, int requestedTicks) {
		long extension = Math.max(1L, Math.min(MAX_EXTENSION_TICKS, (long) requestedTicks));
		long horizon = saturatedAdd(now, MAX_HORIZON_TICKS);
		long current = Math.max(now, currentDeadline);
		long base = Math.min(current, horizon);
		return Math.max(current, Math.min(horizon, saturatedAdd(base, extension)));
	}

	private static long saturatedAdd(long left, long right) {
		return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
	}

	public record Decision(boolean allowed, Refusal refusal) { }
}
