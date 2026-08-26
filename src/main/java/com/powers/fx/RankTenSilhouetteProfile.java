package com.powers.fx;

import com.powers.progression.InnatePowerLevels;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/** Immutable, closed silhouette definitions for the twenty-three rank-ten innate powers. */
public final class RankTenSilhouetteProfile {
	public static final int MAX_PRIMITIVES = 64;
	private static final Palette RADIANT = new Palette(0xFFF6D6, 0xE6B65C, 0xFFF9EB, 0.52);
	private static final Palette DARKNESS = new Palette(0x17091F, 0xBD34BC, 0x351040, 0.44);
	private static final Map<String, RankTenSilhouetteProfile> BY_POWER = catalogue();
	private static final Map<Integer, RankTenSilhouetteProfile> BY_NETWORK = byNetworkId();

	private final String powerId;
	private final int networkId;
	private final List<Primitive> primitives;
	private final String primitiveSignature;

	private RankTenSilhouetteProfile(String powerId, int networkId, List<Primitive> primitives) {
		this.powerId = validPowerId(powerId);
		if (networkId < 0) throw new IllegalArgumentException("negative network ID");
		this.networkId = networkId;
		this.primitives = List.copyOf(primitives);
		if (this.primitives.isEmpty() || this.primitives.size() > MAX_PRIMITIVES
				|| this.primitives.stream().anyMatch(primitive -> !primitive.finite())
				|| expandedVertexUpperBound(this.primitives) > RankTenSilhouetteGeometry.MAX_VERTICES) {
			throw new IllegalArgumentException("invalid silhouette primitives");
		}
		this.primitiveSignature = signature(this.primitives);
	}

	/** Returns the exact immutable authoritative innate ID set. */
	public static Set<String> powerIds() {
		return BY_POWER.keySet();
	}

	/** Resolves an authored power, including the legacy size-morph spelling. */
	public static Optional<RankTenSilhouetteProfile> forPower(String powerId) {
		if (powerId == null) return Optional.empty();
		String canonical = "size_morph".equals(powerId) ? "size_shift" : powerId;
		return Optional.ofNullable(BY_POWER.get(canonical));
	}

	/** Resolves a compact stable wire ID without accepting unknown identities. */
	public static Optional<RankTenSilhouetteProfile> fromNetworkId(int networkId) {
		return Optional.ofNullable(BY_NETWORK.get(networkId));
	}

	public String powerId() {
		return powerId;
	}

	public int networkId() {
		return networkId;
	}

	/** Returns the immutable shape primitives; alignment never changes this list. */
	public List<Primitive> primitives() {
		return primitives;
	}

	/** Returns the canonical monochrome identity used by pairwise silhouette checks. */
	public String primitiveSignature() {
		return primitiveSignature;
	}

	/** Returns the closed radiant or Darkness edge palette. */
	public Palette alignmentPalette(boolean radiant) {
		return radiant ? RADIANT : DARKNESS;
	}

