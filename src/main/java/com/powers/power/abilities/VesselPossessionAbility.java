package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
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

public class VesselPossessionAbility extends Ability {
	private static final int POSSESS_TICKS = 200;
	private record Possession(ServerPlayer target, long endsAt) {}
	private static final Map<UUID, Possession> POSSESSING = new HashMap<>();

	public VesselPossessionAbility() {
		super(PowersMod.id("vessel_possession"),
				Component.translatable("ability.powers.vessel_possession"),
				600, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (POSSESSING.containsKey(player.getUUID())) return false;

		LivingEntity target = PowerTargeting.findLivingTarget(player, 32.0);
		if (!(target instanceof ServerPlayer targetSP) || targetSP == player) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}
		if (AmethystDampening.isDampened(targetSP)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}

		MinecraftServer server = ((ServerLevel) player.level()).getServer();
		POSSESSING.put(player.getUUID(), new Possession(targetSP, server.getTickCount() + POSSESS_TICKS));
		player.setCamera(targetSP);
		ServerLevel level = (ServerLevel) player.level();
		com.powers.fx.PowerFx.beam(level, player.getEyePosition(), targetSP.getEyePosition(),
				net.minecraft.core.particles.ParticleTypes.ENCHANT, 14);
		com.powers.fx.PowerFx.burst(level, targetSP.position().add(0, 1, 0),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18, 0.5, 0.01);
		com.powers.fx.PowerFx.sound(level, targetSP.position(),
				net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = POSSESSING.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			Possession possession = entry.getValue();
			boolean targetOnline = server.getPlayerList().getPlayer(possession.target().getUUID()) == possession.target();
			if (owner == null || !owner.isAlive() || !possession.target().isAlive()
					|| !targetOnline || now >= possession.endsAt()) {
				if (owner != null) owner.setCamera(null);
				it.remove();
			}
		}
	}

	/** Ends any possession by the given player, resetting their camera. */
	public static void clear(ServerPlayer owner) {
		if (POSSESSING.remove(owner.getUUID()) != null && owner.isAlive()) {
			owner.setCamera(null);
		}
	}

	public static void clearAll() {
		POSSESSING.clear();
	}
}
