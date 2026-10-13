package com.powers.gametest;

import com.powers.power.crystals.CreationBlueprintReloadListener;
import com.powers.power.crystals.CreationBlueprintService;
import com.powers.player.CreationBlueprintSelectionStore;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.DyeColor;

/** Live-world proof that creation blueprints reject partial placement. */
public final class CreationBlueprintGameTests {
	@GameTest(maxTicks = 20, padding = 64)
	@SuppressWarnings("removal")
	public void creationBlueprintPreflightIsAtomic(GameTestHelper helper) {
		var registry = CreationBlueprintReloadListener.registry();
		var blueprint = registry.findOrFirst("powers:orange_chamber");
		helper.assertTrue(blueprint != null, "Datapack creation blueprint was not published");
		var player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(CreationBlueprintSelectionStore.selected(player, registry.ids())
				.equals("powers:orange_chamber"), "Legacy/default selection did not reconcile to the chamber");
		String advanced = CreationBlueprintSelectionStore.advance(player, registry.ids());
		helper.assertTrue(CreationBlueprintSelectionStore.selected(player, registry.ids()).equals(advanced),
				"Stable blueprint key was not retained on the player attachment");
		BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
		BlockPos blocked = origin.offset(-2, 0, -2);
		BlockPos untouched = origin.offset(2, 0, 2);
		helper.getLevel().setBlock(blocked, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);

		helper.assertFalse(CreationBlueprintService.place(player, helper.getLevel(), origin, blueprint),
				"A blocked target did not reject the complete blueprint");
		helper.assertTrue(helper.getLevel().getBlockState(untouched).isAir(),
				"Atomic preflight allowed a partial write");

		helper.getLevel().setBlock(blocked, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(CreationBlueprintService.place(player, helper.getLevel(), origin, blueprint),
				"A clear validated blueprint was not placed");
		helper.assertTrue(helper.getLevel().getBlockState(blocked).is(Blocks.CONCRETE.pick(DyeColor.ORANGE)),
				"Validated frame material was not placed");
		helper.assertTrue(helper.getLevel().getBlockState(origin.above(2)).is(Blocks.GLOWSTONE),
				"Validated light material was not placed");

		int entitiesBefore = entityCount(helper);
		var plains = registry.findOrFirst("powers:plains_shelter");
		var desert = registry.findOrFirst("powers:desert_shelter");
		helper.assertTrue(plains != null && desert != null, "Approved vanilla templates were not published");
		helper.assertTrue(CreationBlueprintService.place(player, helper.getLevel(),
				helper.absolutePos(new BlockPos(16, 3, 4)), plains),
				"Approved plains template failed its live bounded placement path");
		helper.assertTrue(CreationBlueprintService.place(player, helper.getLevel(),
				helper.absolutePos(new BlockPos(36, 3, 4)), desert),
				"Approved desert template failed its live bounded placement path");
		assertLootMetadataStripped(helper, new BlockPos(16, 3, 4));
		assertLootMetadataStripped(helper, new BlockPos(36, 3, 4));
		int entitiesAfter = entityCount(helper);
		helper.assertTrue(entitiesAfter == entitiesBefore,
				"Vanilla template placement copied source entities");
		helper.succeed();
	}

	private static int entityCount(GameTestHelper helper) {
		int count = 0;
		for (var ignored : helper.getLevel().getAllEntities()) count++;
		return count;
	}

	private static void assertLootMetadataStripped(GameTestHelper helper, BlockPos relativeOrigin) {
		BlockPos origin = helper.absolutePos(relativeOrigin);
		for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
			var blockEntity = helper.getLevel().getBlockEntity(origin.offset(x, y, z));
			if (blockEntity == null) continue;
			var tag = blockEntity.saveWithoutMetadata(helper.getLevel().registryAccess());
			helper.assertFalse(tag.keySet().stream()
					.anyMatch(key -> key.toLowerCase(java.util.Locale.ROOT).contains("loot")),
					"Vanilla template copied loot metadata at " + blockEntity.getBlockPos());
		}
	}
}
