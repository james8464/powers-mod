package com.powers.entity;

import com.powers.PowerStatusEffects;
import com.powers.ai.PerceptionQueryProfile;
import com.powers.ai.PerceptionSnapshotService;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Applies the guardian's fourth, alignment-specific tactical action. */
public final class GuardianAlignmentField {
	private static final int MAX_TARGETS = 24;

	private GuardianAlignmentField() {
	}

	public static void pulse(ServerLevel level, AbstractPlayerLikeMob guardian,
			ArtifactAlignment alignment) {
		double radius = guardian.eliteGuardian() ? 9.0 : 6.0;
		for (var observation : PerceptionSnapshotService.observe(level, guardian.position(),
				radius, radius, MAX_TARGETS,
				candidate -> !candidate.entityId().equals(guardian.getUUID())
						&& candidate.position().distanceToSqr(guardian.position()) <= radius * radius,
				PerceptionQueryProfile.GUARDIAN_FIELD)) {
			LivingEntity target = PerceptionSnapshotService.resolve(level, observation);
			if (target == null || target.distanceToSqr(guardian) > radius * radius) continue;
			boolean targetDark = target.entityTags().contains(SkillSystem.DARKNESS_TAG);
			if (!GuardianFieldRules.hostile(alignment, targetDark)) {
				if (alignment == ArtifactAlignment.LIGHT) target.heal(guardian.eliteGuardian() ? 8.0F : 4.0F);
				continue;
			}
			if (PowerProtection.isSafeZone(level, target.position())
					|| AmethystDampening.isDampened(target)
					|| SpellFieldManager.isSanctuaryProtected(level, target)) continue;
			target.hurtServer(level, PowerDamage.source(guardian), guardian.eliteGuardian() ? 26.0F : 14.0F);
			target.addEffect(PowerStatusEffects.hidden(alignment == ArtifactAlignment.DARKNESS
					? MobEffects.WITHER : MobEffects.WEAKNESS, 80,
					guardian.eliteGuardian() ? 2 : 1, false, true));
		}
		int color = alignment == ArtifactAlignment.DARKNESS ? 0x3B104D : 0xFFE8A3;
		PowerFx.rune(level, guardian.position(), radius, color,
				guardian.eliteGuardian() ? 36 : 24, level.getGameTime() * 0.08);
		PowerFx.burst(level, guardian.position().add(0, 1, 0),
				alignment == ArtifactAlignment.DARKNESS ? com.powers.PowersParticles.ECLIPSE : com.powers.PowersParticles.GLYPH,
				guardian.eliteGuardian() ? 12 : 7, 0.8, 0.04);
	}
}
