package com.powers.magic.fx;

/** Identifies the choreography family carried by a semantic magic FX packet. */
public enum MagicFxKind {
	CAST(0),
	INTERACTION(1);

	private final int networkId;

	MagicFxKind(int networkId) {
		this.networkId = networkId;
	}

	/** Returns the stable compact identifier used by the clientbound protocol. */
	public int networkId() {
		return networkId;
	}

	/** Rejects unknown protocol values instead of silently changing choreography. */
	public static MagicFxKind fromNetworkId(int networkId) {
		return switch (networkId) {
			case 0 -> CAST;
			case 1 -> INTERACTION;
			default -> throw new IllegalArgumentException("Unknown magic FX kind: " + networkId);
		};
	}
}
