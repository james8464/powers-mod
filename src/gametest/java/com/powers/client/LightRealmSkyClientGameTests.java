package com.powers.client;

import com.google.gson.Gson;
import com.powers.PowersItems;
import com.powers.client.fx.FxAccessibility;
import com.powers.client.realm.LightRealmSkyClientState;
import com.powers.client.realm.LightRealmSkyRenderer;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.testing.TestingOverrides;
import com.powers.visual.LightRealmSkyProfile;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.server.level.ParticleStatus;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Exact-build real-renderer proof for normal, reduced, and fallback Light Realm skies. */
public final class LightRealmSkyClientGameTests implements FabricClientGameTest {
	private static final Gson GSON = new Gson();
	private static final String LIGHT_REALM = "powers:light_realm";
	private static final List<CaptureCase> CAPTURES = List.of(
			new CaptureCase("vfx009/normal/clear/distance4", 4, false, false, false),
			new CaptureCase("vfx009/normal/clear/distance12", 12, false, false, false),
			new CaptureCase("vfx009/normal/clear/distance24", 24, false, false, false),
			new CaptureCase("vfx009/normal/rain/distance12", 12, false, true, false),
			new CaptureCase("vfx009/normal/post_reload/distance12", 12, false, false, true),
			new CaptureCase("vfx009/reduced/clear/distance4", 4, true, false, false),
			new CaptureCase("vfx009/reduced/clear/distance12", 12, true, false, false),
			new CaptureCase("vfx009/reduced/clear/distance24", 24, true, false, false));

