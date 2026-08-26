package com.powers.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure camera-facing expansion of immutable rank-ten primitives into bounded coloured vertices. */
public final class RankTenSilhouetteGeometry {
	public static final int MAX_VERTICES = 256;
	public static final double MAX_WORLD_COORDINATE = 30_000_000.0;
	public static final int AUTHORED_LIFETIME_TICKS = ClientRankTenSilhouetteState.AUTHORED_LIFETIME_TICKS;
	private static final int DISC_SEGMENTS = 8;

	private RankTenSilhouetteGeometry() {
	}

	/** Expands one profile into camera-stable world-space ribbons and filled accents. */
	public static Mesh mesh(RankTenSilhouetteProfile profile, Event event, Camera camera,
			boolean reducedMotion) {
		Objects.requireNonNull(profile, "profile");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(camera, "camera");
		if (event.profileId() != profile.networkId()) {
			throw new IllegalArgumentException("event/profile identity mismatch");
		}
		RankTenSilhouetteProfile.Palette palette = profile.alignmentPalette(event.alignmentId() == 0);
		double phase = reducedMotion ? 0.0 : event.phase();
		double outlineAlpha = palette.fillAlpha();
		double fillAlpha = reducedMotion ? palette.fillAlpha() * 0.62 : palette.fillAlpha();
		Basis basis = basis(event, camera);
		List<Vertex> vertices = new ArrayList<>();
		for (RankTenSilhouetteProfile.Primitive primitive : profile.primitives()) {
			if (primitive instanceof RankTenSilhouetteProfile.Segment segment) {
				segment(vertices, segment, event, basis, palette.outerRgb(), outlineAlpha);
			} else if (primitive instanceof RankTenSilhouetteProfile.Ring ring) {
				ring(vertices, ring, event, basis, palette.accentRgb(), outlineAlpha);
			} else if (primitive instanceof RankTenSilhouetteProfile.Disc disc) {
				disc(vertices, disc, event, basis, palette.fillRgb(), fillAlpha);
			}
		}
		return new Mesh(vertices, profile.primitiveSignature(), profile.primitiveSignature(), phase, fillAlpha);
	}

	/** Clamps the only renderer scale surface to the shared hard presentation bounds. */
	public static double clampScale(double scale) {
		if (!Double.isFinite(scale)) throw new IllegalArgumentException("non-finite silhouette scale");
		return Math.clamp(scale, 0.25, 8.0);
	}

	private static Basis basis(Event event, Camera camera) {
		double dx = camera.x - event.x;
		double dz = camera.z - event.z;
		double length = Math.hypot(dx, dz);
		if (length < 1.0E-6) {
			double yaw = Math.toRadians(event.yaw);
			dx = -Math.sin(yaw);
			dz = Math.cos(yaw);
			length = 1.0;
		}
		return new Basis(-dz / length, dx / length);
	}

	private static void segment(List<Vertex> target, RankTenSilhouetteProfile.Segment line,
			Event event, Basis basis, int rgb, double alpha) {
		double dx = line.x2() - line.x1();
		double dy = line.y2() - line.y1();
		double length = Math.hypot(dx, dy);
		double half = line.width() / 2.0;
		double px = -dy / length * half;
		double py = dx / length * half;
		append(target, line.x1() + px, line.y1() + py, event, basis, rgb, alpha);
		append(target, line.x1() - px, line.y1() - py, event, basis, rgb, alpha);
		append(target, line.x2() - px, line.y2() - py, event, basis, rgb, alpha);
		append(target, line.x2() + px, line.y2() + py, event, basis, rgb, alpha);
	}

	private static void ring(List<Vertex> target, RankTenSilhouetteProfile.Ring ring,
			Event event, Basis basis, int rgb, double alpha) {
		for (int index = 0; index < ring.segments(); index++) {
			double first = index * Math.PI * 2.0 / ring.segments();
			double second = (index + 1) * Math.PI * 2.0 / ring.segments();
			segment(target, new RankTenSilhouetteProfile.Segment(
					ring.x() + Math.cos(first) * ring.radius(), ring.y() + Math.sin(first) * ring.radius(),
					ring.x() + Math.cos(second) * ring.radius(), ring.y() + Math.sin(second) * ring.radius(),
					ring.width()), event, basis, rgb, alpha);
		}
	}

