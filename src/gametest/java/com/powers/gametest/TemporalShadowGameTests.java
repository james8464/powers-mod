package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersWeapons;
import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.ShadowCompanionRules;
import com.powers.companion.combat.ShadowPowerCatalogue;
import com.powers.companion.combat.ShadowPowerExecutor;
import com.powers.companion.combat.ShadowPowerRuntime;
import com.powers.player.SkillSystem;
import com.powers.power.state.GlobalTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.IdentityHashMap;
import java.util.Map;

/** Real executor and companion-control regressions for indefinite Shadow leases. */
@SuppressWarnings("removal")
public final class TemporalShadowGameTests {
	private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();
	private static boolean registered;

	public TemporalShadowGameTests() {
		if (registered) return;
		registered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Probe probe = PROBES.get(server);
			if (probe == null) return;
			probe.activeThroughout &= GlobalTimeStopManager.snapshot(server)
					.map(lease -> lease.leaseToken() == probe.token).orElse(false)
					&& ShadowPowerRuntime.active(probe.owner.getUUID(), "time_freeze")
					&& probe.helper.getLevel().getGameTime() == probe.worldStarted;
			if (server.getTickCount() - probe.controlStarted < 1_300) return;
			PROBES.remove(server);
			GlobalTimeStopManager.stopShadow(probe.owner);
			server.tickRateManager().tick();
			probe.helper.runAfterDelay(1, () -> {
				try {
					probe.helper.assertTrue(probe.activeThroughout,
							"Executor-started Shadow freeze expired at the finite toggle deadline");
					probe.helper.assertTrue(!ShadowPowerRuntime.active(probe.owner.getUUID(), "time_freeze"),
							"Explicit Shadow stop retained its toggle marker");
					probe.helper.assertTrue(!ShadowPowerRuntime.active(probe.owner.getUUID(), "flight"),
							"Finite Shadow toggles no longer expire");
					probe.helper.succeed();
				} finally {
					cleanup(probe.owner);
				}
			});
		});
	}

	@GameTest(environment = "powers:temporal_shadow_lifetime_isolated", maxTicks = 2_000)
	public void executorShadowFreezeHasNoFiniteToggleDeadline(GameTestHelper helper) {
		ServerPlayer owner = manifest(helper);
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		start(helper, owner, shadow);
		MinecraftServer server = helper.getLevel().getServer();
		helper.assertTrue(ShadowPowerExecutor.execute(helper.getLevel(), shadow, null,
				ShadowPowerCatalogue.find("flight"),
				new ShadowPowerExecutor.ExecutionContext(owner, false, server.getTickCount())).success(),
				"Finite-toggle fixture failed");
		PROBES.put(server, new Probe(helper, owner, server.getTickCount(),
				helper.getLevel().getGameTime(), GlobalTimeStopManager.snapshot(server).orElseThrow().leaseToken()));
	}

	@GameTest(environment = "powers:temporal_shadow_cleanup_isolated", maxTicks = 40)
	public void executorShadowLeaseExitsRetireOnlyTheMatchingToggle(GameTestHelper helper) {
		ServerPlayer owner = manifest(helper);
		checkExit(helper, owner, 0);
	}

	private static void checkExit(GameTestHelper helper, ServerPlayer owner, int scenario) {
		MinecraftServer server = helper.getLevel().getServer();
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		try {
			start(helper, owner, shadow);
			long token = GlobalTimeStopManager.snapshot(server).orElseThrow().leaseToken();
			ShadowPowerRuntime.activate(owner.getUUID(), shadow.getUUID(), "flight", Long.MAX_VALUE);
			int togglesBeforeExit = ShadowPowerRuntime.diagnostics().toggles();
			// A stale body's stop callback must not release a newer body's owned clock.
			ShadowCompanionEntity stale = new ShadowCompanionEntity(PowersEntities.SHADOW_COMPANION, helper.getLevel());
			ShadowPowerRuntime.stop(owner, stale, "time_freeze");
			helper.assertTrue(GlobalTimeStopManager.snapshot(server)
					.map(lease -> lease.leaseToken() == token).orElse(false)
					&& ShadowPowerRuntime.active(owner.getUUID(), "time_freeze"),
					"Stale Shadow body cleanup removed a newer lease or marker");
			if (scenario == 0) GlobalTimeStopManager.stopShadow(owner);
			else if (scenario <= 2) server.tickRateManager().setFrozen(scenario == 1);
			else {
				shadow.discard();
				GlobalTimeStopManager.tick(server);
			}
			helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty(), "Shadow exit retained lease authority");
			helper.assertTrue(!ShadowPowerRuntime.active(owner.getUUID(), "time_freeze"),
					"Shadow lease exit retained its toggle marker: scenario " + scenario);
			helper.assertTrue(ShadowPowerRuntime.active(owner.getUUID(), "flight"), "Shadow lease exit cleared another toggle");
			helper.assertTrue(ShadowPowerRuntime.diagnostics().toggles() == togglesBeforeExit - 1,
					"Shadow diagnostic toggle count retained a retired lease");
			helper.assertTrue(server.tickRateManager().isFrozen() == (scenario == 1),
					"Shadow cleanup overwrote an external clock value");
			server.tickRateManager().setFrozen(false);
			ShadowPowerRuntime.stop(owner, shadow, "flight");
			if (scenario < 3) helper.runAfterDelay(1, () -> checkExit(helper, owner, scenario + 1));
			else {
				cleanup(owner);
				helper.succeed();
			}
		} catch (RuntimeException failure) {
			cleanup(owner);
			throw failure;
		}
	}

	private static ServerPlayer manifest(GameTestHelper helper) {
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
		owner.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself"), "Shadow manifestation failed");
		PrivateCompanionManager.tickPlayer(owner, helper.getLevel().getServer().getTickCount());
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		shadow.setEnergy(ShadowCompanionRules.MAX_ENERGY);
		return owner;
	}

	private static void start(GameTestHelper helper, ServerPlayer owner, ShadowCompanionEntity shadow) {
		var result = ShadowPowerExecutor.execute(helper.getLevel(), shadow, null,
				ShadowPowerCatalogue.find("time_freeze"),
				new ShadowPowerExecutor.ExecutionContext(owner, false, helper.getLevel().getServer().getTickCount()));
		helper.assertTrue(result.success(), "Shadow executor setup failed: " + result.reason());
		helper.assertTrue(ShadowPowerRuntime.active(owner.getUUID(), "time_freeze"), "Executor did not record its marker");
	}

	private static void cleanup(ServerPlayer owner) {
		MinecraftServer server = owner.level().getServer();
		GlobalTimeStopManager.clearAll(server);
		if (server.tickRateManager().isFrozen()) server.tickRateManager().setFrozen(false);
		PrivateCompanionManager.forget(owner);
	}

	private static final class Probe {
		private final GameTestHelper helper;
		private final ServerPlayer owner;
		private final long controlStarted;
		private final long worldStarted;
		private final long token;
		private boolean activeThroughout = true;

		private Probe(GameTestHelper helper, ServerPlayer owner, long controlStarted, long worldStarted, long token) {
			this.helper = helper;
			this.owner = owner;
			this.controlStarted = controlStarted;
			this.worldStarted = worldStarted;
			this.token = token;
		}
	}
}
