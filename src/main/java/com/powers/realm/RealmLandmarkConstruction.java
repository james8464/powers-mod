package com.powers.realm;

import com.powers.PowersMod;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Progressively materialises loaded mindscape landmarks under one global edit budget. */
public final class RealmLandmarkConstruction {
	public static final int BLOCK_EDITS_PER_PULSE = 128;
	private static final Map<String, Task> TASKS = new LinkedHashMap<>();

	private record Task(String dimension, MemorySite site, int floorY,
			List<RealmLandmarkTemplates.Piece> pieces, RealmConstructionCursor cursor) {
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
				List<RealmLandmarkTemplates.Piece> pieces = RealmLandmarkTemplates.pieces(site.id());
				return new Task(dimension, site, RealmTerrain.floorY(level, site.x(), site.z()),
						pieces, new RealmConstructionCursor(pieces.size()));
			});
		}

		int remaining = BLOCK_EDITS_PER_PULSE;
		for (var iterator = TASKS.entrySet().iterator(); iterator.hasNext() && remaining > 0;) {
			Task task = iterator.next().getValue();
			if (!task.dimension().equals(dimension)) continue;
			int index = task.cursor().position();
			if (index >= task.pieces().size()) continue;
			RealmLandmarkTemplates.Piece piece = task.pieces().get(index);
			if (piece.blocks() > remaining || !placePiece(level, kind, task, piece)) continue;
			task.cursor().claim(1);
			remaining -= piece.blocks();
			if (task.cursor().complete()) {
				data.complete(dimension, task.site().id());
				iterator.remove();
			}
		}
	}

	private static boolean placePiece(ServerLevel level, RealmKind kind, Task task,
			RealmLandmarkTemplates.Piece piece) {
		StructureTemplate template = level.getStructureManager().get(piece.template()).orElse(null);
		if (template == null) return false;
		BlockPos origin = new BlockPos(task.site().x() + piece.offsetX(),
				task.floorY() + piece.offsetY(), task.site().z() + piece.offsetZ());
		BlockPos far = origin.offset(template.getSize().getX() - 1,
				template.getSize().getY() - 1, template.getSize().getZ() - 1);
		if (!LoadedChunks.contains(level, origin) || !LoadedChunks.contains(level, far)) return false;
		StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
		var registry = level.registryAccess().lookupOrThrow(Registries.PROCESSOR_LIST);
		ResourceKey<StructureProcessorList> processorKey = ResourceKey.create(Registries.PROCESSOR_LIST,
				PowersMod.id(kind == RealmKind.LIGHT ? "realm/light" : "realm/dark"));
		StructureProcessorList processors = registry.getValue(processorKey);
		if (processors != null) processors.list().forEach(settings::addProcessor);
		return template.placeInWorld(level, origin, origin, settings,
				RandomSource.create(piece.template().hashCode()),
				Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
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
