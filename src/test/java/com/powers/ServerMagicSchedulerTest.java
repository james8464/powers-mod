package com.powers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMagicSchedulerTest {
	@Test
	void stormAdmissionCapsGlobalStateButAllowsOwnerReplacement() {
		assertTrue(ServerMagicScheduler.canAdmitStorm(0, false));
		assertTrue(ServerMagicScheduler.canAdmitStorm(31, false));
		assertFalse(ServerMagicScheduler.canAdmitStorm(32, false));
		assertTrue(ServerMagicScheduler.canAdmitStorm(32, true));
	}
}
