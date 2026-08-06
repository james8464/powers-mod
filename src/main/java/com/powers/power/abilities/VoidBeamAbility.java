package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.player.SkillSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Void Beam: a beam of corrupted Darkness along your sight line. Damages the
 * first target hit and withers it. Inspired by Void Steve, the
 * Darkness-corrupted overlord of Rainbow Quest.
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
		HitResult hit = player.pick(SkillSystem.range(player, 32.0), 0.0f, false);
		if (hit.getType() != HitResult.Type.ENTITY) {
			return false;
		}

		EntityHitResult entityHit = (EntityHitResult) hit;
		if (!(entityHit.getEntity() instanceof LivingEntity target)) {
			return false;
		}
		if (AmethystDampening.isDampened(target)) return false;

		com.powers.fx.PowerFx.beam(level, player.getEyePosition(),
				target.position().add(0, target.getBbHeight() / 2, 0),
				net.minecraft.core.particles.ColorParticleOption.create(
						net.minecraft.core.particles.ParticleTypes.ENTITY_EFFECT, 0xFF1A237E), 14);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 0.8f, 1.3f);

		target.hurtServer(level, player.damageSources().magic(), SkillSystem.damage(player, 6.0f));
		target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, true, false));
		return true;
	}
}
