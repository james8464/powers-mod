package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lets a player scout as a spectator projection within a strict 150-block radius. */
public class AstralProjectionAbility extends Ability {
	private static final int DURATION = 600;
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
			end(player, ACTIVE.remove(player.getUUID()));
			return true;
		}

		ServerLevel level = (ServerLevel) player.level();
		Projection projection = new Projection(player, level.dimension(), player.position(), player.gameMode(),
				level.getServer().getTickCount() + DURATION);
		ACTIVE.put(player.getUUID(), projection);
		player.setGameMode(GameType.SPECTATOR);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.SOUL, 24, 0.8, 0.03);
		PowerFx.sound(level, player.position(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.8f);
		player.sendSystemMessage(Component.translatable("ability.powers.astral_started"));
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
				it.remove();
				continue;
			}
			if (now >= projection.endsAt() || player.level().dimension() != projection.dimension()) {
				end(player, projection);
				it.remove();
				continue;
			}
			if (player.position().distanceToSqr(projection.origin()) > RADIUS * RADIUS) {
				player.teleport(new TeleportTransition((ServerLevel) player.level(), projection.origin(), Vec3.ZERO,
						player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
				player.sendSystemMessage(Component.translatable("ability.powers.astral_boundary"));
			}
			if (now % 5 == 0) {
				PowerFx.burst((ServerLevel) player.level(), player.position().add(0, 1, 0),
						ParticleTypes.SOUL_FIRE_FLAME, 2, 0.35, 0.01);
			}
		}
	}

	private static void end(ServerPlayer player, Projection projection) {
		ServerLevel destination = ((ServerLevel) player.level()).getServer().getLevel(projection.dimension());
		if (destination != null) {
			player.teleport(new TeleportTransition(destination, projection.origin(), Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		}
		player.setGameMode(projection.gameMode());
		player.sendSystemMessage(Component.translatable("ability.powers.astral_ended"));
	}

	public static void clear(UUID player) {
		Projection projection = ACTIVE.remove(player);
		if (projection != null && projection.player().isAlive()) end(projection.player(), projection);
	}

	public static void clearAll() {
		for (Projection projection : ACTIVE.values()) {
			if (projection.player().isAlive()) end(projection.player(), projection);
		}
		ACTIVE.clear();
	}
}
