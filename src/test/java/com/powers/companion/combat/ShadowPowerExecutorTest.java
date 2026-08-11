package com.powers.companion.combat;

import com.powers.power.PowerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShadowPowerExecutorTest {
	@BeforeAll
	static void initializePowers() {
		PowerRegistry.initialize();
	}

	@Test
	void allTwentySixActionsHaveNamedNonFallbackHandlers() {
		EnumSet<ShadowPowerExecutor.Handler> handlers = EnumSet.noneOf(ShadowPowerExecutor.Handler.class);
		for (ShadowPowerAction action : ShadowPowerCatalogue.actions()) {
			ShadowPowerExecutor.Handler handler = ShadowPowerExecutor.handler(action.id());
			assertFalse(handler == ShadowPowerExecutor.Handler.UNSUPPORTED, action.id());
			handlers.add(handler);
		}
		assertEquals(EnumSet.complementOf(EnumSet.of(ShadowPowerExecutor.Handler.UNSUPPORTED)), handlers);
	}
}
