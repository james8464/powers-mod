package com.powers.gametest;

import com.powers.spell.CelestialRuinCancellation;
import com.powers.spell.CelestialRuinManager;
import com.powers.testing.RestartSoakScenario;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Destructive global-clock lifecycle test registered only in its isolated Gradle lane. */
public final class RestartSoakGameTests {
	public RestartSoakGameTests() {
	}

	@GameTest(maxTicks = 180, padding = 192)
	@SuppressWarnings("removal") // Minecraft 26.2 exposes no non-deprecated in-level player fixture.
	public void seedsSettlesAndPersistsEveryRuntimeOwner(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(4, 20, 4));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		helper.getLevel().setBlock(origin.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		var seeded = RestartSoakScenario.seed(player, 1);
		helper.assertTrue(seeded.passed(), "Restart soak seed failed: " + seeded.detail());
		helper.runAfterDelay(120, () -> {
			var settled = RestartSoakScenario.status(player, 1);
			helper.assertTrue(settled.passed(), "Restart soak did not settle: " + settled.detail());
			var rollover = RestartSoakScenario.rollover(player, 1);
			helper.assertTrue(rollover.passed(), "Restart soak rollover failed: " + rollover.detail());
			var cancelled = CelestialRuinManager.cancelNearest(helper.getLevel(), player.position());
			helper.assertTrue(cancelled == CelestialRuinCancellation.CANCELLED,
					"Restart soak rollover could not be cancelled after persistence proof");
			RestartSoakScenario.clear(helper.getLevel().getServer());
			helper.succeed();
		});
	}
}
