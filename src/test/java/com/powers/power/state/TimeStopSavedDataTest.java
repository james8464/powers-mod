package com.powers.power.state;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeStopSavedDataTest {
	@Test
	void ownedClockJournalRoundTripsAndCanBeClearedIdempotently() {
		TimeStopSavedData original = new TimeStopSavedData();
		original.activate("00000000-0000-0000-0000-000000000001", "INNATE", 9_000L, "");

		JsonElement encoded = TimeStopSavedData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		TimeStopSavedData decoded = TimeStopSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertTrue(decoded.snapshot().active());
		assertEquals(original.snapshot(), decoded.snapshot());

		decoded.clear();
		decoded.clear();
		assertFalse(decoded.snapshot().active());
	}
}
