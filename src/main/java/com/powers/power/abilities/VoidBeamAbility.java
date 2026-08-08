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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * void beam - a beam of corrupted darkness along your sight line that burns
 * the first living target it hits and withers it, the signature attack of
 * void steve
 */
public class VoidBeamAbility extends Ability {
	public VoidBeamAbility() {
		super(PowersMod.id("void_beam"),
				Component.translatable("ability.powers.void_beam"),
				120, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		// the target search reach, scaled by the player's skills
		LivingEntity target = PowerTargeting.findLivingTarget(player,
				PowerScalingService.range(player, "void_beam", 32.0));
		if (target == null) {
			// nothing in sight - the caller refunds the energy
			return false;
		}
		// amethyst-dampened targets are protected
		if (AmethystDampening.isDampened(target)) return false;

		com.powers.fx.PowerFx.beam(level, player.getEyePosition(),
				target.position().add(0, target.getBbHeight() / 2, 0),
				net.minecraft.core.particles.ColorParticleOption.create(
						net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0xFF1A237E), 14);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.8f, 1.3f);

		// 6 magic damage (scaled), plus a level 2 wither that lasts 5 seconds (100 ticks)
		target.hurtServer(level, PowerDamage.source(player),
				PowerScalingService.damage(player, "void_beam", 6.0f));
		target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, true, false));
		return true;
	}
}
