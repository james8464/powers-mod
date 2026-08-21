package com.powers.client.realm;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.powers.PowersMod;
import com.powers.visual.LightRealmSkyGeometry;
import com.powers.visual.LightRealmSkyProfile;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

/** Owns bounded procedural sky meshes and disables enhancement after the first draw failure. */
public final class LightRealmSkyRenderer implements AutoCloseable {
	private final RenderTarget renderTarget;
	private final List<Mesh> meshes;
	private boolean circuitBroken;
	private boolean failureLogged;
	private boolean closeFailureLogged;
	private boolean closed;

	public LightRealmSkyRenderer(RenderTarget renderTarget) {
		this.renderTarget = renderTarget;
		this.meshes = buildMeshes();
	}

	public boolean available() {
		return !closed && !circuitBroken;
	}

	public boolean tryRender(LightRealmSkyProfile profile) {
		if (!available() || profile.mode() == LightRealmSkyProfile.Mode.NONE
				|| profile.mode() == LightRealmSkyProfile.Mode.STATIC_WHITE) return false;
		try {
			render(profile);
			return true;
		} catch (RuntimeException | LinkageError failure) {
			circuitBroken = true;
			if (!failureLogged) {
				failureLogged = true;
				PowersMod.LOGGER.warn("Light Realm enhanced sky disabled; retaining static white fallback", failure);
			}
			return false;
		}
	}

	private void render(LightRealmSkyProfile profile) {
		List<GpuBufferSlice> transforms = new ArrayList<>(profile.layers().size());
		for (LightRealmSkyProfile.Layer layer : profile.layers()) {
			float pulse = (float) (layer.pulseAmplitude() * Math.sin(layer.phase()));
			float alpha = (float) Math.clamp(layer.alpha() + pulse, 0.0, 1.0);
			int color = ARGB.multiplyAlpha(layer.color(), alpha);
			Matrix4f transform = RenderSystem.getModelViewMatrixCopy()
					.rotateY((float) (profile.rotationRadians() + layer.phase()))
					.scale((float) layer.scale());
			transforms.add(RenderSystem.getDynamicUniforms().writeTransform(
					transform, ARGB.vector4fFromARGB32(color)));
		}
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "POWERS ancient-white sky", renderTarget.getColorTextureView(), Optional.empty(),
				renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(RenderPipelines.SUNRISE_SUNSET);
			RenderSystem.bindDefaultUniforms(pass);
			for (int index = 0; index < profile.layers().size(); index++) {
				pass.setUniform("DynamicTransforms", transforms.get(index));
				Mesh mesh = meshes.get(index);
				pass.setVertexBuffer(0, mesh.buffer().slice());
				for (LightRealmSkyGeometry.DrawRange range : mesh.drawRanges()) {
					pass.draw(range.vertexCount(), 1, range.firstVertex(), 0);
				}
			}
		}
	}

	private static List<Mesh> buildMeshes() {
		List<Mesh> result = new ArrayList<>(LightRealmSkyProfile.Shape.values().length);
		try {
			for (LightRealmSkyProfile.Shape shape : LightRealmSkyProfile.Shape.values()) {
				result.add(buildMesh(shape));
			}
			return List.copyOf(result);
		} catch (RuntimeException | LinkageError failure) {
			for (Mesh mesh : result) {
				try {
					mesh.close();
				} catch (RuntimeException | LinkageError cleanupFailure) {
					failure.addSuppressed(cleanupFailure);
				}
			}
			throw failure;
		}
	}

	private static Mesh buildMesh(LightRealmSkyProfile.Shape shape) {
		LightRealmSkyGeometry.Mesh geometry = LightRealmSkyGeometry.build(shape);
		int bytes = geometry.vertices().size() * DefaultVertexFormat.POSITION_COLOR.getVertexSize();
		try (ByteBufferBuilder storage = ByteBufferBuilder.exactlySized(bytes)) {
			BufferBuilder builder = new BufferBuilder(storage, PrimitiveTopology.TRIANGLE_FAN,
					DefaultVertexFormat.POSITION_COLOR);
			for (LightRealmSkyGeometry.Vertex vertex : geometry.vertices()) {
				builder.addVertex(vertex.x(), vertex.y(), vertex.z()).setColor(vertex.color());
			}
			try (MeshData data = builder.buildOrThrow()) {
				GpuBuffer buffer = RenderSystem.getDevice().createBuffer(
						() -> "POWERS ancient-white " + shape.name().toLowerCase(Locale.ROOT),
						GpuBuffer.USAGE_VERTEX, data.vertexBuffer());
				return new Mesh(buffer, geometry.drawRanges());
			}
		}
	}

	@Override
	public void close() {
		if (closed) return;
		closed = true;
		for (Mesh mesh : meshes) {
			try {
				mesh.close();
			} catch (RuntimeException | LinkageError failure) {
				if (!closeFailureLogged) {
					closeFailureLogged = true;
					PowersMod.LOGGER.warn("Could not release one Light Realm sky buffer; continuing cleanup", failure);
				}
			}
		}
	}

	private record Mesh(GpuBuffer buffer, List<LightRealmSkyGeometry.DrawRange> drawRanges) {
		private void close() {
			if (!buffer.isClosed()) buffer.close();
		}
	}
}
