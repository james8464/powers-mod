package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelChunkLoaderTest {
	@Test
	void readinessMustArriveWithinTheServerTickDeadline() {
		TravelChunkLoader.Budget budget = new TravelChunkLoader.Budget(80, 200);

		assertTrue(budget.readyInTime(1000, 1080));
		assertFalse(budget.readyInTime(1000, 1081));
	}

	@Test
	void loadingTicketOutlivesTheWaitAndLongestTeleportStorm() {
		assertThrows(IllegalArgumentException.class, () -> new TravelChunkLoader.Budget(80, 120));
		assertTrue(TravelChunkLoader.DEFAULT_BUDGET.ticketTicks()
				>= TravelChunkLoader.DEFAULT_BUDGET.waitTicks() + TravelChunkLoader.MAX_FOLLOWUP_TICKS);
	}
}
