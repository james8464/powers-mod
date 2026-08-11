package com.powers.magic.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Server-thread-owned spatial index for active magic. Horizontal cells mirror
 * Minecraft's chunk-oriented lookup while an exact three-dimensional sphere
 * test filters candidates. Reverse maps make player and server cleanup
 * deterministic rather than relying on a future expiry scan.
 */
public final class ActiveMagicIndex {
	private record CellKey(String dimension, int x, int z) {
	}

	private final int cellSize;
	private final Map<CellKey, Set<MagicPresenceId>> cells = new HashMap<>();
	private final Map<MagicPresenceId, MagicPresence> presences = new HashMap<>();
	private final Map<MagicPresenceId, Set<CellKey>> memberships = new HashMap<>();
	private final Map<UUID, Set<MagicPresenceId>> byOwner = new HashMap<>();
	private final TreeMap<Long, Set<MagicPresenceId>> byExpiry = new TreeMap<>();

	/**
	 * Creates an index whose cells have the supplied horizontal block size.
	 * Sixteen aligns with chunks and is the production default.
	 */
	public ActiveMagicIndex(int cellSize) {
		if (cellSize <= 0 || cellSize > 128) {
			throw new IllegalArgumentException("Cell size must be within 1..128");
		}
		this.cellSize = cellSize;
	}

	/** Registers a new presence and rejects accidental owner-token reuse. */
	public void register(MagicPresence presence) {
		Objects.requireNonNull(presence, "presence");
		if (presences.putIfAbsent(presence.id(), presence) != null) {
			throw new IllegalArgumentException("Duplicate magic presence: " + presence.id());
		}
		Set<CellKey> occupied = cellsFor(presence.dimension(), presence.anchor(), presence.radius());
		memberships.put(presence.id(), occupied);
		for (CellKey cell : occupied) {
			cells.computeIfAbsent(cell, ignored -> new LinkedHashSet<>()).add(presence.id());
		}
		byOwner.computeIfAbsent(presence.owner(), ignored -> new LinkedHashSet<>()).add(presence.id());
		scheduleExpiry(presence.id(), presence.expiresAt());
	}

	/**
	 * Moves a registered presence atomically between cells. Returns false when
	 * the token no longer exists, allowing entity-removal races to end quietly.
	 */
	public boolean move(MagicPresenceId id, String dimension, PresenceAnchor anchor) {
		Objects.requireNonNull(id, "id");
		MagicPresence current = presences.get(id);
		if (current == null) return false;
		return replace(current.moved(dimension, anchor));
	}

	/** Reanchors a residue to a physical object and adopts its authoritative expiry. */
	public boolean rebind(MagicPresenceId id, String dimension, PresenceAnchor anchor, long expiresAt) {
		Objects.requireNonNull(id, "id");
		MagicPresence current = presences.get(id);
		if (current == null) return false;
		return replace(current.rebound(dimension, anchor, expiresAt));
	}

	private boolean replace(MagicPresence moved) {
		MagicPresenceId id = moved.id();
		MagicPresence previous = presences.get(id);
		if (previous == null) return false;
		removeCellMembership(id);
		unscheduleExpiry(id, previous.expiresAt());
		presences.put(id, moved);
		Set<CellKey> occupied = cellsFor(moved.dimension(), moved.anchor(), moved.radius());
		memberships.put(id, occupied);
		for (CellKey cell : occupied) {
			cells.computeIfAbsent(cell, ignored -> new LinkedHashSet<>()).add(id);
		}
		scheduleExpiry(id, moved.expiresAt());
		return true;
	}

