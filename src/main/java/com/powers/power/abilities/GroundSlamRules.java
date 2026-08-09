package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure, finite rules for one server-owned Faultbound Verdict rite. */
public final class GroundSlamRules {
	private static final int PRIMARY_AGE = 12;
	private static final int ECHO_AGE = 18;
	private static final int CROWN_AGE = 24;
	private static final int BASE_TARGET_LIMIT = 12;
	private static final int VARIANT_TARGET_BONUS = 6;
	private static final int MAX_TARGET_LIMIT = 24;
	private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;

	/** One authored beat in the seismic rite. */
	public enum Beat {
		PRIMARY,
		SOUL_ECHO,
		CROWN
	}

	/** Material, protection, or physical result resolved before mutation. */
	public enum Counterplay {
		IMPACT,
		UNLOADED,
		SAFE_ZONE,
		AMETHYST,
		SANCTUARY,
		KINETIC_WARD,
		FORCEFIELD,
		BODY_ANCHOR,
		TIME_LOCK,
		COLLISION,
		PROTECTED,
		WATER,
		DARKNESS,
		PURE_LIGHT,
		UNSUPPORTED,
		RESISTED
	}

	private GroundSlamRules() {
	}

	/** Returns the fixed age for one authored beat. */
	public static int beatAge(Beat beat) {
		if (beat == null) return Integer.MAX_VALUE;
		return switch (beat) {
			case PRIMARY -> PRIMARY_AGE;
			case SOUL_ECHO -> ECHO_AGE;
			case CROWN -> CROWN_AGE;
		};
	}

	/** Returns how many enabled beats should have resolved by this age. */
	public static int beatsDue(int age, boolean soulEcho, boolean ancientMastery) {
		int due = age >= PRIMARY_AGE ? 1 : 0;
		if (soulEcho && age >= ECHO_AGE) due++;
		if (ancientMastery && age >= CROWN_AGE) due++;
		return due;
	}

	/** Returns the exclusive completion age after the last enabled beat. */
	public static long finishAge(boolean soulEcho, boolean ancientMastery) {
		if (ancientMastery) return CROWN_AGE + 6L;
		if (soulEcho) return ECHO_AGE + 6L;
		return PRIMARY_AGE + 6L;
	}

	/** Returns the nearest-first body cap after independent Might and Dominion bonuses. */
	public static int targetLimit(boolean empoweredImpact, boolean ancientMastery) {
		int result = BASE_TARGET_LIMIT;
		if (empoweredImpact) result += VARIANT_TARGET_BONUS;
		if (ancientMastery) result += VARIANT_TARGET_BONUS;
		return Math.min(MAX_TARGET_LIMIT, result);
	}

	/** Allows bounded terrain only for a policy-approved primary beat. */
	public static int terrainLimit(boolean terrainAllowed, boolean primary,
			boolean ancientMastery) {
		if (!terrainAllowed || !primary) return 0;
		return ancientMastery ? 16 : 8;
	}

	/** Veil clears at most eight currently visible hostile mob memories once. */
	public static int afterimageTargetLimit(boolean afterimage) {
		return afterimage ? 8 : 0;
	}

	/** Resolves the loaded support medium in stable protection-first order. */
	public static Counterplay environmentDecision(boolean loaded, boolean safeZone,
			boolean amethyst, boolean darkness, boolean water,
			boolean pureLight, boolean supported) {
		if (!loaded) return Counterplay.UNLOADED;
		if (safeZone) return Counterplay.SAFE_ZONE;
		if (amethyst) return Counterplay.AMETHYST;
		if (darkness) return Counterplay.DARKNESS;
		if (water) return Counterplay.WATER;
		if (pureLight) return Counterplay.PURE_LIGHT;
		if (!supported) return Counterplay.UNSUPPORTED;
		return Counterplay.IMPACT;
	}

	/** Resolves body-local damage protection before calling the damage bridge. */
	public static Counterplay bodyDecision(boolean harmAllowed, boolean amethyst,
			boolean sanctuary, boolean kineticWard, boolean forcefield) {
		if (!harmAllowed) return Counterplay.SAFE_ZONE;
		if (amethyst) return Counterplay.AMETHYST;
		if (sanctuary) return Counterplay.SANCTUARY;
		if (kineticWard) return Counterplay.KINETIC_WARD;
		if (forcefield) return Counterplay.FORCEFIELD;
		return Counterplay.IMPACT;
	}

	/** Returns the radius after beat identity and Might expansion. */
	public static double impactRadius(double baseRadius, Beat beat,
			boolean empoweredImpact) {
		if (!Double.isFinite(baseRadius) || baseRadius <= 0.0 || beat == null) return 0.0;
		double beatScale = switch (beat) {
			case PRIMARY -> 1.0;
			case SOUL_ECHO -> 0.72;
			case CROWN -> 1.18;
		};
		return baseRadius * beatScale * (empoweredImpact ? 1.15 : 1.0);
	}

