package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void returnCueShowsOnlyRoundedDistanceDirectionAndDimensionInTheSameRealm() {
		AstralReturnIndicator.Cue cue = AstralReturnIndicator.resolve(
				"minecraft:overworld", new Vec3(10.0, 64.0, -5.0),
				"minecraft:overworld", new Vec3(22.4, 200.0, 7.4));

		assertTrue(cue.sameDimension());
		assertEquals("NW", cue.direction());
		assertEquals(18, cue.distanceBlocks());
		assertEquals("minecraft:overworld", cue.dimension());
		assertEquals(List.of("sameDimension", "dimension", "direction", "distanceBlocks"),
				Arrays.stream(AstralReturnIndicator.Cue.class.getRecordComponents())
						.map(component -> component.getName()).toList());
	}

	@Test
	void returnCueRedactsDirectionAndDistanceAcrossDimensions() {
		AstralReturnIndicator.Cue cue = AstralReturnIndicator.resolve(
				"minecraft:the_nether", new Vec3(12345.0, 64.0, -9876.0),
				"minecraft:overworld", Vec3.ZERO);

		assertFalse(cue.sameDimension());
		assertEquals("", cue.direction());
		assertEquals(-1, cue.distanceBlocks());
		assertEquals("minecraft:the_nether", cue.dimension());
	}
}
