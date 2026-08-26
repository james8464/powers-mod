package com.powers.fx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure nearest-first renderer preparation with camera-relative double-precision expansion. */
public final class RankTenSilhouetteRenderBatch {
	public static final int MAX_VISIBLE = ClientRankTenSilhouetteState.MAX_CAPACITY;
	public static final int MAX_FRAME_VERTICES = MAX_VISIBLE * 384;
	public static final double MAX_RENDER_DISTANCE = 384.0;
	private static final double MINOR_WIDTH_PER_BLOCK = 0.0035;
	private static final double MAX_DISTANCE_STABLE_MINOR_WIDTH = 0.36;
	private static final RankTenSilhouetteGeometry.Camera RELATIVE_CAMERA =
			new RankTenSilhouetteGeometry.Camera(0, 0, 0);

	private RankTenSilhouetteRenderBatch() {
	}

	/** Returns the exact built-in pipeline contract consumed by the client-only renderer. */
	public static PipelineContract pipelineContract() {
		return new PipelineContract("DEBUG_QUADS", "POSITION_COLOR", "QUADS",
				true, false, true, false);
	}

	/** Selects nearest entries with stable event-ID ties and prepares at most one bounded draw. */
	public static Batch batch(List<ClientRankTenSilhouetteState.Entry> entries,
			RankTenSilhouetteGeometry.Camera camera, boolean reducedMotion, long lifecycleTick,
			int requestedVisible, int requestedVertices) {
		if (entries == null || camera == null || lifecycleTick < 0) {
			throw new IllegalArgumentException("invalid silhouette batch input");
		}
		int visibleLimit = Math.clamp(requestedVisible, 0, MAX_VISIBLE);
		int vertexLimit = Math.clamp(requestedVertices, 0, MAX_FRAME_VERTICES) / 4 * 4;
		List<Candidate> candidates = entries.stream()
				.map(entry -> new Candidate(entry, distanceSquared(entry, camera)))
				.filter(candidate -> candidate.distanceSquared() <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE)
				.sorted(Comparator.comparingDouble(Candidate::distanceSquared)
						.thenComparingLong(candidate -> candidate.entry().eventId()))
				.limit(visibleLimit).toList();
		List<Long> selected = new ArrayList<>();
		List<RankTenSilhouetteGeometry.Vertex> vertices = new ArrayList<>();
		List<RankTenSilhouetteGeometry.Mesh> meshes = new ArrayList<>();
		for (Candidate candidate : candidates) {
			double phase = reducedMotion ? 0.0
					: animatedPhase(lifecycleTick, candidate.entry().wire().visualSeed());
			RankTenSilhouetteGeometry.Mesh mesh = renderActualProfileMesh(candidate.entry(), camera,
					reducedMotion, phase);
			List<RankTenSilhouetteGeometry.Vertex> quads = asDebugQuads(candidate.entry(), mesh);
			if (vertices.size() + quads.size() > vertexLimit) break;
			selected.add(candidate.entry().eventId());
			vertices.addAll(quads);
			meshes.add(mesh);
		}
		return new Batch(selected, vertices, meshes, selected.isEmpty() ? 0 : 1);
	}

	/** Resolves the actual profile and subtracts the camera in double precision before float output. */
	public static RankTenSilhouetteGeometry.Mesh renderActualProfileMesh(
			ClientRankTenSilhouetteState.Entry entry, RankTenSilhouetteGeometry.Camera camera,
			boolean reducedMotion, double phase) {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.fromNetworkId(
				entry.wire().profileId()).orElseThrow();
		ClientRankTenSilhouetteState.Wire wire = entry.wire();
		RankTenSilhouetteGeometry.Event relative = new RankTenSilhouetteGeometry.Event(
				wire.eventId(), wire.profileId(), wire.caster(), wire.dimension(),
				wire.x() - camera.x(), wire.y() - camera.y(), wire.z() - camera.z(),
				wire.yaw(), wire.pitch(), wire.alignmentId(), wire.visualSeed(),
				wire.lifetimeTicks(), phase);
		RankTenSilhouetteGeometry.Mesh authored = RankTenSilhouetteGeometry.mesh(
				profile, relative, RELATIVE_CAMERA, reducedMotion);
		return distanceStableMesh(profile, authored, Math.sqrt(distanceSquared(entry, camera)));
	}

