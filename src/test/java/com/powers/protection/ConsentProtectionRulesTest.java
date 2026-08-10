package com.powers.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentProtectionRulesTest {
	@Test
	void safeZonesRemainAuthoritativeWhenConsentChecksAreDisabled() {
		assertFalse(ConsentProtectionRules.mayTarget(false, true, false, true));
		assertTrue(ConsentProtectionRules.mayTarget(false, false, false, false));
	}

	@Test
	void configuredConsentIsRequiredOutsideSafeZonesButSelfTargetingRemainsLegal() {
		assertFalse(ConsentProtectionRules.mayTarget(false, false, true, false));
		assertTrue(ConsentProtectionRules.mayTarget(false, false, true, true));
		assertTrue(ConsentProtectionRules.mayTarget(true, true, true, false));
	}
}
