package com.powers.power.travel;

import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.mind.BodyProxyManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Captures and moves a hard-bounded, consent-free cohort around a teleport caster. */
public final class TravelCohort {
	public record Member(LivingEntity entity, Vec3 offset) {
	}

	public record Snapshot(ServerLevel originLevel, Vec3 origin, LivingEntity principal,
			List<Member> companions) {
	}

	private TravelCohort() {
	}

	public static Snapshot capture(ServerLevel level, ServerPlayer caster, LivingEntity principal) {
		Vec3 origin = caster.position();
		List<Member> companions = new ArrayList<>(TravelCohortRules.MAX_SIZE - 1);
		AABB search = new AABB(origin, origin).inflate(
				TravelCohortRules.RADIUS, TravelCohortRules.RADIUS, TravelCohortRules.RADIUS);
		for (LivingEntity entity : BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(LivingEntity.class), search, TravelCohortRules.MAX_SIZE,
				candidate -> candidate != principal && TravelCohortRules.mayCapture(
						candidate.isAlive(), candidate.isRemoved(), BodyProxyManager.isProxy(candidate),
						candidate.position().distanceToSqr(origin)))) {
			if (companions.size() >= TravelCohortRules.MAX_SIZE - 1) break;
			companions.add(new Member(entity, entity.position().subtract(origin)));
		}
		return new Snapshot(level, origin, principal, List.copyOf(companions));
	}

	/** Moves still-near companions. A single rejected companion never cancels the principal. */
	public static int move(Snapshot cohort, ServerLevel targetLevel, Vec3 target) {
		int moved = 0;
		for (Member member : cohort.companions()) {
			LivingEntity entity = member.entity();
			if (!TravelCohortRules.mayCommit(entity.isAlive() && !entity.isRemoved(),
					entity.level() == cohort.originLevel(),
					entity.position().distanceToSqr(cohort.origin()))) continue;
			Vec3 destination = target.add(member.offset());
			if (!LoadedChunks.contains(targetLevel, BlockPos.containing(destination))) continue;
			if (!(entity instanceof ShadowCompanionEntity)
					&& !SafeDestinationResolver.validateExact(entity, targetLevel, destination,
							TravelKind.COMPANION).allowed()) continue;
			boolean travelled;
			if (entity instanceof ShadowCompanionEntity shadow) {
				travelled = PrivateCompanionManager.travelBody(shadow, targetLevel, destination);
			} else {
				travelled = entity.teleport(new TeleportTransition(targetLevel, destination, Vec3.ZERO,
						entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND)) != null;
			}
			if (travelled) moved++;
		}
		return moved;
	}
}
