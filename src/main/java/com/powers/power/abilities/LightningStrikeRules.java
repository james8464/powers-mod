package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure, finite timing, protection, conduction, and rank rules for one Storm Tribunal. */
public final class LightningStrikeRules {
	private static final int PRIMARY_AGE = 8;
	private static final int CROWN_AGE = 12;
	private static final int BASE_TARGET_LIMIT = 8;
	private static final int VARIANT_TARGET_BONUS = 4;
	private static final int MAX_TARGET_LIMIT = 16;
	private static final int BASE_CHAIN_LIMIT = 3;
	private static final int MAX_CHAIN_LIMIT = 5;
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;

	/** One authored damaging beat in the tribunal. */
	public enum Beat {
		PRIMARY,
		CROWN
	}

	/** Environment, protection, or physical result resolved before mutation. */
	public enum Counterplay {
		STRIKE,
		UNOWNED,
		UNLOADED,
		SAFE_ZONE,
		AMETHYST,
		SANCTUARY,
		KINETIC_WARD,
		FORCEFIELD,
		BODY_ANCHOR,
		TIME_LOCK,
		DARKNESS,
		WATER,
		PURE_LIGHT,
		ROOF,
		RESISTED
	}

	private LightningStrikeRules() {
	}

	/** Returns the fixed age of an authored primary or Dominion beat. */
	public static int beatAge(Beat beat) {
		if (beat == null) return Integer.MAX_VALUE;
		return beat == Beat.PRIMARY ? PRIMARY_AGE : CROWN_AGE;
	}

	/** Returns how many enabled beats should have resolved by this age. */
	public static int beatsDue(int age, boolean ancientMastery) {
		int due = age >= PRIMARY_AGE ? 1 : 0;
		if (ancientMastery && age >= CROWN_AGE) due++;
		return due;
	}

	/** Returns the exclusive state expiry after the last enabled beat. */
	public static long finishAge(boolean ancientMastery) {
		return ancientMastery ? CROWN_AGE + 6L : PRIMARY_AGE + 5L;
	}

	/** Caps radial body work after independent Might and Dominion bonuses. */
	public static int targetLimit(boolean empoweredImpact, boolean ancientMastery) {
		int result = BASE_TARGET_LIMIT;
		if (empoweredImpact) result += VARIANT_TARGET_BONUS;
		if (ancientMastery) result += VARIANT_TARGET_BONUS;
		return Math.min(MAX_TARGET_LIMIT, result);
	}

	/** Caps one unique wet chain after Might and Dominion bonuses. */
	public static int chainLimit(boolean empoweredImpact, boolean ancientMastery) {
		int result = BASE_CHAIN_LIMIT + (empoweredImpact ? 1 : 0)
				+ (ancientMastery ? 1 : 0);
		return Math.min(MAX_CHAIN_LIMIT, result);
	}

	/** Wardcraft grounds at most eight hostile projectiles during one warning. */
	public static int projectileLimit(boolean reflectiveWard) {
		return reflectiveWard ? 8 : 0;
	}

	/** Veil clears at most six visible hostile mob memories once. */
	public static int afterimageTargetLimit(boolean afterimage) {
		return afterimage ? 6 : 0;
	}

	/** Resolves the strike column in stable ownership- and protection-first order. */
	public static Counterplay environmentDecision(boolean owned, boolean loaded,
			boolean safeZone, boolean amethyst, boolean sanctuary,
			boolean kineticWard, boolean darkness, boolean water,
			boolean pureLight, boolean roof) {
		if (!owned) return Counterplay.UNOWNED;
		if (!loaded) return Counterplay.UNLOADED;
		if (safeZone) return Counterplay.SAFE_ZONE;
		if (amethyst) return Counterplay.AMETHYST;
		if (sanctuary) return Counterplay.SANCTUARY;
		if (kineticWard) return Counterplay.KINETIC_WARD;
		if (darkness) return Counterplay.DARKNESS;
		if (water) return Counterplay.WATER;
		if (pureLight) return Counterplay.PURE_LIGHT;
		if (roof) return Counterplay.ROOF;
		return Counterplay.STRIKE;
	}

	/** Resolves target-local damage protection before the damage bridge is called. */
	public static Counterplay bodyDecision(boolean harmAllowed, boolean amethyst,
			boolean sanctuary, boolean kineticWard, boolean forcefield) {
		if (!harmAllowed) return Counterplay.SAFE_ZONE;
		if (amethyst) return Counterplay.AMETHYST;
		if (sanctuary) return Counterplay.SANCTUARY;
		if (kineticWard) return Counterplay.KINETIC_WARD;
		if (forcefield) return Counterplay.FORCEFIELD;
		return Counterplay.STRIKE;
	}

