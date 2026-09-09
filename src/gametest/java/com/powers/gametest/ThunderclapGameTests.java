package com.powers.gametest;

import com.powers.power.abilities.CombatTerrainImpact;
import com.powers.power.abilities.CombatTerrainRules;
import com.powers.power.abilities.ThunderclapEnvironment;
import com.powers.power.abilities.ThunderclapRules;
import com.powers.protection.PowerProtectionAdapters;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Live proof for rank-wide fragile terrain work and enclosure-gated directional echo. */
public final class ThunderclapGameTests {
	private static final String CLAIM_FIXTURE = "pwr011_claim_fixture";

	@GameTest(maxTicks = 40)
	@SuppressWarnings("removal")
	public void terrainAndCaveEchoStayBoundedAtEveryRank(GameTestHelper helper) {
		var level = helper.getLevel();
		var caster = helper.makeMockServerPlayerInLevel();
		BlockPos floorOrigin = helper.absolutePos(new BlockPos(8, 2, 1));
		Vec3 release = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 5, 1)));
		Vec3 forward = new Vec3(0.0, 0.0, 1.0);

		BlockPos protectedGlass = floorOrigin.offset(-1, 0, 3);
		BlockPos forbiddenBedrock = floorOrigin.offset(0, 0, 4);
		prepareGlassFloor(helper, floorOrigin);
		level.setBlock(forbiddenBedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
		helper.assertTrue(PowerProtectionAdapters.register(CLAIM_FIXTURE, 2_000,
				query -> query.position() == null || !query.position().equals(protectedGlass)),
				"The Thunderclap claim fixture did not register");
		try {
			for (int rank = 0; rank <= 10; rank++) {
				int removed = CombatTerrainImpact.thunderclap(
						level, caster, release, forward, 10.0, rank);
				helper.assertTrue(removed > 0,
						"Thunderclap rank " + rank + " did not break tagged fragile terrain");
				helper.assertTrue(removed <= CombatTerrainRules.thunderclapBudget(rank),
						"Thunderclap rank " + rank + " exceeded its hard terrain budget");
				helper.assertTrue(level.getBlockState(protectedGlass).is(Blocks.GLASS),
						"Thunderclap bypassed the active claim provider");
				helper.assertTrue(level.getBlockState(forbiddenBedrock).is(Blocks.BEDROCK),
						"Thunderclap broke a forbidden block");
				prepareGlassFloor(helper, floorOrigin);
				level.setBlock(forbiddenBedrock, Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
			}

			BlockPos front = helper.absolutePos(new BlockPos(8, 5, 7));
			BlockPos left = helper.absolutePos(new BlockPos(3, 5, 1));
			BlockPos ceiling = helper.absolutePos(new BlockPos(8, 9, 1));
			for (int dz = 2; dz <= 14; dz++) {
				level.setBlock(helper.absolutePos(new BlockPos(8, 5, dz)),
						Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			level.setBlock(front, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(left, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(ceiling, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			ThunderclapEnvironment.EchoProbe cave = ThunderclapEnvironment.probe(
					level, release, forward);
			helper.assertTrue(cave.echo() == ThunderclapRules.Echo.CAVE,
					"A forward cave reflection did not produce its directional echo");
			level.setBlock(front, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertTrue(ThunderclapEnvironment.probe(level, release, forward).echo()
					== ThunderclapRules.Echo.NONE,
					"An open forward path produced a false cave echo");
			helper.succeed();
		} finally {
			PowerProtectionAdapters.unregister(CLAIM_FIXTURE);
		}
	}

	private static void prepareGlassFloor(GameTestHelper helper, BlockPos origin) {
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = 2; dz <= 11; dz++) {
				helper.getLevel().setBlock(origin.offset(dx, 0, dz),
						Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}
}
