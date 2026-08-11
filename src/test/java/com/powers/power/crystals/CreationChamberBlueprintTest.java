package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreationChamberBlueprintTest {
	@Test
	void chamberIsASmallUniqueAllowlistedServerTemplate() {
		var placements = CreationChamberBlueprint.placements();
		assertEquals(26, placements.size());
		assertEquals(placements.size(), new HashSet<>(placements.stream()
				.map(CreationChamberBlueprint.Placement::offset).toList()).size());
		assertTrue(placements.stream().allMatch(placement ->
				placement.role() == CreationChamberBlueprint.Role.FRAME
						|| placement.role() == CreationChamberBlueprint.Role.GLASS
						|| placement.role() == CreationChamberBlueprint.Role.LIGHT));
	}
}
