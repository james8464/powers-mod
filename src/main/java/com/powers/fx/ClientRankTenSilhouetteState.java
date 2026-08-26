package com.powers.fx;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable bounded semantic silhouette state, deliberately independent of Minecraft client APIs. */
public final class ClientRankTenSilhouetteState {
	public static final int MAX_CAPACITY = 64;
	public static final int MIN_LIFETIME_TICKS = 1;
	public static final int AUTHORED_LIFETIME_TICKS = 40;
	public static final int MAX_LIFETIME_TICKS = 80;
	private final int capacity;
	private final long connectionEpoch;
	private final String dimension;
	private final long lifecycleTick;
	private final long latestEventId;
	private final Map<Long, Entry> entries;

	private ClientRankTenSilhouetteState(int capacity, long connectionEpoch, String dimension,
			long lifecycleTick, long latestEventId, Map<Long, Entry> entries) {
		this.capacity = capacity;
		this.connectionEpoch = connectionEpoch;
		this.dimension = dimension;
		this.lifecycleTick = lifecycleTick;
		this.latestEventId = latestEventId;
		this.entries = Map.copyOf(entries);
	}

	/** Creates empty state for exactly one live connection epoch and dimension. */
	public static ClientRankTenSilhouetteState empty(int capacity, long connectionEpoch,
			String dimension) {
		if (capacity < 1 || capacity > MAX_CAPACITY || connectionEpoch < 0
				|| !validDimension(dimension)) throw new IllegalArgumentException("invalid silhouette state");
		return new ClientRankTenSilhouetteState(capacity, connectionEpoch, dimension, 0, 0, Map.of());
	}

	/** Validates every wire and handler stamp before making an immutable state transition. */
	public ClientRankTenSilhouetteState receive(Wire wire, long receiptTick, long capturedEpoch,
			String capturedDimension) {
		if (!valid(wire) || receiptTick < 0 || capturedEpoch != connectionEpoch
				|| !dimension.equals(capturedDimension) || !dimension.equals(wire.dimension())) return this;
		if (wire.eventId() <= latestEventId) return this;
		if (entries.size() >= capacity) return this;
		long expiresAt = receiptTick > Long.MAX_VALUE - wire.lifetimeTicks()
				? Long.MAX_VALUE : receiptTick + wire.lifetimeTicks();
		if (expiresAt <= lifecycleTick) return this;
		Map<Long, Entry> changed = new LinkedHashMap<>(entries);
		changed.put(wire.eventId(), new Entry(wire, expiresAt));
		return new ClientRankTenSilhouetteState(capacity, connectionEpoch, dimension,
				Math.max(lifecycleTick, receiptTick), wire.eventId(), changed);
	}

	/** Advances receipt-local lifecycle time and removes records precisely at their expiry tick. */
	public ClientRankTenSilhouetteState tick() {
		if (lifecycleTick == Long.MAX_VALUE) return this;
		long nextTick = lifecycleTick + 1;
		Map<Long, Entry> retained = new LinkedHashMap<>();
		entries.forEach((eventId, entry) -> {
			if (entry.expiresAt() > nextTick) retained.put(eventId, entry);
		});
		return new ClientRankTenSilhouetteState(capacity, connectionEpoch, dimension,
				nextTick, latestEventId, retained);
	}

	/** Clears state on either disconnect or dimension change and installs the exact current stamp. */
	public ClientRankTenSilhouetteState reset(long newConnectionEpoch, String newDimension) {
		return empty(capacity, newConnectionEpoch, newDimension);
	}

	/** Resource reloads only recreate renderer resources and never invalidate semantic entries. */
	public ClientRankTenSilhouetteState rendererResourcesClosed() {
		return this;
	}

	/** Resource reloads only recreate renderer resources and never invalidate semantic entries. */
	public ClientRankTenSilhouetteState rendererResourcesRecreated() {
		return this;
	}

	public int capacity() { return capacity; }
	public long connectionEpoch() { return connectionEpoch; }
	public String dimension() { return dimension; }
	public long lifecycleTick() { return lifecycleTick; }

	/** Returns stable event-ID order for deterministic bounded renderer submission. */
	public List<Entry> entries() {
		return entries.values().stream().sorted(java.util.Comparator.comparingLong(Entry::eventId)).toList();
	}

	@Override
	public boolean equals(Object value) {
		if (this == value) return true;
		return value instanceof ClientRankTenSilhouetteState other && capacity == other.capacity
				&& connectionEpoch == other.connectionEpoch && lifecycleTick == other.lifecycleTick
				&& latestEventId == other.latestEventId
				&& dimension.equals(other.dimension) && entries.equals(other.entries);
	}

	@Override
	public int hashCode() {
		return Objects.hash(capacity, connectionEpoch, dimension, lifecycleTick, latestEventId, entries);
	}

	private static boolean valid(Wire wire) {
		if (wire == null || wire.eventId() <= 0 || RankTenSilhouetteProfile.fromNetworkId(
				wire.profileId()).isEmpty() || wire.caster() == null || !validDimension(wire.dimension())
				|| !RankTenSilhouetteGeometry.validWorldCoordinates(wire.x(), wire.y(), wire.z())
				|| !finite(wire.yaw(), wire.pitch())
				|| (wire.alignmentId() != 0 && wire.alignmentId() != 1)
				|| wire.lifetimeTicks() < MIN_LIFETIME_TICKS || wire.lifetimeTicks() > MAX_LIFETIME_TICKS) return false;
		return true;
	}

	private static boolean validDimension(String value) {
		return value != null && !value.isBlank() && value.length() <= 128;
	}

	private static boolean finite(double... values) {
		for (double value : values) if (!Double.isFinite(value)) return false;
		return true;
	}

	/** Compact pure wire representation; packet codecs belong to the later network task. */
	public record Wire(long eventId, int profileId, UUID caster, String dimension,
			double x, double y, double z, float yaw, float pitch, int alignmentId,
		int visualSeed, int lifetimeTicks) {
		public Wire(long eventId, int profileId, UUID caster, String dimension,
				double x, double y, double z, float yaw, float pitch, int alignmentId,
				int visualSeed) {
			this(eventId, profileId, caster, dimension, x, y, z, yaw, pitch, alignmentId,
					visualSeed, AUTHORED_LIFETIME_TICKS);
		}
		public RankTenSilhouetteGeometry.Event event(double phase) {
			return new RankTenSilhouetteGeometry.Event(eventId, profileId, caster, dimension,
					x, y, z, yaw, pitch, alignmentId, visualSeed, lifetimeTicks, phase);
		}
	}

	/** One accepted wire with its receipt-local saturation-safe expiry. */
	public record Entry(Wire wire, long expiresAt) {
		public Entry {
			wire = Objects.requireNonNull(wire, "wire");
			if (!valid(wire) || expiresAt < 0) throw new IllegalArgumentException("invalid silhouette entry");
		}
		public long eventId() { return wire.eventId(); }
	}
}
