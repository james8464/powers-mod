package com.powers.entity;

import com.powers.animation.CastingHand;
import com.powers.animation.CastingPose;
import com.powers.animation.CastingPoseService;
import com.powers.animation.CastingStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** A target-bound, cancellable warning around the vanilla melee damage entrypoint. */
final class FirstVesselMelee {
	private static final int WARNING_TICKS = 20;
	private UUID targetId;
	private int releaseAt;
	private Component warningName;

	boolean ready(FirstVessel vessel, Entity target, ServerLevel level,
			ServerBossEvent bar, String phase, boolean busy) {
		if (busy || !(target instanceof LivingEntity living) || !living.isAlive()
				|| !vessel.isWithinMeleeAttackRange(living) || !vessel.hasLineOfSight(living)) {
			cancel(vessel, bar, phase);
			return false;
		}
		if (!target.getUUID().equals(targetId)) {
			targetId = target.getUUID();
			releaseAt = vessel.tickCount + WARNING_TICKS;
			warningName = Component.literal(phase.replace('_', ' ') + " — MELEE");
			bar.setName(warningName);
			FirstVesselDialogue.announce(vessel, level, "boss.powers.first_vessel.windup");
			CastingPoseService.start(vessel, CastingPose.CHANNEL, CastingStyle.FIRST_VESSEL,
					CastingHand.BOTH, WARNING_TICKS);
			return false;
		}
		if (vessel.tickCount < releaseAt) return false;
		cancel(vessel, bar, phase);
		return true;
	}

	boolean blocksPlanning(FirstVessel vessel, ServerBossEvent bar, String phase) {
		LivingEntity target = vessel.getTarget();
		if (targetId == null) return false;
		if (target == null || !target.isAlive() || !target.getUUID().equals(targetId)
				|| !vessel.isWithinMeleeAttackRange(target) || !vessel.hasLineOfSight(target)
				|| vessel.tickCount > releaseAt + 20) {
			cancel(vessel, bar, phase);
			return false;
		}
		return true;
	}

	private void cancel(FirstVessel vessel, ServerBossEvent bar, String phase) {
		if (targetId != null) CastingPoseService.clear(vessel);
		if (warningName != null && bar.getName().equals(warningName)) {
			bar.setName(Component.translatable("boss.powers.first_vessel.phase."
					+ phase.toLowerCase(java.util.Locale.ROOT)));
		}
		targetId = null;
		warningName = null;
	}
}
