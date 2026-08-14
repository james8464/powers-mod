package com.powers.force;

import com.powers.AmethystWardBlock;
import com.powers.PowersBlocks;
import com.powers.PowersMod;
import com.powers.protection.PowerProtectionAdapters;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Opt-in live benchmark for catastrophic containment fairness across dimensions. */
public final class CatastrophicBlockWorkGameTests {
	private static final int CATASTROPHIC_CEREMONIES = 30;
	private static final int STARVATION_LIMIT_TICKS = 100;

	@GameTest(maxTicks = 130)
	public void catastrophicContainmentCannotStarveAnotherDimension(GameTestHelper helper) {
		ForceContainmentManager.clear();
		ServerLevel overworld = helper.getLevel();
		ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
		helper.assertTrue(nether != null, "The benchmark requires the vanilla Nether dimension");

		for (int index = 0; index < CATASTROPHIC_CEREMONIES; index++) {
			BlockPos ward = new BlockPos(20_000 + index * 16, 80, 20_000);
			prepareCeremony(overworld, ward);
			ForceContainmentManager.request(overworld, ward);
		}
		BlockPos ordinaryWard = new BlockPos(20_000, 80, 20_000);
		prepareCeremony(nether, ordinaryWard);
		BlockPos ordinaryTarget = ordinaryWard.above();
		ForceContainmentManager.request(nether, ordinaryWard);
		long startedAt = overworld.getGameTime();
		int[] phase = {0};
		long[] phaseStartedAt = {0L};
		BlockPos protectedWard = new BlockPos(21_000, 80, 21_000);
		BlockPos protectedTarget = protectedWard.above();

		helper.onEachTick(() -> {
			long elapsed = overworld.getGameTime() - startedAt;
			if (phase[0] == 0 && nether.getBlockState(ordinaryTarget).is(Blocks.AMETHYST_BLOCK)) {
				helper.assertTrue(elapsed <= STARVATION_LIMIT_TICKS,
						"A loaded dimension waited " + elapsed
								+ " ticks for ordinary containment work");
				ForceContainmentManager.clear();
				prepareCeremony(nether, protectedWard);
				helper.assertTrue(PowerProtectionAdapters.register(
						"perf016_claim_fixture", 1_000,
						query -> query.position() == null || !query.position().equals(protectedTarget)),
						"The benchmark claim fixture did not register exactly once");
				ForceContainmentManager.request(nether, protectedWard);
				phase[0] = 1;
				phaseStartedAt[0] = overworld.getGameTime();
				PowersMod.LOGGER.info("PERF-016 catastrophicCeremonies={} starvationTicks={}",
						CATASTROPHIC_CEREMONIES, elapsed);
				return;
			}
			if (phase[0] == 1 && overworld.getGameTime() - phaseStartedAt[0] >= 8) {
				helper.assertTrue(nether.getBlockState(protectedTarget).is(PowersBlocks.DARKNESS),
						"Containment bypassed the active claim provider");
				PowersMod.LOGGER.info("PERF-016 claimProviderDenied=true");
				ForceContainmentManager.clear();
				helper.succeed();
				return;
			}
			if (phase[0] == 0 && elapsed > STARVATION_LIMIT_TICKS) {
				ForceContainmentManager.clear();
				helper.assertTrue(false, "Catastrophic work starved another dimension for over "
						+ STARVATION_LIMIT_TICKS + " ticks");
			}
		});
	}

	private static void prepareCeremony(ServerLevel level, BlockPos ward) {
		level.getChunkAt(ward);
		level.setBlock(ward.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(ward, PowersBlocks.AMETHYST_WARD.defaultBlockState(), Block.UPDATE_ALL);
		for (ForceContainmentRules.Offset offset : ForceContainmentRules.cardinalCrystals()) {
			level.setBlock(ward.offset(offset.x(), offset.y(), offset.z()),
					Blocks.AMETHYST_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.setBlock(ward.above(), PowersBlocks.DARKNESS.defaultBlockState(), Block.UPDATE_ALL);
		if (!AmethystWardBlock.isPowered(level.getBlockState(ward))) {
			throw new IllegalStateException("Benchmark ward did not retain its powered state");
		}
	}
}
