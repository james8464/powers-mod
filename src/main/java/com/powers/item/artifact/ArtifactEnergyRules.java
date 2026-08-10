package com.powers.item.artifact;

/** Pure carried-artifact regeneration policy, including rank-ten apotheosis. */
public final class ArtifactEnergyRules {
	private ArtifactEnergyRules() {
	}

	public static int regenerationPerSecond(ArtifactAlignment alignment, int rank) {
		int boundedRank = Math.max(0, Math.min(10, rank));
		if (alignment == ArtifactAlignment.DARKNESS) {
			return boundedRank >= 10 ? 900 : 80 + boundedRank * 35;
		}
		return boundedRank >= 10 ? 300 : 40 + boundedRank * 15;
	}
}
