package com.powers.power;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmethystScanCacheTest {
	@Test
	void reusesOnlySamePositionDimensionAndUnexpiredResult() {
		AmethystScanCache cache = new AmethystScanCache();
		UUID player = UUID.randomUUID();
		cache.put(player, "minecraft:overworld", 1, 64, 2, 100, true);
		assertEquals(Boolean.TRUE, cache.get(player, "minecraft:overworld", 1, 64, 2, 100));
		assertNull(cache.get(player, "minecraft:overworld", 2, 64, 2, 100));
		assertNull(cache.get(player, "minecraft:the_nether", 1, 64, 2, 100));
		assertNull(cache.get(player, "minecraft:overworld", 1, 64, 2, 101));
	}
}
