package com.powers.fx;

/** Small per-server limiter that resets on a new game tick. */
public final class ParticleBudget {
	private static final double FIRST_PERSON_CLARITY_RADIUS_SQUARED = 16.0;
	private final int limit;
	private long tick = Long.MIN_VALUE;
	private int used;

	public ParticleBudget(int limit) {
		this.limit = Math.max(1, limit);
	}

	public int claim(long currentTick, int requested) {
		if (requested <= 0) return 0;
		if (tick != currentTick) {
			tick = currentTick;
			used = 0;
		}
		int granted = Math.min(requested, Math.max(0, limit - used));
		used += granted;
		return granted;
	}

	/** Keeps remote silhouettes intact while thinning dense scatter around a viewer's camera. */
	public static int viewerCount(int requested, double distanceSquared) {
		return viewerCount(requested, distanceSquared, -1.0);
	}

	/** Excludes dense scatter directly over a near first-person reticle. */
	public static int viewerCount(int requested, double distanceSquared, double viewDot) {
		if (requested <= 0) return 0;
		if (distanceSquared > FIRST_PERSON_CLARITY_RADIUS_SQUARED) return requested;
		if (distanceSquared <= 2.25 && Double.isFinite(viewDot) && viewDot >= 0.72) return 1;
		return Math.max(1, (int) Math.ceil(requested * 0.25));
	}

	int limit() {
		return limit;
	}
}
