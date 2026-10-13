package com.powers.companion;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowPlaceMemoryTest {
	@Test
	void namedPointsAreNormalizedOwnerBoundedAndExplicitlyDeleted() {
		ShadowPlaceMemory memory = ShadowPlaceMemory.empty();
		Identifier dimension = Identifier.parse("minecraft:overworld");
		for (int i = 0; i < ShadowPlaceMemory.MAX_PLACES; i++) {
			var result = memory.remember("Camp " + i, dimension, new BlockPos(i, 64, i));
			assertTrue(result.accepted());
			memory = result.memory();
		}
		assertEquals(16, memory.size());
		assertFalse(memory.remember("overflow", dimension, BlockPos.ZERO).accepted());

		var updated = memory.remember("CAMP 0", dimension, new BlockPos(99, 70, 99));
		assertTrue(updated.accepted());
		memory = updated.memory();
		assertEquals(new BlockPos(99, 70, 99), memory.find("camp 0").orElseThrow().position());
		memory = memory.forget("camp 0").memory();
		assertTrue(memory.find("camp 0").isEmpty());
		assertEquals(15, memory.size());
	}

	@Test
	void codecRoundTripRetainsDimensionAndBoundedPoints() {
		ShadowPlaceMemory original = ShadowPlaceMemory.empty().remember("home",
				Identifier.parse("minecraft:the_nether"), new BlockPos(-12, 45, 88)).memory();
		var encoded = ShadowPlaceMemory.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		ShadowPlaceMemory decoded = ShadowPlaceMemory.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertEquals(original, decoded);
		assertEquals("minecraft:the_nether", decoded.find("HOME").orElseThrow().dimension());
	}

	@Test
	void navigationAdmissionIsDimensionSafeLoadedOnlyAndDistanceBounded() {
		assertEquals(ShadowPlaceNavigationRules.Decision.GO,
				ShadowPlaceNavigationRules.decide(true, true, 64.0));
		assertEquals(ShadowPlaceNavigationRules.Decision.WRONG_DIMENSION,
				ShadowPlaceNavigationRules.decide(false, true, 64.0));
		assertEquals(ShadowPlaceNavigationRules.Decision.CHUNK_UNLOADED,
				ShadowPlaceNavigationRules.decide(true, false, 64.0));
		assertEquals(ShadowPlaceNavigationRules.Decision.TOO_FAR,
				ShadowPlaceNavigationRules.decide(true, true,
						ShadowPlaceNavigationRules.MAX_DISTANCE_SQUARED + 1.0));
	}
}
