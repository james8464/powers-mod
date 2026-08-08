package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AbilityArithmeticTest {
	@Test
	void cozyCampfireRunsForExactlyTwoHundredTicks() {
		assertEquals(40, AbilityArithmetic.pulseCount(200, 5));
		assertEquals(195, AbilityArithmetic.afterPulse(200, 5));
		assertEquals(0, AbilityArithmetic.afterPulse(5, 5));
	}

	@Test
	void energyDrainDistributesRemaindersAndAlwaysReachesZero() {
		int remaining = 250;
		for (int ticks = 40; ticks > 0; ticks--) {
			remaining -= AbilityArithmetic.drainStep(remaining, ticks);
		}
		assertEquals(0, remaining);
	}

	@Test
	void scaledMissEndpointUsesTheActualSkillRange() {
		assertArrayEquals(new double[] {25.0, 2.0, -5.0},
				AbilityArithmetic.endpoint(1.0, 2.0, 3.0, 0.75, 0.0, -0.25, 32.0));
	}

	@Test
	void selectingTheNextCrystalModeCostsNoEnergy() {
		assertEquals(1, AbilityArithmetic.nextMode(0, 3));
		assertEquals(0, AbilityArithmetic.nextMode(2, 3));
		assertFalse(AbilityArithmetic.costsEnergy(true));
	}
}
