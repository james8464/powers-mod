package com.powers.power.scaling;

import java.util.LinkedHashSet;
import java.util.List;

/** Pure input bounds for the common player scale collision preview. */
public final class SafeScaleTransitionRules {
	private static final double MAX_LOOKAHEAD = 4.0;

	private SafeScaleTransitionRules() { }

	public static List<Double> candidates(List<Double> requested) {
		if (requested == null) return List.of();
		LinkedHashSet<Double> safe = new LinkedHashSet<>();
		for (Double scale : requested) {
			if (scale != null && Double.isFinite(scale) && scale > 0.0) safe.add(scale);
		}
		return List.copyOf(safe);
	}

	public static boolean validLookahead(double blocks) {
		return Double.isFinite(blocks) && blocks >= 0.0 && blocks <= MAX_LOOKAHEAD;
	}

	public static double sanitizeLookahead(double blocks) {
		return validLookahead(blocks) ? blocks : 0.0;
	}
}
