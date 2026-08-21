package com.powers.power.travel;

import com.powers.companion.PrivateCompanionManager;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.mind.BodyProxyManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
		return captureAt(level, caster, principal, caster.position());
	}

	/** Captures around a recorded physical origin, used while the caster is scouting remotely. */
	public static Snapshot captureAt(ServerLevel level, ServerPlayer caster, LivingEntity principal,
			Vec3 origin) {
		List<Member> companions = new ArrayList<>(TravelCohortRules.MAX_SIZE - 1);
		Set<UUID> captured = new HashSet<>(TravelCohortRules.MAX_SIZE);
		captured.add(principal.getUUID());
		captureRidingGraph(principal, origin, companions, captured);
		AABB search = new AABB(origin, origin).inflate(
				TravelCohortRules.RADIUS, TravelCohortRules.RADIUS, TravelCohortRules.RADIUS);
		for (LivingEntity entity : BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(LivingEntity.class), search,
				TravelCohortRules.MAX_SIZE - 1 - companions.size(),
				candidate -> !captured.contains(candidate.getUUID()) && TravelCohortRules.mayCapture(
						candidate.isAlive(), candidate.isRemoved(), BodyProxyManager.isProxy(candidate),
						candidate.position().distanceToSqr(origin)))) {
			if (companions.size() >= TravelCohortRules.MAX_SIZE - 1) break;
			companions.add(new Member(entity, entity.position().subtract(origin)));
			captured.add(entity.getUUID());
		}
		return new Snapshot(level, origin, principal, List.copyOf(companions));
	}

	/** Gives an already-connected riding graph priority without exceeding the cohort cap. */
	private static void captureRidingGraph(LivingEntity principal, Vec3 origin,
			List<Member> companions, Set<UUID> captured) {
		ArrayDeque<Entity> pending = new ArrayDeque<>();
		Set<UUID> discovered = new HashSet<>(TravelCohortRules.MAX_SIZE);
		Entity root = principal.getRootVehicle();
		pending.add(root);
		discovered.add(root.getUUID());
		while (!pending.isEmpty() && companions.size() < TravelCohortRules.MAX_SIZE - 1) {
			Entity entity = pending.removeFirst();
			if (entity instanceof LivingEntity living && living != principal
					&& TravelCohortRules.mayCapture(living.isAlive(), living.isRemoved(),
							BodyProxyManager.isProxy(living), living.position().distanceToSqr(origin))) {
				companions.add(new Member(living, living.position().subtract(origin)));
				captured.add(living.getUUID());
			}
			for (Entity passenger : entity.getPassengers()) {
				if (discovered.size() >= TravelCohortRules.MAX_SIZE) break;
				if (discovered.add(passenger.getUUID())) pending.addLast(passenger);
			}
		}
	}

	/** Moves still-near companions. A single rejected companion never cancels the principal. */
	public static int move(Snapshot cohort, ServerLevel targetLevel, Vec3 target) {
		List<Member> travelling = new ArrayList<>(cohort.companions().size());
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
			travelling.add(member);
		}
		// Vanilla teleport detachment depends on which member of a nested riding graph
		// moves first. Make the documented cohort policy deterministic before any move.
		for (Member member : travelling) {
			member.entity().ejectPassengers();
			member.entity().stopRiding();
		}
		int moved = 0;
		for (Member member : travelling) {
			LivingEntity entity = member.entity();
			Vec3 destination = target.add(member.offset());
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
