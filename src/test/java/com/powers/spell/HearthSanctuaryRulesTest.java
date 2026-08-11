package com.powers.spell;

import com.powers.power.state.MagicShieldManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HearthSanctuaryRulesTest {
	@Test
	void radiusAndWorkBudgetAreStrictlyBounded() {
		assertTrue(HearthSanctuaryRules.withinRadius(0.0));
		assertTrue(HearthSanctuaryRules.withinRadius(9.0));
		assertFalse(HearthSanctuaryRules.withinRadius(9.0001));
		assertFalse(HearthSanctuaryRules.withinRadius(Double.NaN));
		assertEquals(32, HearthSanctuaryRules.MAX_TARGETS);
		assertEquals(40.0F, HearthSanctuaryRules.INTEGRITY);
	}

	@Test
	void everyRecipientOwnsIndependentNoOverflowIntegrity() {
		MagicShieldManager manager = new MagicShieldManager();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		manager.raise(first, HearthSanctuaryRules.INTEGRITY, Long.MAX_VALUE, false);
		manager.raise(second, HearthSanctuaryRules.INTEGRITY, Long.MAX_VALUE, false);

		MagicShieldManager.Impact overkill = manager.absorb(first, 50_000.0F, 10);

		assertTrue(overkill.blocked());
		assertTrue(overkill.shattered());
		assertFalse(manager.active(first, 10));
		assertTrue(manager.active(second, 10));
		assertFalse(manager.absorb(second, 1.0F, 11).reflective());
	}
}
