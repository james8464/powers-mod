package com.powers;

import com.powers.fx.PowerFx;
import com.powers.util.ScheduledTaskQueue;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns short-lived server-side magical storms and delayed visual callbacks.
 * All state is advanced from the server tick and discarded during shutdown.
 */
final class ServerMagicScheduler {
	private static final List<LightningStorm> STORMS = new ArrayList<>();
	private static final ScheduledTaskQueue DELAYED = new ScheduledTaskQueue();

	private ServerMagicScheduler() {
	}

	static void startStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks,
			PowersMod.StormTheme theme) {
		STORMS.add(new LightningStorm(level, position, follow, ticks, followTicks, theme));
	}

	static void schedule(MinecraftServer server, int ticks, Runnable action) {
		DELAYED.schedule(server.getTickCount() + Math.max(1, ticks), action);
	}

	static void tick(int currentTick) {
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
	}

	/** A visual-only lightning storm that may chase a living player temporarily. */
	private static final class LightningStorm {
		private final ServerLevel level;
		private Vec3 position;
		private final ServerPlayer follow;
		private final int followTicks;
		private final int totalTicks;
		private final PowersMod.StormTheme theme;
		private int remaining;
		private boolean firstBolt = true;

		private LightningStorm(ServerLevel level, Vec3 position, ServerPlayer follow, int ticks, int followTicks,
				PowersMod.StormTheme theme) {
			this.level = level;
			this.position = position;
			this.follow = follow;
			this.totalTicks = Math.max(1, ticks);
			this.followTicks = Math.max(0, Math.min(followTicks, this.totalTicks));
			this.theme = theme;
			this.remaining = this.totalTicks;
		}

		private void tick() {
			if (follow != null && follow.isAlive() && follow.level() == level
					&& remaining > totalTicks - followTicks) {
				position = follow.position();
			}
			emitThemeParticles();
			if (remaining % 2 == 0) emitBolt();
			remaining--;
		}

		private void emitThemeParticles() {
			if (theme == PowersMod.StormTheme.DARK) {
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 4, 0.7, 0.02);
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.LARGE_SMOKE, 3, 0.6, 0.03);
			} else if (theme == PowersMod.StormTheme.LIGHT) {
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.TOTEM_OF_UNDYING, 2, 0.9, 0.12);
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.FIREWORK, 3, 0.7, 0.1);
				PowerFx.burst(level, position.add(0, 0.5, 0), ParticleTypes.END_ROD, 2, 0.5, 0.06);
			}
		}

		private void emitBolt() {
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
