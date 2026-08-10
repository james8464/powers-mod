package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.PowersEffects;
import com.powers.PowersMod;
import com.powers.fx.ShadowSwordFx;
import com.powers.item.ShadowSwordPowerRules;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/** Rank-five nullification wave that consumes projectiles, boons, and enemy energy. */
public final class OblivionPulseAbility extends Ability {
	private static final double RADIUS = 32.0;

	public OblivionPulseAbility() {
		super(PowersMod.id("oblivion_pulse"),
				Component.translatable("ability.powers.oblivion_pulse"), 800, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		AABB bounds = AABB.ofSize(player.position(), RADIUS * 2.0, RADIUS * 2.0, RADIUS * 2.0);
		List<LivingEntity> victims = BoundedEntityCandidates.living(level, bounds, 192,
				target -> eligible(player, target), Comparator.comparingDouble(player::distanceToSqr));
		List<Projectile> projectiles = BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), bounds, 192,
				projectile -> projectile.distanceToSqr(player) <= RADIUS * RADIUS,
				Comparator.comparingDouble(player::distanceToSqr));
		if (victims.isEmpty() && projectiles.isEmpty()) return false;

		projectiles.forEach(Projectile::discard);
		for (LivingEntity target : victims) {
			for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
				if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
					target.removeEffect(effect.getEffect());
				}
			}
			target.addEffect(PowerStatusEffects.hidden(PowersEffects.EXHAUSTION, 400, 0, false, true));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 160, 3, false, true));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.DARKNESS, 160, 0, false, true));
			target.hurtServer(level, PowerDamage.source(player),
					ShadowSwordPowerRules.oblivionDamage(target.getMaxHealth()));
			if (target instanceof ServerPlayer targetPlayer) {
				PlayerPowers.get(targetPlayer).emptyEnergy();
				PowersPackets.syncTo(targetPlayer);
			}
		}
		ShadowSwordFx.oblivionPulse(level, player.position(), !projectiles.isEmpty());
		return true;
	}

	private static boolean eligible(ServerPlayer caster, LivingEntity target) {
		return target != caster && target.isAlive() && caster.distanceToSqr(target) <= RADIUS * RADIUS
				&& !target.entityTags().contains(SkillSystem.DARKNESS_TAG)
				&& !AmethystDampening.isDampened(target)
				&& PowerProtection.mayHarm(caster, target)
				&& !SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target);
	}
}
