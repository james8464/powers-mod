package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards Cinderheart charge, lifetime, reflection, impact, and counter rules. */
class FireballRulesTest {
	@Test
	void deliberateReleaseKeepsExactOwnershipAndStartsBoundedFlight() {
		UUID owner = UUID.fromString("2d018d48-4c22-4932-a236-625005e4710c");
		FireballRules.ReleaseState release = FireballRules.deliberateRelease(
				owner, new Vec3(0.0, 0.0, 4.0));

		assertTrue(release.launched());
		assertEquals(owner, release.originalOwner());
		assertEquals(owner, release.controller());
		assertEquals(0, release.reflections());
		assertEquals(new Vec3(0.0, 0.0, 1.5), release.velocity());
		assertFalse(FireballRules.deliberateRelease(owner, Vec3.ZERO).launched());
	}

	@Test
	void chargeTiersAreFiniteAndAncientMasteryUnlocksTheFourthSeal() {
		assertEquals(3, FireballRules.maximumTier(false));
		assertEquals(4, FireballRules.maximumTier(true));
		assertEquals(1, FireballRules.nextTier(0, false));
		assertEquals(2, FireballRules.nextTier(1, false));
		assertEquals(3, FireballRules.nextTier(3, false));
		assertEquals(4, FireballRules.nextTier(3, true));
		assertEquals(4, FireballRules.nextTier(99, true));
		assertTrue(FireballRules.canCharge(2, false));
		assertFalse(FireballRules.canCharge(3, false));
	}

	@Test
	void hoverAndLaunchLifetimesUseExclusiveBoundaries() {
		assertEquals(380L, FireballRules.extendedHoverExpiry(100L, 340L, 140L));
		assertEquals(380L, FireballRules.extendedHoverExpiry(100L, 340L, 330L));
		assertEquals(460L, FireballRules.extendedHoverExpiry(100L, 450L, 430L));
		assertEquals(620L, FireballRules.launchExpiry(500L));
		assertEquals(1, FireballRules.remainingTicks(100L, 260L, 259L));
		assertEquals(0, FireballRules.remainingTicks(100L, 260L, 260L));
		assertEquals(0, FireballRules.remainingTicks(100L, 90L, 80L));
	}

	@Test
	void reflectionsIncludeRankBonusesAndNeverBecomeInfinite() {
		assertEquals(2, FireballRules.reflectionLimit(false, false));
		assertEquals(3, FireballRules.reflectionLimit(true, false));
		assertEquals(3, FireballRules.reflectionLimit(false, true));
		assertEquals(4, FireballRules.reflectionLimit(true, true));
		assertTrue(FireballRules.reflectionAllowed(false, 99, 0));
		assertTrue(FireballRules.reflectionAllowed(true, 1, 2));
		assertFalse(FireballRules.reflectionAllowed(true, 2, 2));
		assertFalse(FireballRules.reflectionAllowed(true, -1, 2));
	}

	@Test
	void tierAndMightScaleImpactWithoutUnboundedValues() {
		assertEquals(2.0, FireballRules.impactRadius(1, false), 0.0001);
		assertEquals(3.5, FireballRules.impactRadius(3, false), 0.0001);
		assertEquals(4.9, FireballRules.impactRadius(4, true), 0.0001);
		assertEquals(1.0, FireballRules.damageMultiplier(1, false), 0.0001);
		assertEquals(1.65, FireballRules.damageMultiplier(3, false), 0.0001);
		assertEquals(2.3575, FireballRules.damageMultiplier(4, true), 0.0001);
		assertEquals(3, FireballRules.burnSeconds(1));
		assertEquals(9, FireballRules.burnSeconds(4));
		assertEquals(12, FireballRules.targetLimit(false));
		assertEquals(16, FireballRules.targetLimit(true));
		assertEquals(0, FireballRules.terrainScorchLimit(4, false));
		assertEquals(8, FireballRules.terrainScorchLimit(4, true));
	}

	@Test
	void distanceFalloffAndTrailSamplingRemainFinite() {
		assertEquals(1.0, FireballRules.falloff(0.0, 4.0), 0.0001);
		assertEquals(0.675, FireballRules.falloff(2.0, 4.0), 0.0001);
		assertEquals(0.35, FireballRules.falloff(4.0, 4.0), 0.0001);
		assertEquals(0.0, FireballRules.falloff(4.1, 4.0), 0.0001);
		assertEquals(0.0, FireballRules.falloff(Double.NaN, 4.0), 0.0001);
		assertFalse(FireballRules.trailAllowed(0.0, 12.0));
		assertTrue(FireballRules.trailAllowed(9.0, 12.0));
		assertFalse(FireballRules.trailAllowed(145.0, 12.0));
		assertEquals(6, FireballRules.trailSegments(3.0));
		assertEquals(24, FireballRules.trailSegments(100.0));
		assertEquals(new Vec3(0.6, 0.2, 0.8), FireballRules.pressureImpulse(
				Vec3.ZERO, new Vec3(3.0, 4.0, 4.0), 1.0, 0.2));
		assertEquals(Vec3.ZERO, FireballRules.pressureImpulse(
				Vec3.ZERO, Vec3.ZERO, 1.0, 0.2));
	}

	@Test
	void semanticImpactTerminalsUseAStablePriority() {
		assertEquals(FireballRules.ImpactDecision.DETONATE,
				FireballRules.impactDecision(true, false, false, false,
						false, false, false, false));
		assertEquals(FireballRules.ImpactDecision.UNOWNED,
				FireballRules.impactDecision(false, true, true, true,
						true, true, true, true));
		assertEquals(FireballRules.ImpactDecision.SAFE_ZONE,
				FireballRules.impactDecision(true, true, true, true,
						true, true, true, true));
		assertEquals(FireballRules.ImpactDecision.AMETHYST,
				FireballRules.impactDecision(true, false, true, true,
						true, true, true, true));
		assertEquals(FireballRules.ImpactDecision.SANCTUARY,
				FireballRules.impactDecision(true, false, false, true,
						true, true, true, true));
		assertEquals(FireballRules.ImpactDecision.KINETIC_WARD,
				FireballRules.impactDecision(true, false, false, false,
						true, true, true, true));
		assertEquals(FireballRules.ImpactDecision.FORCEFIELD,
				FireballRules.impactDecision(true, false, false, false,
						false, true, true, true));
		assertEquals(FireballRules.ImpactDecision.WATER,
				FireballRules.impactDecision(true, false, false, false,
						false, false, true, true));
		assertEquals(FireballRules.ImpactDecision.FROST,
				FireballRules.impactDecision(true, false, false, false,
						false, false, false, true));
	}

	@Test
	void lifecycleRequiresEveryOwnerInvariant() {
		assertTrue(FireballRules.continues(
				true, true, true, false, false, true, 259L, 260L));
		assertFalse(FireballRules.continues(
				false, true, true, false, false, true, 259L, 260L));
		assertFalse(FireballRules.continues(
				true, false, true, false, false, true, 259L, 260L));
		assertFalse(FireballRules.continues(
				true, true, false, false, false, true, 259L, 260L));
		assertFalse(FireballRules.continues(
				true, true, true, true, false, true, 259L, 260L));
		assertFalse(FireballRules.continues(
				true, true, true, false, true, true, 259L, 260L));
		assertFalse(FireballRules.continues(
				true, true, true, false, false, false, 259L, 260L));
		assertFalse(FireballRules.continues(
				true, true, true, false, false, true, 260L, 260L));
	}
}
