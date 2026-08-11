package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static com.powers.power.abilities.LightningStrikeRules.Beat.CROWN;
import static com.powers.power.abilities.LightningStrikeRules.Beat.PRIMARY;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.AMETHYST;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.DARKNESS;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.PURE_LIGHT;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.ROOF;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.SAFE_ZONE;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.STRIKE;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.UNLOADED;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.UNOWNED;
import static com.powers.power.abilities.LightningStrikeRules.Counterplay.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards Storm Tribunal timing, bounds, rank mechanics, and counter priority. */
class LightningStrikeRulesTest {
	@Test
	void authoredScheduleWarnsBeforeEitherFiniteVerdict() {
		assertEquals(0, new LightningStrikeAbility().cooldownTicks());
		assertEquals(8, LightningStrikeRules.beatAge(PRIMARY));
		assertEquals(12, LightningStrikeRules.beatAge(CROWN));
		assertEquals(0, LightningStrikeRules.beatsDue(7, true));
		assertEquals(1, LightningStrikeRules.beatsDue(8, true));
		assertEquals(2, LightningStrikeRules.beatsDue(12, true));
		assertEquals(1, LightningStrikeRules.beatsDue(99, false));
		assertEquals(13L, LightningStrikeRules.finishAge(false));
		assertEquals(18L, LightningStrikeRules.finishAge(true));
	}

	@Test
	void allWorkBudgetsRemainHardCapped() {
		assertEquals(8, LightningStrikeRules.targetLimit(false, false));
		assertEquals(12, LightningStrikeRules.targetLimit(true, false));
		assertEquals(12, LightningStrikeRules.targetLimit(false, true));
		assertEquals(16, LightningStrikeRules.targetLimit(true, true));
		assertEquals(3, LightningStrikeRules.chainLimit(false, false));
		assertEquals(4, LightningStrikeRules.chainLimit(true, false));
		assertEquals(5, LightningStrikeRules.chainLimit(true, true));
		assertEquals(0, LightningStrikeRules.projectileLimit(false));
		assertEquals(8, LightningStrikeRules.projectileLimit(true));
		assertEquals(0, LightningStrikeRules.afterimageTargetLimit(false));
		assertEquals(6, LightningStrikeRules.afterimageTargetLimit(true));
		assertEquals(64, LightningStrikeRules.directCandidateLimit());
		assertEquals(32, LightningStrikeRules.chainCandidateLimit());
		assertEquals(32, LightningStrikeRules.rankCandidateLimit());
	}

