package com.powers.gametest;

import com.powers.PowersBlocks;
import com.powers.AmethystWardBlock;
import com.powers.entity.DarknessCreature;
import com.powers.force.ForceContainmentManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Live-world spreading coverage kept separate for unambiguous test discovery. */
public final class LivingForceGameTests {
	public LivingForceGameTests() {
	}

	@GameTest(maxTicks = 40)
	public void darknessRandomTickSpreadsIntoOrdinaryBlocks(GameTestHelper helper) {
		BlockPos center = new BlockPos(1, 1, 1);
		helper.setBlock(center, PowersBlocks.DARKNESS);
		for (var direction : net.minecraft.core.Direction.values()) {
			helper.setBlock(center.relative(direction), Blocks.STONE);
		}
		for (int tick = 1; tick <= 12; tick++) {
			helper.runAtTickTime(tick, () -> helper.randomTick(center));
		}
		helper.runAtTickTime(20, () -> {
			long spread = java.util.Arrays.stream(net.minecraft.core.Direction.values())
					.filter(direction -> helper.getBlockState(center.relative(direction)).is(PowersBlocks.DARKNESS))
					.count();
			helper.assertTrue(spread > 0, "Darkness did not spread during repeated random ticks");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void pureLightRandomTickSpreadsIntoOrdinaryBlocks(GameTestHelper helper) {
		BlockPos center = new BlockPos(1, 1, 1);
		helper.setBlock(center, PowersBlocks.PURE_LIGHT);
		for (var direction : net.minecraft.core.Direction.values()) {
			helper.setBlock(center.relative(direction), Blocks.STONE);
		}
		for (int tick = 1; tick <= 12; tick++) {
			helper.runAtTickTime(tick, () -> helper.randomTick(center));
		}
		helper.runAtTickTime(20, () -> {
			long spread = java.util.Arrays.stream(net.minecraft.core.Direction.values())
					.filter(direction -> helper.getBlockState(center.relative(direction))
							.is(PowersBlocks.PURE_LIGHT))
					.count();
			helper.assertTrue(spread > 0, "Pure Light did not spread during repeated random ticks");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void poweredAmethystCeremonyCrystallisesLivingForce(GameTestHelper helper) {
		BlockPos ward = new BlockPos(8, 2, 8);
		helper.setBlock(ward.below(), Blocks.REDSTONE_BLOCK);
		helper.setBlock(ward, PowersBlocks.AMETHYST_WARD);
		helper.setBlock(ward.offset(2, 0, 0), Blocks.AMETHYST_BLOCK);
		helper.setBlock(ward.offset(-2, 0, 0), Blocks.AMETHYST_BLOCK);
		helper.setBlock(ward.offset(0, 0, 2), Blocks.AMETHYST_BLOCK);
		helper.setBlock(ward.offset(0, 0, -2), Blocks.AMETHYST_BLOCK);
		BlockPos infection = ward.offset(3, 0, 0);
		helper.setBlock(infection, PowersBlocks.DARKNESS);
		BlockPos absoluteWard = helper.absolutePos(ward);
		helper.assertTrue(AmethystWardBlock.isPowered(helper.getBlockState(ward)),
				"Test ceremony ward lost its redstone state");
		helper.assertTrue(ForceContainmentManager.isCeremony(helper.getLevel(), absoluteWard),
				"Four cardinal crystals did not complete the ceremony");
		ForceContainmentManager.request(helper.getLevel(), absoluteWard);
		helper.runAfterDelay(8, () -> {
			helper.assertBlockPresent(Blocks.AMETHYST_BLOCK, infection);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 260)
	@SuppressWarnings("removal")
	public void opposedLivingForceOpensABoundedFactionInvasion(GameTestHelper helper) {
		var player = helper.makeMockServerPlayerInLevel();
		BlockPos local = new BlockPos(8, 2, 8);
		BlockPos absolute = helper.absolutePos(local);
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		helper.setBlock(local.offset(1, 0, 0), PowersBlocks.DARKNESS);
		helper.runAfterDelay(220, () -> {
			var invaders = helper.getLevel().getEntitiesOfClass(DarknessCreature.class,
					player.getBoundingBox().inflate(32.0), entity -> entity.temporaryGuardian());
			helper.assertTrue(!invaders.isEmpty(), "Nearby Darkness never manifested a Hollowed patrol");
			helper.assertTrue(invaders.size() <= 3, "Faction invasion exceeded its local cap");
			helper.succeed();
		});
	}
}
