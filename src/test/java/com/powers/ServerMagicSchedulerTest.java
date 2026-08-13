package com.powers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMagicSchedulerTest {
	@Test
	void stormAdmissionCapsGlobalStateButAllowsOwnerReplacement() {
		assertTrue(ServerMagicScheduler.canAdmitStorm(0, false));
		assertTrue(ServerMagicScheduler.canAdmitStorm(31, false));
		assertFalse(ServerMagicScheduler.canAdmitStorm(32, false));
		assertTrue(ServerMagicScheduler.canAdmitStorm(32, true));
	}

	@Test
	void visualStormBoltsOrbitOutsideTheFirstPersonCamera() {
		var first = ServerMagicScheduler.stormBoltOffset(0);
		var second = ServerMagicScheduler.stormBoltOffset(2);

		assertTrue(first.horizontalDistance() >= 7.5);
		assertTrue(first.horizontalDistance() <= 10.5);
		assertNotEquals(first, second);
	}
}
