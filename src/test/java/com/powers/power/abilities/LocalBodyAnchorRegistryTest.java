package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalBodyAnchorRegistryTest {
	@Test
	void stateIsIdentityOnlyFiniteAndRemovedAtItsDeadline() {
		LocalBodyAnchorRegistry registry = new LocalBodyAnchorRegistry(2);
		UUID body = UUID.randomUUID();

		assertTrue(registry.bind(body, "minecraft:overworld", 100L, 40));
		assertEquals("minecraft:overworld", registry.dimensionId(body, 139L));
		assertNull(registry.dimensionId(body, 140L));
		assertEquals(0, registry.size());
	}

	@Test
	void finiteCapacityRejectsANewBodyButAllowsExistingIdentityRenewal() {
		LocalBodyAnchorRegistry registry = new LocalBodyAnchorRegistry(1);
		UUID first = UUID.randomUUID();
		assertTrue(registry.bind(first, "minecraft:overworld", 100L, 20));
		assertFalse(registry.bind(UUID.randomUUID(), "minecraft:overworld", 100L, 20));

		assertTrue(registry.bind(first, "minecraft:overworld", 110L, 20));
		assertEquals(140L, registry.deadline(first, 110L));
	}

	@Test
	void invalidIdentityOrDimensionNeverCreatesState() {
		LocalBodyAnchorRegistry registry = new LocalBodyAnchorRegistry(2);
		assertFalse(registry.bind(null, "minecraft:overworld", 0L, 20));
		assertFalse(registry.bind(UUID.randomUUID(), "not a dimension", 0L, 20));
		assertEquals(0, registry.size());
	}

	@Test
	void expiredUnqueriedBodiesCannotStarveNewLoadedBodies() {
		LocalBodyAnchorRegistry registry = new LocalBodyAnchorRegistry(2);
		assertTrue(registry.bind(UUID.randomUUID(), "minecraft:overworld", 0L, 1));
		assertTrue(registry.bind(UUID.randomUUID(), "minecraft:overworld", 0L, 1));

		assertTrue(registry.bind(UUID.randomUUID(), "minecraft:overworld", 2L, 20));
		assertEquals(1, registry.size());
	}
}
