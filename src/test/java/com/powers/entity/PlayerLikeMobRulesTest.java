package com.powers.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLikeMobRulesTest {
	@Test
	void darknessCreaturesIgnoreTheirOwnFaction() {
		assertTrue(PlayerLikeMobRules.mayTarget(true, false));
		assertFalse(PlayerLikeMobRules.mayTarget(true, true));
		assertFalse(PlayerLikeMobRules.mayTarget(false, false));
	}

	@Test
	void combatCadenceAlternatesReadablePlayerPowers() {
		assertEquals(PlayerLikeMobRules.Cast.LIGHTNING, PlayerLikeMobRules.castAt(0));
		assertEquals(PlayerLikeMobRules.Cast.FIREBALL, PlayerLikeMobRules.castAt(80));
		assertEquals(PlayerLikeMobRules.Cast.NONE, PlayerLikeMobRules.castAt(20));
	}
}
