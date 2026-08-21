package com.powers.visual;

import java.util.ArrayList;
import java.util.List;

/** Pure bounded downward-facing polygons for the procedural ancient-white sky. */
public final class LightRealmSkyGeometry {
	public static final int MAX_VERTICES_PER_SHAPE = 128;
	public static final int MAX_DRAWS_PER_SHAPE = 32;
	public static final int MAX_TOTAL_VERTICES = 256;
	public static final int MAX_TOTAL_DRAWS = 64;
	private static final float HEIGHT = 100.0F;

	private LightRealmSkyGeometry() {
	}

	public static Mesh build(LightRealmSkyProfile.Shape shape) {
		Builder builder = new Builder();
		switch (shape) {
			case OUTER_HALO -> ring(builder, 32, 88.0F, 118.0F);
			case RUNIC_COMPASS -> arms(builder, 8, 30.0F, 118.0F, Math.toRadians(10.0));
			case CROWN_ARCS -> crown(builder);
			case RADIAL_VEIL -> arms(builder, 12, 22.0F, 108.0F, Math.toRadians(7.0));
		}
		return builder.build();
	}

	private static void ring(Builder builder, int segments, float innerRadius, float outerRadius) {
		double step = Math.PI * 2.0 / segments;
		for (int segment = 0; segment < segments; segment++) {
			builder.strip(innerRadius, outerRadius, -segment * step, -(segment + 1) * step);
		}
	}

	private static void crown(Builder builder) {
		double spacing = Math.PI / 2.0;
		double halfArc = Math.toRadians(22.5);
		for (int arc = 0; arc < 4; arc++) {
			double centre = -arc * spacing;
			for (int segment = 0; segment < 3; segment++) {
				double start = centre + halfArc - segment * halfArc * 2.0 / 3.0;
				double end = centre + halfArc - (segment + 1) * halfArc * 2.0 / 3.0;
				builder.strip(80.0F, 112.0F, start, end);
			}
		}
	}

	private static void arms(Builder builder, int count, float innerRadius,
			float outerRadius, double angularWidth) {
		double spacing = Math.PI * 2.0 / count;
		for (int arm = 0; arm < count; arm++) {
			double centre = -arm * spacing;
			builder.strip(innerRadius, outerRadius,
					centre + angularWidth / 2.0, centre - angularWidth / 2.0);
		}
	}

	public record Vertex(float x, float y, float z, int color) {
	}

	public record DrawRange(int firstVertex, int vertexCount) {
		public DrawRange {
			if (firstVertex < 0 || vertexCount < 3) {
				throw new IllegalArgumentException("triangle-fan range is invalid");
			}
		}
	}

	public record Mesh(List<Vertex> vertices, List<DrawRange> drawRanges) {
		public Mesh {
			vertices = List.copyOf(vertices);
			drawRanges = List.copyOf(drawRanges);
			if (vertices.isEmpty() || vertices.size() > MAX_VERTICES_PER_SHAPE
					|| drawRanges.isEmpty() || drawRanges.size() > MAX_DRAWS_PER_SHAPE) {
				throw new IllegalArgumentException("sky geometry exceeds its per-shape work budget");
			}
			for (DrawRange range : drawRanges) {
				if (range.firstVertex() + range.vertexCount() > vertices.size()) {
					throw new IllegalArgumentException("triangle-fan range exceeds the vertex buffer");
				}
			}
		}
	}

	private static final class Builder {
		private final List<Vertex> vertices = new ArrayList<>();
		private final List<DrawRange> ranges = new ArrayList<>();

		private void strip(float innerRadius, float outerRadius, double startAngle, double endAngle) {
			int first = vertices.size();
			vertices.add(vertex(innerRadius, startAngle, 0x70FFFFFF));
			vertices.add(vertex(outerRadius, startAngle, 0xE8FFFFFF));
			vertices.add(vertex(outerRadius, endAngle, 0xE8FFFFFF));
			vertices.add(vertex(innerRadius, endAngle, 0x70FFFFFF));
			ranges.add(new DrawRange(first, 4));
		}

		private Mesh build() {
			return new Mesh(vertices, ranges);
		}

		private static Vertex vertex(float radius, double angle, int color) {
			return new Vertex((float) Math.sin(angle) * radius, HEIGHT,
					(float) Math.cos(angle) * radius, color);
		}
	}
}
