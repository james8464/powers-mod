package com.powers.force;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chunk-bucketed in-memory index of loaded darkness and pure-light blocks. */
final class LivingForceIndex {
	private final Map<Long, Map<Long, LivingForceKind>> chunks = new HashMap<>();
	private int size;

	void add(long position, LivingForceKind kind) {
		long chunk = ChunkPos.pack(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
		Map<Long, LivingForceKind> entries = chunks.computeIfAbsent(chunk, ignored -> new HashMap<>());
		if (entries.put(position, kind) == null) size++;
	}

	void remove(long position) {
		long chunk = ChunkPos.pack(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
		Map<Long, LivingForceKind> entries = chunks.get(chunk);
		if (entries == null || entries.remove(position) == null) return;
		size--;
		if (entries.isEmpty()) chunks.remove(chunk);
	}

	void removeChunk(long chunk) {
		Map<Long, LivingForceKind> removed = chunks.remove(chunk);
		if (removed != null) size -= removed.size();
	}

	List<Long> within(double x, double y, double z, double radius, LivingForceKind kind) {
		if (radius < 0.0 || !Double.isFinite(radius)) return List.of();
		int minChunkX = ((int) Math.floor(x - radius)) >> 4;
		int maxChunkX = ((int) Math.floor(x + radius)) >> 4;
		int minChunkZ = ((int) Math.floor(z - radius)) >> 4;
		int maxChunkZ = ((int) Math.floor(z + radius)) >> 4;
		double radiusSquared = radius * radius;
		List<Long> result = new ArrayList<>();
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				Map<Long, LivingForceKind> entries = chunks.get(ChunkPos.pack(chunkX, chunkZ));
				if (entries == null) continue;
				for (Map.Entry<Long, LivingForceKind> entry : entries.entrySet()) {
					if (entry.getValue() != kind) continue;
					long position = entry.getKey();
					double dx = BlockPos.getX(position) + 0.5 - x;
					double dy = BlockPos.getY(position) + 0.5 - y;
					double dz = BlockPos.getZ(position) + 0.5 - z;
					if (dx * dx + dy * dy + dz * dz <= radiusSquared) result.add(position);
				}
			}
		}
		return List.copyOf(result);
	}

	int size() {
		return size;
	}

	void clear() {
		chunks.clear();
		size = 0;
	}
}
