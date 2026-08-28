package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientCastingPoseStateTest {
	private static final UUID ONE = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID TWO = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final ClientCastingPoseState.HandlerStamp STAMP =
			new ClientCastingPoseState.HandlerStamp(3, 7);
	private static final ClientCastingPoseState.WorldIdentity WORLD =
			new ClientCastingPoseState.WorldIdentity(3, 7);

	@Test
	void lateReceiptResolvesFromAuthoritativeStartTime() {
		var state = new ClientCastingPoseState();
		assertTrue(state.accept(wire(12, ONE, 1, 20, 20), STAMP, WORLD,
				entity(12, ONE), 30));
		assertEquals(0.5, state.resolve(ONE, 30).orElseThrow().progress());
	}

	@Test
	void staleEpochUuidMismatchFutureSkewAndExpiredReceiptFailClosed() {
		var state = new ClientCastingPoseState();
		assertFalse(state.accept(wire(12, ONE, 1, 20, 20),
				new ClientCastingPoseState.HandlerStamp(2, 7), WORLD, entity(12, ONE), 20));
		assertFalse(state.accept(wire(12, ONE, 1, 20, 20), STAMP, WORLD,
				entity(12, TWO), 20));
		assertFalse(state.accept(wire(12, ONE, 1, 26, 20), STAMP, WORLD,
				entity(12, ONE), 20));
		assertFalse(state.accept(wire(12, ONE, 1, 0, 20), STAMP, WORLD,
				entity(12, ONE), 20));
		assertFalse(state.accept(wire(12, ONE, 1, 20, 20), STAMP, WORLD,
				new ClientCastingPoseState.EntityIdentity(12, ONE, false), 20));
		assertTrue(state.entries().isEmpty());
	}

	@Test
	void replayAndOlderSequenceCannotReplaceNewerPose() {
		var state = new ClientCastingPoseState();
		assertTrue(state.accept(wire(12, ONE, 2, 20, 20), STAMP, WORLD, entity(12, ONE), 20));
		assertFalse(state.accept(wire(12, ONE, 2, 21, 20), STAMP, WORLD, entity(12, ONE), 21));
		assertFalse(state.accept(wire(12, ONE, 1, 21, 20), STAMP, WORLD, entity(12, ONE), 21));
		assertEquals(2L, state.entries().get(ONE).sequence());
	}

	@Test
	void entityIdReuseCannotAnimateDifferentUuidAndWorldResetClearsState() {
		var state = new ClientCastingPoseState();
		assertFalse(state.accept(wire(12, ONE, 1, 20, 20), STAMP, WORLD, entity(12, TWO), 20));
		assertTrue(state.accept(wire(12, ONE, 1, 20, 20), STAMP, WORLD, entity(12, ONE), 20));
		state.reset(new ClientCastingPoseState.WorldIdentity(3, 8));
		assertTrue(state.entries().isEmpty());
		assertFalse(state.accept(wire(12, ONE, 2, 21, 20), STAMP,
				new ClientCastingPoseState.WorldIdentity(3, 8), entity(12, ONE), 21));
	}

	@Test
	void capacityEvictsEarliestFinishingEntry() {
		var state = new ClientCastingPoseState();
		for (int index = 0; index < ClientCastingPoseState.MAX_ENTRIES; index++) {
			UUID uuid = new UUID(9, index + 1L);
			assertTrue(state.accept(wire(index, uuid, 1, 20, index == 0 ? 5 : 100),
					STAMP, WORLD, entity(index, uuid), 20));
		}
		assertTrue(state.accept(wire(200, TWO, 1, 20, 100), STAMP, WORLD,
				entity(200, TWO), 20));
		assertEquals(ClientCastingPoseState.MAX_ENTRIES, state.entries().size());
		assertFalse(state.entries().containsKey(new UUID(9, 1)));
		assertTrue(state.entries().containsKey(TWO));
	}

	private static ClientCastingPoseState.Wire wire(int entityId, UUID uuid, long sequence,
			long start, int duration) {
		return new ClientCastingPoseState.Wire(entityId, uuid, sequence, CastingPose.PROJECT,
				CastingStyle.RADIANT, CastingHand.RIGHT, start, duration);
	}

	private static ClientCastingPoseState.EntityIdentity entity(int id, UUID uuid) {
		return new ClientCastingPoseState.EntityIdentity(id, uuid, true);
	}
}
