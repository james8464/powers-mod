package com.powers.realm;

import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Progressively materialises loaded mindscape landmarks under one global edit budget. */
public final class RealmLandmarkConstruction {
	public static final int BLOCK_EDITS_PER_PULSE = 128;
	private static final Map<String, Task> TASKS = new LinkedHashMap<>();

	private record Task(String dimension, String siteId,
			List<RealmBlockPlacement> placements, RealmConstructionCursor cursor) {
	}

	private RealmLandmarkConstruction() {
	}

	public static void tick(ServerLevel level, RealmKind kind, RealmLandmarkSavedData data) {
		String dimension = level.dimension().identifier().toString();
		List<MemorySite> sites = RealmLayout.sites(kind);
		for (MemorySite site : sites) {
			String key = key(dimension, site.id());
			if (data.missing(dimension, List.of(site.id())).isEmpty()) {
				TASKS.remove(key);
				continue;
			}
			BlockPos center = new BlockPos(site.x(), RealmTerrain.provisionalArrivalY(level), site.z());
			if (!LoadedChunks.contains(level, center)) continue;
			TASKS.computeIfAbsent(key, ignored -> {
				List<RealmBlockPlacement> blueprint = RealmLandmarkBlueprint.create(
						kind, site, RealmTerrain.floorY(level, site.x(), site.z()));
				return new Task(dimension, site.id(), blueprint,
						new RealmConstructionCursor(blueprint.size()));
			});
		}

		int remaining = BLOCK_EDITS_PER_PULSE;
		for (var iterator = TASKS.entrySet().iterator(); iterator.hasNext() && remaining > 0;) {
			Task task = iterator.next().getValue();
			if (!task.dimension().equals(dimension)) continue;
			int start = task.cursor().position();
			int available = Math.min(remaining, task.placements().size() - start);
			int loaded = loadedPrefix(level, task.placements(), start, available);
			if (loaded == 0) continue;
			int claimed = task.cursor().claim(loaded);
			for (int index = start; index < start + claimed; index++) {
				RealmBlockPlacement placement = task.placements().get(index);
				if (!level.getBlockState(placement.position()).is(placement.block())) {
					level.setBlockAndUpdate(placement.position(), placement.block().defaultBlockState());
				}
			}
			remaining -= claimed;
			if (task.cursor().complete()) {
				data.complete(dimension, task.siteId());
				iterator.remove();
			}
		}
	}

	private static int loadedPrefix(ServerLevel level, List<RealmBlockPlacement> placements,
			int start, int limit) {
		int count = 0;
		while (count < limit && LoadedChunks.contains(level, placements.get(start + count).position())) {
			count++;
		}
		return count;
	}

	private static String key(String dimension, String site) {
		return dimension + '\u0000' + site;
	}

	public static int activeTaskCount() {
		return TASKS.size();
	}

	public static void clear() {
		TASKS.clear();
	}
}
