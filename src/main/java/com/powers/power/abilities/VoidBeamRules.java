package com.powers.power.abilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure timing, geometry, penetration, and safety bounds for Void Beam. */
public final class VoidBeamRules {
	public static final int CHARGE_TICKS = 12;
	public static final int MAX_PENETRATIONS = 5;
	public static final int MAX_ACTIVE_SCARS = 128;
	public static final int MAX_SCAR_TARGETS = 16;

	/** Visually distinct reasons why an abyssal ray stopped without a scar. */
	public enum Counterplay {
		NONE,
		LIGHT,
		AMETHYST,
		KINETIC_WARD,
		SANCTUARY,
		FORCEFIELD,
		SAFE_ZONE
	}

	/** One real ray intersection paired with its distance from the origin. */
	public record RayCandidate<T>(T target, double distance) {
	}

	private VoidBeamRules() {
	}

	/** Returns a saturating charge countdown; zero means release is legal now. */
	public static int chargeRemaining(long startedAt, long now) {
		long safeStart = Math.max(0L, startedAt);
		long releaseAt = safeStart > Long.MAX_VALUE - CHARGE_TICKS
				? Long.MAX_VALUE : safeStart + CHARGE_TICKS;
		return (int) Math.min(CHARGE_TICKS, Math.max(0L, releaseAt - Math.max(0L, now)));
	}

	/** Adds named rank bores while retaining a hard multiplayer target cap. */
	public static int penetrationLimit(boolean empoweredImpact, boolean ancientMastery) {
		return Math.min(MAX_PENETRATIONS,
				3 + (empoweredImpact ? 1 : 0) + (ancientMastery ? 1 : 0));
	}

	/** Returns first/second/later target damage falloff for an ordered ray. */
	public static double damageMultiplier(int penetrationIndex) {
		if (penetrationIndex < 0) return 0.0;
		if (penetrationIndex == 0) return 1.0;
		return penetrationIndex == 1 ? 0.72 : 0.52;
	}

	/**
	 * Filters malformed/out-of-segment intersections, sorts them nearest-first,
	 * removes duplicate targets, and enforces both requested and global caps.
	 */
	public static <T> List<RayCandidate<T>> selectPenetrations(
			Collection<RayCandidate<T>> candidates, double terminalDistance, int requestedLimit) {
		Objects.requireNonNull(candidates, "candidates");
		if (!Double.isFinite(terminalDistance) || terminalDistance < 0.0 || requestedLimit <= 0) {
			return List.of();
		}
		List<RayCandidate<T>> ordered = candidates.stream()
				.filter(Objects::nonNull)
				.filter(candidate -> candidate.target() != null
						&& Double.isFinite(candidate.distance()) && candidate.distance() >= 0.0
						&& candidate.distance() <= terminalDistance)
				.sorted(Comparator.comparingDouble(RayCandidate::distance))
				.toList();
		int limit = Math.min(MAX_PENETRATIONS, requestedLimit);
		List<RayCandidate<T>> result = new ArrayList<>(Math.min(limit, ordered.size()));
		Set<T> seen = new LinkedHashSet<>();
		for (RayCandidate<T> candidate : ordered) {
			if (!seen.add(candidate.target())) continue;
			result.add(candidate);
			if (result.size() == limit) break;
		}
		return List.copyOf(result);
	}

	/**
	 * Returns distance from segment start to its first sphere entry, zero when
	 * starting inside, or {@link Double#NaN} when the finite segment misses.
	 */
	public static double segmentSphereEntry(double startX, double startY, double startZ,
			double endX, double endY, double endZ, double centerX, double centerY,
			double centerZ, double radius) {
		double[] values = {startX, startY, startZ, endX, endY, endZ,
				centerX, centerY, centerZ, radius};
		for (double value : values) if (!Double.isFinite(value)) return Double.NaN;
		if (radius <= 0.0) return Double.NaN;
		double dx = endX - startX;
		double dy = endY - startY;
		double dz = endZ - startZ;
		double fx = startX - centerX;
		double fy = startY - centerY;
		double fz = startZ - centerZ;
		double radiusSquared = radius * radius;
		if (fx * fx + fy * fy + fz * fz <= radiusSquared) return 0.0;
		double segmentSquared = dx * dx + dy * dy + dz * dz;
		if (segmentSquared <= 1.0E-12) return Double.NaN;
		double b = 2.0 * (fx * dx + fy * dy + fz * dz);
		double c = fx * fx + fy * fy + fz * fz - radiusSquared;
		double discriminant = b * b - 4.0 * segmentSquared * c;
		if (discriminant < 0.0) return Double.NaN;
		double root = Math.sqrt(discriminant);
		double first = (-b - root) / (2.0 * segmentSquared);
		double second = (-b + root) / (2.0 * segmentSquared);
		double parameter = first >= 0.0 && first <= 1.0 ? first
				: second >= 0.0 && second <= 1.0 ? second : Double.NaN;
		return Double.isNaN(parameter) ? Double.NaN : parameter * Math.sqrt(segmentSquared);
	}

	/** Draws scars only on positive five-tick beats. */
	public static boolean shouldRenderScar(int ageTicks) {
		return ageTicks > 0 && ageTicks % 5 == 0;
	}

	/** Applies scar gameplay only on positive ten-tick beats. */
	public static boolean shouldPulseScar(int ageTicks) {
		return ageTicks > 0 && ageTicks % 10 == 0;
	}

	/** Sanitizes interaction-scaled scar radii to the server-safe 1..4 range. */
	public static double scarRadius(double scaledRadius) {
		if (!Double.isFinite(scaledRadius)) return 1.0;
		return Math.max(1.0, Math.min(4.0, scaledRadius));
	}

	/** Applies Ancient Mastery's 25% persistence and clamps to 1..8 seconds. */
	public static int scarDuration(int scaledTicks, boolean ancientMastery) {
		long safe = Math.max(20L, scaledTicks);
		if (ancientMastery) safe = (safe * 5L + 3L) / 4L;
		return (int) Math.max(20L, Math.min(160L, safe));
	}
}
