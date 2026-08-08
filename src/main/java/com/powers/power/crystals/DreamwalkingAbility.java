package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
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
import net.minecraft.sounds.SoundEvents;
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
	private static final double BASE_RANGE = 32.0;
	private static final int DURATION = 2400;
	private static final Map<UUID, Dream> ACTIVE = new HashMap<>();

	private record Dream(UUID hostId, long endsAt) {}

	public DreamwalkingAbility() {
		super(PowersMod.id("dreamwalking"), Component.translatable("ability.powers.dreamwalking"), 1200, false);
	}

	@Override
	public boolean isSelectionAction(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		// activating again while dreaming ends the current dream early
		Dream current = ACTIVE.remove(player.getUUID());
		if (current != null) {
			end(player);
			return true;
		}
		LivingEntity target = PowerTargeting.findLivingTarget(player, scaledRange(player, BASE_RANGE));
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
		if (!BodyProxyManager.start(player, BodyProxyKind.DREAMWALK)) return false;
		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		Dream dream = new Dream(host.getUUID(), server.getTickCount() + scaledDuration(player, DURATION));
		ACTIVE.put(player.getUUID(), dream);
		player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
		player.setCamera(host);
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.beam(level, player.getEyePosition(), host.getEyePosition(),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18);
		PowerFx.rune(level, player.position(), 1.5, 0x3F51B5, 28, 0.0);
		PowerFx.rune(level, host.position(), 1.1, 0x81D4FA, 20, Math.PI);
		PowerFx.sound(level, host.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.45f);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer dreamer = server.getPlayerList().getPlayer(entry.getKey());
			Dream dream = entry.getValue();
			ServerPlayer host = server.getPlayerList().getPlayer(dream.hostId());
			boolean invalid = dreamer == null || !dreamer.isAlive() || host == null || !host.isAlive()
					|| now >= dream.endsAt();
			// Consent and amethyst are live counterplay, not one-time entry checks.
			if (!invalid) {
				invalid = AmethystDampening.isDampened(dreamer) || AmethystDampening.isDampened(host)
						|| !PowerProtection.mayDreamwalk(dreamer, host);
			}
			if (invalid) {
				if (dreamer != null) end(dreamer);
				it.remove();
			} else if (now % 20 == 0) {
				ServerLevel hostLevel = (ServerLevel) host.level();
				PowerFx.coloredBurst(hostLevel, host.getEyePosition(), 0x7986CB, 3, 0.22);
			}
		}
	}

	private static void end(ServerPlayer dreamer) {
		ServerLevel level = (ServerLevel) dreamer.level();
		PowerFx.rune(level, dreamer.position(), 1.1, 0x7986CB, 18, Math.PI);
		PowerFx.sound(level, dreamer.position(), SoundEvents.BEACON_DEACTIVATE, 0.65f, 1.35f);
		dreamer.setCamera(null);
		BodyProxyManager.returnToBody(dreamer);
	}

	/** Resets every surviving dreamer's camera during server shutdown. */
	public static void clearAll(MinecraftServer server) {
		for (UUID dreamerId : ACTIVE.keySet()) {
			ServerPlayer dreamer = server.getPlayerList().getPlayer(dreamerId);
			if (dreamer != null) dreamer.setCamera(null);
		}
		ACTIVE.clear();
	}

	/** Ends one dream and restores its camera during disconnect cleanup. */
	public static void clear(ServerPlayer dreamer) {
		Dream dream = ACTIVE.remove(dreamer.getUUID());
		if (dream != null) end(dreamer);
	}
}
