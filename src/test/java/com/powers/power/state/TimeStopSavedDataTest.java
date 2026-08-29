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
				 "source":"INNATE","deadline":9223372036854775807,"shadow_body":""}
				""");
		TimeStopSavedData decoded = TimeStopSavedData.CODEC.parse(JsonOps.INSTANCE, schemaOne).getOrThrow();
		assertTrue(decoded.snapshot().active());
		assertEquals(1, decoded.snapshot().schemaVersion());
		assertEquals(0L, decoded.snapshot().leaseToken());
		assertTrue(decoded.snapshot().staleRecoveryOnly());
		assertEquals(TimeStopSavedData.RecoveryDecision.CLEAR_CONFIRMED,
				decoded.snapshot().recoveryDecision());
	}

	@Test
	void malformedActiveJournalsAreClearedWithoutAuthorizingVanillaThaw() {
		String owner = "00000000-0000-0000-0000-000000000001";
		String body = "00000000-0000-0000-0000-000000000002";
		for (TimeStopSavedData.Snapshot malformed : new TimeStopSavedData.Snapshot[] {
				new TimeStopSavedData.Snapshot(2, true, 1, "", "INNATE", 4, Long.MAX_VALUE, ""),
				new TimeStopSavedData.Snapshot(2, true, 1, "not-a-uuid", "INNATE", 4, Long.MAX_VALUE, ""),
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "UNKNOWN", 4, Long.MAX_VALUE, ""),
				new TimeStopSavedData.Snapshot(2, true, 0, owner, "INNATE", 4, Long.MAX_VALUE, ""),
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "CRYSTAL", 8, 7, ""),
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "CRYSTAL", 8, 1_209, ""),
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "CRYSTAL", -1, 7, ""),
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "INNATE", 4, Long.MAX_VALUE, body),
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "SHADOW", 4, Long.MAX_VALUE, ""),
				new TimeStopSavedData.Snapshot(3, true, 1, owner, "SHADOW", 4, Long.MAX_VALUE, body)
		}) {
			assertEquals(TimeStopSavedData.RecoveryDecision.CLEAR_ONLY,
					malformed.recoveryDecision(), malformed.toString());
		}
		for (String source : new String[] {"INNATE", "SHADOW"}) {
			String shadowBody = source.equals("SHADOW") ? body : "";
			assertEquals(TimeStopSavedData.RecoveryDecision.CLEAR_ONLY,
					new TimeStopSavedData.Snapshot(1, true, 0, owner, source,
							0, 9_000, shadowBody).recoveryDecision());
		}
	}

	@Test
	void evenValidPowersJournalsRetireWithoutThawingAnAmbiguousStartupClock() {
		String owner = "00000000-0000-0000-0000-000000000001";
		String body = "00000000-0000-0000-0000-000000000002";
		assertEquals(TimeStopSavedData.RecoveryDecision.NONE,
				new TimeStopSavedData().snapshot().recoveryDecision());
		for (TimeStopSavedData.Snapshot valid : new TimeStopSavedData.Snapshot[] {
				new TimeStopSavedData.Snapshot(2, true, 1, owner, "INNATE", 4, Long.MAX_VALUE, ""),
				new TimeStopSavedData.Snapshot(2, true, 2, owner, "CRYSTAL", 4, 1_204, ""),
				new TimeStopSavedData.Snapshot(2, true, 3, owner, "SHADOW", 4, Long.MAX_VALUE, body)
		}) {
			assertEquals(TimeStopSavedData.RecoveryDecision.CLEAR_CONFIRMED,
					valid.recoveryDecision(), valid.toString());
		}
	}

}
