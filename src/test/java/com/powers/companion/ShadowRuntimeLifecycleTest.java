package com.powers.companion;

import com.powers.companion.combat.ShadowCombatController;
import com.powers.companion.combat.ShadowPowerRuntime;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowRuntimeLifecycleTest {
	@Test
	void timeFreezeRetirementIsBodyScopedIdempotentAndPreservesOtherToggles() {
		UUID owner = UUID.randomUUID();
		UUID body = UUID.randomUUID();
		try {
			ShadowPowerRuntime.activate(owner, body, "time_freeze", Long.MAX_VALUE);
			ShadowPowerRuntime.activate(owner, body, "flight", 1200);
			ShadowPowerRuntime.retireTimeFreeze(owner, UUID.randomUUID());
			assertTrue(ShadowPowerRuntime.active(owner, "time_freeze"));
			ShadowPowerRuntime.retireTimeFreeze(owner, body);
			ShadowPowerRuntime.retireTimeFreeze(owner, body);
			assertFalse(ShadowPowerRuntime.active(owner, "time_freeze"));
			assertTrue(ShadowPowerRuntime.active(owner, "flight"));
		} finally {
			ShadowPowerRuntime.forget(owner);
		}
	}

	@Test
	void repeatedCleanupIsIdempotentAcrossEveryTransientOwner() {
		UUID owner = UUID.randomUUID();
		UUID body = UUID.randomUUID();
		ShadowConjurationManager.abandon(owner);
		ShadowConjurationManager.abandon(owner);
		ShadowPowerRuntime.forget(owner);
		ShadowPowerRuntime.forget(owner);
		ShadowCombatController.clearBody(body);
		ShadowCombatController.clearBody(body);
		assertEquals(0, ShadowConjurationManager.activeCount());
		assertEquals(0, ShadowPowerRuntime.diagnostics().owners());
		assertEquals(0, ShadowCombatController.diagnostics().bodies());
	}
}
