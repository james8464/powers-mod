package com.powers.gametest;

import com.powers.magic.runtime.MagicRuntime;
import com.powers.performance.ServerTickProfiler;
import com.powers.player.PlayerPowers;
import com.powers.power.AbilityActivationService;
import com.powers.power.Power;
import com.powers.power.PowerAbilityRuntime;
import com.powers.power.PowerRegistry;
import com.powers.testing.TestingOverrides;
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
	private static final List<String> PROFILE_ACTIONS = List.of(
			"lightning_strike", "energy_beam", "thunderclap");

	public ConnectedBotProfileGameTests() {
	}

	@GameTest(manualOnly = false, maxTicks = PROFILE_TICKS * 3 + PHASE_GAP * 3, padding = 64)
	@SuppressWarnings("removal")
	public void connectedTenFiftyAndHundredPlayerProfiles(GameTestHelper helper) {
		int profileTicks = requestedTicks();
		List<Integer> populations = List.of(10, 50, 100);
		List<ServerPlayer> bots = new ArrayList<>(100);
		int[] attempts = new int[populations.size()];
		int[] successes = new int[populations.size()];
		helper.onEachTick(() -> exerciseCombatWorkload(helper, populations, bots,
				attempts, successes));
		for (int phase = 0; phase < populations.size(); phase++) {
			int expected = populations.get(phase);
			int phaseIndex = phase;
			long startTick = (long) phase * (profileTicks + PHASE_GAP);
			helper.runAtTickTime(startTick, () -> {
				connectUntil(helper, bots, expected);
				helper.assertTrue(helper.getLevel().getServer().getPlayerCount() >= expected,
						"Could not establish " + expected + " embedded player connections");
				helper.assertTrue(ServerTickProfiler.start(helper.getLevel().getServer(), expected,
						profileTicks, profileLabel(expected, profileTicks), true),
						"A previous profiling phase was still active");
			});
			helper.runAtTickTime(startTick + profileTicks + 1L, () -> {
				helper.assertFalse(ServerTickProfiler.status(
						helper.getLevel().getServer()).active(),
						"The " + expected + "-player profile did not publish");
				helper.assertTrue(attempts[phaseIndex] > 0 && successes[phaseIndex] > 0,
						"The " + expected + "-player phase did not execute live magic");
			});
		}
		helper.runAtTickTime((long) populations.size() * (profileTicks + PHASE_GAP) - 1L, () -> {
			for (ServerPlayer bot : bots) {
				for (String action : PROFILE_ACTIONS) {
					PowerAbilityRuntime.rollbackFailedActivation(bot, action);
				}
				MagicRuntime.global().clearOwner(bot.getUUID());
				TestingOverrides.clear(bot.getUUID());
			}
			helper.succeed();
		});
	}

	private static void exerciseCombatWorkload(GameTestHelper helper, List<Integer> populations,
			List<ServerPlayer> bots, int[] attempts, int[] successes) {
		var server = helper.getLevel().getServer();
		ServerTickProfiler.Status status = ServerTickProfiler.status(server);
		if (!status.active() || (server.getTickCount() & 1) != 0) return;
		int phase = populations.indexOf(status.expectedPlayers());
		if (phase < 0 || bots.size() < status.expectedPlayers()) return;
		int attempt = attempts[phase]++;
		int botIndex = Math.floorMod(attempt, status.expectedPlayers());
		ServerPlayer bot = bots.get(botIndex);
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		bot.snapTo(origin.add(botIndex % 10, 0.0, botIndex / 10), 0.0F, 0.0F);
		bot.setDeltaMovement(Vec3.ZERO);
		String action = PROFILE_ACTIONS.get(Math.floorMod(attempt, PROFILE_ACTIONS.size()));
		PowerAbilityRuntime.rollbackFailedActivation(bot, action);
		MagicRuntime.global().clearOwner(bot.getUUID());
		PlayerPowers.get(bot).forceRestoreEnergy();
		Power power = PowerRegistry.get(action);
		boolean activated = power != null && AbilityActivationService.activate(
				bot, power.ability(), power.id().toString())
				== AbilityActivationService.Result.ACTIVATED;
		if (activated) successes[phase]++;
		ServerTickProfiler.recordAction(server, activated);
	}

	@SuppressWarnings("removal")
	private static void connectUntil(GameTestHelper helper, List<ServerPlayer> bots, int target) {
		Vec3 origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
		while (bots.size() < target) {
			ServerPlayer bot = helper.makeMockServerPlayerInLevel();
			int index = bots.size();
			bot.snapTo(origin.add(index % 10, 0.0, index / 10), 0.0F, 0.0F);
			bot.setInvulnerable(true);
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(bot);
			data.setSkillLevel(bot, 10);
			data.setSlots(bot, PROFILE_ACTIONS.stream()
					.map(id -> "powers:" + id).toList());
			TestingOverrides.setAll(bot.getUUID(), true);
			bots.add(bot);
		}
	}

	private static int requestedTicks() {
		return Math.clamp(Integer.getInteger("powers.profile.ticks", PROFILE_TICKS),
				200, PROFILE_TICKS);
	}

	private static String profileLabel(int players, int ticks) {
		return ticks == PROFILE_TICKS ? players + "p-30m"
				: players + "p-smoke-" + ticks + "t";
	}
}
