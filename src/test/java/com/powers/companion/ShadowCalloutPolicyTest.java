package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCalloutPolicyTest {
	@Test
	void criticalWarningsSurviveCriticalOnlyModeAndChatterCooldown() {
		assertTrue(ShadowCalloutPolicy.shouldEmit(ShadowCalloutPolicy.Kind.OWNER_DANGER,
				ShadowCalloutMode.CRITICAL_ONLY, 1_000, 0, 1_000));
		assertFalse(ShadowCalloutPolicy.shouldEmit(ShadowCalloutPolicy.Kind.ENGAGE,
				ShadowCalloutMode.CRITICAL_ONLY, 1_000, 0, 0));
		assertFalse(ShadowCalloutPolicy.shouldEmit(ShadowCalloutPolicy.Kind.OWNER_DANGER,
				ShadowCalloutMode.VERBOSE, 1_050, 1_000, 0));
		assertTrue(ShadowCalloutPolicy.shouldEmit(ShadowCalloutPolicy.Kind.OWNER_DANGER,
				ShadowCalloutMode.NORMAL, 1_100, 1_000, 1_100));
	}

	@Test
	void normalAndVerboseChatterUseDifferentGlobalFrequencyCaps() {
		assertFalse(ShadowCalloutPolicy.shouldEmit(ShadowCalloutPolicy.Kind.ENGAGE,
				ShadowCalloutMode.NORMAL, 1_100, 0, 1_000));
		assertTrue(ShadowCalloutPolicy.shouldEmit(ShadowCalloutPolicy.Kind.ENGAGE,
				ShadowCalloutMode.VERBOSE, 1_100, 0, 1_000));
		for (ShadowCalloutPolicy.Kind kind : ShadowCalloutPolicy.Kind.values()) {
			assertFalse(ShadowCalloutPolicy.spoken(kind).isBlank());
			assertFalse(ShadowCalloutPolicy.visual(kind).isBlank());
		}
	}
}
