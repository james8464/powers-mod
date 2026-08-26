package com.powers.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Models bounded client semantic scar state independently from renderer and Minecraft lifecycle APIs. */
public final class ClientVisualScarState {
	private final int capacity;
	private final long connectionEpoch;
	private final long lifecycleTick;
	private final Map<Key, Entry> entries;

	private ClientVisualScarState(int capacity, long connectionEpoch,
			long lifecycleTick, Map<Key, Entry> entries) {
		this.capacity = capacity;
		this.connectionEpoch = connectionEpoch;
		this.lifecycleTick = lifecycleTick;
		this.entries = Map.copyOf(entries);
	}

	/** Returns an empty bounded state for one connection epoch. */
	public static ClientVisualScarState empty(int capacity, long connectionEpoch) {
		if (capacity < 1 || capacity > 2_048 || connectionEpoch < 0) {
			throw new IllegalArgumentException("invalid client state");
		}
		return new ClientVisualScarState(capacity, connectionEpoch, 0, Map.of());
	}

	/** Applies one validated wire for the captured epoch, preserving exact replay idempotence. */
	public ClientVisualScarState receive(ScarFxProtocolRules.Wire wire,
			long receiptTick, long capturedEpoch) {
		return receiveObserved(wire, receiptTick, capturedEpoch).state();
	}

	/** Applies one wire and reports whether a capacity rejection requires authoritative resync. */
	public ReceiveResult receiveObserved(ScarFxProtocolRules.Wire wire,
			long receiptTick, long capturedEpoch) {
		if (!ScarFxProtocolRules.validate(wire) || receiptTick < 0 || capturedEpoch != connectionEpoch) {
			return new ReceiveResult(this, ReceiveOutcome.REJECTED_INVALID_OR_STALE, false);
		}
		if (wire.operation() == ScarFxProtocolRules.RESET_DIMENSION) {
			return new ReceiveResult(new ClientVisualScarState(capacity, connectionEpoch,
					Math.max(lifecycleTick, receiptTick), Map.of()), ReceiveOutcome.APPLIED_RESET, false);
		}
		Key key = new Key(wire.position(), wire.face());
		Entry existing = entries.get(key);
		if (wire.operation() == ScarFxProtocolRules.REMOVE) {
			if (existing == null || existing.generation() != wire.generation()) {
				return new ReceiveResult(this, ReceiveOutcome.IGNORED_REPLAY, false);
			}
			Map<Key, Entry> changed = new HashMap<>(entries);
			changed.remove(key);
			return new ReceiveResult(new ClientVisualScarState(capacity, connectionEpoch,
					Math.max(lifecycleTick, receiptTick), changed), ReceiveOutcome.APPLIED, false);
		}
		if (existing != null && existing.generation() == wire.generation()) {
			return new ReceiveResult(this, ReceiveOutcome.IGNORED_REPLAY, false);
		}
		if (existing != null && !ScarFxProtocolRules.newerUnsigned(
				wire.generation(), existing.generation())) {
			return new ReceiveResult(this, ReceiveOutcome.IGNORED_REPLAY, false);
		}
		if (existing == null && entries.size() >= capacity) {
			return new ReceiveResult(this, ReceiveOutcome.REJECTED_CAP, true);
		}
		Map<Key, Entry> changed = new HashMap<>(entries);
		long expiresAt = receiptTick > Long.MAX_VALUE - wire.leaseTicks()
				? Long.MAX_VALUE : receiptTick + wire.leaseTicks();
		changed.put(key, new Entry(wire, expiresAt));
		return new ReceiveResult(new ClientVisualScarState(capacity, connectionEpoch,
				Math.max(lifecycleTick, receiptTick), changed), ReceiveOutcome.APPLIED, false);
	}

	/** Applies a callback only when its captured connection and dimension stamp remains exact. */
	public ClientVisualScarState receiveFrom(HandlerStamp captured, HandlerStamp current,
			ScarFxProtocolRules.Wire wire, long receiptTick) {
		if (!Objects.equals(captured, current) || captured.connectionEpoch() != connectionEpoch) return this;
		return receive(wire, receiptTick, captured.connectionEpoch());
	}

