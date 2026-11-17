package com.powers.boss;

import com.powers.entity.FirstVessel;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

/** Bounded world observations for the First Vessel's tactical planner. */
public final class FirstVesselEncounterSnapshot {
	private FirstVesselEncounterSnapshot() { }

	/** Captures current targets, incoming projectiles and protection without retaining entities. */
	public static FirstVesselEncounterFacts capture(FirstVessel vessel, ServerLevel level,
			LivingEntity target, float healthRatio, String previousAction) {
		int cluster = 0;
		for (ServerPlayer player : level.players()) {
			if (cluster >= FirstVesselRules.MAX_CANDIDATES) break;
			if (player.isAlive() && !player.isSpectator()
					&& player.distanceToSqr(target) <= 10.0 * 10.0) cluster++;
		}
		int incoming = BoundedEntityCandidates.ofClass(level, Projectile.class,
				AABB.ofSize(vessel.position(), 24.0, 24.0, 24.0), FirstVesselRules.MAX_CANDIDATES,
				projectile -> projectile.isAlive() && projectile.getOwner() != vessel).size();
		boolean lineOfSight = vessel.getSensing().hasLineOfSight(target);
		boolean warded = PowerProtection.isSafeZone(level, target.position())
				|| SpellFieldManager.isSanctuaryProtected(level, target)
				|| AmethystDampening.isDampenedAt(level, target.blockPosition());
		return new FirstVesselEncounterFacts(target.isAlive(), vessel.distanceTo(target),
				Math.abs(target.getY() - vessel.getY()), lineOfSight, cluster, incoming,
				healthRatio, target.getDeltaMovement().horizontalDistanceSqr() > 0.04,
				warded, !lineOfSight, previousAction,
				vessel.getUUID().getLeastSignificantBits() ^ vessel.tickCount / 10L);
	}
}
