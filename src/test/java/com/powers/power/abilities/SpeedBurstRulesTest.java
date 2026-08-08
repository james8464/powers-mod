package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards finite dash geometry, collision ordering, and ranked re-entry boundaries. */
class SpeedBurstRulesTest {
	@Test
	void dashNormalizesDirectionAndCapsVerticalAcceleration() {
		Vec3 upward = SpeedBurstRules.dashVector(
				new Vec3(0.6, 0.8, 0.0), 2.2, -0.35, 0.8);
		Vec3 downward = SpeedBurstRules.dashVector(
				new Vec3(0.6, -0.8, 0.0), 2.2, -0.35, 0.8);

		assertEquals(1.32, upward.x, 0.0001);
		assertEquals(0.80, upward.y, 0.0001);
		assertEquals(0.0, upward.z, 0.0001);
		assertEquals(-0.35, downward.y, 0.0001);
	}

	@Test
	void malformedOrPowerlessDashInputsProduceNoMovement() {
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(null, 2.2, -0.35, 0.8));
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(Vec3.ZERO, 2.2, -0.35, 0.8));
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(
				new Vec3(Double.NaN, 0.0, 0.0), 2.2, -0.35, 0.8));
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(
				new Vec3(1.0, 0.0, 0.0), 0.0, -0.35, 0.8));
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(
				new Vec3(1.0, 0.0, 0.0), -2.2, -0.35, 0.8));
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(
				new Vec3(1.0, 0.0, 0.0), Double.NaN, -0.35, 0.8));
		assertEquals(Vec3.ZERO, SpeedBurstRules.dashVector(
				new Vec3(1.0, 0.0, 0.0), 2.2, 0.8, -0.35));
	}

	@Test
	void collisionFractionStopsAtTheFirstBlockedBodySample() {
		assertEquals(0.5, SpeedBurstRules.lastSafeFraction(true, true, false, true), 0.0001);
		assertEquals(0.0, SpeedBurstRules.lastSafeFraction(false, true, true), 0.0001);
		assertEquals(1.0, SpeedBurstRules.lastSafeFraction(true, true, true), 0.0001);
		assertEquals(0.0, SpeedBurstRules.lastSafeFraction(), 0.0001);
		assertEquals(0.0, SpeedBurstRules.lastSafeFraction((boolean[]) null), 0.0001);
	}

	@Test
	void secondStepUsesAnInclusiveOpeningAndExclusiveExpiry() {
		assertFalse(SpeedBurstRules.secondStepAvailable(102L, 150L, 101L, true));
		assertTrue(SpeedBurstRules.secondStepAvailable(102L, 150L, 102L, true));
		assertTrue(SpeedBurstRules.secondStepAvailable(102L, 150L, 149L, true));
		assertFalse(SpeedBurstRules.secondStepAvailable(102L, 150L, 150L, true));
		assertFalse(SpeedBurstRules.secondStepAvailable(102L, 150L, 120L, false));
		assertFalse(SpeedBurstRules.secondStepAvailable(150L, 102L, 120L, true));
	}

	@Test
	void secondStepRemainingIsNonNegativeAndOverflowSafe() {
		assertEquals(48, SpeedBurstRules.secondStepRemaining(150L, 102L, true));
		assertEquals(0, SpeedBurstRules.secondStepRemaining(150L, 150L, true));
		assertEquals(0, SpeedBurstRules.secondStepRemaining(150L, 102L, false));
		assertEquals(Integer.MAX_VALUE,
				SpeedBurstRules.secondStepRemaining(Long.MAX_VALUE, 0L, true));
	}
}
