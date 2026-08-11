package com.powers.fx;

/** Hysteretic server-load controller that preserves silhouettes before density. */
public final class AdaptiveFxBudget {
	private enum Tier {
		FULL(1.0), REDUCED(0.5), MINIMAL(0.25);
		private final double scale;
		Tier(double scale) { this.scale = scale; }
	}

	private final int recoveryTicks;
	private Tier tier = Tier.FULL;
	private int healthyTicks;

	public AdaptiveFxBudget(int recoveryTicks) {
		if (recoveryTicks < 1) throw new IllegalArgumentException("Recovery window must be positive");
		this.recoveryTicks = recoveryTicks;
	}

	public double update(double mspt) {
		if (!Double.isFinite(mspt) || mspt > 48.0) {
			tier = Tier.MINIMAL;
			healthyTicks = 0;
		} else if (mspt > 42.0) {
			if (tier == Tier.FULL) tier = Tier.REDUCED;
			healthyTicks = 0;
		} else {
			double recoveryThreshold = tier == Tier.MINIMAL ? 43.0 : 38.0;
			if (tier != Tier.FULL && mspt < recoveryThreshold && ++healthyTicks >= recoveryTicks) {
				tier = tier == Tier.MINIMAL ? Tier.REDUCED : Tier.FULL;
				healthyTicks = 0;
			} else if (mspt >= recoveryThreshold) {
				healthyTicks = 0;
			}
		}
		return tier.scale;
	}

	public double scale() {
		return tier.scale;
	}

	public static int scaleCount(int requested, double scale, int readableMinimum) {
		if (requested <= 0) return 0;
		int minimum = Math.clamp(readableMinimum, 1, requested);
		return Math.clamp((int) Math.round(requested * Math.clamp(scale, 0.0, 1.0)), minimum, requested);
	}
}