	private static Map<String, RankTenSilhouetteProfile> catalogue() {
		Map<String, RankTenSilhouetteProfile> profiles = new LinkedHashMap<>();
		put(profiles, "size_shift", 0, s(-1.0, -1.4, -1.0, 1.4), s(1.0, -1.4, 1.0, 1.4),
				s(-1.0, 1.4, -1.7, 1.9), s(1.0, 1.4, 1.7, 1.9), s(-1.7, 1.9, 1.7, 1.9));
		put(profiles, "time_shift", 1, r(-0.72, 0, 0.7, 8), r(0.72, 0, 0.7, 8),
				s(-1.3, -0.9, 1.3, 0.9));
		put(profiles, "flight", 2, s(0, -1.2, -2.0, 1.1), s(0, -1.2, 2.0, 1.1),
				s(-2.0, 1.1, -0.4, 0.6), s(2.0, 1.1, 0.4, 0.6));
		put(profiles, "starfall", 3, d(0, 0.4, 0.48), s(0, 0.8, 0, -1.7),
				s(-1.1, 1.4, 0, 2.0), s(0, 2.0, 1.1, 1.4));
		put(profiles, "void_beam", 4, r(0, 0, 1.35, 12), s(-0.25, -1.5, -0.25, 1.5),
				s(0.25, -1.5, 0.25, 1.5));
		put(profiles, "fireball", 5, d(0, 0, 1.1), s(-1.0, 0.7, -1.9, 1.2),
				s(0.7, -0.9, 1.7, -1.4), s(0.9, 0.3, 1.9, 0.4));
		put(profiles, "lightning_strike", 6, s(0, 2.0, 0, 0.7), s(0, 0.7, -0.65, -0.2),
				s(-0.65, -0.2, 0.15, -1.8), s(-1.3, 1.3, 0, 2.0), s(0, 2.0, 1.3, 1.3));
		put(profiles, "thunderclap", 7, r(0, 0, 0.75, 8), r(0, 0, 1.45, 12),
				s(-2.0, -0.6, -0.8, 0), s(2.0, -0.6, 0.8, 0));
		put(profiles, "speed_burst", 8, s(-1.8, -0.8, 1.7, 0), s(-1.8, 0.8, 1.7, 0),
				s(-2.2, -1.2, -0.7, -0.8), s(-2.2, 1.2, -0.7, 0.8));
		put(profiles, "telekinesis", 9, r(0, 0, 0.72, 8), s(0, 0.4, -1.7, 1.5),
				s(0, 0.4, -0.8, 2.0), s(0, 0.4, 0, 2.2), s(0, 0.4, 0.8, 2.0), s(0, 0.4, 1.7, 1.5));
		put(profiles, "energy_beam", 10, r(0, 0, 0.9, 10), s(-2.0, -1.3, 2.1, 1.3),
				s(1.5, 0.9, 2.1, 1.3), s(1.5, 1.6, 2.1, 1.3));
		put(profiles, "super_speed", 11, r(0, 0, 1.3, 10), s(-0.15, 1.5, 0.15, 0.55),
				s(0.15, 0.55, -0.55, -0.7), s(-1.8, -1.2, 1.3, -1.2));
		put(profiles, "breezy_bash", 12, s(0, -1.8, 0, 1.8), s(0, 1.8, -1.35, 0.5),
				s(0, 1.8, 1.35, 0.5), r(0, -0.2, 0.7, 8));
		put(profiles, "invisibility", 13, s(-0.9, -1.5, -0.9, -0.2), s(-0.9, 0.4, -0.9, 1.5),
				s(0.9, -1.5, 0.9, -0.2), s(0.9, 0.4, 0.9, 1.5));
		put(profiles, "time_freeze", 14, s(-1.4, -1.4, 1.4, -1.4), s(1.4, -1.4, 1.4, 1.4),
				s(1.4, 1.4, -1.4, 1.4), s(-1.4, 1.4, -1.4, -1.4), s(0, 0, 0, 0.95));
		put(profiles, "forcefield", 15, r(0, 0, 1.55, 6), d(0, 0, 0.35), r(0, 0, 0.72, 6));
		put(profiles, "gravity_displacement", 16, r(-0.6, 0, 0.86, 8), r(0.6, 0, 0.86, 8),
				r(0, 0.75, 0.86, 8), d(0, 0, 0.28));
		put(profiles, "vessel_possession", 17, r(-0.75, 0.65, 0.48, 8), r(0.75, 0.65, 0.48, 8),
				s(-0.75, 0.15, 0, -1.0), s(0, -1.0, 0.75, 0.15), r(0, -0.35, 0.25, 6));
		put(profiles, "astral_projection", 18, r(-0.45, 0, 0.9, 10), r(0.45, 0.2, 0.9, 10),
				s(-0.65, -1.5, -0.65, 1.3), s(0.65, -1.3, 0.65, 1.5));
		put(profiles, "energy_drain", 19, r(0.35, 0, 1.45, 12), s(-1.5, 0.85, -0.3, 0.3),
				s(-1.5, -0.85, -0.3, -0.3), s(1.2, 0.7, 0.25, 0.15));
		put(profiles, "ice_manipulation", 20, s(0, -1.7, 0, 1.8), s(0, 1.2, -1.35, 0.35),
				s(0, 1.2, 1.35, 0.35), s(-0.75, 0.8, 0.75, 0.8), s(-1.35, 0.35, -1.35, -0.7), s(1.35, 0.35, 1.35, -0.7));
		put(profiles, "plant_healing_acceleration", 21, s(0, -1.9, 0, 1.4), s(0, 0.7, -1.5, 1.7),
				s(0, 0.9, 1.5, 1.8), s(0, -1.2, -1.3, -1.9), s(0, -1.2, 1.3, -1.9), r(0, 1.45, 0.7, 8));
		put(profiles, "double_health", 22, r(-0.55, 0.2, 0.72, 8), r(0.55, 0.2, 0.72, 8),
				r(0, -0.35, 1.5, 12), s(0, 0.7, 0, -1.3));
		if (!profiles.keySet().equals(InnatePowerLevels.powerIds()) || profiles.size() != 23) {
			throw new IllegalStateException("rank-ten catalogue must exactly match innate powers");
		}
		if (profiles.values().stream().map(RankTenSilhouetteProfile::primitiveSignature).distinct().count()
				!= profiles.size()) throw new IllegalStateException("duplicate silhouette signature");
		return Map.copyOf(profiles);
	}

