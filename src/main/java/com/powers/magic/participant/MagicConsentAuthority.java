package com.powers.magic.participant;

/** The only four ways a magic target may answer a consent-gated request. */
public enum MagicConsentAuthority {
	PLAYER_SETTINGS,
	OWNER_DELEGATED,
	ALWAYS_ALLOW_TESTS,
	NONE
}
