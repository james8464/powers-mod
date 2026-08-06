package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
		Vec3 end = origin.add(look.scale(32.0));

		HitResult hit = player.pick(32.0, 0.0f, true);
		if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();

		if (hit instanceof EntityHitResult entHit && entHit.getEntity() instanceof LivingEntity target) {
			if (AmethystDampening.isDampened(target)) return false;
			target.hurtServer(level, player.damageSources().freeze(), 8.0f);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 4, false, false));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, false));
			target.setTicksFrozen(160);
		}

		// Freeze blocks along the beam path
		Vec3 dir = end.subtract(origin).normalize();
		double dist = origin.distanceTo(end);
		for (double d = 0; d < dist; d += 0.5) {
			Vec3 point = origin.add(dir.scale(d));
			BlockPos pos = new BlockPos((int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.WATER)) {
				level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
			} else if (state.is(Blocks.LAVA)) {
				level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
			} else if (state.isAir() && d > 1.0) {
				level.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
			} else if (!state.isAir() && !state.is(Blocks.BEDROCK) && d < dist - 1.0) {
				// Freeze the block itself if it's solid ground — coat with ice layer
				BlockPos above = pos.above();
				if (level.getBlockState(above).isAir()) {
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
