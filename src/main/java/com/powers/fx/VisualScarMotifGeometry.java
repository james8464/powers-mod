package com.powers.fx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Generates bounded shape-specific surface meshes and deterministic single-batch frame selections. */
public final class VisualScarMotifGeometry {
	public static final int MAX_QUADS_PER_SCAR = 16;
	public static final int MAX_VERTICES_PER_SCAR = 64;
	public static final int MAX_VISIBLE_SCARS = 512;
	public static final int MAX_FRAME_QUADS = 8_192;
	public static final int MAX_FRAME_VERTICES = 32_768;

	private VisualScarMotifGeometry() {
	}

	/** Generates one finite bounded motif mesh from the complete presentation profile. */
	public static Mesh mesh(VisualScarPresentation.Profile profile, VisualScarRules.Face face,
			double worldX, double worldY, double worldZ,
			double cameraX, double cameraY, double cameraZ,
			double outward, double size, long visualSeed, Lod lod) {
		Objects.requireNonNull(profile, "profile");
		Objects.requireNonNull(face, "face");
		Objects.requireNonNull(lod, "lod");
		if (!finite(worldX, worldY, worldZ, cameraX, cameraY, cameraZ, outward, size)
				|| outward <= 0.0 || outward > 0.01 || size <= 0.0 || size > 1.0) {
			throw new IllegalArgumentException("invalid scar geometry bounds");
		}
		int budget = quadBudget(profile.segments(), lod);
		List<Line> lines = motifLines(profile, visualSeed, budget);
		List<Quad> quads = new ArrayList<>(lines.size());
		VisualScarGeometry.Basis basis = VisualScarGeometry.basis(face);
		for (int index = 0; index < lines.size(); index++) {
			Line line = lines.get(index);
			int rgb = (index & 1) == 0 ? profile.materialBaseRgb() : profile.accentRgb();
			int rgba = rgb << 8 | (int) Math.round(profile.alpha() * 255.0);
			quads.add(toQuad(line, profile.stroke(), size, face, basis,
					worldX, worldY, worldZ, cameraX, cameraY, cameraZ, outward, rgba));
		}
		return new Mesh(profile, profile.motif(), lod, List.copyOf(quads),
				profile.motif().name() + ":" + topologyCode(profile.motif(), lines), 1);
	}

	/** Returns the deterministic distance tier used by visible scar selection. */
	public static Lod lodForDistance(double distance) {
		if (!Double.isFinite(distance) || distance < 0.0 || distance > 256.0) {
			throw new IllegalArgumentException("distance outside visible scar range");
		}
		return distance <= 48.0 ? Lod.NEAR : distance <= 128.0 ? Lod.MID : Lod.FAR;
	}

	/** Returns the exact built-in pipeline state required by the renderer boundary. */
	public static PipelineContract pipelineContract() {
		return new PipelineContract("DEBUG_QUADS", "POSITION_COLOR", "QUADS",
				true, false, true, false);
	}

	/** Selects nearest meshes with stable-key ties and flattens them into one hard-bounded draw. */
	public static Batch batchNearestCandidates(List<Candidate> candidates, int requestedScarLimit,
			int requestedQuadLimit, int requestedVertexLimit, Function<Candidate, Mesh> meshFactory) {
		Objects.requireNonNull(candidates, "candidates");
		Objects.requireNonNull(meshFactory, "meshFactory");
		int scarLimit = Math.clamp(requestedScarLimit, 0, MAX_VISIBLE_SCARS);
		int quadLimit = Math.clamp(requestedQuadLimit, 0, MAX_FRAME_QUADS);
		int vertexLimit = Math.clamp(requestedVertexLimit, 0, MAX_FRAME_VERTICES);
		List<Candidate> sorted = candidates.stream().sorted(Comparator
				.comparingDouble(Candidate::distance).thenComparingLong(Candidate::key)).toList();
		List<Long> selected = new ArrayList<>(scarLimit);
		java.util.HashSet<Long> selectedSet = new java.util.HashSet<>();
		List<Vertex> vertices = new ArrayList<>();
		int quads = 0;
		for (Candidate candidate : sorted) {
			if (selected.size() >= scarLimit) break;
			if (!selectedSet.add(candidate.key())) continue;
			Mesh mesh = Objects.requireNonNull(meshFactory.apply(candidate), "generated mesh");
			int candidateQuads = mesh.quads().size();
			int candidateVertices = mesh.vertices().size();
			if (quads + candidateQuads > quadLimit
					|| vertices.size() + candidateVertices > vertexLimit) {
				selectedSet.remove(candidate.key());
				break;
			}
			selected.add(candidate.key());
			quads += candidateQuads;
			vertices.addAll(mesh.vertices());
		}
		return new Batch(List.copyOf(selected), List.copyOf(vertices), quads,
				selected.isEmpty() ? 0 : 1);
	}

