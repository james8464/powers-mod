package com.powers.gametest;

import com.powers.PowersEntities;
import com.powers.PowersItems;
import com.powers.PowersWeapons;
import com.powers.item.ArtifactWeaponManager;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AbilityActivationService;
import com.powers.power.ActivationCooldowns;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.power.state.GlobalTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Real selected Chrono Stop routes park each boss until the exact control-clock deadline. */
public final class BossChronoStopGameTests {
	private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();
	private static boolean registered;

	public BossChronoStopGameTests() {
		if (registered) return;
		registered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			var probe = PROBES.get(server);
			if (probe != null) probe.tick(server);
		});
	}

	@GameTest(environment = "powers:temporal_deadline_isolated", maxTicks = 1300)
	public void blueChronoParksAndReleasesEveryBoss(GameTestHelper helper) { freeze(helper, false); }
	@GameTest(environment = "powers:temporal_deadline_isolated", maxTicks = 1300)
	public void artifactChronoParksAndReleasesEveryBoss(GameTestHelper helper) { freeze(helper, true); }

	@GameTest(environment = "powers:temporal_deadline_isolated", maxTicks = 100)
	public void artifactTimeFreezeParksAndReleasesEveryBoss(GameTestHelper helper) {
		freeze(helper, true, true);
	}

	private static void freeze(GameTestHelper helper, boolean artifact) {
		freeze(helper, artifact, false);
	}

	@SuppressWarnings("removal")
	private static void freeze(GameTestHelper helper, boolean artifact, boolean toggle) {
		var server = helper.getLevel().getServer();
		var caster = helper.makeMockServerPlayerInLevel();
		caster.setGameMode(GameType.SURVIVAL);
		caster.setNoGravity(true);
		var origin = helper.absolutePos(new BlockPos(4, 3, 4));
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		for (int x = chunkX - 1; x <= chunkX + 1; x++) for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
			helper.getLevel().setChunkForced(x, z, true);
		}
		caster.snapTo(origin.getX() + 0.5, 100, origin.getZ() + 0.5);
		var data = PlayerPowers.get(caster);
		if (artifact) {
			caster.addTag(SkillSystem.DARKNESS_TAG);
			data.setDarknessLevel(caster, 1);
			caster.setItemInHand(InteractionHand.MAIN_HAND, PowersWeapons.weapon("lycanbane").getDefaultInstance());
			helper.assertTrue(ArtifactWeaponManager.select(caster, ArtifactAlignment.DARKNESS,
					toggle ? "innate/time_freeze" : "crystal/chrono_stop", -1), "Temporal selection failed");
		} else caster.setItemInHand(InteractionHand.MAIN_HAND, PowersItems.BLUE_CRYSTAL.getDefaultInstance());
		data.forceRestoreEnergy();
		List<Mob> bosses = List.of(helper.spawn(PowersEntities.LIGHT_HERALD, new BlockPos(8, 3, 4)),
				helper.spawn(PowersEntities.DARK_HERALD, new BlockPos(12, 3, 4)),
				helper.spawn(PowersEntities.FIRST_VESSEL, new BlockPos(16, 3, 4)));
		for (int i = 0; i < bosses.size(); i++) {
			var boss = bosses.get(i);
			boss.setNoGravity(true);
			boss.setPos(caster.getX() + 4 + i * 4, 100, caster.getZ());
			boss.setDeltaMovement(0.1, 0, 0);
		}
		helper.startSequence().thenWaitUntil(() ->
				helper.assertTrue(bosses.stream().allMatch(b -> b.tickCount >= 5),
						"Boss fixture was not ticking before freeze: "
								+ bosses.stream().map(b -> b.getType() + ":" + b.tickCount).toList()))
				.thenExecute(() -> {
		int energy = data.energy();
		long acquired = server.getTickCount();
		if (artifact) helper.assertTrue(ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
				== AbilityActivationService.Result.ACTIVATED, "Artifact Chrono Stop refused");
		else helper.assertTrue(CrystalPowerRegistry.tryActivate(caster, PowersItems.BLUE_CRYSTAL), "Blue Chrono Stop refused");
		var ability = artifact ? ArtifactWeaponManager.action(ArtifactAlignment.DARKNESS, "crystal/chrono_stop").ability()
				: CrystalPowerRegistry.get(PowersItems.BLUE_CRYSTAL);
		helper.assertTrue(toggle || data.energy() < energy && ActivationCooldowns.remainingTicks(caster, ability) == 3600,
				"Chrono Stop bypassed payment/cooldown");
		var lease = GlobalTimeStopManager.snapshot(server).orElseThrow();
		helper.assertTrue(lease.owner().equals(caster.getUUID())
				&& (toggle ? lease.remainingTicks() == -1 : lease.deadline() - acquired == 1200)
				&& server.tickRateManager().isFrozen(), "Wrong freeze owner or capped deadline");
		Runnable releaseToggle = toggle ? () -> helper.assertTrue(
				ArtifactWeaponManager.activateSelected(caster, ArtifactAlignment.DARKNESS)
						== AbilityActivationService.Result.ACTIVATED
						&& !data.isToggleActive("artifact/darkness/innate/time_freeze"),
				"Artifact Time Freeze failed to toggle off") : null;
		var probe = new Probe(helper, acquired, helper.getLevel().getGameTime(), bosses, releaseToggle);
		PROBES.put(server, probe);
		helper.succeedWhen(() -> {
			helper.assertTrue(probe.released, "Control-clock probe has not released the lease");
			helper.assertTrue(bosses.stream().allMatch(b -> b.tickCount > probe.ages.get(bosses.indexOf(b))),
					"A boss failed to resume ticking after thaw");
			for (int x = chunkX - 1; x <= chunkX + 1; x++) for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
				helper.getLevel().setChunkForced(x, z, false);
			}
		});
		});
	}

	private static final class Probe {
		private final GameTestHelper helper;
		private final long acquired;
		private final long worldTick;
		private final List<Mob> bosses;
		private final List<Integer> ages;
		private final List<Vec3> positions;
		private final Runnable releaseToggle;
		private boolean boundarySeen;
		private boolean released;

		private Probe(GameTestHelper helper, long acquired, long worldTick, List<Mob> bosses, Runnable releaseToggle) {
			this.helper = helper;
			this.acquired = acquired;
			this.worldTick = worldTick;
			this.bosses = bosses;
			this.releaseToggle = releaseToggle;
			ages = bosses.stream().map(b -> b.tickCount).toList();
			positions = bosses.stream().map(Mob::position).toList();
		}

		private void tick(MinecraftServer server) {
			long elapsed = server.getTickCount() - acquired;
			if (elapsed < (releaseToggle == null ? 1199 : 5)) return;
			try {
				helper.assertTrue(helper.getLevel().getGameTime() == worldTick, "Frozen world clock advanced");
				for (int i = 0; i < bosses.size(); i++) {
					helper.assertTrue(bosses.get(i).tickCount == ages.get(i)
							&& bosses.get(i).position().equals(positions.get(i)), "Boss tick/motion advanced while frozen");
				}
				if (releaseToggle != null) {
					releaseToggle.run();
				} else if (elapsed == 1199) {
					boundarySeen = server.tickRateManager().isFrozen() && GlobalTimeStopManager.snapshot(server).isPresent();
					return;
				} else {
					helper.assertTrue(elapsed == 1200 && boundarySeen, "Freeze did not last exactly 1200 control ticks");
					GlobalTimeStopManager.tick(server);
				}
				helper.assertTrue(!server.tickRateManager().isFrozen() && GlobalTimeStopManager.snapshot(server).isEmpty(),
						"Deadline retained freeze authority");
				server.tickRateManager().tick();
				released = true;
				PROBES.remove(server);
			} catch (RuntimeException failure) {
				PROBES.remove(server);
				GlobalTimeStopManager.clearAll(server);
				if (server.tickRateManager().isFrozen()) server.tickRateManager().setFrozen(false);
				throw failure;
			}
		}
	}
}
