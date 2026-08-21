package com.powers.client;

import com.powers.PowersEffects;
import com.powers.PowersWeapons;
import com.powers.client.acceptance.VfxGalleryClientAgent;
import com.powers.client.screen.ArcaneCrucibleScreen;
import com.powers.client.screen.CelestialLocatorScreen;
import com.powers.client.screen.ClientAcceptanceScreens;
import com.powers.client.screen.GrimoireIndexScreen;
import com.powers.client.screen.PowerSelectionScreen;
import com.powers.client.screen.RainbowConvergenceScreen;
import com.powers.client.screen.RankMazeScreen;
import com.powers.client.screen.ReservoirTransferScreen;
import com.powers.client.screen.ShadowSwordScreen;
import com.powers.client.screen.TeleportInputScreen;
import com.powers.forge.ArcaneCrucibleMenu;
import com.powers.gametest.VfxGalleryFixture;
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactFavouriteRules;
import com.powers.network.CompanionPackets;
import com.powers.network.PowerStatePayload;
import com.powers.network.RelicPackets;
import com.powers.power.PowerRegistry;
import com.powers.power.abilities.SizeMorphRules;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellIndexEntry;
import com.powers.spell.SpellRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Actual production HUD, boss overlay, screen, and gameplay-camera acceptance gallery. */
final class VfxUiGallery {
	private static final VfxGalleryClientAgent.Background BACKGROUND =
			VfxGalleryClientAgent.Background.CHECKER;
	private static final int DISMOUNT_OVERLAY_SETTLE_TICKS = 65;

	private VfxUiGallery() {
	}

