package com.powers.visual;

import java.util.List;

/** Pure selection rules; client rendering availability is supplied by the narrow render boundary. */
public final class LightRealmSkyRules {
	private static final int ANCIENT_WHITE = 0xFFFFFFFF;
	private static final double TAU = Math.PI * 2.0;

	private LightRealmSkyRules() {
	}

	public static LightRealmSkyProfile resolve(boolean lightRealm, boolean reducedMotion,
			boolean enhancedAvailable, double gameTime) {
		if (!lightRealm) return profile(LightRealmSkyProfile.Mode.NONE, 0, List.of(), 0.0);
		if (!enhancedAvailable) {
			return profile(LightRealmSkyProfile.Mode.STATIC_WHITE, ANCIENT_WHITE, List.of(), 0.0);
		}

		double rotation = rotation(gameTime);
		if (reducedMotion) {
			return profile(LightRealmSkyProfile.Mode.ANCIENT_WHITE_REDUCED, ANCIENT_WHITE, List.of(
					layer(LightRealmSkyProfile.Shape.OUTER_HALO, 0xFFFFF8DE, 0.10, 1.00, 0.0, 0.0, 0.0),
					layer(LightRealmSkyProfile.Shape.RUNIC_COMPASS, 0xFFFFE5A6, 0.08, 0.78, 0.0, 0.0, 0.0)), 0.0);
		}

		return profile(LightRealmSkyProfile.Mode.ANCIENT_WHITE, ANCIENT_WHITE, List.of(
				layer(LightRealmSkyProfile.Shape.OUTER_HALO, 0xFFFFF8DE, 0.16, 1.00, 0.00016, 0.018,
						phase(rotation, 1.0, 0.0)),
				layer(LightRealmSkyProfile.Shape.RUNIC_COMPASS, 0xFFFFE5A6, 0.13, 0.78, -0.00024, 0.014,
						phase(rotation, -1.5, Math.PI / 4.0)),
				layer(LightRealmSkyProfile.Shape.CROWN_ARCS, 0xFFFFF0C7, 0.10, 0.57, 0.00032, 0.010,
						phase(rotation, 2.0, Math.PI / 2.0)),
				layer(LightRealmSkyProfile.Shape.RADIAL_VEIL, 0xFFFFD77A, 0.06, 0.35, -0.00012, 0.008,
						phase(rotation, -0.75, Math.PI))), rotation);
	}

	private static LightRealmSkyProfile profile(LightRealmSkyProfile.Mode mode, int baseColor,
			List<LightRealmSkyProfile.Layer> layers, double rotation) {
		return new LightRealmSkyProfile(mode, baseColor, layers, rotation, false, false);
	}

	private static LightRealmSkyProfile.Layer layer(LightRealmSkyProfile.Shape shape, int color, double alpha,
			double scale, double velocity, double pulse, double phase) {
		return new LightRealmSkyProfile.Layer(shape, color, alpha, scale, velocity, pulse, phase);
	}

	private static double rotation(double gameTime) {
		if (!Double.isFinite(gameTime)) return 0.0;
		return Math.IEEEremainder(gameTime, 24_000.0) / 24_000.0 * TAU;
	}

	private static double phase(double rotation, double multiplier, double offset) {
		return Math.IEEEremainder(rotation * multiplier + offset, TAU);
	}
}