	@Override
	public void runTest(ClientGameTestContext context) {
		context.restoreDefaultGameOptions();
		context.getInput().resizeWindow(1280, 720);
		AtomicInteger resourceReloadRevision = new AtomicInteger();
		AtomicBoolean fallbackClosed = new AtomicBoolean();
		AtomicReference<RenderTarget> fallbackRenderTarget = new AtomicReference<>();
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitFor(client -> client.player != null && client.level != null);
			context.runOnClient(LightRealmSkyClientGameTests::beginMetadata);
			try {
				enterLightRealm(singleplayer);
				waitForLightRealm(context, singleplayer);
				for (CaptureCase capture : CAPTURES) {
					prepareCapture(context, singleplayer, capture, resourceReloadRevision);
					capture(context, capture.id(), resourceReloadRevision.get(), expectedEnhancedMode(capture.reduced()));
				}
				prepareFallback(context, singleplayer, fallbackClosed, fallbackRenderTarget);
				capture(context, "vfx009/fallback/clear/distance12", resourceReloadRevision.get(),
						LightRealmSkyProfile.Mode.STATIC_WHITE);
			} finally {
				cleanupAfterFixture(context, singleplayer, fallbackClosed.get(), fallbackRenderTarget.get());
			}
		}
	}

	private static void enterLightRealm(TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			TestingOverrides.setEnergyDisabled(player.getUUID(), true);
			TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
			if (!CrystalPowerRegistry.tryActivate(player, PowersItems.LIGHT_CRYSTAL)) {
				throw new AssertionError("Production Light Crystal rejected VFX-009 realm travel");
			}
		});
	}

	private static void waitForLightRealm(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		singleplayer.getServer().waitFor(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			return LIGHT_REALM.equals(player.level().dimension().identifier().toString())
					&& BodyProxyManager.hasSession(player, BodyProxyKind.REALM);
		});
		context.waitFor(client -> client.level != null && client.player != null
				&& LIGHT_REALM.equals(client.level.dimension().identifier().toString())
				&& client.gui.screen() == null);
		context.waitTicks(20);
	}

	private static void prepareCapture(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, CaptureCase capture,
			AtomicInteger resourceReloadRevision) {
		setWeather(singleplayer, capture.raining());
		context.runOnClient(client -> {
			client.options.renderDistance().set(capture.renderDistance());
			client.options.particles().set(capture.reduced() ? ParticleStatus.MINIMAL : ParticleStatus.ALL);
			client.options.screenEffectScale().set(capture.reduced() ? 0.0 : 1.0);
		});
		if (capture.reloadResources()) reloadResources(context, resourceReloadRevision);
		context.waitFor(client -> client.level != null
				&& FxAccessibility.reducedMotion(client) == capture.reduced()
				&& client.options.renderDistance().get() == capture.renderDistance()
				&& client.level.isRaining() == capture.raining()
				&& observedMode(client) == expectedEnhancedMode(capture.reduced()));
		context.waitTicks(10);
	}

	private static void prepareFallback(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, AtomicBoolean fallbackClosed,
			AtomicReference<RenderTarget> fallbackRenderTarget) {
		setWeather(singleplayer, false);
		context.runOnClient(client -> {
			client.options.renderDistance().set(12);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.screenEffectScale().set(1.0);
			LightRealmSkyClientState state = actualSkyState(client);
			LightRealmSkyRenderer renderer = reflectedField(state, LightRealmSkyRenderer.class);
			RenderTarget renderTarget = reflectedField(renderer, RenderTarget.class);
			if (renderTarget == null) throw new AssertionError("Enhanced renderer has no owned render target");
			fallbackRenderTarget.set(renderTarget);
			state.close();
			fallbackClosed.set(true);
		});
		context.waitFor(client -> client.level != null && !client.level.isRaining()
				&& observedMode(client) == LightRealmSkyProfile.Mode.STATIC_WHITE);
		context.waitTicks(10);
	}

	private static void reloadResources(ClientGameTestContext context,
			AtomicInteger resourceReloadRevision) {
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		context.runOnClient(client -> reload.set(client.reloadResourcePacks()));
		context.waitFor(client -> reload.get() != null && reload.get().isDone());
		context.computeOnClient(client -> {
			reload.get().join();
			resourceReloadRevision.incrementAndGet();
			return true;
		});
		// The reload future completes before the bounded vanilla reload overlay exits.
		context.waitTicks(60);
	}

	private static void setWeather(TestSingleplayerContext singleplayer, boolean raining) {
		singleplayer.getServer().runOnServer(server -> {
			var level = server.getPlayerList().getPlayers().getFirst().level();
			var weather = level.getWeatherData();
			weather.setClearWeatherTime(raining ? 0 : 12_000);
			weather.setRaining(raining);
			weather.setRainTime(12_000);
			weather.setThundering(false);
			weather.setThunderTime(12_000);
			weather.setDirty();
		});
	}

	private static void capture(ClientGameTestContext context, String captureId,
			int resourceReloadRevision, LightRealmSkyProfile.Mode expectedMode) {
		String name = captureId.replace('/', '-');
		Path screenshot = context.takeScreenshot(name);
		context.waitTick();
		context.runOnClient(client -> {
			LightRealmSkyProfile.Mode mode = observedMode(client);
			if (mode != expectedMode) {
				throw new AssertionError("Renderer mode drifted before " + captureId + ": " + mode);
			}
			boolean enhancedRendererAvailable = actualSkyState(client).enhancedAvailable();
			boolean expectedAvailability = mode == LightRealmSkyProfile.Mode.ANCIENT_WHITE
					|| mode == LightRealmSkyProfile.Mode.ANCIENT_WHITE_REDUCED;
			if (enhancedRendererAvailable != expectedAvailability) {
				throw new AssertionError("Effective renderer availability drifted after " + captureId
						+ ": mode=" + mode + ", available=" + enhancedRendererAvailable);
			}
			record(client, screenshot, captureId, mode, enhancedRendererAvailable,
					resourceReloadRevision);
		});
	}

	private static void restoreSkyState(ClientGameTestContext context, RenderTarget renderTarget) {
		if (renderTarget == null) throw new AssertionError("Fallback cleanup lost the renderer target");
		context.runOnClient(client -> {
			SkyRenderer skyRenderer = activeSkyRenderer(client);
			Field stateField = reflectedFieldDefinition(skyRenderer, LightRealmSkyClientState.class);
			try {
				stateField.set(skyRenderer, new LightRealmSkyClientState(renderTarget));
			} catch (IllegalAccessException error) {
				throw new IllegalStateException("Could not restore GameTest-only sky state", error);
			}
		});
		context.waitFor(client -> actualSkyState(client).enhancedAvailable()
				&& observedMode(client) == LightRealmSkyProfile.Mode.ANCIENT_WHITE);
	}

	private static LightRealmSkyProfile.Mode expectedEnhancedMode(boolean reduced) {
		return reduced ? LightRealmSkyProfile.Mode.ANCIENT_WHITE_REDUCED
				: LightRealmSkyProfile.Mode.ANCIENT_WHITE;
	}

	private static LightRealmSkyProfile.Mode observedMode(Minecraft client) {
		LightRealmSkyProfile profile = reflectedField(actualSkyState(client), LightRealmSkyProfile.class);
		if (profile == null) throw new AssertionError("Light Realm sky profile has not been extracted yet");
		return profile.mode();
	}

	private static LightRealmSkyClientState actualSkyState(Minecraft client) {
		SkyRenderer renderer = activeSkyRenderer(client);
		LightRealmSkyClientState state = reflectedField(renderer, LightRealmSkyClientState.class);
		if (state == null) throw new AssertionError("SkyRenderer has no VFX-009 per-instance state");
		return state;
	}

	private static SkyRenderer activeSkyRenderer(Minecraft client) {
		SkyRenderer renderer = client.levelRenderer.skyRenderer();
		if (renderer == null) throw new AssertionError("Client has no active SkyRenderer");
		return renderer;
	}

	private static <T> T reflectedField(Object owner, Class<T> type) {
		if (owner == null) return null;
		Field field = reflectedFieldDefinition(owner, type);
		try {
			return type.cast(field.get(owner));
		} catch (IllegalAccessException error) {
			throw new IllegalStateException("Could not inspect GameTest-only sky state", error);
		}
	}

	private static Field reflectedFieldDefinition(Object owner, Class<?> type) {
		for (Field field : owner.getClass().getDeclaredFields()) {
			if (!type.isAssignableFrom(field.getType())) continue;
			field.setAccessible(true);
			return field;
		}
		throw new AssertionError(owner.getClass().getName() + " has no field of type " + type.getName());
	}

	private static void beginMetadata(Minecraft client) {
		Path metadata = metadataPath(client);
		try {
			Files.createDirectories(metadata.getParent());
			Files.writeString(metadata, "", StandardCharsets.UTF_8);
		} catch (IOException error) {
			throw new IllegalStateException("Could not initialise VFX-009 metadata", error);
		}
	}

	private static void record(Minecraft client, Path screenshot, String captureId,
			LightRealmSkyProfile.Mode rendererMode, boolean enhancedRendererAvailable,
			int resourceReloadRevision) {
		String weather = client.level == null ? "unavailable"
				: client.level.isThundering() ? "thunder" : client.level.isRaining() ? "rain" : "clear";
		RuntimeOptions runtimeOptions = new RuntimeOptions(client.getWindow().getWidth(),
				client.getWindow().getHeight(), client.options.guiScale().get(),
				(int) client.getWindow().getGuiScale(), client.options.particles().get().toString(),
				client.options.screenEffectScale().get(), client.options.renderDistance().get(),
				client.options.graphicsPreset().get().toString(),
				client.getResourcePackRepository().getSelectedIds().stream().sorted().toList(),
				client.level == null ? -1L : client.level.getGameTime());
		CaptureMetadata metadata = new CaptureMetadata(captureId, screenshot.getFileName().toString(),
				screenshotSha256(screenshot), runtimeOptions, rendererMode.name(), enhancedRendererAvailable,
				client.options.renderDistance().get(), FxAccessibility.reducedMotion(client), weather,
				resourceReloadRevision);
		try {
			Files.writeString(metadataPath(client), GSON.toJson(metadata) + "\n",
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException error) {
			throw new IllegalStateException("Could not append VFX-009 metadata", error);
		}
	}

	private static String screenshotSha256(Path screenshot) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(Files.readAllBytes(screenshot)));
		} catch (IOException | NoSuchAlgorithmException error) {
			throw new IllegalStateException("Could not hash VFX-009 screenshot " + screenshot, error);
		}
	}

	private static Path metadataPath(Minecraft client) {
		return client.gameDirectory.toPath().resolve("vfx-009-gallery/vfx009-captures.jsonl");
	}

	private static void cleanupAfterFixture(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, boolean fallbackClosed, RenderTarget renderTarget) {
		Throwable failure = null;
		if (fallbackClosed) {
			failure = captureCleanupFailure(() -> restoreSkyState(context, renderTarget), null);
		}
		failure = captureCleanupFailure(() -> cleanup(singleplayer), failure);
		failure = captureCleanupFailure(context::restoreDefaultGameOptions, failure);
		if (failure instanceof RuntimeException runtime) throw runtime;
		if (failure instanceof Error error) throw error;
	}

	private static Throwable captureCleanupFailure(Runnable cleanup, Throwable firstFailure) {
		try {
			cleanup.run();
		} catch (RuntimeException | Error failure) {
			if (firstFailure == null) return failure;
			firstFailure.addSuppressed(failure);
		}
		return firstFailure;
	}

	private static void cleanup(TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			var players = server.getPlayerList().getPlayers();
			if (players.isEmpty()) return;
			var player = players.getFirst();
			setClear(player.level());
			if (BodyProxyManager.hasSession(player, BodyProxyKind.REALM)) {
				BodyProxyManager.recoverToBody(player);
			}
			TestingOverrides.clear(player.getUUID());
		});
	}

	private static void setClear(net.minecraft.server.level.ServerLevel level) {
		var weather = level.getWeatherData();
		weather.setClearWeatherTime(12_000);
		weather.setRaining(false);
		weather.setRainTime(12_000);
		weather.setThundering(false);
		weather.setThunderTime(12_000);
		weather.setDirty();
	}

	private record CaptureCase(String id, int renderDistance, boolean reduced,
			boolean raining, boolean reloadResources) {
	}

	private record CaptureMetadata(String captureId, String screenshot, String screenshotSha256,
			RuntimeOptions runtimeOptions, String rendererMode, boolean enhancedRendererAvailable, int renderDistance,
			boolean reducedMotion, String weather, int resourceReloadRevision) {
	}

	private record RuntimeOptions(int physicalWidth, int physicalHeight, int requestedGuiScale,
			int effectiveGuiScale, String particles, double screenEffectScale, int renderDistance,
			String graphicsMode, List<String> resourcePacks, long gameTime) {
	}
}
