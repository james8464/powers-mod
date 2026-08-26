package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the immutable connection-scoped semantic silhouette lifecycle. */
class ClientRankTenSilhouetteStateTest {
	@Test
	void validationHappensBeforeMutationAndStaleEpochOrDimensionIsRejected() {
		ClientRankTenSilhouetteState empty = ClientRankTenSilhouetteState.empty(
				64, 7, "minecraft:overworld");
		ClientRankTenSilhouetteState.Wire invalid = new ClientRankTenSilhouetteState.Wire(
				1, 99, UUID.randomUUID(), "minecraft:overworld", 0, 64, 0,
				0, 0, 0, 1, 40);
		assertEquals(empty, empty.receive(invalid, 5, 7, "minecraft:overworld"));
		ClientRankTenSilhouetteState.Wire valid = wire(1, 40);
		assertEquals(empty, empty.receive(valid, 5, 6, "minecraft:overworld"));
		assertEquals(empty, empty.receive(valid, 5, 7, "minecraft:the_nether"));
		ClientRankTenSilhouetteState.Wire wrongWireDimension = new ClientRankTenSilhouetteState.Wire(
				1, 0, UUID.randomUUID(), "minecraft:the_nether", 0, 64, 0, 0, 0, 0, 1, 40);
		assertEquals(empty, empty.receive(wrongWireDimension, 5, 7, "minecraft:overworld"));
		assertEquals(1, empty.receive(valid, 5, 7, "minecraft:overworld").entries().size());
	}

	@Test
	void capAndExactEventReplayAreBoundedAndIdempotent() {
		ClientRankTenSilhouetteState state = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld");
		for (int id = 1; id <= 64; id++) state = state.receive(wire(id, 40), 10, 3,
				"minecraft:overworld");
		assertEquals(64, state.entries().size());
		assertEquals(state, state.receive(wire(1, 1), 11, 3, "minecraft:overworld"));
		assertEquals(state, state.receive(wire(65, 40), 11, 3, "minecraft:overworld"));
	}

	@Test
	void expiredEventIdIsNeverResurrectedByReplay() {
		ClientRankTenSilhouetteState expired = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld").receive(wire(1, 1), 100, 3,
				"minecraft:overworld").tick();
		assertTrue(expired.entries().isEmpty());
		assertEquals(expired, expired.receive(wire(1, 40), 101, 3, "minecraft:overworld"));
	}

	@Test
	void outOfOrderReceiptThatHasAlreadyExpiredNeverEntersState() {
		ClientRankTenSilhouetteState state = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld").receive(wire(1, 1), 100, 3,
				"minecraft:overworld").tick();
		assertEquals(state, state.receive(wire(2, 1), 100, 3, "minecraft:overworld"));
	}

	@Test
	void outOfRangeFiniteWorldCoordinatesAreRejectedBeforeMutation() {
		ClientRankTenSilhouetteState empty = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld");
		ClientRankTenSilhouetteState.Wire unsafe = new ClientRankTenSilhouetteState.Wire(
				1, 0, UUID.randomUUID(), "minecraft:overworld", 1.0E100, 70, -5,
				30, 5, 0, 41, 40);
		assertEquals(empty, empty.receive(unsafe, 0, 3, "minecraft:overworld"));
	}

	@Test
	void defaultWireLifetimeIsTheAuthoredFortyTicks() {
		assertEquals(40, ClientRankTenSilhouetteState.AUTHORED_LIFETIME_TICKS);
		ClientRankTenSilhouetteState.Wire defaultLifetime = new ClientRankTenSilhouetteState.Wire(
				1, 0, UUID.randomUUID(), "minecraft:overworld", 12, 70, -5,
				30, 5, 0, 41);
		assertEquals(ClientRankTenSilhouetteState.AUTHORED_LIFETIME_TICKS,
				defaultLifetime.lifetimeTicks());
	}

	@Test
	void receiptLocalExpiryIsExactAndSaturatesAtTheMaximumTick() {
		ClientRankTenSilhouetteState state = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld").receive(wire(1, 2), 100, 3,
				"minecraft:overworld");
		assertEquals(102, state.entries().getFirst().expiresAt());
		assertEquals(1, state.tick().entries().size());
		assertEquals(0, state.tick().tick().entries().size());

		ClientRankTenSilhouetteState saturated = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld").receive(wire(2, 80), Long.MAX_VALUE - 2,
				3, "minecraft:overworld");
		assertEquals(Long.MAX_VALUE, saturated.entries().getFirst().expiresAt());
		assertEquals(1, saturated.tick().entries().size());
	}

	@Test
	void resetClearsConnectionOrDimensionStateButReloadPreservesIt() {
		ClientRankTenSilhouetteState state = ClientRankTenSilhouetteState.empty(
				64, 3, "minecraft:overworld").receive(wire(1, 40), 0, 3,
				"minecraft:overworld");
		assertEquals(state, state.rendererResourcesClosed());
		assertEquals(state, state.rendererResourcesRecreated());
		ClientRankTenSilhouetteState dimensionReset = state.reset(3, "minecraft:the_nether");
		assertTrue(dimensionReset.entries().isEmpty());
		assertEquals("minecraft:the_nether", dimensionReset.dimension());
		assertTrue(state.reset(4, "minecraft:overworld").entries().isEmpty());
		assertFalse(state.entries().isEmpty());
	}

	private static ClientRankTenSilhouetteState.Wire wire(long eventId, int lifetime) {
		return new ClientRankTenSilhouetteState.Wire(eventId, 0, UUID.fromString(
				"00000000-0000-0000-0000-000000000001"), "minecraft:overworld",
				12, 70, -5, 30, 5, 0, 41, lifetime);
	}
}
