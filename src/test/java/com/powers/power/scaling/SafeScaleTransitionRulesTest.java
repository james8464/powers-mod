package com.powers.power.scaling;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeScaleTransitionRulesTest {
	@Test
	void previewOrderIsStableFiniteAndNeverInventsAScale() {
		assertEquals(List.of(10.0, 4.0, 1.0),
				SafeScaleTransitionRules.candidates(List.of(10.0, 4.0, 1.0)));
		assertEquals(List.of(1.0),
				SafeScaleTransitionRules.candidates(List.of(Double.NaN, -1.0, 1.0, 1.0)));
		assertTrue(SafeScaleTransitionRules.validLookahead(1.5));
		assertEquals(0.0, SafeScaleTransitionRules.sanitizeLookahead(Double.POSITIVE_INFINITY));
	}
}
