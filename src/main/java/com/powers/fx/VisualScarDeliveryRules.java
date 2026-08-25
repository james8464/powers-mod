package com.powers.fx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Owns bounded pure delivery queues, tombstone precedence, and fair live/resync draining. */
public final class VisualScarDeliveryRules {
	private VisualScarDeliveryRules() { }
	public static Pending empty(int observerLimit, int globalLimit) {
		return new Pending(observerLimit, globalLimit);
	}
	public static VisualScarDeliveryModel.AuthoritativeSnapshot authoritativeSnapshot(long revision,
			List<VisualScarDeliveryModel.SnapshotRow> rows) {
		return new VisualScarDeliveryModel.AuthoritativeSnapshot(revision, rows);
	}
	public static boolean sessionCurrent(VisualScarLedgerRules.ObserverSession captured,
			VisualScarLedgerRules.ObserverSession current) {
		return VisualScarLedgerRules.sessionCurrent(captured, current);
	}

	private static FailureAction onSendFailure(FailureReason reason, boolean sessionCurrent) {
		Objects.requireNonNull(reason, "reason");
		if (!sessionCurrent) return FailureAction.DISCARD_STALE;
		if (reason == FailureReason.UNSUPPORTED_CAPABILITY) {
			return FailureAction.CANCEL_UNSUPPORTED_WITHOUT_RESYNC;
		}
		return FailureAction.MARK_BOUNDED_RESYNC;
	}

	public enum FailureReason { UNSUPPORTED_CAPABILITY, INJECTED_LOSS, QUEUE_OVERFLOW, EXPIRED_BEFORE_SEND,
		SESSION_PREDICATE_FALSE }
	public enum FailureAction { CANCEL_UNSUPPORTED_WITHOUT_RESYNC, MARK_BOUNDED_RESYNC, DISCARD_STALE }
	/** Mutable pure-core owner intended for one authoritative thread. */
	static final class Pending {
		private final int observerLimit;
		private final int globalLimit;
		private final Map<VisualScarLedgerRules.ObserverSession,
				LinkedHashMap<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire>> byObserver = new LinkedHashMap<>();
		private final Map<VisualScarLedgerRules.ObserverSession, ResyncState> resyncStates = new LinkedHashMap<>();
		private final Set<VisualScarLedgerRules.ObserverSession> trackedSessions = new HashSet<>();
		private final Map<VisualScarLedgerRules.ObserverSession, Long> deliveryGenerations = new LinkedHashMap<>();
		private int globalSize;
		private int observerRotation;
		private VisualScarDeliveryModel.Eviction lastGlobalEviction;

