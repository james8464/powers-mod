package com.powers.item.artifact;

import java.util.Set;

/** Discrete equipment scaling that never borrows the continuous innate rank curve. */
public final class ArtifactScalingRules {
	/** Explicit potency, reach, duration, and mechanic variants for one artifact tier. */
	public record Profile(double potency, double range, double duration,
			boolean apotheosis, Set<String> variants) {
		public Profile {
			if (!Double.isFinite(potency) || potency < 1.0
					|| !Double.isFinite(range) || range < 1.0
					|| !Double.isFinite(duration) || duration < 1.0) {
				throw new IllegalArgumentException("Artifact scaling must be finite and non-reductive");
			}
			variants = Set.copyOf(variants);
		}
	}

	private ArtifactScalingRules() {
	}

	/** Rank ten unlocks one discrete apotheosis; ranks zero through nine share the relic baseline. */
	public static Profile profile(ArtifactAlignment alignment, int rank) {
		boolean apotheosis = rank >= 10;
		if (alignment == ArtifactAlignment.DARKNESS) {
			return apotheosis
					? new Profile(6.0, 2.0, 2.0, true, Set.of(
							"empowered_impact", "afterimage", "reflective_ward",
							"dark_resurgence", "ancient_mastery"))
					: new Profile(2.5, 1.35, 1.4, false, Set.of(
							"empowered_impact", "afterimage"));
		}
		return apotheosis
				? new Profile(3.0, 1.65, 1.75, true, Set.of(
						"empowered_impact", "soul_echo", "reflective_ward", "ancient_mastery"))
				: new Profile(1.75, 1.25, 1.3, false, Set.of(
						"empowered_impact", "soul_echo"));
	}
}
