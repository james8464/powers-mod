package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionalAnchorRulesTest {
	@Test void renewalsExtendFromTheCurrentDeadlineAndDiagnosticsAreExact() {
		assertEquals(1400, DimensionalAnchorRules.renewedDeadline(1000, 1200, 200));
		assertEquals(1200, DimensionalAnchorRules.renewedDeadline(1000, 900, 200));
		assertEquals(37, DimensionalAnchorRules.remainingTicks(963, 1000));
		assertEquals(0, DimensionalAnchorRules.remainingTicks(1001, 1000));
	}
}