		private Pending(int observerLimit, int globalLimit) {
			if (observerLimit < 1 || observerLimit > 2_048 || globalLimit < 1 || globalLimit > 32_768) {
				throw new IllegalArgumentException("pending limits exceed hard bounds");
			}
			this.observerLimit = observerLimit;
			this.globalLimit = globalLimit;
		}
		public Pending offer(VisualScarLedgerRules.ObserverSession session,
				ScarFxProtocolRules.Wire wire) {
			return offerObserved(session, wire).pending();
		}
		/** Offers one wire and atomically reports any cross-observer CREATE eviction. */
		public VisualScarDeliveryModel.OfferResult offerObserved(VisualScarLedgerRules.ObserverSession session,
				ScarFxProtocolRules.Wire wire) {
			lastGlobalEviction = null;
			offerInternal(session, wire);
			return new VisualScarDeliveryModel.OfferResult(this, Optional.ofNullable(lastGlobalEviction));
		}
		private Pending offerInternal(VisualScarLedgerRules.ObserverSession session,
				ScarFxProtocolRules.Wire wire) {
			Objects.requireNonNull(session, "session");
			if (!ScarFxProtocolRules.validate(wire)
					|| wire.operation() == ScarFxProtocolRules.RESET_DIMENSION
					|| !ensureTracked(session)) return this;
			LinkedHashMap<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire> queue = byObserver.computeIfAbsent(
					session, ignored -> new LinkedHashMap<>());
			VisualScarDeliveryModel.ScarKey key = new VisualScarDeliveryModel.ScarKey(wire.position(), wire.face());
			ScarFxProtocolRules.Wire current = queue.get(key);
			if (current != null) {
				ScarFxProtocolRules.Wire selected = select(current, wire);
				if (selected != current) queue.put(key, selected);
				return this;
			}
			boolean observerFull = queue.size() >= observerLimit;
			boolean globalFull = globalSize >= globalLimit;
			if (observerFull || globalFull) {
				if (wire.operation() == ScarFxProtocolRules.REMOVE && observerFull
						&& evictObserverCreate(session, queue)) {
					queue.put(key, wire);
					globalSize++;
					markNeedsResync(session, unboundCursor());
					return this;
				}
				if (wire.operation() == ScarFxProtocolRules.REMOVE && !observerFull && globalFull) {
					Optional<VisualScarDeliveryModel.Eviction> eviction = evictGlobalCreate();
					if (eviction.isPresent()) {
						lastGlobalEviction = eviction.get();
						markNeedsResync(lastGlobalEviction.victimSession(), unboundCursor());
						queue.put(key, wire);
						globalSize++;
						markNeedsResync(session, unboundCursor());
						return this;
					}
				}
				markNeedsResync(session, unboundCursor());
				return this;
			}
			queue.put(key, wire);
			globalSize++;
			return this;
		}
		public Optional<ScarFxProtocolRules.Wire> entry(
				VisualScarLedgerRules.ObserverSession session, long position, int face) {
			Map<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire> queue = byObserver.get(session);
			return queue == null ? Optional.empty()
					: Optional.ofNullable(queue.get(new VisualScarDeliveryModel.ScarKey(position, face)));
		}

		public Optional<VisualScarDeliveryModel.Eviction> lastGlobalEviction() {
			return Optional.ofNullable(lastGlobalEviction);
		}
		public int globalSize() { return globalSize; }
		public int observerSize(VisualScarLedgerRules.ObserverSession session) {
			Map<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire> queue = byObserver.get(session);
			return queue == null ? 0 : queue.size();
		}
		public boolean needsResync(VisualScarLedgerRules.ObserverSession session) {
			return resyncStates.containsKey(session);
		}
		public int materializedActiveRecordCopies() { return 0; }
		public int trackedSessionCount() { return trackedSessions.size(); }
		public long deliveryGeneration(VisualScarLedgerRules.ObserverSession session) {
			return deliveryGenerations.getOrDefault(session, 0L);
		}
		public Pending markNeedsResync(VisualScarLedgerRules.ObserverSession session,
				VisualScarDeliveryModel.ResyncCursor cursor) {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(cursor, "cursor");
			boolean alreadyTracked = ownsSession(session);
			if (!ensureTracked(session)) return this;
			ResyncState previous = resyncStates.get(session);
			if (previous != null) {
				previous.followupRequested = true;
				if (cursor.revision() >= 0) {
					previous.latestRevision = Math.max(previous.latestRevision, cursor.revision());
				}
				return this;
			}
			long generation = alreadyTracked
					? incrementDeliveryGeneration(deliveryGeneration(session))
					: deliveryGeneration(session);
			deliveryGenerations.put(session, generation);
			resyncStates.put(session, ResyncState.reset(generation, cursor.revision()));
			return this;
		}
		/** Applies one guarded-send failure only while its exact session and delivery guard are current. */
		public Pending recordSendFailure(VisualScarDeliveryModel.Send send, FailureReason reason,
				VisualScarLedgerRules.ObserverSession currentSession) {
			Objects.requireNonNull(send, "send");
			if (!guardCurrent(send, currentSession)) return this;
			FailureAction action = onSendFailure(reason, true);
			if (action == FailureAction.CANCEL_UNSUPPORTED_WITHOUT_RESYNC) return cancel(send.session());
			if (action != FailureAction.MARK_BOUNDED_RESYNC) return this;
			if (!send.resync()) return markNeedsResync(send.session(), unboundCursor());
			ResyncState state = resyncStates.get(send.session());
			if (state == null || state.awaiting == null || !state.awaiting.equals(send)) return this;
			state.awaiting = null;
			if (send.payload().operation() != ScarFxProtocolRules.RESET_DIMENSION) {
				state.awaitingHeldKey = null;
			}
			return this;
		}
		/** Advances reset/snapshot state only after the exact guarded send boundary succeeded. */
		public Pending recordSendSuccess(VisualScarDeliveryModel.Send send,
				VisualScarLedgerRules.ObserverSession currentSession) {
			if (!guardCurrent(send, currentSession) || !send.resync()) return this;
			ResyncState state = resyncStates.get(send.session());
			if (state == null || state.awaiting == null || !state.awaiting.equals(send)) return this;
			if (send.payload().operation() == ScarFxProtocolRules.RESET_DIMENSION) {
				clearLiveQueue(send.session());
				state.phase = ResyncPhase.SNAPSHOT;
				state.afterKey = null;
			} else {
				state.afterKey = state.awaitingHeldKey;
			}
			state.awaiting = null;
			state.awaitingHeldKey = null;
			return this;
		}