	/** Combines beat, Might, grounding, and support-medium damage scales. */
	public static double damageMultiplier(Beat beat, boolean empoweredImpact,
			boolean grounded, Counterplay medium) {
		if (beat == null || medium == null) return 0.0;
		double result = switch (beat) {
			case PRIMARY -> 1.0;
			case SOUL_ECHO -> 0.42;
			case CROWN -> 0.80;
		};
		if (empoweredImpact) result *= 1.20;
		if (!grounded) result *= 0.35;
		if (medium == Counterplay.WATER) result *= 0.65;
		if (medium == Counterplay.DARKNESS) result *= 0.75;
		if (medium == Counterplay.PURE_LIGHT) result *= 1.10;
		return result;
	}

	/** Lets wet footing soften an ordinary beat without overriding realm matter. */
	public static Counterplay targetMedium(Counterplay environment, boolean targetWet) {
		if (environment == null) return Counterplay.UNSUPPORTED;
		return targetWet && environment == Counterplay.IMPACT
				? Counterplay.WATER : environment;
	}

	/** Returns the beat and medium scale for consent-safe seismic pressure. */
	public static double pressureMultiplier(Beat beat, boolean empoweredImpact,
			boolean grounded, Counterplay medium) {
		if (beat == null || medium == null) return 0.0;
		double result = switch (beat) {
			case PRIMARY -> 1.0;
			case SOUL_ECHO -> 0.52;
			case CROWN -> 1.35;
		};
		if (empoweredImpact) result *= 1.20;
		if (!grounded) result *= 0.40;
		if (medium == Counterplay.WATER) result *= 0.50;
		if (medium == Counterplay.DARKNESS) result *= 1.15;
		if (medium == Counterplay.PURE_LIGHT) result *= 0.90;
		return result;
	}

	/** Quadratic centre-to-edge falloff with a thirty-percent interior floor. */
	public static double falloff(double distance, double radius) {
		if (!Double.isFinite(distance) || distance < 0.0
				|| !Double.isFinite(radius) || radius <= 0.0 || distance >= radius) return 0.0;
		double remaining = 1.0 - distance / radius;
		return 0.30 + 0.70 * remaining * remaining;
	}

	/** Resolves every forced-movement counter before any velocity write. */
	public static Counterplay pressureDecision(boolean movementAllowed, boolean amethyst,
			boolean bodyAnchor, boolean forcefield, boolean spellWard,
			boolean frozen, boolean clearPath) {
		if (!movementAllowed) return Counterplay.PROTECTED;
		if (amethyst) return Counterplay.AMETHYST;
		if (bodyAnchor) return Counterplay.BODY_ANCHOR;
		if (forcefield) return Counterplay.FORCEFIELD;
		if (spellWard) return Counterplay.KINETIC_WARD;
		if (frozen) return Counterplay.TIME_LOCK;
		if (!clearPath) return Counterplay.COLLISION;
		return Counterplay.IMPACT;
	}

	/** Produces finite outward-and-upward pressure, retaining lift at the centre. */
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

	/** Moves a Motion omen toward the caster under explicit step and origin-leash caps. */
	public static Vec3 trackedCenter(Vec3 current, Vec3 desired, Vec3 origin,
			boolean secondStep, double maximumLeash, double maximumStep) {
		if (!secondStep || !finite(current) || !finite(desired) || !finite(origin)
				|| !Double.isFinite(maximumLeash) || maximumLeash < 0.0
				|| !Double.isFinite(maximumStep) || maximumStep <= 0.0
				|| desired.distanceToSqr(origin) > maximumLeash * maximumLeash) return current;
		Vec3 delta = desired.subtract(current);
		if (delta.lengthSqr() <= maximumStep * maximumStep) return desired;
		if (delta.lengthSqr() <= MIN_LENGTH_SQUARED) return current;
		return current.add(delta.scale(maximumStep / delta.length()));
	}

	/** Authors one deterministic golden-angle terrain sample inside the radius. */
	public static Vec3 terrainOffset(int index, int count, double radius) {
		if (index < 0 || count <= 0 || index >= count
				|| !Double.isFinite(radius) || radius <= 0.0) return Vec3.ZERO;
		double radial = radius * Math.sqrt((index + 0.5) / count);
		double angle = index * GOLDEN_ANGLE + Math.PI / 8.0;
		return new Vec3(Math.cos(angle) * radial, 0.0, Math.sin(angle) * radial);
	}

	/** Offsets Communion's echo toward the cast-facing edge without exceeding the field. */
	public static Vec3 echoCenter(Vec3 center, Vec3 lookDirection, double radius) {
		if (!finite(center)) return Vec3.ZERO;
		if (!finite(lookDirection)
				|| !Double.isFinite(radius) || radius <= 0.0) return center;
		Vec3 horizontal = new Vec3(lookDirection.x, 0.0, lookDirection.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) return center;
		return center.add(horizontal.normalize().scale(Math.min(3.5, radius * 0.55)));
	}

	/** Keeps a rite live only while every owner, power, and deadline invariant holds. */
	public static boolean riteContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen,
			boolean ownsPower, long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened
				&& !ownerFrozen && ownsPower && currentTick < expiresAt;
	}

	/** Returns whether an environment result still permits a transformed impact. */
	public static boolean impactAllowed(Counterplay counterplay) {
		return counterplay == Counterplay.IMPACT || counterplay == Counterplay.WATER
				|| counterplay == Counterplay.DARKNESS
				|| counterplay == Counterplay.PURE_LIGHT;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
