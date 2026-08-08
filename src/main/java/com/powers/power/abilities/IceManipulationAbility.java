package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.progression.PowerScalingService;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ice Manipulation: a freezing beam that hurts whatever you hit and coats
 * the ground in ice. Freezes water into ice, lava into obsidian, and adds
 * snow on the way past.
 */
public class IceManipulationAbility extends Ability {
	public IceManipulationAbility() {
		super(PowersMod.id("ice_manipulation"),
				Component.translatable("ability.powers.ice_manipulation"),
				100, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		double range = PowerScalingService.range(player, "ice_manipulation", 32.0);
		Vec3 end = origin.add(look.scale(range));
		HitResult hit = PowerTargeting.raycast(player, range);
		if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();

		if (hit instanceof net.minecraft.world.phys.EntityHitResult entHit
				&& entHit.getEntity() instanceof LivingEntity target) {
			// looking at a protected entity means the cast fails and energy is refunded
			if (AmethystDampening.isDampened(target)) return false;
			// 8 damage that scales with skill, a heavy slow, weakness and a deep freeze
			target.hurtServer(level, PowerDamage.source(player),
					PowerScalingService.damage(player, "ice_manipulation", 8.0f));
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 4, false, false));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
			target.setTicksFrozen(160);
		}

		// walk the beam in half-block steps and freeze blocks along the path
		Vec3 dir = end.subtract(origin).normalize();
		double dist = origin.distanceTo(end);
		for (double d = 0; d < dist; d += 0.5) {
			Vec3 point = origin.add(dir.scale(d));
			BlockPos pos = new BlockPos((int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.WATER) && PowerProtection.mayAffectBlock(player, level, pos)) {
				level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
			} else if (state.is(Blocks.LAVA) && PowerProtection.mayAffectBlock(player, level, pos)) {
				level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
			} else if (!state.isAir() && !state.is(Blocks.BEDROCK) && d < dist - 1.0) {
				// coat solid ground with snow near the beam's end, only where the space above is free
				BlockPos above = pos.above();
				if (level.getBlockState(above).isAir() && PowerProtection.mayAffectBlock(player, level, above)) {
					level.setBlockAndUpdate(above, Blocks.SNOW.defaultBlockState());
				}
			}
		}

		com.powers.fx.PowerFx.beam(level, origin, end,
				net.minecraft.core.particles.ColorParticleOption.create(
						net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0xFF81D4FA), 28);
		com.powers.fx.PowerFx.sound(level, origin, SoundEvents.PLAYER_HURT_FREEZE, 1.2f, 0.9f);
		return true;
	}
}
