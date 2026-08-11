package com.powers.item;

import com.powers.PowerStatusEffects;
import com.powers.PowersParticles;
import com.powers.fx.PowerFx;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** A finite on-hit proc gives every imported weapon family an actual combat identity. */
public final class FantasyWeaponItem extends Item {
	private final FantasyWeaponArchetype archetype;

	public FantasyWeaponItem(FantasyWeaponArchetype archetype, Properties properties) {
		super(properties);
		this.archetype = archetype;
	}

	public FantasyWeaponArchetype archetype() {
		return archetype;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (!(attacker instanceof ServerPlayer player) || !(target.level() instanceof ServerLevel level)
				|| player.getCooldowns().isOnCooldown(stack)
				|| AmethystDampening.isDampened(player) || AmethystDampening.isDampened(target)
				|| PowerProtection.isSafeZone(level, target.position())) return;
		player.getCooldowns().addCooldown(stack, archetype.procCooldownTicks());
		apply(level, player, target);
		Vec3 impact = target.getEyePosition();
		PowerFx.rune(level, target.position().add(0.0, 0.08, 0.0), 0.9,
				archetype.color(), 14, level.getGameTime() * 0.08);
		PowerFx.burst(level, impact, PowersParticles.FRACTURE, 6, 0.35, 0.08);
		PowerFx.coloredBurst(level, impact, archetype.color(), 6, 0.35);
		PowerFx.sound(level, impact, SoundEvents.ENCHANTMENT_TABLE_USE, 0.38F,
				0.72F + archetype.ordinal() * 0.045F);
	}

	private void apply(ServerLevel level, ServerPlayer player, LivingEntity target) {
		switch (archetype) {
			case FROST -> {
				target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, 80, 2, true, true));
				target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, 80, 1, true, true));
			}
			case SWIFT -> player.addEffect(PowerStatusEffects.hidden(MobEffects.SPEED, 60, 2, true, true));
			case REAPER -> target.hurtServer(level, PowerDamage.source(player),
					Math.max(6.0F, (target.getMaxHealth() - target.getHealth()) * 0.12F));
			case CRUSHER -> {
				Vec3 away = target.position().subtract(player.position());
				if (away.lengthSqr() > 1.0E-4) target.push(away.normalize().scale(1.8));
				target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS, 100, 2, true, true));
			}
			case BERSERKER -> {
				target.hurtServer(level, PowerDamage.source(player), 10.0F);
				player.addEffect(PowerStatusEffects.hidden(MobEffects.STRENGTH, 80, 1, true, true));
			}
			case ARCANE -> {
				PlayerPowers.get(player).refundEnergy(30);
				PowersPackets.syncTo(player);
				target.hurtServer(level, PowerDamage.source(player), 6.0F);
			}
			case VITAL -> {
				player.heal(6.0F);
				player.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION, 80, 1, true, true));
			}
			case RADIANT -> {
				target.igniteForSeconds(4.0F);
				target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 100, 0, true, true));
				target.hurtServer(level, PowerDamage.source(player), 8.0F);
			}
			case ABYSSAL -> {
				target.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 80, 1, true, true));
				player.heal(4.0F);
			}
			case GUARDIAN -> {
				player.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 120, 2, true, true));
				player.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 80, 0, true, true));
			}
			case HUNTER -> {
				target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 120, 0, true, true));
				target.addEffect(PowerStatusEffects.hidden(MobEffects.POISON, 80, 1, true, true));
			}
			case PIERCER -> target.hurtServer(level, target.damageSources().magic(), 12.0F);
		}
	}
}
