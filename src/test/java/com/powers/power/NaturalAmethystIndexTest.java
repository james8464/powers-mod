package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaturalAmethystIndexTest {
	@Test
	void nearbyLookupTouchesOnlyIntersectingSections() {
		var centered = NaturalAmethystIndex.sectionKeys(8, 72, 8, 6);
		assertEquals(1, centered.size());
		var boundary = NaturalAmethystIndex.sectionKeys(15, 63, 15, 6);
		assertEquals(8, boundary.size());
		assertEquals(boundary.size(), boundary.stream().distinct().count());
	}

	@Test
	void sectionFootprintIsBoundedForTheConfiguredRadius() {
		for (int x = -32; x <= 32; x++) {
			var sections = NaturalAmethystIndex.sectionKeys(x, 0, -x, 6);
			assertTrue(sections.size() >= 1 && sections.size() <= 8);
		}
	}
}
