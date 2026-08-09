package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure, finite rules for one server-owned Astral Convergence storm. */
public final class StarfallRules {
	private static final int OMEN_TICKS = 20;
	private static final int STRIKE_INTERVAL = 6;
	private static final int BASE_STRIKES = 8;
	private static final int VARIANT_STRIKES = 2;
	private static final int MAX_STRIKES = 12;
	private static final int REPEAT_INTERVAL = 12;
	private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;

	/** Readable phase of the authored storm timeline. */
	public enum Phase {
		OMEN,
		RAIN,
		CROWN,
		FINISHED
	}

	/** Material or protection outcome resolved before an impact mutates a body. */
	public enum Counterplay {
		STRIKE,
		UNOWNED,
		UNLOADED,
		SAFE_ZONE,
		AMETHYST,
		SANCTUARY,
		KINETIC_WARD,
		DARKNESS,
		WATER,
		PURE_LIGHT,
		FORCEFIELD,
		TIME_LOCK,
		BODY_ANCHOR,
		RESISTED
	}

	private StarfallRules() {
	}

	/** Returns the fixed regular-strike budget after independent rank bonuses. */
	public static int strikeCount(boolean empoweredImpact, boolean ancientMastery) {
		int result = BASE_STRIKES;
		if (empoweredImpact) result += VARIANT_STRIKES;
		if (ancientMastery) result += VARIANT_STRIKES;
		return Math.min(MAX_STRIKES, result);
	}

