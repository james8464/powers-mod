package com.powers.power.crystals;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Life Bloom: the Green Crystal's power. Life itself answers your call:
 * every player within twenty blocks is healed to full health, freed of
 * every ailment, and wreathed in regeneration so potent it outlasts
 * any fight
 */
public class LifeBloomAbility extends Ability {
	private static final int COOLDOWN_TICKS = 2400;
	private static final int RADIUS = 20;

	public LifeBloomAbility() {
		super(PowersMod.id("life_bloom"),
				Component.translatable("ability.powers.life_bloom"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.position().add(0, 1, 0);
		double radius = scaledRange(player, RADIUS);
		int blessingDuration = scaledDuration(player, 600);
		// the box reaches 20 blocks out from the caster in every direction
		for (LivingEntity ally : BoundedEntityCandidates.living(level,
				AABB.ofSize(origin, radius * 2, radius * 2, radius * 2),
				256,
				e -> e.isAlive() && CrystalTargeting.withinRadius(e.distanceToSqr(player), radius))) {
			if (ally instanceof ServerPlayer orPlayer) {
				// Cleanse ailments without deleting beneficial effects owned by
				// potions, beacons, other mods, or an active POWERS toggle.
				for (MobEffectInstance effect : java.util.List.copyOf(orPlayer.getActiveEffects())) {
					if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
						orPlayer.removeEffect(effect.getEffect());
					}
				}
				orPlayer.heal(orPlayer.getMaxHealth());
				// 600 ticks = 30 seconds of regen, absorption and saturation, enough to carry anyone through a fight
				orPlayer.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION,
						blessingDuration, 4, true, true));
				orPlayer.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION,
						blessingDuration, 3, true, true));
				orPlayer.addEffect(PowerStatusEffects.hidden(MobEffects.SATURATION,
						blessingDuration, 0, true, true));
				PowerFx.coloredBurst(level, orPlayer.position().add(0, 1, 0), 0x00C853, 12, 0.6);
			}
		}
		PowerFx.coloredBurst(level, origin, 0x00C853, 32, 2.0);
		PowerFx.burst(level, origin, PowerFx.dust(0xD8FF8A, 1.25F), 18, 1.6, 0.0);
		PowerFx.rune(level, origin, radius * 0.65, 0x78E06B, 36, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.TOTEM_USE, 1.0f, 0.8f);
		return true;
	}
}
