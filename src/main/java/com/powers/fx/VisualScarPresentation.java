package com.powers.fx;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Defines the closed five-impact by six-material visual profile table. */
public final class VisualScarPresentation {
	private static final Map<VisualScarRules.Impact, Motif> MOTIFS = motifTable();
	private static final Map<VisualScarRules.Material, Integer> BASES = materialTable();
	private static final List<Profile> PROFILES = profiles();

	private VisualScarPresentation() {
	}

	/** Returns the one closed motif owned by an impact family. */
	public static Motif motif(VisualScarRules.Impact impact) {
		return Objects.requireNonNull(MOTIFS.get(impact), "unknown impact");
	}

	/** Returns the immutable profile for one exact impact and material pair. */
	public static Profile profile(VisualScarRules.Impact impact, VisualScarRules.Material material) {
		return PROFILES.stream().filter(candidate -> candidate.key().equals(new Key(impact, material)))
				.findFirst().orElseThrow();
	}

	/** Returns all thirty immutable profiles in stable impact-major order. */
	public static List<Profile> allProfiles() {
		return PROFILES;
	}

	private static Map<VisualScarRules.Impact, Motif> motifTable() {
		Map<VisualScarRules.Impact, Motif> result = new EnumMap<>(VisualScarRules.Impact.class);
		result.put(VisualScarRules.Impact.BEAM, Motif.LINEAR_RUNE);
		result.put(VisualScarRules.Impact.SLAM, Motif.RADIAL_CRACK);
		result.put(VisualScarRules.Impact.THUNDERCLAP, Motif.FORKED_WAVE);
		result.put(VisualScarRules.Impact.ICE, Motif.FROST_BRANCH);
		result.put(VisualScarRules.Impact.FIRE, Motif.EMBER_RING);
		return Map.copyOf(result);
	}

	private static Map<VisualScarRules.Material, Integer> materialTable() {
		Map<VisualScarRules.Material, Integer> result = new EnumMap<>(VisualScarRules.Material.class);
		result.put(VisualScarRules.Material.STONE, 0x8C9198);
		result.put(VisualScarRules.Material.EARTH, 0x795538);
		result.put(VisualScarRules.Material.WOOD, 0x9B6A3D);
		result.put(VisualScarRules.Material.METAL, 0xB9C7D5);
		result.put(VisualScarRules.Material.SAND, 0xD8C58A);
		result.put(VisualScarRules.Material.COLD, 0xA8E8F4);
		return Map.copyOf(result);
	}

	private static List<Profile> profiles() {
		List<Profile> result = new ArrayList<>(30);
		for (VisualScarRules.Impact impact : VisualScarRules.Impact.values()) {
			for (VisualScarRules.Material material : VisualScarRules.Material.values()) {
				int ordinal = impact.ordinal() * 6 + material.ordinal();
				result.add(new Profile(new Key(impact, material), motif(impact), BASES.get(material),
						accent(impact, material), 0.48 + material.ordinal() * 0.05,
						8 + impact.ordinal() + material.ordinal() % 3,
						0.035 + material.ordinal() * 0.008,
						0.04 + impact.ordinal() * 0.025,
						0.12 + ordinal * 0.019, false, false));
			}
		}
		return List.copyOf(result);
	}

	private static int accent(VisualScarRules.Impact impact, VisualScarRules.Material material) {
		int[] accents = {0xF5E7A1, 0xE6D0A8, 0xDCEBFF, 0xD4FBFF, 0xFF9B54};
		int value = accents[impact.ordinal()];
		return (value ^ (material.ordinal() * 0x070503)) & 0xFFFFFF;
	}

	public enum Motif { LINEAR_RUNE, RADIAL_CRACK, FORKED_WAVE, FROST_BRANCH, EMBER_RING }

	public record Key(VisualScarRules.Impact impact, VisualScarRules.Material material) {
		public Key {
			impact = Objects.requireNonNull(impact, "impact");
			material = Objects.requireNonNull(material, "material");
		}
	}

	public record Profile(Key key, Motif motif, int materialBaseRgb, int accentRgb,
			double alpha, int segments, double stroke, double inset, double variation,
			boolean usesTexture, boolean usesCustomShader) {
		public Profile {
			key = Objects.requireNonNull(key, "key");
			motif = Objects.requireNonNull(motif, "motif");
			if (materialBaseRgb < 0 || materialBaseRgb > 0xFFFFFF
					|| accentRgb < 0 || accentRgb > 0xFFFFFF
					|| !Double.isFinite(alpha) || alpha < 0.15 || alpha > 0.95
					|| segments < 3 || segments > 24
					|| !Double.isFinite(stroke) || stroke < 0.01 || stroke > 0.20
					|| !Double.isFinite(inset) || inset < 0.0 || inset > 0.40
					|| !Double.isFinite(variation) || variation < 0.0 || variation > 1.0
					|| usesTexture || usesCustomShader) {
				throw new IllegalArgumentException("profile exceeds closed presentation bounds");
			}
		}

		/** Returns this profile with a validated segment count replacement. */
		public Profile withSegments(int value) {
			return new Profile(key, motif, materialBaseRgb, accentRgb, alpha, value,
					stroke, inset, variation, false, false);
		}

		/** Returns this profile with a validated stroke replacement. */
		public Profile withStroke(double value) {
			return new Profile(key, motif, materialBaseRgb, accentRgb, alpha, segments,
					value, inset, variation, false, false);
		}

		/** Returns this profile with a validated inset replacement. */
		public Profile withInset(double value) {
			return new Profile(key, motif, materialBaseRgb, accentRgb, alpha, segments,
					stroke, value, variation, false, false);
		}

		/** Returns this profile with a validated variation replacement. */
		public Profile withVariation(double value) {
			return new Profile(key, motif, materialBaseRgb, accentRgb, alpha, segments,
					stroke, inset, value, false, false);
		}

		/** Returns this profile with a validated material-base color replacement. */
		public Profile withMaterialBaseRgb(int value) {
			return new Profile(key, motif, value, accentRgb, alpha, segments,
					stroke, inset, variation, false, false);
		}

		/** Returns this profile with a validated accent color replacement. */
		public Profile withAccentRgb(int value) {
			return new Profile(key, motif, materialBaseRgb, value, alpha, segments,
					stroke, inset, variation, false, false);
		}

		/** Returns this profile with a validated alpha replacement. */
		public Profile withAlpha(double value) {
			return new Profile(key, motif, materialBaseRgb, accentRgb, value, segments,
					stroke, inset, variation, false, false);
		}
	}
}
