package com.powers.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powers.fx.ClientVisualScarState;
import com.powers.fx.VisualScarMotifGeometry;
import com.powers.fx.VisualScarPresentation;
import com.powers.fx.VisualScarRules;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds all visible surface motifs into one bounded DEBUG_QUADS submission per frame. */
public final class ClientVisualScarRenderer {
	private static final Object PIPELINE_CONTRACT = RenderPipelines.DEBUG_QUADS;
	private static boolean registered;
	private static volatile boolean resourcesOpen = true;

	private ClientVisualScarRenderer() {
	}

	public static void initialize() {
		if (registered) return;
		registered = true;
		LevelRenderEvents.COLLECT_SUBMITS.register(ClientVisualScarRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		if (!resourcesOpen || PIPELINE_CONTRACT == null) return;
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return;
		Vec3 camera = context.levelState().cameraRenderState.pos;
		List<VisualScarMotifGeometry.Candidate> candidates = new ArrayList<>();
		Map<Long, ClientVisualScarState.Entry> byKey = new HashMap<>();
		for (ClientVisualScarState.Entry entry : ClientVisualScarManager.entries()) {
			BlockPos support = BlockPos.of(entry.position());
			Direction face = Direction.values()[entry.face()];
			BlockPos origin = support.relative(face);
			double x = support.getX() + 0.5 + face.getStepX() * 0.5;
			double y = support.getY() + 0.5 + face.getStepY() * 0.5;
			double z = support.getZ() + 0.5 + face.getStepZ() * 0.5;
			double distance = Math.sqrt(camera.distanceToSqr(x, y, z));
			boolean loaded = client.level.hasChunk(support.getX() >> 4, support.getZ() >> 4)
					&& client.level.hasChunk(origin.getX() >> 4, origin.getZ() >> 4);
			boolean supportValid = loaded && client.level.getBlockState(support)
					.isFaceSturdy(client.level, support, face);
			if (VisualScarMotifGeometry.visibility(distance, true, loaded, supportValid)
					!= VisualScarMotifGeometry.Visibility.VISIBLE) continue;
			long key = candidates.size();
			byKey.put(key, entry);
			candidates.add(new VisualScarMotifGeometry.Candidate(key, distance));
		}
		VisualScarMotifGeometry.Batch batch = VisualScarMotifGeometry.batchNearestCandidates(
				candidates, VisualScarMotifGeometry.MAX_VISIBLE_SCARS,
				VisualScarMotifGeometry.MAX_FRAME_QUADS, VisualScarMotifGeometry.MAX_FRAME_VERTICES,
				candidate -> renderActualMotifMesh(byKey.get(candidate.key()), camera));
		if (batch.vertices().isEmpty()) return;
		PoseStack pose = new PoseStack();
		context.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugQuads(),
				(ignored, consumer) -> batch.vertices().forEach(vertex ->
						consumer.addVertex(vertex.x(), vertex.y(), vertex.z()).setColor(
								VisualScarMotifGeometry.rgbaToArgb(vertex.rgba()))));
	}

	/** Builds actual profile-derived motif geometry; keys and colour swatches are never substitutes. */
	public static VisualScarMotifGeometry.Mesh renderActualMotifMesh(
			ClientVisualScarState.Entry entry, Vec3 camera) {
		BlockPos support = BlockPos.of(entry.position());
		Direction direction = Direction.values()[entry.face()];
		VisualScarRules.Face face = VisualScarRules.Face.valueOf(direction.name());
		double x = support.getX() + 0.5 + direction.getStepX() * 0.5;
		double y = support.getY() + 0.5 + direction.getStepY() * 0.5;
		double z = support.getZ() + 0.5 + direction.getStepZ() * 0.5;
		double distance = Math.sqrt(camera.distanceToSqr(x, y, z));
		VisualScarPresentation.Profile profile = VisualScarPresentation.profile(
				VisualScarRules.Impact.values()[entry.impact()],
				VisualScarRules.Material.values()[entry.material()]);
		return VisualScarMotifGeometry.mesh(profile, face, x, y, z,
				camera.x, camera.y, camera.z, 0.002, 0.96, entry.visualSeed(),
				VisualScarMotifGeometry.lodForDistance(Math.min(256.0, distance)));
	}

	public static void closeResources() {
		resourcesOpen = false;
		ClientVisualScarManager.rendererResourcesClosed();
	}

	public static void recreateResources() {
		resourcesOpen = true;
		ClientVisualScarManager.rendererResourcesRecreated();
	}
}
