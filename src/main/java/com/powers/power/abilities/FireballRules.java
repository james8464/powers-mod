package com.powers.power.abilities;

/** Pure finite rules for the server-owned Cinderheart projectile runtime. */
public final class FireballRules {
	private static final int BASE_MAXIMUM_TIER = 3;
	private static final int ANCIENT_MAXIMUM_TIER = 4;
	private static final int HOVER_EXTENSION_TICKS = 40;
	private static final int MAXIMUM_HOVER_LIFETIME = 360;
	private static final int LAUNCHED_LIFETIME = 120;
	private static final int MAX_TRAIL_SEGMENTS = 24;

	private FireballRules() {
	}

	/** Exhaustive semantic result at the first material impact surface. */
	public enum ImpactDecision {
		DETONATE,
		UNOWNED,
		SAFE_ZONE,
		AMETHYST,
		SANCTUARY,
		KINETIC_WARD,
		FORCEFIELD,
		WATER,
		FROST
	}

	/** Returns the highest paid charge tier available to this rank. */
	public static int maximumTier(boolean ancientMastery) {
		return ancientMastery ? ANCIENT_MAXIMUM_TIER : BASE_MAXIMUM_TIER;
	}

	/** Advances by one tier while normalizing malformed values into the legal range. */
	public static int nextTier(int currentTier, boolean ancientMastery) {
		int maximum = maximumTier(ancientMastery);
		return Math.min(maximum, Math.max(0, currentTier) + 1);
	}

	/** Returns whether another paid cast can deepen the hovering seal. */
	public static boolean canCharge(int currentTier, boolean ancientMastery) {
		return currentTier >= 1 && currentTier < maximumTier(ancientMastery);
	}

	/** Extends a hover by forty ticks without passing 360 ticks from creation. */
	public static long extendedHoverExpiry(long startedAt, long currentExpiry, long currentTick) {
		if (startedAt < 0L || currentExpiry < startedAt || currentTick < startedAt) return startedAt;
		long maximum = saturatingAdd(startedAt, MAXIMUM_HOVER_LIFETIME);
		return Math.min(maximum, saturatingAdd(currentExpiry, HOVER_EXTENSION_TICKS));
	}

	/** Returns the exclusive six-second launched-projectile expiry. */
	public static long launchExpiry(long currentTick) {
		return currentTick < 0L ? 0L : saturatingAdd(currentTick, LAUNCHED_LIFETIME);
	}

	/** Returns an exclusive, overflow-safe number of ticks remaining. */
	public static int remainingTicks(long startedAt, long expiresAt, long currentTick) {
		if (startedAt < 0L || expiresAt <= startedAt || currentTick >= expiresAt) return 0;
		long remaining = expiresAt - Math.max(startedAt, currentTick);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, remaining));
	}

	/** Returns the finite post-launch reflection budget granted by rank paths. */
	public static int reflectionLimit(boolean reflectiveWard, boolean ancientMastery) {
		return 2 + (reflectiveWard ? 1 : 0) + (ancientMastery ? 1 : 0);
	}

	/** Allows the initial launch freely, then admits only unused finite reflections. */
	public static boolean reflectionAllowed(boolean launched, int reflections, int limit) {
		if (!launched) return true;
		return reflections >= 0 && limit > 0 && reflections < limit;
	}

	/** Returns the tier and Might-scaled visual and mechanical impact radius. */
	public static double impactRadius(int tier, boolean empoweredImpact) {
		int bounded = boundedTier(tier);
		return 2.0 + (bounded - 1) * 0.75 + (empoweredImpact ? 0.65 : 0.0);
	}

	/** Returns the tier multiplier with a bounded fifteen-percent Might bonus. */
	public static double damageMultiplier(int tier, boolean empoweredImpact) {
		double tierMultiplier = switch (boundedTier(tier)) {
			case 1 -> 1.0;
			case 2 -> 1.30;
			case 3 -> 1.65;
			default -> 2.05;
		};
		return tierMultiplier * (empoweredImpact ? 1.15 : 1.0);
	}

	/** Returns the finite ignition duration for a successful flame impact. */
	public static int burnSeconds(int tier) {
		return 3 + (boundedTier(tier) - 1) * 2;
	}

	/** Returns the nearest-first living-body cap for this rank. */
	public static int targetLimit(boolean ancientMastery) {
		return ancientMastery ? 16 : 12;
	}

	/** Returns the terrain-fire cap only when server policy explicitly permits it. */
	public static int terrainScorchLimit(int tier, boolean terrainAllowed) {
		return terrainAllowed ? Math.min(8, boundedTier(tier) * 2) : 0;
	}

	/** Returns linear centre-to-edge damage falloff with a 35% edge floor. */
	public static double falloff(double distance, double radius) {
		if (!Double.isFinite(distance) || distance < 0.0
				|| !Double.isFinite(radius) || radius <= 0.0 || distance > radius) return 0.0;
		return 1.0 - 0.65 * (distance / radius);
	}

	/** Admits measured flight while rejecting stillness and teleport-sized gaps. */
	public static boolean trailAllowed(double distanceSquared, double maximumDistance) {
		return Double.isFinite(distanceSquared) && distanceSquared > 1.0E-6
				&& Double.isFinite(maximumDistance) && maximumDistance > 0.0
				&& distanceSquared <= maximumDistance * maximumDistance;
	}

	/** Allocates two trail samples per travelled block under the hard cap. */
	public static int trailSegments(double distance) {
		if (!Double.isFinite(distance) || distance <= 0.0) return 0;
		return Math.min(MAX_TRAIL_SEGMENTS, (int) Math.ceil(distance * 2.0));
	}

	/** Classifies terminals before any protected damage, ignition, or movement write. */
	public static ImpactDecision impactDecision(boolean controllerPresent,
			boolean safeZone, boolean amethyst, boolean sanctuary,
			boolean kineticWard, boolean forcefield, boolean water, boolean frost) {
		if (!controllerPresent) return ImpactDecision.UNOWNED;
		if (safeZone) return ImpactDecision.SAFE_ZONE;
		if (amethyst) return ImpactDecision.AMETHYST;
		if (sanctuary) return ImpactDecision.SANCTUARY;
		if (kineticWard) return ImpactDecision.KINETIC_WARD;
		if (forcefield) return ImpactDecision.FORCEFIELD;
		if (water) return ImpactDecision.WATER;
		if (frost) return ImpactDecision.FROST;
		return ImpactDecision.DETONATE;
	}

	/** Keeps a Cinderheart live only while every owner and expiry invariant holds. */
	public static boolean continues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen,
			boolean ownsPower, long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened
				&& !ownerFrozen && ownsPower && currentTick < expiresAt;
	}

	private static int boundedTier(int tier) {
		return Math.max(1, Math.min(ANCIENT_MAXIMUM_TIER, tier));
	}

	private static long saturatingAdd(long value, long increment) {
		if (increment > 0L && value > Long.MAX_VALUE - increment) return Long.MAX_VALUE;
		return value + increment;
	}
}
