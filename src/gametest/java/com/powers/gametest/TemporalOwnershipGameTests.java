package com.powers.gametest;

import com.powers.entity.DarknessFireballProjectile;
import com.powers.player.PlayerPowers;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.realm.RealmEventManager;
import com.powers.realm.RealmKind;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellFieldManager;
import com.powers.time.TemporalClocks;
import com.powers.time.TemporalSubsystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/** Live dedicated-server acceptance for INT-008 temporal ownership boundaries. */
@SuppressWarnings("removal")
public final class TemporalOwnershipGameTests {
	private static final Map<MinecraftServer, FreezeProbe> FREEZE_PROBES = new IdentityHashMap<>();
	private static boolean tickHookRegistered;

	public TemporalOwnershipGameTests() {
		if (tickHookRegistered) return;
		tickHookRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			FreezeProbe probe = FREEZE_PROBES.get(server);
			if (probe == null || server.getTickCount() < probe.thawAtControlTick) return;
			probe.stationary = probe.projectile.position().distanceToSqr(probe.frozenAt) < 1.0E-8;
			probe.checked = true;
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
			FREEZE_PROBES.remove(server);
		});
	}
	@GameTest(environment = "powers:temporal_ownership_isolated", maxTicks = 20)
	public void administratorFreezeRejectsAcquisitionAndRemainsAuthoritative(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		try {
			server.tickRateManager().setFrozen(true);
			helper.assertTrue(!GlobalTimeStopManager.startCrystal(owner, 20),
					"POWERS stole an administrator-owned frozen clock");
			helper.assertTrue(server.tickRateManager().isFrozen(),
					"Rejected acquisition thawed the administrator clock");
			helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty(),
					"Rejected acquisition created a lease journal");
		} finally {
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
		}
		helper.succeed();
	}

	@GameTest(environment = "powers:temporal_ownership_isolated", maxTicks = 20)
	public void externalSameValueWriteSupersedesLeaseWithoutBeingUndone(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		try {
			helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
					"Crystal lease could not acquire a free clock");
			server.tickRateManager().setFrozen(true);
			GlobalTimeStopManager.stopCrystal(owner);
			helper.assertTrue(server.tickRateManager().isFrozen(),
					"POWERS undid an external same-value clock write");
			helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty(),
					"Superseded lease authority remained active");
		} finally {
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
		}
		helper.succeed();
	}

	@GameTest(environment = "powers:temporal_ownership_isolated", maxTicks = 20)
	public void crystalDeadlineUsesExactlyTwelveHundredControlTicks(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		long acquired = server.getTickCount();
		try {
			helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 1_200),
					"Crystal lease could not acquire a free clock");
			var snapshot = GlobalTimeStopManager.snapshot(server).orElseThrow();
			helper.assertTrue(snapshot.deadline() - acquired == 1_200L,
					"Crystal deadline drifted from the control clock: " + snapshot);
			helper.assertTrue(snapshot.remainingTicks() == 1_200L,
					"Fresh crystal lease did not expose the full control duration");
			helper.assertTrue(snapshot.clock().equals("CONTROL"),
					"Lease diagnostics mislabeled the authoritative clock");
		} finally {
			GlobalTimeStopManager.stopCrystal(owner);
			reset(server);
		}
		helper.succeed();
	}

	@GameTest(environment = "powers:temporal_ownership_isolated", maxTicks = 20)
	public void externalFreezeBlocksEverySelectedWorldManager(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		reset(server);
		int energy = PlayerPowers.get(player).energy();
		long worldTick = helper.getLevel().getGameTime();
		try {
			server.tickRateManager().setFrozen(true);
			helper.assertTrue(!TemporalClocks.worldAdvances(server,
					TemporalSubsystem.CHANNELS), "External freeze still advanced the world clock");
			SpellCastingManager.tick(server);
			SpellFieldManager.tick(server);
			CelestialRuinManager.tick(server);
			RealmEventManager.tickPlayer(player, helper.getLevel(), RealmKind.LIGHT);
			helper.assertTrue(helper.getLevel().getGameTime() == worldTick,
					"World time changed inside a synchronous frozen boundary");
			helper.assertTrue(PlayerPowers.get(player).energy() == energy,
					"A frozen world-owned manager mutated player energy");
		} finally {
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
		}
		helper.succeed();
	}

	@GameTest(environment = "powers:temporal_projectile_isolated", maxTicks = 30)
	public void projectilePausesAndResumesAcrossVanillaFreeze(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		Projectile projectile = new DarknessFireballProjectile(helper.getLevel(), owner,
				new Vec3(0.45, 0.0, 0.0));
		projectile.setPos(helper.absoluteVec(new Vec3(2.5, 3.0, 2.5)));
		helper.getLevel().addFreshEntity(projectile);
		Vec3 frozenAt = projectile.position();
		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Projectile fixture could not acquire the clock");
		// The same-value external write keeps the global freeze authoritative even
		// after the mock owner is intentionally absent from the real PlayerList.
		server.tickRateManager().setFrozen(true);
		FreezeProbe probe = new FreezeProbe(projectile, frozenAt, server.getTickCount() + 4L);
		FREEZE_PROBES.put(server, probe);
		helper.runAfterDelay(4, () -> {
			try {
				helper.assertTrue(probe.checked && probe.stationary,
						"Projectile moved while vanilla simulation was frozen");
				helper.assertTrue(projectile.position().distanceToSqr(frozenAt) > 0.01,
						"Projectile did not resume after vanilla thawed");
			} finally {
				projectile.discard();
				reset(server);
			}
			helper.succeed();
		});
	}

	@GameTest(environment = "powers:temporal_ownership_isolated", maxTicks = 20)
	public void ownerLifecycleCleanupReleasesOnlyItsLease(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Lifecycle fixture could not acquire the clock");
		GlobalTimeStopManager.clear(server, owner.getUUID());
		helper.assertTrue(!server.tickRateManager().isFrozen(),
				"Owner lifecycle cleanup left its clock frozen");
		helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty(),
				"Owner lifecycle cleanup retained lease authority");
		helper.succeed();
	}

	private static void reset(MinecraftServer server) {
		FREEZE_PROBES.remove(server);
		GlobalTimeStopManager.clearAll(server);
		if (server.tickRateManager().isFrozen()) server.tickRateManager().setFrozen(false);
	}

	private static final class FreezeProbe {
		private final Projectile projectile;
		private final Vec3 frozenAt;
		private final long thawAtControlTick;
		private boolean checked;
		private boolean stationary;

		private FreezeProbe(Projectile projectile, Vec3 frozenAt, long thawAtControlTick) {
			this.projectile = projectile;
			this.frozenAt = frozenAt;
			this.thawAtControlTick = thawAtControlTick;
		}
	}
}
