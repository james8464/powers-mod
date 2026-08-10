package com.powers.item.artifact;

/** Cooldown asymmetry for Abyssal Apotheosis and Empyrean Ascendance. */
public final class ArtifactCooldownRules {
	private ArtifactCooldownRules() {
	}

	/** Returns the bounded cooldown after alignment rank-ten rules are applied. */
	public static int cooldownTicks(ArtifactAlignment alignment, int rank, int baseTicks) {
		int bounded = Math.max(0, baseTicks);
		if (rank < 10) return bounded;
		return alignment == ArtifactAlignment.DARKNESS ? 0 : (int) Math.ceil(bounded * 0.4);
	}
}
