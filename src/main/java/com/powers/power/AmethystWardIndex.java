package com.powers.power;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Chunk-bucketed powered-ward index with bounded local queries. */
public final class AmethystWardIndex {
	private final Map<Long, Set<BlockPos>> byChunk = new HashMap<>();
	private int size;

	public void add(BlockPos pos) {
		BlockPos immutable = pos.immutable();
		long chunk = ChunkPos.pack(immutable.getX() >> 4, immutable.getZ() >> 4);
		if (byChunk.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(immutable)) size++;
	}

	public void remove(BlockPos pos) {
		long chunk = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
		Set<BlockPos> positions = byChunk.get(chunk);
		if (positions == null || !positions.remove(pos)) return;
		size--;
		if (positions.isEmpty()) byChunk.remove(chunk);
	}

	public List<BlockPos> nearby(BlockPos center, int radius) {
		int safeRadius = Math.max(0, radius);
		int minChunkX = (center.getX() - safeRadius) >> 4;
		int maxChunkX = (center.getX() + safeRadius) >> 4;
		int minChunkZ = (center.getZ() - safeRadius) >> 4;
		int maxChunkZ = (center.getZ() + safeRadius) >> 4;
		long radiusSquared = (long) safeRadius * safeRadius;
		List<BlockPos> result = new ArrayList<>();
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				Set<BlockPos> positions = byChunk.get(ChunkPos.pack(chunkX, chunkZ));
				if (positions == null) continue;
				for (BlockPos pos : positions) {
					if (center.distSqr(pos) <= radiusSquared) result.add(pos);
				}
			}
		}
		result.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
		return result;
	}

	public int size() {
		return size;
	}

	public void clear() {
		byChunk.clear();
		size = 0;
	}
}
