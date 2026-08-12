package com.powers.companion;

import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Performs one validated Shadow body move and exposes the replacement entity to its session owner. */
final class ShadowTravelRuntime {
	record FollowResult(ShadowCompanionEntity body, boolean moved) {
	}

	private ShadowTravelRuntime() {
	}

	static ShadowCompanionEntity move(ShadowCompanionEntity body, ServerLevel target,
			Vec3 destination, float yRot, float xRot) {
		if (body == null || target == null || destination == null || body.isRemoved()
				|| !body.isAlive() || !Double.isFinite(destination.x)
				|| !Double.isFinite(destination.y) || !Double.isFinite(destination.z)) return null;
		if (body.level() == target) {
			body.setPos(destination);
			body.setDeltaMovement(Vec3.ZERO);
			return body;
		}
		Entity moved = body.teleport(new TeleportTransition(target, destination, Vec3.ZERO,
				yRot, xRot, TeleportTransition.PLAY_PORTAL_SOUND));
		return moved instanceof ShadowCompanionEntity shadow ? shadow : null;
	}

	static FollowResult follow(ServerPlayer owner, ShadowCompanionEntity body) {
		Vec3 desired = PrivateCompanionRules.followPoint(owner.position(), owner.getLookAngle());
		boolean changedDimension = body.level() != owner.level();
		if (!changedDimension && !ShadowCompanionRules.shouldTeleport(
				body.position().distanceToSqr(desired))) return new FollowResult(body, false);
		ServerLevel destinationLevel = (ServerLevel) owner.level();
		if (!LoadedChunks.contains(destinationLevel, BlockPos.containing(desired))) {
			return new FollowResult(body, false);
		}
		AABB landing = body.getBoundingBox().move(desired.subtract(body.position()));
		if (!destinationLevel.getWorldBorder().isWithinBounds(landing)
				|| !destinationLevel.noBlockCollision(body, landing)) desired = owner.position();
		ShadowCompanionEntity moved = move(body, destinationLevel, desired,
				owner.getYRot(), owner.getXRot());
		return new FollowResult(moved, moved != null);
	}
}
