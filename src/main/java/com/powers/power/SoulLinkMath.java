package com.powers.power;

/** Pure health-delta calculations for Soul Link. */
public final class SoulLinkMath {
	private SoulLinkMath() {
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
}
