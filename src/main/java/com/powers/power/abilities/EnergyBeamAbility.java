package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.player.SkillSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EnergyBeamAbility extends Ability {
	public EnergyBeamAbility() {
		super(PowersMod.id("energy_beam"),
				Component.translatable("ability.powers.energy_beam"),
				80, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();

		double range = SkillSystem.range(player, 48.0);
		HitResult hit = player.pick(range, 0.0f, true);
		Vec3 end;
		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getLocation();
			if (hit instanceof EntityHitResult entHit && entHit.getEntity() instanceof LivingEntity target) {
				if (AmethystDampening.isDampened(target)) return false;
				target.hurtServer(level, player.damageSources().magic(), SkillSystem.damage(player, 10.0f));
				target.setRemainingFireTicks(60);
			}
		} else {
			end = origin.add(look.scale(range));
		}

		com.powers.fx.PowerFx.beam(level, origin, end,
				net.minecraft.core.particles.ColorParticleOption.create(
						net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0xFFFF4500), 20);
		com.powers.fx.PowerFx.sound(level, origin,
				SoundEvents.BEACON_ACTIVATE, 1.0f, 1.5f);
		return true;
	}
}
