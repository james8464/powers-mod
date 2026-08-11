package com.powers.power.abilities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoubleHealthRulesTest {
	@Test void healingFillsOnlyTheNewRowsAndRapidRetoggleCannotHealAgain() {
		assertEquals(20.0F, DoubleHealthAbility.healToCap(10, 20, 40));
		assertEquals(0.0F, DoubleHealthAbility.healToCap(40, 20, 40));
		assertFalse(DoubleHealthAbility.mayHeal(100, 250));
		assertTrue(DoubleHealthAbility.mayHeal(100, 301));
	}
}
