package com.powers.client.visual;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** No-launch contract for the post-soak real-client VFX-009 proof. */
class LightRealmSkyGalleryContractTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));
	private static final Path GALLERY = ROOT.resolve(
			"src/gametest/java/com/powers/client/LightRealmSkyClientGameTests.java");

	@Test
	void exactVisualMatrixIsRegisteredAndCannotSilentlyShrink() throws IOException {
		String descriptor = Files.readString(ROOT.resolve("src/gametest/resources/fabric.mod.json"));
		assertTrue(descriptor.contains("com.powers.client.LightRealmSkyClientGameTests"),
				"the real renderer proof must be a registered Fabric client GameTest");
		String source = gallery();
		for (String captureId : List.of(
				"vfx009/normal/clear/distance4", "vfx009/normal/clear/distance12",
				"vfx009/normal/clear/distance24", "vfx009/normal/rain/distance12",
				"vfx009/normal/post_reload/distance12", "vfx009/reduced/clear/distance4",
				"vfx009/reduced/clear/distance12", "vfx009/reduced/clear/distance24",
				"vfx009/fallback/clear/distance12")) {
			assertTrue(source.contains(captureId), "missing exact sky capture " + captureId);
		}
	}

	@Test
	void fixtureUsesRealRealmTravelAndObservedClientStates() throws IOException {
		String source = gallery();
		assertTrue(source.contains("CrystalPowerRegistry.tryActivate") && source.contains("PowersItems.LIGHT_CRYSTAL"),
				"fixture must enter through the production Light Crystal travel route");
		assertTrue(source.contains("powers:light_realm") && source.contains("BodyProxyKind.REALM"));
		assertTrue(source.contains("context.waitTicks(100)"),
				"production travel particles must settle before normal-mode sky screenshots are judged");
		assertTrue(source.contains("client.player.setXRot(-55.0F)")
				&& source.contains("client.gameRenderer.mainCamera().xRot()"),
				"every renderer capture must observe a deterministic upward real-client camera orientation");
		assertTrue(source.contains("client.options.renderDistance().set"));
		assertTrue(source.contains("client.reloadResourcePacks()"));
		assertTrue(source.contains("getCommands().getDispatcher().execute")
					&& source.contains("weather rain") && source.contains("weather clear"),
				"fixture must use the production weather command so the client observes rain state");
		assertTrue(source.contains("weatherCommandRain"),
				"rain-command intent and observed client weather must be recorded separately");
		assertTrue(source.contains("FxAccessibility.reducedMotion"),
				"normal/reduced screenshots require observed production accessibility state");
		assertTrue(source.contains("LightRealmSkyClientState") && source.contains("Mode.STATIC_WHITE"),
				"fallback proof must observe the real closed/unavailable renderer state, not fabricate metadata");
		assertTrue(source.contains("restoreSkyState") && source.contains("enhancedAvailable()"),
				"terminal fallback state must be restored and enhanced rows must prove renderer availability");
		assertTrue(source.contains("profile != null && profile.mode() == expectedMode"),
				"fallback waits must tolerate one render extraction before observing the replacement profile");
		assertTrue(source.contains("cleanupAfterFixture") && source.contains("addSuppressed"),
				"renderer, realm/body, override, and option cleanup failures must not skip later cleanup phases");
	}

	@Test
	void everyRawScreenshotOwnsClientEmittedDigestAndRuntimeMetadata() throws IOException {
		String source = gallery();
		for (String field : List.of("screenshotSha256", "runtimeOptions", "rendererMode",
				"enhancedRendererAvailable", "renderDistance", "reducedMotion", "weather",
				"resourceReloadRevision")) {
			assertTrue(source.contains(field), "missing client-emitted metadata field " + field);
		}
		assertTrue(source.contains("vfx009-captures.jsonl"));
		assertTrue(source.contains("takeScreenshot") && source.indexOf("takeScreenshot") < source.indexOf("screenshotSha256"),
				"digest must be emitted from the screenshot bytes after capture");
	}

	@Test
	void realSkyGalleryCanRunWithoutTheIndependentVfx011ClientMatrix() throws IOException {
		String build = Files.readString(ROOT.resolve("build.gradle"));
		assertTrue(build.contains("vfx009ClientOnly"),
				"the bounded VFX-009 proof needs an explicit client-run selection");
		assertTrue(build.contains("vfx009SodiumJar") && build.contains("runtimeOnly files"),
				"the pinned Sodium gallery must enter the client runtime through an explicit property");
		assertTrue(build.contains("line.contains(\"com.powers.gametest.PowersClientGameTests\")")
					&& build.contains("line.contains(\"com.powers.client.VfxGalleryClientGameTests\")"),
				"VFX-009-only runs must exclude only the two unrelated client entrypoints");
		String descriptor = Files.readString(ROOT.resolve("src/gametest/resources/fabric.mod.json"));
		assertTrue(descriptor.indexOf("com.powers.client.VfxGalleryClientGameTests")
					< descriptor.indexOf("com.powers.client.LightRealmSkyClientGameTests\"\n"),
				"the retained VFX-009 entrypoint must be terminal after filtering");
	}

	private static String gallery() throws IOException {
		assertTrue(Files.isRegularFile(GALLERY), "missing post-soak Light Realm client gallery fixture");
		return Files.readString(GALLERY);
	}
}