	/** Keeps vulnerable projection bodies damageable while blocking chained secondary magic. */
	public static Counterplay secondaryDecision(boolean damageSucceeded,
			boolean bodyAnchor, boolean frozen) {
		if (!damageSucceeded) return Counterplay.RESISTED;
		if (bodyAnchor) return Counterplay.BODY_ANCHOR;
		if (frozen) return Counterplay.TIME_LOCK;
		return Counterplay.STRIKE;
	}

	/** Returns whether an environment result still permits a transformed impact. */
	public static boolean impactAllowed(Counterplay counterplay) {
		return counterplay == Counterplay.STRIKE || counterplay == Counterplay.WATER
				|| counterplay == Counterplay.PURE_LIGHT || counterplay == Counterplay.ROOF;
	}

	/** Returns impact radius after beat, Might, and conductive-water expansion. */
	public static double impactRadius(double baseRadius, Beat beat,
			boolean empoweredImpact, Counterplay medium) {
		if (!Double.isFinite(baseRadius) || baseRadius <= 0.0
				|| beat == null || medium == null) return 0.0;
		double scale = beat == Beat.CROWN ? 1.25 : 1.0;
		if (empoweredImpact) scale *= 1.18;
		if (medium == Counterplay.WATER) scale *= 1.45;
		return baseRadius * scale;
	}

	/** Combines beat, Might, and impact medium into one bounded damage scale. */
	public static double damageMultiplier(Beat beat, boolean empoweredImpact,
			Counterplay medium) {
		if (beat == null || medium == null || !impactAllowed(medium)) return 0.0;
		double result = beat == Beat.CROWN ? 0.55 : 1.0;
		if (empoweredImpact) result *= 1.20;
		if (medium == Counterplay.WATER) result *= 0.72;
		if (medium == Counterplay.PURE_LIGHT) result *= 1.15;
		if (medium == Counterplay.ROOF) result *= 0.85;
		return result;
	}

	/** Quadratic radial falloff with a forty-percent interior floor. */
	public static double falloff(double distance, double radius) {
		if (!Double.isFinite(distance) || distance < 0.0
				|| !Double.isFinite(radius) || radius <= 0.0 || distance >= radius) return 0.0;
		double remaining = 1.0 - distance / radius;
		return 0.40 + 0.60 * remaining * remaining;
	}

	/** Returns globally authored attenuation for one zero-based chain link. */
	public static double chainDamageMultiplier(int linkIndex) {
		if (linkIndex < 0) return 0.0;
		if (linkIndex == 0) return 0.62;
		if (linkIndex == 1) return 0.46;
		if (linkIndex == 2) return 0.34;
		return 0.26;
	}

	/** Communion's one branch carries less than two-fifths primary damage. */
	public static double forkDamageMultiplier() {
		return 0.38;
	}

	/** Communion may fork exactly once from the second successful chain node. */
	public static boolean forkAllowed(boolean soulEcho, int successfulLinkIndex) {
		return soulEcho && successfulLinkIndex == 1;
	}

	/** Returns the finite nearest-neighbour range after Might expansion. */
	public static double chainRange(boolean empoweredImpact) {
		return empoweredImpact ? 7.5 : 6.0;
	}

	/** Requires every chain node to be wet, loaded, unique, nearby, and unprotected. */
	public static boolean chainEligible(boolean wet, boolean loaded, boolean seen,
			double distance, double maximumRange, Counterplay counterplay) {
		return wet && loaded && !seen && Double.isFinite(distance) && distance >= 0.0
				&& Double.isFinite(maximumRange) && maximumRange > 0.0
				&& distance <= maximumRange && counterplay == Counterplay.STRIKE;
	}

	/** Moves a Motion omen toward its original target under step and leash caps. */
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

	/** Grounds a projectile without reflection, acceleration, or ownership transfer. */
	public static Vec3 groundedProjectileVelocity(Vec3 velocity, double fallSpeed) {
		if (!finite(velocity) || !Double.isFinite(fallSpeed) || fallSpeed <= 0.0) {
			return Vec3.ZERO;
		}
		return new Vec3(velocity.x * 0.35, -fallSpeed, velocity.z * 0.35);
	}

	/** Keeps a tribunal live only while every owner, power, and deadline invariant holds. */
	public static boolean tribunalContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen,
			boolean ownsPower, long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened
				&& !ownerFrozen && ownsPower && currentTick < expiresAt;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
