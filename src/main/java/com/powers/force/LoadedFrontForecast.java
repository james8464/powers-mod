package com.powers.force;

import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Immutable bounded observation of authoritative frontier entries in loaded chunks. */
public record LoadedFrontForecast(int observedFronts, int nearestDistanceBlocks,
		boolean truncated) {
	private static final int MAX_OBSERVED_FRONTS = 128;
	private static final int MAX_INSPECTED_ENTRIES = 512;

	public static LoadedFrontForecast none() {
		return new LoadedFrontForecast(0, -1, false);
	}

	public boolean observed() {
		return observedFronts > 0;
	}

	/** Reads the manager-owned loaded frontier index without loading a missing chunk. */
	public static LoadedFrontForecast read(ServerLevel level, BlockPos center, int radius,
			LivingForceKind kind) {
		return observe(level, LivingForceManager.index(level), center, radius, kind);
	}

	static LoadedFrontForecast observe(ServerLevel level, LivingForceIndex index, BlockPos center,
			int radius, LivingForceKind kind) {
		Vec3 origin = Vec3.atCenterOf(center);
		LivingForceIndex.Sample sample = index.sampleWithin(origin.x, origin.y, origin.z, radius,
				kind, MAX_OBSERVED_FRONTS + 1, MAX_INSPECTED_ENTRIES);
		List<Long> candidates = sample.positions();
		int observed = 0;
		double nearestSquared = Double.POSITIVE_INFINITY;
		for (long packed : candidates) {
			BlockPos position = BlockPos.of(packed);
			if (!LoadedChunks.contains(level, position)) continue;
			if (!level.getBlockState(position).is(kind.block())) {
				index.removeStale(packed);
				continue;
			}
			if (observed >= MAX_OBSERVED_FRONTS) continue;
			observed++;
			nearestSquared = Math.min(nearestSquared,
					position.distToCenterSqr(origin.x, origin.y, origin.z));
		}
		boolean truncated = sample.truncated() || candidates.size() > MAX_OBSERVED_FRONTS;
		if (observed == 0) return new LoadedFrontForecast(0, -1, truncated);
		return new LoadedFrontForecast(observed, (int) Math.ceil(Math.sqrt(nearestSquared)), truncated);
	}
}
