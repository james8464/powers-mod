package com.powers.power;

import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Lazy loaded-section tag index for natural amethyst suppression blocks. */
public final class NaturalAmethystIndex {
	public record Diagnostics(long queries, long candidates, long misses, long sectionScans,
			long staleRemovals, int sections, int positions, long estimatedBytes) { }

	private static final class WorkCounters {
		private long queries;
		private long candidates;
		private long misses;
		private long sectionScans;
		private long invalidations;
	}

	private final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> dimensions = new HashMap<>();
	private final Map<ResourceKey<Level>, WorkCounters> workByDimension = new HashMap<>();

	public boolean nearby(ServerLevel level, BlockPos center, int radius) {
		WorkCounters work = workByDimension.computeIfAbsent(level.dimension(), ignored -> new WorkCounters());
		work.queries++;
		for (long key : sectionKeys(center.getX(), center.getY(), center.getZ(), radius)) {
			Set<BlockPos> positions = section(level, key, work);
			for (BlockPos position : positions) {
				work.candidates++;
				if (Math.abs(position.getX() - center.getX()) <= radius
						&& Math.abs(position.getY() - center.getY()) <= radius
						&& Math.abs(position.getZ() - center.getZ()) <= radius) return true;
			}
		}
		work.misses++;
		return false;
	}

	/** Updates only a section that has already been indexed; unseen sections remain lazy. */
	public void blockChanged(ServerLevel level, BlockPos position, BlockState state) {
		Map<Long, Set<BlockPos>> sections = dimensions.get(level.dimension());
		if (sections == null) return;
		long key = SectionPos.asLong(position);
		Set<BlockPos> positions = sections.get(key);
		if (positions == null) return;
		if (state.is(AmethystDampening.AMETHYST_BLOCKS)) positions.add(position.immutable());
		else positions.remove(position);
	}

	/** Invalidates all vertical sections when a chunk loads, unloads, or is externally rewritten. */
	public void invalidateChunk(ServerLevel level, ChunkPos chunk) {
		Map<Long, Set<BlockPos>> sections = dimensions.get(level.dimension());
		if (sections == null) return;
		for (int sectionY = SectionPos.blockToSectionCoord(level.getMinY());
				sectionY <= SectionPos.blockToSectionCoord(level.getMaxY() - 1); sectionY++) {
			if (sections.remove(SectionPos.asLong(chunk.x(), sectionY, chunk.z())) != null) {
				workByDimension.computeIfAbsent(level.dimension(), ignored -> new WorkCounters())
						.invalidations++;
			}
		}
		if (sections.isEmpty()) dimensions.remove(level.dimension());
	}

	public Diagnostics diagnostics() {
		long queries = 0L, candidates = 0L, misses = 0L, scans = 0L, stale = 0L, memory = 0L;
		int sections = 0, positions = 0;
		for (Diagnostics value : diagnosticsByDimension().values()) {
			queries += value.queries();
			candidates += value.candidates();
			misses += value.misses();
			scans += value.sectionScans();
			stale += value.staleRemovals();
			sections += value.sections();
			positions += value.positions();
			memory += value.estimatedBytes();
		}
		return new Diagnostics(queries, candidates, misses, scans, stale, sections, positions, memory);
	}

	/** Per-dimension lazy-index work and footprint, keyed by registry dimension ID. */
	public Map<String, Diagnostics> diagnosticsByDimension() {
		Set<ResourceKey<Level>> keys = new HashSet<>(dimensions.keySet());
		keys.addAll(workByDimension.keySet());
		Map<String, Diagnostics> result = new TreeMap<>();
		for (ResourceKey<Level> key : keys) {
			WorkCounters work = workByDimension.getOrDefault(key, new WorkCounters());
			Map<Long, Set<BlockPos>> indexed = dimensions.getOrDefault(key, Map.of());
			int sections = indexed.size();
			int positions = indexed.values().stream().mapToInt(Set::size).sum();
			result.put(key.identifier().toString(), new Diagnostics(work.queries, work.candidates,
					work.misses, work.sectionScans, work.invalidations, sections, positions,
					sections * 80L + positions * 56L));
		}
		return Collections.unmodifiableMap(result);
	}

	public void clear() {
		dimensions.clear();
		workByDimension.clear();
	}

	private Set<BlockPos> section(ServerLevel level, long key, WorkCounters work) {
		Map<Long, Set<BlockPos>> sections = dimensions.computeIfAbsent(
				level.dimension(), ignored -> new HashMap<>());
		Set<BlockPos> cached = sections.get(key);
		if (cached != null) return cached;
		Set<BlockPos> indexed = new HashSet<>();
		int sectionX = SectionPos.x(key);
		int sectionY = SectionPos.y(key);
		int sectionZ = SectionPos.z(key);
		BlockPos origin = new BlockPos(sectionX << 4,
				Math.clamp(sectionY << 4, level.getMinY(), level.getMaxY() - 1), sectionZ << 4);
		if (LoadedChunks.contains(level, origin)) {
			work.sectionScans++;
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			int minY = Math.max(level.getMinY(), sectionY << 4);
			int maxY = Math.min(level.getMaxY(), (sectionY + 1) << 4);
			for (int y = minY; y < maxY; y++) {
				for (int x = sectionX << 4; x < (sectionX + 1) << 4; x++) {
					for (int z = sectionZ << 4; z < (sectionZ + 1) << 4; z++) {
						cursor.set(x, y, z);
						if (level.getBlockState(cursor).is(AmethystDampening.AMETHYST_BLOCKS)) {
							indexed.add(cursor.immutable());
						}
					}
				}
			}
		}
		sections.put(key, indexed);
		return indexed;
	}

	static List<Long> sectionKeys(int x, int y, int z, int radius) {
		int bounded = Math.max(0, radius);
		List<Long> result = new ArrayList<>(8);
		for (int sx = SectionPos.blockToSectionCoord(x - bounded);
				sx <= SectionPos.blockToSectionCoord(x + bounded); sx++) {
			for (int sy = SectionPos.blockToSectionCoord(y - bounded);
					sy <= SectionPos.blockToSectionCoord(y + bounded); sy++) {
				for (int sz = SectionPos.blockToSectionCoord(z - bounded);
						sz <= SectionPos.blockToSectionCoord(z + bounded); sz++) {
					result.add(SectionPos.asLong(sx, sy, sz));
				}
			}
		}
		return List.copyOf(result);
	}
}
