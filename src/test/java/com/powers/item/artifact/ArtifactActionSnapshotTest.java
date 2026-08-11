package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactActionSnapshotTest {
	@Test
	void clampsUntrustedLiveNumbersWithoutLosingState() {
		ArtifactActionSnapshot snapshot = new ArtifactActionSnapshot(
				"innate/lightning_strike", ArtifactActionCategory.ROUTED_POWER,
				-4, -5, -3, -2, true, false, 99);

		assertEquals(0, snapshot.cost());
		assertEquals(0, snapshot.energySaved());
		assertEquals(0, snapshot.cooldownTicks());
		assertEquals(0, snapshot.cooldownMaximumTicks());
		assertEquals(99, snapshot.variant());
	}

	@Test
	void rejectsBlankOrOversizedKeys() {
		assertThrows(IllegalArgumentException.class, () -> new ArtifactActionSnapshot(
				"", ArtifactActionCategory.DOMINION, 1, 1, 1, 1, false, false, -1));
		assertThrows(IllegalArgumentException.class, () -> new ArtifactActionSnapshot(
				"x".repeat(97), ArtifactActionCategory.DOMINION, 1, 1, 1, 1, false, false, -1));
	}
}
