package com.powers.companion;

import com.powers.companion.combat.ShadowCombatController;
import com.powers.companion.combat.ShadowPowerRuntime;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowRuntimeLifecycleTest {
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
