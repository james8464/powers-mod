package com.powers.magic;

/** Identifies the authoritative registry that owns a magical action. */
public enum MagicOrigin {
	INNATE,
	CRYSTAL,
	ARTIFACT,
	SPELL,
	AMETHYST,
	REALM,
	/** Third-party action registered through the versioned server API. */
	EXTENSION
}
