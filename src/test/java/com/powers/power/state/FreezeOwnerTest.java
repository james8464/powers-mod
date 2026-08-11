package com.powers.power.state;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FreezeOwnerTest {
	@Test
	void eachPowerGetsAStableDistinctOwnershipToken() {
		UUID player = UUID.randomUUID();
		assertEquals(FreezeOwner.token("chrono_stop", player), FreezeOwner.token("chrono_stop", player));
		assertNotEquals(FreezeOwner.token("chrono_stop", player), FreezeOwner.token("time_freeze", player));
	}
}