	private static void disc(List<Vertex> target, RankTenSilhouetteProfile.Disc disc,
			Event event, Basis basis, int rgb, double alpha) {
		for (int index = 0; index < DISC_SEGMENTS; index++) {
			double first = index * Math.PI * 2.0 / DISC_SEGMENTS;
			double second = (index + 1) * Math.PI * 2.0 / DISC_SEGMENTS;
			append(target, disc.x(), disc.y(), event, basis, rgb, alpha);
			append(target, disc.x() + Math.cos(first) * disc.radius(),
					disc.y() + Math.sin(first) * disc.radius(), event, basis, rgb, alpha);
			append(target, disc.x() + Math.cos(second) * disc.radius(),
					disc.y() + Math.sin(second) * disc.radius(), event, basis, rgb, alpha);
		}
	}

	private static void append(List<Vertex> target, double localX, double localY,
			Event event, Basis basis, int rgb, double alpha) {
		if (target.size() >= MAX_VERTICES) throw new IllegalArgumentException("silhouette vertex cap");
		target.add(new Vertex((float) (event.x + localX * basis.x), (float) (event.y + localY),
				(float) (event.z + localX * basis.z), rgb << 8 | (int) Math.round(alpha * 255.0)));
	}

	private record Basis(double x, double z) {
	}

	public record Camera(double x, double y, double z) {
		public Camera {
			if (!finite(x, y, z)) throw new IllegalArgumentException("non-finite camera");
		}
	}

	/** Semantic geometry input: no renderer or Minecraft client classes enter this boundary. */
	public record Event(long eventId, int profileId, UUID caster, String dimension,
			double x, double y, double z, float yaw, float pitch, int alignmentId,
			int visualSeed, int lifetimeTicks, double phase) {
		public Event {
			if (eventId <= 0 || RankTenSilhouetteProfile.fromNetworkId(profileId).isEmpty()
					|| caster == null || !validDimension(dimension) || !validWorldCoordinates(x, y, z)
					|| !finite(yaw, pitch, phase) || (alignmentId != 0 && alignmentId != 1)
					|| lifetimeTicks < ClientRankTenSilhouetteState.MIN_LIFETIME_TICKS
					|| lifetimeTicks > ClientRankTenSilhouetteState.MAX_LIFETIME_TICKS) {
				throw new IllegalArgumentException("invalid silhouette event");
			}
		}
		public Event(long eventId, int profileId, UUID caster, String dimension,
				double x, double y, double z, float yaw, float pitch, int alignmentId,
				int visualSeed, int lifetimeTicks) {
			this(eventId, profileId, caster, dimension, x, y, z, yaw, pitch, alignmentId,
					visualSeed, lifetimeTicks, 0.0);
		}
		public Event(long eventId, int profileId, UUID caster, String dimension,
				double x, double y, double z, float yaw, float pitch, int alignmentId,
				int visualSeed) {
			this(eventId, profileId, caster, dimension, x, y, z, yaw, pitch, alignmentId,
					visualSeed, AUTHORED_LIFETIME_TICKS, 0.0);
		}
		public Event withPhase(double value) {
			return new Event(eventId, profileId, caster, dimension, x, y, z, yaw, pitch,
					alignmentId, visualSeed, lifetimeTicks, value);
		}
	}

	public record Vertex(float x, float y, float z, int rgba) {
		public boolean finite() { return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z); }
	}

	public record Mesh(List<Vertex> vertices, String primitiveSignature, String outerOutlineSignature,
			double phase, double fillAlpha) {
		public Mesh {
			vertices = List.copyOf(vertices);
			if (vertices.isEmpty() || vertices.size() > MAX_VERTICES
					|| vertices.stream().anyMatch(vertex -> !vertex.finite())
					|| primitiveSignature == null || primitiveSignature.isBlank()
					|| outerOutlineSignature == null || outerOutlineSignature.isBlank()
					|| !Double.isFinite(phase) || !Double.isFinite(fillAlpha) || fillAlpha <= 0.0) {
				throw new IllegalArgumentException("invalid silhouette mesh");
			}
		}
	}

	private static boolean validDimension(String dimension) {
		return dimension != null && !dimension.isBlank() && dimension.length() <= 128;
	}

	static boolean validWorldCoordinates(double x, double y, double z) {
		return finite(x, y, z) && Math.abs(x) <= MAX_WORLD_COORDINATE
				&& Math.abs(y) <= MAX_WORLD_COORDINATE && Math.abs(z) <= MAX_WORLD_COORDINATE;
	}

	private static boolean finite(double... values) {
		for (double value : values) if (!Double.isFinite(value)) return false;
		return true;
	}
}
