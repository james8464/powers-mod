package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Slow World: for five seconds the world crawls around you while you move
 * at full speed. Nearby enemies get heavily slowed, and it all happens
 * server-side so it keeps running after you activate it.
 */
public class SlowWorldAbility extends Ability {
	// 5 seconds of world slowing, then a full minute before you can use it again
	private static final int DURATION_TICKS = 100;
	private static final int COOLDOWN_TICKS = 1200;
	private static final int RADIUS = 10;

	// tracks remaining ticks per player so tickAll can keep the effect alive
	private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

	public SlowWorldAbility() {
		super(PowersMod.id("slow_world"),
				Component.translatable("ability.powers.slow_world"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (ACTIVE.containsKey(player.getUUID())) {
			// already slowing the world, so this cast fails and energy is refunded
			return false;
		}
		ACTIVE.put(player.getUUID(), DURATION_TICKS);
		// you feel fast while the world around you is slowed
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION_TICKS, 2, true, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION_TICKS, 1, true, false));
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x26C6DA, 18, 1.0);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.3f);
		return true;
	}

	/** Runs on every server tick while any slow world is active. */
	public static void tickAll(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			int left = entry.getValue();

			if (player == null || !player.isAlive()) {
				// player left the server or died, so drop them from the map
				it.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			// once per second, pulse the ring and re-slow everyone nearby
			if (left % 20 == 0) {
				PowerFx.ring(level, player.position(), RADIUS, 0x26C6DA, 24, left * 0.04);
				PowerFx.burst(level, player.position().add(0, 1, 0),
						net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 8, 0.8, 0.01);
				// heavy slow for 3 seconds so it lingers between pulses
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
						AABB.ofSize(player.position().add(0, 1, 0), RADIUS * 2, RADIUS * 2, RADIUS * 2),
						e -> e.isAlive() && e != player)) {
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4, false, false));
				}
			}

			// count the effect down and remove it when time runs out
			if (--left <= 0) {
				it.remove();
			} else {
				entry.setValue(left);
			}
		}
	}

	public static void clear(UUID player) {
		ACTIVE.remove(player);
	}

	public static void clearAll() {
		ACTIVE.clear();
	}
}
