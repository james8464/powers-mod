package com.powers.power.abilities;

import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
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
import java.util.List;

/** Bounded companion capture and safe-arrival work shared by both Teleport input routes. */
final class TeleportCompanionMover {
	record Candidate(Entity entity, Vec3 offset) {
	}

	private TeleportCompanionMover() {
	}

	static List<Candidate> collect(ServerLevel originLevel, ServerPlayer caster,
			LivingEntity traveller, Vec3 departure, double radius) {
		List<Candidate> companions = new ArrayList<>();
		AABB search = new AABB(departure, departure).inflate(radius, radius + 2.0, radius);
		for (Entity entity : BoundedEntityCandidates.collect(originLevel,
				EntityTypeTest.forClass(Entity.class), search, 64,
				candidate -> candidate.isAlive() && candidate != traveller
						&& candidate.position().distanceToSqr(departure) <= radius * radius)) {
			if (entity instanceof ServerPlayer player
					&& !PowerProtection.mayBringCompanion(caster, player)) continue;
			companions.add(new Candidate(entity, entity.position().subtract(departure)));
		}
		return List.copyOf(companions);
	}

	static void move(ServerLevel originLevel, ServerLevel targetLevel, Vec3 departure,
			Vec3 target, double radius, List<Candidate> companions) {
		for (Candidate companion : companions) {
			Entity entity = companion.entity();
			if (!DelayedTravelRules.companionMayTravel(!entity.isRemoved() && entity.isAlive(),
					entity.level() == originLevel, entity.position().distanceToSqr(departure), radius)) continue;
			Vec3 destination = target.add(companion.offset());
			if (!LoadedChunks.contains(targetLevel, BlockPos.containing(destination))) continue;
			if (entity instanceof ServerPlayer player && !SafeDestinationResolver.validate(
					player, targetLevel, destination, TravelKind.COMPANION).allowed()) continue;
			AABB landingBox = entity.getBoundingBox().move(destination.subtract(entity.position()));
			if (!targetLevel.getWorldBorder().isWithinBounds(landingBox)
					|| !targetLevel.noBlockCollision(entity, landingBox)) continue;
			entity.teleport(new TeleportTransition(targetLevel, destination, Vec3.ZERO,
					entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		}
	}
}
