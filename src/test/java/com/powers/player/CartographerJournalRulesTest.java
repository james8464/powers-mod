package com.powers.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartographerJournalRulesTest {
	@Test
	void journalIsPersistentBoundedDeduplicatedAndDimensionScoped() {
		List<CartographerSite> sites = new ArrayList<>();
		for (int index = 0; index < 35; index++) {
			sites = CartographerJournalRules.discover(sites, site("biome", "test:" + index,
					"minecraft:overworld", index, index));
		}
		assertEquals(32, sites.size());
		assertEquals("test:3", sites.getFirst().queryTarget());

		CartographerSite replacement = site("biome", "test:10", "minecraft:overworld", 99, 80);
		sites = CartographerJournalRules.discover(sites, replacement);
		assertEquals(32, sites.size());
		assertEquals(replacement, sites.getLast());
		assertTrue(CartographerJournalRules.find(sites, "minecraft:overworld", "biome", "test:10")
				.isPresent());
		assertTrue(CartographerJournalRules.find(sites, "minecraft:the_nether", "biome", "test:10")
				.isEmpty());
	}

	@Test
	void siteCodecRoundTripsStableJournalFacts() {
		CartographerSite original = site("structure", "minecraft:village_plains",
				"minecraft:overworld", -240, 1234);
		var encoded = CartographerSite.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		assertEquals(original, CartographerSite.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
	}

	private static CartographerSite site(String kind, String target, String dimension, int x, long tick) {
		return new CartographerSite(kind, target, target, dimension, x, 64, x + 20, tick);
	}
}
