package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the cooldown boundary used by server-authoritative ranked reactivation. */
class ActivationCooldownsTest {
	@Test
	void activeCooldownBlocksOrdinaryCastsButAllowsAnExplicitReactivation() {
		assertTrue(ActivationCooldowns.blocks(80, false));
		assertFalse(ActivationCooldowns.blocks(80, true));
	}

	@Test
	void readyOrMalformedCooldownNeverBlocksAFirstCast() {
		assertFalse(ActivationCooldowns.blocks(0, false));
		assertFalse(ActivationCooldowns.blocks(-20, false));
	}
}
