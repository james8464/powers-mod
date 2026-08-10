package com.powers.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestActorPowerStateTest {
	private static final UUID ACTOR = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

	@AfterEach
	void clearState() {
		TestActorPowerState.clearAll();
	}

	@Test
	void simulatedEnergyIsFiniteAndSaturating() {
		assertEquals(1_000, TestActorPowerState.energy(ACTOR));
		assertEquals(250, TestActorPowerState.drain(ACTOR, 250));
		assertEquals(750, TestActorPowerState.energy(ACTOR));
		assertEquals(750, TestActorPowerState.drain(ACTOR, Integer.MAX_VALUE));
		assertEquals(0, TestActorPowerState.energy(ACTOR));

		TestActorPowerState.restore(ACTOR);
		assertEquals(1_000, TestActorPowerState.energy(ACTOR));
		TestActorPowerState.empty(ACTOR);
		assertEquals(0, TestActorPowerState.energy(ACTOR));
	}

	@Test
	void anchorExpiresAtItsExactDeadline() {
		TestActorPowerState.anchor(ACTOR, "powers:dark_realm", 200L);

		assertEquals("powers:dark_realm", TestActorPowerState.anchorDimensionId(ACTOR, 199L));
		assertNull(TestActorPowerState.anchorDimensionId(ACTOR, 200L));
		assertNull(TestActorPowerState.anchorDimensionId(ACTOR, 201L));
	}

	@Test
	void invalidAnchorAndExplicitCleanupLeaveNoState() {
		TestActorPowerState.anchor(ACTOR, "not a dimension", 200L);
		assertNull(TestActorPowerState.anchorDimensionId(ACTOR, 100L));

		TestActorPowerState.drain(ACTOR, 100);
		TestActorPowerState.anchor(ACTOR, "minecraft:overworld", 200L);
		TestActorPowerState.clear(ACTOR);
		assertEquals(1_000, TestActorPowerState.energy(ACTOR));
		assertNull(TestActorPowerState.anchorDimensionId(ACTOR, 100L));
	}
}
