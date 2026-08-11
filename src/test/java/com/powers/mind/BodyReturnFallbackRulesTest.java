package com.powers.mind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyReturnFallbackRulesTest {
	@Test
	void activeAnchorsNeverFallBackToUncheckedTeleportation() {
		assertFalse(BodyReturnFallbackRules.mayUseLegacyFallback(true, false));
		assertFalse(BodyReturnFallbackRules.mayUseLegacyFallback(true, true));
	}

	@Test
	void onlyMissingLegacyAnchorsCanUseRecordedFallback() {
		assertTrue(BodyReturnFallbackRules.mayUseLegacyFallback(false, false));
		assertFalse(BodyReturnFallbackRules.mayUseLegacyFallback(false, true));
	}
}
