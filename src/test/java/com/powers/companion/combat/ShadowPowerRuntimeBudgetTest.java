package com.powers.companion.combat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowPowerRuntimeBudgetTest {
	@AfterEach
	void clear() {
		ShadowPowerRuntime.clear();
	}

	@Test
	void plannerCanInspectTheAbsolutePerTickWorkBudgetWithoutSpendingIt() {
		assertTrue(ShadowPowerRuntime.canReserve(ShadowPowerAction.WorkClass.GLOBAL, 41));
		assertTrue(ShadowPowerRuntime.tryReserve(ShadowPowerAction.WorkClass.GLOBAL, 41));
		assertFalse(ShadowPowerRuntime.canReserve(ShadowPowerAction.WorkClass.GLOBAL, 41));
		assertFalse(ShadowPowerRuntime.tryReserve(ShadowPowerAction.WorkClass.GLOBAL, 41));
		assertTrue(ShadowPowerRuntime.canReserve(ShadowPowerAction.WorkClass.GLOBAL, 42));
	}
}
