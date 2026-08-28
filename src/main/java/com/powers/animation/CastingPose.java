package com.powers.animation;

import java.util.Arrays;
import java.util.Optional;

/** Closed semantic casting-pose vocabulary shared by server and client. */
public enum CastingPose {
	INVOKE(0), PROJECT(1), CHANNEL(2), RELEASE(3);

	private final int networkId;

	CastingPose(int networkId) {
		this.networkId = networkId;
	}

	public int networkId() {
		return networkId;
	}

	public static Optional<CastingPose> fromNetworkId(int id) {
		return Arrays.stream(values()).filter(value -> value.networkId == id).findFirst();
	}
}
