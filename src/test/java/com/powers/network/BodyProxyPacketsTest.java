package com.powers.network;

import com.powers.mind.BodySnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyProxyPacketsTest {
	@Test
	void boundedJsonTransportRoundTripsTheSnapshotCodec() {
		BodySnapshot snapshot = new BodySnapshot(
				new BodySnapshot.Profile(UUID.fromString("00000000-0000-0000-0000-000000000222"),
						"skin-owner", 127, List.of("minecraft:air#0#0")),
				new BodySnapshot.PoseState("standing", "right", "", 10, -5, 12, 8,
						1, 0, 0, 0),
				new BodySnapshot.AnimationState(false, "main_hand", 0, 0,
						0, 0, false, "main_hand", 0));

		String json = BodyProxyPackets.encode(snapshot);

		assertEquals(snapshot, BodyProxyPackets.decode(json).orElseThrow());
		assertTrue(json.length() < BodyProxyPackets.MAX_SNAPSHOT_CHARS);
	}

	@Test
	void invalidAndOversizedPayloadsAreRejectedWithoutClientState() {
		assertTrue(BodyProxyPackets.decode("not-json").isEmpty());
		assertThrows(IllegalArgumentException.class,
				() -> new BodyProxyPackets.BodySnapshotPayload(1,
						"x".repeat(BodyProxyPackets.MAX_SNAPSHOT_CHARS + 1)));
	}
}
