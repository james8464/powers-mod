package com.powers.power.abilities;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Finite identity-only leases for loaded vulnerable body proxies. */
public final class LocalBodyAnchorRegistry {
	public static final int MAX_ACTIVE = 256;
	private static final LocalBodyAnchorRegistry GLOBAL =
			new LocalBodyAnchorRegistry(MAX_ACTIVE);

	private final int capacity;
	private final Map<UUID, Lease> leases = new HashMap<>();

	public LocalBodyAnchorRegistry(int capacity) {
		if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
		this.capacity = capacity;
	}

	public static LocalBodyAnchorRegistry global() {
		return GLOBAL;
	}

	public boolean bind(UUID bodyId, String dimensionId, long now, int durationTicks) {
		if (bodyId == null || Identifier.tryParse(dimensionId) == null) return false;
		Lease current = live(bodyId, now);
		if (current == null && leases.size() >= capacity) {
			leases.values().removeIf(lease -> lease.deadline() <= now);
		}
		if (current == null && leases.size() >= capacity) return false;
		long deadline = DimensionalStabilizationRules.extendedDeadline(
				now, current == null ? now : current.deadline(), durationTicks);
		leases.put(bodyId, new Lease(dimensionId, deadline));
		return true;
	}

	public String dimensionId(UUID bodyId, long now) {
		Lease lease = live(bodyId, now);
		return lease == null ? null : lease.dimensionId();
	}

	public long deadline(UUID bodyId, long now) {
		Lease lease = live(bodyId, now);
		return lease == null ? 0L : lease.deadline();
	}

	public void remove(UUID bodyId) {
		if (bodyId != null) leases.remove(bodyId);
	}

	public void clear() {
		leases.clear();
	}

	public int size() {
		return leases.size();
	}

	private Lease live(UUID bodyId, long now) {
		if (bodyId == null) return null;
		Lease lease = leases.get(bodyId);
		if (lease != null && lease.deadline() <= now) {
			leases.remove(bodyId);
			return null;
		}
		return lease;
	}

	private record Lease(String dimensionId, long deadline) { }
}
