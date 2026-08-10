package com.powers.item.artifact;

import com.powers.magic.MagicSignificance;

/** Immutable server-authored metadata for one selectable artifact invocation. */
public record ArtifactActionDefinition(
		String key,
		String abilityId,
		ArtifactActionCategory category,
		ArtifactAlignment alignment,
		int requiredRank,
		int energyCost,
		int baseCooldownTicks,
		MagicSignificance significance) {
	public ArtifactActionDefinition {
		if (key == null || key.isBlank() || key.length() > 96) {
			throw new IllegalArgumentException("Invalid artifact action key");
		}
		if (abilityId == null || !abilityId.matches("[a-z0-9_]+")) {
			throw new IllegalArgumentException("Invalid artifact ability ID");
		}
		if (category == null || alignment == null || significance == null) {
			throw new IllegalArgumentException("Artifact action metadata is incomplete");
		}
		if (requiredRank < 1 || requiredRank > 10 || energyCost <= 0 || baseCooldownTicks < 0) {
			throw new IllegalArgumentException("Artifact action numbers are out of bounds");
		}
	}
}
