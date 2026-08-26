package com.powers.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powers.fx.ClientRankTenSilhouetteState;
import com.powers.fx.RankTenSilhouetteGeometry;
import com.powers.fx.RankTenSilhouetteProfile;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Collects all active rank-ten profiles into one bounded depth-tested world-geometry submission. */
public final class ClientRankTenSilhouetteRenderer {
	private static final int MAX_VISIBLE = ClientRankTenSilhouetteState.MAX_CAPACITY;
	private static final int MAX_FRAME_VERTICES = MAX_VISIBLE * 384;
	private static final Object PIPELINE_CONTRACT = RenderPipelines.DEBUG_QUADS;
	private static boolean registered;
	private static volatile boolean resourcesOpen = true;

	private ClientRankTenSilhouetteRenderer() {
	}

	public static void initialize() {
		if (registered) return;
		registered = true;
		LevelRenderEvents.COLLECT_SUBMITS.register(ClientRankTenSilhouetteRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		if (!resourcesOpen || PIPELINE_CONTRACT == null) return;
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		var position = context.levelState().cameraRenderState.pos;
		RankTenSilhouetteGeometry.Camera camera = new RankTenSilhouetteGeometry.Camera(
				position.x, position.y, position.z);
		boolean reducedMotion = client.options.screenEffectScale().get() < 0.45D;
		List<Candidate> candidates = ClientRankTenSilhouetteManager.entries().stream()
				.map(entry -> new Candidate(entry, distanceSquared(entry, camera)))
				.sorted(Comparator.comparingDouble(Candidate::distance)
						.thenComparingLong(candidate -> candidate.entry().eventId()))
				.limit(MAX_VISIBLE).toList();
		List<RankTenSilhouetteGeometry.Vertex> batch = new ArrayList<>();
		for (Candidate candidate : candidates) {
			double phase = reducedMotion ? 0.0
					: ClientRankTenSilhouetteManager.animatedPhase(candidate.entry());
			RankTenSilhouetteGeometry.Mesh mesh = renderActualProfileMesh(candidate.entry(), camera,
					reducedMotion, phase);
			List<RankTenSilhouetteGeometry.Vertex> quads = asDebugQuads(candidate.entry(), mesh);
			if (batch.size() + quads.size() > MAX_FRAME_VERTICES) break;
			batch.addAll(quads);
		}
		if (batch.isEmpty()) return;
		PoseStack pose = new PoseStack();
		context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugQuads(),
				(ignored, consumer) -> batch.forEach(vertex -> consumer.addVertex(
						(float) (vertex.x() - camera.x()),
						(float) (vertex.y() - camera.y()),
						(float) (vertex.z() - camera.z())).setColor(rgbaToArgb(vertex.rgba()))));
	}

	/** Test-visible seam that resolves and expands the actual immutable production profile mesh. */
	public static RankTenSilhouetteGeometry.Mesh renderActualProfileMesh(
			ClientRankTenSilhouetteState.Entry entry, RankTenSilhouetteGeometry.Camera camera,
			boolean reducedMotion, double phase) {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.fromNetworkId(
				entry.wire().profileId()).orElseThrow();
		return RankTenSilhouetteGeometry.mesh(profile, entry.wire().event(phase), camera,
				reducedMotion);
	}

	public static void closeResources() {
		resourcesOpen = false;
		ClientRankTenSilhouetteManager.rendererResourcesClosed();
	}

	public static void recreateResources() {
		resourcesOpen = true;
		ClientRankTenSilhouetteManager.rendererResourcesRecreated();
	}

	private static double distanceSquared(ClientRankTenSilhouetteState.Entry entry,
			RankTenSilhouetteGeometry.Camera camera) {
		double x = entry.wire().x() - camera.x();
		double y = entry.wire().y() - camera.y();
		double z = entry.wire().z() - camera.z();
		return x * x + y * y + z * z;
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

	private static int rgbaToArgb(int rgba) {
		return rgba >>> 8 | rgba << 24;
	}

	private record Candidate(ClientRankTenSilhouetteState.Entry entry, double distance) {
	}
}