		/** Revalidates the PacketFault-delayed guard against exact session and delivery generation. */
		public boolean guardCurrent(VisualScarDeliveryModel.Send send,
				VisualScarLedgerRules.ObserverSession currentSession) {
			if (send == null || !Objects.equals(send.session(), currentSession)) return false;
			return deliveryGeneration(send.session()) == send.deliveryGeneration();
		}

		public VisualScarDeliveryModel.Drain drain(int maximum, List<VisualScarLedgerRules.ObserverSession> currentSessions) {
			checkSendMaximum(maximum);
			int stale = dropStale(currentSessions);
			List<VisualScarDeliveryModel.Send> sent = drainLive(maximum, currentSessions);
			return new VisualScarDeliveryModel.Drain(sent, this, stale, sent.size(), 0);
		}

		public VisualScarDeliveryModel.Drain drainFair(int maximum, int liveReserve, int resyncReserve,
				List<VisualScarLedgerRules.ObserverSession> currentSessions,
				VisualScarDeliveryModel.AuthoritativeSnapshot snapshot) {
			checkSendMaximum(maximum);
			if (liveReserve < 0 || liveReserve > 192 || resyncReserve < 0 || resyncReserve > 64
					|| liveReserve + resyncReserve > maximum) {
				throw new IllegalArgumentException("invalid fair drain bounds");
			}
			int stale = dropStale(currentSessions);
			List<VisualScarDeliveryModel.Send> resync = drainResync(resyncReserve, currentSessions, snapshot);
			List<VisualScarDeliveryModel.Send> live = drainLive(liveReserve, currentSessions);
			int remaining = maximum - resync.size() - live.size();
			if (remaining > 0) {
				List<VisualScarDeliveryModel.Send> extra = drainResync(remaining, currentSessions, snapshot);
				resync.addAll(extra);
				remaining -= extra.size();
			}
			if (remaining > 0) live.addAll(drainLive(remaining, currentSessions));
			List<VisualScarDeliveryModel.Send> sent = new ArrayList<>(resync.size() + live.size());
			sent.addAll(resync);
			sent.addAll(live);
			return new VisualScarDeliveryModel.Drain(sent, this, stale, live.size(), resync.size());
		}

		public Pending cancel(VisualScarLedgerRules.ObserverSession session) {
			clearLiveQueue(session);
			resyncStates.remove(session);
			deliveryGenerations.remove(session);
			trackedSessions.remove(session);
			return this;
		}

		private int dropStale(List<VisualScarLedgerRules.ObserverSession> currentSessions) {
			Set<VisualScarLedgerRules.ObserverSession> current = Set.copyOf(currentSessions);
			List<VisualScarLedgerRules.ObserverSession> stale = List.copyOf(trackedSessions).stream()
					.filter(session -> !current.contains(session))
					.toList();
			int dropped = stale.stream().mapToInt(this::observerSize).sum();
			stale.forEach(this::cancel);
			return dropped;
		}

