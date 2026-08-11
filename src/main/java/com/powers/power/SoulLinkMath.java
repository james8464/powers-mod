package com.powers.power;

/** Pure health-delta calculations for Soul Link. */
public final class SoulLinkMath {
	private static final int MAXIMUM_LINKS = 8;

	private SoulLinkMath() {
	}

	/** Canonical topology cap shared by acquisition, presentation, and tests. */
	public static int maximumLinks() { return MAXIMUM_LINKS; }

	/** Measures only damage after the last known mirrored-health baseline. */
	public static float woundAfterMirror(float lastHealth, Float mirroredHealth, float currentHealth) {
		float baseline = mirroredHealth == null ? lastHealth : Math.min(lastHealth, mirroredHealth);
		return wound(baseline, currentHealth);
	}

	public static float wound(float previous, float current) {
		return Math.max(0.0f, previous - current);
	}

	public static float largestWound(float[] previous, float[] current) {
		if (previous.length != current.length) throw new IllegalArgumentException("length mismatch");
		float largest = 0.0f;
		for (int i = 0; i < previous.length; i++) {
			largest = Math.max(largest, wound(previous[i], current[i]));
		}
		return largest;
	}

	public static float[] snapshot(float[] health) {
		return health.clone();
	}

	public static float cappedMirror(float requested, float remainingCap) {
		return Math.min(Math.max(0.0f, requested), Math.max(0.0f, remainingCap));
	}

	public static float remainingCap(float currentCap, float applied) {
		return Math.max(0.0f, currentCap - Math.max(0.0f, applied));
	}
}
