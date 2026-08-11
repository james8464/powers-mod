package com.powers.power.travel;

/** Pure contraction policy shared by travel recovery and persistent world events. */
public final class WorldBoundaryRules {
	public enum EventDecision { CONTINUE, CANCEL, COMPLETE_CLIPPED }

	private WorldBoundaryRules() {
	}

	public static boolean validSpan(double minimum, double maximum, double margin) {
		return Double.isFinite(minimum) && Double.isFinite(maximum) && Double.isFinite(margin)
				&& margin >= 0.0 && maximum - minimum > margin * 2.0;
	}

	public static double clampCoordinate(double value, double minimum, double maximum, double margin) {
		if (!validSpan(minimum, maximum, margin) || !Double.isFinite(value)) return value;
		return Math.clamp(value, minimum + margin, maximum - margin);
	}

	public static EventDecision eventDecision(boolean committed, boolean centerInside) {
		if (centerInside) return EventDecision.CONTINUE;
		return committed ? EventDecision.COMPLETE_CLIPPED : EventDecision.CANCEL;
	}
}
