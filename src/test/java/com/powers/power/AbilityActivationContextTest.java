package com.powers.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AbilityActivationContextTest {
	@Test
	void scopedCooldownIsVisibleAndAlwaysRestored() {
		assertNull(AbilityActivationContext.cooldownOverride());
		int value = AbilityActivationContext.withCooldown(80, () -> {
			assertEquals(80, AbilityActivationContext.cooldownOverride());
			return 4;
		});
		assertEquals(4, value);
		assertNull(AbilityActivationContext.cooldownOverride());
	}

	@Test
	void nestedScopesRestoreTheOuterCast() {
		AbilityActivationContext.withCooldown(40, () -> {
			assertEquals(0, AbilityActivationContext.withCooldown(0,
					AbilityActivationContext::cooldownOverride));
			assertEquals(40, AbilityActivationContext.cooldownOverride());
			return true;
		});
	}
}
