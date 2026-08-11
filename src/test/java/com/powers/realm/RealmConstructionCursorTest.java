package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmConstructionCursorTest {
	@Test
	void constructionAdvancesWithinABoundedPerTickBudget() {
		RealmConstructionCursor cursor = new RealmConstructionCursor(275);
		assertEquals(128, cursor.claim(128));
		assertEquals(128, cursor.position());
		assertFalse(cursor.complete());
		assertEquals(128, cursor.claim(128));
		assertEquals(19, cursor.claim(128));
		assertTrue(cursor.complete());
		assertEquals(0, cursor.claim(128));
	}
}
