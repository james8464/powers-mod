package com.powers.power.artifact;

/** Resolves hostile artifact damage and movement as independent protected outcomes. */
public final class ArtifactImpactRules {
	public record Decision(boolean damage, boolean move) {
	}

	private ArtifactImpactRules() {
	}

	public static Decision decide(boolean hostile, boolean dampened, boolean mayHarm,
			boolean mayForceMove, boolean movementWard) {
		boolean active = hostile && !dampened;
		return new Decision(active && mayHarm, active && mayForceMove && !movementWard);
	}
}
