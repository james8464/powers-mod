package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure finite rules for the server-owned Chronal Overdrive runtime. */
public final class SuperSpeedRules {
	private static final int MAX_TRAIL_SEGMENTS = 24;
	private static final int MAX_PRESSURE_TARGETS = 8;
	private static final int MAX_AFTERIMAGE_TARGETS = 8;
	private static final int MAX_PROJECTILES = 16;
	private static final double WATER_MULTIPLIER = 0.35;
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;

	private SuperSpeedRules() {
	}

	/** Returns an exclusive, overflow-safe number of ticks remaining. */
	public static int remainingTicks(long startedAt, long expiresAt, long currentTick) {
		if (expiresAt <= startedAt || currentTick >= expiresAt) return 0;
		long remaining = expiresAt - Math.max(startedAt, currentTick);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, remaining));
	}

	/** Returns the owned land-speed bonus, grounded to 35% while in water. */
	public static double speedModifier(double potencyMultiplier, boolean inWater) {
		if (!Double.isFinite(potencyMultiplier) || potencyMultiplier <= 0.0) return 0.0;
		double modifier = Math.min(1.4, potencyMultiplier);
		return modifier * (inWater ? WATER_MULTIPLIER : 1.0);
	}

	/** Admits measured movement while rejecting stillness and teleport-sized gaps. */
	public static boolean trailAllowed(double distanceSquared, double maximumDistance) {
		return Double.isFinite(distanceSquared) && distanceSquared > 1.0E-6
				&& Double.isFinite(maximumDistance) && maximumDistance > 0.0
				&& distanceSquared <= maximumDistance * maximumDistance;
	}

	/** Allocates two samples per moved block under the global per-wake cap. */
	public static int trailSegments(double distance) {
		if (!Double.isFinite(distance) || distance <= 0.0) return 0;
		return Math.min(MAX_TRAIL_SEGMENTS, (int) Math.ceil(distance * 2.0));
	}

	/** Produces one finite backward-and-upward Second Step after a collision. */
	public static Vec3 rebound(Vec3 lookDirection, double backwardStrength,
			double upwardStrength) {
		if (!finite(lookDirection) || !Double.isFinite(backwardStrength)
				|| backwardStrength <= 0.0 || !Double.isFinite(upwardStrength)
				|| upwardStrength < 0.0) return Vec3.ZERO;
		Vec3 horizontal = new Vec3(lookDirection.x, 0.0, lookDirection.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		horizontal = horizontal.normalize().scale(-backwardStrength);
		return new Vec3(canonicalZero(horizontal.x), upwardStrength,
				canonicalZero(horizontal.z));
	}

	/** Produces a consent-checkable radial pressure impulse with bounded lift. */
	public static Vec3 pressureImpulse(Vec3 center, Vec3 target,
			double horizontalStrength, double verticalStrength) {
		if (!finite(center) || !finite(target) || !Double.isFinite(horizontalStrength)
				|| horizontalStrength <= 0.0 || !Double.isFinite(verticalStrength)
				|| verticalStrength < 0.0) return Vec3.ZERO;
		Vec3 horizontal = new Vec3(target.x - center.x, 0.0, target.z - center.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		horizontal = horizontal.normalize().scale(horizontalStrength);
		return new Vec3(horizontal.x, verticalStrength, horizontal.z);
	}

	/** Curves one hostile projectile away without reflection or ownership transfer. */
	public static Vec3 curveProjectile(Vec3 position, Vec3 velocity, Vec3 center,
			double outwardStrength, double maximumSpeed) {
		if (!finite(position) || !finite(velocity) || !finite(center)
				|| !Double.isFinite(outwardStrength) || outwardStrength <= 0.0
				|| !Double.isFinite(maximumSpeed) || maximumSpeed <= 0.0) return Vec3.ZERO;
		Vec3 radial = new Vec3(position.x - center.x, 0.0, position.z - center.z);
		if (radial.lengthSqr() <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		Vec3 result = velocity.scale(0.82).add(radial.normalize().scale(outwardStrength));
		double lengthSquared = result.lengthSqr();
		if (!Double.isFinite(lengthSquared) || lengthSquared <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		if (lengthSquared <= maximumSpeed * maximumSpeed) return result;
		return result.scale(maximumSpeed / Math.sqrt(lengthSquared));
	}

	/** Returns the one-shot Empowered Impact body cap. */
	public static int pressureTargetLimit(boolean empoweredImpact) {
		return empoweredImpact ? MAX_PRESSURE_TARGETS : 0;
	}

	/** Returns the periodic Veil memory-slip body cap. */
	public static int afterimageTargetLimit(boolean afterimage) {
		return afterimage ? MAX_AFTERIMAGE_TARGETS : 0;
	}

	/** Returns the Ancient Mastery per-cast projectile cap. */
	public static int projectileLimit(boolean ancientMastery) {
		return ancientMastery ? MAX_PROJECTILES : 0;
	}

	/** Keeps overdrive live only while every owner and expiry invariant holds. */
	public static boolean overdriveContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen, boolean ownsPower,
			long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened && !ownerFrozen
				&& ownsPower && currentTick < expiresAt;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}

	private static double canonicalZero(double value) {
		return Math.abs(value) <= MIN_LENGTH_SQUARED ? 0.0 : value;
	}
}
