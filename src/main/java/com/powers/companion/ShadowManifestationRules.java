package com.powers.companion;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Pure body identity, visibility-transition, death, and recall policy. */
public final class ShadowManifestationRules {
	public static final int RECALL_DELAY_TICKS = 100;

	public record VisibilityTransition(UUID bodyId, boolean revealed,
			boolean replaceBody, boolean restoreHealthOrEffects) {
	}

	private ShadowManifestationRules() {
	}

	public static VisibilityTransition visibility(UUID bodyId,
			boolean wasRevealed, boolean revealed) {
		return new VisibilityTransition(bodyId, revealed, false, false);
	}

	public static Optional<UUID> canonicalBody(UUID recorded, List<UUID> loaded) {
		if (loaded == null || loaded.isEmpty()) return Optional.empty();
		if (recorded != null && loaded.contains(recorded)) return Optional.of(recorded);
		return loaded.stream().filter(java.util.Objects::nonNull)
				.min(Comparator.comparing(UUID::toString));
	}

	public static ShadowCompanionData afterDeath(ShadowCompanionData data, long currentTick) {
		return data.withoutBody().withStance(ShadowStance.DOWNED)
				.withEnergy(ShadowCompanionRules.recallEnergy())
				.withRecallReadyAt(Math.max(0L, currentTick) + RECALL_DELAY_TICKS);
	}

	public static boolean mayRecall(ShadowCompanionData data, long currentTick) {
		return data != null && currentTick >= data.recallReadyAt();
	}
}
