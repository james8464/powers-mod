package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Boss control outcomes always retain explicit, bounded feedback semantics. */
class ControlResistanceTest {
	@Test
	void everyOutcomeHasAnExplicitDurationAndImpulsePolicy() {
		assertEquals(200, ControlResistance.adjustDuration(200, ControlResistance.Outcome.FULL));
		assertEquals(60, ControlResistance.adjustDuration(200, ControlResistance.Outcome.RESISTED));
		assertEquals(0, ControlResistance.adjustDuration(200, ControlResistance.Outcome.IMMUNE));
		assertEquals(0, ControlResistance.adjustDuration(200, ControlResistance.Outcome.REFLECTED));

		Vec3 impulse = new Vec3(10, 4, -2);
		assertEquals(impulse, ControlResistance.adjustImpulse(impulse, ControlResistance.Outcome.FULL));
		assertEquals(new Vec3(3, 1.2, -0.6),
				ControlResistance.adjustImpulse(impulse, ControlResistance.Outcome.RESISTED));
		assertEquals(Vec3.ZERO, ControlResistance.adjustImpulse(impulse, ControlResistance.Outcome.IMMUNE));
		assertEquals(Vec3.ZERO, ControlResistance.adjustImpulse(impulse, ControlResistance.Outcome.REFLECTED));
	}

	@Test
	void tagPrecedenceCannotSilentlyChange() {
		assertEquals(ControlResistance.Outcome.REFLECTED,
				ControlResistance.fromFlags(true, true, true));
		assertEquals(ControlResistance.Outcome.IMMUNE,
				ControlResistance.fromFlags(false, true, true));
		assertEquals(ControlResistance.Outcome.RESISTED,
				ControlResistance.fromFlags(false, false, true));
		assertEquals(ControlResistance.Outcome.FULL,
				ControlResistance.fromFlags(false, false, false));
	}
}
