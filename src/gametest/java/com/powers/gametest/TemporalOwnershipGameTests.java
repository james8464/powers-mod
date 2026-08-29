package com.powers.gametest;

import com.powers.ImportedPackItems;
import com.powers.PowerStatusEffects;
import com.powers.PowersEffects;
import com.powers.PowersWeapons;
import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.entity.DarknessFireballProjectile;
import com.powers.network.PacketRateLimiter;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.realm.RealmEventManager;
import com.powers.realm.RealmHeraldManager;
import com.powers.realm.RealmKind;
import com.powers.spell.CelestialRuinManager;
import com.powers.spell.CelestialRuinSavedData;
import com.powers.spell.SpellCastingManager;
import com.powers.spell.SpellFieldKind;
import com.powers.spell.SpellFieldManager;
import com.powers.time.TemporalClocks;
import com.powers.time.TemporalSubsystem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Live dedicated-server acceptance for INT-008 temporal ownership boundaries. */
@SuppressWarnings("removal")
public final class TemporalOwnershipGameTests {
	private static final String IMPLEMENTATION_SHA_PROPERTY = "powers.int008.implementationSha";
	private static final String DIAGNOSTIC_SHA = "0".repeat(40);
	private static final Map<MinecraftServer, FreezeProbe> FREEZE_PROBES = new IdentityHashMap<>();
	private static final Map<MinecraftServer, DeadlineProbe> DEADLINE_PROBES = new IdentityHashMap<>();
	private static boolean tickHookRegistered;

