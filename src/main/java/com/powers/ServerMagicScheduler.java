package com.powers;

import com.powers.fx.PowerFx;
import com.powers.util.ScheduledTaskQueue;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Owns short-lived server-side magical storms and delayed visual callbacks.
 * All state is advanced from the server tick and discarded during shutdown.
 */
final class ServerMagicScheduler {
	private static final int MAX_STORMS = 32;
	private static final int LOG_INTERVAL_TICKS = 200;
	private static final List<LightningStorm> STORMS = new ArrayList<>();
	private static final ScheduledTaskQueue DELAYED = new ScheduledTaskQueue(8_192, 256,
			ServerMagicScheduler::reportTaskFailure);
	private static long currentTick;
	private static long lastFailureLogTick = -LOG_INTERVAL_TICKS;
	private static long lastCapacityLogTick = -LOG_INTERVAL_TICKS;

	private ServerMagicScheduler() {
	}

	static void startStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks,
			PowersMod.StormTheme theme) {
		UUID owner = follow == null ? null : follow.getUUID();
		int replacementIndex = owner == null ? -1 : findStorm(owner);
		if (!canAdmitStorm(STORMS.size(), replacementIndex >= 0)) {
			if (currentTick - lastCapacityLogTick >= LOG_INTERVAL_TICKS) {
				lastCapacityLogTick = currentTick;
				PowersMod.LOGGER.warn("Refused visual storm because the {}-storm cap is full", MAX_STORMS);
			}
			return;
		}
		LightningStorm storm = new LightningStorm(level.getServer(), level.dimension(), position,
				owner, ticks, followTicks, theme);
		if (replacementIndex >= 0) STORMS.set(replacementIndex, storm);
		else STORMS.add(storm);
	}

	static boolean canAdmitStorm(int activeStorms, boolean replacesOwnerStorm) {
		return replacesOwnerStorm || activeStorms < MAX_STORMS;
	}

	private static int findStorm(UUID owner) {
		for (int index = 0; index < STORMS.size(); index++) {
			if (owner.equals(STORMS.get(index).followId)) return index;
		}
		return -1;
	}

	static ScheduledTaskQueue.TaskToken schedule(MinecraftServer server, int ticks, Runnable action) {
		ScheduledTaskQueue.TaskToken token = DELAYED.schedule(
				server.getTickCount() + Math.max(1, ticks), action);
		if (!token.accepted() && currentTick - lastCapacityLogTick >= LOG_INTERVAL_TICKS) {
			lastCapacityLogTick = currentTick;
			PowersMod.LOGGER.warn("Refused delayed magic task because the 8192-task cap is full");
		}
		return token;
	}

	static void tick(int currentTick) {
		ServerMagicScheduler.currentTick = currentTick;
		var iterator = STORMS.iterator();
		while (iterator.hasNext()) {
			LightningStorm storm = iterator.next();
			storm.tick();
			if (storm.finished()) iterator.remove();
		}
		DELAYED.runDue(currentTick);
	}

	static void clear() {
		STORMS.clear();
		DELAYED.clear();
		currentTick = 0L;
		lastFailureLogTick = -LOG_INTERVAL_TICKS;
		lastCapacityLogTick = -LOG_INTERVAL_TICKS;
	}

	private static void reportTaskFailure(Throwable failure) {
		if (currentTick - lastFailureLogTick < LOG_INTERVAL_TICKS) return;
		lastFailureLogTick = currentTick;
		PowersMod.LOGGER.error("Isolated delayed magic task failure", failure);
	}

	/** A visual-only lightning storm that may chase a living player temporarily. */
	private static final class LightningStorm {
		private final MinecraftServer server;
		private final ResourceKey<Level> dimension;
		private Vec3 position;
		private final UUID followId;
		private final int followTicks;
		private final int totalTicks;
		private final PowersMod.StormTheme theme;
		private int remaining;
		private boolean firstBolt = true;

		private LightningStorm(MinecraftServer server, ResourceKey<Level> dimension, Vec3 position,
				UUID followId, int ticks, int followTicks,
				PowersMod.StormTheme theme) {
			this.server = server;
			this.dimension = dimension;
			this.position = position;
			this.followId = followId;
			this.totalTicks = Math.max(1, ticks);
			this.followTicks = Math.max(0, Math.min(followTicks, this.totalTicks));
			this.theme = theme;
			this.remaining = this.totalTicks;
		}

		private void tick() {
			ServerLevel level = server.getLevel(dimension);
			if (level == null) {
				remaining = 0;
				return;
			}
			ServerPlayer follow = followId == null ? null : server.getPlayerList().getPlayer(followId);
			if (follow != null && follow.isAlive() && follow.level() == level
					&& remaining > totalTicks - followTicks) {
				position = follow.position();
			}
			emitThemeParticles(level);
			if (remaining % 2 == 0) emitBolt(level);
			remaining--;
		}

		private void emitThemeParticles(ServerLevel level) {
			if (theme == PowersMod.StormTheme.DARK) {
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 4, 0.7, 0.02);
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.LARGE_SMOKE, 3, 0.6, 0.03);
			} else if (theme == PowersMod.StormTheme.LIGHT) {
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.TOTEM_OF_UNDYING, 2, 0.9, 0.12);
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.FIREWORK, 3, 0.7, 0.1);
				PowerFx.burst(level, position.add(0, 0.5, 0), com.powers.PowersParticles.GLYPH, 2, 0.5, 0.06);
			}
		}

		private void emitBolt(ServerLevel level) {
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt == null) return;
			bolt.setVisualOnly(true);
			bolt.setSilent(!firstBolt);
			bolt.setPos(position.x, position.y, position.z);
			level.addFreshEntity(bolt);
			firstBolt = false;
		}

		private boolean finished() {
			return remaining <= 0;
		}
	}
}
