package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ground Slam: a Hulk-style ground pound. The impact detonates the ground
 * beneath the caster, carving a crater and hurling everything nearby.
 */
public class GroundSlamAbility extends Ability {
	public GroundSlamAbility() {
		super(PowersMod.id("ground_slam"),
				Component.translatable("ability.powers.ground_slam"),
				200, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.burst(level, player.position().add(0, 0.5, 0), net.minecraft.core.particles.ParticleTypes.EXPLOSION, 28, 1.5, 0.2);
		PowerFx.ring(level, player.position().add(0, 0.1, 0), 3.5, 0xFF8A00, 28, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.8f);
		level.explode(player, player.getX(), player.getY() - 0.5, player.getZ(),
			2.0f, false, Level.ExplosionInteraction.BLOCK);

		DamageSource source = player.damageSources().mobAttack(player);
		AABB area = AABB.ofSize(player.position(), 10.0, 6.0, 10.0);
		for (LivingEntity target : level.getEntities(
			EntityTypeTest.forClass(LivingEntity.class), area,
				e -> e.isAlive() && e != player)) {
			target.hurtServer(level, source, SkillSystem.damage(player, 6.0f));
			Vec3 away = target.position().subtract(player.position()).normalize();
			target.knockback(1.6, away.x, away.z, source, 1.0f);
			PowerFx.burst(level, target.position().add(0, target.getBbHeight() * 0.5, 0),
					net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, 6, 0.5, 0.02);
		}
		return true;
	}
}
