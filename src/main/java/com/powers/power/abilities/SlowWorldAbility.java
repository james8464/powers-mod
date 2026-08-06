package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
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

/**
 * Slow World: Time Steve's mastery over time without the world-freeze of the
 * true Chrono Stop (which now belongs to the Blue Crystal alone). For five
 * seconds the world crawls around him while he moves at full speed.
 */
public class SlowWorldAbility extends Ability {
	private static final int DURATION_TICKS = 100;
	private static final int COOLDOWN_TICKS = 1200;
	private static final int RADIUS = 10;

	private static final Map<ServerPlayer, Integer> ACTIVE = new HashMap<>();

	public SlowWorldAbility() {
		super(PowersMod.id("slow_world"),
				Component.translatable("ability.powers.slow_world"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (ACTIVE.containsKey(player)) {
			return false;
		}
		ACTIVE.put(player, DURATION_TICKS);
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION_TICKS, 2, true, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION_TICKS, 1, true, false));
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x26C6DA, 18, 1.0);
		PowerFx.sound(level, player.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.3f);
		return true;
	}

	/** Called every server tick while any slow world is active. */
	public static void tickAll() {
		Iterator<Map.Entry<ServerPlayer, Integer>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ServerPlayer, Integer> entry = it.next();
			ServerPlayer player = entry.getKey();
			int left = entry.getValue();

			if (!player.isAlive()) {
				it.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) player.level();
			if (left % 20 == 0) {
				PowerFx.ring(level, player.position(), RADIUS, 0x26C6DA, 24, left * 0.04);
				PowerFx.burst(level, player.position().add(0, 1, 0),
						net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 8, 0.8, 0.01);
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
						AABB.ofSize(player.position().add(0, 1, 0), RADIUS * 2, RADIUS * 2, RADIUS * 2),
						e -> e.isAlive() && e != player)) {
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4, false, false));
				}
			}

			if (--left <= 0) {
				it.remove();
			} else {
				entry.setValue(left);
			}
		}
	}
}
