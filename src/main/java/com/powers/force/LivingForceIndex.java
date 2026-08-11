package com.powers.force;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chunk-bucketed in-memory index of loaded darkness and pure-light blocks. */
final class LivingForceIndex {
	private static final class ChunkBucket {
		private final Map<Long, LivingForceKind> entries = new HashMap<>();
		private int darkness;
		private int pureLight;

		LivingForceKind put(long position, LivingForceKind kind) {
			LivingForceKind previous = entries.put(position, kind);
			if (previous != kind) {
				adjust(previous, -1);
				adjust(kind, 1);
			}
			return previous;
		}

		LivingForceKind remove(long position) {
			LivingForceKind removed = entries.remove(position);
			adjust(removed, -1);
			return removed;
		}

		boolean has(LivingForceKind kind) {
			return kind == LivingForceKind.DARKNESS ? darkness > 0 : pureLight > 0;
		}

		private void adjust(LivingForceKind kind, int delta) {
			if (kind == LivingForceKind.DARKNESS) darkness += delta;
			else if (kind == LivingForceKind.PURE_LIGHT) pureLight += delta;
		}
	}

	private final Map<Long, ChunkBucket> chunks = new HashMap<>();
	private int size;
	private long queries;
	private long candidates;
	private long misses;
	private long staleRemovals;

	record Diagnostics(long queries, long candidates, long misses, long staleRemovals,
			int entries, int chunks, long estimatedBytes) {
	}

	void add(long position, LivingForceKind kind) {
		long chunk = ChunkPos.pack(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
		ChunkBucket bucket = chunks.computeIfAbsent(chunk, ignored -> new ChunkBucket());
		if (bucket.put(position, kind) == null) size++;
	}

	void remove(long position) {
		long chunk = ChunkPos.pack(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
		ChunkBucket entries = chunks.get(chunk);
		if (entries == null || entries.remove(position) == null) return;
		size--;
		if (entries.entries.isEmpty()) chunks.remove(chunk);
	}

	void removeStale(long position) {
		int before = size;
		remove(position);
		if (size < before) staleRemovals++;
	}

	void removeChunk(long chunk) {
		ChunkBucket removed = chunks.remove(chunk);
		if (removed != null) size -= removed.entries.size();
	}

	List<Long> within(double x, double y, double z, double radius, LivingForceKind kind) {
		return within(x, y, z, radius, kind, Integer.MAX_VALUE);
	}

	/** Range query that stops collecting once the caller's fixed work allowance is full. */
	List<Long> within(double x, double y, double z, double radius, LivingForceKind kind, int limit) {
		if (radius < 0.0 || !Double.isFinite(radius) || limit <= 0) return List.of();
		queries++;
		int minChunkX = ((int) Math.floor(x - radius)) >> 4;
		int maxChunkX = ((int) Math.floor(x + radius)) >> 4;
		int minChunkZ = ((int) Math.floor(z - radius)) >> 4;
		int maxChunkZ = ((int) Math.floor(z + radius)) >> 4;
		double radiusSquared = radius * radius;
		List<Long> result = new ArrayList<>();
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				ChunkBucket bucket = chunks.get(ChunkPos.pack(chunkX, chunkZ));
				Map<Long, LivingForceKind> entries = bucket == null ? null : bucket.entries;
				if (entries == null) continue;
				for (Map.Entry<Long, LivingForceKind> entry : entries.entrySet()) {
					candidates++;
					if (entry.getValue() != kind) continue;
					long position = entry.getKey();
					double dx = BlockPos.getX(position) + 0.5 - x;
					double dy = BlockPos.getY(position) + 0.5 - y;
					double dz = BlockPos.getZ(position) + 0.5 - z;
					if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
						result.add(position);
						if (result.size() >= limit) return List.copyOf(result);
					}
				}
			}
		}
		if (result.isEmpty()) misses++;
		return List.copyOf(result);
	}

	/** Returns only loaded chunk buckets that contain the requested force. */
	List<Long> chunksWith(LivingForceKind kind) {
		List<Long> result = new ArrayList<>();
		for (Map.Entry<Long, ChunkBucket> entry : chunks.entrySet()) {
			if (entry.getValue().has(kind)) result.add(entry.getKey());
		}
		return List.copyOf(result);
	}

	int size() {
		return size;
	}

	Diagnostics diagnostics() {
		return new Diagnostics(queries, candidates, misses, staleRemovals,
				size, chunks.size(), chunks.size() * 96L + size * 48L);
	}

	void clear() {
		chunks.clear();
		size = 0;
		queries = candidates = misses = staleRemovals = 0L;
	}
}
