package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerTargeting;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
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
 * dreamwalking - the blue crystal's power: with consent, watch through
 * another player's eyes for 2 minutes (2400 ticks) without altering their
 * health or making their vulnerable body safer.
 */
public class DreamwalkingAbility extends Ability {
	// 2 minutes per dream
	private static final int DURATION = 2400;
	// one dream per dreamer uuid, cleaned up on disconnect and server stop so it can't leak
	private static final Map<UUID, Dream> ACTIVE = new HashMap<>();

	private record Dream(ServerPlayer host, long endsAt) {}

	public DreamwalkingAbility() {
		super(PowersMod.id("dreamwalking"), Component.translatable("ability.powers.dreamwalking"), 1200, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// activating again while dreaming ends the current dream early
		Dream current = ACTIVE.remove(player.getUUID());
		if (current != null) {
			end(player);
			return true;
		}
		LivingEntity target = PowerTargeting.findLivingTarget(player, 32.0);
		// must be another player, not yourself
		if (!(target instanceof ServerPlayer host) || host == player) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}
		if (AmethystDampening.isDampened(host)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}
		if (!PowerProtection.mayDreamwalk(player, host)) {
			PowerMessages.send(player, "powers.packet.consent_denied", 1, host.getName().getString());
			return false;
		}
		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		Dream dream = new Dream(host, server.getTickCount() + DURATION);
		ACTIVE.put(player.getUUID(), dream);
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
				if (dreamer != null) end(dreamer);
				it.remove();
			}
		}
	}

	private static void end(ServerPlayer dreamer) {
		dreamer.setCamera(null);
	}

	// server stop - reset every surviving dreamer's camera
	public static void clearAll(MinecraftServer server) {
		for (UUID dreamerId : ACTIVE.keySet()) {
			ServerPlayer dreamer = server.getPlayerList().getPlayer(dreamerId);
			if (dreamer != null) dreamer.setCamera(null);
		}
		ACTIVE.clear();
	}

	// disconnect - end the dream and restore the camera
	public static void clear(ServerPlayer dreamer) {
		Dream dream = ACTIVE.remove(dreamer.getUUID());
		if (dream != null) end(dreamer);
	}
}
