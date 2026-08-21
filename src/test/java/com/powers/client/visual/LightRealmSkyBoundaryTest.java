package com.powers.client.visual;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the enhanced sky behind one client-only, fail-closed vanilla renderer boundary. */
class LightRealmSkyBoundaryTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));
	private static final String MIXIN = "src/client/java/com/powers/mixin/LightRealmSkyMixin.java";
	private static final String RENDERER = "src/client/java/com/powers/client/realm/LightRealmSkyRenderer.java";
	private static final String STATE = "src/client/java/com/powers/client/realm/LightRealmSkyClientState.java";

	@Test
	void mixinPreservesWhiteFallbackThenAddsAndClosesOneOwnedRenderer() throws IOException {
		String mixin = source(MIXIN);
		assertTrue(mixin.contains("method = \"<init>\"") && mixin.contains("@At(\"RETURN\")"),
				"owned render state must be constructed once with its SkyRenderer");
		assertTrue(mixin.contains("RenderTarget renderTarget")
				&& mixin.contains("new LightRealmSkyClientState(renderTarget)"),
				"construction must use SkyRenderer's injected target, not a global framebuffer");
		assertFalse(mixin.contains("getMainRenderTarget"), "the renderer cannot capture global target state");
		assertTrue(mixin.contains("state.skyColor = 0xFFFFFFFF"), "static white fallback must remain unconditional");
		assertTrue(mixin.indexOf("state.skyColor = 0xFFFFFFFF") < mixin.indexOf("LightRealmSkyRules.resolve"),
				"profile selection must happen only after installing the white fallback");
		assertFalse(mixin.contains("if (!lightRealm) return"),
				"non-Light-Realm extraction must overwrite any profile retained by a reused SkyRenderer");
		assertTrue(mixin.indexOf("LightRealmSkyRules.resolve") < mixin.indexOf("powers$sky.update(profile)"),
				"every extraction must store the freshly resolved profile, including NONE");
		assertTrue(mixin.contains("FxAccessibility.reducedMotion"), "selector must use the production accessibility rule");
		assertTrue(mixin.contains("@Unique") && mixin.contains("LightRealmSkyClientState"),
				"each vanilla SkyRenderer must own exactly one bounded client state");
		assertTrue(mixin.contains("method = \"renderSkyDisc\"") && mixin.contains("@At(\"TAIL\")"),
				"enhancement must draw only after the vanilla white disc");
		assertTrue(mixin.contains("method = \"close\"") && mixin.contains("powers$sky.close()"),
				"SkyRenderer.close must release the owned enhancement");
	}

	@Test
	void lifecyclePrebuildsBoundedBuffersAndCircuitBreaksToStaticWhite() throws IOException {
		String renderer = source(RENDERER);
		String state = source(STATE);
		assertTrue(renderer.contains("implements AutoCloseable"));
		assertTrue(renderer.contains("RenderPipelines.SUNRISE_SUNSET"),
				"alpha geometry needs Minecraft's built-in translucent position-colour pipeline");
		assertFalse(renderer.contains("RenderPipelines.SKY"), "the opaque sky pipeline cannot blend ancient layers");
		assertTrue(renderer.contains("GpuBuffer") && renderer.contains("buildMeshes"),
				"bounded geometry must be uploaded once outside frame rendering");
		assertTrue(renderer.contains("LightRealmSkyGeometry.build(shape)"),
				"GPU buffers must consume the pure reviewed polygon geometry");
		assertTrue(renderer.contains("for (LightRealmSkyGeometry.DrawRange range : mesh.drawRanges())")
				&& renderer.contains("pass.draw(range.vertexCount(), 1, range.firstVertex(), 0)"),
				"each bounded polygon must retain vanilla's vertex-count/instance-count draw order");
		assertTrue(renderer.indexOf("writeTransform") < renderer.indexOf("createRenderPass"),
				"dynamic uniforms must be written before opening the backend render pass");
		assertTrue(renderer.contains("tryRender(LightRealmSkyProfile"));
		assertTrue(renderer.contains("circuitBroken") && renderer.contains("failureLogged"),
				"one failed enhanced draw must log once and leave later frames on static white");
		assertTrue(renderer.contains("closed") && renderer.contains("void close()"),
				"buffer cleanup must be idempotent");
		assertTrue(count(renderer, "catch (RuntimeException | LinkageError failure)") >= 2,
				"draw and cleanup linkage incompatibilities must fail to static white without catching VM errors");
		assertTrue(renderer.contains("for (Mesh mesh : meshes)") && renderer.contains("closeFailureLogged"),
				"cleanup must attempt every owned buffer and log compatibility failure once");
		assertTrue(state.contains("LightRealmSkyProfile") && state.contains("LightRealmSkyRenderer"));
		assertTrue(state.contains("enhancedAvailable") && state.contains("tryRender") && state.contains("close"));
		assertTrue(state.contains("catch (RuntimeException | LinkageError failure)"),
				"optional renderer construction must fail to the static fallback on linkage incompatibility");
		assertTrue(state.contains("finally") && state.contains("renderer = null"),
				"state ownership must be revoked even if enhancement cleanup fails");
	}

	@Test
	void boundaryHasNoShaderTextureSodiumOrOuterRendererCoupling() throws IOException {
		String mixin = source(MIXIN);
		String procedural = source(RENDERER) + source(STATE);
		String combined = mixin + procedural;
		assertFalse(combined.contains("LevelRenderer"), "outer world-renderer replacement is Sodium-fragile");
		assertFalse(combined.toLowerCase().contains("sodium"), "production code must not detect compatibility mods");
		assertFalse(combined.contains("RenderPipeline.builder"), "the feature must not define a custom pipeline/shader");
		assertFalse(procedural.contains("ResourceLocation") || procedural.contains("Identifier texture")
				|| procedural.contains("TextureManager"), "procedural sky geometry cannot depend on textures");
		assertTrue(mixin.contains("TextureManager textures"),
				"the target constructor signature may name its vanilla texture argument without using it");
		assertFalse(combined.contains("net.minecraft.server") || combined.contains("ServerLevel"),
				"the enhancement must remain entirely client-side");
	}

	private static String source(String relative) throws IOException {
		Path path = ROOT.resolve(relative);
		assertTrue(Files.isRegularFile(path), "missing production boundary: " + relative);
		return Files.readString(path);
	}

	private static int count(String source, String needle) {
		return (source.length() - source.replace(needle, "").length()) / needle.length();
	}
}
