package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightRulesTest {
	@Test
	void propulsionSupportsHoverVerticalControlAndRankedSprint() {
		assertEquals(0.0, FlightRules.motion(0.0F, false, false, false,
				false, false, false, false, 0).y(), 0.0001);
		assertEquals(0.9, FlightRules.motion(0.0F, false, false, false,
				false, true, false, false, 0).y(), 0.0001);
		assertEquals(-0.9, FlightRules.motion(0.0F, false, false, false,
				false, false, true, false, 0).y(), 0.0001);
		double base = FlightRules.motion(0.0F, true, false, false,
				false, false, false, false, 0).horizontalSpeed();
		double mastered = FlightRules.motion(0.0F, true, false, false,
				false, false, false, true, 10).horizontalSpeed();
		assertTrue(mastered > base);
	}

	@Test
	void yawAndStrafeProduceNormalizedHorizontalMotion() {
		FlightRules.Motion motion = FlightRules.motion(90.0F, true, false, false,
				true, false, false, true, 5);
		assertTrue(motion.horizontalSpeed() > 1.0);
		assertTrue(Double.isFinite(motion.x()));
		assertTrue(Double.isFinite(motion.z()));
	}

	@Test
	void momentumTurnsAndAccelerationAreBounded() {
		FlightRules.Motion east = new FlightRules.Motion(1.0, 0.0, 0.0);
		FlightRules.Step turn = FlightRules.step(east, 0.0F,
				new FlightRules.Input(true, false, false, false, false, false, true),
				new FlightRules.Environment(false, 400, 500, false), 10);
		assertTrue(turn.motion().x() > 0.0, "one input packet cannot snap east momentum away");
		assertTrue(turn.motion().z() > 0.0, "the new northward intent starts a smooth turn");
		assertTrue(distance(east, turn.motion()) <= FlightRules.MAX_AIR_ACCELERATION + 0.0001);
		assertTrue(Math.abs(turn.tiltDegrees()) <= FlightRules.MAX_TILT_DEGREES);
	}

	@Test
	void releasingMovementAirBrakesWithoutReversingOrDropping() {
		FlightRules.Motion fast = new FlightRules.Motion(2.4, 0.35, 0.0);
		FlightRules.Step braking = FlightRules.step(fast, 0.0F,
				FlightRules.Input.NONE,
				new FlightRules.Environment(false, 300, 500, false), 6);
		assertTrue(braking.motion().x() < fast.x());
		assertTrue(braking.motion().x() >= 0.0);
		assertTrue(Math.abs(braking.motion().y()) < Math.abs(fast.y()));
	}

	@Test
	void waterTransitionCapsSpeedAndVerticalControl() {
		FlightRules.Motion fast = new FlightRules.Motion(3.5, 0.9, 0.0);
		FlightRules.Motion water = fast;
		for (int tick = 0; tick < 40; tick++) {
			water = FlightRules.step(water, 90.0F,
					new FlightRules.Input(true, false, false, false, true, false, true),
					new FlightRules.Environment(true, 300, 500, false), 10).motion();
		}
		assertTrue(water.horizontalSpeed() <= FlightRules.MAX_WATER_SPEED + 0.0001);
		assertTrue(water.y() <= FlightRules.MAX_WATER_VERTICAL_SPEED + 0.0001);
		FlightRules.Step settled = FlightRules.step(water, 90.0F,
				FlightRules.Input.NONE,
				new FlightRules.Environment(true, 300, 500, false), 10);
		assertFalse(settled.sonicCrossed());
	}

	@Test
	void sonicAndStaminaCuesAreDeterministic() {
		FlightRules.Step sonic = FlightRules.step(new FlightRules.Motion(2.95, 0.0, 0.0),
				-90.0F, new FlightRules.Input(true, false, false, false, false, false, true),
				new FlightRules.Environment(false, 400, 500, false), 10);
		assertTrue(sonic.sonicCrossed());
		assertEquals(FlightRules.SoundCue.SONIC_BOOM, sonic.soundCue());

		FlightRules.Step low = FlightRules.step(FlightRules.Motion.ZERO, 0.0F,
				FlightRules.Input.NONE,
				new FlightRules.Environment(false, 50, 500, false), 0);
		assertEquals(FlightRules.SoundCue.LOW_ENERGY, low.soundCue());
	}

	private static double distance(FlightRules.Motion first, FlightRules.Motion second) {
		double x = first.x() - second.x();
		double y = first.y() - second.y();
		double z = first.z() - second.z();
		return Math.sqrt(x * x + y * y + z * z);
	}
}
