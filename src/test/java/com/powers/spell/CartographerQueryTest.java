package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartographerQueryTest {
	@Test
	void parsesNamespacedStructureAndBiomeQueries() {
		assertEquals(new CartographerQuery(CartographerQuery.Kind.STRUCTURE, "minecraft:stronghold"),
				CartographerQuery.parse(" structure minecraft:stronghold ").orElseThrow());
		assertEquals(new CartographerQuery(CartographerQuery.Kind.BIOME, "minecraft:deep_dark"),
				CartographerQuery.parse("biome minecraft:deep_dark").orElseThrow());
	}

	@Test
	void normalizesAuthoredLandmarkNames() {
		assertEquals(new CartographerQuery(CartographerQuery.Kind.LANDMARK, "herald_court"),
				CartographerQuery.parse("landmark Herald Court").orElseThrow());
	}

	@Test
	void rejectsMissingModesInvalidIdentifiersAndOversizedInput() {
		assertTrue(CartographerQuery.parse("minecraft:stronghold").isEmpty());
		assertTrue(CartographerQuery.parse("structure stronghold").isEmpty());
		assertTrue(CartographerQuery.parse("biome minecraft:bad id").isEmpty());
		assertTrue(CartographerQuery.parse("landmark !!!").isEmpty());
		assertTrue(CartographerQuery.parse("structure " + "x".repeat(70)).isEmpty());
	}
}
