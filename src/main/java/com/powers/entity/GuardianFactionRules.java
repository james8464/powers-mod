package com.powers.entity;

import com.powers.item.artifact.ArtifactAlignment;
import java.util.UUID;

/** Pure owner, faction, and lifetime predicates for aligned player-shaped guardians. */
public final class GuardianFactionRules {
	private GuardianFactionRules() {
	}

	/** Darkness hunts non-darkness; light hunts darkness; neither may target its owner. */
	public static boolean mayTarget(ArtifactAlignment alignment, UUID ownerId,
			UUID targetId, boolean targetDarkness) {
		if (ownerId != null && ownerId.equals(targetId)) return false;
		return alignment == ArtifactAlignment.DARKNESS ? !targetDarkness : targetDarkness;
	}

	/** Summons expire at zero or when their owner leaves; natural realm mobs use -1. */
	public static boolean shouldExpire(int remainingLifetime, boolean ownerPresent) {
		return remainingLifetime == 0 || remainingLifetime > 0 && !ownerPresent;
	}
}
