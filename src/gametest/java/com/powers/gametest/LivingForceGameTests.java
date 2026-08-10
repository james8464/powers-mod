package com.powers.gametest;

import com.powers.PowersBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
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
}