	/** Advances receipt-local lifecycle time even while custom game time is frozen. */
	public ClientVisualScarState tickLifecycle(boolean timeFrozen) {
		long nextTick = lifecycleTick == Long.MAX_VALUE ? Long.MAX_VALUE : lifecycleTick + 1;
		Map<Key, Entry> retained = new HashMap<>();
		entries.forEach((key, entry) -> {
			if (entry.localExpiresAt() > nextTick) retained.put(key, entry);
		});
		return new ClientVisualScarState(capacity, connectionEpoch, nextTick, retained);
	}

	/** Clears semantic state for a dimension or connection reset and installs the new epoch. */
	public ClientVisualScarState reset(Reset reset, long newConnectionEpoch) {
		Objects.requireNonNull(reset, "reset");
		if (newConnectionEpoch < 0) throw new IllegalArgumentException("negative epoch");
		return empty(capacity, newConnectionEpoch);
	}

	/** Preserves semantic records when renderer-owned resources close. */
	public ClientVisualScarState rendererResourcesClosed() {
		return this;
	}

	/** Preserves semantic records when renderer-owned resources are recreated. */
	public ClientVisualScarState rendererResourcesRecreated() {
		return this;
	}

	/** Returns the entry for one packed position and face when present. */
	public Optional<Entry> get(long position, int face) {
		return Optional.ofNullable(entries.get(new Key(position, face)));
	}

	/** Returns the exact active semantic-record count. */
	public int size() {
		return entries.size();
	}

	/** Reports exact key/generation membership for bounded reconciliation assertions. */
	public boolean containsGeneration(long position, int face, long generation) {
		Entry entry = entries.get(new Key(position, face));
		return entry != null && entry.generation() == generation;
	}

	/** Returns an immutable exact key/generation view for authoritative reconciliation. */
	public Map<VisualScarDeliveryModel.ScarKey, Long> generations() {
		Map<VisualScarDeliveryModel.ScarKey, Long> result = new HashMap<>();
		entries.forEach((key, entry) -> result.put(
				new VisualScarDeliveryModel.ScarKey(key.position(), key.face()), entry.generation()));
		return Map.copyOf(result);
	}

	/** Reports renderer visibility without deleting hidden semantic state. */
	public boolean visible(long position, int face, boolean clientChunkLoaded,
			boolean supportFaceValid) {
		return entries.containsKey(new Key(position, face)) && clientChunkLoaded && supportFaceValid;
	}

	@Override
	public boolean equals(Object value) {
		if (this == value) return true;
		return value instanceof ClientVisualScarState other && capacity == other.capacity
				&& connectionEpoch == other.connectionEpoch && lifecycleTick == other.lifecycleTick
				&& entries.equals(other.entries);
	}

	@Override
	public int hashCode() {
		return Objects.hash(capacity, connectionEpoch, lifecycleTick, entries);
	}

	public enum Reset { DIMENSION_CHANGE, CONNECTION_EPOCH }

	public enum ReceiveOutcome {
		APPLIED, APPLIED_RESET, IGNORED_REPLAY, REJECTED_CAP, REJECTED_INVALID_OR_STALE
	}

	public record ReceiveResult(ClientVisualScarState state, ReceiveOutcome outcome,
			boolean needsAuthoritativeResync) {
		public ReceiveResult {
			state = Objects.requireNonNull(state, "state");
			outcome = Objects.requireNonNull(outcome, "outcome");
			if (needsAuthoritativeResync != (outcome == ReceiveOutcome.REJECTED_CAP)) {
				throw new IllegalArgumentException("only capacity rejection schedules resync");
			}
		}
	}

	public record HandlerStamp(long connectionEpoch, String dimension) {
		public HandlerStamp {
			dimension = Objects.requireNonNull(dimension, "dimension");
			if (connectionEpoch < 0) throw new IllegalArgumentException("negative connection epoch");
		}
	}

	public record Entry(int operation, long position, int face, int impact, int material,
			int visualSeed, long generation, int leaseTicks, long localExpiresAt) {
		private Entry(ScarFxProtocolRules.Wire wire, long localExpiresAt) {
			this(wire.operation(), wire.position(), wire.face(), wire.impact(), wire.material(),
					wire.visualSeed(), wire.generation(), wire.leaseTicks(), localExpiresAt);
		}
	}

	private record Key(long position, int face) {
	}
}
