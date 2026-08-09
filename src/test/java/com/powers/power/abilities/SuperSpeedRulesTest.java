package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards Chronal Overdrive timing, finite motion, rank caps, and lifecycle. */
class SuperSpeedRulesTest {
	@Test
	void durationUsesExclusiveServerTickBoundary() {
		assertEquals(160, SuperSpeedRules.remainingTicks(100L, 260L, 100L));
		assertEquals(1, SuperSpeedRules.remainingTicks(100L, 260L, 259L));
		assertEquals(0, SuperSpeedRules.remainingTicks(100L, 260L, 260L));
		assertEquals(0, SuperSpeedRules.remainingTicks(100L, 260L, 999L));
	}

	@Test
	void ownedSpeedModifierIsFiniteAndWaterGrounded() {
		assertEquals(1.0, SuperSpeedRules.speedModifier(1.0, false), 0.0001);
		assertEquals(0.35, SuperSpeedRules.speedModifier(1.0, true), 0.0001);
		assertEquals(1.4, SuperSpeedRules.speedModifier(99.0, false), 0.0001);
		assertEquals(0.0, SuperSpeedRules.speedModifier(Double.NaN, false), 0.0001);
		assertEquals(0.0, SuperSpeedRules.speedModifier(-1.0, false), 0.0001);
	}

	@Test
	void trailRejectsStillnessAndTeleportDiscontinuities() {
		assertFalse(SuperSpeedRules.trailAllowed(0.0, 12.0));
		assertTrue(SuperSpeedRules.trailAllowed(4.0, 12.0));
		assertFalse(SuperSpeedRules.trailAllowed(144.1, 12.0));
		assertEquals(8, SuperSpeedRules.trailSegments(4.0));
		assertEquals(24, SuperSpeedRules.trailSegments(30.0));
		assertEquals(0, SuperSpeedRules.trailSegments(Double.NaN));
	}

	@Test
	void secondStepReboundMovesBackFromTheCollision() {
		assertEquals(new Vec3(0.0, 0.32, -0.85), SuperSpeedRules.rebound(
				new Vec3(0.0, 0.0, 1.0), 0.85, 0.32));
		assertEquals(Vec3.ZERO, SuperSpeedRules.rebound(Vec3.ZERO, 0.85, 0.32));
		assertEquals(Vec3.ZERO, SuperSpeedRules.rebound(
				new Vec3(Double.NaN, 0.0, 1.0), 0.85, 0.32));
	}

	@Test
	void pressureAndProjectileMotionStayCapped() {
		assertEquals(new Vec3(0.6, 0.18, 0.8), SuperSpeedRules.pressureImpulse(
				Vec3.ZERO, new Vec3(3.0, 8.0, 4.0), 1.0, 0.18));
		assertEquals(Vec3.ZERO, SuperSpeedRules.pressureImpulse(
				Vec3.ZERO, Vec3.ZERO, 1.0, 0.18));
		Vec3 curved = SuperSpeedRules.curveProjectile(
				new Vec3(3.0, 1.0, 4.0), new Vec3(0.0, 0.0, 1.0),
				Vec3.ZERO, 0.35, 2.2);
		assertTrue(curved.x > 0.0);
		assertTrue(curved.length() <= 2.2001);
		assertEquals(Vec3.ZERO, SuperSpeedRules.curveProjectile(
				Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 0.35, 2.2));
	}

	@Test
	void rankWorkAndLifecycleHaveHardCaps() {
		assertEquals(8, SuperSpeedRules.pressureTargetLimit(true));
		assertEquals(0, SuperSpeedRules.pressureTargetLimit(false));
		assertEquals(8, SuperSpeedRules.afterimageTargetLimit(true));
		assertEquals(0, SuperSpeedRules.afterimageTargetLimit(false));
		assertEquals(16, SuperSpeedRules.projectileLimit(true));
		assertEquals(0, SuperSpeedRules.projectileLimit(false));

		assertTrue(SuperSpeedRules.overdriveContinues(
				true, true, true, false, false, true, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				false, true, true, false, false, true, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				true, false, true, false, false, true, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				true, true, false, false, false, true, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				true, true, true, true, false, true, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				true, true, true, false, true, true, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				true, true, true, false, false, false, 259L, 260L));
		assertFalse(SuperSpeedRules.overdriveContinues(
				true, true, true, false, false, true, 260L, 260L));
	}
}
