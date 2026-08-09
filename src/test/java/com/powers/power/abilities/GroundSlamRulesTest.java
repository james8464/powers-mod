package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.powers.power.abilities.GroundSlamRules.Beat.CROWN;
import static com.powers.power.abilities.GroundSlamRules.Beat.PRIMARY;
import static com.powers.power.abilities.GroundSlamRules.Beat.SOUL_ECHO;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.AMETHYST;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.COLLISION;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.DARKNESS;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.IMPACT;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.PURE_LIGHT;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.SAFE_ZONE;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.UNLOADED;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.UNSUPPORTED;
import static com.powers.power.abilities.GroundSlamRules.Counterplay.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards finite Faultbound Verdict timing, geometry, rank, and counterplay. */
class GroundSlamRulesTest {
	@Test
	void authoredBeatScheduleAndExpiryRemainFinite() {
		assertEquals(12, GroundSlamRules.beatAge(PRIMARY));
		assertEquals(18, GroundSlamRules.beatAge(SOUL_ECHO));
		assertEquals(24, GroundSlamRules.beatAge(CROWN));
		assertEquals(0, GroundSlamRules.beatsDue(11, true, true));
		assertEquals(1, GroundSlamRules.beatsDue(12, true, true));
		assertEquals(2, GroundSlamRules.beatsDue(18, true, true));
		assertEquals(3, GroundSlamRules.beatsDue(24, true, true));
		assertEquals(1, GroundSlamRules.beatsDue(100, false, false));
		assertEquals(18L, GroundSlamRules.finishAge(false, false));
		assertEquals(24L, GroundSlamRules.finishAge(true, false));
		assertEquals(30L, GroundSlamRules.finishAge(false, true));
	}

	@Test
	void ranksExpandBodyAndTerrainBudgetsUnderHardCaps() {
		assertEquals(12, GroundSlamRules.targetLimit(false, false));
		assertEquals(18, GroundSlamRules.targetLimit(true, false));
		assertEquals(18, GroundSlamRules.targetLimit(false, true));
		assertEquals(24, GroundSlamRules.targetLimit(true, true));
		assertEquals(0, GroundSlamRules.terrainLimit(false, true, true));
		assertEquals(0, GroundSlamRules.terrainLimit(true, false, true));
		assertEquals(8, GroundSlamRules.terrainLimit(true, true, false));
		assertEquals(16, GroundSlamRules.terrainLimit(true, true, true));
		assertEquals(0, GroundSlamRules.afterimageTargetLimit(false));
		assertEquals(8, GroundSlamRules.afterimageTargetLimit(true));
	}

	@Test
	void environmentCounterplayUsesStableProtectionFirstPriority() {
		assertEquals(UNLOADED, environment(false, true, true, true, true, true, true));
		assertEquals(SAFE_ZONE, environment(true, true, true, true, true, true, true));
		assertEquals(AMETHYST, environment(true, false, true, true, true, true, true));
		assertEquals(DARKNESS, environment(true, false, false, true, true, true, true));
		assertEquals(WATER, environment(true, false, false, false, true, true, true));
		assertEquals(PURE_LIGHT, environment(true, false, false, false, false, true, true));
		assertEquals(UNSUPPORTED, environment(true, false, false, false, false, false, false));
		assertEquals(IMPACT, environment(true, false, false, false, false, false, true));
	}

	@Test
	void damageRadiusFalloffAndMediumsAreExplicit() {
		assertEquals(5.0, GroundSlamRules.impactRadius(5.0, PRIMARY, false), 0.0001);
		assertEquals(5.75, GroundSlamRules.impactRadius(5.0, PRIMARY, true), 0.0001);
		assertEquals(3.6, GroundSlamRules.impactRadius(5.0, SOUL_ECHO, false), 0.0001);
		assertEquals(5.9, GroundSlamRules.impactRadius(5.0, CROWN, false), 0.0001);
		assertEquals(1.0, GroundSlamRules.damageMultiplier(PRIMARY, false, true, IMPACT), 0.0001);
		assertEquals(0.42, GroundSlamRules.damageMultiplier(SOUL_ECHO, false, true, IMPACT), 0.0001);
		assertEquals(0.8, GroundSlamRules.damageMultiplier(CROWN, false, true, IMPACT), 0.0001);
		assertEquals(0.35, GroundSlamRules.damageMultiplier(PRIMARY, false, false, IMPACT), 0.0001);
		assertEquals(0.65, GroundSlamRules.damageMultiplier(PRIMARY, false, true, WATER), 0.0001);
		assertEquals(0.75, GroundSlamRules.damageMultiplier(PRIMARY, false, true, DARKNESS), 0.0001);
		assertEquals(1.10, GroundSlamRules.damageMultiplier(PRIMARY, false, true, PURE_LIGHT), 0.0001);
		assertEquals(WATER, GroundSlamRules.targetMedium(IMPACT, true));
		assertEquals(DARKNESS, GroundSlamRules.targetMedium(DARKNESS, true));
		assertEquals(IMPACT, GroundSlamRules.targetMedium(IMPACT, false));
		assertEquals(1.0, GroundSlamRules.falloff(0.0, 5.0), 0.0001);
		assertEquals(0.475, GroundSlamRules.falloff(2.5, 5.0), 0.0001);
		assertEquals(0.0, GroundSlamRules.falloff(5.0, 5.0), 0.0001);
	}

