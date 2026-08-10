package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowSwordRulesTest {
	@Test
	void onlyDarknessMayCommandTheSword() {
		assertFalse(ShadowSwordRules.mayUse(false));
		assertTrue(ShadowSwordRules.mayUse(true));
	}

	@Test
	void nightfallRankRemovesEverySwordCooldown() {
		assertFalse(ShadowSwordRules.bypassesCooldown(9));
		assertTrue(ShadowSwordRules.bypassesCooldown(10));
		assertTrue(ShadowSwordRules.bypassesCooldown(50));
	}

	@Test
	void inventoryConsequencesAreBoundedAndDeliberate() {
		assertEquals(50, ShadowSwordRules.AUTHORIZED_REGEN_PER_SECOND);
		assertEquals(50, ShadowSwordRules.regenerationPerSecond(0));
		assertEquals(250, ShadowSwordRules.regenerationPerSecond(10));
		assertEquals(4, ShadowSwordRules.MAX_PROTECTORS);
		assertEquals(2, ShadowSwordRules.protectorsToSummon(0));
		assertEquals(1, ShadowSwordRules.protectorsToSummon(3));
		assertEquals(0, ShadowSwordRules.protectorsToSummon(4));
		assertEquals(0, ShadowSwordRules.protectorsToSummon(20));
		assertEquals(8, ShadowSwordRules.commandedGuardiansToSummon(8, 0));
		assertEquals(2, ShadowSwordRules.commandedGuardiansToSummon(8, 30));
		assertEquals(0, ShadowSwordRules.commandedGuardiansToSummon(4, 32));
	}

	@Test
	void groundCorruptionUsesACompactDiscInsteadOfACube() {
		assertTrue(ShadowSwordRules.inSpreadDisc(0, 0));
		assertTrue(ShadowSwordRules.inSpreadDisc(6, 0));
		assertFalse(ShadowSwordRules.inSpreadDisc(6, 1));
	}
}
