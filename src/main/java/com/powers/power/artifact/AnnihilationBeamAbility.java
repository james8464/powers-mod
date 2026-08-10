package com.powers.power.artifact;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Rank-seven sword beam that cuts a boss-scale corridor through hostile life and projectiles. */
public final class AnnihilationBeamAbility extends Ability {
	private static final int COOLDOWN_TICKS = 1200;

	public AnnihilationBeamAbility() {
		super(PowersMod.id("annihilation_beam"),
				Component.translatable("ability.powers.annihilation_beam"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getLookAngle().scale(ShadowSwordPowerRules.BEAM_RANGE));
		AABB corridor = new AABB(from, to).inflate(5.0);
		var victims = BoundedEntityCandidates.living(level, corridor, 192,
				target -> eligible(player, target, from, to),
				Comparator.comparingDouble(target -> target.distanceToSqr(from)));
		var projectiles = BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), corridor, 128,
				projectile -> distanceToSegmentSqr(projectile.position(), from, to) <= 25.0,
				Comparator.comparingDouble(projectile -> projectile.distanceToSqr(from)));
		if (victims.isEmpty() && projectiles.isEmpty()) return false;
		victims.forEach(target -> target.hurtServer(level, PowerDamage.source(player),
				ShadowSwordPowerRules.annihilationDamage(target.getMaxHealth())));
		projectiles.forEach(Projectile::discard);
		ShadowSwordFx.annihilationBeam(level, from, to);
		return true;
	}

	private static boolean eligible(ServerPlayer caster, LivingEntity target, Vec3 from, Vec3 to) {
		return target != caster && target.isAlive()
				&& !target.entityTags().contains(SkillSystem.DARKNESS_TAG)
				&& distanceToSegmentSqr(target.getEyePosition(), from, to) <= 25.0
				&& !AmethystDampening.isDampened(target)
				&& PowerProtection.mayHarm(caster, target)
				&& !SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target);
	}

	private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSquared = segment.lengthSqr();
		if (lengthSquared <= 1.0E-8) return point.distanceToSqr(start);
		double projection = Math.clamp(point.subtract(start).dot(segment) / lengthSquared, 0.0, 1.0);
		return point.distanceToSqr(start.add(segment.scale(projection)));
	}
}
