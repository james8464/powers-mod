package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.powers.power.abilities.StarfallRules.Counterplay.AMETHYST;
import static com.powers.power.abilities.StarfallRules.Counterplay.DARKNESS;
import static com.powers.power.abilities.StarfallRules.Counterplay.KINETIC_WARD;
import static com.powers.power.abilities.StarfallRules.Counterplay.PURE_LIGHT;
import static com.powers.power.abilities.StarfallRules.Counterplay.SAFE_ZONE;
import static com.powers.power.abilities.StarfallRules.Counterplay.SANCTUARY;
import static com.powers.power.abilities.StarfallRules.Counterplay.STRIKE;
import static com.powers.power.abilities.StarfallRules.Counterplay.UNLOADED;
import static com.powers.power.abilities.StarfallRules.Counterplay.UNOWNED;
import static com.powers.power.abilities.StarfallRules.Counterplay.WATER;
import static com.powers.power.abilities.StarfallRules.Phase.CROWN;
import static com.powers.power.abilities.StarfallRules.Phase.FINISHED;
import static com.powers.power.abilities.StarfallRules.Phase.OMEN;
import static com.powers.power.abilities.StarfallRules.Phase.RAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards finite Astral Convergence timing, geometry, counterplay, and ownership. */
class StarfallRulesTest {
	@Test
	void strikeScheduleAndRankBonusesRemainFinite() {
		assertEquals(8, StarfallRules.strikeCount(false, false));
		assertEquals(10, StarfallRules.strikeCount(true, false));
		assertEquals(10, StarfallRules.strikeCount(false, true));
		assertEquals(12, StarfallRules.strikeCount(true, true));
		assertEquals(20, StarfallRules.strikeAge(0));
		assertEquals(26, StarfallRules.strikeAge(1));
		assertEquals(0, StarfallRules.strikesDue(19, 8));
		assertEquals(1, StarfallRules.strikesDue(20, 8));
		assertEquals(8, StarfallRules.strikesDue(500, 8));
		assertEquals(Long.MAX_VALUE, StarfallRules.crownAge(8, false));
		assertEquals(70L, StarfallRules.crownAge(8, true));
	}

	@Test
	void phasesExposeOmenRainCrownAndExclusiveFinish() {
		assertEquals(OMEN, StarfallRules.phase(0, 8, true));
		assertEquals(OMEN, StarfallRules.phase(19, 8, true));
		assertEquals(RAIN, StarfallRules.phase(20, 8, true));
		assertEquals(CROWN, StarfallRules.phase(70, 8, true));
		assertEquals(FINISHED, StarfallRules.phase(78, 8, true));
		assertEquals(RAIN, StarfallRules.phase(65, 8, false));
		assertEquals(FINISHED, StarfallRules.phase(72, 8, false));
	}

	@Test
	void goldenAngleOffsetsAreDeterministicUniqueAndInsideTheField() {
		Set<Vec3> offsets = new HashSet<>();
		for (int index = 0; index < 12; index++) {
			Vec3 offset = StarfallRules.strikeOffset(0x1234ABCDL, index, 12, 8.0);
			assertEquals(offset, StarfallRules.strikeOffset(0x1234ABCDL, index, 12, 8.0));
			assertTrue(offset.horizontalDistance() <= 8.000001);
			assertEquals(0.0, offset.y, 0.0);
			offsets.add(offset);
		}
		assertEquals(12, offsets.size());
		assertNotEquals(StarfallRules.strikeOffset(1L, 0, 8, 6.0),
				StarfallRules.strikeOffset(2L, 0, 8, 6.0));
		assertEquals(Vec3.ZERO, StarfallRules.strikeOffset(1L, -1, 8, 6.0));
		assertEquals(Vec3.ZERO, StarfallRules.strikeOffset(1L, 0, 0, 6.0));
	}

	@Test
	void impactCounterplayUsesStableProtectionFirstPriority() {
		assertEquals(UNOWNED, decision(false, false, true, true, true, true, true, true, true));
		assertEquals(UNLOADED, decision(true, false, true, true, true, true, true, true, true));
		assertEquals(SAFE_ZONE, decision(true, true, true, true, true, true, true, true, true));
		assertEquals(AMETHYST, decision(true, true, false, true, true, true, true, true, true));
		assertEquals(SANCTUARY, decision(true, true, false, false, true, true, true, true, true));
		assertEquals(KINETIC_WARD, decision(true, true, false, false, false, true, true, true, true));
		assertEquals(DARKNESS, decision(true, true, false, false, false, false, true, true, true));
		assertEquals(WATER, decision(true, true, false, false, false, false, false, true, true));
		assertEquals(PURE_LIGHT, decision(true, true, false, false, false, false, false, false, true));
		assertEquals(STRIKE, decision(true, true, false, false, false, false, false, false, false));
	}

