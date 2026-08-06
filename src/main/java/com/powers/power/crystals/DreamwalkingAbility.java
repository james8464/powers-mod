package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DreamwalkingAbility extends Ability {
	private static final int DURATION = 2400;
	private static final Map<UUID, Dream> ACTIVE = new HashMap<>();

	private record Dream(ServerPlayer host, float savedHealth, long endsAt) {}

	public DreamwalkingAbility() {
		super(PowersMod.id("dreamwalking"), Component.translatable("ability.powers.dreamwalking"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		Dream current = ACTIVE.remove(player.getUUID());
		if (current != null) {
			end(player, current);
			return true;
		}
		HitResult hit = player.pick(32.0, 0.0f, false);
		if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayer host)
				|| host == player) {
			player.sendSystemMessage(Component.translatable("ability.powers.no_player_target"));
			return false;
		}
		MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
		Dream dream = new Dream(host, host.getHealth(), server.getTickCount() + DURATION);
		ACTIVE.put(player.getUUID(), dream);
		host.setHealth(Math.min(host.getHealth(), host.getMaxHealth() / 2.0f));
		player.setCamera(host);
		if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			com.powers.fx.PowerFx.beam(level, player.getEyePosition(), host.getEyePosition(),
					net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 18);
			com.powers.fx.PowerFx.sound(level, host.position(),
					net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.45f);
		}
		return true;
	}

	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer dreamer = server.getPlayerList().getPlayer(entry.getKey());
			Dream dream = entry.getValue();
			if (dreamer == null || !dreamer.isAlive() || !dream.host().isAlive() || now >= dream.endsAt()) {
				if (dreamer != null) end(dreamer, dream);
				it.remove();
			}
		}
	}

	private static void end(ServerPlayer dreamer, Dream dream) {
		dreamer.setCamera(null);
		if (dream.host().isAlive()) dream.host().setHealth(Math.min(dream.savedHealth(), dream.host().getMaxHealth()));
	}

	public static void clearAll() {
		for (Dream dream : ACTIVE.values()) {
			if (dream.host().isAlive()) dream.host().setHealth(Math.min(dream.savedHealth(), dream.host().getMaxHealth()));
		}
		ACTIVE.clear();
	}

	public static void clear(UUID player) {
		Dream dream = ACTIVE.remove(player);
		if (dream != null && dream.host().isAlive()) {
			dream.host().setHealth(Math.min(dream.savedHealth(), dream.host().getMaxHealth()));
		}
	}
}
