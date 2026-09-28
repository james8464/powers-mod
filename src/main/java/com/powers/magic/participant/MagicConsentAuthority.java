package com.powers.magic.participant;

/** The only four ways a magic target may answer a consent-gated request. */
public enum MagicConsentAuthority {
	PLAYER_SETTINGS,
	OWNER_DELEGATED,
	ALWAYS_ALLOW_TESTS,
	NONE;

	/** True only when an automated actor explicitly opted into a consent-gated action. */
	public boolean permitsAutomatedTarget() {
		return this == ALWAYS_ALLOW_TESTS;
	}
}
