package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ensures the removed self-slow mode cannot re-enter the crystal cycle. */
class SpaceTimeModeRulesTest {
	@Test
	void cycleContainsOnlyUsefulAccelerationAndFreezeModes() {
		assertEquals(2, SpaceTimeModeRules.count());
		assertEquals(SpaceTimeModeRules.Mode.ACCELERATE, SpaceTimeModeRules.mode(0));
		assertEquals(SpaceTimeModeRules.Mode.FREEZE, SpaceTimeModeRules.mode(1));
		assertEquals(1, SpaceTimeModeRules.next(0));
		assertEquals(0, SpaceTimeModeRules.next(1));
	}

	@Test
	void freezeHasTheLowOminousPitchInsteadOfTheAccelerationPitch() {
		assertEquals(1.4F, SpaceTimeModeRules.soundPitch(
				SpaceTimeModeRules.Mode.ACCELERATE));
		assertEquals(0.35F, SpaceTimeModeRules.soundPitch(
				SpaceTimeModeRules.Mode.FREEZE));
	}
}