		private List<VisualScarDeliveryModel.Send> drainLive(int maximum,
				List<VisualScarLedgerRules.ObserverSession> sessions) {
			List<VisualScarDeliveryModel.Send> result = new ArrayList<>(maximum);
			if (sessions.isEmpty()) return result;
			int idle = 0;
			while (result.size() < maximum && idle < sessions.size()) {
				var session = sessions.get(Math.floorMod(observerRotation++, sessions.size()));
				ResyncState resync = resyncStates.get(session);
				if (resync != null && (resync.phase == ResyncPhase.RESET_PENDING
						|| resync.awaiting != null && resync.awaiting.payload().operation()
						== ScarFxProtocolRules.RESET_DIMENSION)) {
					idle++;
					continue;
				}
				LinkedHashMap<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire> queue = byObserver.get(session);
				if (queue == null || queue.isEmpty()) {
					idle++;
					continue;
				}
				var iterator = queue.entrySet().iterator();
				var entry = iterator.next();
				iterator.remove();
				globalSize--;
				if (queue.isEmpty()) byObserver.remove(session);
				result.add(new VisualScarDeliveryModel.Send(session, entry.getValue(), deliveryGeneration(session), false));
				idle = 0;
			}
			return result;
		}

		private List<VisualScarDeliveryModel.Send> drainResync(int maximum,
				List<VisualScarLedgerRules.ObserverSession> sessions,
				VisualScarDeliveryModel.AuthoritativeSnapshot snapshot) {
			List<VisualScarDeliveryModel.Send> result = new ArrayList<>(maximum);
			if (maximum == 0 || sessions.isEmpty()) return result;
			int idle = 0;
			int rotation = observerRotation;
			while (result.size() < maximum && idle < sessions.size()) {
				var session = sessions.get(Math.floorMod(rotation++, sessions.size()));
				if (!needsResync(session)) {
					idle++;
					continue;
				}
				Optional<VisualScarDeliveryModel.Send> next = nextResyncSend(session, snapshot);
				if (next.isEmpty()) {
					idle++;
					continue;
				}
				result.add(next.get());
				idle = 0;
			}
			observerRotation = rotation;
			return result;
		}

		private Optional<VisualScarDeliveryModel.Send> nextResyncSend(VisualScarLedgerRules.ObserverSession session,
				VisualScarDeliveryModel.AuthoritativeSnapshot currentSnapshot) {
			ResyncState state = resyncStates.get(session);
			if (state == null || state.awaiting != null) return Optional.empty();
			state.latestRevision = Math.max(state.latestRevision, currentSnapshot.revision());
			if (state.phase == ResyncPhase.RESET_PENDING) {
				state.heldSnapshot = currentSnapshot;
				VisualScarDeliveryModel.Send reset = new VisualScarDeliveryModel.Send(session,
						ScarFxProtocolRules.resetDimension(state.deliveryGeneration),
						state.deliveryGeneration, true);
				state.awaiting = reset;
				return Optional.of(reset);
			}
			while (true) {
				VisualScarDeliveryModel.ResyncCursor heldCursor = new VisualScarDeliveryModel.ResyncCursor(state.heldSnapshot.revision(), state.afterKey);
				Optional<VisualScarDeliveryModel.SnapshotRow> heldNext = state.heldSnapshot.next(session.dimension(), heldCursor);
				if (heldNext.isEmpty()) {
					if (state.followupRequested || state.latestRevision > state.heldSnapshot.revision()) {
						startDistinctPass(session, state.latestRevision);
						return nextResyncSend(session, currentSnapshot);
					}
					resyncStates.remove(session);
					return Optional.empty();
				}
				VisualScarDeliveryModel.SnapshotRow heldRow = heldNext.get();
				Optional<VisualScarDeliveryModel.SnapshotRow> current = currentSnapshot.lookup(session.dimension(), heldRow.key());
				if (current.isEmpty()) {
					state.afterKey = heldRow.key();
					continue;
				}
				VisualScarDeliveryModel.SnapshotRow selected = current.get();
				VisualScarDeliveryModel.Send send = new VisualScarDeliveryModel.Send(session, selected.wire(), state.deliveryGeneration, true);
				state.awaiting = send;
				state.awaitingHeldKey = heldRow.key();
				return Optional.of(send);
			}
		}

		private boolean evictObserverCreate(VisualScarLedgerRules.ObserverSession session,
				Map<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire> queue) {
			var iterator = queue.entrySet().iterator();
			while (iterator.hasNext()) {
				var entry = iterator.next();
				if (entry.getValue().operation() == ScarFxProtocolRules.CREATE_OR_UPDATE) {
					iterator.remove();
					globalSize--;
					lastGlobalEviction = new VisualScarDeliveryModel.Eviction(session, entry.getKey());
					return true;
				}
			}
			return false;
		}

