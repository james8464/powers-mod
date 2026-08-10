package com.powers.power.artifact;

/** Pure damage-sharing math for Covenant Chain. */
public final class ArtifactCovenantRules {
	public static final int MAX_LINKS_PER_OWNER = 8;
	private ArtifactCovenantRules() {
	}

	/** Half of damage actually received is transferred; invalid values transfer nothing. */
	public static float sharedDamage(float damageTaken) {
		return Float.isFinite(damageTaken) && damageTaken > 0.0F ? damageTaken * 0.5F : 0.0F;
	}

	/** Allows refreshes at the cap but bounds distinct linked allies. */
	public static boolean mayAddLink(int existingOwnerLinks, boolean replacingTarget) {
		return replacingTarget || existingOwnerLinks < MAX_LINKS_PER_OWNER;
	}

	/** Expires at, not after, the authored world-game-time boundary. */
	public static boolean expired(long currentGameTime, long expiresAt) {
		return currentGameTime >= expiresAt;
	}
}
