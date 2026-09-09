package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powers.realm.RealmKind;
import org.junit.jupiter.api.Test;

class CartographerVisibilityRulesTest {
	@Test
	void ordinaryWorldQueriesDoNotRequireRealmProgression() {
		assertTrue(CartographerVisibilityRules.mayReveal(false, null, false, 0, 0));
	}

	@Test
	void landmarksRequireTheMatchingCompletedProgression() {
		assertFalse(CartographerVisibilityRules.mayReveal(true, null, false, 10, 10));
		assertTrue(CartographerVisibilityRules.mayReveal(true, RealmKind.LIGHT, false, 10, 0));
		assertFalse(CartographerVisibilityRules.mayReveal(true, RealmKind.LIGHT, true, 10, 10));
		assertFalse(CartographerVisibilityRules.mayReveal(true, RealmKind.LIGHT, false, 9, 10));
		assertTrue(CartographerVisibilityRules.mayReveal(true, RealmKind.DARK, true, 0, 10));
		assertFalse(CartographerVisibilityRules.mayReveal(true, RealmKind.DARK, false, 10, 10));
		assertFalse(CartographerVisibilityRules.mayReveal(true, RealmKind.DARK, true, 10, 9));
	}
}
