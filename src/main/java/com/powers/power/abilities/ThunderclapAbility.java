package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.ThunderclapFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.player.PlayerPowers;
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

/** Wide Viltrumite-style pressure clap that damages, stuns, repels, and clears projectiles. */
public final class ThunderclapAbility extends Ability {
	private static final double BASE_RANGE = 32.0;

	public ThunderclapAbility() {
		super(PowersMod.id("thunderclap"), Component.translatable("ability.powers.thunderclap"),
				240, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		ThunderclapRules.HorizontalDirection forward = ThunderclapRules.horizontalDirection(
				look.x, look.z, player.getYRot());
		Vec3 horizontal = new Vec3(forward.x(), 0.0, forward.z());
		double range = scaledRange(player, BASE_RANGE);
		AABB bounds = AABB.ofSize(origin, range * 2.0, range, range * 2.0);
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds, 160,
				candidate -> eligible(player, candidate, range, horizontal),
				Comparator.comparingDouble(player::distanceToSqr))) {
			ControlResistance.Outcome control = ControlResistance.evaluate(
					player, target, "thunderclap");
			boolean mayHarm = PowerProtection.mayHarm(player, target)
					&& !SpellFieldManager.isSanctuaryProtected(level, target);
			if (mayHarm) {
				target.hurtServer(level, PowerDamage.source(player), scaledPotency(player, 50.0F));
				int controlTicks = ControlResistance.adjustDuration(scaledDuration(player, 80), control);
				if (controlTicks > 0) {
					target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS,
							controlTicks, 3, false, true));
					target.addEffect(PowerStatusEffects.hidden(MobEffects.WEAKNESS,
							controlTicks, 2, false, true));
				}
			}
			if (PowerProtection.mayForceMove(player, target)
					&& !SpellFieldManager.blocksForcedMovement(level, target, player.getUUID())) {
				Vec3 push = target.position().subtract(player.position()).normalize()
						.scale(2.2 * scaling(player).potencyMultiplier());
				push = ControlResistance.adjustImpulse(push, control);
				target.setDeltaMovement(push.x, Math.max(0.45, push.y + 0.35), push.z);
				target.hurtMarked = true;
			}
		}
		for (Projectile projectile : BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), bounds, 160,
				candidate -> inCone(player, candidate.position(), range, horizontal),
				Comparator.comparingDouble(player::distanceToSqr))) {
			Vec3 push = projectile.position().subtract(player.position()).normalize().scale(4.0);
			projectile.setDeltaMovement(push);
			projectile.hurtMarked = true;
		}
		CombatTerrainImpact.thunderclap(level, player, origin, horizontal, range,
				CombatTerrainImpact.tier(player, CastScalingContext.currentSource(), "thunderclap"));
		ThunderclapFx.release(level, origin.add(horizontal.scale(2.0)), horizontal, range);
		return true;
	}

	private static boolean eligible(ServerPlayer caster, LivingEntity target,
			double range, Vec3 horizontal) {
		return target != caster && target.isAlive() && !caster.isAlliedTo(target)
				&& inCone(caster, target.position(), range, horizontal)
				&& !AmethystDampening.isDampened(target)
				&& (PowerProtection.mayHarm(caster, target)
				|| PowerProtection.mayForceMove(caster, target));
	}

	private static boolean inCone(ServerPlayer caster, Vec3 target, double range, Vec3 horizontal) {
		Vec3 offset = target.subtract(caster.position());
		return ThunderclapRules.inCone(offset.x, offset.z, horizontal.x, horizontal.z, range);
	}
}
