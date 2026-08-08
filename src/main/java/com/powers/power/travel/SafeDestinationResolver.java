package com.powers.power.travel;

import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SafeDestinationResolver {
	public record Result(DestinationFailure failure, Vec3 destination) {
		public boolean allowed() {
			return failure == DestinationFailure.NONE;
		}
	}

	private SafeDestinationResolver() {
	}

	public static Result validate(ServerPlayer subject, ServerLevel target, Vec3 requested, TravelKind kind) {
		var border = target.getWorldBorder();
		DestinationFailure bounds = boundsFailure(requested.x, requested.y, requested.z,
				target.getMinY(), target.getMaxY(), border.getMinX(), border.getMaxX(),
				border.getMinZ(), border.getMaxZ());
		if (bounds != DestinationFailure.NONE) return new Result(bounds, requested);

		DestinationFailure realm = realmFailure(target.dimension().identifier().getPath().equals("middleworld"), kind);
		if (realm != DestinationFailure.NONE) return new Result(realm, requested);
		if (kind != TravelKind.RETURN && kind != TravelKind.ADMIN && DimensionalAnchorAbility.isAnchored(subject)
				&& !target.dimension().equals(DimensionalAnchorAbility.anchorDimension(subject))) {
			return new Result(DestinationFailure.ANCHOR, requested);
		}
		if (SkillSystem.isDarkRealm(target.dimension())
				&& !SkillSystem.isDarkRealm(subject.level().dimension())
				&& kind != TravelKind.CRYSTAL && kind != TravelKind.RETURN && kind != TravelKind.ADMIN
				&& !SkillSystem.canEnterDarkRealm(subject)) {
			return new Result(DestinationFailure.REALM_RESTRICTED, requested);
		}

		BlockPos feet = BlockPos.containing(requested);
		if (!target.hasChunkAt(feet)) return new Result(DestinationFailure.UNLOADED_CHUNK, requested);
		if (kind != TravelKind.RETURN && kind != TravelKind.ADMIN
				&& AmethystDampening.findPoweredWard(target, feet).isPresent()) {
			return new Result(DestinationFailure.WARD, requested);
		}
		if (kind != TravelKind.RETURN && kind != TravelKind.ADMIN && PowerProtection.isSafeZone(target, requested)) {
			return new Result(DestinationFailure.SAFE_ZONE, requested);
		}
		BlockPos head = feet.above();
		if (!target.getFluidState(feet).isEmpty() || !target.getFluidState(head).isEmpty()) {
			return new Result(DestinationFailure.HAZARD, requested);
		}
		AABB moved = subject.getBoundingBox().move(requested.subtract(subject.position()));
		if (!border.isWithinBounds(moved) || !target.noCollision(subject, moved)) {
			return new Result(DestinationFailure.COLLISION, requested);
		}
		return new Result(DestinationFailure.NONE, requested);
	}

	static DestinationFailure boundsFailure(double x, double y, double z, int minY, int maxY,
			double minX, double maxX, double minZ, double maxZ) {
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
				|| y < minY || y >= maxY || x < minX || x >= maxX || z < minZ || z >= maxZ) {
			return DestinationFailure.OUT_OF_BOUNDS;
		}
		return DestinationFailure.NONE;
	}

	static DestinationFailure realmFailure(boolean middleworld, TravelKind kind) {
		return middleworld && kind != TravelKind.CRYSTAL && kind != TravelKind.RETURN && kind != TravelKind.ADMIN
				? DestinationFailure.REALM_RESTRICTED : DestinationFailure.NONE;
	}
}
