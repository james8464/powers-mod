package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoidBeamRulesTest {
	@Test
	void chargeOpensOnlyAtTheTwelfthTick() {
		assertEquals(12, VoidBeamRules.chargeRemaining(100, 100));
		assertEquals(1, VoidBeamRules.chargeRemaining(100, 111));
		assertEquals(0, VoidBeamRules.chargeRemaining(100, 112));
		assertEquals(0, VoidBeamRules.chargeRemaining(100, 200));
	}

	@Test
	void rankVariantsAddFinitePenetration() {
		assertEquals(3, VoidBeamRules.penetrationLimit(false, false));
		assertEquals(4, VoidBeamRules.penetrationLimit(true, false));
		assertEquals(4, VoidBeamRules.penetrationLimit(false, true));
		assertEquals(5, VoidBeamRules.penetrationLimit(true, true));
	}

	@Test
	void penetrationDamageFallsAndNeverBecomesMalformed() {
		assertEquals(1.0, VoidBeamRules.damageMultiplier(0), 0.0);
		assertEquals(0.72, VoidBeamRules.damageMultiplier(1), 0.0);
		assertEquals(0.52, VoidBeamRules.damageMultiplier(2), 0.0);
		assertEquals(0.52, VoidBeamRules.damageMultiplier(4), 0.0);
		assertEquals(0.0, VoidBeamRules.damageMultiplier(-1), 0.0);
	}

	@Test
	void candidatesAreNearestFirstDeduplicatedAndCapped() {
		var selected = VoidBeamRules.selectPenetrations(List.of(
				new VoidBeamRules.RayCandidate<>("far", 8.0),
				new VoidBeamRules.RayCandidate<>("near", 2.0),
				new VoidBeamRules.RayCandidate<>("near", 3.0),
				new VoidBeamRules.RayCandidate<>("outside", 12.0),
				new VoidBeamRules.RayCandidate<>("malformed", Double.NaN)), 10.0, 2);

		assertEquals(List.of("near", "far"), selected.stream()
				.map(VoidBeamRules.RayCandidate::target).toList());
	}

	@Test
	void segmentSphereReturnsTheNearestEntryDistance() {
		assertEquals(4.0, VoidBeamRules.segmentSphereEntry(
				0, 0, 0, 10, 0, 0, 5, 0, 0, 1), 1.0E-6);
		assertEquals(0.0, VoidBeamRules.segmentSphereEntry(
				5, 0, 0, 10, 0, 0, 5, 0, 0, 1), 1.0E-6);
		assertTrue(Double.isNaN(VoidBeamRules.segmentSphereEntry(
				0, 0, 0, 2, 0, 0, 5, 0, 0, 1)));
	}

	@Test
	void nearestValidWardInterceptWinsBeforeTheTerminalBlock() {
		var result = VoidBeamRules.nearestIntercept(List.of(
				new VoidBeamRules.RayIntercept(VoidBeamRules.Counterplay.KINETIC_WARD, 7.0),
				new VoidBeamRules.RayIntercept(VoidBeamRules.Counterplay.SANCTUARY, 3.0),
				new VoidBeamRules.RayIntercept(VoidBeamRules.Counterplay.NONE, 1.0),
				new VoidBeamRules.RayIntercept(VoidBeamRules.Counterplay.AMETHYST, Double.NaN)), 6.0);

		assertTrue(result.isPresent());
		assertEquals(VoidBeamRules.Counterplay.SANCTUARY, result.orElseThrow().counterplay());
		assertEquals(3.0, result.orElseThrow().distance(), 0.0);
		assertTrue(VoidBeamRules.nearestIntercept(List.of(
				new VoidBeamRules.RayIntercept(VoidBeamRules.Counterplay.KINETIC_WARD, 7.0)), 6.0)
				.isEmpty());
	}

	@Test
	void scarCadenceAndBoundsAreHardCapped() {
		assertTrue(VoidBeamRules.shouldRenderScar(5));
		assertFalse(VoidBeamRules.shouldRenderScar(0));
		assertTrue(VoidBeamRules.shouldPulseScar(10));
		assertFalse(VoidBeamRules.shouldPulseScar(0));
		assertFalse(VoidBeamRules.shouldPulseScar(9));
		assertEquals(1.0, VoidBeamRules.scarRadius(Double.NaN), 0.0);
		assertEquals(4.0, VoidBeamRules.scarRadius(99), 0.0);
		assertEquals(20, VoidBeamRules.scarDuration(-1, false));
		assertEquals(100, VoidBeamRules.scarDuration(80, true));
		assertEquals(160, VoidBeamRules.scarDuration(999, true));
	}
}
