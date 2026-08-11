package com.powers.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Server-thread-owned horizontal spatial index for bounded fields and anchors.
 * Entries occupy only the chunk-sized cells touched by their radius, while an
 * exact circle-intersection check prevents cell-edge false positives.
 */
public final class ChunkSpatialIndex<K, V> {
	private static final double MAX_RADIUS = 256.0;

	private record Cell(String dimension, int x, int z) {
	}

	private record Entry<V>(String dimension, double x, double z, double radius, V value) {
	}

	private static final class WorkCounters {
		private long queries;
		private long candidates;
		private long misses;
		private long staleRemovals;
	}

	private final int cellSize;
	private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
	private final Map<Cell, Set<K>> cells = new HashMap<>();
	private final Map<K, Set<Cell>> memberships = new HashMap<>();
	private final Map<String, WorkCounters> workByDimension = new HashMap<>();

	/** Bounded operational counters; memory is an intentionally conservative estimate. */
	public record Diagnostics(long queries, long candidates, long misses, long staleRemovals,
			int entries, int cells, int memberships, long estimatedBytes) {
	}

	public ChunkSpatialIndex(int cellSize) {
		if (cellSize < 1 || cellSize > 128) {
			throw new IllegalArgumentException("Cell size must be within 1..128");
		}
		this.cellSize = cellSize;
	}

	/** Adds or atomically replaces one indexed value. */
	public void put(K key, String dimension, double x, double z, double radius, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(dimension, "dimension");
		Objects.requireNonNull(value, "value");
		validateGeometry(x, z, radius);
		remove(key);
		Entry<V> entry = new Entry<>(dimension, x, z, radius, value);
		entries.put(key, entry);
		Set<Cell> occupied = cellsFor(dimension, x, z, radius);
		memberships.put(key, occupied);
		for (Cell cell : occupied) {
			cells.computeIfAbsent(cell, ignored -> new LinkedHashSet<>()).add(key);
		}
	}

	/** Returns stable, de-duplicated values intersecting a horizontal query circle. */
	public List<V> nearby(String dimension, double x, double z, double radius) {
		Objects.requireNonNull(dimension, "dimension");
		validateGeometry(x, z, radius);
		WorkCounters work = workByDimension.computeIfAbsent(dimension, ignored -> new WorkCounters());
		Set<K> candidates = new LinkedHashSet<>();
		for (Cell cell : cellsFor(dimension, x, z, radius)) {
			candidates.addAll(cells.getOrDefault(cell, Set.of()));
		}
		work.queries++;
		work.candidates += candidates.size();
		List<V> result = new ArrayList<>();
		for (K key : candidates) {
			Entry<V> entry = entries.get(key);
			if (entry == null || !entry.dimension().equals(dimension)) continue;
			double combined = radius + entry.radius();
			double dx = x - entry.x();
			double dz = z - entry.z();
			if (dx * dx + dz * dz <= combined * combined) result.add(entry.value());
		}
		if (result.isEmpty()) work.misses++;
		return List.copyOf(result);
	}

	/** Removes one key and all occupied-cell memberships. */
	public boolean remove(K key) {
		Entry<V> removed = entries.remove(key);
		if (removed == null) return false;
		Collection<Cell> occupied = memberships.remove(key);
		if (occupied != null) {
			for (Cell cell : occupied) {
				Set<K> keys = cells.get(cell);
				if (keys == null) continue;
				keys.remove(key);
				if (keys.isEmpty()) cells.remove(cell);
			}
		}
		return true;
	}

	/** Removes an entry rejected by authoritative world state and records the repair. */
	public boolean removeStale(K key) {
		Entry<V> entry = entries.get(key);
		boolean removed = remove(key);
		if (removed && entry != null) {
			workByDimension.computeIfAbsent(entry.dimension(), ignored -> new WorkCounters())
					.staleRemovals++;
		}
		return removed;
	}

	public void clear() {
		entries.clear();
		cells.clear();
		memberships.clear();
		workByDimension.clear();
	}

	public int size() {
		return entries.size();
	}

	public int cellCount() {
		return cells.size();
	}

	public Diagnostics diagnostics() {
		long queries = 0L, candidates = 0L, misses = 0L, stale = 0L, memory = 0L;
		for (Diagnostics value : diagnosticsByDimension().values()) {
			queries += value.queries();
			candidates += value.candidates();
			misses += value.misses();
			stale += value.staleRemovals();
			memory += value.estimatedBytes();
		}
		return new Diagnostics(queries, candidates, misses, stale,
				entries.size(), cells.size(), memberships.size(), memory);
	}

	/** Per-dimension counters used by operator diagnostics without scanning world state. */
	public Map<String, Diagnostics> diagnosticsByDimension() {
		Set<String> dimensions = new java.util.TreeSet<>(workByDimension.keySet());
		entries.values().forEach(entry -> dimensions.add(entry.dimension()));
		Map<String, Diagnostics> result = new TreeMap<>();
		for (String dimension : dimensions) {
			WorkCounters work = workByDimension.getOrDefault(dimension, new WorkCounters());
			int entryCount = (int) entries.values().stream()
					.filter(entry -> entry.dimension().equals(dimension)).count();
			int cellCount = (int) cells.keySet().stream()
					.filter(cell -> cell.dimension().equals(dimension)).count();
			long membershipCount = cells.entrySet().stream()
					.filter(entry -> entry.getKey().dimension().equals(dimension))
					.mapToLong(entry -> entry.getValue().size()).sum();
			long estimatedBytes = entryCount * 96L + cellCount * 80L
					+ entryCount * 48L + membershipCount * 24L;
			result.put(dimension, new Diagnostics(work.queries, work.candidates, work.misses,
					work.staleRemovals, entryCount, cellCount, entryCount, estimatedBytes));
		}
		return Collections.unmodifiableMap(result);
	}

	private Set<Cell> cellsFor(String dimension, double x, double z, double radius) {
		int minX = floorCell(x - radius);
		int maxX = floorCell(x + radius);
		int minZ = floorCell(z - radius);
		int maxZ = floorCell(z + radius);
		Set<Cell> occupied = new LinkedHashSet<>();
		for (int cellX = minX; cellX <= maxX; cellX++) {
			for (int cellZ = minZ; cellZ <= maxZ; cellZ++) {
				occupied.add(new Cell(dimension, cellX, cellZ));
			}
		}
		return occupied;
	}

	private int floorCell(double coordinate) {
		return (int) Math.floor(coordinate / cellSize);
	}

	private static void validateGeometry(double x, double z, double radius) {
		if (!Double.isFinite(x) || !Double.isFinite(z)
				|| !Double.isFinite(radius) || radius < 0.0 || radius > MAX_RADIUS) {
			throw new IllegalArgumentException("Index geometry must be finite with radius within 0..256");
		}
	}
}
