package com.powers.visual;

import java.util.List;
import java.util.Objects;

/** Immutable, renderer-agnostic description of the Light Realm sky for one frame. */
public record LightRealmSkyProfile(Mode mode, int baseColor, List<Layer> layers,
		double animationTimeTicks, boolean usesTexture, boolean usesCustomShader) {
	private static final double TAU = Math.PI * 2.0;
	private static final List<Shape> AUTHORED_SHAPES = List.of(
			Shape.OUTER_HALO, Shape.RUNIC_COMPASS, Shape.CROWN_ARCS, Shape.RADIAL_VEIL);
	private static final List<Integer> AUTHORED_COLORS = List.of(
			0xFFFFF8DE, 0xFFFFE5A6, 0xFFFFF0C7, 0xFFFFD77A);
	private static final List<Double> AUTHORED_SCALES = List.of(1.00, 0.78, 0.57, 0.35);
	private static final double NORMAL_MIN_CONTRAST = 0.40;
	private static final double REDUCED_MAX_CONTRAST = 0.20;

	public LightRealmSkyProfile {
		Objects.requireNonNull(mode, "mode");
		layers = List.copyOf(layers);
		if (!Double.isFinite(animationTimeTicks)) {
			throw new IllegalArgumentException("animationTimeTicks must be finite");
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
		if (mode == Mode.ANCIENT_WHITE) validateEnhanced(layers, AUTHORED_SHAPES.size(), false, animationTimeTicks);
		if (mode == Mode.ANCIENT_WHITE_REDUCED) validateEnhanced(layers, 2, true, animationTimeTicks);
	}

	/** Exact per-layer angle consumed by the client renderer for this extracted frame. */
	public double effectiveRotationRadians(Layer layer) {
		Objects.requireNonNull(layer, "layer");
		return Math.IEEEremainder(layer.phase() + layer.angularVelocity() * animationTimeTicks, TAU);
	}

	private static void validateEnhanced(List<Layer> layers, int expectedLayers,
			boolean reduced, double animationTimeTicks) {
		if (layers.size() != expectedLayers) {
			throw new IllegalArgumentException("enhanced sky layer cardinality does not match its mode");
		}
		for (int index = 0; index < expectedLayers; index++) {
			Layer layer = layers.get(index);
			if (layer.shape() != AUTHORED_SHAPES.get(index) || layer.color() != AUTHORED_COLORS.get(index)
					|| Double.compare(layer.scale(), AUTHORED_SCALES.get(index)) != 0) {
				throw new IllegalArgumentException("enhanced sky layers require the ordered authored shape palette and scale");
			}
			if (reduced && (layer.angularVelocity() != 0.0 || layer.pulseAmplitude() != 0.0
					|| layer.phase() != 0.0)) {
				throw new IllegalArgumentException("reduced-motion layers must be fully static");
			}
		}
		double contrast = layers.stream().mapToDouble(Layer::alpha).sum();
		if ((!reduced && contrast < NORMAL_MIN_CONTRAST) || (reduced && contrast > REDUCED_MAX_CONTRAST)) {
			throw new IllegalArgumentException("enhanced sky contrast does not match its mode");
		}
		if (!reduced && (layers.stream().noneMatch(layer -> layer.angularVelocity() != 0.0)
				|| layers.stream().noneMatch(layer -> layer.pulseAmplitude() != 0.0))) {
			throw new IllegalArgumentException("ordinary enhanced sky requires bounded drift and pulse");
		}
		if (reduced && animationTimeTicks != 0.0) {
			throw new IllegalArgumentException("reduced-motion sky animation time must be zero");
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