	public TemporalOwnershipGameTests() {
		if (tickHookRegistered) return;
		tickHookRegistered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			DeadlineProbe deadline = DEADLINE_PROBES.get(server);
			if (deadline != null) deadline.tick(server);
			FreezeProbe probe = FREEZE_PROBES.get(server);
			if (probe == null || server.getTickCount() < probe.thawAtControlTick) return;
			probe.frozenDistanceSquared = probe.projectile.position().distanceToSqr(probe.frozenAt);
			probe.stationary = probe.frozenDistanceSquared < 1.0E-8;
			probe.checked = true;
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
			FREEZE_PROBES.remove(server);
		});
	}
	@GameTest(environment = "powers:temporal_admin_isolated", maxTicks = 20)
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
			emit("admin-preservation", 0, 0,
					"{\"acquired\":false,\"leaseActive\":false,\"vanillaFrozen\":true}");
		} finally {
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
		}
		helper.succeed();
	}

	@GameTest(environment = "powers:temporal_supersession_isolated", maxTicks = 20)
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
			emit("external-supersession", 0, 0,
					"{\"leaseActive\":false,\"superseded\":true,\"vanillaFrozen\":true}");
		} finally {
			server.tickRateManager().setFrozen(false);
			GlobalTimeStopManager.clearAll(server);
		}
		helper.succeed();
	}

	@GameTest(environment = "powers:temporal_deadline_isolated", maxTicks = 1_230)
	public void crystalDeadlineUsesExactlyTwelveHundredControlTicks(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		long acquired = server.getTickCount();
		long worldTick = helper.getLevel().getGameTime();
		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 1_200),
				"Crystal lease could not acquire a free clock");
		var snapshot = GlobalTimeStopManager.snapshot(server).orElseThrow();
		helper.assertTrue(snapshot.deadline() - acquired == 1_200L,
				"Crystal deadline drifted from the control clock: " + snapshot);
		helper.assertTrue(snapshot.remainingTicks() == 1_200L,
				"Fresh crystal lease did not expose the full control duration");
		helper.assertTrue(snapshot.clock().equals("CONTROL"),
				"Lease diagnostics mislabeled the authoritative clock");
		helper.assertTrue(server.getPlayerList().getPlayer(owner.getUUID()) == owner,
				"Deadline fixture is not present through the real online-player boundary");
		DeadlineProbe probe = new DeadlineProbe(helper, acquired, worldTick);
		DEADLINE_PROBES.put(server, probe);
		// The exact boundary is observed from the server control clock while vanilla
		// simulation is parked. Complete from the resumed GameTest ticker so its
		// batch tracker can advance to the next isolated environment normally.
		helper.succeedWhen(() -> {
			helper.assertTrue(probe.releasedAt1200,
					"Crystal lease never reached its exact control-clock deadline");
			helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty()
					&& !server.tickRateManager().isFrozen(),
					"Released crystal lease reappeared after simulation resumed");
		});
	}

	@GameTest(environment = "powers:temporal_world_managers_isolated", maxTicks = 20)
	public void externalAndOwnedFreezeParkSeededWorldManagers(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		reset(server);
		long originalWorldTick = helper.getLevel().getGameTime();
		try {
			WorldFixture external = seedWorldFixture(helper, player);
			server.tickRateManager().setFrozen(true);
			assertFixturePaused(helper, player, external, "external");
			server.tickRateManager().setFrozen(false);
			clearWorldFixture(server, player, external);

			WorldFixture owned = seedWorldFixture(helper, player);
			helper.assertTrue(GlobalTimeStopManager.startCrystal(player, 20),
					"Owned-freeze fixture could not acquire the clock");
			assertFixturePaused(helper, player, owned, "owned");
			GlobalTimeStopManager.stopCrystal(player);
			helper.assertTrue(RealmHeraldManager.tick(helper.getLevel(), RealmKind.LIGHT)
					!= RealmHeraldManager.TickResult.PARKED,
					"Herald cadence remained parked after the owned freeze released");
			emit("world-managers-paused", 0, 0,
					"{\"celestialPaused\":true,\"channelsPaused\":true,\"energyMutated\":false,"
							+ "\"externalFreeze\":true,\"fieldsPaused\":true,"
							+ "\"heraldCadencePaused\":true,\"ownedFreeze\":true,"
							+ "\"realmPaused\":true,\"worldAdvanced\":false}");
		} finally {
			clearWorldFixture(server, player, null);
			((ServerLevelData) helper.getLevel().getLevelData()).setGameTime(originalWorldTick);
			reset(server);
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
		long startedAtControlTick = server.getTickCount();
		long startedAtWorldTick = helper.getLevel().getGameTime();
		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Projectile fixture could not acquire the clock");
		// The same-value external write keeps the global freeze authoritative even
		// after the mock owner is intentionally absent from the real PlayerList.
		server.tickRateManager().setFrozen(true);
		FreezeProbe probe = new FreezeProbe(projectile, frozenAt, startedAtControlTick,
				startedAtWorldTick, startedAtControlTick + 4L);
		FREEZE_PROBES.put(server, probe);
		helper.runAfterDelay(4, () -> {
			try {
				helper.assertTrue(probe.checked && probe.stationary,
						"Projectile moved while vanilla simulation was frozen");
				double resumedDistanceSquared = projectile.position().distanceToSqr(frozenAt);
				helper.assertTrue(resumedDistanceSquared > 0.01,
						"Projectile did not resume after vanilla thawed");
				emit("projectile-pause-resume",
						Math.max(0L, server.getTickCount() - probe.startedAtControlTick),
						Math.max(0L, helper.getLevel().getGameTime() - probe.startedAtWorldTick),
						"{\"frozenDistanceSquared\":" + probe.frozenDistanceSquared
								+ ",\"resumedDistanceSquared\":" + resumedDistanceSquared + "}");
			} finally {
				projectile.discard();
				reset(server);
			}
			helper.succeed();
		});
	}

	@GameTest(environment = "powers:temporal_lifecycle_isolated", maxTicks = 20)
	public void ownerLifecycleCleanupReleasesOnlyItsLease(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		reset(server);
		helper.assertTrue(server.getPlayerList().getPlayer(owner.getUUID()) == owner,
				"Lifecycle fixture is not present through the real online-player boundary");
		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Lifecycle fixture could not acquire the clock");
		GlobalTimeStopManager.stop(owner);
		helper.assertTrue(server.tickRateManager().isFrozen()
				&& GlobalTimeStopManager.snapshot(server).isPresent(),
				"A mismatched innate release stole the crystal lease");
		owner.setHealth(0.0F);
		GlobalTimeStopManager.tick(server);
		helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty()
				&& !server.tickRateManager().isFrozen(),
				"Owner death did not release its crystal lease");
		owner.setHealth(owner.getMaxHealth());

		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Dampening fixture could not acquire the clock");
		owner.addEffect(PowerStatusEffects.hidden(PowersEffects.AMETHYST_POISONING,
				40, 0, true, true));
		GlobalTimeStopManager.tick(server);
		helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty()
				&& !server.tickRateManager().isFrozen(),
				"Amethyst dampening did not release its crystal lease");
		owner.removeEffect(PowersEffects.AMETHYST_POISONING);

		owner.addTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().add(PowersWeapons.weapon("lycanbane").getDefaultInstance());
		helper.assertTrue(PrivateCompanionManager.handleChat(owner, "shadow, reveal yourself"),
				"Shadow-loss fixture did not accept its manifestation command");
		PrivateCompanionManager.tickPlayer(owner, server.getTickCount());
		ShadowCompanionEntity shadow = PrivateCompanionManager.body(owner.getUUID()).orElseThrow();
		helper.assertTrue(GlobalTimeStopManager.startShadow(owner, shadow),
				"Shadow-loss fixture could not acquire the clock");
		shadow.discard();
		GlobalTimeStopManager.tick(server);
		helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty()
				&& !server.tickRateManager().isFrozen(),
				"Lost Shadow body did not release its lease");
		PrivateCompanionManager.forget(owner);
		owner.removeTag(SkillSystem.DARKNESS_TAG);
		owner.getInventory().clearContent();

		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Shutdown fixture could not acquire the clock");
		GlobalTimeStopManager.onServerStopping(server);
		helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty()
				&& !server.tickRateManager().isFrozen(),
				"Production shutdown handler retained lease authority");

		helper.assertTrue(GlobalTimeStopManager.startCrystal(owner, 20),
				"Disconnect fixture could not acquire the clock");
		helper.assertTrue(owner.connection != null,
				"Lifecycle fixture has no real server connection boundary");
		java.util.UUID ownerId = owner.getUUID();
		owner.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(
				net.minecraft.network.chat.Component.literal("INT-008 disconnect lifecycle")));
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(server.getPlayerList().getPlayer(ownerId) == null,
					"Disconnected lifecycle fixture remained in the live player list");
			helper.assertTrue(!server.tickRateManager().isFrozen(),
					"Disconnect lifecycle cleanup left its clock frozen");
			helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty(),
					"Disconnect lifecycle cleanup retained lease authority");
			emit("lifecycle-cleanup", 0, 0,
					"{\"dampeningReleased\":true,\"deathReleased\":true,"
							+ "\"disconnectReleased\":true,\"leaseActive\":false,"
							+ "\"mismatchedSourcePreserved\":true,\"shadowLossReleased\":true,"
							+ "\"shutdownReleased\":true,\"vanillaFrozen\":false}");
			helper.succeed();
		});
	}

	private static WorldFixture seedWorldFixture(GameTestHelper helper, ServerPlayer player) {
		MinecraftServer server = helper.getLevel().getServer();
		SpellCastingManager.clear(player);
		SpellFieldManager.clearAll();
		PacketRateLimiter.forgetPlayer(player.getUUID());
		CelestialRuinSavedData data = server.overworld().getDataStorage()
				.computeIfAbsent(CelestialRuinSavedData.TYPE);
		data.replace(List.of());
		CelestialRuinManager.clearAll();
		PlayerPowers.PlayerPowersData powers = PlayerPowers.get(player);
		powers.clearCooldown("spell:augury");
		powers.forceRestoreEnergy();
		player.setItemInHand(InteractionHand.MAIN_HAND,
				ImportedPackItems.item("imported_book_grimoire_celestial").getDefaultInstance());
		powers.setSelectedSpell("book_grimoire_celestial", 1);
		SpellCastingManager.use(player, "book_grimoire_celestial");
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()),
				"Seeded Augury channel did not become active");
		SpellFieldManager.add(SpellFieldKind.SANCTUARY, player, 1, 4.0, 1);
		helper.assertTrue(SpellFieldManager.hasField(player.getUUID(), SpellFieldKind.SANCTUARY),
				"Seeded spell field did not become active");
		BlockPos focus = helper.absolutePos(new BlockPos(2, 2, 5));
		helper.setBlock(new BlockPos(2, 2, 5), Blocks.STONE);
		helper.assertTrue(CelestialRuinManager.begin(player, focus),
				"Seeded Celestial Ruin did not become active");
		long armedWorldTick = ((helper.getLevel().getGameTime() / 20L) + 2L) * 20L;
		((ServerLevelData) helper.getLevel().getLevelData()).setGameTime(armedWorldTick);
		powers.emptyEnergy();
		return new WorldFixture(armedWorldTick, powers.energy(),
				List.copyOf(data.snapshots()), focus);
	}

	private static void assertFixturePaused(GameTestHelper helper, ServerPlayer player,
			WorldFixture fixture, String ownerKind) {
		MinecraftServer server = helper.getLevel().getServer();
		helper.assertTrue(!TemporalClocks.worldAdvances(server, TemporalSubsystem.CHANNELS),
				ownerKind + " freeze still advanced the channel clock");
		for (int attempt = 0; attempt < 20; attempt++) {
			SpellCastingManager.tick(server);
			SpellFieldManager.tick(server);
			CelestialRuinManager.tick(server);
			RealmEventManager.tickPlayer(player, helper.getLevel(), RealmKind.LIGHT);
			helper.assertTrue(RealmHeraldManager.tick(helper.getLevel(), RealmKind.LIGHT)
					== RealmHeraldManager.TickResult.PARKED,
					ownerKind + " freeze advanced Herald cadence");
		}
		helper.assertTrue(SpellCastingManager.isChanneling(player.getUUID()),
				ownerKind + " freeze advanced an expired channel");
		helper.assertTrue(SpellFieldManager.hasField(player.getUUID(), SpellFieldKind.SANCTUARY),
				ownerKind + " freeze removed an expired field");
		List<CelestialRuinSavedData.Snapshot> celestial = server.overworld().getDataStorage()
				.computeIfAbsent(CelestialRuinSavedData.TYPE).snapshots();
		helper.assertTrue(celestial.equals(fixture.celestialSnapshots),
				ownerKind + " freeze advanced the Celestial Ruin journal");
		helper.assertTrue(helper.getLevel().getGameTime() == fixture.worldTick,
				ownerKind + " freeze advanced world time");
		helper.assertTrue(PlayerPowers.get(player).energy() == fixture.energy,
				ownerKind + " freeze advanced realm energy pressure");
	}

	private static void clearWorldFixture(MinecraftServer server, ServerPlayer player,
			WorldFixture fixture) {
		SpellCastingManager.clear(player);
		SpellFieldManager.clearAll();
		if (fixture != null) {
			CelestialRuinManager.cancelNearest((net.minecraft.server.level.ServerLevel) player.level(),
					Vec3.atCenterOf(fixture.celestialFocus));
		}
		server.overworld().getDataStorage().computeIfAbsent(CelestialRuinSavedData.TYPE)
				.replace(List.of());
		CelestialRuinManager.clearAll();
		RealmEventManager.forget(player.getUUID());
	}

	private static void emit(String caseId, long controlTicks, long worldTicks, String facts) {
		String implementationSha = System.getProperty(IMPLEMENTATION_SHA_PROPERTY, DIAGNOSTIC_SHA);
		if (!implementationSha.matches("[0-9a-f]{40}")) {
			throw new IllegalStateException("Invalid INT-008 implementation SHA");
		}
		System.out.println("INT008_TEMPORAL {\"schemaVersion\":2,\"implementationSha\":\""
				+ implementationSha + "\",\"case\":\"" + caseId + "\",\"result\":\"PASS\","
				+ "\"controlTicks\":" + controlTicks + ",\"worldTicks\":" + worldTicks
				+ ",\"facts\":" + facts + "}");
	}

	private static void reset(MinecraftServer server) {
		FREEZE_PROBES.remove(server);
		DEADLINE_PROBES.remove(server);
		GlobalTimeStopManager.clearAll(server);
		if (server.tickRateManager().isFrozen()) server.tickRateManager().setFrozen(false);
	}

	private record WorldFixture(long worldTick, int energy,
			List<CelestialRuinSavedData.Snapshot> celestialSnapshots, BlockPos celestialFocus) { }

	private static final class DeadlineProbe {
		private final GameTestHelper helper;
		private final long acquiredAt;
		private final long worldTick;
		private boolean activeAt1199;
		private boolean releasedAt1200;

		private DeadlineProbe(GameTestHelper helper, long acquiredAt, long worldTick) {
			this.helper = helper;
			this.acquiredAt = acquiredAt;
			this.worldTick = worldTick;
		}

		private void tick(MinecraftServer server) {
			long elapsed = server.getTickCount() - acquiredAt;
			if (elapsed == 1_199L) {
				activeAt1199 = GlobalTimeStopManager.snapshot(server).isPresent()
						&& server.tickRateManager().isFrozen()
						&& helper.getLevel().getGameTime() == worldTick;
				return;
			}
			if (elapsed < 1_200L) return;
			try {
				helper.assertTrue(elapsed == 1_200L,
						"Crystal lease exceeded its 1,200-control-tick deadline");
				helper.assertTrue(activeAt1199,
						"Crystal lease was not active through control tick 1,199");
				// This test hook is registered before the production lifecycle hook, so invoke
				// the same public production tick at this exact control-clock boundary before
				// observing its result. The lifecycle invokes it again idempotently afterwards.
				GlobalTimeStopManager.tick(server);
				helper.assertTrue(GlobalTimeStopManager.snapshot(server).isEmpty()
						&& !server.tickRateManager().isFrozen(),
						"Crystal lease was not released on control tick 1,200");
				// GameTest runs in accelerated batches. Refresh its public tick-rate state
				// immediately after the exact-boundary thaw so the next batch can advance;
				// a production server performs this refresh at the next wall-clock tick.
				server.tickRateManager().tick();
				helper.assertTrue(helper.getLevel().getGameTime() == worldTick,
						"World time advanced inside the crystal acceptance window");
				emit("crystal-control-deadline", elapsed, 0,
						"{\"activeAt1199\":true,\"clock\":\"CONTROL\",\"duration\":1200,"
								+ "\"releasedAt1200\":true,\"worldTicksParked\":true}");
				releasedAt1200 = true;
				DEADLINE_PROBES.remove(server);
			} catch (RuntimeException failure) {
				DEADLINE_PROBES.remove(server);
				GlobalTimeStopManager.clearAll(server);
				if (server.tickRateManager().isFrozen()) server.tickRateManager().setFrozen(false);
				throw failure;
			}
		}
	}

	private static final class FreezeProbe {
		private final Projectile projectile;
		private final Vec3 frozenAt;
		private final long startedAtControlTick;
		private final long startedAtWorldTick;
		private final long thawAtControlTick;
		private boolean checked;
		private boolean stationary;
		private double frozenDistanceSquared;

		private FreezeProbe(Projectile projectile, Vec3 frozenAt, long startedAtControlTick,
				long startedAtWorldTick, long thawAtControlTick) {
			this.projectile = projectile;
			this.frozenAt = frozenAt;
			this.startedAtControlTick = startedAtControlTick;
			this.startedAtWorldTick = startedAtWorldTick;
			this.thawAtControlTick = thawAtControlTick;
		}
	}
}
