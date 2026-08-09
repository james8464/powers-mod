package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.AMETHYST;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.BODY_ANCHOR;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.CAPTURE;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.CEILING;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.FORCEFIELD;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.PROTECTED;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.SPELL_WARD;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.TIME_LOCK;
import static com.powers.power.abilities.BreezyBashRules.CaptureDecision.WIND_RESONANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards bounded Tempest Rite capture, movement, ownership, and lifecycle rules. */
class BreezyBashRulesTest {
	@Test
	void rankCapsRemainFinite() {
		assertEquals(16, BreezyBashRules.targetLimit(false, false));
		assertEquals(24, BreezyBashRules.targetLimit(true, false));
		assertEquals(24, BreezyBashRules.targetLimit(false, true));
		assertEquals(32, BreezyBashRules.targetLimit(true, true));
		assertEquals(16, BreezyBashRules.projectileLimit(true));
		assertEquals(0, BreezyBashRules.projectileLimit(false));
	}

	@Test
	void captureCountersResolveInPrivacyFirstOrder() {
		assertEquals(PROTECTED, decision(false, true, true, true, true, true, false, false));
		assertEquals(AMETHYST, decision(true, true, true, true, true, true, false, false));
		assertEquals(BODY_ANCHOR, decision(true, false, true, true, true, true, false, false));
		assertEquals(FORCEFIELD, decision(true, false, false, true, true, true, false, false));
		assertEquals(SPELL_WARD, decision(true, false, false, false, true, true, false, false));
		assertEquals(TIME_LOCK, decision(true, false, false, false, false, true, false, false));
		assertEquals(CEILING, decision(true, false, false, false, false, false, false, false));
		assertEquals(WIND_RESONANCE, decision(true, false, false, false, false, false, true, false));
		assertEquals(CAPTURE, decision(true, false, false, false, false, false, true, true));
	}

	@Test
	void launchAndSlamImpulsesAreFinite() {
		assertEquals(new Vec3(0.6, 1.4, 0.8), BreezyBashRules.launchImpulse(
				Vec3.ZERO, new Vec3(3.0, 9.0, 4.0), 1.0, 1.4));
		assertEquals(new Vec3(0.0, 1.4, 0.0), BreezyBashRules.launchImpulse(
				Vec3.ZERO, new Vec3(0.0, 4.0, 0.0), 1.0, 1.4));
		assertEquals(Vec3.ZERO, BreezyBashRules.launchImpulse(
				Vec3.ZERO, Vec3.ZERO, Double.NaN, 1.4));
		assertEquals(new Vec3(0.25, -2.5, -0.5), BreezyBashRules.slamVelocity(
				new Vec3(1.0, 8.0, -2.0), 2.5));
		assertEquals(Vec3.ZERO, BreezyBashRules.slamVelocity(Vec3.ZERO, -1.0));
	}

	@Test
	void projectileCurvaturePushesOutwardUnderSpeedCap() {
		Vec3 curved = BreezyBashRules.curveProjectile(
				new Vec3(3.0, 1.0, 4.0), new Vec3(0.0, 0.0, 1.0),
				Vec3.ZERO, 0.4, 1.5);
		assertTrue(curved.x > 0.0);
		assertTrue(curved.z > 0.0);
		assertTrue(curved.length() <= 1.5001);
		assertEquals(Vec3.ZERO, BreezyBashRules.curveProjectile(
				Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, 0.4, 1.5));
	}

	@Test
	void ownershipAndLifecycleRejectCompetingOrInvalidRites() {
		UUID owner = UUID.randomUUID();
		assertTrue(BreezyBashRules.claimAllowed(null, owner));
		assertTrue(BreezyBashRules.claimAllowed(owner, owner));
		assertFalse(BreezyBashRules.claimAllowed(UUID.randomUUID(), owner));
		assertFalse(BreezyBashRules.claimAllowed(null, null));

		assertTrue(BreezyBashRules.riteContinues(
				true, true, true, false, false, true, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				false, true, true, false, false, true, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				true, false, true, false, false, true, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				true, true, false, false, false, true, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				true, true, true, true, false, true, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				true, true, true, false, true, true, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				true, true, true, false, false, false, 117L, 118L));
		assertFalse(BreezyBashRules.riteContinues(
				true, true, true, false, false, true, 118L, 118L));
	}

	private static BreezyBashRules.CaptureDecision decision(boolean movementAllowed,
			boolean dampened, boolean bodyAnchor, boolean forcefield, boolean spellWard,
			boolean frozen, boolean clearPath, boolean claimAllowed) {
		return BreezyBashRules.captureDecision(movementAllowed, dampened, bodyAnchor,
				forcefield, spellWard, frozen, clearPath, claimAllowed);
	}
}
