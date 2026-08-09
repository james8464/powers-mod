package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure, finite rules for the server-owned Gravity Displacement orrery. */
public final class GravityDisplacementRules {
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;
	private static final int BASE_TARGET_LIMIT = 16;
	private static final int VARIANT_TARGET_BONUS = 8;
	private static final int MAX_TARGET_LIMIT = 32;
	private static final int ANCIENT_PROJECTILE_LIMIT = 24;

	/** Why a candidate was captured or visibly resisted the field. */
	public enum CaptureDecision {
		CAPTURE,
		PROTECTED,
		AMETHYST,
		BODY_ANCHOR,
		FORCEFIELD,
		SPELL_WARD,
		TIME_LOCK,
		GRAVITY_RESONANCE
	}

	private GravityDisplacementRules() {
	}

	/** Resolves counterplay in privacy-first order before any motion is applied. */
	public static CaptureDecision captureDecision(boolean movementAllowed, boolean amethystDampened,
			boolean anchoredBody, boolean forcefield, boolean spellWard, boolean frozen) {
		if (!movementAllowed) return CaptureDecision.PROTECTED;
		if (amethystDampened) return CaptureDecision.AMETHYST;
		if (anchoredBody) return CaptureDecision.BODY_ANCHOR;
		if (forcefield) return CaptureDecision.FORCEFIELD;
		if (spellWard) return CaptureDecision.SPELL_WARD;
		if (frozen) return CaptureDecision.TIME_LOCK;
		return CaptureDecision.CAPTURE;
	}

	/** Returns the hard target cap after rank variants, never exceeding 32. */
	public static int targetLimit(boolean empoweredImpact, boolean ancientMastery) {
		int result = BASE_TARGET_LIMIT;
		if (empoweredImpact) result += VARIANT_TARGET_BONUS;
		if (ancientMastery) result += VARIANT_TARGET_BONUS;
		return Math.min(MAX_TARGET_LIMIT, result);
	}

	/** Ancient Mastery alone bends projectiles, under a fixed per-pulse cap. */
	public static int projectileLimit(boolean ancientMastery) {
		return ancientMastery ? ANCIENT_PROJECTILE_LIMIT : 0;
	}

	/** A nearer field may take a shared target only after crossing a hysteresis margin. */
	public static boolean claimWinner(double candidateDistanceSquared,
			double currentDistanceSquared, double hysteresisSquared) {
		return Double.isFinite(candidateDistanceSquared) && candidateDistanceSquared >= 0.0
				&& Double.isFinite(currentDistanceSquared) && currentDistanceSquared >= 0.0
				&& Double.isFinite(hysteresisSquared) && hysteresisSquared >= 0.0
				&& candidateDistanceSquared + hysteresisSquared < currentDistanceSquared;
	}

	/** Generates a deterministic, rotating orbit point within the authored field. */
	public static Vec3 orbitOffset(long seed, int age, double radius, double height) {
		if (!Double.isFinite(radius) || radius <= 0.0 || !Double.isFinite(height) || height < 0.0) {
			return Vec3.ZERO;
		}
		double phase = unsignedUnit(seed) * Math.PI * 2.0;
		double ring = 0.32 + unsignedByte(seed >>> 16) * 0.34;
		double angle = phase + Math.max(0, age) * 0.105;
		double y = height * (0.40 + unsignedByte(seed >>> 24) * 0.22
				+ Math.sin(angle * 0.7) * 0.12);
		double horizontal = radius * ring;
		return new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
	}

	/** Blends existing momentum toward an orbit point and clamps the final speed. */
	public static Vec3 steeringVelocity(Vec3 position, Vec3 currentVelocity, Vec3 desired,
			double pull, double maximumSpeed) {
		if (!finite(position) || !finite(currentVelocity) || !finite(desired)
				|| !Double.isFinite(pull) || pull <= 0.0
				|| !Double.isFinite(maximumSpeed) || maximumSpeed <= 0.0) return Vec3.ZERO;
		Vec3 result = desired.subtract(position).scale(pull).add(currentVelocity.scale(0.35));
		return capped(result, maximumSpeed);
	}

	/** Produces a radial collapse impulse with an explicitly bounded downward component. */
	public static Vec3 collapseImpulse(Vec3 center, Vec3 target, double horizontalForce,
			double downwardForce) {
		if (!finite(center) || !finite(target) || !Double.isFinite(horizontalForce)
				|| horizontalForce <= 0.0 || !Double.isFinite(downwardForce)
				|| downwardForce < 0.0) return Vec3.ZERO;
		Vec3 horizontal = new Vec3(target.x - center.x, 0.0, target.z - center.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) {
			return new Vec3(0.0, -downwardForce, 0.0);
		}
		horizontal = horizontal.normalize().scale(horizontalForce);
		return new Vec3(horizontal.x, -downwardForce, horizontal.z);
	}

	/** Curves one projectile tangentially without reflecting it or changing ownership. */
	public static Vec3 bendProjectile(Vec3 position, Vec3 velocity, Vec3 center,
			double bendStrength, double maximumSpeed) {
		if (!finite(position) || !finite(velocity) || !finite(center)
				|| !Double.isFinite(bendStrength) || bendStrength <= 0.0
				|| !Double.isFinite(maximumSpeed) || maximumSpeed <= 0.0) return Vec3.ZERO;
		Vec3 radial = new Vec3(position.x - center.x, 0.0, position.z - center.z);
		Vec3 tangent = radial.lengthSqr() <= MIN_LENGTH_SQUARED
				? Vec3.ZERO : new Vec3(-radial.z, 0.0, radial.x).normalize();
		return capped(velocity.scale(0.88).add(tangent.scale(bendStrength)), maximumSpeed);
	}

	/** A field remains live only while every owner and expiry invariant holds. */
	public static boolean fieldContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened && currentTick < expiresAt;
	}

	private static Vec3 capped(Vec3 vector, double maximumSpeed) {
		double lengthSquared = vector.lengthSqr();
		if (!Double.isFinite(lengthSquared)) return Vec3.ZERO;
		if (lengthSquared <= maximumSpeed * maximumSpeed) return vector;
		if (lengthSquared <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		return vector.scale(maximumSpeed / Math.sqrt(lengthSquared));
	}

	private static double unsignedUnit(long seed) {
		return (seed & 0xFFFFL) / 65535.0;
	}

	private static double unsignedByte(long value) {
		return (value & 0xFFL) / 255.0;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
