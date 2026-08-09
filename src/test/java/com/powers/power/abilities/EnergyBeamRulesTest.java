package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.powers.power.abilities.EnergyBeamRules.Counterplay.AMETHYST;
import static com.powers.power.abilities.EnergyBeamRules.Counterplay.KINETIC_WARD;
import static com.powers.power.abilities.EnergyBeamRules.Counterplay.SURFACE;
import static com.powers.power.abilities.EnergyBeamRules.Counterplay.WATER;
import static com.powers.power.abilities.EnergyBeamRules.Phase.FINISHED;
import static com.powers.power.abilities.EnergyBeamRules.Phase.FIRING;
import static com.powers.power.abilities.EnergyBeamRules.Phase.FOCUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards sunfire timing, escalation, finite terminal ordering, and bounded rank work. */
class EnergyBeamRulesTest {
	@Test
	void focusAndFourDamageBeatsUseExactServerBoundaries() {
		long start = 100L;

		assertEquals(FOCUS, EnergyBeamRules.phase(start, 100L));
		assertEquals(FOCUS, EnergyBeamRules.phase(start, 107L));
		assertEquals(FIRING, EnergyBeamRules.phase(start, 108L));
		assertEquals(FIRING, EnergyBeamRules.phase(start, 139L));
		assertEquals(FINISHED, EnergyBeamRules.phase(start, 140L));
		assertEquals(8, EnergyBeamRules.focusRemaining(start, 100L));
		assertEquals(1, EnergyBeamRules.focusRemaining(start, 107L));
		assertEquals(0, EnergyBeamRules.focusRemaining(start, 108L));

		assertTrue(EnergyBeamRules.damageBeat(start, 108L));
		assertTrue(EnergyBeamRules.damageBeat(start, 118L));
		assertTrue(EnergyBeamRules.damageBeat(start, 128L));
		assertTrue(EnergyBeamRules.damageBeat(start, 138L));
		assertFalse(EnergyBeamRules.damageBeat(start, 107L));
		assertFalse(EnergyBeamRules.damageBeat(start, 109L));
		assertFalse(EnergyBeamRules.damageBeat(start, 140L));
	}

	@Test
	void consecutiveScorchEscalatesThenCapsAndResets() {
		assertEquals(1, EnergyBeamRules.nextStreak(false, 0));
		assertEquals(2, EnergyBeamRules.nextStreak(true, 1));
		assertEquals(3, EnergyBeamRules.nextStreak(true, 2));
		assertEquals(3, EnergyBeamRules.nextStreak(true, Integer.MAX_VALUE));
		assertEquals(1.0, EnergyBeamRules.scorchMultiplier(1), 0.0001);
		assertEquals(1.15, EnergyBeamRules.scorchMultiplier(2), 0.0001);
		assertEquals(1.30, EnergyBeamRules.scorchMultiplier(3), 0.0001);
		assertEquals(5.2, EnergyBeamRules.scorchDamage(4.0, 50), 0.0001);
	}

	@Test
	void burnSteamAndMasteryDamageRemainBounded() {
		assertEquals(60, EnergyBeamRules.burnTicks(60, 1));
		assertEquals(75, EnergyBeamRules.burnTicks(60, 2));
		assertEquals(90, EnergyBeamRules.burnTicks(60, 3));
		assertEquals(0, EnergyBeamRules.burnTicks(-1, 3));
		assertEquals(2.6, EnergyBeamRules.steamDamage(4.0), 0.0001);
		assertEquals(1.8, EnergyBeamRules.splitDamage(4.0), 0.0001);
		assertEquals(0.0, EnergyBeamRules.steamDamage(Double.NaN), 0.0001);
	}

	@Test
	void rankVariantsHaveHardTargetAndFlareCaps() {
		assertEquals(0, EnergyBeamRules.splitLimit(false));
		assertEquals(2, EnergyBeamRules.splitLimit(true));
		assertEquals(8, EnergyBeamRules.auxiliaryTargetLimit());
		assertTrue(EnergyBeamRules.flareReady(true, 3, false, 0));
		assertFalse(EnergyBeamRules.flareReady(false, 3, false, 0));
		assertFalse(EnergyBeamRules.flareReady(true, 2, false, 0));
		assertFalse(EnergyBeamRules.flareReady(true, 3, true, 0));
		assertFalse(EnergyBeamRules.flareReady(true, 3, false, 1));
	}

	@Test
	void nearestFiniteTerminalWinsInsideRange() {
		var terminal = EnergyBeamRules.nearestTerminal(List.of(
				new EnergyBeamRules.Intercept(SURFACE, 20.0),
				new EnergyBeamRules.Intercept(KINETIC_WARD, 12.0),
				new EnergyBeamRules.Intercept(WATER, 10.0),
				new EnergyBeamRules.Intercept(AMETHYST, Double.NaN),
				new EnergyBeamRules.Intercept(AMETHYST, -1.0)), 48.0).orElseThrow();

		assertEquals(WATER, terminal.counterplay());
		assertEquals(10.0, terminal.distance(), 0.0001);
		assertTrue(EnergyBeamRules.nearestTerminal(List.of(
				new EnergyBeamRules.Intercept(SURFACE, 49.0)), 48.0).isEmpty());
		assertTrue(EnergyBeamRules.nearestTerminal(null, 48.0).isEmpty());
	}

	@Test
	void waterSamplingAndSteamImpulseAreFiniteAndCapped() {
		assertEquals(96, EnergyBeamRules.waterSamples(48.0));
		assertEquals(128, EnergyBeamRules.waterSamples(96.0));
		assertEquals(0, EnergyBeamRules.waterSamples(Double.NaN));
		Vec3 impulse = EnergyBeamRules.steamImpulse(Vec3.ZERO, new Vec3(3.0, 8.0, 4.0), 0.8);
		assertEquals(0.48, impulse.x, 0.0001);
		assertEquals(0.20, impulse.y, 0.0001);
		assertEquals(0.64, impulse.z, 0.0001);
		assertEquals(Vec3.ZERO, EnergyBeamRules.steamImpulse(Vec3.ZERO, Vec3.ZERO, 0.8));
	}

	@Test
	void lifecycleRequiresLiveUnsuppressedUnfrozenOwnerAndPower() {
		assertTrue(EnergyBeamRules.channelContinues(
				true, true, true, false, false, true, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				false, true, true, false, false, true, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				true, false, true, false, false, true, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				true, true, false, false, false, true, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				true, true, true, true, false, true, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				true, true, true, false, true, true, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				true, true, true, false, false, false, 139L, 140L));
		assertFalse(EnergyBeamRules.channelContinues(
				true, true, true, false, false, true, 140L, 140L));
	}
}
