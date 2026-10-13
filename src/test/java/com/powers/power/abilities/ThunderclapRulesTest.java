package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThunderclapRulesTest {
	@Test
	void waveIsWideForwardFacingAndRangeBounded() {
		assertTrue(ThunderclapRules.inCone(0.0, 10.0, 0.0, 1.0, 32.0));
		assertTrue(ThunderclapRules.inCone(8.0, 10.0, 0.0, 1.0, 32.0));
		assertFalse(ThunderclapRules.inCone(0.0, -10.0, 0.0, 1.0, 32.0));
		assertFalse(ThunderclapRules.inCone(0.0, 33.0, 0.0, 1.0, 32.0));
	}

	@Test
	void verticalAimFallsBackToTheCastersYaw() {
		ThunderclapRules.HorizontalDirection south =
				ThunderclapRules.horizontalDirection(0.0, 0.0, 0.0F);
		assertEquals(0.0, south.x(), 1.0E-8);
		assertEquals(1.0, south.z(), 1.0E-8);

		ThunderclapRules.HorizontalDirection west =
				ThunderclapRules.horizontalDirection(0.0, 0.0, 90.0F);
		assertEquals(-1.0, west.x(), 1.0E-8);
		assertEquals(0.0, west.z(), 1.0E-8);
	}

	@Test
	void caveEchoRequiresAForwardReflectionAndEnclosure() {
		assertEquals(ThunderclapRules.Echo.NONE,
				ThunderclapRules.echo(false, true, true, true));
		assertEquals(ThunderclapRules.Echo.NONE,
				ThunderclapRules.echo(true, false, false, false));
		assertEquals(ThunderclapRules.Echo.CAVE,
				ThunderclapRules.echo(true, true, false, true));
		assertEquals(ThunderclapRules.Echo.CAVE,
				ThunderclapRules.echo(true, false, true, true));
	}

	@Test
	void taggedFragileTerrainBreaksAtEveryRankWithoutAdmittingForbiddenBlocks() {
		for (int rank = 0; rank <= 10; rank++) {
			assertTrue(ThunderclapRules.mayBreakTerrain(true, false, 100.0F, rank));
		}
		assertFalse(ThunderclapRules.mayBreakTerrain(true, true, 0.3F, 10));
		assertFalse(ThunderclapRules.mayBreakTerrain(false, false, 20.0F, 10));
		assertTrue(ThunderclapRules.mayBreakTerrain(false, false, 2.5F, 0));
	}
}
