package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shadow Step: blink a short distance in the direction you're looking,
 * emerging on top of any solid surface. Fails with a warning if there's
 * no room to land.
 */
public class ShadowStepAbility extends Ability {
	public ShadowStepAbility() {
		super(PowersMod.id("shadow_step"),
				Component.translatable("ability.powers.shadow_step"),
				100, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		var level = (net.minecraft.server.level.ServerLevel) player.level();
		double range = scaledRange(player, 12.0);
		HitResult hit = player.pick(range, 0.0f, false);
		BlockPos target = BlockPos.containing(hit.getLocation());
		Vec3 look = player.getLookAngle();
		if (hit.getType() == HitResult.Type.MISS) {
			target = BlockPos.containing(player.getEyePosition().add(look.scale(range)));
		}

		BlockPos feet = findStandingSpot(player, level, target);
		if (feet == null) {
			// no valid landing spot, so warn the player and refund energy
			PowerMessages.send(player, "ability.powers.no_room", 3);
			return false;
		}

		Vec3 from = player.position();
		Vec3 dest = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
		com.powers.fx.PowerFx.burst(level, from, net.minecraft.core.particles.ParticleTypes.SMOKE, 14, 0.4, 0.06);
		com.powers.fx.PowerFx.burst(level, from, net.minecraft.core.particles.ParticleTypes.PORTAL, 10, 0.5, 0.1);
		player.teleport(new net.minecraft.world.level.portal.TeleportTransition(level,
				dest, Vec3.ZERO, player.getYRot(), player.getXRot(),
				net.minecraft.world.level.portal.TeleportTransition.PLAY_PORTAL_SOUND));
		com.powers.fx.PowerFx.burst(level, dest, net.minecraft.core.particles.ParticleTypes.SMOKE, 14, 0.4, 0.06);
		com.powers.fx.PowerFx.burst(level, dest, net.minecraft.core.particles.ParticleTypes.PORTAL, 10, 0.5, 0.1);
		com.powers.fx.PowerFx.sound(level, dest, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
		if (scaling(player).unlockedVariants().contains("second_step")) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.SPEED, 30, 1, false, false));
			com.powers.fx.PowerFx.rune(level, from, 1.0, 0x55265F, 18, 0.0);
			com.powers.fx.PowerFx.rune(level, dest, 1.0, 0xD7F8FF, 18, Math.PI);
		}
		return true;
	}

	private BlockPos findStandingSpot(ServerPlayer player, net.minecraft.server.level.ServerLevel level, BlockPos start) {
		BlockPos pos = start;
		if (!level.getBlockState(pos).isAir()) {
			pos = pos.above();
		}
		// walk down looking for a spot with open air and solid ground, up to 8 blocks deep
		for (int i = 0; i < 8; i++) {
			if (level.getBlockState(pos).isAir()
					&& level.getBlockState(pos.below()).entityCanStandOn(level, pos.below(), player)) {
				Vec3 destination = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				if (SafeDestinationResolver.validate(player, level, destination, TravelKind.POWER).allowed()) {
					return pos;
				}
				return null;
			}
			if (!level.getBlockState(pos).isAir()) {
				// a solid block overhead means no room to stand, give up
				return null;
			}
			pos = pos.below();
		}
		return null;
	}
}
