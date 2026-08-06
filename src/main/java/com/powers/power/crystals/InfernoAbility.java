package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Inferno: the Red Crystal's power. For eight seconds the world around you
 * becomes a firestorm - blazing meteors rain down, everything within ten
 * blocks is set ablaze, and no one inside escapes the flames.
 */
public class InfernoAbility extends Ability {
	private static final int DURATION_TICKS = 160;
	private static final int COOLDOWN_TICKS = 1800;
	private static final int RADIUS = 12;

	private static final Map<ServerPlayer, Integer> ACTIVE = new HashMap<>();

	public InfernoAbility() {
		super(PowersMod.id("inferno"),
				Component.translatable("ability.powers.inferno"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (ACTIVE.containsKey(player)) {
			return false;
		}
		ACTIVE.put(player, DURATION_TICKS);
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFF3D00, 30, 1.5);
		PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.FLAME, 40, 1.2, 0.4);
		PowerFx.sound(level, player.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
		return true;
	}

	/** Called every server tick while any Inferno is active. */
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
			if (left % 8 == 0) {
				Vec3 origin = player.position().add(0, 1.2, 0);
				for (int i = 0; i < 6; i++) {
					SmallFireball fireball = EntityTypes.SMALL_FIREBALL.create(level, EntitySpawnReason.TRIGGERED);
					if (fireball != null) {
						fireball.setPos(origin.x + (level.getRandom().nextDouble() - 0.5) * 2 * RADIUS,
								origin.y + level.getRandom().nextDouble() * 6,
								origin.z + (level.getRandom().nextDouble() - 0.5) * 2 * RADIUS);
						fireball.setDeltaMovement(new Vec3(
								(level.getRandom().nextDouble() - 0.5) * 0.5,
								-0.25 - level.getRandom().nextDouble() * 0.3,
								(level.getRandom().nextDouble() - 0.5) * 0.5));
						level.addFreshEntity(fireball);
					}
				}
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
						AABB.ofSize(origin, RADIUS * 2, 8, RADIUS * 2),
						e -> e.isAlive() && e != player)) {
					target.igniteForSeconds(8);
				}
				PowerFx.burst(level, origin, ParticleTypes.FLAME, 20, 2.5, 0.2);
			}

			if (--left <= 0) {
				it.remove();
				PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.SMOKE, 24, 1.2, 0.1);
			} else {
				entry.setValue(left);
			}
		}
	}
}
