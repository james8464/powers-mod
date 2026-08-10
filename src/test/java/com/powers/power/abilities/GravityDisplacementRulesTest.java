package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.AMETHYST;
import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.BODY_ANCHOR;
import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.CAPTURE;
import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.FORCEFIELD;
import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.PROTECTED;
import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.SPELL_WARD;
import static com.powers.power.abilities.GravityDisplacementRules.CaptureDecision.TIME_LOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards bounded gravity geometry, counter priority, rank caps, and lifecycle boundaries. */
class GravityDisplacementRulesTest {
	@Test
	void captureCounterplayHasStablePrivacyFirstPriority() {
		assertEquals(PROTECTED, GravityDisplacementRules.captureDecision(
				false, true, true, true, true, true));
		assertEquals(AMETHYST, GravityDisplacementRules.captureDecision(
				true, true, true, true, true, true));
		assertEquals(BODY_ANCHOR, GravityDisplacementRules.captureDecision(
				true, false, true, true, true, true));
		assertEquals(FORCEFIELD, GravityDisplacementRules.captureDecision(
				true, false, false, true, true, true));
		assertEquals(SPELL_WARD, GravityDisplacementRules.captureDecision(
				true, false, false, false, true, true));
		assertEquals(TIME_LOCK, GravityDisplacementRules.captureDecision(
				true, false, false, false, false, true));
		assertEquals(CAPTURE, GravityDisplacementRules.captureDecision(
				true, false, false, false, false, false));
	}

	@Test
	void rankVariantsIncreaseButBoundRuntimeWork() {
		assertEquals(16, GravityDisplacementRules.targetLimit(false, false));
		assertEquals(24, GravityDisplacementRules.targetLimit(true, false));
		assertEquals(24, GravityDisplacementRules.targetLimit(false, true));
		assertEquals(32, GravityDisplacementRules.targetLimit(true, true));
		assertEquals(0, GravityDisplacementRules.projectileLimit(false));
		assertEquals(24, GravityDisplacementRules.projectileLimit(true));
	}

	@Test
	void seededOrbitIsDeterministicAndBounded() {
		Vec3 first = GravityDisplacementRules.orbitOffset(0x1234ABCDL, 20, 8.0, 4.0);
		Vec3 repeat = GravityDisplacementRules.orbitOffset(0x1234ABCDL, 20, 8.0, 4.0);
		Vec3 later = GravityDisplacementRules.orbitOffset(0x1234ABCDL, 21, 8.0, 4.0);

		assertEquals(first, repeat);
		assertNotEquals(first, later);
		double horizontal = Math.sqrt(first.x * first.x + first.z * first.z);
		assertTrue(horizontal >= 2.5 && horizontal <= 5.4);
		assertTrue(first.y >= 1.0 && first.y <= 3.2);
		assertEquals(Vec3.ZERO, GravityDisplacementRules.orbitOffset(1L, 0, Double.NaN, 4.0));
		assertEquals(Vec3.ZERO, GravityDisplacementRules.orbitOffset(1L, 0, 8.0, -1.0));
	}

	@Test
	void steeringBlendsMomentumAndCapsSpeed() {
		Vec3 velocity = GravityDisplacementRules.steeringVelocity(
				Vec3.ZERO, new Vec3(0.2, 0.0, 0.0), new Vec3(10.0, 2.0, 0.0), 0.18, 0.85);

		assertTrue(velocity.length() <= 0.850001);
		assertTrue(velocity.x > 0.0);
		assertTrue(velocity.y > 0.0);
		assertEquals(Vec3.ZERO, GravityDisplacementRules.steeringVelocity(
				null, Vec3.ZERO, Vec3.ZERO, 0.18, 0.85));
		assertEquals(Vec3.ZERO, GravityDisplacementRules.steeringVelocity(
				Vec3.ZERO, Vec3.ZERO, new Vec3(Double.NaN, 0.0, 0.0), 0.18, 0.85));
	}

	@Test
	void collapseImpulseIsRadialAndVerticallyBounded() {
		Vec3 impulse = GravityDisplacementRules.collapseImpulse(
				Vec3.ZERO, new Vec3(3.0, 30.0, 4.0), 1.25, 0.7);

		assertEquals(0.75, impulse.x, 0.0001);
		assertEquals(-0.7, impulse.y, 0.0001);
		assertEquals(1.0, impulse.z, 0.0001);
		assertEquals(new Vec3(0.0, -0.7, 0.0),
				GravityDisplacementRules.collapseImpulse(Vec3.ZERO, Vec3.ZERO, 1.25, 0.7));
		assertEquals(Vec3.ZERO, GravityDisplacementRules.collapseImpulse(
				Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), -1.0, 0.7));
	}

	@Test
	void ancientProjectileCurvePreservesFiniteBoundedMotion() {
		Vec3 bent = GravityDisplacementRules.bendProjectile(
				new Vec3(4.0, 1.0, 0.0), new Vec3(0.0, 0.0, 2.0), Vec3.ZERO, 0.22, 2.0);

		assertTrue(bent.length() <= 2.000001);
		assertNotEquals(new Vec3(0.0, 0.0, 2.0), bent);
		assertEquals(Vec3.ZERO, GravityDisplacementRules.bendProjectile(
				Vec3.ZERO, new Vec3(Double.POSITIVE_INFINITY, 0.0, 0.0), Vec3.ZERO, 0.22, 2.0));
	}

	@Test
	void nearestOrreryWinsSharedTargetOnlyBeyondHysteresis() {
		assertTrue(GravityDisplacementRules.claimWinner(8.0, 9.0, 0.25));
		assertFalse(GravityDisplacementRules.claimWinner(8.8, 9.0, 0.25));
		assertFalse(GravityDisplacementRules.claimWinner(10.0, 9.0, 0.25));
		assertFalse(GravityDisplacementRules.claimWinner(Double.NaN, 9.0, 0.25));
		assertFalse(GravityDisplacementRules.claimWinner(8.0, -1.0, 0.25));
	}

	@Test
	void fieldRequiresLiveUnsuppressedOwnerBeforeExclusiveExpiry() {
		assertTrue(GravityDisplacementRules.fieldContinues(
				true, true, true, false, false, true, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				false, true, true, false, false, true, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				true, false, true, false, false, true, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				true, true, false, false, false, true, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				true, true, true, true, false, true, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				true, true, true, false, true, true, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				true, true, true, false, false, false, 99L, 100L));
		assertFalse(GravityDisplacementRules.fieldContinues(
				true, true, true, false, false, true, 100L, 100L));
	}
}
