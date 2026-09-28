package com.powers.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstVesselTelegraphRulesTest {
	@Test
	void everyCatalogueActionHasAnAccessibleNonzeroWarning() {
		for (FirstVesselPowerAction action : FirstVesselPowerCatalogue.actions()) {
			assertTrue(FirstVesselTelegraphRules.warningTicks(action) >= 20, action.powerId());
		}
	}
}
