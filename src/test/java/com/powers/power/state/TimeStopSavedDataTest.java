package com.powers.power.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.powers.time.ControlTick;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeStopSavedDataTest {
	@Test
	void schemaTwoLeaseJournalRoundTripsAndCanBeClearedIdempotently() {
		TimeStopSavedData original = new TimeStopSavedData();
		original.activate(TimeStopLeaseRules.create(17L,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				TimeStopLeaseSource.CRYSTAL, ControlTick.at(2_000L), 1_200L, null));

		JsonElement encoded = TimeStopSavedData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		TimeStopSavedData decoded = TimeStopSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertTrue(decoded.snapshot().active());
		assertEquals(2, decoded.snapshot().schemaVersion());
		assertEquals(17L, decoded.snapshot().leaseToken());
		assertEquals(2_000L, decoded.snapshot().acquiredControlTick());
		assertEquals(original.snapshot(), decoded.snapshot());

		decoded.clear();
		decoded.clear();
		assertFalse(decoded.snapshot().active());
	}

	@Test
	void schemaOneJournalDecodesOnlyAsStaleRecoveryAuthority() {
		JsonElement schemaOne = JsonParser.parseString("""
				{"active":true,"owner":"00000000-0000-0000-0000-000000000001",
				 "source":"INNATE","deadline":9000,"shadow_body":""}
				""");
		TimeStopSavedData decoded = TimeStopSavedData.CODEC.parse(JsonOps.INSTANCE, schemaOne).getOrThrow();
		assertTrue(decoded.snapshot().active());
		assertEquals(1, decoded.snapshot().schemaVersion());
		assertEquals(0L, decoded.snapshot().leaseToken());
		assertTrue(decoded.snapshot().staleRecoveryOnly());
	}
}