	/** Returns the age of one zero-based regular strike. */
	public static int strikeAge(int index) {
		if (index < 0) return Integer.MAX_VALUE;
		long age = OMEN_TICKS + (long) index * STRIKE_INTERVAL;
		return age > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) age;
	}

	/** Returns how many regular strikes should have resolved by this age. */
	public static int strikesDue(int age, int count) {
		if (age < OMEN_TICKS || count <= 0) return 0;
		return Math.min(Math.min(MAX_STRIKES, count), 1 + (age - OMEN_TICKS) / STRIKE_INTERVAL);
	}

	/** Returns the mastered crown age, or an unreachable deadline without mastery. */
	public static long crownAge(int strikeCount, boolean ancientMastery) {
		if (!ancientMastery || strikeCount <= 0) return Long.MAX_VALUE;
		return (long) strikeAge(Math.min(MAX_STRIKES, strikeCount) - 1) + 8L;
	}

	/** Returns the exclusive authored completion age. */
	public static long finishAge(int strikeCount, boolean ancientMastery) {
		if (strikeCount <= 0) return 0L;
		long lastStrike = strikeAge(Math.min(MAX_STRIKES, strikeCount) - 1);
		return lastStrike + (ancientMastery ? 16L : 10L);
	}

	/** Classifies one age without depending on mutable runtime state. */
	public static Phase phase(int age, int strikeCount, boolean ancientMastery) {
		if (age >= finishAge(strikeCount, ancientMastery)) return Phase.FINISHED;
		if (age < OMEN_TICKS) return Phase.OMEN;
		if (ancientMastery && age >= crownAge(strikeCount, true)) return Phase.CROWN;
		return Phase.RAIN;
	}

	/** Generates one deterministic golden-angle point strictly inside the storm disc. */
	public static Vec3 strikeOffset(long seed, int index, int count, double radius) {
		if (index < 0 || count <= 0 || index >= count || !Double.isFinite(radius) || radius <= 0.0) {
			return Vec3.ZERO;
		}
		double phase = unsignedUnit(mix(seed)) * Math.PI * 2.0;
		double radial = radius * Math.sqrt((index + 0.5) / Math.min(MAX_STRIKES, (double) count));
		double angle = phase + index * GOLDEN_ANGLE;
		return new Vec3(Math.cos(angle) * radial, 0.0, Math.sin(angle) * radial);
	}

	/** Returns the mirrored, inward Communion echo point for a regular strike. */
	public static Vec3 echoOffset(Vec3 regularOffset) {
		return finite(regularOffset) ? new Vec3(-regularOffset.x * 0.62, 0.0,
				-regularOffset.z * 0.62) : Vec3.ZERO;
	}

	/** Resolves environment counterplay in stable protection-first order. */
	public static Counterplay impactDecision(boolean owned, boolean loaded,
			boolean safeZone, boolean amethyst, boolean sanctuary, boolean kineticWard,
			boolean darkness, boolean water, boolean pureLight) {
		if (!owned) return Counterplay.UNOWNED;
		if (!loaded) return Counterplay.UNLOADED;
		if (safeZone) return Counterplay.SAFE_ZONE;
		if (amethyst) return Counterplay.AMETHYST;
		if (sanctuary) return Counterplay.SANCTUARY;
		if (kineticWard) return Counterplay.KINETIC_WARD;
		if (darkness) return Counterplay.DARKNESS;
		if (water) return Counterplay.WATER;
		if (pureLight) return Counterplay.PURE_LIGHT;
		return Counterplay.STRIKE;
	}

	/** Scales one strike by sequence, crown, Might, and resolved material. */
	public static double damageMultiplier(int index, boolean crown,
			boolean empoweredImpact, Counterplay medium) {
		double result = crown ? 1.75 : 0.82 + Math.min(7, Math.max(0, index)) * 0.045;
		if (empoweredImpact) result *= 1.20;
		if (medium == Counterplay.WATER) result *= 0.70;
		if (medium == Counterplay.PURE_LIGHT) result *= 1.15;
		return result;
	}

	/** Quadratic radial falloff with a useful centre and hard boundary zero. */
	public static double falloff(double distance, double radius) {
		if (!Double.isFinite(distance) || !Double.isFinite(radius)
				|| radius <= 0.0 || distance >= radius) return 0.0;
		double remaining = 1.0 - Math.max(0.0, distance) / radius;
		return 0.35 + 0.65 * remaining * remaining;
	}

	/** Returns the bounded body query cap for one impact. */
	public static int targetLimit(boolean ancientMastery) {
		return ancientMastery ? 18 : 12;
	}

	/** Prevents rapid repeats and caps total hits from one convergence. */
	public static boolean hitAllowed(long now, long lastHit, int priorHits,
			boolean ancientMastery) {
		if (priorHits < 0 || priorHits >= (ancientMastery ? 4 : 3)) return false;
		return lastHit == Long.MIN_VALUE || now >= lastHit && now - lastHit >= REPEAT_INTERVAL;
	}

	/** Communion mirrors every third regular strike and never the crown. */
	public static boolean echoAllowed(boolean soulEcho, int regularIndex) {
		return soulEcho && regularIndex >= 0 && (regularIndex + 1) % 3 == 0;
	}

	/** Returns the impact radius after Might, crown, and water transformation. */
	public static double impactRadius(boolean empoweredImpact, boolean crown,
			Counterplay medium) {
		double radius = crown ? 5.0 : empoweredImpact ? 3.35 : 2.6;
		return medium == Counterplay.WATER ? radius * 1.5 : radius;
	}

	/** Moves a Motion-ranked storm toward its original target under step and leash caps. */
	public static Vec3 trackedCenter(Vec3 current, Vec3 desired, Vec3 origin,
			boolean secondStep, double maximumLeash, double maximumStep) {
		if (!secondStep || !finite(current) || !finite(desired) || !finite(origin)
				|| !Double.isFinite(maximumLeash) || maximumLeash < 0.0
				|| !Double.isFinite(maximumStep) || maximumStep <= 0.0
				|| desired.distanceToSqr(origin) > maximumLeash * maximumLeash) return current;
		Vec3 delta = desired.subtract(current);
		double distanceSquared = delta.lengthSqr();
		if (distanceSquared <= maximumStep * maximumStep) return desired;
		if (distanceSquared <= MIN_LENGTH_SQUARED) return current;
		return current.add(delta.scale(maximumStep / Math.sqrt(distanceSquared)));
	}

	/** Produces finite radial shock pressure, retaining lift at the exact centre. */
	public static Vec3 pressureImpulse(Vec3 center, Vec3 target,
			double horizontalStrength, double verticalStrength) {
		if (!finite(center) || !finite(target) || !Double.isFinite(horizontalStrength)
				|| horizontalStrength < 0.0 || !Double.isFinite(verticalStrength)
				|| verticalStrength < 0.0) return Vec3.ZERO;
		Vec3 horizontal = new Vec3(target.x - center.x, 0.0, target.z - center.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) {
			return new Vec3(0.0, verticalStrength, 0.0);
		}
		horizontal = horizontal.normalize().scale(horizontalStrength);
		return new Vec3(horizontal.x, verticalStrength, horizontal.z);
	}

	/** Keeps a storm live only while every owner, power, and deadline invariant holds. */
	public static boolean stormContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen, boolean ownsPower,
			long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened
				&& !ownerFrozen && ownsPower && currentTick < expiresAt;
	}

	private static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static double unsignedUnit(long value) {
		return (value >>> 11) * 0x1.0p-53;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
