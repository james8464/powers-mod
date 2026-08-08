package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.progression.PowerScalingService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Raycasts a visible energy beam and scorches its first valid living target. */
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

		// skill ranks stretch the beam out beyond the base 48 blocks
		double range = PowerScalingService.range(player, "energy_beam", 48.0);
		HitResult hit = PowerTargeting.raycast(player, range);
		Vec3 end;
		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getLocation();
			if (hit instanceof net.minecraft.world.phys.EntityHitResult entHit
					&& entHit.getEntity() instanceof LivingEntity target) {
				// shielded targets block the cast entirely, refunding the energy
				if (AmethystDampening.isDampened(target)) return false;
				target.hurtServer(level, PowerDamage.source(player),
						PowerScalingService.damage(player, "energy_beam", 10.0f));
				// 3 seconds of burn
				target.setRemainingFireTicks(60);
			}
		} else {
			// nothing hit, the beam still travels the full range visually
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
