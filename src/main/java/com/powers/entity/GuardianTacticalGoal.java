package com.powers.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Bounded ranged, cover, and retreat movement layered above ordinary melee AI. */
final class GuardianTacticalGoal extends Goal {
	private final AbstractPlayerLikeMob guardian;

	GuardianTacticalGoal(AbstractPlayerLikeMob guardian) {
		this.guardian = guardian;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = guardian.getTarget();
		return stance(target) != GuardianTactics.Stance.IDLE
				&& stance(target) != GuardianTactics.Stance.MELEE;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void stop() {
		guardian.getNavigation().stop();
	}

	@Override
	public void tick() {
		LivingEntity target = guardian.getTarget();
		if (target == null) return;
		guardian.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (guardian.tickCount % GuardianTactics.NAVIGATION_INTERVAL != 0) return;
		switch (stance(target)) {
			case ADVANCE -> guardian.getNavigation().moveTo(target, 1.15);
			case RANGED -> holdRangedSpacing(target);
			case SEEK_COVER -> seekCover(target);
			case RETREAT -> retreat(target, 9.0, 1.3);
			default -> guardian.getNavigation().stop();
		}
	}

	private GuardianTactics.Stance stance(LivingEntity target) {
		return GuardianTactics.choose(target == null ? 0.0 : guardian.distanceTo(target),
				guardian.getHealth() / Math.max(1.0F, guardian.getMaxHealth()),
				target != null && guardian.getSensing().hasLineOfSight(target),
				target != null && target.isAlive() && guardian.canAttack(target));
	}

	private void holdRangedSpacing(LivingEntity target) {
		double distance = guardian.distanceTo(target);
		if (distance < 8.0) {
			retreat(target, 5.0, 1.1);
		} else if (distance > 14.0) {
			guardian.getNavigation().moveTo(target, 1.0);
		} else {
			guardian.getNavigation().stop();
		}
	}

	private void seekCover(LivingEntity target) {
		if (guardian.tickCount % GuardianTactics.COVER_SEARCH_INTERVAL != 0) return;
		BlockPos origin = guardian.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		int visited = 0;
		for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) {
			for (int dz = -2; dz <= 2 && visited < GuardianTactics.MAX_COVER_CANDIDATES; dz++, visited++) {
				BlockPos candidate = origin.offset(dx, dy, dz);
				BlockPos ground = candidate.below();
				if (!guardian.level().getBlockState(candidate).isAir()
						|| !guardian.level().getBlockState(candidate.above()).isAir()
						|| guardian.level().getBlockState(ground)
								.getCollisionShape(guardian.level(), ground).isEmpty()) continue;
				Vec3 end = Vec3.atBottomCenterOf(candidate).add(0.0, guardian.getEyeHeight(), 0.0);
				HitResult hit = guardian.level().clip(new ClipContext(target.getEyePosition(), end,
						ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, guardian));
				double distance = guardian.distanceToSqr(end);
				if (hit.getType() != HitResult.Type.MISS && distance < bestDistance) {
					best = candidate;
					bestDistance = distance;
				}
			}
		}
		if (best != null) guardian.getNavigation().moveTo(best.getX() + 0.5,
				best.getY(), best.getZ() + 0.5, 1.2);
		else retreat(target, 7.0, 1.2);
	}

	private void retreat(LivingEntity target, double distance, double speed) {
		Vec3 away = guardian.position().subtract(target.position());
		if (away.lengthSqr() < 1.0E-6) away = new Vec3(1.0, 0.0, 0.0);
		Vec3 destination = guardian.position().add(away.normalize().scale(distance));
		guardian.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
	}
}
