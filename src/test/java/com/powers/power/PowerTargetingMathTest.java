package com.powers.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PowerTargetingMathTest {
	@Test
	void squaresProjectileSearchRange() {
		assertEquals(2304.0, PowerTargeting.maxDistanceSquared(48.0));
	}

	@Test
	void entityWinsWhenItIsInFrontOfTheBlock() {
		assertEquals(PowerTargeting.TargetKind.ENTITY,
				PowerTargeting.nearestKind(true, 36.0, true, 25.0));
	}

	@Test
	void blockWinsWhenItIsInFrontOfTheEntity() {
		assertEquals(PowerTargeting.TargetKind.BLOCK,
				PowerTargeting.nearestKind(true, 16.0, true, 25.0));
	}

	@Test
	void missWinsOnlyWhenThereIsNoHit() {
		assertEquals(PowerTargeting.TargetKind.MISS,
				PowerTargeting.nearestKind(false, 0.0, false, 0.0));
	}
}