	@Test
	void pressureCountersResolveBeforeVelocityWrites() {
		assertEquals(SAFE_ZONE, GroundSlamRules.bodyDecision(
				false, true, true, true, true));
		assertEquals(AMETHYST, GroundSlamRules.bodyDecision(
				true, true, true, true, true));
		assertEquals(GroundSlamRules.Counterplay.SANCTUARY, GroundSlamRules.bodyDecision(
				true, false, true, true, true));
		assertEquals(GroundSlamRules.Counterplay.KINETIC_WARD, GroundSlamRules.bodyDecision(
				true, false, false, true, true));
		assertEquals(GroundSlamRules.Counterplay.FORCEFIELD, GroundSlamRules.bodyDecision(
				true, false, false, false, true));
		assertEquals(IMPACT, GroundSlamRules.bodyDecision(
				true, false, false, false, false));
		assertEquals(GroundSlamRules.Counterplay.PROTECTED, GroundSlamRules.pressureDecision(
				false, true, true, true, true, true, false));
		assertEquals(AMETHYST, GroundSlamRules.pressureDecision(
				true, true, true, true, true, true, false));
		assertEquals(GroundSlamRules.Counterplay.BODY_ANCHOR, GroundSlamRules.pressureDecision(
				true, false, true, true, true, true, false));
		assertEquals(GroundSlamRules.Counterplay.FORCEFIELD, GroundSlamRules.pressureDecision(
				true, false, false, true, true, true, false));
		assertEquals(GroundSlamRules.Counterplay.KINETIC_WARD, GroundSlamRules.pressureDecision(
				true, false, false, false, true, true, false));
		assertEquals(GroundSlamRules.Counterplay.TIME_LOCK, GroundSlamRules.pressureDecision(
				true, false, false, false, false, true, false));
		assertEquals(COLLISION, GroundSlamRules.pressureDecision(
				true, false, false, false, false, false, false));
		assertEquals(IMPACT, GroundSlamRules.pressureDecision(
				true, false, false, false, false, false, true));
	}

	@Test
	void trackingPressureAndTerrainSamplesRejectMalformedGeometry() {
		assertEquals(new Vec3(0.75, 0.0, 0.0), GroundSlamRules.trackedCenter(
				Vec3.ZERO, new Vec3(3.0, 0.0, 0.0), Vec3.ZERO, true, 6.0, 0.75));
		assertEquals(Vec3.ZERO, GroundSlamRules.trackedCenter(
				Vec3.ZERO, new Vec3(7.0, 0.0, 0.0), Vec3.ZERO, true, 6.0, 0.75));
		assertEquals(Vec3.ZERO, GroundSlamRules.trackedCenter(
				Vec3.ZERO, new Vec3(3.0, 0.0, 0.0), Vec3.ZERO, false, 6.0, 0.75));
		assertEquals(new Vec3(0.6, 0.28, 0.8), GroundSlamRules.pressureImpulse(
				Vec3.ZERO, new Vec3(3.0, 0.0, 4.0), 1.0, 0.28));
		assertEquals(Vec3.ZERO, GroundSlamRules.pressureImpulse(
				Vec3.ZERO, Vec3.ZERO, -1.0, 0.28));
		assertEquals(new Vec3(2.75, 0.0, 0.0), GroundSlamRules.echoCenter(
				Vec3.ZERO, new Vec3(8.0, 3.0, 0.0), 5.0));
		assertEquals(Vec3.ZERO, GroundSlamRules.echoCenter(
				new Vec3(Double.NaN, 0.0, 0.0), Vec3.ZERO, 5.0));
		assertEquals(1.2, GroundSlamRules.pressureMultiplier(
				PRIMARY, true, true, IMPACT), 0.0001);
		assertEquals(0.5, GroundSlamRules.pressureMultiplier(
				PRIMARY, false, true, WATER), 0.0001);
		Set<Vec3> offsets = new HashSet<>();
		for (int index = 0; index < 16; index++) {
			Vec3 offset = GroundSlamRules.terrainOffset(index, 16, 5.0);
			assertEquals(offset, GroundSlamRules.terrainOffset(index, 16, 5.0));
			assertTrue(offset.horizontalDistance() <= 5.000001);
			offsets.add(offset);
		}
		assertEquals(16, offsets.size());
		assertNotEquals(GroundSlamRules.terrainOffset(0, 16, 5.0), Vec3.ZERO);
	}

	@Test
	void lifecycleRequiresEveryOwnerInvariantUntilExclusiveExpiry() {
		assertTrue(GroundSlamRules.riteContinues(
				true, true, true, false, false, true, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				false, true, true, false, false, true, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				true, false, true, false, false, true, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				true, true, false, false, false, true, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				true, true, true, true, false, true, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				true, true, true, false, true, true, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				true, true, true, false, false, false, 29L, 30L));
		assertFalse(GroundSlamRules.riteContinues(
				true, true, true, false, false, true, 30L, 30L));
	}

	private static GroundSlamRules.Counterplay environment(boolean loaded,
			boolean safeZone, boolean amethyst, boolean darkness,
			boolean water, boolean pureLight, boolean supported) {
		return GroundSlamRules.environmentDecision(loaded, safeZone, amethyst,
				darkness, water, pureLight, supported);
	}
}
