package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ArtifactOwnershipPolicyTest {
	@Test
	void acceptsMatchesInEveryExposedTopLevelSlot() {
		List<String> slots = List.of("empty", "carrier", "empty", "empty", "artifact");
		assertTrue(ArtifactOwnershipPolicy.anyDirect(
				slots.size(), slots::get, "artifact"::equals));
	}

	@Test
	void doesNotInferOwnershipFromOpaqueCarrierEntries() {
		List<String> slots = List.of("carrier-with-nested-artifact");
		assertFalse(ArtifactOwnershipPolicy.anyDirect(
				slots.size(), slots::get, "artifact"::equals));
	}

	@Test
	void emptyAndInvalidInventoriesFailClosed() {
		assertFalse(ArtifactOwnershipPolicy.anyDirect(0, slot -> "artifact", "artifact"::equals));
		assertFalse(ArtifactOwnershipPolicy.anyDirect(1, null, "artifact"::equals));
		assertFalse(ArtifactOwnershipPolicy.anyDirect(1, slot -> "artifact", null));
	}
}
