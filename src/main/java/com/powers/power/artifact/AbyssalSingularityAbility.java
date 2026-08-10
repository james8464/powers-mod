package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.ShadowSwordFx;
import com.powers.item.ShadowSwordPowerRules;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Rank-three sword rite that collapses enemies and projectiles into an imploding void. */
public final class AbyssalSingularityAbility extends Ability {
	private static final int COOLDOWN_TICKS = 900;

	public AbyssalSingularityAbility() {
		super(PowersMod.id("abyssal_singularity"),
				Component.translatable("ability.powers.abyssal_singularity"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(18.0));
		AABB bounds = AABB.ofSize(center, ShadowSwordPowerRules.SINGULARITY_RADIUS * 2.0,
				ShadowSwordPowerRules.SINGULARITY_RADIUS * 2.0,
				ShadowSwordPowerRules.SINGULARITY_RADIUS * 2.0);
		var victims = BoundedEntityCandidates.living(level, bounds, 192,
				target -> eligible(player, target, center),
				Comparator.comparingDouble(target -> target.distanceToSqr(center)));
		var projectiles = BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), bounds, 192,
				projectile -> projectile.distanceToSqr(center)
						<= ShadowSwordPowerRules.SINGULARITY_RADIUS * ShadowSwordPowerRules.SINGULARITY_RADIUS,
				Comparator.comparingDouble(projectile -> projectile.distanceToSqr(center)));
		if (victims.isEmpty() && projectiles.isEmpty()) return false;

		for (LivingEntity target : victims) {
			Vec3 pull = center.subtract(target.position()).normalize().scale(2.4);
			target.setDeltaMovement(pull.x, Math.max(0.35, pull.y), pull.z);
			target.hurtMarked = true;
			target.addEffect(PowerStatusEffects.hidden(MobEffects.WITHER, 80, 2, false, true));
		}
		projectiles.forEach(Projectile::discard);
		ShadowSwordFx.singularity(level, center, false);
		PowersMod.scheduleDelayed(level.getServer(), 20,
				() -> detonate(player, level, center, bounds));
		return true;
	}

	private static void detonate(ServerPlayer player, ServerLevel level, Vec3 center, AABB bounds) {
		if (player.isRemoved() || !player.isAlive()) return;
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds, 192,
				candidate -> eligible(player, candidate, center),
				Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)))) {
			target.hurtServer(level, PowerDamage.source(player),
					ShadowSwordPowerRules.singularityDamage(target.getMaxHealth()));
		}
		ShadowSwordFx.singularity(level, center, true);
	}

	private static boolean eligible(ServerPlayer caster, LivingEntity target, Vec3 center) {
		return target != caster && target.isAlive()
				&& !target.entityTags().contains(SkillSystem.DARKNESS_TAG)
				&& target.distanceToSqr(center) <= ShadowSwordPowerRules.SINGULARITY_RADIUS
						* ShadowSwordPowerRules.SINGULARITY_RADIUS
				&& !AmethystDampening.isDampened(target)
				&& PowerProtection.mayForceMove(caster, target)
				&& PowerProtection.mayHarm(caster, target)
				&& !SpellFieldManager.blocksForcedMovement(
						(ServerLevel) target.level(), target, caster.getUUID())
				&& !SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target);
	}
}
