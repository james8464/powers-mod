package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

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

		HitResult hit = player.pick(32.0, 0.0f, false);
		if (!(hit instanceof EntityHitResult eHit) || !(eHit.getEntity() instanceof ServerPlayer target)) {
			PowerMessages.send(player, "ability.powers.no_player_target", 4);
			return false;
		}
		if (target == player) return false;
		if (AmethystDampening.isDampened(target)) {
			PowerMessages.send(player, "amethyst.powers.target_protected", 4);
			return false;
		}

		MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
		POSSESSING.put(player.getUUID(), new Possession(target, server.getTickCount() + POSSESS_TICKS));
		player.setCamera(target);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.beam(level, player.getEyePosition(), target.getEyePosition(),
					net.minecraft.core.particles.ParticleTypes.ENCHANT, 14);
			com.powers.fx.PowerFx.burst(level, target.position().add(0, 1, 0),
					net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18, 0.5, 0.01);
			com.powers.fx.PowerFx.sound(level, target.position(),
					net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 0.6f);
		}
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = POSSESSING.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			Possession possession = entry.getValue();
			if (owner == null || !owner.isAlive() || !possession.target().isAlive()
					|| now >= possession.endsAt()) {
				if (owner != null) owner.setCamera(null);
				it.remove();
			}
		}
	}

	public static void clear(UUID owner) {
		POSSESSING.remove(owner);
	}

	public static void clearAll() {
		POSSESSING.clear();
	}
}
