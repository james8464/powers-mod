package com.powers.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTickCadenceTest {
	@Test
	void derivesAllPlayerWorkCadencesFromOneTick() {
		PlayerTickCadence ordinary = PlayerTickCadence.at(19);
		assertFalse(ordinary.fiveTick());
		assertFalse(ordinary.second());
		assertFalse(ordinary.passiveRefresh());

		PlayerTickCadence second = PlayerTickCadence.at(20);
		assertTrue(second.fiveTick());
		assertTrue(second.second());
		assertFalse(second.passiveRefresh());

		assertTrue(PlayerTickCadence.at(100).passiveRefresh());
		assertFalse(PlayerTickCadence.at(80).passiveRefresh());
	}

	@Test
	void oneCoordinatorPassScalesLinearlyAtTwentyAndFiftyPlayers() {
		assertEquals(20, PlayerTickCadence.playerVisits(20));
		assertEquals(50, PlayerTickCadence.playerVisits(50));
		assertEquals(0, PlayerTickCadence.playerVisits(-1));
	}
}
