package com.powers.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Small horizontal cell index rebuilt once per server tick for nearby FX recipients. */
public final class ViewerSpatialIndex<T> {
	private record Entry<T>(T value, double x, double z) {
	}

	private final int cellSize;
	private final Map<Long, List<Entry<T>>> cells = new HashMap<>();
	private final List<Entry<T>> entries = new ArrayList<>();
	private int size;

	public ViewerSpatialIndex(int cellSize) {
		if (cellSize < 1 || cellSize > 128) throw new IllegalArgumentException("Invalid cell size");
		this.cellSize = cellSize;
	}

	public void put(T value, double x, double z) {
		Objects.requireNonNull(value, "value");
		if (!Double.isFinite(x) || !Double.isFinite(z)) return;
		Entry<T> entry = new Entry<>(value, x, z);
		cells.computeIfAbsent(key(cell(x), cell(z)), ignored -> new ArrayList<>()).add(entry);
		entries.add(entry);
		size++;
	}

	public List<T> nearby(double x, double z, double radius) {
		if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(radius)
				|| radius < 0.0 || radius > FxLodScope.CATASTROPHIC.maximumRange()) return List.of();
		if (radius > 256.0) return filter(entries, x, z, radius);
		int minX = cell(x - radius);
		int maxX = cell(x + radius);
		int minZ = cell(z - radius);
		int maxZ = cell(z + radius);
		double radiusSquared = radius * radius;
		List<T> result = new ArrayList<>();
		for (int cellX = minX; cellX <= maxX; cellX++) {
			for (int cellZ = minZ; cellZ <= maxZ; cellZ++) {
				for (Entry<T> entry : cells.getOrDefault(key(cellX, cellZ), List.of())) {
					double dx = x - entry.x();
					double dz = z - entry.z();
					if (dx * dx + dz * dz <= radiusSquared) result.add(entry.value());
				}
			}
		}
		return List.copyOf(result);
	}

	private static <T> List<T> filter(List<Entry<T>> candidates,
			double x, double z, double radius) {
		double radiusSquared = radius * radius;
		List<T> result = new ArrayList<>();
		for (Entry<T> entry : candidates) {
			double dx = x - entry.x();
			double dz = z - entry.z();
			if (dx * dx + dz * dz <= radiusSquared) result.add(entry.value());
		}
		return List.copyOf(result);
	}

	public void clear() {
		cells.clear();
		entries.clear();
		size = 0;
	}

	public int size() {
		return size;
	}

	private int cell(double coordinate) {
		return (int) Math.floor(coordinate / cellSize);
	}

	private static long key(int x, int z) {
		return (long) x << 32 ^ z & 0xFFFFFFFFL;
	}
}