		private Optional<VisualScarDeliveryModel.Eviction> evictGlobalCreate() {
			for (var observer : List.copyOf(byObserver.entrySet())) {
				var iterator = observer.getValue().entrySet().iterator();
				while (iterator.hasNext()) {
					var entry = iterator.next();
					if (entry.getValue().operation() == ScarFxProtocolRules.CREATE_OR_UPDATE) {
						iterator.remove();
						globalSize--;
						if (observer.getValue().isEmpty()) byObserver.remove(observer.getKey());
						return Optional.of(new VisualScarDeliveryModel.Eviction(observer.getKey(), entry.getKey()));
					}
				}
			}
			return Optional.empty();
		}

		private boolean ownsSession(VisualScarLedgerRules.ObserverSession session) {
			return trackedSessions.contains(session);
		}

		private boolean ensureTracked(VisualScarLedgerRules.ObserverSession session) {
			if (trackedSessions.contains(session)) return true;
			if (trackedSessions.size() >= globalLimit) return false;
			trackedSessions.add(session);
			deliveryGenerations.put(session, 1L);
			return true;
		}

		private void startDistinctPass(VisualScarLedgerRules.ObserverSession session,
				long latestRevision) {
			long generation = incrementDeliveryGeneration(deliveryGeneration(session));
			deliveryGenerations.put(session, generation);
			resyncStates.put(session, ResyncState.reset(generation, latestRevision));
		}

		private void clearLiveQueue(VisualScarLedgerRules.ObserverSession session) {
			Map<VisualScarDeliveryModel.ScarKey, ScarFxProtocolRules.Wire> removed = byObserver.remove(session);
			if (removed != null) globalSize -= removed.size();
		}

		private static long incrementDeliveryGeneration(long current) {
			if (current <= 0 || current == Long.MAX_VALUE) {
				throw new IllegalStateException("delivery generation exhausted");
			}
			return current + 1;
		}

		private static ScarFxProtocolRules.Wire select(ScarFxProtocolRules.Wire current,
				ScarFxProtocolRules.Wire offered) {
			if (current.operation() == ScarFxProtocolRules.REMOVE) {
				return offered.operation() == ScarFxProtocolRules.CREATE_OR_UPDATE
						&& ScarFxProtocolRules.newerUnsigned(offered.generation(), current.generation())
						? offered : current;
			}
			if (offered.operation() == ScarFxProtocolRules.REMOVE
					&& offered.generation() == current.generation()) return offered;
			return offered.operation() == ScarFxProtocolRules.CREATE_OR_UPDATE
					&& Long.compareUnsigned(offered.generation(), current.generation()) >= 0
					? offered : current;
		}

		private static VisualScarDeliveryModel.ResyncCursor unboundCursor() {
			return new VisualScarDeliveryModel.ResyncCursor(-1, null);
		}
		private static void checkSendMaximum(int maximum) {
			if (maximum < 0 || maximum > 256) {
				throw new IllegalArgumentException("maximum outside send bounds");
			}
		}

		private enum ResyncPhase { RESET_PENDING, SNAPSHOT }

		private static final class ResyncState {
			private final long deliveryGeneration;
			private ResyncPhase phase;
			private long latestRevision;
			private VisualScarDeliveryModel.AuthoritativeSnapshot heldSnapshot;
			private VisualScarDeliveryModel.ScarKey afterKey;
			private VisualScarDeliveryModel.ScarKey awaitingHeldKey;
			private VisualScarDeliveryModel.Send awaiting;
			private boolean followupRequested;

			private ResyncState(long deliveryGeneration, long latestRevision) {
				this.deliveryGeneration = deliveryGeneration;
				this.latestRevision = Math.max(0, latestRevision);
				this.phase = ResyncPhase.RESET_PENDING;
			}

			private static ResyncState reset(long deliveryGeneration, long latestRevision) {
				return new ResyncState(deliveryGeneration, latestRevision);
			}
		}
	}
}
