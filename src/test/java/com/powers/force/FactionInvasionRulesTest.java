package com.powers.force;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionInvasionRulesTest {
	@Test
	void opposingForceInvadesButAlignedForceDoesNot() {
		assertTrue(FactionInvasionRules.shouldInvade(LivingForceKind.DARKNESS, false));
		assertFalse(FactionInvasionRules.shouldInvade(LivingForceKind.DARKNESS, true));
		assertTrue(FactionInvasionRules.shouldInvade(LivingForceKind.PURE_LIGHT, true));
		assertFalse(FactionInvasionRules.shouldInvade(LivingForceKind.PURE_LIGHT, false));
	}

	@Test
	void invasionAndScarWorkStayHardCapped() {
		assertEquals(64, FactionInvasionRules.GLOBAL_INVADER_CAP);
		assertEquals(3, FactionInvasionRules.NEARBY_INVADER_CAP);
		assertEquals(5, FactionInvasionRules.scarOffsets().size());
	}
}