	/**
	 * Returns live presences whose spheres intersect the query sphere. Expired
	 * state is removed before lookup, making reads authoritative for the tick.
	 */
	public List<MagicPresence> nearby(String dimension, double x, double y, double z,
			double radius, long gameTime) {
		Objects.requireNonNull(dimension, "dimension");
		PresenceAnchor query = PresenceAnchor.fixed(x, y, z);
		if (!Double.isFinite(radius) || radius < 0.0 || radius > 128.0) {
			throw new IllegalArgumentException("Query radius must be finite and within 0..128");
		}
		expire(gameTime);
		Set<MagicPresenceId> candidates = new LinkedHashSet<>();
		for (CellKey cell : cellsFor(dimension, query, radius)) {
			candidates.addAll(cells.getOrDefault(cell, Set.of()));
		}
		List<MagicPresence> result = new ArrayList<>();
		for (MagicPresenceId id : candidates) {
			MagicPresence presence = presences.get(id);
			if (presence == null || !presence.dimension().equals(dimension)) continue;
			double combined = radius + presence.radius();
			double dx = x - presence.anchor().x();
			double dy = y - presence.anchor().y();
			double dz = z - presence.anchor().z();
			if (dx * dx + dy * dy + dz * dz <= combined * combined) result.add(presence);
		}
		return List.copyOf(result);
	}

	/** Removes a specific presence and every reverse-index entry it owns. */
	public boolean remove(MagicPresenceId id) {
		MagicPresence removed = presences.remove(Objects.requireNonNull(id, "id"));
		if (removed == null) return false;
		removeCellMembership(id);
		unscheduleExpiry(id, removed.expiresAt());
		Set<MagicPresenceId> ownerEntries = byOwner.get(removed.owner());
		if (ownerEntries != null) {
			ownerEntries.remove(id);
			if (ownerEntries.isEmpty()) byOwner.remove(removed.owner());
		}
		return true;
	}

	/** Removes every presence owned by a disconnecting or dying player. */
	public int removeOwner(UUID owner) {
		Set<MagicPresenceId> owned = byOwner.get(Objects.requireNonNull(owner, "owner"));
		if (owned == null) return 0;
		List<MagicPresenceId> copy = List.copyOf(owned);
		copy.forEach(this::remove);
		return copy.size();
	}

	/** Removes every presence whose expiry tick has been reached. */
	public int expire(long gameTime) {
		if (gameTime < 0L) throw new IllegalArgumentException("Game time cannot be negative");
		List<MagicPresenceId> expired = byExpiry.headMap(gameTime, true).values().stream()
				.flatMap(Collection::stream).toList();
		expired.forEach(this::remove);
		return expired.size();
	}

	/** Clears all state during server stop or full runtime reload. */
	public void clear() {
		cells.clear();
		presences.clear();
		memberships.clear();
		byOwner.clear();
		byExpiry.clear();
	}

	/** Returns active presence count for diagnostics and invariant tests. */
	public int size() {
		return presences.size();
	}

	/** Returns allocated cell count for diagnostics and leak tests. */
	public int cellCount() {
		return cells.size();
	}

	/** Exact lookup used by the physical-handle bridge on the server thread. */
	MagicPresence get(MagicPresenceId id) {
		return presences.get(id);
	}

	private Set<CellKey> cellsFor(String dimension, PresenceAnchor anchor, double radius) {
		int minX = floorCell(anchor.x() - radius);
		int maxX = floorCell(anchor.x() + radius);
		int minZ = floorCell(anchor.z() - radius);
		int maxZ = floorCell(anchor.z() + radius);
		Set<CellKey> result = new LinkedHashSet<>();
		for (int cellX = minX; cellX <= maxX; cellX++) {
			for (int cellZ = minZ; cellZ <= maxZ; cellZ++) {
				result.add(new CellKey(dimension, cellX, cellZ));
			}
		}
		return result;
	}

	private int floorCell(double coordinate) {
		return (int) Math.floor(coordinate / cellSize);
	}

	private void removeCellMembership(MagicPresenceId id) {
		Collection<CellKey> occupied = memberships.remove(id);
		if (occupied == null) return;
		for (CellKey cell : occupied) {
			Set<MagicPresenceId> entries = cells.get(cell);
			if (entries == null) continue;
			entries.remove(id);
			if (entries.isEmpty()) cells.remove(cell);
		}
	}

	private void scheduleExpiry(MagicPresenceId id, long expiresAt) {
		byExpiry.computeIfAbsent(expiresAt, ignored -> new LinkedHashSet<>()).add(id);
	}

	private void unscheduleExpiry(MagicPresenceId id, long expiresAt) {
		Set<MagicPresenceId> bucket = byExpiry.get(expiresAt);
		if (bucket == null) return;
		bucket.remove(id);
		if (bucket.isEmpty()) byExpiry.remove(expiresAt);
	}
}
