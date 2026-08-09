package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Pure bounded rules for the two-stage Tempest Breezy Bash rite. */
public final class BreezyBashRules {
	private static final int BASE_TARGET_LIMIT = 16;
	private static final int VARIANT_TARGET_BONUS = 8;
	private static final int MAX_TARGET_LIMIT = 32;
	private static final int MASTERED_PROJECTILE_LIMIT = 16;
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;

	/** Why a body was captured or visibly resisted the wind rite. */
	public enum CaptureDecision {
		CAPTURE,
		PROTECTED,
		AMETHYST,
		BODY_ANCHOR,
		FORCEFIELD,
		SPELL_WARD,
		TIME_LOCK,
		CEILING,
		WIND_RESONANCE
	}

	private BreezyBashRules() {
	}

	/** Returns the hard body cap after independent impact and mastery bonuses. */
	public static int targetLimit(boolean empoweredImpact, boolean ancientMastery) {
		int limit = BASE_TARGET_LIMIT;
		if (empoweredImpact) limit += VARIANT_TARGET_BONUS;
		if (ancientMastery) limit += VARIANT_TARGET_BONUS;
		return Math.min(MAX_TARGET_LIMIT, limit);
	}

	/** Ancient Mastery alone curves projectiles, under a strict per-cast cap. */
	public static int projectileLimit(boolean ancientMastery) {
		return ancientMastery ? MASTERED_PROJECTILE_LIMIT : 0;
	}

	/** Resolves movement counterplay before any body velocity is changed. */
	public static CaptureDecision captureDecision(boolean movementAllowed,
			boolean dampened, boolean bodyAnchor, boolean forcefield, boolean spellWard,
			boolean frozen, boolean clearPath, boolean claimAllowed) {
		if (!movementAllowed) return CaptureDecision.PROTECTED;
		if (dampened) return CaptureDecision.AMETHYST;
		if (bodyAnchor) return CaptureDecision.BODY_ANCHOR;
		if (forcefield) return CaptureDecision.FORCEFIELD;
		if (spellWard) return CaptureDecision.SPELL_WARD;
		if (frozen) return CaptureDecision.TIME_LOCK;
		if (!clearPath) return CaptureDecision.CEILING;
		if (!claimAllowed) return CaptureDecision.WIND_RESONANCE;
		return CaptureDecision.CAPTURE;
	}

	/** Produces a finite outward-and-upward launch, retaining lift at the exact centre. */
	public static Vec3 launchImpulse(Vec3 center, Vec3 target,
			double outwardStrength, double verticalStrength) {
		if (!finite(center) || !finite(target) || !Double.isFinite(outwardStrength)
				|| outwardStrength < 0.0 || !Double.isFinite(verticalStrength)
				|| verticalStrength <= 0.0) return Vec3.ZERO;
		Vec3 horizontal = new Vec3(target.x - center.x, 0.0, target.z - center.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) {
			return new Vec3(0.0, verticalStrength, 0.0);
		}
		horizontal = horizontal.normalize().scale(outwardStrength);
		return new Vec3(horizontal.x, verticalStrength, horizontal.z);
	}

	/** Retains a quarter of horizontal momentum while applying the downward verdict. */
	public static Vec3 slamVelocity(Vec3 currentVelocity, double downwardStrength) {
		if (!finite(currentVelocity) || !Double.isFinite(downwardStrength)
				|| downwardStrength <= 0.0) return Vec3.ZERO;
		return new Vec3(currentVelocity.x * 0.25, -downwardStrength,
				currentVelocity.z * 0.25);
	}

	/** Curves a hostile projectile radially outward without reflection or ownership change. */
	public static Vec3 curveProjectile(Vec3 position, Vec3 velocity, Vec3 center,
			double outwardStrength, double maximumSpeed) {
		if (!finite(position) || !finite(velocity) || !finite(center)
				|| !Double.isFinite(outwardStrength) || outwardStrength <= 0.0
				|| !Double.isFinite(maximumSpeed) || maximumSpeed <= 0.0) return Vec3.ZERO;
		Vec3 radial = new Vec3(position.x - center.x, 0.0, position.z - center.z);
		if (radial.lengthSqr() <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		Vec3 result = velocity.scale(0.78).add(radial.normalize().scale(outwardStrength));
		double lengthSquared = result.lengthSqr();
		if (!Double.isFinite(lengthSquared) || lengthSquared <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		if (lengthSquared <= maximumSpeed * maximumSpeed) return result;
		return result.scale(maximumSpeed / Math.sqrt(lengthSquared));
	}

	/** Allows an unclaimed body or an idempotent claim by the same owner. */
	public static boolean claimAllowed(UUID currentOwner, UUID candidateOwner) {
		return candidateOwner != null && (currentOwner == null || currentOwner.equals(candidateOwner));
	}

	/** Keeps the rite live only while every owner and expiry invariant holds. */
	public static boolean riteContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen, boolean ownsPower,
			long currentTick, long resolvesAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened && !ownerFrozen
				&& ownsPower && currentTick < resolvesAt;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