	/** Keeps authored near width, adds 0.0035 blocks per distance block, and caps at 0.36 blocks. */
	public static double distanceStableMinorWidth(double authoredWidth, double distance) {
		if (!Double.isFinite(authoredWidth) || authoredWidth <= 0 || !Double.isFinite(distance)
				|| distance < 0) throw new IllegalArgumentException("invalid silhouette minor width");
		return Math.max(authoredWidth,
				Math.min(MAX_DISTANCE_STABLE_MINOR_WIDTH, distance * MINOR_WIDTH_PER_BLOCK));
	}

	private static double animatedPhase(long lifecycleTick, int visualSeed) {
		return (lifecycleTick + Integer.toUnsignedLong(visualSeed)) * 0.12D;
	}

	private static double distanceSquared(ClientRankTenSilhouetteState.Entry entry,
			RankTenSilhouetteGeometry.Camera camera) {
		double x = entry.wire().x() - camera.x();
		double y = entry.wire().y() - camera.y();
		double z = entry.wire().z() - camera.z();
		return x * x + y * y + z * z;
	}

	private static RankTenSilhouetteGeometry.Mesh distanceStableMesh(
			RankTenSilhouetteProfile profile, RankTenSilhouetteGeometry.Mesh mesh, double distance) {
		List<RankTenSilhouetteGeometry.Vertex> widened = new ArrayList<>(mesh.vertices().size());
		int cursor = 0;
		for (RankTenSilhouetteProfile.Primitive primitive : profile.primitives()) {
			if (primitive instanceof RankTenSilhouetteProfile.Segment segment) {
				cursor = appendDistanceStableQuad(widened, mesh.vertices(), cursor,
						distanceStableMinorWidth(segment.width(), distance));
			} else if (primitive instanceof RankTenSilhouetteProfile.Ring ring) {
				for (int index = 0; index < ring.segments(); index++) {
					cursor = appendDistanceStableQuad(widened, mesh.vertices(), cursor,
							distanceStableMinorWidth(ring.width(), distance));
				}
			} else {
				for (int index = 0; index < 24; index++) widened.add(mesh.vertices().get(cursor++));
			}
		}
		if (cursor != mesh.vertices().size()) {
			throw new IllegalStateException("invalid silhouette primitive expansion");
		}
		if (widened.equals(mesh.vertices())) return mesh;
		return new RankTenSilhouetteGeometry.Mesh(widened, mesh.primitiveSignature(),
				mesh.outerOutlineSignature(), mesh.phase(), mesh.fillAlpha());
	}

	private static int appendDistanceStableQuad(List<RankTenSilhouetteGeometry.Vertex> target,
			List<RankTenSilhouetteGeometry.Vertex> source, int cursor, double targetWidth) {
		RankTenSilhouetteGeometry.Vertex first = source.get(cursor);
		RankTenSilhouetteGeometry.Vertex second = source.get(cursor + 1);
		RankTenSilhouetteGeometry.Vertex third = source.get(cursor + 2);
		RankTenSilhouetteGeometry.Vertex fourth = source.get(cursor + 3);
		double currentWidth = vertexDistance(first, second);
		if (targetWidth <= currentWidth + 1.0E-7) {
			target.add(first);
			target.add(second);
			target.add(third);
			target.add(fourth);
			return cursor + 4;
		}
		RankTenSilhouetteGeometry.Vertex[] start = widenedPair(first, second, targetWidth);
		RankTenSilhouetteGeometry.Vertex[] end = widenedPair(fourth, third, targetWidth);
		target.add(start[0]);
		target.add(start[1]);
		target.add(end[1]);
		target.add(end[0]);
		return cursor + 4;
	}

