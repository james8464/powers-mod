package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/** Pure, finite rules for the server-owned Sunfire Energy Beam channel. */
public final class EnergyBeamRules {
	public static final int FOCUS_TICKS = 8;
	public static final int TOTAL_TICKS = 40;
	private static final int DAMAGE_INTERVAL = 10;
	private static final int MAX_STREAK = 3;
	private static final int MAX_SPLITS = 2;
	private static final int MAX_AUXILIARY_TARGETS = 8;
	private static final int MAX_WATER_SAMPLES = 128;
	private static final double MIN_LENGTH_SQUARED = 1.0E-12;

	/** The authoritative lifecycle phase of one beam channel. */
	public enum Phase {
		FOCUS,
		FIRING,
		FINISHED
	}

	/** A terminal material or ward that visibly transforms or stops the ray. */
	public enum Counterplay {
		SURFACE,
		WATER,
		AMETHYST,
		PURE_LIGHT,
		DARKNESS,
		KINETIC_WARD,
		SANCTUARY,
		FORCEFIELD,
		SAFE_ZONE
	}

	/** A finite candidate terminal measured from the beam origin. */
	public record Intercept(Counterplay counterplay, double distance) {
	}

	private EnergyBeamRules() {
	}

	/** Returns the channel phase using exclusive server-tick boundaries. */
	public static Phase phase(long startedAt, long currentTick) {
		long age = age(startedAt, currentTick);
		if (age < FOCUS_TICKS) return Phase.FOCUS;
		if (age < TOTAL_TICKS) return Phase.FIRING;
		return Phase.FINISHED;
	}

	/** Returns how many whole focus ticks remain before the first damage beat. */
	public static int focusRemaining(long startedAt, long currentTick) {
		return Math.max(0, FOCUS_TICKS - (int) Math.min(FOCUS_TICKS, age(startedAt, currentTick)));
	}

	/** Returns true only for the four authored damage beats after focus. */
	public static boolean damageBeat(long startedAt, long currentTick) {
		long age = age(startedAt, currentTick);
		return age >= FOCUS_TICKS && age < TOTAL_TICKS
				&& (age - FOCUS_TICKS) % DAMAGE_INTERVAL == 0L;
	}

	/** Advances a same-target scorch streak and caps it at three. */
	public static int nextStreak(boolean sameTarget, int previousStreak) {
		if (!sameTarget) return 1;
		if (previousStreak >= MAX_STREAK) return MAX_STREAK;
		return Math.max(0, previousStreak) + 1;
	}

	/** Returns the bounded damage multiplier for a scorch streak. */
	public static double scorchMultiplier(int streak) {
		return switch (Math.clamp(streak, 1, MAX_STREAK)) {
			case 2 -> 1.15;
			case 3 -> 1.30;
			default -> 1.0;
		};
	}

	/** Applies the scorch multiplier to finite non-negative base damage. */
	public static double scorchDamage(double baseDamage, int streak) {
		return validAmount(baseDamage) * scorchMultiplier(streak);
	}

	/** Escalates burn duration by fifteen ticks per repeat, capped at two repeats. */
	public static int burnTicks(int baseTicks, int streak) {
		if (baseTicks <= 0) return 0;
		long result = (long) baseTicks + 15L * (Math.clamp(streak, 1, MAX_STREAK) - 1L);
		return (int) Math.min(Integer.MAX_VALUE, result);
	}

	/** Converts a water-intercepted beat to sixty-five percent steam damage. */
	public static double steamDamage(double baseDamage) {
		return validAmount(baseDamage) * 0.65;
	}

	/** Converts Ancient Mastery forks to forty-five percent damage. */
	public static double splitDamage(double baseDamage) {
		return validAmount(baseDamage) * 0.45;
	}

	/** Returns the hard secondary-fork cap for this rank variant. */
	public static int splitLimit(boolean ancientMastery) {
		return ancientMastery ? MAX_SPLITS : 0;
	}

	/** Returns the shared cap used by steam and flare area searches. */
	public static int auxiliaryTargetLimit() {
		return MAX_AUXILIARY_TARGETS;
	}

	/** Allows at most one flare, on the first third-streak empowered impact. */
	public static boolean flareReady(boolean empoweredImpact, int streak,
			boolean alreadyFlared, int flaresUsed) {
		return empoweredImpact && streak >= MAX_STREAK && !alreadyFlared && flaresUsed <= 0;
	}

	/** Chooses the nearest valid terminal inside the ray's finite maximum distance. */
	public static Optional<Intercept> nearestTerminal(Collection<Intercept> intercepts,
			double maximumDistance) {
		if (intercepts == null || !Double.isFinite(maximumDistance) || maximumDistance < 0.0) {
			return Optional.empty();
		}
		return intercepts.stream()
				.filter(intercept -> intercept != null && intercept.counterplay() != null)
				.filter(intercept -> Double.isFinite(intercept.distance())
						&& intercept.distance() >= 0.0 && intercept.distance() <= maximumDistance)
				.min(Comparator.comparingDouble(Intercept::distance));
	}

	/** Returns half-block water samples under a strict per-ray cap. */
	public static int waterSamples(double distance) {
		if (!Double.isFinite(distance) || distance <= 0.0) return 0;
		return Math.min(MAX_WATER_SAMPLES, (int) Math.ceil(distance * 2.0));
	}

	/** Produces a bounded horizontal steam shove with a small fixed-ratio lift. */
	public static Vec3 steamImpulse(Vec3 center, Vec3 target, double force) {
		if (!finite(center) || !finite(target) || !Double.isFinite(force) || force <= 0.0) {
			return Vec3.ZERO;
		}
		Vec3 horizontal = new Vec3(target.x - center.x, 0.0, target.z - center.z);
		if (horizontal.lengthSqr() <= MIN_LENGTH_SQUARED) return Vec3.ZERO;
		horizontal = horizontal.normalize().scale(force);
		return new Vec3(horizontal.x, Math.min(0.2, force * 0.25), horizontal.z);
	}

	/** Keeps a channel alive only while all authority and suppression invariants hold. */
	public static boolean channelContinues(boolean ownerPresent, boolean sameDimension,
			boolean ownerAlive, boolean ownerDampened, boolean ownerFrozen, boolean ownsPower,
			long currentTick, long expiresAt) {
		return ownerPresent && sameDimension && ownerAlive && !ownerDampened && !ownerFrozen
				&& ownsPower && currentTick < expiresAt;
	}

	private static long age(long startedAt, long currentTick) {
		if (currentTick <= startedAt) return 0L;
		long difference = currentTick - startedAt;
		return difference < 0L ? Long.MAX_VALUE : difference;
	}

	private static double validAmount(double amount) {
		return Double.isFinite(amount) && amount > 0.0 ? amount : 0.0;
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}
