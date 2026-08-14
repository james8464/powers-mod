package com.powers.fx;

/** Declares how far an authored event remains meaningful to an observer. */
public enum FxLodScope {
	LOCAL(64.0, 160.0, 256.0),
	EVENT_SCALE(96.0, 512.0, 2_048.0),
	CATASTROPHIC(128.0, 1_024.0, 6_000.0);

	private final double nearRange;
	private final double midRange;
	private final double maximumRange;

	FxLodScope(double nearRange, double midRange, double maximumRange) {
		this.nearRange = nearRange;
		this.midRange = midRange;
		this.maximumRange = maximumRange;
	}

	double nearRange() {
		return nearRange;
	}

	double midRange() {
		return midRange;
	}

	public double maximumRange() {
		return maximumRange;
	}
}
