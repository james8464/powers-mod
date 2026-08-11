package com.powers.protection;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentOverrideRulesTest {
	@Test
	void everyPersistentConsentCategoryUsesTheSameOverridePolicy() {
		assertEquals(EnumSet.of(ConsentKind.TELEPORT, ConsentKind.LOCATOR,
				ConsentKind.COMPANION, ConsentKind.DREAMWALK, ConsentKind.POSSESSION),
				EnumSet.allOf(ConsentKind.class));
		assertEquals(40, ConsentOverrideRules.OVERRIDE_ENERGY_SURCHARGE);
	}

	@Test
	void selfAndOrdinaryConsentPassWithoutACharge() {
		assertEquals(ConsentOverrideRules.Decision.ALLOW_FREE,
				ConsentOverrideRules.decide(true, true, false, false, false));
		assertEquals(ConsentOverrideRules.Decision.ALLOW_FREE,
				ConsentOverrideRules.decide(false, false, true, false, false));
	}

	@Test
	void safeZonesRemainAuthoritativeOverTheJewel() {
		assertEquals(ConsentOverrideRules.Decision.DENY_SAFE_ZONE,
				ConsentOverrideRules.decide(false, true, false, true, true));
	}

	@Test
	void overrideRequiresExactlyOneCarriedJewelAndEnoughEnergy() {
		assertEquals(ConsentOverrideRules.Decision.DENY_CONSENT,
				ConsentOverrideRules.decide(false, false, false, false, true));
		assertEquals(ConsentOverrideRules.Decision.DENY_ENERGY,
				ConsentOverrideRules.decide(false, false, false, true, false));
		assertEquals(ConsentOverrideRules.Decision.ALLOW_OVERRIDE,
				ConsentOverrideRules.decide(false, false, false, true, true));
	}
}
