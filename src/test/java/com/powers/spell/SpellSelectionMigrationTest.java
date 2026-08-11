package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellSelectionMigrationTest {
	@Test
	void retiredDeepSelectionsCollapseToDimensionalAnchor() {
		for (int oldIndex = 0; oldIndex < 4; oldIndex++) {
			assertEquals(0, SpellSelectionMigration.canonicalIndex(
					"book_grimoire_deep", oldIndex));
		}
	}

	@Test
	void abyssalWardStaysSelectedAndEveryRetiredActionMigratesToDispel() {
		assertEquals(0, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_abyssal", 0));
		assertEquals(1, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_abyssal", 1));
		assertEquals(1, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_abyssal", 2));
		assertEquals(1, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_abyssal", 3));
	}

	@Test
	void existingCanonicalIndicesRemainIdempotent() {
		assertEquals(2, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_wild", 2));
		assertEquals(3, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_celestial", 3));
		assertEquals(1, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_blight", 1));
	}

	@Test
	void corruptAndDormantSelectionsUseSafeDefaults() {
		assertEquals(0, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_infernal", 2));
		assertEquals(0, SpellSelectionMigration.canonicalIndex(
				"book_grimoire_deep", -50));
		assertEquals(0, SpellSelectionMigration.canonicalIndex("unknown", 99));
	}
}
