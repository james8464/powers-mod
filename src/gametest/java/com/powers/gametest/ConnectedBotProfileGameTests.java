package com.powers.gametest;

import com.powers.performance.ServerTickProfiler;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Manual, wall-clock performance acceptance with real embedded connections and JFR output. */
public final class ConnectedBotProfileGameTests {
	private static final int PROFILE_TICKS = 36_000;
	private static final int PHASE_GAP = 10;

	public ConnectedBotProfileGameTests() {
	}

	@GameTest(manualOnly = true, maxTicks = PROFILE_TICKS * 3 + PHASE_GAP * 3, padding = 64)
	@SuppressWarnings("removal")
	public void connectedTenFiftyAndHundredPlayerProfiles(GameTestHelper helper) {
		List<Integer> populations = List.of(10, 50, 100);
		List<ServerPlayer> bots = new ArrayList<>(100);
		for (int phase = 0; phase < populations.size(); phase++) {
			int expected = populations.get(phase);
			long startTick = (long) phase * (PROFILE_TICKS + PHASE_GAP);
			helper.runAtTickTime(startTick, () -> {
				connectUntil(helper, bots, expected);
				helper.assertTrue(helper.getLevel().getServer().getPlayerCount() >= expected,
						"Could not establish " + expected + " embedded player connections");
				helper.assertTrue(ServerTickProfiler.start(helper.getLevel().getServer(), expected,
						PROFILE_TICKS, expected + "p-30m"),
						"A previous profiling phase was still active");
			});
			helper.runAtTickTime(startTick + PROFILE_TICKS + 1L, () ->
					helper.assertFalse(ServerTickProfiler.status(
							helper.getLevel().getServer()).active(),
							"The " + expected + "-player profile did not publish"));
		}
		helper.runAtTickTime((long) populations.size() * (PROFILE_TICKS + PHASE_GAP) - 1L,
				helper::succeed);
	}

	@SuppressWarnings("removal")
	private static void connectUntil(GameTestHelper helper, List<ServerPlayer> bots, int target) {
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		while (bots.size() < target) {
			ServerPlayer bot = helper.makeMockServerPlayerInLevel();
			int index = bots.size();
			bot.snapTo(origin.add(index % 10, 0.0, index / 10), 0.0F, 0.0F);
			bots.add(bot);
		}
	}
}
