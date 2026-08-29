package com.powers.spell;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialRuinRulesTest {
	@Test
	void ritualCountsDownForExactlyOneMinute() {
		assertEquals(1_200, CelestialRuinRules.COUNTDOWN_TICKS);
		assertEquals(50, CelestialRuinRules.BEAM_RADIUS);
	}

	@Test
	void chunkTicketsAreDeferredAndGrowOnlyDuringTheFinalFiveSeconds() {
		assertEquals(-1, CelestialRuinTicketRules.radiusForCountdown(1_200, false, 9));
		assertEquals(-1, CelestialRuinTicketRules.radiusForCountdown(101, false, 9));
		assertEquals(1, CelestialRuinTicketRules.radiusForCountdown(100, false, 9));
		assertEquals(5, CelestialRuinTicketRules.radiusForCountdown(50, false, 9));
		assertEquals(9, CelestialRuinTicketRules.radiusForCountdown(0, false, 9));
		assertEquals(9, CelestialRuinTicketRules.radiusForCountdown(900, true, 9));
	}

	@Test
	void detonationIsAtLeastTwentyTimesTheLivingForcePeakDamage() {
		assertTrue(CelestialRuinRules.PEAK_DAMAGE >= 20_000.0f);
		assertEquals(6_000, CelestialRuinRules.DAMAGE_RADIUS);
		assertTrue(CelestialRuinRules.damage(0.0) >= 20_000.0f);
		assertTrue(CelestialRuinRules.damage(1_000.0) >= 100.0f);
		assertTrue(CelestialRuinRules.damage(5_999.0) > 0.0f);
		assertEquals(0.0f, CelestialRuinRules.damage(6_000.0), 0.001f);
	}

	@Test
	void damageAndKnockbackFollowTheExactAuthoredFalloff() {
		assertEquals(50_000.0f, CelestialRuinRules.damage(0.0), 0.001f);
		assertEquals(40_500.0f, CelestialRuinRules.damage(600.0), 0.01f);
		assertEquals(12_500.0f, CelestialRuinRules.damage(3_000.0), 0.01f);
		assertEquals(500.0f, CelestialRuinRules.damage(5_400.0), 0.01f);
		assertEquals(18.0, CelestialRuinRules.knockback(0.0), 0.000001);
		assertEquals(18.0 * Math.pow(0.5, 0.65), CelestialRuinRules.knockback(3_000.0), 0.000001);
		assertEquals(0.0f, CelestialRuinRules.damage(-1.0), 0.0f);
		assertEquals(0.0f, CelestialRuinRules.damage(Double.NaN), 0.0f);
		assertEquals(0.0, CelestialRuinRules.knockback(-1.0), 0.0);
		assertEquals(0.0, CelestialRuinRules.knockback(Double.POSITIVE_INFINITY), 0.0);
	}

	@Test
	void damageQueryCoversTheDimensionsEntireBuildHeight() {
		AABB bounds = CelestialRuinRules.damageBounds(new Vec3(12.5, 64.0, -8.5), -64, 320);
		assertEquals(-64.0, bounds.minY, 0.001);
		assertEquals(320.0, bounds.maxY, 0.001);
		assertEquals(12.5 - CelestialRuinRules.DAMAGE_RADIUS, bounds.minX, 0.001);
		assertEquals(-8.5 + CelestialRuinRules.DAMAGE_RADIUS, bounds.maxZ, 0.001);
	}

	@Test
	void shockwaveThrowsBodiesAcrossTheFullDamageRadius() {
		assertTrue(CelestialRuinRules.knockback(0.0) >= 10.0);
		assertTrue(CelestialRuinRules.knockback(5_999.0) > 0.0);
		assertEquals(0.0, CelestialRuinRules.knockback(6_000.0), 0.0001);
	}

	@Test
	void destructionSphereHasAHardBoundary() {
		assertTrue(CelestialRuinRules.insideBlast(120, 0, 0));
		assertFalse(CelestialRuinRules.insideBlast(121, 0, 0));
		assertTrue(CelestialRuinRules.insideBlast(0, -120, 0));
		assertTrue(CelestialRuinRules.insideBlast(0, 0, 120));
		assertFalse(CelestialRuinRules.insideBlast(85, 85, 0));
	}

	@Test
	void radialAftershockCreatesThousandsOfBlocksOfBoundedStreaks() {
		assertTrue(CelestialRuinRules.aftershockTotalSteps() >= 40_000);
		CelestialRuinRules.AftershockOffset first = CelestialRuinRules.aftershockOffset(0);
		CelestialRuinRules.AftershockOffset last = CelestialRuinRules.aftershockOffset(
				CelestialRuinRules.aftershockTotalSteps() - 1);
		assertTrue(Math.hypot(first.x(), first.z()) <= CelestialRuinRules.DAMAGE_RADIUS);
		assertTrue(Math.hypot(last.x(), last.z()) <= CelestialRuinRules.DAMAGE_RADIUS + 1.0);
		assertEquals(first, CelestialRuinRules.aftershockOffset(0));
		assertEquals(new CelestialRuinRules.AftershockOffset(4, 0), first);
		assertEquals(first, CelestialRuinRules.aftershockOffset(-100));
		assertEquals(last, CelestialRuinRules.aftershockOffset(Integer.MAX_VALUE));
		assertEquals(new CelestialRuinRules.AftershockOffset(0, 4),
				CelestialRuinRules.aftershockOffset(24));
		assertEquals(new CelestialRuinRules.AftershockOffset(-4, 0),
				CelestialRuinRules.aftershockOffset(48));
	}

	@Test
	void livingForceCleanupSurvivesOptionalTerrainSafetyPolicy() {
		assertTrue(CelestialRuinRules.shouldDestroy(true, true, false, false));
		assertTrue(CelestialRuinRules.shouldDestroy(true, false, false, false));
		assertFalse(CelestialRuinRules.shouldDestroy(false, false, false, true));
		assertFalse(CelestialRuinRules.shouldDestroy(false, true, true, false));
		assertTrue(CelestialRuinRules.shouldDestroy(false, true, true, true));
	}

	@Test
	void aftershockFireObeysTerrainAndSafeZoneProtection() {
		assertTrue(CelestialRuinRules.shouldIgnite(true, false));
		assertFalse(CelestialRuinRules.shouldIgnite(false, false));
		assertFalse(CelestialRuinRules.shouldIgnite(true, true));
	}
}
