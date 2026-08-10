package com.powers.power.artifact;

/** Pure damage-sharing math for Covenant Chain. */
public final class ArtifactCovenantRules {
	private ArtifactCovenantRules() {
	}

	/** Half of damage actually received is transferred; invalid values transfer nothing. */
	public static float sharedDamage(float damageTaken) {
		return Float.isFinite(damageTaken) && damageTaken > 0.0F ? damageTaken * 0.5F : 0.0F;
	}
}
