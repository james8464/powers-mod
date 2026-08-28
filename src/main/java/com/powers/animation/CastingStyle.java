package com.powers.animation;

import java.util.Arrays;
import java.util.Optional;

/** Visual identity layered over the small shared pose vocabulary. */
public enum CastingStyle {
	SHADOW(0), RADIANT(1), DARKNESS(2), HERALD_LIGHT(3), HERALD_DARK(4), FIRST_VESSEL(5);

	private final int networkId;

	CastingStyle(int networkId) {
		this.networkId = networkId;
	}

	public int networkId() {
		return networkId;
	}

	public static Optional<CastingStyle> fromNetworkId(int id) {
		return Arrays.stream(values()).filter(value -> value.networkId == id).findFirst();
	}
}
