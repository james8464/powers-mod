package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForcefieldAbility extends Ability {
	private static final int DURATION = 160;
	private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

	public ForcefieldAbility() {
		super(PowersMod.id("forcefield"),
				Component.translatable("ability.powers.forcefield"),
				500, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ACTIVE.put(player.getUUID(), DURATION);
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION, 9, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, DURATION, 4, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION, 0, false, false));

		com.powers.fx.PowerFx.sound((net.minecraft.server.level.ServerLevel) player.level(),
				player.position(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.8f, 0.5f);
		return true;
	}

	public static boolean protects(net.minecraft.world.entity.LivingEntity entity) {
		return ACTIVE.containsKey(entity.getUUID());
	}

	public static void tickAll(MinecraftServer server) {
		for (var it = ACTIVE.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) player.level();
			if (entry.getValue() % 10 == 0) {
				double phase = entry.getValue() * 0.04;
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 0.15, 0), 1.5, 0x40C4FF, 20, phase);
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 1.0, 0), 1.25, 0x40C4FF, 20, -phase);
				com.powers.fx.PowerFx.ring(level, player.position().add(0, 1.85, 0), 1.5, 0x40C4FF, 20, phase);
				com.powers.fx.PowerFx.spiral(level, player.position().add(0, 0.1, 0), 1.4, 1.8, 0x40C4FF, 12, phase);
			}
			int remaining = entry.getValue() - 5;
			if (remaining <= 0) it.remove();
			else entry.setValue(remaining);
		}
	}

	public static void clear(UUID player) {
		ACTIVE.remove(player);
	}

	public static void clearAll() {
		ACTIVE.clear();
	}
}
