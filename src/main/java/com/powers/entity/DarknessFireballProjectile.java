package com.powers.entity;

import com.powers.fx.PowerFx;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Unsaved, terrain-safe fireball used by player-shaped darkness combatants. */
final class DarknessFireballProjectile extends LargeFireball {
	private static final double RADIUS = 5.0;
	private final boolean radiant;

	DarknessFireballProjectile(ServerLevel level, LivingEntity owner, Vec3 direction) {
		this(level, owner, direction, false);
	}

	DarknessFireballProjectile(ServerLevel level, LivingEntity owner, Vec3 direction, boolean radiant) {
		super(level, owner, direction, 0);
		this.radiant = radiant;
		PowerEntityState.markEphemeral(this);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		// The bounded detonation below owns all damage; suppress vanilla's extra six points.
	}

	@Override
	protected void onHit(HitResult hit) {
		if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof LivingEntity owner)) {
			discard();
			return;
		}
		Vec3 center = hit.getLocation();
		AABB bounds = AABB.ofSize(center, RADIUS * 2.0, RADIUS * 2.0, RADIUS * 2.0);
		for (LivingEntity target : BoundedEntityCandidates.living(level, bounds, 48,
				candidate -> candidate != owner && candidate.isAlive()
						&& (radiant == candidate.entityTags().contains(SkillSystem.DARKNESS_TAG))
						&& candidate.distanceToSqr(center) <= RADIUS * RADIUS
						&& !AmethystDampening.isDampened(candidate)
						&& !PowerProtection.isSafeZone(level, candidate.position())
						&& !SpellFieldManager.isSanctuaryProtected(level, candidate),
				Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)))) {
			double falloff = 1.0 - Math.sqrt(target.distanceToSqr(center)) / RADIUS;
			if (target.hurtServer(level, PowerDamage.projectileSource(owner, this),
					(float) (18.0 + 22.0 * Math.max(0.0, falloff)))) {
				if (radiant) target.setRemainingFireTicks(0);
				else target.igniteForSeconds(8.0F);
			}
		}
		PowerFx.burst(level, center, radiant ? ParticleTypes.END_ROD
				: ParticleTypes.SOUL_FIRE_FLAME, 28, 1.5, 0.12);
		PowerFx.rune(level, center, RADIUS, radiant ? 0xFFE89B : 0x51113F,
				28, level.getGameTime() * 0.08);
		PowerFx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.5F,
				radiant ? 1.4F : 0.55F);
		discard();
	}
}
