package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrystalTargetingTest {
	@Test
	void emptyAimAndSneakUseAlwaysFallBackToTheCaster() {
		assertEquals(CrystalTargeting.JourneyTarget.CASTER,
				CrystalTargeting.journeyTarget(false, false));
		assertEquals(CrystalTargeting.JourneyTarget.CASTER,
				CrystalTargeting.journeyTarget(true, true));
	}

	@Test
	void ordinaryUseTargetsOnlyAnActuallyAimedPlayer() {
		assertEquals(CrystalTargeting.JourneyTarget.AIMED_PLAYER,
				CrystalTargeting.journeyTarget(false, true));
	}
}
