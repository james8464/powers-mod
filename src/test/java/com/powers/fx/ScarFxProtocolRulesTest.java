package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarFxProtocolRulesTest {
	@Test
	void wireHasExactEightValidatedFields() {
		assertEquals(Set.of("operation", "position", "face", "impact", "material",
				"visualSeed", "generation", "leaseTicks"), Arrays.stream(
						ScarFxProtocolRules.Wire.class.getRecordComponents())
						.map(java.lang.reflect.RecordComponent::getName).collect(Collectors.toSet()));
		assertTrue(ScarFxProtocolRules.validate(wire(0, 42, 1, 0, 0, 9, 1, 40)));
		assertFalse(ScarFxProtocolRules.validate(wire(2, 42, 1, 0, 0, 9, 1, 40)));
		assertFalse(ScarFxProtocolRules.validate(wire(0, 42, 6, 0, 0, 9, 1, 40)));
		assertFalse(ScarFxProtocolRules.validate(wire(0, 42, 1, 5, 0, 9, 1, 40)));
		assertFalse(ScarFxProtocolRules.validate(wire(0, 42, 1, 0, 6, 9, 1, 40)));
		assertFalse(ScarFxProtocolRules.validate(wire(0, 42, 1, 0, 0, 9, 0, 40)));
		assertFalse(ScarFxProtocolRules.validate(wire(0, 42, 1, 0, 0, 9, 1, 0)));
		assertFalse(ScarFxProtocolRules.validate(wire(0, 42, 1, 0, 0, 9, 1, 1_201)));
		assertTrue(ScarFxProtocolRules.validate(reset(7)));
		assertFalse(ScarFxProtocolRules.validate(new ScarFxProtocolRules.Wire(
				ScarFxProtocolRules.RESET_DIMENSION, 1, 0, 0, 0, 0, 7, 1)));
	}

	@Test
	void generationNotSeedOrTickOrdersSameKeyRecreationAndReorder() {
		var state = ClientVisualScarState.empty(2_048, 7);
		var first = wire(0, 42, 1, 0, 0, 9, 1, 40);
		state = state.receive(first, 5_000, 7);
		assertEquals(state, state.receive(first, 5_006, 7));
		var sameTickSameSeed = wire(0, 42, 1, 1, 2, 9, 2, 50);
		state = state.receive(sameTickSameSeed, 5_000, 7);
		assertEquals(2, state.get(42, 1).orElseThrow().generation());
		var differentSeedOlder = wire(0, 42, 1, 4, 5, 99, 1, 60);
		assertEquals(state, state.receive(differentSeedOlder, 5_006, 7));
		assertEquals(state, state.receive(first, 5_006, 7));
	}

	@Test
	void exactRemoveWinsButDelayedOldRemoveCannotEraseReplacement() {
		var state = ClientVisualScarState.empty(2_048, 7)
				.receive(wire(0, 42, 1, 0, 0, 1, 1, 40), 100, 7)
				.receive(wire(0, 42, 1, 1, 1, 2, 2, 40), 106, 7);
		assertEquals(1, state.receive(wire(1, 42, 1, 0, 0, 1, 1, 1), 112, 7).size());
		assertEquals(0, state.receive(wire(1, 42, 1, 1, 1, 2, 2, 1), 112, 7).size());
	}

	@Test
	void counterExhaustionPermanentlyDisablesAdmissionsUntilActualServerRestart() {
		var last = ScarFxProtocolRules.advanceGeneration(Long.MAX_VALUE - 1, 2_048, 7, 12);
		assertEquals(Long.MAX_VALUE, last.nextGeneration());
		assertTrue(last.newAdmissionsAllowed());
		assertEquals(7, last.serverEpoch());
		assertEquals(12, last.connectionEpoch());
		var reset = ScarFxProtocolRules.advanceGeneration(Long.MAX_VALUE, 2_048, 7, 12);
		assertEquals(ScarFxProtocolRules.GenerationAction.DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART,
				reset.action());
		assertFalse(reset.newAdmissionsAllowed());
		assertTrue(reset.existingExpiryAndRemovesAllowed());
		assertFalse(reset.hasNextGeneration());
		assertEquals(reset, ScarFxProtocolRules.advanceDisabledGeneration(reset, 2_048));
		assertThrows(IllegalArgumentException.class,
				() -> ScarFxProtocolRules.serverRestart(last, 8, 13));
		assertThrows(IllegalArgumentException.class,
				() -> ScarFxProtocolRules.serverRestart(reset, 7, 13));
		assertThrows(IllegalArgumentException.class,
				() -> ScarFxProtocolRules.serverRestart(reset, 6, 13));
		assertThrows(IllegalArgumentException.class,
				() -> ScarFxProtocolRules.serverRestart(reset, 8, 12));
		var restarted = ScarFxProtocolRules.serverRestart(reset, 8, 13);
		assertEquals(1, restarted.nextGeneration());
		assertEquals(8, restarted.serverEpoch());
		assertEquals(13, restarted.connectionEpoch());
		assertTrue(restarted.clientConnectionResetRequired());
		assertTrue(ScarFxProtocolRules.newerUnsigned(2, 1));
		assertFalse(ScarFxProtocolRules.newerUnsigned(1, 2));
	}

	@Test
	void clientCapAllowsExistingUpdateButRejectsNewKeyAndInvalidBeforeMutation() {
		var empty = ClientVisualScarState.empty(2_048, 7);
		var state = empty;
		for (int key = 0; key < 2_048; key++) {
			state = state.receive(wire(0, key, 1, 0, 0, key, 1, 40), 100, 7);
		}
		assertEquals(2_048, state.size());
		assertEquals(0, empty.size());
		var atCap = state;
		var rejected = atCap.receiveObserved(
				wire(0, 9_999, 1, 0, 0, 1, 1, 40), 101, 7);
		assertEquals(ClientVisualScarState.ReceiveOutcome.REJECTED_CAP, rejected.outcome());
		assertTrue(rejected.needsAuthoritativeResync());
		assertEquals(atCap, rejected.state());
		var updated = atCap.receive(wire(0, 42, 1, 1, 1, 2, 2, 40), 101, 7);
		assertEquals(2_048, updated.size());
		assertEquals(2, updated.get(42, 1).orElseThrow().generation());
		assertEquals(updated, updated.receive(wire(9, 42, 1, 1, 1, 3, 3, 40), 102, 7));

		var removed = atCap.receiveObserved(wire(1, 42, 1, 0, 0, 42, 1, 1), 102, 7);
		assertEquals(ClientVisualScarState.ReceiveOutcome.APPLIED, removed.outcome());
		assertFalse(removed.needsAuthoritativeResync());
		var recreated = removed.state().receiveObserved(
				wire(0, 42, 1, 1, 1, 99, 2, 40), 103, 7);
		assertEquals(ClientVisualScarState.ReceiveOutcome.APPLIED, recreated.outcome());
		assertEquals(2, recreated.state().get(42, 1).orElseThrow().generation());
		var delayedOldRemove = recreated.state().receiveObserved(
				wire(1, 42, 1, 0, 0, 42, 1, 1), 104, 7);
		assertEquals(ClientVisualScarState.ReceiveOutcome.IGNORED_REPLAY,
				delayedOldRemove.outcome());
		assertFalse(delayedOldRemove.needsAuthoritativeResync());
		assertEquals(2, delayedOldRemove.state().get(42, 1).orElseThrow().generation());
	}

	@Test
	void handlerCapturedBeforeConnectionOrDimensionResetIsIgnored() {
		var old = new ClientVisualScarState.HandlerStamp(7, "minecraft:overworld");
		var current = new ClientVisualScarState.HandlerStamp(8, "minecraft:the_nether");
		var state = ClientVisualScarState.empty(2_048, 8);
		assertEquals(state, state.receiveFrom(old, current,
				wire(0, 42, 1, 0, 0, 1, 1, 40), 100));
		assertEquals(1, state.receiveFrom(current, current,
				wire(0, 42, 1, 0, 0, 1, 1, 40), 100).size());
	}

	@Test
	void remainingServerLeaseMapsToReceiptLocalLifecycleTickAcrossDelayAndFreeze() {
		assertEquals(46, ScarFxProtocolRules.remainingLease(200, 154));
		assertEquals(10, ScarFxProtocolRules.remainingLease(Long.MAX_VALUE, Long.MAX_VALUE - 10));
		assertEquals(1_200, ScarFxProtocolRules.remainingLease(Long.MAX_VALUE, 0));
		assertThrows(IllegalArgumentException.class,
				() -> ScarFxProtocolRules.remainingLease(100, -1));
		assertThrows(IllegalArgumentException.class,
				() -> ScarFxProtocolRules.remainingLease(-1, 0));
		var state = ClientVisualScarState.empty(2_048, 7)
				.receive(wire(0, 42, 1, 0, 0, 1, 1, 46), 10_006, 7);
		assertEquals(10_052, state.get(42, 1).orElseThrow().localExpiresAt());
		for (int index = 0; index < 45; index++) state = state.tickLifecycle(true);
		assertEquals(1, state.size());
		state = state.tickLifecycle(true);
		assertEquals(0, state.size());
		var saturatedClock = ClientVisualScarState.empty(2_048, 7)
				.receive(wire(0, 1, 1, 0, 0, 1, 1, 40), Long.MAX_VALUE - 2, 7);
		assertEquals(Long.MAX_VALUE, saturatedClock.get(1, 1).orElseThrow().localExpiresAt());
		assertThrows(IllegalArgumentException.class,
				() -> new ClientVisualScarState.HandlerStamp(-1, "minecraft:overworld"));
	}

	@Test
	void reconnectAndDimensionResetEpochButResourceReloadPreservesSemanticState() {
		var state = ClientVisualScarState.empty(2_048, 7)
				.receive(wire(0, 42, 1, 0, 0, 1, 1, 40), 100, 7);
		assertEquals(state, state.rendererResourcesClosed());
		assertEquals(state, state.rendererResourcesRecreated());
		assertEquals(0, state.reset(ClientVisualScarState.Reset.DIMENSION_CHANGE, 8).size());
		assertEquals(0, state.reset(ClientVisualScarState.Reset.CONNECTION_EPOCH, 9).size());
		assertEquals(1, ClientVisualScarState.empty(2_048, 9)
				.receive(wire(0, 42, 1, 0, 0, 1, 1, 40), 0, 9).size());
		assertEquals(Set.of(ClientVisualScarState.Reset.DIMENSION_CHANGE,
				ClientVisualScarState.Reset.CONNECTION_EPOCH), Set.of(ClientVisualScarState.Reset.values()));
	}

	@Test
	void resetDimensionClearsCurrentSessionStateBeforeSnapshotCreates() {
		var populated = ClientVisualScarState.empty(2_048, 7)
				.receive(wire(0, 42, 1, 0, 0, 1, 1, 40), 100, 7)
				.receive(wire(0, 43, 1, 0, 0, 2, 1, 40), 100, 7);
		var cleared = populated.receiveObserved(reset(9), 101, 7);
		assertEquals(ClientVisualScarState.ReceiveOutcome.APPLIED_RESET, cleared.outcome());
		assertEquals(0, cleared.state().size());
		assertFalse(cleared.needsAuthoritativeResync());
		assertEquals(cleared.state(), cleared.state().receiveObserved(reset(9), 102, 8).state());

		var oldDimension = new ClientVisualScarState.HandlerStamp(7, "minecraft:overworld");
		var currentDimension = new ClientVisualScarState.HandlerStamp(7, "minecraft:the_nether");
		assertEquals(populated, populated.receiveFrom(oldDimension, currentDimension, reset(10), 103));
		assertEquals(0, populated.receiveFrom(currentDimension, currentDimension, reset(10), 103).size());
	}

	private static ScarFxProtocolRules.Wire wire(int operation, long position, int face,
			int impact, int material, int seed, long generation, int lease) {
		return new ScarFxProtocolRules.Wire(operation, position, face, impact, material,
				seed, generation, lease);
	}

	private static ScarFxProtocolRules.Wire reset(long deliveryGeneration) {
		return new ScarFxProtocolRules.Wire(ScarFxProtocolRules.RESET_DIMENSION,
				0, 0, 0, 0, 0, deliveryGeneration, 1);
	}
}
