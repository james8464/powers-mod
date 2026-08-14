package com.powers.fx;

/** Stable semantic distance tier; geometry identity never depends on particle density. */
public enum FxLodTier {
	NEAR(0),
	MID(1),
	FAR(2),
	HIDDEN(3);

	private final int networkId;

	FxLodTier(int networkId) {
		this.networkId = networkId;
	}

	public int networkId() {
		return networkId;
	}

	public static FxLodTier fromNetworkId(int id) {
		return switch (id) {
			case 0 -> NEAR;
			case 1 -> MID;
			case 2 -> FAR;
			default -> HIDDEN;
		};
	}
}
