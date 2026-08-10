package com.powers.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

	private final int cellSize;
	private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
	private final Map<Cell, Set<K>> cells = new HashMap<>();
	private final Map<K, Set<Cell>> memberships = new HashMap<>();

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
		Set<K> candidates = new LinkedHashSet<>();
		for (Cell cell : cellsFor(dimension, x, z, radius)) {
			candidates.addAll(cells.getOrDefault(cell, Set.of()));
		}
		List<V> result = new ArrayList<>();
		for (K key : candidates) {
			Entry<V> entry = entries.get(key);
			if (entry == null || !entry.dimension().equals(dimension)) continue;
			double combined = radius + entry.radius();
			double dx = x - entry.x();
			double dz = z - entry.z();
			if (dx * dx + dz * dz <= combined * combined) result.add(entry.value());
		}
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

	public void clear() {
		entries.clear();
		cells.clear();
		memberships.clear();
	}

	public int size() {
		return entries.size();
	}

	public int cellCount() {
		return cells.size();
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
