package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportDimensionMenuTest {
	@Test
	void keepsCompleteServerSuppliedModdedIdentifiers() {
		String longId = "other_mod:the_unabridged_celestial_archive_dimension";
		List<String> visible = TeleportDimensionMenu.visibleIds(List.of(
				"powers:light_realm", longId, "minecraft:overworld"), true);

		assertTrue(visible.contains(longId));
		assertEquals("minecraft:overworld", visible.getFirst());
	}

	@Test
	void filtersOnlyTheDarkRealmWhenTheServerStateHidesIt() {
		List<String> visible = TeleportDimensionMenu.visibleIds(List.of(
				"powers:dark_realm", "example:dark_realm", "powers:light_realm"), false);

		assertFalse(visible.contains("powers:dark_realm"));
		assertTrue(visible.contains("example:dark_realm"));
	}
}