	/** Returns a hide-without-delete decision for range, frustum, chunk, and support observations. */
	public static Visibility visibility(double distance, boolean inFrustum,
			boolean clientChunkLoaded, boolean supportFaceValid) {
		if (!Double.isFinite(distance) || distance < 0.0 || distance > 256.0) {
			return Visibility.HIDE_RANGE;
		}
		if (!inFrustum) return Visibility.HIDE_FRUSTUM;
		if (!clientChunkLoaded) return Visibility.HIDE_UNLOADED;
		return supportFaceValid ? Visibility.VISIBLE : Visibility.HIDE_SUPPORT;
	}

	private static int quadBudget(int authored, Lod lod) {
		return switch (lod) {
			case NEAR -> Math.min(MAX_QUADS_PER_SCAR, authored);
			case MID -> Math.min(10, Math.max(3, (int) Math.ceil(authored * 0.65)));
			case FAR -> Math.min(6, Math.max(3, (int) Math.ceil(authored * 0.4)));
		};
	}

	private static List<Line> motifLines(VisualScarPresentation.Profile profile,
			long seed, int count) {
		double radius = 0.42 - profile.inset() * 0.25 - profile.stroke() * 0.5;
		double phase = ((seed ^ Double.doubleToLongBits(profile.variation())) & 0xFFFFL)
				/ 65_535.0 * Math.PI * 2.0 * profile.variation()
				+ profile.segments() * 0.013;
		return switch (profile.motif()) {
			case LINEAR_RUNE -> linearRune(count, radius, phase, profile.variation());
			case RADIAL_CRACK -> radialCrack(count, radius, phase, profile.variation());
			case FORKED_WAVE -> forkedWave(count, radius, phase, profile.variation());
			case FROST_BRANCH -> frostBranch(count, radius, phase, profile.variation());
			case EMBER_RING -> emberRing(count, radius, phase, profile.variation());
		};
	}

	private static List<Line> linearRune(int count, double radius, double phase, double variation) {
		List<Line> lines = new ArrayList<>(count);
		lines.add(new Line(0, -radius, 0, radius));
		for (int index = 1; index < count; index++) {
			double y = -radius + 2.0 * radius * index / count;
			double skew = Math.sin(phase + index) * variation * radius * 0.18;
			lines.add(new Line(-radius * 0.65 + skew, y,
					radius * 0.65 - skew, y + (index % 2 == 0 ? radius * 0.12 : -radius * 0.12)));
		}
		return lines;
	}

