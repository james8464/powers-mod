package com.powers.fx;

/** Geometry families with different minimum samples for a recognisable silhouette. */
public enum FxShapeFamily {
	BEAM(8),
	RING(12),
	RUNE(15),
	SPIRAL(12),
	MAGIC(12),
	COLUMN(32);

	private final int minimumSamples;

	FxShapeFamily(int minimumSamples) {
		this.minimumSamples = minimumSamples;
	}

	int minimumSamples() {
		return minimumSamples;
	}
}