	@Test
	void damageFalloffAndTargetCapsAreBounded() {
		assertEquals(0.82, StarfallRules.damageMultiplier(0, false, false, STRIKE), 0.0001);
		assertEquals(1.2 * 1.135, StarfallRules.damageMultiplier(99, false, true, STRIKE), 0.0001);
		assertEquals(1.75, StarfallRules.damageMultiplier(0, true, false, STRIKE), 0.0001);
		assertEquals(0.7 * 0.82, StarfallRules.damageMultiplier(0, false, false, WATER), 0.0001);
		assertEquals(1.15 * 0.82, StarfallRules.damageMultiplier(0, false, false, PURE_LIGHT), 0.0001);
		assertEquals(1.0, StarfallRules.falloff(0.0, 4.0), 0.0001);
		assertEquals(0.5125, StarfallRules.falloff(2.0, 4.0), 0.0001);
		assertEquals(0.0, StarfallRules.falloff(4.0, 4.0), 0.0001);
		assertEquals(12, StarfallRules.targetLimit(false));
		assertEquals(18, StarfallRules.targetLimit(true));
	}

	@Test
	void repeatHitsEchoesAndImpactRadiiHaveHardCaps() {
		assertTrue(StarfallRules.hitAllowed(100L, Long.MIN_VALUE, 0, false));
		assertFalse(StarfallRules.hitAllowed(110L, 100L, 1, false));
		assertTrue(StarfallRules.hitAllowed(112L, 100L, 1, false));
		assertFalse(StarfallRules.hitAllowed(200L, 100L, 3, false));
		assertTrue(StarfallRules.hitAllowed(200L, 100L, 3, true));
		assertTrue(StarfallRules.echoAllowed(true, 2));
		assertFalse(StarfallRules.echoAllowed(true, 1));
		assertFalse(StarfallRules.echoAllowed(false, 2));
		assertEquals(2.6, StarfallRules.impactRadius(false, false, STRIKE), 0.0001);
		assertEquals(3.35, StarfallRules.impactRadius(true, false, STRIKE), 0.0001);
		assertEquals(5.0, StarfallRules.impactRadius(false, true, STRIKE), 0.0001);
		assertEquals(3.9, StarfallRules.impactRadius(false, false, WATER), 0.0001);
	}

	@Test
	void motionTrackingIsLeashedAndStepCapped() {
		Vec3 current = Vec3.ZERO;
		Vec3 origin = Vec3.ZERO;
		Vec3 desired = new Vec3(6.0, 0.0, 0.0);
		assertEquals(new Vec3(1.25, 0.0, 0.0), StarfallRules.trackedCenter(
				current, desired, origin, true, 16.0, 1.25));
		assertEquals(current, StarfallRules.trackedCenter(
				current, desired, origin, false, 16.0, 1.25));
		assertEquals(current, StarfallRules.trackedCenter(
				current, new Vec3(17.0, 0.0, 0.0), origin, true, 16.0, 1.25));
		assertEquals(current, StarfallRules.trackedCenter(
				current, new Vec3(Double.NaN, 0.0, 0.0), origin, true, 16.0, 1.25));
	}

	@Test
	void shockPressureAndLifecycleRejectUnsafeInputs() {
		assertEquals(new Vec3(0.6, 0.3, 0.8), StarfallRules.pressureImpulse(
				Vec3.ZERO, new Vec3(3.0, 20.0, 4.0), 1.0, 0.3));
		assertEquals(new Vec3(0.0, 0.3, 0.0), StarfallRules.pressureImpulse(
				Vec3.ZERO, Vec3.ZERO, 1.0, 0.3));
		assertEquals(Vec3.ZERO, StarfallRules.pressureImpulse(
				Vec3.ZERO, Vec3.ZERO, -1.0, 0.3));

		assertTrue(StarfallRules.stormContinues(
				true, true, true, false, false, true, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				false, true, true, false, false, true, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				true, false, true, false, false, true, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				true, true, false, false, false, true, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				true, true, true, true, false, true, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				true, true, true, false, true, true, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				true, true, true, false, false, false, 77L, 78L));
		assertFalse(StarfallRules.stormContinues(
				true, true, true, false, false, true, 78L, 78L));
	}

	private static StarfallRules.Counterplay decision(boolean owned, boolean loaded,
			boolean safeZone, boolean amethyst, boolean sanctuary, boolean kineticWard,
			boolean darkness, boolean water, boolean pureLight) {
		return StarfallRules.impactDecision(owned, loaded, safeZone, amethyst,
				sanctuary, kineticWard, darkness, water, pureLight);
	}
}
