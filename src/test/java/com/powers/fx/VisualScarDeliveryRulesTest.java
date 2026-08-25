package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualScarDeliveryRulesTest {
	@Test
	void lostSnapshotRowRetriesThatRowWithoutRestartingThePass() {
		var observer = session(1, 10, "overworld", 1);
		var authority = VisualScarDeliveryRules.authoritativeSnapshot(7, List.of(
				row("overworld", wire(0, 1, 11, 1)),
				row("overworld", wire(0, 2, 22, 1))));
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.markNeedsResync(observer, authority.cursor());
		var reset = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		pending = pending.recordSendSuccess(reset, observer);
		var lost = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();

		pending = pending.recordSendFailure(lost,
				VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, observer);
		var retry = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();

		assertEquals(ScarFxProtocolRules.CREATE_OR_UPDATE, retry.payload().operation());
		assertEquals(lost.payload().position(), retry.payload().position());
		assertEquals(lost.deliveryGeneration(), retry.deliveryGeneration());
	}

	@Test
	void resetBarrierRecoversLostRemoveAndTombstoneSaturationBeforeActiveCreates() {
		var observer = session(1, 10, "overworld", 1);
		var authority = VisualScarDeliveryRules.authoritativeSnapshot(7, List.of(
				row("overworld", wire(0, 2, 2, 1)),
				row("overworld", wire(0, 3, 3, 1))));
		var client = ClientVisualScarState.empty(2_048, 1)
				.receive(wire(0, 1, 1, 1), 0, 1)
				.receive(wire(0, 2, 2, 1), 0, 1);
		var pending = VisualScarDeliveryRules.empty(2, 3)
				.offer(observer, wire(1, 10, 10, 1))
				.offer(observer, wire(1, 11, 11, 1))
				.offer(session(2, 20, "overworld", 1), wire(1, 12, 12, 1))
				.markNeedsResync(observer, authority.cursor());

		var first = pending.drainFair(256, 192, 64, List.of(observer), authority);
		assertEquals(1, first.sent().size());
		var reset = first.sent().getFirst();
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, reset.payload().operation());
		assertTrue(reset.resync());
		assertEquals(reset.deliveryGeneration(), reset.payload().generation());
		assertTrue(first.remaining().drainFair(256, 192, 64,
				List.of(observer), authority).sent().isEmpty());

		pending = first.remaining().recordSendFailure(reset,
				VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, observer);
		var retry = pending.drainFair(256, 192, 64, List.of(observer), authority);
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, retry.sent().getFirst().payload().operation());
		assertEquals(reset.deliveryGeneration(), retry.sent().getFirst().deliveryGeneration());
		pending = retry.remaining().recordSendSuccess(retry.sent().getFirst(), observer);
		client = client.receive(retry.sent().getFirst().payload(), 1, 1);
		assertEquals(0, client.size());

		for (int round = 0; round < 8 && pending.needsResync(observer); round++) {
			var drained = pending.drainFair(64, 0, 64, List.of(observer), authority);
			for (var sent : drained.sent()) {
				assertEquals(ScarFxProtocolRules.CREATE_OR_UPDATE, sent.payload().operation());
				client = client.receive(sent.payload(), 2 + round, 1);
				pending = drained.remaining().recordSendSuccess(sent, observer);
			}
		}
		assertEquals(authority.generations("minecraft:overworld"), client.generations());
		assertFalse(pending.needsResync(observer));
	}

	@Test
	void activeResyncIsIdempotentAndDelayedResetRemainsCurrent() {
		var observer = session(1, 10, "overworld", 1);
		var authority = VisualScarDeliveryRules.authoritativeSnapshot(1, List.of(
				row("overworld", wire(0, 1, 1, 1))));
		var pending = VisualScarDeliveryRules.empty(2, 3)
				.markNeedsResync(observer, authority.cursor());
		var oldReset = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		for (int overflow = 0; overflow < 100; overflow++) {
			pending = pending.offer(observer, wire(1, 100 + overflow, overflow, 1))
					.markNeedsResync(observer, authority.cursor());
		}
		assertEquals(oldReset.deliveryGeneration(), pending.deliveryGeneration(observer));
		assertTrue(pending.guardCurrent(oldReset, observer));
		assertTrue(pending.drainFair(1, 0, 1, List.of(observer), authority).sent().isEmpty());

		pending = pending.recordSendSuccess(oldReset, observer);
		var snapshotSend = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		pending = pending.recordSendSuccess(snapshotSend, observer);
		var followupReset = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, followupReset.payload().operation());
		assertTrue(followupReset.deliveryGeneration() > oldReset.deliveryGeneration());
		assertFalse(pending.guardCurrent(oldReset, observer));
		pending = pending.recordSendSuccess(followupReset, observer);
		var followupSnapshot = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		pending = pending.recordSendSuccess(followupSnapshot, observer);
		pending = pending.drainFair(1, 0, 1, List.of(observer), authority).remaining();
		assertFalse(pending.needsResync(observer));

		var liveOwner = session(2, 20, "overworld", 1);
		var livePending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.offer(liveOwner, wire(0, 9, 9, 1));
		var delayedLive = livePending.drain(1, List.of(liveOwner)).sent().getFirst();
		assertTrue(livePending.guardCurrent(delayedLive, liveOwner));
		livePending = livePending.markNeedsResync(liveOwner, authority.cursor());
		assertFalse(livePending.guardCurrent(delayedLive, liveOwner));

		var failedLiveOwner = session(3, 30, "overworld", 1);
		var failedLivePending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.offer(failedLiveOwner, wire(0, 10, 10, 1));
		var failedLive = failedLivePending.drain(1, List.of(failedLiveOwner)).sent().getFirst();
		failedLivePending = failedLivePending.recordSendFailure(failedLive,
				VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, failedLiveOwner);
		assertTrue(failedLivePending.needsResync(failedLiveOwner));
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, failedLivePending
				.drainFair(1, 0, 1, List.of(failedLiveOwner), authority)
				.sent().getFirst().payload().operation());
	}

	@Test
	void deliveryEpochPersistsAfterPassAndOnlyDistinctPassAdvancesIt() {
		var observer = session(1, 10, "overworld", 1);
		var authority = VisualScarDeliveryRules.authoritativeSnapshot(1, List.of(
				row("overworld", wire(0, 1, 1, 1))));
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.offer(observer, wire(0, 9, 9, 1));
		var preResetLive = pending.drain(1, List.of(observer)).sent().getFirst();
		assertTrue(preResetLive.deliveryGeneration() > 0);
		assertEquals(preResetLive.deliveryGeneration(), pending.deliveryGeneration(observer));

		pending = pending.markNeedsResync(observer, authority.cursor());
		var firstReset = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		assertTrue(firstReset.deliveryGeneration() > preResetLive.deliveryGeneration());
		assertFalse(pending.guardCurrent(preResetLive, observer));
		pending = pending.recordSendSuccess(firstReset, observer);
		var firstSnapshot = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		pending = pending.recordSendSuccess(firstSnapshot, observer);
		pending = pending.drainFair(1, 0, 1, List.of(observer), authority).remaining();
		assertFalse(pending.needsResync(observer));
		assertEquals(firstReset.deliveryGeneration(), pending.deliveryGeneration(observer));

		int trackedBeforeStaleCallbacks = pending.trackedSessionCount();
		pending = pending.recordSendSuccess(firstReset, observer)
				.recordSendFailure(firstSnapshot,
						VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, observer);
		assertFalse(pending.needsResync(observer));
		assertEquals(trackedBeforeStaleCallbacks, pending.trackedSessionCount());

		pending = pending.markNeedsResync(observer, authority.cursor());
		var secondReset = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		assertTrue(secondReset.deliveryGeneration() > firstReset.deliveryGeneration());
		assertFalse(pending.guardCurrent(firstReset, observer));
		assertFalse(pending.guardCurrent(firstSnapshot, observer));
		pending = pending.recordSendSuccess(secondReset, observer);
		var secondSnapshot = pending.drainFair(1, 0, 1, List.of(observer), authority).sent().getFirst();
		pending = pending.recordSendSuccess(secondSnapshot, observer);
		pending = pending.drainFair(1, 0, 1, List.of(observer), authority).remaining();
		assertEquals(secondReset.deliveryGeneration(), pending.deliveryGeneration(observer));
		var liveAfterSecondPass = pending.offer(observer, wire(0, 10, 10, 2))
				.drain(1, List.of(observer)).sent().getFirst();
		assertEquals(secondReset.deliveryGeneration(), liveAfterSecondPass.deliveryGeneration());

		pending = pending.cancel(observer);
		assertEquals(0, pending.deliveryGeneration(observer));
		assertEquals(0, pending.trackedSessionCount());
	}

	@Test
	void authoritativeSnapshotRejectsResetRows() {
		var reset = ScarFxProtocolRules.resetDimension(1);
		assertThrows(IllegalArgumentException.class,
				() -> row("overworld", reset));
		assertThrows(IllegalArgumentException.class,
				() -> VisualScarDeliveryRules.authoritativeSnapshot(1,
						List.of(new VisualScarDeliveryModel.SnapshotRow("minecraft:overworld", reset))));
	}

	@Test
	void sessionOwnershipIsConstantTimeAndFailureCallbacksRequireExactSend() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/fx/VisualScarDeliveryRules.java"));
		assertTrue(source.contains("trackedSessions"));
		assertFalse(source.contains("new HashSet<>(byObserver.keySet())"));
		assertFalse(java.util.Arrays.stream(VisualScarDeliveryRules.Pending.class.getDeclaredMethods())
				.filter(method -> method.getName().equals("recordSendFailure"))
				.anyMatch(method -> java.util.Arrays.stream(method.getParameterTypes())
						.anyMatch(type -> type == boolean.class)));
		assertFalse(java.util.Arrays.stream(VisualScarDeliveryRules.class.getDeclaredMethods())
				.filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
				.anyMatch(method -> java.util.Arrays.stream(method.getParameterTypes())
						.anyMatch(type -> type == boolean.class)));
	}

	@Test
	void heldSnapshotFinishesStableTailDuringPerTickRevisionChurnThenSchedulesNextPass() {
		var observer = session(1, 10, "overworld", 1);
		var held = VisualScarDeliveryRules.authoritativeSnapshot(1,
				java.util.stream.LongStream.range(0, 2_048)
						.mapToObj(key -> row("overworld", wire(0, key, (int) key, 1))).toList());
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.markNeedsResync(observer, held.cursor());
		var reset = pending.drainFair(1, 0, 1, List.of(observer), held).sent().getFirst();
		pending = pending.recordSendSuccess(reset, observer);
		Set<Long> delivered = new HashSet<>();
		for (int tick = 0; tick < 2_048; tick++) {
			var current = VisualScarDeliveryRules.authoritativeSnapshot(2L + tick,
					held.rows().stream().collect(java.util.stream.Collectors.toList()));
			var drained = pending.drainFair(1, 0, 1, List.of(observer), current);
			assertEquals(1, drained.sent().size());
			var sent = drained.sent().getFirst();
			delivered.add(sent.payload().position());
			pending = drained.remaining().recordSendSuccess(sent, observer);
		}
		assertEquals(2_048, delivered.size());
		assertTrue(delivered.contains(2_047L));
		assertTrue(pending.needsResync(observer));
		var nextPass = pending.drainFair(1, 0, 1, List.of(observer),
				VisualScarDeliveryRules.authoritativeSnapshot(3_000, held.rows())).sent().getFirst();
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, nextPass.payload().operation());
		assertTrue(nextPass.deliveryGeneration() > reset.deliveryGeneration());
	}

	@Test
	void resyncSkipsRemovedHeldRowsAndCurrentTombstoneWins() {
		var observer = session(1, 10, "overworld", 1);
		var held = VisualScarDeliveryRules.authoritativeSnapshot(1, List.of(
				row("overworld", wire(0, 0, 0, 1)),
				row("overworld", wire(0, 1, 1, 1)),
				row("overworld", wire(0, 2, 2, 1))));
		var current = VisualScarDeliveryRules.authoritativeSnapshot(2, List.of(
				row("overworld", wire(0, 0, 0, 1)),
				row("overworld", wire(1, 2, 2, 1))));
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.markNeedsResync(observer, held.cursor());
		var reset = pending.drainFair(1, 0, 1, List.of(observer), held).sent().getFirst();
		pending = pending.recordSendSuccess(reset, observer);
		List<ScarFxProtocolRules.Wire> sent = new ArrayList<>();
		for (int index = 0; index < 3; index++) {
			var drained = pending.drainFair(1, 0, 1, List.of(observer), current);
			if (drained.sent().isEmpty()) break;
			sent.add(drained.sent().getFirst().payload());
			pending = drained.remaining().recordSendSuccess(drained.sent().getFirst(), observer);
		}
		assertEquals(List.of(0L, 2L), sent.subList(0, 2).stream()
				.map(ScarFxProtocolRules.Wire::position).toList());
		assertEquals(ScarFxProtocolRules.REMOVE, sent.get(1).operation());
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, sent.get(2).operation());
	}

	@Test
	void resyncOnlySessionOwnershipIsHardBoundedAcrossReconnectAndDimensionChurn() {
		var snapshot = VisualScarDeliveryRules.authoritativeSnapshot(1, List.of());
		var pending = VisualScarDeliveryRules.empty(2, 3);
		for (int index = 0; index < 3; index++) {
			pending = pending.markNeedsResync(session(index + 1, index + 10,
					index == 2 ? "the_nether" : "overworld", 1), snapshot.cursor());
		}
		var rejected = session(9, 99, "overworld", 1);
		pending = pending.markNeedsResync(rejected, snapshot.cursor());
		assertEquals(3, pending.trackedSessionCount());
		assertFalse(pending.needsResync(rejected));
		for (int round = 0; round < 100; round++) {
			var current = session(1, 1_000 + round,
					(round & 1) == 0 ? "overworld" : "the_nether", round + 2);
			pending = pending.drain(0, List.of(current)).remaining()
					.markNeedsResync(current, snapshot.cursor());
			assertTrue(pending.trackedSessionCount() <= 3);
		}
	}

	@Test
	void guardedSendFailuresResyncOnlyCurrentSupportedSessions() {
		var observer = session(1, 10, "overworld", 1);
		var reconnect = session(1, 11, "overworld", 2);
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.offer(observer, wire(0, 1, 1, 1));
		var unsupported = pending.drain(1, List.of(observer)).sent().getFirst();
		pending = pending.recordSendFailure(unsupported,
				VisualScarDeliveryRules.FailureReason.UNSUPPORTED_CAPABILITY, reconnect);
		assertEquals(1, pending.trackedSessionCount());
		pending = pending.recordSendFailure(unsupported,
				VisualScarDeliveryRules.FailureReason.UNSUPPORTED_CAPABILITY, observer);
		assertEquals(0, pending.trackedSessionCount());

		pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.offer(observer, wire(0, 2, 2, 1));
		var lost = pending.drain(1, List.of(observer)).sent().getFirst();
		pending = pending.recordSendFailure(lost,
				VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, reconnect);
		assertFalse(pending.needsResync(observer));
		pending = pending.recordSendFailure(lost,
				VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, observer);
		assertTrue(pending.needsResync(observer));
	}

	@Test
	void oneAndFivePercentInjectedLossProfilesConvergeWhileAuthorityRemainsActive() {
		for (int lossPercent : List.of(1, 5)) {
			var observer = session(lossPercent, 100 + lossPercent, "overworld", 1);
			var authority = VisualScarDeliveryRules.authoritativeSnapshot(lossPercent, java.util.stream
					.LongStream.range(0, 100)
					.mapToObj(key -> row("overworld", wire(0, key, (int) key, 1))).toList());
			var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
					.markNeedsResync(observer, authority.cursor());
			var client = ClientVisualScarState.empty(2_048, 1);
			int lossesRemaining = lossPercent;
			for (int round = 0; round < 256 && pending.needsResync(observer); round++) {
				var drained = pending.drainFair(64, 0, 64, List.of(observer), authority);
				for (var sent : drained.sent()) {
					if (lossesRemaining-- > 0) {
						pending = drained.remaining().recordSendFailure(sent,
								VisualScarDeliveryRules.FailureReason.INJECTED_LOSS, observer);
					} else {
						client = client.receive(sent.payload(), round, 1);
						pending = drained.remaining().recordSendSuccess(sent, observer);
					}
				}
				pending = drained.remaining();
			}
			assertFalse(pending.needsResync(observer));
			assertEquals(authority.generations("minecraft:overworld"), client.generations());
		}
	}

	@Test
	void repeatedUpdatesCoalesceAndExactRemoveTakesPrecedence() {
		var session = session(1, 10, "overworld", 1);
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768);
		for (int seed = 0; seed < 100; seed++) {
			pending = pending.offer(session, wire(0, 42, seed, 1));
		}
		assertEquals(1, pending.globalSize());
		assertEquals(99, pending.entry(session, 42, 1).orElseThrow().visualSeed());
		pending = pending.offer(session, wire(1, 42, 99, 1));
		assertEquals(1, pending.globalSize());
		assertEquals(ScarFxProtocolRules.REMOVE,
				pending.entry(session, 42, 1).orElseThrow().operation());
		pending = pending.offer(session, wire(0, 42, 100, 1));
		assertEquals(ScarFxProtocolRules.REMOVE,
				pending.entry(session, 42, 1).orElseThrow().operation());
		pending = pending.offer(session, wire(0, 42, 100, 2));
		assertEquals(ScarFxProtocolRules.CREATE_OR_UPDATE,
				pending.entry(session, 42, 1).orElseThrow().operation());
	}

	@Test
	void observerAndGlobalFloodsSetCursorInsteadOfMaterialisingCartesianState() {
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768);
		List<VisualScarLedgerRules.ObserverSession> sessions = new ArrayList<>();
		var saturated = session(999, 999, "overworld", 1);
		for (int key = 0; key < 2_100; key++) pending = pending.offer(saturated, wire(0, key, key, 1));
		assertEquals(2_048, pending.observerSize(saturated));
		assertTrue(pending.needsResync(saturated));
		for (int observer = 0; observer < 200; observer++) {
			var session = session(observer + 1, observer + 10, "overworld", 1);
			sessions.add(session);
			for (int key = 0; key < 300; key++) pending = pending.offer(session, wire(0, key, key, 1));
		}
		var snapshot = pending;
		assertTrue(snapshot.globalSize() <= 32_768);
		assertTrue(sessions.stream().allMatch(session -> snapshot.observerSize(session) <= 2_048));
		assertTrue(sessions.stream().anyMatch(snapshot::needsResync));
		assertEquals(0, snapshot.materializedActiveRecordCopies());
		assertThrows(IllegalArgumentException.class, () ->
				VisualScarDeliveryRules.authoritativeSnapshot(
						1,
						java.util.stream.LongStream.range(0, 2_049)
								.mapToObj(key -> row("overworld", wire(0, key, (int) key, 1))).toList()));
	}

	@Test
	void exactRemoveEvictsCreateAndMarksCrossObserverVictimForResync() {
		var first = session(1, 10, "overworld", 1);
		var second = session(2, 20, "overworld", 1);
		var pending = VisualScarDeliveryRules.empty(2, 3)
				.offer(first, wire(0, 1, 1, 1))
				.offer(first, wire(0, 2, 2, 1))
				.offer(second, wire(0, 3, 3, 1));
		pending = pending.offer(first, wire(1, 99, 9, 1));
		assertEquals(3, pending.globalSize());
		assertEquals(ScarFxProtocolRules.REMOVE,
				pending.entry(first, 99, 1).orElseThrow().operation());
		assertTrue(pending.needsResync(first));

		var global = VisualScarDeliveryRules.empty(3, 3)
				.offer(first, wire(0, 1, 1, 1))
				.offer(second, wire(0, 2, 2, 1))
				.offer(second, wire(0, 3, 3, 1));
		var offered = global.offerObserved(session(3, 30, "overworld", 1), wire(1, 99, 9, 1));
		global = offered.pending();
		var eviction = offered.eviction().orElseThrow();
		assertEquals(first, eviction.victimSession());
		assertEquals(new VisualScarDeliveryModel.ScarKey(1, 1), eviction.victimKey());
		assertTrue(global.needsResync(first));

		var tombstonesOnly = VisualScarDeliveryRules.empty(2, 3)
				.offer(first, wire(1, 1, 1, 1))
				.offer(first, wire(1, 2, 2, 1))
				.offer(second, wire(1, 3, 3, 1));
		var deferred = tombstonesOnly.offer(second, wire(1, 4, 4, 1));
		assertEquals(3, deferred.globalSize());
		assertTrue(deferred.needsResync(second));
	}

	@Test
	void saturatedDrainRefillConvergesToEvolvingNonemptyAuthorityWithoutLeaseExpiry() {
		var observer = session(1, 10, "overworld", 1);
		var pending = VisualScarDeliveryRules.empty(8, 8);
		var client = ClientVisualScarState.empty(2_048, 1);
		long previous = -1;
		for (int round = 0; round < 16; round++) {
			long retained = 1_000 + round;
			var authority = VisualScarDeliveryRules.authoritativeSnapshot(round + 1L, List.of(
					row("overworld", wire(0, retained, 200 + round, round + 2L)),
					row("overworld", wire(0, 10_000, 999, 1))));
			if (previous >= 0) pending = pending.offer(observer,
					wire(1, previous, 200 + round - 1, round + 1L));
			pending = pending.offer(observer, wire(0, retained, 200 + round, round + 2L));
			pending = pending.offer(observer, wire(0, 10_000, 999, 1));
			pending = pending.markNeedsResync(observer, authority.cursor());
			for (int page = 0; page < 8 && (pending.globalSize() > 0
					|| pending.needsResync(observer)); page++) {
				var drained = pending.drainFair(3, 2, 1, List.of(observer), authority);
				for (var sent : drained.sent()) {
					client = client.receive(sent.payload(), round, 1);
					pending = drained.remaining().recordSendSuccess(sent, observer);
				}
				pending = drained.remaining();
			}
			assertEquals(authority.generations("minecraft:overworld"), client.generations());
			previous = retained;
		}
		assertFalse(pending.needsResync(observer));
		assertTrue(client.size() > 0);
	}

	@Test
	void resyncOwnsFairShareUnderContinuousLiveSaturationAndLendsUnusedCapacity() {
		var live = session(1, 10, "overworld", 1);
		var missing = session(2, 20, "overworld", 1);
		var alsoMissing = session(3, 30, "the_nether", 1);
		long missingPosition = 99_999;
		var active = VisualScarDeliveryRules.authoritativeSnapshot(1, List.of(
				row("overworld", wire(0, missingPosition, 77, 9))));
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.markNeedsResync(missing, active.cursor())
				.markNeedsResync(alsoMissing, active.cursor());
		boolean delivered = false;
		int totalSent = 0;
		for (int round = 0; round < 16 && pending.needsResync(missing); round++) {
			for (int key = 0; key < 256; key++) {
				pending = pending.offer(live, wire(0, round * 1_000L + key, key, round + 1));
			}
			var drained = pending.drainFair(256, 192, 64,
					List.of(live, missing, alsoMissing), active);
			assertTrue(drained.sent().size() <= 256);
			if (round == 0) {
				assertEquals(2, drained.resyncSent());
				assertEquals(254, drained.liveSent());
				assertEquals(Set.of(missing, alsoMissing), drained.sent().stream()
						.filter(send -> !send.session().equals(live))
						.map(VisualScarDeliveryModel.Send::session).collect(
								java.util.stream.Collectors.toSet()));
			}
			for (var sent : drained.sent().stream().filter(VisualScarDeliveryModel.Send::resync).toList()) {
				pending = drained.remaining().recordSendSuccess(sent, sent.session());
			}
			delivered |= drained.sent().stream().anyMatch(send -> send.session().equals(missing)
					&& send.payload().position() == missingPosition);
			totalSent += drained.sent().size();
			pending = drained.remaining();
		}
		assertTrue(delivered);
		assertFalse(pending.needsResync(missing));
		assertFalse(pending.needsResync(alsoMissing));
		assertTrue(totalSent > 0);

		var largeActive = VisualScarDeliveryRules.authoritativeSnapshot(2,
				java.util.stream.LongStream.range(0, 300)
						.mapToObj(key -> row("overworld", wire(0, key, (int) key, 1))).toList());
		pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.markNeedsResync(missing, largeActive.cursor());
		var reset = pending.drainFair(256, 192, 64, List.of(missing), largeActive);
		assertEquals(1, reset.sent().size());
		pending = reset.remaining().recordSendSuccess(reset.sent().getFirst(), missing);
		int resynced = 0;
		for (int index = 0; index < 301 && pending.needsResync(missing); index++) {
			var one = pending.drainFair(256, 192, 64, List.of(missing), largeActive);
			for (var sent : one.sent()) {
				resynced++;
				pending = one.remaining().recordSendSuccess(sent, missing);
			}
		}
		assertEquals(300, resynced);
		assertFalse(pending.needsResync(missing));
	}

	@Test
	void revisionedStableKeyResyncRestartsOnMutationAndNeverCrossesDimension() {
		var overworld = session(1, 10, "overworld", 1);
		var first = VisualScarDeliveryRules.authoritativeSnapshot(10, List.of(
				row("overworld", wire(0, 3, 3, 1)),
				row("the_nether", wire(0, 50, 50, 1)),
				row("overworld", wire(0, 1, 1, 1)),
				row("overworld", wire(0, 2, 2, 1))));
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.markNeedsResync(overworld, first.cursor());
		var page = pending.drainFair(1, 0, 1, List.of(overworld), first);
		assertEquals(ScarFxProtocolRules.RESET_DIMENSION, page.sent().getFirst().payload().operation());
		pending = page.remaining().recordSendSuccess(page.sent().getFirst(), overworld);
		page = pending.drainFair(1, 0, 1, List.of(overworld), first);
		assertEquals(List.of(1L), page.sent().stream().map(send -> send.payload().position()).toList());
		pending = page.remaining().recordSendSuccess(page.sent().getFirst(), overworld);

		var changed = VisualScarDeliveryRules.authoritativeSnapshot(11, List.of(
				row("overworld", wire(0, 4, 4, 1)),
				row("overworld", wire(0, 0, 0, 1)),
				row("the_nether", wire(0, 51, 51, 1)),
				row("overworld", wire(0, 3, 3, 1)),
				row("overworld", wire(0, 1, 1, 1))));
		List<Long> delivered = new ArrayList<>();
		for (int guard = 0; guard < 32 && pending.needsResync(overworld); guard++) {
			var next = pending.drainFair(2, 0, 2, List.of(overworld), changed);
			for (var sent : next.sent()) {
				if (sent.payload().operation() != ScarFxProtocolRules.RESET_DIMENSION) {
					delivered.add(sent.payload().position());
				}
				pending = next.remaining().recordSendSuccess(sent, overworld);
			}
			pending = next.remaining();
		}
		assertTrue(delivered.containsAll(List.of(3L, 0L, 1L, 4L)));
		assertFalse(delivered.contains(2L));
		assertFalse(delivered.contains(50L));
		assertFalse(delivered.contains(51L));
		assertEquals(0, pending.materializedActiveRecordCopies());
		assertThrows(IllegalArgumentException.class, () ->
				VisualScarDeliveryRules.authoritativeSnapshot(12, List.of(
						row("overworld", wire(0, 1, 1, 1)),
						row("overworld", wire(0, 1, 2, 2)))));
	}

	@Test
	void globalDrainIsFairBoundedConnectionScopedAndEventuallyConverges() {
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768);
		List<VisualScarLedgerRules.ObserverSession> sessions = List.of(
				session(1, 10, "overworld", 1), session(2, 20, "overworld", 1),
				session(3, 30, "the_nether", 1));
		for (var session : sessions) {
			for (int key = 0; key < 300; key++) pending = pending.offer(session, wire(0, key, key, 1));
		}
		Set<UUID> firstOwners = new HashSet<>();
		var first = pending.drain(256, sessions);
		first.sent().forEach(send -> firstOwners.add(send.session().player()));
		assertEquals(256, first.sent().size());
		assertEquals(Set.of(new UUID(0, 1), new UUID(0, 2), new UUID(0, 3)), firstOwners);
		pending = first.remaining();
		int rounds = 1;
		while (pending.globalSize() > 0 && rounds++ < 10) pending = pending.drain(256, sessions).remaining();
		assertEquals(0, pending.globalSize());
	}

	@Test
	void reconnectDimensionAndLogoutCancelStaleSessionTail() {
		var old = session(1, 10, "overworld", 1);
		var reconnect = session(1, 11, "overworld", 2);
		var dimension = session(1, 10, "the_nether", 2);
		var pending = VisualScarDeliveryRules.empty(2_048, 32_768)
				.offer(old, wire(0, 42, 1, 1));
		assertFalse(VisualScarDeliveryRules.sessionCurrent(old, reconnect));
		assertFalse(VisualScarDeliveryRules.sessionCurrent(old, dimension));
		var staleDrain = pending.drain(256, List.of(reconnect));
		assertTrue(staleDrain.sent().isEmpty());
		assertEquals(1, staleDrain.staleEntriesDropped());
		assertEquals(0, pending.cancel(old).globalSize());
	}

	private static VisualScarLedgerRules.ObserverSession session(long player, long connection,
			String dimension, long generation) {
		return new VisualScarLedgerRules.ObserverSession(new UUID(0, player), connection,
				"minecraft:" + dimension, generation);
	}

	private static ScarFxProtocolRules.Wire wire(int operation, long position,
			int seed, long generation) {
		return new ScarFxProtocolRules.Wire(operation, position, 1, 0, 0, seed, generation, 40);
	}

	private static VisualScarDeliveryModel.SnapshotRow row(String dimension,
			ScarFxProtocolRules.Wire wire) {
		return new VisualScarDeliveryModel.SnapshotRow("minecraft:" + dimension, wire);
	}
}
