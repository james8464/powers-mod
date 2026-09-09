package com.powers.power.crystals;

import com.powers.protection.PowerProtection;
import com.powers.PowersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves and atomically preflights one bounded blueprint before world mutation. */
public final class CreationBlueprintService {
	private static final int MAX_TEMPLATE_MATERIALS = 32;
	private static final Set<String> DENIED_TEMPLATE_BLOCKS = Set.of(
			"minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block",
			"minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault");
	private static final Set<String> STRIPPED_TEMPLATE_BLOCKS = Set.of(
			"minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw");

	private CreationBlueprintService() { }

	public record Target(BlockPos pos, BlockState state) { }

	public static boolean place(ServerPlayer player, ServerLevel level, BlockPos origin,
			CreationBlueprintDefinition definition) {
		List<Target> targets;
		try {
			targets = resolve(level, origin, definition);
		} catch (UnsafeTemplateException rejected) {
			PowersMod.LOGGER.warn("Rejected unsafe block {} in creation template {}",
					rejected.blockId, definition.template());
			return false;
		}
		return CreationBlueprintPreflight.placeAtomically(targets,
				target -> mayPlace(player, level, target.pos()),
				target -> level.setBlockAndUpdate(target.pos(), target.state()))
				== CreationBlueprintPreflight.Result.PLACED;
	}

	static List<Target> resolve(ServerLevel level, BlockPos origin, CreationBlueprintDefinition definition) {
		if (!definition.isVanillaTemplate()) {
			return definition.blocks().stream().map(entry -> new Target(
					origin.offset(entry.offset().x(), entry.offset().y(), entry.offset().z()),
					BuiltInRegistries.BLOCK.getValue(entry.block()).defaultBlockState())).toList();
		}
		var template = level.getStructureManager().get(definition.template()).orElse(null);
		if (template == null || template.getSize().getX() > CreationBlueprintParser.MAX_SPAN
				|| template.getSize().getY() > CreationBlueprintParser.MAX_SPAN
				|| template.getSize().getZ() > CreationBlueprintParser.MAX_SPAN) {
			PowersMod.LOGGER.warn("Rejected missing/oversize creation template {} size={}",
					definition.template(), template == null ? "missing" : template.getSize());
			return List.of();
		}
		StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
		Map<BlockPos, BlockState> unique = new LinkedHashMap<>();
		Set<String> materials = new HashSet<>();
		BuiltInRegistries.BLOCK.stream().forEach(block -> {
			if (block == Blocks.AIR) return;
			for (var info : template.filterBlocks(origin, settings, block)) {
				if (info.state().isAir()) continue;
				String blockId = BuiltInRegistries.BLOCK.getKey(info.state().getBlock()).toString();
				if (DENIED_TEMPLATE_BLOCKS.contains(blockId)) throw new UnsafeTemplateException(blockId);
				if (STRIPPED_TEMPLATE_BLOCKS.contains(blockId)) continue;
				materials.add(blockId);
				unique.put(info.pos(), info.state());
			}
		});
		if (unique.isEmpty() || unique.size() > CreationBlueprintParser.MAX_BLOCKS
				|| materials.size() > MAX_TEMPLATE_MATERIALS) {
			PowersMod.LOGGER.warn("Rejected creation template {} blocks={} materials={}",
					definition.template(), unique.size(), materials.size());
			return List.of();
		}
		List<Target> result = new ArrayList<>(unique.size());
		unique.forEach((pos, state) -> result.add(new Target(pos, state)));
		return List.copyOf(result);
	}

	private static boolean mayPlace(ServerPlayer player, ServerLevel level, BlockPos pos) {
		return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos)
				&& level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && level.getBlockState(pos).isAir()
				&& PowerProtection.mayAffectBlock(player, level, pos);
	}

	private static final class UnsafeTemplateException extends RuntimeException {
		private final String blockId;

		private UnsafeTemplateException(String blockId) {
			this.blockId = blockId;
		}
	}
}
