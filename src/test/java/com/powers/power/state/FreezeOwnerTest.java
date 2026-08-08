package com.powers.power.state;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FreezeOwnerTest {
	@Test
	void eachPowerGetsAStableDistinctOwnershipToken() {
		UUID player = UUID.randomUUID();
		assertEquals(FreezeOwner.token("space_time", player), FreezeOwner.token("space_time", player));
		assertNotEquals(FreezeOwner.token("space_time", player), FreezeOwner.token("time_freeze", player));
	}
}
