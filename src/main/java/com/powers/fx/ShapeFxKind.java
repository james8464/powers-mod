package com.powers.fx;

/** Geometry families expanded by the client instead of the game server. */
public enum ShapeFxKind {
	RING,
	RUNE,
	SPIRAL;

	/** Returns the bounded number of particles represented by an authored shape. */
	public int requestedParticles(int authoredPoints) {
		int points = Math.clamp(authoredPoints, 0, 256);
		if (points == 0) return 0;
		return this == RUNE ? points * 2 + Math.max(6, points / 2) : points;
	}

	public int networkId() {
		return ordinal();
	}

	public static ShapeFxKind fromNetworkId(int id) {
		return values()[Math.clamp(id, 0, values().length - 1)];
	}
}
