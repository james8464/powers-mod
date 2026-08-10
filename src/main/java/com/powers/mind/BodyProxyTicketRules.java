package com.powers.mind;

/** Forced-chunk footprint for vulnerable physical bodies. */
public final class BodyProxyTicketRules {
	private BodyProxyTicketRules() {
	}

	public static int radius() {
		return 0;
	}

	public static int maximumChunksPerBody() {
		int diameter = radius() * 2 + 1;
		return diameter * diameter;
	}
}
