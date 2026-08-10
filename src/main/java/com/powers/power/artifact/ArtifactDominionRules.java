package com.powers.power.artifact;

import com.powers.item.artifact.ArtifactAlignment;

/** Pure caps and scaling shared by the opposed artifacts' stateful rites. */
public final class ArtifactDominionRules {
	public static final int MAX_FIELDS = 4;
	public static final int MAX_NORMAL_GUARDIANS = 4;
	public static final int MAX_ELITE_GUARDIANS = 2;

	private ArtifactDominionRules() {
	}

	public static float decreeDamage(ArtifactAlignment alignment, float maximumHealth,
			boolean playerTarget, int rank) {
		float percent = alignment == ArtifactAlignment.DARKNESS ? 0.22F : 0.18F;
		float raw = Math.max(0.0F, maximumHealth) * percent + Math.clamp(rank, 0, 10) * 2.0F;
		return Math.min(playerTarget ? 400.0F : 2000.0F, raw);
	}

	public static boolean mayStartField(int activeFields, boolean replacingOwnerField) {
		return replacingOwnerField || activeFields < MAX_FIELDS;
	}

	public static int guardiansToSpawn(int requested, int existingOwned, boolean elite) {
		int cap = elite ? MAX_ELITE_GUARDIANS : MAX_NORMAL_GUARDIANS;
		return Math.max(0, Math.min(Math.max(0, requested), cap - Math.max(0, existingOwned)));
	}

	public static float restoredHealth(ArtifactAlignment alignment, float maximumHealth) {
		float fraction = alignment == ArtifactAlignment.DARKNESS ? 0.35F : 0.45F;
		return Math.max(1.0F, Math.max(0.0F, maximumHealth) * fraction);
	}
}