	@Test
	void conductorClassificationIsExplicitAndRetainsTheNodeCap() {
		assertEquals(LightningStrikeRules.Conductance.NONE,
				LightningStrikeRules.conductance(false, false, false, false));
		assertEquals(LightningStrikeRules.Conductance.ARMOUR,
				LightningStrikeRules.conductance(false, false, false, true));
		assertEquals(LightningStrikeRules.Conductance.BLOCK,
				LightningStrikeRules.conductance(false, false, true, true));
		assertEquals(LightningStrikeRules.Conductance.WATER,
				LightningStrikeRules.conductance(true, false, true, true));
		assertEquals(LightningStrikeRules.Conductance.LIGHTNING_ROD,
				LightningStrikeRules.conductance(true, true, true, true));
		assertEquals(5, LightningStrikeRules.chainLimit(true, true));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.NONE, true, false, 2.0, 6.0, STRIKE));
		assertTrue(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.BLOCK, true, false, 2.0, 6.0, STRIKE));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.LIGHTNING_ROD,
				true, false, 2.0, 6.0, STRIKE));
	}

	@Test
	void conductorDamageMetadataIsMediumSpecificAndBounded() {
		assertEquals(0.62, LightningStrikeRules.chainDamageMultiplier(
				0, LightningStrikeRules.Conductance.WATER), 0.0001);
		assertEquals(0.558, LightningStrikeRules.chainDamageMultiplier(
				0, LightningStrikeRules.Conductance.BLOCK), 0.0001);
		assertEquals(0.496, LightningStrikeRules.chainDamageMultiplier(
				0, LightningStrikeRules.Conductance.ARMOUR), 0.0001);
		assertEquals(0.0, LightningStrikeRules.chainDamageMultiplier(
				0, LightningStrikeRules.Conductance.NONE), 0.0001);
		assertEquals(0.0, LightningStrikeRules.chainDamageMultiplier(
				0, LightningStrikeRules.Conductance.LIGHTNING_ROD), 0.0001);
	}

	@Test
	void environmentUsesProtectionFirstPriorityAndExplicitTransformations() {
		assertEquals(UNOWNED, environment(false, false, true, true, true,
				true, true, true, true, true));
		assertEquals(UNLOADED, environment(true, false, true, true, true,
				true, true, true, true, true));
		assertEquals(SAFE_ZONE, environment(true, true, true, true, true,
				true, true, true, true, true));
		assertEquals(AMETHYST, environment(true, true, false, true, true,
				true, true, true, true, true));
		assertEquals(LightningStrikeRules.Counterplay.SANCTUARY,
				environment(true, true, false, false, true,
						true, true, true, true, true));
		assertEquals(LightningStrikeRules.Counterplay.KINETIC_WARD,
				environment(true, true, false, false, false,
						true, true, true, true, true));
		assertEquals(DARKNESS, environment(true, true, false, false, false,
				false, true, true, true, true));
		assertEquals(WATER, environment(true, true, false, false, false,
				false, false, true, true, true));
		assertEquals(PURE_LIGHT, environment(true, true, false, false, false,
				false, false, false, true, true));
		assertEquals(ROOF, environment(true, true, false, false, false,
				false, false, false, false, true));
		assertEquals(STRIKE, environment(true, true, false, false, false,
				false, false, false, false, false));
		assertTrue(LightningStrikeRules.impactAllowed(ROOF));
		assertFalse(LightningStrikeRules.impactAllowed(DARKNESS));
		assertFalse(LightningStrikeRules.impactAllowed(
				LightningStrikeRules.Counterplay.OBSTRUCTED));
		assertTrue(LightningStrikeRules.roofCatch(92, 60));
		assertFalse(LightningStrikeRules.roofCatch(60, 60));
		assertFalse(LightningStrikeRules.roofCatch(44, 60));
	}

	@Test
	void everyBodyProtectionPrecedesDamageAndSecondaryEffects() {
		assertEquals(SAFE_ZONE, body(false, true, true, true, true));
		assertEquals(AMETHYST, body(true, true, true, true, true));
		assertEquals(LightningStrikeRules.Counterplay.SANCTUARY,
				body(true, false, true, true, true));
		assertEquals(LightningStrikeRules.Counterplay.KINETIC_WARD,
				body(true, false, false, true, true));
		assertEquals(LightningStrikeRules.Counterplay.FORCEFIELD,
				body(true, false, false, false, true));
		assertEquals(STRIKE, body(true, false, false, false, false));
		assertEquals(LightningStrikeRules.Counterplay.BODY_ANCHOR,
				LightningStrikeRules.secondaryDecision(true, true, false));
		assertEquals(LightningStrikeRules.Counterplay.TIME_LOCK,
				LightningStrikeRules.secondaryDecision(true, false, true));
		assertEquals(STRIKE,
				LightningStrikeRules.secondaryDecision(true, false, false));
	}

	@Test
	void damageRadiusFalloffAndConductionAreFiniteAndRankAware() {
		assertEquals(2.75, LightningStrikeRules.impactRadius(
				2.75, PRIMARY, false, STRIKE), 0.0001);
		assertEquals(3.245, LightningStrikeRules.impactRadius(
				2.75, PRIMARY, true, STRIKE), 0.0001);
		assertEquals(3.9875, LightningStrikeRules.impactRadius(
				2.75, PRIMARY, false, WATER), 0.0001);
		assertEquals(3.4375, LightningStrikeRules.impactRadius(
				2.75, CROWN, false, STRIKE), 0.0001);
		assertEquals(1.0, LightningStrikeRules.damageMultiplier(
				PRIMARY, false, STRIKE), 0.0001);
		assertEquals(1.2, LightningStrikeRules.damageMultiplier(
				PRIMARY, true, STRIKE), 0.0001);
		assertEquals(0.72, LightningStrikeRules.damageMultiplier(
				PRIMARY, false, WATER), 0.0001);
		assertEquals(1.15, LightningStrikeRules.damageMultiplier(
				PRIMARY, false, PURE_LIGHT), 0.0001);
		assertEquals(0.85, LightningStrikeRules.damageMultiplier(
				PRIMARY, false, ROOF), 0.0001);
		assertEquals(0.55, LightningStrikeRules.damageMultiplier(
				CROWN, false, STRIKE), 0.0001);
		assertEquals(1.0, LightningStrikeRules.falloff(0.0, 4.0), 0.0001);
		assertEquals(0.55, LightningStrikeRules.falloff(2.0, 4.0), 0.0001);
		assertEquals(0.0, LightningStrikeRules.falloff(4.0, 4.0), 0.0001);
		assertEquals(0.62, LightningStrikeRules.chainDamageMultiplier(0), 0.0001);
		assertEquals(0.46, LightningStrikeRules.chainDamageMultiplier(1), 0.0001);
		assertEquals(0.34, LightningStrikeRules.chainDamageMultiplier(2), 0.0001);
		assertEquals(0.0, LightningStrikeRules.chainDamageMultiplier(-1), 0.0001);
		assertEquals(0.38, LightningStrikeRules.forkDamageMultiplier(), 0.0001);
		assertTrue(LightningStrikeRules.forkAllowed(true, 1));
		assertFalse(LightningStrikeRules.forkAllowed(true, 0));
		assertFalse(LightningStrikeRules.forkAllowed(false, 1));
	}

	@Test
	void chainEligibilityCannotBypassWetLoadedUniqueProtectedNodes() {
		assertTrue(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.WATER, true, false, 5.9, 6.0, STRIKE));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.NONE, true, false, 5.9, 6.0, STRIKE));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.WATER, false, false, 5.9, 6.0, STRIKE));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.WATER, true, true, 5.9, 6.0, STRIKE));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.WATER, true, false, 6.1, 6.0, STRIKE));
		assertFalse(LightningStrikeRules.chainEligible(
				LightningStrikeRules.Conductance.WATER, true, false, 5.9, 6.0, AMETHYST));
		assertEquals(6.0, LightningStrikeRules.chainRange(false), 0.0001);
		assertEquals(7.5, LightningStrikeRules.chainRange(true), 0.0001);
	}

	@Test
	void trackingAndProjectileGroundingRejectMalformedGeometry() {
		assertEquals(new Vec3(1.0, 0.0, 0.0), LightningStrikeRules.trackedCenter(
				Vec3.ZERO, new Vec3(4.0, 0.0, 0.0), Vec3.ZERO, true, 10.0, 1.0));
		assertEquals(Vec3.ZERO, LightningStrikeRules.trackedCenter(
				Vec3.ZERO, new Vec3(11.0, 0.0, 0.0), Vec3.ZERO, true, 10.0, 1.0));
		assertEquals(Vec3.ZERO, LightningStrikeRules.trackedCenter(
				Vec3.ZERO, new Vec3(4.0, 0.0, 0.0), Vec3.ZERO, false, 10.0, 1.0));
		assertEquals(new Vec3(0.35, -0.5, -0.35),
				LightningStrikeRules.groundedProjectileVelocity(
						new Vec3(1.0, 0.5, -1.0), 0.5));
		assertEquals(Vec3.ZERO, LightningStrikeRules.groundedProjectileVelocity(
				new Vec3(Double.NaN, 0.0, 0.0), 0.5));
		Vec3 capped = LightningStrikeRules.groundedProjectileVelocity(
				new Vec3(Double.MAX_VALUE, 1.0, Double.MAX_VALUE), 99.0);
		assertTrue(capped.horizontalDistance() <= 1.500001);
		assertEquals(-1.5, capped.y, 0.0001);
	}

	@Test
	void lifecycleRequiresEveryOwnerInvariantUntilExclusiveExpiry() {
		assertTrue(LightningStrikeRules.tribunalContinues(
				true, true, true, false, false, true, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				false, true, true, false, false, true, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, false, true, false, false, true, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, true, false, false, false, true, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, true, true, true, false, true, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, true, true, false, true, true, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, true, true, false, false, false, true, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, true, true, false, false, true, false, 17L, 18L));
		assertFalse(LightningStrikeRules.tribunalContinues(
				true, true, true, false, false, true, true, 18L, 18L));
	}

	private static LightningStrikeRules.Counterplay environment(boolean owned,
			boolean loaded, boolean safeZone, boolean amethyst, boolean sanctuary,
			boolean kineticWard, boolean darkness, boolean water,
			boolean pureLight, boolean roof) {
		return LightningStrikeRules.environmentDecision(owned, loaded, safeZone,
				amethyst, sanctuary, kineticWard, darkness, water, pureLight, roof);
	}

	private static LightningStrikeRules.Counterplay body(boolean harmAllowed,
			boolean amethyst, boolean sanctuary, boolean kineticWard,
			boolean forcefield) {
		return LightningStrikeRules.bodyDecision(harmAllowed, amethyst,
				sanctuary, kineticWard, forcefield);
	}
}
