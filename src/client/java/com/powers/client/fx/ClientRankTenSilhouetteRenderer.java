package com.powers.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powers.fx.ClientRankTenSilhouetteState;
import com.powers.fx.RankTenSilhouetteGeometry;
import com.powers.fx.RankTenSilhouetteRenderBatch;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Collects all active rank-ten profiles into one bounded depth-tested world-geometry submission. */
public final class ClientRankTenSilhouetteRenderer {
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
		RankTenSilhouetteRenderBatch.Batch batch = RankTenSilhouetteRenderBatch.batch(
				ClientRankTenSilhouetteManager.entries(), camera, reducedMotion,
				ClientRankTenSilhouetteManager.lifecycleTick(), RankTenSilhouetteRenderBatch.MAX_VISIBLE,
				RankTenSilhouetteRenderBatch.MAX_FRAME_VERTICES);
		if (batch.vertices().isEmpty()) return;
		PoseStack pose = new PoseStack();
		context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugQuads(),
				(ignored, consumer) -> batch.vertices().forEach(vertex -> consumer.addVertex(
						vertex.x(), vertex.y(), vertex.z()).setColor(rgbaToArgb(vertex.rgba()))));
	}

	/** Test-visible seam that resolves and expands the actual immutable production profile mesh. */
	public static RankTenSilhouetteGeometry.Mesh renderActualProfileMesh(
			ClientRankTenSilhouetteState.Entry entry, RankTenSilhouetteGeometry.Camera camera,
			boolean reducedMotion, double phase) {
		return RankTenSilhouetteRenderBatch.renderActualProfileMesh(entry, camera, reducedMotion, phase);
	}

	public static void closeResources() {
		resourcesOpen = false;
		ClientRankTenSilhouetteManager.rendererResourcesClosed();
	}

	public static void recreateResources() {
		resourcesOpen = true;
		ClientRankTenSilhouetteManager.rendererResourcesRecreated();
	}

	private static int rgbaToArgb(int rgba) {
		return rgba >>> 8 | rgba << 24;
	}

}
