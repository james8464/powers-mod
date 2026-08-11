package com.powers.power.travel;

import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.protection.PowerProtection;
import com.powers.realm.RealmConfinementRules;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

/** Validates bounds, loaded chunks, collision, hazards, wards, and safe zones before travel. */
public final class SafeDestinationResolver {
	public record Result(DestinationFailure failure, Vec3 destination) {
		public boolean allowed() {
			return failure == DestinationFailure.NONE;
		}
	}

	private SafeDestinationResolver() {
	}

	public static Result validate(LivingEntity subject, ServerLevel target, Vec3 requested, TravelKind kind) {
		Result preflight = validatePreload(subject, target, requested, kind);
		if (!preflight.allowed()) return preflight;

		BlockPos feet = BlockPos.containing(requested);
		if (!LoadedChunks.contains(target, feet)) return new Result(DestinationFailure.UNLOADED_CHUNK, requested);
		if (!recovery(kind)
				&& AmethystDampening.findPoweredWard(target, feet).isPresent()) {
			return new Result(DestinationFailure.WARD, requested);
		}
		if (!recovery(kind)
				&& PowerProtection.isSafeZone(target, requested)) {
			return new Result(DestinationFailure.SAFE_ZONE, requested);
		}
		if (!recovery(kind)
				&& SpellFieldManager.blocksTravel(subject, target, requested)) {
			return new Result(DestinationFailure.ANTI_PORTAL, requested);
		}
		BlockPos head = feet.above();
		if (!target.getFluidState(feet).isEmpty() || !target.getFluidState(head).isEmpty()) {
			return new Result(DestinationFailure.HAZARD, requested);
		}
		if (kind == TravelKind.POWER || kind == TravelKind.CRYSTAL || kind == TravelKind.COMPANION) {
			BlockPos floor = feet.below();
			var support = target.getBlockState(floor);
			if (!support.entityCanStandOn(target, floor, subject)
					|| support.is(Blocks.MAGMA_BLOCK) || support.is(Blocks.CACTUS)
					|| support.is(Blocks.CAMPFIRE) || support.is(Blocks.SOUL_CAMPFIRE)
					|| support.is(Blocks.WITHER_ROSE) || support.is(Blocks.POWDER_SNOW)) {
				return new Result(DestinationFailure.HAZARD, requested);
			}
		}
		AABB moved = subject.getBoundingBox().move(requested.subtract(subject.position()));
		if (!target.getWorldBorder().isWithinBounds(moved) || !target.noCollision(subject, moved)) {
			return new Result(DestinationFailure.COLLISION, requested);
		}
		return new Result(DestinationFailure.NONE, requested);
	}

	/** Runs every policy check that is safe before a remote destination chunk has loaded. */
	public static Result validatePreload(LivingEntity subject, ServerLevel target, Vec3 requested, TravelKind kind) {
		var border = target.getWorldBorder();
		DestinationFailure bounds = boundsFailure(requested.x, requested.y, requested.z,
				target.getMinY(), target.getMaxY(), border.getMinX(), border.getMaxX(),
				border.getMinZ(), border.getMaxZ());
		if (bounds != DestinationFailure.NONE) return new Result(bounds, requested);

		if (subject instanceof ServerPlayer player) {
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			DestinationFailure realm = realmFailure(
					player.level().dimension().identifier().toString(), target.dimension().identifier().toString(), kind,
					SkillSystem.hasDarknessTag(player), data.skillLevel(), data.darknessLevel());
			if (realm != DestinationFailure.NONE) return new Result(realm, requested);
		}
		if (!recovery(kind)
				&& DimensionalAnchorAbility.isAnchored(subject)
				&& !target.dimension().equals(DimensionalAnchorAbility.anchorDimension(subject))) {
			return new Result(DestinationFailure.ANCHOR, requested);
		}
		if (!recovery(kind) && !PowerProtection.mayPortal(subject, target,
				BlockPos.containing(requested))) {
			return new Result(DestinationFailure.SAFE_ZONE, requested);
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

	static DestinationFailure realmFailure(String origin, String target, TravelKind kind,
			boolean darknessTag, int normalLevel, int darknessLevel) {
		if (kind == TravelKind.ADMIN_RECOVERY || kind == TravelKind.FATAL_SOUL_RETURN
				|| origin.equals(target)) {
			return DestinationFailure.NONE;
		}
		boolean qualifiedDarkness = darknessTag && darknessLevel >= SkillSystem.DARKNESS_GATE_LEVEL;
		boolean fromMindscape = origin.equals("powers:dark_realm") || origin.equals("powers:light_realm");
		if (fromMindscape) {
			if (kind != TravelKind.PLAYER_RETURN) return DestinationFailure.REALM_RESTRICTED;
			if (RealmConfinementRules.requiredRespawnRealm(origin, darknessTag,
					normalLevel, darknessLevel) != null) return DestinationFailure.REALM_RESTRICTED;
		}
		if (target.equals("powers:middleworld") && kind != TravelKind.CRYSTAL) {
			return DestinationFailure.REALM_RESTRICTED;
		}
		if (target.equals("powers:dark_realm") && kind != TravelKind.CRYSTAL && !qualifiedDarkness) {
			return DestinationFailure.REALM_RESTRICTED;
		}
		return DestinationFailure.NONE;
	}

	private static boolean recovery(TravelKind kind) {
		return kind == TravelKind.PLAYER_RETURN || kind == TravelKind.FATAL_SOUL_RETURN
				|| kind == TravelKind.ADMIN_RECOVERY;
	}
}
