package com.powers.ai;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerceptionSnapshotRulesTest {
	@Test
	void selectionIsBoundedDeterministicAndImmutable() {
		UUID farther = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID tiedLater = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID tiedFirst = UUID.fromString("00000000-0000-0000-0000-000000000001");
		List<PerceptionObservation> observations = List.of(
				new PerceptionObservation(farther, new Vec3(5, 0, 0), new Vec3(5, 1, 0),
						true, false, true, false, 20, 3),
				new PerceptionObservation(tiedLater, new Vec3(2, 0, 0), new Vec3(2, 1, 0),
						true, false, true, false, 20, 3),
				new PerceptionObservation(tiedFirst, new Vec3(-2, 0, 0), new Vec3(-2, 1, 0),
						true, true, true, false, 20, 3));

		List<PerceptionObservation> selected = PerceptionSnapshotRules.select(observations,
				Vec3.ZERO, 8, 4, 2, observation -> observation.monster());

		assertEquals(List.of(tiedFirst, tiedLater), selected.stream()
				.map(PerceptionObservation::entityId).toList());
		assertThrows(UnsupportedOperationException.class, () -> selected.add(observations.getFirst()));
	}

	@Test
	void sharedSnapshotPassesThirtyPercentInspectionGate() {
		var result = PerceptionSnapshotRules.reduction(12, 64, 192);
		assertEquals(768, result.baselineInspections());
		assertEquals(192, result.actualInspections());
		assertTrue(result.fraction() >= 0.30);
	}

	@Test
	void queryProfilesPreserveLegacyCapsAndGrowOnlyOnDemand() {
		assertEquals(16, PerceptionQueryProfile.GUARDIAN_FIELD.inspectionLimit());
		assertEquals(16, PerceptionQueryProfile.ALLY_LANE.inspectionLimit());
		assertEquals(64, PerceptionQueryProfile.SHADOW_TARGET.inspectionLimit());
		assertEquals(256, PerceptionQueryProfile.GUARDIAN_TARGET.inspectionLimit());
		assertTrue(PerceptionSnapshotRules.requiresRecapture(16, 64));
		assertTrue(!PerceptionSnapshotRules.requiresRecapture(64, 16));
	}
}
