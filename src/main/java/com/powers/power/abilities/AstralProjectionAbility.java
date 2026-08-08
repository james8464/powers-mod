package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Astral Projection: your spirit leaves your body and you scout around as an
 * untouchable ghost, yanked back if you drift more than 150 blocks from home.
 */
public class AstralProjectionAbility extends Ability {
	// 30 seconds as a ghost
	private static final int DURATION = 600;
	// leash radius in blocks
	private static final double RADIUS = 150.0;
	private static final Map<UUID, Projection> ACTIVE = new HashMap<>();

	private record Projection(ServerPlayer player, ResourceKey<Level> dimension, Vec3 origin, GameType gameMode, long endsAt) {}

	public AstralProjectionAbility() {
		super(PowersMod.id("astral_projection"),
				Component.translatable("ability.powers.astral_projection"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (ACTIVE.containsKey(player.getUUID())) {
			// activating again while projecting ends it early and returns home
			end(player, ACTIVE.remove(player.getUUID()));
			return true;
		}

		ServerLevel level = (ServerLevel) player.level();
		if (!BodyProxyManager.start(player, BodyProxyKind.ASTRAL)) return false;
		Projection projection = new Projection(player, level.dimension(), player.position(), player.gameMode(),
				level.getServer().getTickCount() + DURATION);
		ACTIVE.put(player.getUUID(), projection);
		player.setGameMode(GameType.SPECTATOR);
		PowerFx.rune(level, player.position(), 1.8, 0x7C4DFF, 28, 0.0);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.SOUL, 24, 0.8, 0.03);
		PowerFx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
		PowerMessages.send(player, "ability.powers.astral_started", 3);
		return true;
	}

	public static boolean isActive(UUID player) {
		return ACTIVE.containsKey(player);
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			Projection projection = entry.getValue();
			ServerPlayer player = projection.player();
			if (player == null || !player.isAlive()) {
				// a dead ghost would otherwise stay stuck in spectator forever
				if (player != null) player.setGameMode(projection.gameMode());
				it.remove();
				continue;
			}
			if (now >= projection.endsAt() || player.level().dimension() != projection.dimension()) {
				// time is up or the ghost switched dimensions, send them home
				end(player, projection);
				it.remove();
				continue;
			}
			if (player.position().distanceToSqr(projection.origin()) > RADIUS * RADIUS) {
				// leash snapped: teleport the ghost back to the origin
				PowerFx.sound((ServerLevel) player.level(), player.position(), SoundEvents.ENDERMAN_TELEPORT, 0.75f, 0.75f);
				PowerFx.rune((ServerLevel) player.level(), player.position(), 1.2, 0x7C4DFF, 18, now * 0.125);
				player.teleport(new TeleportTransition((ServerLevel) player.level(), projection.origin(), Vec3.ZERO,
						player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
				PowerMessages.send(player, "ability.powers.astral_boundary", 3);
			}
			if (now % 5 == 0) {
				// every 5 ticks trail soul flames behind the ghost so it's visible
				ServerLevel activeLevel = (ServerLevel) player.level();
				PowerFx.rune(activeLevel, player.position().add(0, 0.5, 0), 1.0, 0x7C4DFF, 16, now * 0.1);
				PowerFx.burst(activeLevel, player.position().add(0, 1, 0),
						ParticleTypes.SOUL_FIRE_FLAME, 2, 0.35, 0.01);
			}
		}
	}

	private static void end(ServerPlayer player, Projection projection) {
		// bring the ghost back to the recorded origin, then restore game mode
		ServerLevel destination = ((ServerLevel) player.level()).getServer().getLevel(projection.dimension());
		boolean returned = BodyProxyManager.returnToBody(player);
		if (!returned && destination != null) {
			player.teleport(new TeleportTransition(destination, projection.origin(), Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
			PowerFx.burst(destination, projection.origin().add(0, 1, 0), ParticleTypes.SOUL, 16, 0.8, 0.02);
			PowerFx.sound(destination, projection.origin(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.95f);
		}
		if (!returned) {
			player.setGameMode(projection.gameMode());
			BodyProxyManager.finish(player);
		}
		PowerMessages.send(player, "ability.powers.astral_ended", 3);
	}

	public static void clear(UUID player) {
		Projection projection = ACTIVE.remove(player);
		if (projection != null && projection.player().isAlive()) end(projection.player(), projection);
		else if (projection != null) BodyProxyManager.discardOnDeath(projection.player());
	}

	public static void clearAll() {
		for (Projection projection : ACTIVE.values()) {
			if (projection.player().isAlive()) end(projection.player(), projection);
		}
		ACTIVE.clear();
	}
}
