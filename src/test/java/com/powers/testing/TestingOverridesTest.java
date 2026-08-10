package com.powers.testing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that operator testing switches are isolated and session-only. */
class TestingOverridesTest {
	private final UUID first = UUID.randomUUID();
	private final UUID second = UUID.randomUUID();

	@AfterEach
	void cleanUp() {
		TestingOverrides.clearAll();
	}

	@Test
	void masterSwitchEnablesBothLimitsForOnlyThatPlayer() {
		TestingOverrides.setAll(first, true);

		assertTrue(TestingOverrides.energyDisabled(first));
		assertTrue(TestingOverrides.cooldownsDisabled(first));
		assertFalse(TestingOverrides.energyDisabled(second));
		assertFalse(TestingOverrides.cooldownsDisabled(second));
	}

	@Test
	void individualSwitchesRemainIndependentAndCanBeCleared() {
		TestingOverrides.setEnergyDisabled(first, true);
		assertTrue(TestingOverrides.energyDisabled(first));
		assertFalse(TestingOverrides.cooldownsDisabled(first));

		TestingOverrides.setCooldownsDisabled(first, true);
		TestingOverrides.setEnergyDisabled(first, false);
		assertFalse(TestingOverrides.energyDisabled(first));
		assertTrue(TestingOverrides.cooldownsDisabled(first));

		TestingOverrides.clear(first);
		assertEquals(TestingOverrides.State.DEFAULT, TestingOverrides.state(first));
	}

	@Test
	void disabledLimitsSuppressPaymentDrainAndRecoveryTime() {
		assertEquals(40, TestingOverrides.energyAfterCost(80, 40, false));
		assertEquals(80, TestingOverrides.energyAfterCost(80, 40, true));
		assertEquals(120, TestingOverrides.cooldownRemaining(120, false));
		assertEquals(0, TestingOverrides.cooldownRemaining(120, true));
	}
}
