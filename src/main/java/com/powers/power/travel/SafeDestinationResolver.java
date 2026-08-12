package com.powers.power.travel;

import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.DimensionalAnchorAbility;
import com.powers.protection.PowerProtection;
import com.powers.protection.CrossSystemPrecedence;
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

import java.util.EnumSet;

/** Validates bounds, loaded chunks, collision, hazards, wards, and safe zones before travel. */
public final class SafeDestinationResolver {
	public enum DestinationMode {
		SAFE_LANDING,
		EXACT
	}

	public record Result(DestinationFailure failure, Vec3 destination) {
		public boolean allowed() {
			return failure == DestinationFailure.NONE;
		}
	}

	private SafeDestinationResolver() {
	}

	public static Result validate(LivingEntity subject, ServerLevel target, Vec3 requested, TravelKind kind) {
		return validate(subject, target, requested, kind, DestinationMode.SAFE_LANDING);
	}

	/** Validates route policy without treating solid, fluid, or unsupported coordinates as errors. */
	public static Result validateExact(LivingEntity subject, ServerLevel target, Vec3 requested,
			TravelKind kind) {
		return validate(subject, target, requested, kind, DestinationMode.EXACT);
	}

	private static Result validate(LivingEntity subject, ServerLevel target, Vec3 requested,
			TravelKind kind, DestinationMode mode) {
		Result preflight = validatePreload(subject, target, requested, kind);
		if (!preflight.allowed()) return preflight;

		BlockPos feet = BlockPos.containing(requested);
		if (!LoadedChunks.contains(target, feet)) return new Result(DestinationFailure.UNLOADED_CHUNK, requested);
		if (!recovery(kind) && !(subject instanceof ShadowCompanionEntity)) {
			EnumSet<CrossSystemPrecedence.Guard> guards = EnumSet.noneOf(CrossSystemPrecedence.Guard.class);
			if (PowerProtection.isSafeZone(target, requested)) guards.add(CrossSystemPrecedence.Guard.SAFE_ZONE);
			if (AmethystDampening.findPoweredWard(target, feet).isPresent()) {
				guards.add(CrossSystemPrecedence.Guard.AMETHYST);
			}
			if (SpellFieldManager.blocksTravel(subject, target, requested)) {
				guards.add(CrossSystemPrecedence.Guard.ANTI_PORTAL_FIELD);
			}
			if (DimensionalAnchorAbility.isAnchored(subject)
					&& !target.dimension().equals(DimensionalAnchorAbility.anchorDimension(subject))) {
				guards.add(CrossSystemPrecedence.Guard.DIMENSIONAL_ANCHOR);
			}
			CrossSystemPrecedence.Guard guard = CrossSystemPrecedence.first(guards);
			if (guard != null) return new Result(switch (guard) {
				case SAFE_ZONE -> DestinationFailure.SAFE_ZONE;
				case AMETHYST -> DestinationFailure.WARD;
				case ANTI_PORTAL_FIELD -> DestinationFailure.ANTI_PORTAL;
				case DIMENSIONAL_ANCHOR -> DestinationFailure.ANCHOR;
				default -> DestinationFailure.REALM_RESTRICTED;
			}, requested);
		}
		if (!environmentalSafetyRequired(mode)) return new Result(DestinationFailure.NONE, requested);
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

	static boolean environmentalSafetyRequired(DestinationMode mode) {
		return mode == DestinationMode.SAFE_LANDING;
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
		if (!(subject instanceof ShadowCompanionEntity) && !recovery(kind)
				&& !PowerProtection.mayPortal(subject, target,
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
