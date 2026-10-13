package com.powers.companion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Owner-local saved places and loaded-only navigation; never acquires chunk tickets. */
final class ShadowPlaceCommands {
	private ShadowPlaceCommands() { }

	static ShadowPlaceMemory.RememberResult rememberPlace(ServerPlayer owner, String name) {
		ShadowCompanionData stored = ShadowCompanionStore.get(owner);
		var result = stored.places().remember(name, owner.level().dimension().identifier(),
				owner.blockPosition());
		if (result.accepted()) {
			ShadowCompanionStore.update(owner, state -> state.withPlaces(result.memory()));
		}
		return result;
	}

	static ShadowPlaceMemory.ForgetResult forgetPlace(ServerPlayer owner, String name) {
		var result = ShadowCompanionStore.get(owner).places().forget(name);
		if (result.removed()) {
			ShadowCompanionStore.update(owner, state -> state.withPlaces(result.memory()));
		}
		return result;
	}

	static PlaceNavigationResult goToPlace(ServerPlayer owner, String name) {
		var place = ShadowCompanionStore.get(owner).places().find(name);
		if (place.isEmpty()) return new PlaceNavigationResult(false, "unknown_place");
		PrivateCompanionSession session = PrivateCompanionManager.session(owner.getUUID());
		if (session == null || session.body == null || !session.body.isAlive()) {
			return new PlaceNavigationResult(false, "manifest_first");
		}
		ShadowCompanionEntity shadow = session.body;
		ServerLevel level = (ServerLevel) shadow.level();
		BlockPos target = place.get().position();
		ChunkPos chunk = new ChunkPos(target.getX() >> 4, target.getZ() >> 4);
		boolean sameDimension = level.dimension().identifier().toString()
				.equals(place.get().dimension());
		boolean loaded = sameDimension && level.getChunkSource().hasChunk(chunk.x(), chunk.z());
		var decision = ShadowPlaceNavigationRules.decide(sameDimension, loaded,
				shadow.distanceToSqr(Vec3.atCenterOf(target)));
		if (decision != ShadowPlaceNavigationRules.Decision.GO) {
			return new PlaceNavigationResult(false, decision.name().toLowerCase(java.util.Locale.ROOT));
		}
		boolean moving = shadow.getNavigation().moveTo(
				target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.15);
		return new PlaceNavigationResult(moving, moving ? "navigating" : "no_path");
	}

	static String placeNames(ServerPlayer owner) {
		return String.join(", ", ShadowCompanionStore.get(owner).places().places().stream()
				.map(ShadowPlaceMemory.Place::name).toList());
	}

	record PlaceNavigationResult(boolean accepted, String reason) { }

}