	private static Map<Integer, RankTenSilhouetteProfile> byNetworkId() {
		Map<Integer, RankTenSilhouetteProfile> profiles = new LinkedHashMap<>();
		for (RankTenSilhouetteProfile profile : BY_POWER.values()) {
			if (profiles.put(profile.networkId, profile) != null) {
				throw new IllegalStateException("duplicate silhouette network ID");
			}
		}
		return Map.copyOf(profiles);
	}

	private static void put(Map<String, RankTenSilhouetteProfile> target, String id,
			int networkId, Primitive... primitives) {
		if (target.put(id, new RankTenSilhouetteProfile(id, networkId, List.of(primitives))) != null) {
			throw new IllegalStateException("duplicate silhouette power ID");
		}
	}

	private static Segment s(double x1, double y1, double x2, double y2) {
		return new Segment(x1, y1, x2, y2, 0.12);
	}

	private static Ring r(double x, double y, double radius, int segments) {
		return new Ring(x, y, radius, segments, 0.11);
	}

	private static Disc d(double x, double y, double radius) {
		return new Disc(x, y, radius);
	}

	private static String validPowerId(String value) {
		if (value == null || value.isBlank() || !value.equals(value.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("invalid silhouette power ID");
		}
		return value;
	}

	private static String signature(List<Primitive> primitives) {
		StringJoiner joiner = new StringJoiner("|");
		primitives.forEach(primitive -> joiner.add(primitive.signature()));
		return joiner.toString();
	}

	private static int expandedVertexUpperBound(List<Primitive> primitives) {
		int vertices = 0;
		for (Primitive primitive : primitives) {
			vertices += switch (primitive) {
				case Segment ignored -> 4;
				case Ring ring -> ring.segments() * 4;
				case Disc ignored -> 24;
			};
		}
		return vertices;
	}

	public sealed interface Primitive permits Segment, Ring, Disc {
		boolean finite();
		String signature();
	}

	/** A thin authored outline segment in local silhouette space. */
	public record Segment(double x1, double y1, double x2, double y2, double width) implements Primitive {
		public Segment {
			if (!RankTenSilhouetteProfile.finite(x1, y1, x2, y2, width) || width <= 0 || width > 1.0
					|| Math.hypot(x2 - x1, y2 - y1) < 0.01 || outside(x1, y1, x2, y2)) {
				throw new IllegalArgumentException("invalid silhouette segment");
			}
		}
		@Override public boolean finite() { return RankTenSilhouetteProfile.finite(x1, y1, x2, y2, width); }
		@Override public String signature() { return "S:" + x1 + ',' + y1 + ',' + x2 + ',' + y2 + ',' + width; }
	}

	/** A bounded polygonal outline ring. */
	public record Ring(double x, double y, double radius, int segments, double width) implements Primitive {
		public Ring {
			if (!RankTenSilhouetteProfile.finite(x, y, radius, width) || radius <= 0 || radius > 4.0
					|| segments < 3 || segments > 16 || width <= 0 || width > 1.0 || outside(x, y)) {
				throw new IllegalArgumentException("invalid silhouette ring");
			}
		}
		@Override public boolean finite() { return RankTenSilhouetteProfile.finite(x, y, radius, width); }
		@Override public String signature() { return "R:" + x + ',' + y + ',' + radius + ',' + segments + ',' + width; }
	}

	/** A small filled accent disc; it never supplies the profile's identity by itself. */
	public record Disc(double x, double y, double radius) implements Primitive {
		public Disc {
			if (!RankTenSilhouetteProfile.finite(x, y, radius) || radius <= 0 || radius > 4.0 || outside(x, y)) {
				throw new IllegalArgumentException("invalid silhouette disc");
			}
		}
		@Override public boolean finite() { return RankTenSilhouetteProfile.finite(x, y, radius); }
		@Override public String signature() { return "D:" + x + ',' + y + ',' + radius; }
	}

	/** Closed alignment colours applied only after the monochrome primitive identity is selected. */
	public record Palette(int outerRgb, int accentRgb, int fillRgb, double fillAlpha) {
		public Palette {
			if (!legalRgb(outerRgb) || !legalRgb(accentRgb) || !legalRgb(fillRgb)
					|| !Double.isFinite(fillAlpha) || fillAlpha < 0.15 || fillAlpha > 0.75) {
				throw new IllegalArgumentException("invalid silhouette palette");
			}
		}
		public boolean legal() { return true; }
	}

	private static boolean finite(double... values) {
		for (double value : values) if (!Double.isFinite(value)) return false;
		return true;
	}

	private static boolean outside(double... values) {
		for (double value : values) if (Math.abs(value) > 4.0) return true;
		return false;
	}

	private static boolean legalRgb(int value) {
		return value >= 0 && value <= 0xFFFFFF;
	}
}
