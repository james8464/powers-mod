package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Guards the spirit's first visible frame from overlapping its body proxy. */
class AstralProjectionAbilityTest {
	@Test
	void initialSpiritPositionClearsTheBodyRegardlessOfLookPitch() {
		Vec3 origin = new Vec3(10.0, 64.0, -5.0);

		assertEquals(new Vec3(10.0, 64.65, -3.75),
				AstralProjectionAbility.initialSpiritPosition(origin, new Vec3(0.0, 0.0, 1.0)));
		assertEquals(new Vec3(10.0, 64.65, -3.75),
				AstralProjectionAbility.initialSpiritPosition(origin, new Vec3(0.0, 1.0, 0.0)));
	}

	@Test
	void initialSpiritPositionUsesTheHorizontalLookDirection() {
		assertEquals(new Vec3(10.75, 64.65, -4.0),
				AstralProjectionAbility.initialSpiritPosition(
						new Vec3(10.0, 64.0, -5.0), new Vec3(3.0, 99.0, 4.0)));
	}
}