	private static RankTenSilhouetteGeometry.Vertex[] widenedPair(
			RankTenSilhouetteGeometry.Vertex positive, RankTenSilhouetteGeometry.Vertex negative,
			double targetWidth) {
		double dx = positive.x() - negative.x();
		double dy = positive.y() - negative.y();
		double dz = positive.z() - negative.z();
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length <= 1.0E-7) throw new IllegalStateException("collapsed silhouette minor axis");
		float centerX = (positive.x() + negative.x()) * 0.5F;
		float centerY = (positive.y() + negative.y()) * 0.5F;
		float centerZ = (positive.z() + negative.z()) * 0.5F;
		double half = targetWidth * 0.5;
		float positiveX = (float) (centerX + dx / length * half);
		float positiveY = (float) (centerY + dy / length * half);
		float positiveZ = (float) (centerZ + dz / length * half);
		RankTenSilhouetteGeometry.Vertex widenedPositive = new RankTenSilhouetteGeometry.Vertex(
				positiveX, positiveY, positiveZ, positive.rgba());
		RankTenSilhouetteGeometry.Vertex widenedNegative = new RankTenSilhouetteGeometry.Vertex(
				2 * centerX - positiveX, 2 * centerY - positiveY, 2 * centerZ - positiveZ,
				negative.rgba());
		return new RankTenSilhouetteGeometry.Vertex[] {widenedPositive, widenedNegative};
	}

	private static double vertexDistance(RankTenSilhouetteGeometry.Vertex first,
			RankTenSilhouetteGeometry.Vertex second) {
		double x = first.x() - second.x();
		double y = first.y() - second.y();
		double z = first.z() - second.z();
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** DEBUG_QUADS needs four vertices; authored disc triangles use a repeated final corner. */
	private static List<RankTenSilhouetteGeometry.Vertex> asDebugQuads(
			ClientRankTenSilhouetteState.Entry entry, RankTenSilhouetteGeometry.Mesh mesh) {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.fromNetworkId(
				entry.wire().profileId()).orElseThrow();
		List<RankTenSilhouetteGeometry.Vertex> result = new ArrayList<>();
		int cursor = 0;
		for (RankTenSilhouetteProfile.Primitive primitive : profile.primitives()) {
			if (primitive instanceof RankTenSilhouetteProfile.Disc) {
				for (int triangle = 0; triangle < 8; triangle++) {
					result.add(mesh.vertices().get(cursor++));
					result.add(mesh.vertices().get(cursor++));
					RankTenSilhouetteGeometry.Vertex finalCorner = mesh.vertices().get(cursor++);
					result.add(finalCorner);
					result.add(finalCorner);
				}
			} else {
				int count = primitive instanceof RankTenSilhouetteProfile.Ring ring
						? ring.segments() * 4 : 4;
				for (int index = 0; index < count; index++) result.add(mesh.vertices().get(cursor++));
			}
		}
		if (cursor != mesh.vertices().size() || result.size() % 4 != 0) {
			throw new IllegalStateException("invalid silhouette quad expansion");
		}
		return List.copyOf(result);
	}

	private record Candidate(ClientRankTenSilhouetteState.Entry entry, double distanceSquared) {
		private Candidate {
			if (entry == null || !Double.isFinite(distanceSquared) || distanceSquared < 0) {
				throw new IllegalArgumentException("invalid silhouette candidate");
			}
		}
	}

	public record PipelineContract(String pipelineName, String vertexFormat, String topology,
			boolean translucent, boolean cull, boolean reverseDepthTest, boolean depthWrite) {
	}

	/** Immutable one-draw batch plus the exact production meshes used to construct it. */
	public record Batch(List<Long> eventIds, List<RankTenSilhouetteGeometry.Vertex> vertices,
			List<RankTenSilhouetteGeometry.Mesh> meshes, int drawCalls) {
		public Batch {
			eventIds = List.copyOf(eventIds);
			vertices = List.copyOf(vertices);
			meshes = List.copyOf(meshes);
			if (eventIds.size() > MAX_VISIBLE || eventIds.size() != meshes.size()
					|| new java.util.HashSet<>(eventIds).size() != eventIds.size()
					|| vertices.size() > MAX_FRAME_VERTICES || vertices.size() % 4 != 0
					|| drawCalls < 0 || drawCalls > 1
					|| eventIds.isEmpty() != (drawCalls == 0)) {
				throw new IllegalArgumentException("invalid silhouette batch");
			}
		}
		public int silhouetteCount() { return eventIds.size(); }
	}
}
