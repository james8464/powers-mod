package com.powers.visual;

import java.util.List;
import java.util.Objects;

/** Immutable, renderer-agnostic description of the Light Realm sky for one frame. */
public record LightRealmSkyProfile(Mode mode, int baseColor, List<Layer> layers,
		double rotationRadians, boolean usesTexture, boolean usesCustomShader) {
	public LightRealmSkyProfile {
		Objects.requireNonNull(mode, "mode");
		layers = List.copyOf(layers);
		if (!Double.isFinite(rotationRadians)) {
			throw new IllegalArgumentException("rotationRadians must be finite");
		}
		if (usesTexture || usesCustomShader) {
			throw new IllegalArgumentException("Light Realm sky profiles cannot depend on textures or custom shaders");
		}
		if (mode == Mode.NONE) {
			if (baseColor != 0 || !layers.isEmpty()) {
				throw new IllegalArgumentException("NONE cannot claim a base colour or enhanced layers");
			}
		} else if (baseColor != 0xFFFFFFFF) {
			throw new IllegalArgumentException("every Light Realm profile requires an exact opaque-white base");
		}
		if (mode == Mode.STATIC_WHITE && !layers.isEmpty()) {
			throw new IllegalArgumentException("STATIC_WHITE cannot carry enhanced layers");
		}
	}

	public enum Mode {
		NONE,
		STATIC_WHITE,
		ANCIENT_WHITE,
		ANCIENT_WHITE_REDUCED
	}

	public enum Shape {
		OUTER_HALO,
		RUNIC_COMPASS,
		CROWN_ARCS,
		RADIAL_VEIL
	}

	public record Layer(Shape shape, int color, double alpha, double scale,
			double angularVelocity, double pulseAmplitude, double phase) {
		public Layer {
			Objects.requireNonNull(shape, "shape");
			if ((color & 0xFF000000) != 0xFF000000) {
				throw new IllegalArgumentException("layer palette colours must be opaque ARGB");
			}
			if (!Double.isFinite(alpha) || alpha < 0.04 || alpha > 0.22) {
				throw new IllegalArgumentException("alpha must be within renderer-safe [0.04,0.22]");
			}
			if (!Double.isFinite(scale) || scale < 0.35 || scale > 1.0) {
				throw new IllegalArgumentException("scale must be within renderer-safe [0.35,1]");
			}
			if (!Double.isFinite(angularVelocity) || Math.abs(angularVelocity) > 0.001) {
				throw new IllegalArgumentException("angular velocity exceeds renderer-safe bounds");
			}
			if (!Double.isFinite(pulseAmplitude) || pulseAmplitude < 0.0 || pulseAmplitude > 0.05) {
				throw new IllegalArgumentException("pulse amplitude exceeds renderer-safe bounds");
			}
			if (!Double.isFinite(phase) || Math.abs(phase) > Math.PI * 2.0) {
				throw new IllegalArgumentException("phase exceeds one bounded revolution");
			}
		}
	}
}