	private static List<Line> radialCrack(int count, double radius, double phase, double variation) {
		List<Line> lines = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			double angle = phase + index * Math.PI * 2.0 / count;
			double inner = radius * (0.08 + variation * 0.08);
			double outer = radius * (0.78 + 0.18 * ((index & 1) + 1) / 2.0);
			lines.add(polar(inner, angle, outer, angle + Math.sin(index + phase) * 0.08));
		}
		return lines;
	}

	private static List<Line> forkedWave(int count, double radius, double phase, double variation) {
		List<Line> lines = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			double y1 = -radius + 2.0 * radius * index / count;
			double y2 = -radius + 2.0 * radius * (index + 1) / count;
			double x1 = Math.sin(phase + index * 1.7) * radius * (0.18 + variation * 0.08);
			double x2 = Math.sin(phase + (index + 1) * 1.7) * radius * (0.18 + variation * 0.08);
			if (index % 3 == 2) x2 += (index % 2 == 0 ? 1 : -1) * radius * 0.42;
			lines.add(new Line(x1, y1, x2, y2));
		}
		return lines;
	}

	private static List<Line> frostBranch(int count, double radius, double phase, double variation) {
		List<Line> lines = new ArrayList<>(count);
		lines.add(new Line(0, -radius, 0, radius));
		for (int index = 1; index < count; index++) {
			double side = (index & 1) == 0 ? 1.0 : -1.0;
			double y = -radius * 0.7 + radius * 1.4 * index / count;
			double reach = radius * (0.42 + variation * 0.12);
			lines.add(new Line(0, y, side * reach,
					y + radius * (0.22 + Math.sin(phase + index) * 0.04)));
		}
		return lines;
	}

	private static List<Line> emberRing(int count, double radius, double phase, double variation) {
		List<Line> lines = new ArrayList<>(count);
		for (int index = 0; index < count; index++) {
			double first = phase + index * Math.PI * 2.0 / count;
			double second = phase + (index + 1) * Math.PI * 2.0 / count;
			double pulse = radius * (0.92 + Math.sin(index * 2.0 + phase) * variation * 0.06);
			lines.add(polar(pulse, first, pulse, second));
		}
		return lines;
	}

	private static Line polar(double firstRadius, double firstAngle,
			double secondRadius, double secondAngle) {
		return new Line(Math.cos(firstAngle) * firstRadius, Math.sin(firstAngle) * firstRadius,
				Math.cos(secondAngle) * secondRadius, Math.sin(secondAngle) * secondRadius);
	}

	private static Quad toQuad(Line line, double stroke, double size, VisualScarRules.Face face,
			VisualScarGeometry.Basis basis, double worldX, double worldY, double worldZ,
			double cameraX, double cameraY, double cameraZ, double outward, int rgba) {
		double dx = line.u2() - line.u1();
		double dy = line.v2() - line.v1();
		double length = Math.hypot(dx, dy);
		double half = Math.min(stroke, 0.20) * 0.5;
		double pu = -dy / length * half;
		double pv = dx / length * half;
		double[][] local = {{line.u1() + pu, line.v1() + pv},
				{line.u1() - pu, line.v1() - pv}, {line.u2() - pu, line.v2() - pv},
				{line.u2() + pu, line.v2() + pv}};
		List<Vertex> vertices = new ArrayList<>(4);
		boolean bounded = true;
		for (double[] point : local) {
			double u = point[0] * size;
			double v = point[1] * size;
			bounded &= Math.abs(u) <= size * 0.5 && Math.abs(v) <= size * 0.5;
			vertices.add(VisualScarGeometry.vertex(basis, u, v, worldX, worldY, worldZ,
					cameraX, cameraY, cameraZ, outward, rgba));
		}
		return new Quad(List.copyOf(vertices), length * stroke * size * size,
				outward, bounded, face);
	}

	private static int topologyCode(VisualScarPresentation.Motif motif, List<Line> lines) {
		int code = motif.ordinal() + 1;
		for (Line line : lines) code = 31 * code + line.hashCode();
		return code;
	}

	private static boolean finite(double... values) {
		for (double value : values) if (!Double.isFinite(value)) return false;
		return true;
	}

	public enum Lod { NEAR, MID, FAR }

	public enum Visibility { VISIBLE, HIDE_RANGE, HIDE_FRUSTUM, HIDE_UNLOADED, HIDE_SUPPORT }

	public record Vertex(float x, float y, float z, int rgba) {
		/** Reports whether all renderer coordinates are finite. */
		public boolean finite() {
			return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
		}
	}

	public record Quad(List<Vertex> vertices, double area, double outwardOffset,
			boolean withinScarBounds, VisualScarRules.Face face) {
		public Quad {
			vertices = List.copyOf(vertices);
			face = Objects.requireNonNull(face, "face");
			if (vertices.size() != 4 || vertices.stream().anyMatch(vertex -> !vertex.finite())
					|| !Double.isFinite(area) || area <= 0.0
					|| !Double.isFinite(outwardOffset) || outwardOffset <= 0.0
					|| outwardOffset > 0.01 || !withinScarBounds) {
				throw new IllegalArgumentException("invalid motif quad");
			}
		}

		/** Reports whether all vertices lie on one plane aligned to the expected face. */
		public boolean surfaceAligned(VisualScarRules.Face expected) {
			if (face != expected || vertices.size() != 4) return false;
			VisualScarGeometry.Vec normal = VisualScarGeometry.basis(face).normal();
			double plane = dot(vertices.getFirst(), normal);
			return vertices.stream().allMatch(vertex -> Math.abs(dot(vertex, normal) - plane) < 0.001);
		}

		/** Reports whether the primitive winding points toward the expected outward face. */
		public boolean windingFacesOutward(VisualScarRules.Face expected) {
			if (face != expected || vertices.size() != 4) return false;
			Vertex first = vertices.get(0);
			Vertex second = vertices.get(1);
			Vertex fourth = vertices.get(3);
			double ax = second.x() - first.x();
			double ay = second.y() - first.y();
			double az = second.z() - first.z();
			double bx = fourth.x() - first.x();
			double by = fourth.y() - first.y();
			double bz = fourth.z() - first.z();
			double cx = ay * bz - az * by;
			double cy = az * bx - ax * bz;
			double cz = ax * by - ay * bx;
			VisualScarGeometry.Vec normal = VisualScarGeometry.basis(face).normal();
			return cx * normal.x() + cy * normal.y() + cz * normal.z() > 0.0;
		}

		private static double dot(Vertex vertex, VisualScarGeometry.Vec vector) {
			return vertex.x() * vector.x() + vertex.y() * vector.y() + vertex.z() * vector.z();
		}
	}

	public record Mesh(VisualScarPresentation.Profile profile, VisualScarPresentation.Motif motif,
			Lod lod, List<Quad> quads, String topologySignature, int recognitionAnchors) {
		public Mesh {
			profile = Objects.requireNonNull(profile, "profile");
			motif = Objects.requireNonNull(motif, "motif");
			lod = Objects.requireNonNull(lod, "lod");
			quads = List.copyOf(quads);
			topologySignature = Objects.requireNonNull(topologySignature, "topologySignature");
			if (motif != profile.motif() || quads.isEmpty() || quads.size() > MAX_QUADS_PER_SCAR
					|| quads.size() * 4 > MAX_VERTICES_PER_SCAR
					|| topologySignature.isBlank() || recognitionAnchors < 1) {
				throw new IllegalArgumentException("invalid bounded motif mesh");
			}
		}

		/** Returns the immutable flattened vertex order for this motif mesh. */
		public List<Vertex> vertices() {
			return quads.stream().flatMap(quad -> quad.vertices().stream()).toList();
		}

		/** Returns a deterministic digest string that changes with emitted geometry. */
		public String geometryDigest() {
			return Integer.toHexString(Objects.hash(motif, lod, vertices()));
		}

		/** Returns the motif identity retained by every nonzero LOD. */
		public VisualScarPresentation.Motif recognisableSilhouette() {
			return motif;
		}
	}

	public record Candidate(long key, double distance) {
		public Candidate {
			if (!Double.isFinite(distance) || distance < 0.0) {
				throw new IllegalArgumentException("invalid candidate distance");
			}
		}
	}

	public record PipelineContract(String pipelineName, String vertexFormat, String topology,
			boolean translucent, boolean cull, boolean reverseDepthTest, boolean depthWrite) {
	}

	public record Batch(List<Long> selectedKeys, List<Vertex> vertices,
			int quadCount, int drawCalls) {
		public Batch {
			selectedKeys = List.copyOf(selectedKeys);
			vertices = List.copyOf(vertices);
			if (selectedKeys.size() > MAX_VISIBLE_SCARS
					|| new java.util.HashSet<>(selectedKeys).size() != selectedKeys.size()
					|| quadCount < 0 || quadCount > MAX_FRAME_QUADS
					|| vertices.size() > MAX_FRAME_VERTICES || vertices.size() != quadCount * 4
					|| drawCalls < 0 || drawCalls > 1
					|| selectedKeys.isEmpty() != (drawCalls == 0)) {
				throw new IllegalArgumentException("invalid bounded visual scar batch");
			}
		}

		/** Returns the exact number of selected visible scars. */
		public int scarCount() {
			return selectedKeys.size();
		}
	}

	private record Line(double u1, double v1, double u2, double v2) {
	}
}
