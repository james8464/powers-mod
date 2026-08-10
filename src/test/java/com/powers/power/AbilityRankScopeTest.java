package com.powers.power;

import com.powers.power.abilities.LightningStrikeAbility;
import com.powers.power.crystals.InfernoAbility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityRankScopeTest {
	@Test
	void onlyInnatePlayerPowersOptIntoRankScaling() {
		assertTrue(new LightningStrikeAbility().usesRankScaling());
		assertFalse(new InfernoAbility().usesRankScaling());
	}
}
