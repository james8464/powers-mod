package com.powers.item.artifact;

import java.util.Objects;

/** Immutable server-authored live menu state for one artifact invocation. */
public record ArtifactActionSnapshot(
		String key,
		ArtifactActionCategory category,
		int cost,
		int energySaved,
		int cooldownTicks,
		int cooldownMaximumTicks,
		boolean active,
		boolean locked,
		int variant) {
	public ArtifactActionSnapshot {
		if (key == null || key.isBlank() || key.length() > 96) {
			throw new IllegalArgumentException("Artifact action key must contain 1..96 characters");
		}
		Objects.requireNonNull(category, "category");
		cost = Math.max(0, cost);
		energySaved = Math.max(0, energySaved);
		cooldownTicks = Math.max(0, cooldownTicks);
		cooldownMaximumTicks = Math.max(0, cooldownMaximumTicks);
	}
}
