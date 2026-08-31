package com.powers.gametest;

import com.powers.player.PlayerPowers;
import com.powers.power.abilities.FireballAbility;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.GlobalTimeStopManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Exercises the real Cinderheart callback across external and both owned freeze cases. */
@SuppressWarnings("removal")
public final class TemporalProjectileGameTests {
	private static final Map<MinecraftServer, Probe> PROBES = new IdentityHashMap<>();
	private static boolean registered;

	public TemporalProjectileGameTests() {
		if (registered) return;
		registered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Probe probe = PROBES.get(server);
			if (probe == null) return;
			probe.aliveThroughout &= probe.projectile.isAlive();
			probe.stationary &= probe.projectile.position().distanceToSqr(probe.position) < 1.0E-8;
			probe.parked &= probe.helper.getLevel().getGameTime() == probe.worldStarted;
			if (server.getTickCount() - probe.controlStarted < 400) return;
			// Even an overdue deadline must wait for world simulation to resume.
			long restore = probe.helper.getLevel().getGameTime();
			try {
				((ServerLevelData) probe.helper.getLevel().getLevelData()).setGameTime(
						probe.worldStarted + (probe.scenario % 2 == 0 ? 240 : 120));
				FireballAbility.tickAll(server);
				probe.expiryParked = probe.projectile.isAlive();
			} finally {
				((ServerLevelData) probe.helper.getLevel().getLevelData()).setGameTime(restore);
			}
			probe.exactLease = probe.freezer == null
					? GlobalTimeStopManager.snapshot(server).isEmpty()
					: GlobalTimeStopManager.snapshot(server).map(lease -> lease.leaseToken() == probe.token
							&& lease.owner().equals(probe.freezer.getUUID())
							&& lease.source().equals("CRYSTAL")).orElse(false);
			PROBES.remove(server);
			if (probe.freezer == null) server.tickRateManager().setFrozen(false);
			else GlobalTimeStopManager.stopCrystal(probe.freezer);
			probe.helper.runAfterDelay(1, () -> probe.verifyAndContinue());
		});
	}

	@GameTest(environment = "powers:temporal_cinderheart_isolated", maxTicks = 3_000)
	public void cinderheartLifetimePausesUnderEveryVanillaFreeze(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 2));
		caster.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		caster.setYRot(0.0F);
		caster.setXRot(-90.0F);
		PlayerPowers.get(caster).setSlots(caster,
				List.of("powers:fireball", "powers:flight", "powers:super_speed"));
		new Probe(helper, caster, other, 0).start();
	}

	private static final class Probe {
		private final GameTestHelper helper;
		private final ServerPlayer caster;
		private final ServerPlayer other;
		private final int scenario;
		private LargeFireball projectile;
		private ServerPlayer freezer;
		private long worldStarted;
		private long controlStarted;
		private Vec3 position;
		private boolean aliveThroughout = true;
		private boolean stationary = true;
		private boolean parked = true;
		private boolean exactLease;
		private boolean expiryParked;
		private long token;

		private Probe(GameTestHelper helper, ServerPlayer caster, ServerPlayer other, int scenario) {
			this.helper = helper;
			this.caster = caster;
			this.other = other;
			this.scenario = scenario;
		}

		private void start() {
			MinecraftServer server = helper.getLevel().getServer();
			FireballAbility.clearAll(server);
			FireballAbility ability = new FireballAbility();
			caster.setShiftKeyDown(false);
			caster.setPose(net.minecraft.world.entity.Pose.STANDING);
			helper.assertTrue(ability.activate(caster, PlayerPowers.get(caster)), "Cinderheart setup failed");
			projectile = helper.getLevel().getEntitiesOfClass(LargeFireball.class,
					caster.getBoundingBox().inflate(8), entity -> entity.isAlive()
							&& entity.getOwner() == caster).stream().findFirst().orElseThrow();
			if (scenario % 2 == 1) {
				caster.setShiftKeyDown(true);
				caster.setPose(net.minecraft.world.entity.Pose.CROUCHING);
				helper.assertTrue(ability.activate(caster, PlayerPowers.get(caster)), "Cinderheart launch failed");
				helper.assertTrue(projectile.getDeltaMovement().lengthSqr() > 1.0,
						"Launch fixture charged instead of releasing");
				caster.setShiftKeyDown(false);
				caster.setPose(net.minecraft.world.entity.Pose.STANDING);
			}
			worldStarted = helper.getLevel().getGameTime();
			controlStarted = server.getTickCount();
			position = projectile.position();
			freezer = scenario / 2 == 0 ? null : scenario / 2 == 1 ? caster : other;
			if (freezer == null) server.tickRateManager().setFrozen(true);
			else helper.assertTrue(GlobalTimeStopManager.startCrystal(freezer, 1_200), "Freeze setup failed");
			token = GlobalTimeStopManager.snapshot(server).map(lease -> lease.leaseToken()).orElse(0L);
			PROBES.put(server, this);
		}

		private void verifyAndContinue() {
			MinecraftServer server = helper.getLevel().getServer();
			long restoreWorld = helper.getLevel().getGameTime();
			try {
				helper.assertTrue(expiryParked, "Cinderheart processed an overdue expiry while world simulation was parked");
				helper.assertTrue(aliveThroughout && projectile.isAlive(),
						"Cinderheart expired or was discarded during frozen world time: scenario " + scenario);
				helper.assertTrue(stationary && parked, "Cinderheart/world moved during freeze");
				helper.assertTrue(exactLease, "Freeze ownership changed during the measured interval");
				long expiry = worldStarted + (scenario % 2 == 0 ? 240 : 120);
				((ServerLevelData) helper.getLevel().getLevelData()).setGameTime(expiry - 1);
				FireballAbility.tickAll(server);
				helper.assertTrue(projectile.isAlive(), "Cinderheart lost remaining world lifetime after thaw");
				((ServerLevelData) helper.getLevel().getLevelData()).setGameTime(expiry);
				FireballAbility.tickAll(server);
				helper.assertTrue(projectile.isRemoved(), "Cinderheart did not expire at its authored world deadline");
			} finally {
				((ServerLevelData) helper.getLevel().getLevelData()).setGameTime(restoreWorld);
				FireballAbility.clearAll(server);
				GlobalTimeStopManager.clearAll(server);
				if (server.tickRateManager().isFrozen()) server.tickRateManager().setFrozen(false);
			}
			if (scenario < 5) helper.runAfterDelay(1, () -> new Probe(helper, caster, other, scenario + 1).start());
			else {
				verifyLocalFreezeStillInterrupts();
				helper.succeed();
			}
		}

		private void verifyLocalFreezeStillInterrupts() {
			MinecraftServer server = helper.getLevel().getServer();
			FireballAbility ability = new FireballAbility();
			helper.assertTrue(ability.activate(caster, PlayerPowers.get(caster)), "Local-freeze setup failed");
			LargeFireball local = helper.getLevel().getEntitiesOfClass(LargeFireball.class,
					caster.getBoundingBox().inflate(8), entity -> entity.isAlive()
							&& entity.getOwner() == caster).stream().findFirst().orElseThrow();
			try {
				server.tickRateManager().setFrozen(true);
				EntityFreezeController.claim(caster, other.getUUID());
				FireballAbility.tickAll(server);
				helper.assertTrue(local.isRemoved(), "Global freeze hid a genuine local-freeze interruption");
			} finally {
				EntityFreezeController.release(other.getUUID(), List.of(caster.getUUID()));
				FireballAbility.clearAll(server);
				server.tickRateManager().setFrozen(false);
			}
		}
	}
}
