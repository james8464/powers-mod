package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerTargeting;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * dreamwalking - the blue crystal's power: watch through another player's
 * eyes for 2 minutes (2400 ticks) while their health is capped at half, and
 * they get it all back the moment the dream ends
 */
public class DreamwalkingAbility extends Ability {
	// 2 minutes per dream
	private static final int DURATION = 2400;
	// one dream per dreamer uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, Dream> ACTIVE = new HashMap<>();

	// the host whose eyes we watch, plus their health before the dream so we can give it back
	private record Dream(ServerPlayer host, float savedHealth, long endsAt) {}

	public DreamwalkingAbility() {
		super(PowersMod.id("dreamwalking"), Component.translatable("ability.powers.dreamwalking"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// activating again while dreaming ends the current dream early
		Dream current = ACTIVE.remove(player.getUUID());
		if (current != null) {
			ServerLevel level = (ServerLevel) player.level();
			end(player, current, level.getServer());
			return true;
		}
		LivingEntity target = PowerTargeting.findLivingTarget(player, 32.0);
		// must be another player, not yourself
		if (!(target instanceof ServerPlayer host) || host == player) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}
		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		Dream dream = new Dream(host, host.getHealth(), server.getTickCount() + DURATION);
		ACTIVE.put(player.getUUID(), dream);
		// the host is weakened while their mind is watched - health capped at half
		host.setHealth(Math.min(host.getHealth(), host.getMaxHealth() / 2.0f));
		player.setCamera(host);
		ServerLevel level = (ServerLevel) player.level();
		com.powers.fx.PowerFx.beam(level, player.getEyePosition(), host.getEyePosition(),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18);
		com.powers.fx.PowerFx.sound(level, host.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.45f);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer dreamer = server.getPlayerList().getPlayer(entry.getKey());
			Dream dream = entry.getValue();
			boolean hostOnline = server.getPlayerList().getPlayer(dream.host().getUUID()) == dream.host();
			// end the dream if the dreamer or the host dies or logs off, or when time runs out
			if (dreamer == null || !dreamer.isAlive() || !hostOnline || !dream.host().isAlive()
					|| now >= dream.endsAt()) {
				if (dreamer != null) end(dreamer, dream, server);
				it.remove();
			}
		}
	}

	/** Restores the host's health to its saved value via the live host instance. */
	private static void end(ServerPlayer dreamer, Dream dream, MinecraftServer server) {
		dreamer.setCamera(null);
		ServerPlayer host = server.getPlayerList().getPlayer(dream.host().getUUID());
		// the host may have died during the dream - then there's nothing to restore
		if (host != null && host.isAlive()) {
			host.setHealth(Math.min(dream.savedHealth(), host.getMaxHealth()));
		}
	}

	// server stop - give every host their health back before the world shuts down
	public static void clearAll(MinecraftServer server) {
		for (Dream dream : ACTIVE.values()) {
			ServerPlayer host = server.getPlayerList().getPlayer(dream.host().getUUID());
			if (host != null && host.isAlive()) {
				host.setHealth(Math.min(dream.savedHealth(), host.getMaxHealth()));
			}
		}
		ACTIVE.clear();
	}

	// disconnect - end the dream and restore the host
	public static void clear(ServerPlayer dreamer, MinecraftServer server) {
		Dream dream = ACTIVE.remove(dreamer.getUUID());
		if (dream != null && dreamer.isAlive()) end(dreamer, dream, server);
	}
}
