package com.powers.mind;

/** Decides whether old saves without a proxy anchor may use their recorded fallback. */
public final class BodyReturnFallbackRules {
	private BodyReturnFallbackRules() {
	}

	/** Active anchors must always pass the normal realm-aware return validator. */
	public static boolean mayUseLegacyFallback(boolean hasActiveAnchor, boolean returnAccepted) {
		return !hasActiveAnchor && !returnAccepted;
	}
}
