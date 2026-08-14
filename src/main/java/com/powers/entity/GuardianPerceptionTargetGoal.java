package com.powers.entity;

import com.powers.ai.PerceptionQueryProfile;
import com.powers.ai.PerceptionSnapshotService;
import com.powers.item.artifact.ArtifactAlignment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Deterministic target acquisition backed by the shared immutable AI snapshot. */
final class GuardianPerceptionTargetGoal extends Goal {
	private static final int INTERVAL = 5;
	private static final int MAX_CANDIDATES = 64;
	private static final int UNSEEN_MEMORY_TICKS = 60;
	private final AbstractPlayerLikeMob guardian;
	private LivingEntity candidate;
	private int unseenTicks;

	GuardianPerceptionTargetGoal(AbstractPlayerLikeMob guardian) {
		this.guardian = guardian;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (!(guardian.level() instanceof ServerLevel level)) return false;
		double range = Math.min(48.0, guardian.getAttributeValue(Attributes.FOLLOW_RANGE));
		ArtifactAlignment alignment = guardian.radiantCombat()
				? ArtifactAlignment.LIGHT : ArtifactAlignment.DARKNESS;
		LivingEntity current = guardian.getTarget();
		if (current != null && (!current.isAlive() || !guardian.canAttack(current)
				|| guardian.distanceToSqr(current) > range * range
				|| !GuardianFactionRules.mayTarget(alignment, guardian.guardianOwner(),
				current.getUUID(), current.entityTags().contains(
						com.powers.player.SkillSystem.DARKNESS_TAG)))) {
			guardian.setTarget(null);
			current = null;
		}
		if (current != null) {
			if (guardian.getSensing().hasLineOfSight(current)) unseenTicks = 0;
			else unseenTicks++;
			if (unseenTicks <= UNSEEN_MEMORY_TICKS) return false;
			guardian.setTarget(null);
		}
		unseenTicks = 0;
		if (Math.floorMod(guardian.tickCount, INTERVAL)
				!= Math.floorMod(guardian.getUUID().hashCode(), INTERVAL)) return false;
		for (var observation : PerceptionSnapshotService.observe(level, guardian.position(),
				range, 6.0, MAX_CANDIDATES,
				fact -> !fact.entityId().equals(guardian.getUUID())
						&& Math.abs(fact.position().y - guardian.position().y) <= 4.0
						&& GuardianFactionRules.mayTarget(alignment, guardian.guardianOwner(),
						fact.entityId(), fact.darknessAligned()),
				PerceptionQueryProfile.GUARDIAN_TARGET)) {
			LivingEntity resolved = PerceptionSnapshotService.resolve(level, observation);
			if (resolved != null && guardian.canAttack(resolved)
					&& guardian.getSensing().hasLineOfSight(resolved)) {
				candidate = resolved;
				return true;
			}
		}
		return false;
	}

	@Override
	public void start() {
		guardian.setTarget(candidate);
		candidate = null;
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}
}
