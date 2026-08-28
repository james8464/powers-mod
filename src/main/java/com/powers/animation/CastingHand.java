package com.powers.animation;

import java.util.Arrays;
import java.util.Optional;

/** Authored hand emphasis for one semantic casting pose. */
public enum CastingHand {
	NONE(0), LEFT(1), RIGHT(2), BOTH(3);

	private final int networkId;

	CastingHand(int networkId) {
		this.networkId = networkId;
	}

	public int networkId() {
		return networkId;
	}

	public static Optional<CastingHand> fromNetworkId(int id) {
		return Arrays.stream(values()).filter(value -> value.networkId == id).findFirst();
	}
}
