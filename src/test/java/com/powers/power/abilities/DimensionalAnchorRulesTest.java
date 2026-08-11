package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionalAnchorRulesTest {
	@Test void renewalsExtendFromTheCurrentDeadlineAndDiagnosticsAreExact() {
		assertEquals(1400, DimensionalAnchorAbility.renewedDeadline(1000, 1200, 200));
		assertEquals(1200, DimensionalAnchorAbility.renewedDeadline(1000, 900, 200));
		assertEquals(37, DimensionalAnchorAbility.remainingTicks(963, 1000));
		assertEquals(0, DimensionalAnchorAbility.remainingTicks(1001, 1000));
	}
}
