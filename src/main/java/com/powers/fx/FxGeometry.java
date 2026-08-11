package com.powers.fx;

import com.powers.magic.fx.FxMotif;
import com.powers.magic.fx.FxOrientation;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure deterministic geometry generator for bounded client particle choreography. */
public final class FxGeometry {
	public static final int MAX_POINTS = 96;
	public static final int MAX_POOLED_GEOMETRIES = 256;
	private static final double MAX_DISTANCE = 128.0;
	private record GeometryKey(FxMotif motif, int phaseSeed, int intensity, int count) { }
	private static final Map<GeometryKey, List<Point>> POOL = new LinkedHashMap<>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<GeometryKey, List<Point>> eldest) {
			return size() > MAX_POOLED_GEOMETRIES;
		}
	};

	private FxGeometry() {
	}

	/** Returns a finite local-space point cloud for the requested motif. */
	public static List<Point> points(FxMotif motif, int seed, int intensity, int requestedBudget) {
		int count = Math.clamp(requestedBudget, 0, MAX_POINTS);
		if (count == 0) return List.of();
		int phaseSeed = Math.floorMod(seed, 360);
		int boundedIntensity = Math.clamp(intensity, 1, 5);
		GeometryKey key = new GeometryKey(motif, phaseSeed, boundedIntensity, count);
		synchronized (POOL) {
			List<Point> cached = POOL.get(key);
			if (cached != null) return cached;
		}
		double phase = phaseSeed * Math.PI / 180.0;
		double scale = 0.65 + boundedIntensity * 0.18;
		List<Point> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			double progress = i / (double) Math.max(1, count - 1);
			double angle = phase + progress * Math.PI * 2.0;
			result.add(point(motif, i, progress, angle, scale));
		}
		List<Point> immutable = List.copyOf(result);
		synchronized (POOL) {
			List<Point> existing = POOL.putIfAbsent(key, immutable);
			return existing == null ? immutable : existing;
		}
	}

	public static int poolSize() {
		synchronized (POOL) {
			return POOL.size();
		}
	}

	/** Uses static, shape-distinct sigils when reduced motion is requested. */
	public static FxMotif accessibleMotif(FxMotif motif, boolean reducedMotion) {
		return motif.accessible(reducedMotion);
	}

	/** Applies intensity, distance, and accessibility scale to the hard point cap. */
	public static int budget(double distance, int intensity, double effectScale) {
		if (!Double.isFinite(distance) || distance >= MAX_DISTANCE || effectScale <= 0.0) return 0;
		double distanceFactor = distance <= 24.0 ? 1.0 : Math.max(0.1, 1.0 - (distance - 24.0) / 112.0);
		int base = 12 + Math.clamp(intensity, 1, 5) * 16;
		return Math.clamp((int) Math.round(base * distanceFactor * Math.clamp(effectScale, 0.0, 1.0)), 0, MAX_POINTS);
	}

	/** Applies a choreography radius to a finite local-space point. */
	public static Point scale(Point point, double scale) {
		if (point == null || !point.finite()) throw new IllegalArgumentException("FX point must be finite");
		if (!Double.isFinite(scale) || scale < 0.0) {
			throw new IllegalArgumentException("FX geometry scale must be finite and non-negative");
		}
		return new Point(point.x() * scale, point.y() * scale, point.z() * scale);
	}

	/** Rotates a finite point into its resolved world-space presentation plane. */
	public static Point transform(Point point, FxOrientation orientation, double angleRadians) {
		if (point == null || !point.finite()) throw new IllegalArgumentException("FX point must be finite");
		if (orientation == null) throw new IllegalArgumentException("FX orientation is required");
		if (!Double.isFinite(angleRadians)) throw new IllegalArgumentException("FX angle must be finite");
		return switch (orientation) {
			case NATIVE -> point;
			case GROUND -> new Point(point.x(), point.z(), point.y());
			case BILLBOARD -> {
				double cosine = Math.cos(angleRadians);
				double sine = Math.sin(angleRadians);
				yield new Point(point.x() * cosine - point.z() * sine, point.y(),
						point.x() * sine + point.z() * cosine);
			}
			case AUTO -> throw new IllegalArgumentException("Automatic FX orientation must be resolved first");
		};
	}

	private static Point point(FxMotif motif, int index, double progress, double angle, double scale) {
		return switch (motif) {
			case RING -> new Point(Math.cos(angle) * scale, 0.0, Math.sin(angle) * scale);
			case SPIRAL -> new Point(Math.cos(angle * 2.0) * scale, progress * 1.8 - 0.5,
					Math.sin(angle * 2.0) * scale);
			case TETHER -> new Point((progress - 0.5) * scale * 2.2,
					Math.sin(angle * 3.0) * 0.12, Math.cos(angle * 2.0) * 0.12);
			case FORK -> new Point((progress - 0.5) * scale,
					progress * 1.8 - 0.6, ((index % 3) - 1) * 0.16 * scale);
			case SHARD -> new Point(Math.cos(angle) * scale * progress,
					(index % 4) * 0.16 - 0.25, Math.sin(angle) * scale * progress);
			case GLYPH -> glyph(index, angle, scale);
			case ROOT -> new Point(Math.cos(angle * 1.5) * scale * progress,
					-progress * 0.35, Math.sin(angle * 1.5) * scale * progress);
			case ECLIPSE -> new Point(Math.cos(angle) * scale,
					Math.sin(angle) * scale, (index % 2 == 0 ? -0.06 : 0.06));
			case FRACTURE -> new Point(Math.cos(angle) * scale * progress,
					Math.sin(angle * 3.0) * scale * 0.5, Math.sin(angle) * scale * progress);
		};
	}

	private static Point glyph(int index, double angle, double scale) {
		double radius = index % 4 == 0 ? scale * 0.35 : scale;
		return new Point(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0);
	}

	/** Immutable local-space sample. */
	public record Point(double x, double y, double z) {
		public boolean finite() {
			return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
		}
	}
}