	static void capture(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		Set<String> emitted = new LinkedHashSet<>();
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.clearRendererEntities(
				server.getPlayerList().getPlayers().getFirst()));
		context.waitTicks(2);
		captureScreens(context, singleplayer, emitted);
		captureHud(context, singleplayer, emitted);
		captureGameplayCameras(context, singleplayer, emitted);
		captureBossBars(context, singleplayer, emitted);
		if (emitted.size() != 465) {
			throw new AssertionError("VFX UI exact coverage drifted: " + emitted.size());
		}
	}

	private static void captureScreens(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, Set<String> emitted) {
		for (int scale = 1; scale <= 4; scale++) {
			for (boolean reduced : List.of(false, true)) {
				for (WindowCase window : List.of(new WindowCase("wide", 1280, 720),
						new WindowCase("narrow", 960, 720))) {
					context.getInput().resizeWindow(window.width, window.height);
					int currentScale = scale;
					context.runOnClient(client -> {
						client.options.guiScale().set(currentScale);
						client.resizeGui();
						VfxGalleryClientAgent.applyOptions(client, 4, reduced);
					});
					context.waitTick();
					for (ScreenCase screen : screens(context)) {
						prepareScreenState(context, singleplayer, screen);
						String id = "screen/" + screen.id + "/" + screen.state + "/scale" + scale
								+ "/" + (reduced ? "reduced" : "normal") + "/" + window.id;
						captureScreen(context, screen.factory, id, emitted, reduced);
					}
				}
			}
		}
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
		});
		captureScreen(context, screens(context).stream()
				.filter(screen -> screen.id.equals("artifact_catalogue")
						&& screen.state.equals("selected")).findFirst().orElseThrow().factory,
				"screen/artifact_catalogue/hover/scale2/normal/wide", emitted, false);
		var invalidRef = new java.util.concurrent.atomic.AtomicReference<TeleportInputScreen>();
		context.setScreen(() -> {
			TeleportInputScreen invalid = new TeleportInputScreen(0);
			invalidRef.set(invalid);
			return invalid;
		});
		context.waitTick();
		context.runOnClient(client -> {
			TeleportInputScreen invalid = invalidRef.get();
			if (invalid == null) throw new AssertionError("Teleport error screen was not created");
			if (!invalid.submitAcceptanceCoordinates("not-a-number", "64", "0")) {
				throw new AssertionError("Teleport validation did not retain its production error state");
			}
		});
		context.waitTick();
		String invalidId = "screen/teleport/invalid_coordinates/scale2/normal/wide";
		var invalidShot = context.takeScreenshot(fileName(invalidId));
		record(context, invalidShot.getFileName().toString(), invalidId, false, "screen");
		emitted.add(invalidId);
	}

	private static void prepareScreenState(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, ScreenCase screen) {
		if (!screen.id.equals("advancement_roots")) return;
		boolean darkness = screen.state.equals("darkness");
		String rootId = darkness ? "powers:darkness_root" : "powers:skill_root";
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.configureAdvancementPath(
				server.getPlayerList().getPlayers().getFirst(), darkness));
		context.waitFor(client -> client.player.connection.getAdvancements()
				.get(net.minecraft.resources.Identifier.parse(rootId)) != null);
	}

	private static List<ScreenCase> screens(ClientGameTestContext context) {
		List<ArtifactActionSnapshot> snapshots = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS)
				.stream().map(action -> new ArtifactActionSnapshot(action.key(), action.category(),
						action.energyCost(), 0, 0, action.baseCooldownTicks(), false, false,
						action.abilityId().equals("size_shift") ? SizeMorphRules.normalOption() : -1)).toList();
		List<String> favourites = ArtifactFavouriteRules.defaults(
				ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS), 10,
				"innate/lightning_strike");
		var celestial = SpellRegistry.defaults().forTexture("book_grimoire_celestial");
		List<SpellIndexEntry> spells = celestial.spells().stream().map(SpellIndexEntry::from).toList();
		List<String> rainbow = CrystalAbilityCatalog.defaults().get("rainbow_crystal");
		List<ScreenCase> result = new ArrayList<>();
		result.add(new ScreenCase("teleport", "default", () -> new TeleportInputScreen(0)));
		result.add(new ScreenCase("teleport", "artifact", () -> TeleportInputScreen.artifact(
				100L, "darkness", "innate/time_shift")));
		result.add(new ScreenCase("locator", "entity", () -> new CelestialLocatorScreen(
				CelestialSearchMode.ENTITY, new java.util.UUID(0L, 1L))));
		result.add(new ScreenCase("locator", "world", () -> new CelestialLocatorScreen(
				CelestialSearchMode.WORLD, new java.util.UUID(0L, 2L))));
		result.add(new ScreenCase("rank_maze_light", "focused", () -> rankScreen(false)));
		result.add(new ScreenCase("rank_maze_dark", "focused", () -> rankScreen(true)));
		result.add(new ScreenCase("power_selection", "selected", () -> new PowerSelectionScreen(0,
				PowerRegistry.get("size_shift").ability(), SizeMorphRules.normalOption())));
		result.add(new ScreenCase("artifact_catalogue", "selected", () ->
				ClientAcceptanceScreens.artifactCatalogue(100L, "darkness", "innate/lightning_strike",
						10, SizeMorphRules.normalOption(), 100, favourites, snapshots)));
		result.add(new ScreenCase("artifact_catalogue", "empty", () ->
				ClientAcceptanceScreens.artifactCatalogue(100L, "darkness", "",
						10, SizeMorphRules.normalOption(), 100,
						List.of("", "", "", "", "", "", "", ""), List.of())));
		result.add(new ScreenCase("shadow_sword", "selected", () -> new ShadowSwordScreen(100L,
				"darkness", "innate/lightning_strike", 10, SizeMorphRules.normalOption(), 100,
				favourites, snapshots)));
		result.add(new ScreenCase("grimoire_index", "preview", () -> new GrimoireIndexScreen(
				100L, celestial.key(), Math.min(1, spells.size() - 1), spells)));
		result.add(new ScreenCase("grimoire_index", "empty", () -> new GrimoireIndexScreen(
				100L, celestial.key(), 0, List.of())));
		result.add(new ScreenCase("rainbow_convergence", "sevenfold", () ->
				new RainbowConvergenceScreen(100L, rainbow, 5)));
		result.add(new ScreenCase("reservoir_transfer", "partial", () ->
				new ReservoirTransferScreen(new RelicPackets.OpenReservoirPayload(0, 55, 100, 30, 80, 40, 0))));
		result.add(new ScreenCase("arcane_crucible", "empty", () -> {
			var client = net.minecraft.client.Minecraft.getInstance();
			ArcaneCrucibleMenu menu = new ArcaneCrucibleMenu(91, client.player.getInventory(), BlockPos.ZERO);
			return new ArcaneCrucibleScreen(menu, client.player.getInventory(),
					Component.translatable("block.powers.arcane_crucible"));
		}));
		result.add(new ScreenCase("arcane_crucible", "populated_darkness", () -> {
			var client = net.minecraft.client.Minecraft.getInstance();
			ArcaneCrucibleMenu menu = new ArcaneCrucibleMenu(92, client.player.getInventory(), BlockPos.ZERO);
			menu.getSlot(0).set(net.minecraft.world.item.Items.DIAMOND_SWORD.getDefaultInstance());
			menu.getSlot(1).set(com.powers.PowersBlocks.DARKNESS.asItem().getDefaultInstance());
			if (menu.choices().isEmpty()) throw new AssertionError("Populated Crucible fixture has no choices");
			return new ArcaneCrucibleScreen(menu, client.player.getInventory(),
					Component.translatable("block.powers.arcane_crucible"));
		}));
		result.add(new ScreenCase("advancement_roots", "skill", () -> advancementScreen("powers:skill_root")));
		return List.copyOf(result);
	}

	private static Screen advancementScreen(String id) {
		var client = net.minecraft.client.Minecraft.getInstance();
		var advancements = client.player.connection.getAdvancements();
		var root = advancements.get(net.minecraft.resources.Identifier.parse(id));
		if (root == null) throw new AssertionError("Missing loaded POWERS advancement root " + id);
		advancements.setSelectedTab(root, false);
		return new AdvancementsScreen(advancements);
	}

	private static Screen rankScreen(boolean darkness) {
		updatePowerState(darkness, false, 100, List.of(), List.of(0, 0, 0), List.of(0, 0, 0));
		return new RankMazeScreen();
	}

	private static void captureScreen(ClientGameTestContext context, Supplier<Screen> factory,
			String id, Set<String> emitted, boolean reduced) {
		context.setScreen(factory);
		context.getInput().setCursorPos(id.contains("/hover/") ? 145 : 2,
				id.contains("/hover/") ? 185 : 2);
		context.waitTick();
		var screenshot = context.takeScreenshot(fileName(id));
		record(context, screenshot.getFileName().toString(), id, reduced, "screen");
		if (!emitted.add(id)) throw new AssertionError("Duplicate VFX screen ID " + id);
	}

	private static void captureHud(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, Set<String> emitted) {
		context.setScreen(() -> null);
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
		});
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.teleportDimension(
				server.getPlayerList().getPlayers().getFirst(), "powers:dark_realm"));
		context.waitFor(client -> client.level != null && client.level.dimension().identifier()
				.toString().equals("powers:dark_realm"));
		context.waitTicks(10);
		context.waitFor(client -> client.gui.screen() == null);
		for (boolean reduced : List.of(false, true)) {
			prepareOrdinaryHud(context, singleplayer);
			context.runOnClient(client -> VfxGalleryClientAgent.applyOptions(client, 4, reduced));
			captureHudCombination(context, "realm_dark", reduced, emitted, () ->
					updatePowerState(true, false, 75, List.of(), List.of(0, 0, 0), List.of(1, 1, 1)));
		}
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.teleportDimension(
				server.getPlayerList().getPlayers().getFirst(), "minecraft:overworld"));
		context.waitFor(client -> client.level != null && client.level.dimension().identifier()
				.toString().equals("minecraft:overworld"));
		context.waitTicks(10);
		context.waitFor(client -> client.gui.screen() == null);
		for (boolean reduced : List.of(false, true)) {
			prepareOrdinaryHud(context, singleplayer);
			context.runOnClient(client -> PrivateCompanionClient.clear());
			context.runOnClient(client -> VfxGalleryClientAgent.applyOptions(client, 4, reduced));
			for (HudMode mode : HudMode.values()) {
				int first = mode == HudMode.EMPTY ? 0 : mode == HudMode.DAMPENED ? 0 : 1;
				int last = mode == HudMode.EMPTY ? 0 : 20;
				for (int half = first; half <= last; half++) {
					int currentHalf = half;
					context.runOnClient(client -> {
						if (mode == HudMode.DAMPENED) client.player.addEffect(new MobEffectInstance(
								PowersEffects.AMETHYST_POISONING, 20_000, 0, false, false));
						else client.player.removeEffect(PowersEffects.AMETHYST_POISONING);
						updatePowerState(mode == HudMode.DARKNESS, mode == HudMode.PROJECTION,
								currentHalf * 5, List.of("powers:flight"), List.of(18, 40, 0),
								List.of(80, 100, 1));
					});
					context.waitTick();
					String id = "hud/energy/" + mode.name().toLowerCase(java.util.Locale.ROOT)
							+ "/half" + half + "/" + (reduced ? "reduced" : "normal");
					var screenshot = context.takeScreenshot(fileName(id));
					record(context, screenshot.getFileName().toString(), id, reduced, "hud");
					if (!emitted.add(id)) throw new AssertionError("Duplicate VFX HUD ID " + id);
				}
			}
			captureHudCombination(context, "slots_cooldowns", reduced, emitted, () ->
					updatePowerState(false, false, 65, List.of("powers:flight"),
							List.of(60, 25, 0), List.of(100, 100, 1)));
			captureHudCombination(context, "shadow_active", reduced, emitted, () -> {
				var client = net.minecraft.client.Minecraft.getInstance();
				PrivateCompanionClient.handleStatus(new CompanionPackets.StatusPayload(client.player.getUUID(),
						true, 72, 100, "guard", true, false, 0));
			});
			captureHudCombination(context, "shadow_recall", reduced, emitted, () -> {
				var client = net.minecraft.client.Minecraft.getInstance();
				PrivateCompanionClient.handleStatus(new CompanionPackets.StatusPayload(client.player.getUUID(),
						false, 0, 100, "guard", false, false, 120));
			});
			captureHudCombination(context, "astral", reduced, emitted, () ->
					updatePowerState(false, true, 75, List.of(), List.of(0, 0, 0), List.of(1, 1, 1)));
			for (String vanilla : List.of("low_health", "armor", "air", "mount", "spectator")) {
				prepareVanillaHudTransition(context, singleplayer, vanilla);
				singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.configureVanillaHud(
						server.getPlayerList().getPlayers().getFirst(), vanilla));
				if (vanilla.equals("mount")) awaitHudState(context, false, true);
				else if (vanilla.equals("spectator")) awaitHudState(context, true, false);
				else awaitHudState(context, false, false);
				context.waitTicks(2);
				captureHudCombination(context, "vanilla_" + vanilla, reduced, emitted, () ->
						updatePowerState(false, false, 65, List.of("powers:flight"),
								List.of(20, 0, 0), List.of(100, 1, 1)));
			}
		}
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.configureVanillaHud(
				server.getPlayerList().getPlayers().getFirst(), "default"));
		context.waitTicks(2);
		PrivateCompanionClient.clear();
	}

	private static void prepareOrdinaryHud(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.configureVanillaHud(
				server.getPlayerList().getPlayers().getFirst(), "default"));
		awaitHudState(context, false, false);
		// Vanilla's dismount prompt outlives the vehicle. Settle every motion half so
		// no state retained by the preceding half can contaminate its first capture.
		context.waitTicks(DISMOUNT_OVERLAY_SETTLE_TICKS);
	}

	private static void prepareVanillaHudTransition(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, String nextState) {
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.configureVanillaHud(
				server.getPlayerList().getPlayers().getFirst(), "default"));
		awaitHudState(context, false, false);
		if (nextState.equals("spectator")) {
			// The preceding mount capture intentionally displays the dismount prompt;
			// spectator acceptance must begin only after that bounded vanilla overlay expires.
			context.waitTicks(DISMOUNT_OVERLAY_SETTLE_TICKS);
		}
	}

	private static void awaitHudState(ClientGameTestContext context,
			boolean spectator, boolean riding) {
		context.waitFor(client -> client.player != null && client.gameMode != null
				&& client.gameMode.getPlayerMode() == (spectator ? GameType.SPECTATOR : GameType.SURVIVAL)
				&& client.player.isSpectator() == spectator
				&& (client.player.getVehicle() != null) == riding);
	}

	private static void captureHudCombination(ClientGameTestContext context, String state,
			boolean reduced, Set<String> emitted, Runnable setup) {
		context.runOnClient(client -> setup.run());
		context.waitTick();
		String id = "hud/combination/" + state + "/" + (reduced ? "reduced" : "normal");
		var screenshot = context.takeScreenshot(fileName(id));
		record(context, screenshot.getFileName().toString(), id, reduced, "hud");
		if (!emitted.add(id)) throw new AssertionError("Duplicate HUD combination " + id);
	}

	private static void captureGameplayCameras(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, Set<String> emitted) {
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			VfxGalleryFixture.configureGameplay(player);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					PowersWeapons.weapon("solstice").getDefaultInstance());
		});
		context.waitFor(client -> client.player != null && client.player.getVehicle() == null);
		// Vanilla retains its dismount overlay after the authoritative vehicle is gone.
		context.waitTicks(65);
		captureWorld(context, "gameplay/first_person/solstice", emitted, "first-person");
		context.getInput().pressKey(options -> options.keyTogglePerspective);
		context.waitTicks(3);
		captureWorld(context, "gameplay/third_person/solstice", emitted, "third-person");
		context.getInput().pressKey(options -> options.keyTogglePerspective);
		context.waitTick();
	}

	private static void captureBossBars(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, Set<String> emitted) {
		for (BossCase boss : List.of(
				new BossCase("light_herald", 0.72F), new BossCase("dark_herald", 0.38F),
				new BossCase("first_vessel/opening", 0.90F),
				new BossCase("first_vessel/unbound", 0.52F),
				new BossCase("first_vessel/last_covenant", 0.14F))) {
			singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.showBoss(
					server.getPlayerList().getPlayers().getFirst(), boss.id, boss.healthRatio));
			context.waitTicks(4);
			captureWorld(context, "boss/" + boss.id + "/progress" + Math.round(boss.healthRatio * 100),
					emitted, "boss-overlay");
		}
		singleplayer.getServer().runOnServer(server -> VfxGalleryFixture.clearBosses(
				server.getPlayerList().getPlayers().getFirst()));
	}

	private static void captureWorld(ClientGameTestContext context, String id,
			Set<String> emitted, String camera) {
		context.setScreen(() -> null);
		var screenshot = context.takeScreenshot(fileName(id));
		record(context, screenshot.getFileName().toString(), id, false, camera);
		if (!emitted.add(id)) throw new AssertionError("Duplicate world capture " + id);
	}

	private static void updatePowerState(boolean darkness, boolean projection, int energy,
			List<String> toggles, List<Integer> cooldowns, List<Integer> maximums) {
		ClientPowerState.update(new PowerStatePayload(
				List.of("powers:flight", "powers:forcefield", "powers:double_health"), toggles,
				cooldowns, maximums, List.of(0, 12, 0), energy, 100, darkness, darkness,
				projection, SizeMorphRules.normalOption(),
				List.of("legacy_0", "legacy_1", "legacy_2"), "legacy_2", 2));
	}

	private static void record(ClientGameTestContext context, String screenshot, String id,
			boolean reduced, String camera) {
		context.runOnClient(client -> VfxGalleryClientAgent.record(client, screenshot, List.of(id),
				4, reduced, BACKGROUND, camera));
	}

	private static String fileName(String id) {
		return "vfx011-" + id.replace('/', '-');
	}

	private enum HudMode { NORMAL, EMPTY, DAMPENED, DARKNESS, PROJECTION }
	private record ScreenCase(String id, String state, Supplier<Screen> factory) { }
	private record WindowCase(String id, int width, int height) { }
	private record BossCase(String id, float healthRatio) { }
}
