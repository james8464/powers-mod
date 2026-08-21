package com.powers.client;

import com.powers.client.acceptance.VfxGalleryClientAgent;
import com.powers.gametest.VfxGalleryFixture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.item.ItemDisplayContext;

/** Exact-build renderer proof for the VFX-011 model/entity surface. */
public final class VfxGalleryClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.restoreDefaultGameOptions();
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(VfxGalleryClientAgent::begin);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitFor(client -> client.player != null && client.level != null);
			singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.stabilize(
					server.getPlayerList().getPlayers().getFirst()));
			captureItemModels(context);
			captureEntityModels(context, singleplayer);
			VfxUiGallery.capture(context, singleplayer);
		}
	}

	private static void captureEntityModels(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		List<java.util.UUID> profiles = context.computeOnClient(
				client -> VfxGalleryClientAgent.wideAndSlimProfileIds());
		AtomicReference<List<Integer>> ids = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> ids.set(VfxGalleryFixture.spawnRendererEntities(
				server.getPlayerList().getPlayers().getFirst(), profiles.get(0), profiles.get(1))));
		context.waitFor(client -> ids.get() != null && ids.get().stream().allMatch(id ->
				client.level != null && client.level.getEntity(id) != null));
		List<net.minecraft.world.entity.player.PlayerModelType> models = context.computeOnClient(
				client -> VfxGalleryClientAgent.avatarModels(client, ids.get()));
		if (!models.equals(List.of(net.minecraft.world.entity.player.PlayerModelType.WIDE,
				net.minecraft.world.entity.player.PlayerModelType.SLIM,
				net.minecraft.world.entity.player.PlayerModelType.WIDE,
				net.minecraft.world.entity.player.PlayerModelType.SLIM))) {
			throw new AssertionError("Shadow/Echo wide/slim production renderer resolution drifted: " + models);
		}
		for (VfxGalleryClientAgent.EntityView view : VfxGalleryClientAgent.EntityView.values()) {
			String mode = view.name().toLowerCase(java.util.Locale.ROOT);
			String name = "vfx011-entities-" + mode;
			context.setScreen(() -> VfxGalleryClientAgent.entityScreen(name, ids.get(), view,
					VfxGalleryClientAgent.Background.CHECKER));
			context.waitTick();
			var screenshot = context.takeScreenshot(name);
			List<String> captures = entityCaptureIds(mode);
			context.runOnClient(client -> VfxGalleryClientAgent.record(client,
					screenshot.getFileName().toString(), captures, 4, false,
					VfxGalleryClientAgent.Background.CHECKER, "entity-" + mode));
		}
		Set<String> configurations = new LinkedHashSet<>();
		for (int mip = 0; mip <= 4; mip++) {
			int currentMip = mip;
			applyConfiguration(context, currentMip, false, true);
			for (boolean reduced : List.of(false, true)) {
				VfxGalleryClientAgent.Background background = VfxGalleryClientAgent.Background.values()[mip % 3];
				if (reduced) applyConfiguration(context, currentMip, true, false);
				for (VfxGalleryClientAgent.EntityView view : VfxGalleryClientAgent.EntityView.values()) {
					String mode = view.name().toLowerCase(java.util.Locale.ROOT);
					String name = "vfx011-entities-config-mip" + mip + "-"
							+ (reduced ? "reduced" : "normal") + "-" + mode;
					context.setScreen(() -> VfxGalleryClientAgent.entityScreen(name, ids.get(), view, background));
					context.waitTick();
					var screenshot = context.takeScreenshot(name);
					List<String> sourceKeys = entityCaptureIds(mode);
					List<String> captures = sourceKeys.stream().map(source -> "configuration/entity/mip"
							+ currentMip + "/" + (reduced ? "reduced" : "normal") + "/" + source).toList();
					configurations.addAll(captures);
					context.runOnClient(client -> VfxGalleryClientAgent.record(client,
							screenshot.getFileName().toString(), captures, sourceKeys, currentMip, reduced,
							background, "entity-" + mode));
				}
			}
		}
		if (configurations.size() != 5 * 2 * VfxGalleryClientAgent.EntityView.values().length * 10) {
			throw new AssertionError("Entity mip/motion/view matrix incomplete: " + configurations.size());
		}
		context.setScreen(() -> null);
	}

	private static List<String> entityCaptureIds(String view) {
		List<String> result = new java.util.ArrayList<>();
		for (String family : List.of("dark_herald", "darkness_creature", "first_vessel",
				"light_herald", "power_test_actor", "radiant_sentinel")) {
			result.add("entity/" + family + "/" + view);
		}
		result.add("entity/shadow/wide/overlay/" + view);
		result.add("entity/shadow/slim/overlay/" + view);
		result.add("entity/echo/wide/overlay/" + view);
		result.add("entity/echo/slim/overlay/" + view);
		return List.copyOf(result);
	}

	private static void captureItemModels(ClientGameTestContext context) {
		List<String> items = VfxGalleryClientAgent.itemIds();
		Set<String> emitted = new LinkedHashSet<>();
		applyConfiguration(context, 4, false, true);
		for (ItemDisplayContext display : VfxGalleryClientAgent.requiredContexts()) {
			capturePages(context, items, display, 4, false,
					VfxGalleryClientAgent.Background.CHECKER, true, emitted);
		}
		capturePages(context, VfxGalleryClientAgent.shelfItemIds(), ItemDisplayContext.ON_SHELF,
				4, false, VfxGalleryClientAgent.Background.CHECKER, true, emitted);
		if (!emitted.equals(VfxGalleryClientAgent.requiredItemCaptureIds())) {
			Set<String> missing = new LinkedHashSet<>(VfxGalleryClientAgent.requiredItemCaptureIds());
			missing.removeAll(emitted);
			Set<String> extra = new LinkedHashSet<>(emitted);
			extra.removeAll(VfxGalleryClientAgent.requiredItemCaptureIds());
			throw new AssertionError("Exact item capture inventory drift; missing=" + missing + ", extra=" + extra);
		}
		for (int mip = 0; mip <= 4; mip++) {
			int currentMip = mip;
			applyConfiguration(context, currentMip, false, true);
			for (boolean reduced : List.of(false, true)) {
				VfxGalleryClientAgent.Background background = switch (mip % 3) {
					case 0 -> VfxGalleryClientAgent.Background.LIGHT;
					case 1 -> VfxGalleryClientAgent.Background.DARK;
					default -> VfxGalleryClientAgent.Background.CHECKER;
				};
				if (reduced) applyConfiguration(context, currentMip, true, false);
				for (ItemDisplayContext display : VfxGalleryClientAgent.requiredContexts()) {
					capturePages(context, items, display, currentMip, reduced,
							background, false, emitted);
				}
				capturePages(context, VfxGalleryClientAgent.shelfItemIds(), ItemDisplayContext.ON_SHELF,
						currentMip, reduced, background, false, emitted);
			}
		}
		long configurationCount = emitted.stream().filter(id -> id.startsWith("configuration/")).count();
		if (configurationCount != 5L * 2L * VfxGalleryClientAgent.requiredItemCaptureIds().size()) {
			throw new AssertionError("Item mip/motion/context matrix incomplete: " + configurationCount);
		}
		context.setScreen(() -> null);
	}

	private static void applyConfiguration(ClientGameTestContext context, int mip,
			boolean reduced, boolean reloadResources) {
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		context.runOnClient(client -> {
			VfxGalleryClientAgent.applyOptions(client, mip, reduced);
			if (reloadResources) reload.set(client.reloadResourcePacks());
		});
		if (!reloadResources) return;
		context.waitFor(client -> reload.get() != null && reload.get().isDone());
		context.computeOnClient(client -> {
			reload.get().join();
			return true;
		});
		// The reload future completes before the branded fade overlay's bounded three-second exit.
		for (int tick = 0; tick < 60; tick++) context.waitTick();
	}

	private static void capturePages(ClientGameTestContext context, List<String> items,
			ItemDisplayContext display, int mip, boolean reduced,
			VfxGalleryClientAgent.Background background, boolean evidenceIds, Set<String> emitted) {
		for (int page = 0; page * 18 < items.size(); page++) {
			int from = page * 18;
			List<String> pageItems = items.subList(from, Math.min(items.size(), from + 18));
			String mode = display.getSerializedName();
			String name = "vfx011-items-" + (evidenceIds ? "evidence-" : "configuration-")
					+ mode + "-mip" + mip + "-"
					+ (reduced ? "reduced" : "normal") + "-page" + String.format("%02d", page + 1);
			context.setScreen(() -> VfxGalleryClientAgent.itemScreen(name, pageItems, display, background));
			context.waitTick();
			var screenshot = context.takeScreenshot(name);
			List<String> ids;
			List<String> sourceKeys = pageItems.stream().map(id -> VfxGalleryClientAgent.captureId(id, display))
					.filter(VfxGalleryClientAgent.requiredItemCaptureIds()::contains).toList();
			if (evidenceIds) {
				ids = sourceKeys;
				emitted.addAll(ids);
			} else {
				ids = sourceKeys.stream().map(source -> "configuration/mip" + mip + "/"
						+ (reduced ? "reduced" : "normal") + "/" + source).toList();
				emitted.addAll(ids);
			}
			List<String> captured = ids;
			context.runOnClient(client -> VfxGalleryClientAgent.record(client,
					screenshot.getFileName().toString(), captured, sourceKeys, mip, reduced, background, mode));
		}
	}
}
