package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		assertEquals(TravelChunkLoader.MAX_FOLLOWUP_TICKS,
				TravelChunkLoader.releaseDelayTicks(TravelChunkLoader.Resolution.READY));
		assertEquals(0, TravelChunkLoader.releaseDelayTicks(TravelChunkLoader.Resolution.TIMEOUT));
	}

	@Test
	void requestStateSettlesExactlyOnceAcrossReadyTimeoutAndReplacement() {
		TravelChunkLoader.RequestState ready = new TravelChunkLoader.RequestState();
		assertTrue(ready.resolve(TravelChunkLoader.Resolution.READY));
		assertFalse(ready.resolve(TravelChunkLoader.Resolution.TIMEOUT));
		assertEquals(TravelChunkLoader.Resolution.READY, ready.resolution());

		TravelChunkLoader.RequestState replaced = new TravelChunkLoader.RequestState();
		assertTrue(replaced.resolve(TravelChunkLoader.Resolution.REPLACED));
		assertFalse(replaced.resolve(TravelChunkLoader.Resolution.READY));
		assertEquals(TravelChunkLoader.Resolution.REPLACED, replaced.resolution());
	}
}
