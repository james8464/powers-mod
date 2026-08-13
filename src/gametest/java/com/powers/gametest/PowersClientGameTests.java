package com.powers.gametest;

import com.powers.PowersItems;
import com.powers.client.ClientPowerState;
import com.powers.client.fx.ClientMagicFx;
import com.powers.client.screen.ArtifactCatalogueScreen;
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
import com.powers.item.artifact.ArtifactActionCatalogue;
import com.powers.item.artifact.ArtifactActionSnapshot;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.item.artifact.ArtifactFavouriteRules;
import com.powers.network.PowerStatePayload;
import com.powers.network.RelicPackets;
import com.powers.mind.BodyProxyKind;
import com.powers.mind.BodyProxyManager;
import com.powers.power.PowerRegistry;
import com.powers.power.abilities.SizeMorphRules;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellIndexEntry;
import com.powers.spell.SpellRegistry;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Runs acceptance checks that require a real Minecraft client, renderer, and
 * integrated server. Server-only GameTests cannot prove these client paths.
 */
public final class PowersClientGameTests implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.restoreDefaultGameOptions();
        context.getInput().resizeWindow(1280, 720);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            context.runOnClient(client -> {
                if (client.player == null || client.level == null) {
                    throw new AssertionError("Client player and level must exist after world creation");
                }
            });
            context.takeScreenshot("powers-client-world-smoke");
            verifyCrystalTravel(context, singleplayer);
			quiesceVisuals(context);
            captureHudStates(context);
            captureScreens(context);
            captureRemainingScreens(context);
            captureCompactScreens(context);
            smokeOperatorCommands(singleplayer);
        }
    }

	private static void quiesceVisuals(ClientGameTestContext context) {
		for (int tick = 0; tick < 35; tick++) {
			context.runOnClient(client -> {
				ClientMagicFx.reset();
				client.particleEngine.clearParticles();
			});
			context.waitTick();
		}
	}

    private static void smokeOperatorCommands(TestSingleplayerContext singleplayer) {
        for (String command : List.of(
                "powers list",
                "powers slots Player0",
                "powers assign Player0 flight 0",
                "execute as Player0 run powers consent teleport allow",
                "execute as Player0 run powers path list",
                "execute as Player0 run powers darkprefix false",
                "powers diagnose",
				"execute as Player0 at Player0 run powers ruin preview",
                "powers shadow learning reset Player0",
				"execute as Player0 at Player0 run powers testing on",
				"execute as Player0 at Player0 run powers testing status",
				"execute as Player0 at Player0 run powers testing energy off",
				"execute as Player0 at Player0 run powers testing cooldowns off",
				"execute as Player0 at Player0 run powers testing refill",
				"execute as Player0 at Player0 run powers testing coverage",
				"execute as Player0 at Player0 run powers testing quest-telemetry",
				"execute as Player0 at Player0 run powers testing profile status",
				"execute as Player0 at Player0 run powers testing arena spawn",
				"execute as Player0 at Player0 run powers testing actor spawn AcceptanceActor",
				"execute as Player0 at Player0 run powers testing arena clear",
				"execute as Player0 at Player0 run powers testing reset",
				"execute as Player0 at Player0 run powers testing off",
                "powers reload")) {
			int result = singleplayer.getServer().computeOnServer(server -> {
				try {
					return server.getCommands().getDispatcher().execute(
							command, server.createCommandSourceStack());
				} catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
					throw new AssertionError("Operator command did not parse: /" + command, exception);
				}
			});
			if (result <= 0) {
				throw new AssertionError("Operator command reported failure: /" + command);
			}
        }
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayerByName("Player0");
			if (player == null || com.powers.player.PlayerPowers.get(player).getPower(0) == null
					|| !com.powers.player.PlayerPowers.get(player).getPower(0).id().getPath().equals("flight")) {
				throw new AssertionError("/powers assign did not persist the requested slot");
			}
			if (!com.powers.testing.TestingOverrides.state(player.getUUID()).equals(
					com.powers.testing.TestingOverrides.State.DEFAULT)) {
				throw new AssertionError("Testing command cleanup left limits disabled");
			}
		});
    }

    private static void verifyCrystalTravel(ClientGameTestContext context,
            TestSingleplayerContext singleplayer) {
        verifyCrystalTravel(context, singleplayer, PowersItems.DARK_CRYSTAL,
                "powers:dark_realm", "powers-dark-realm-live");
        verifyCrystalTravel(context, singleplayer, PowersItems.LIGHT_CRYSTAL,
                "powers:light_realm", "powers-light-realm-live");
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            TestingOverrides.clear(player.getUUID());
        });
    }

    private static void verifyCrystalTravel(ClientGameTestContext context,
            TestSingleplayerContext singleplayer, net.minecraft.world.item.Item crystal,
            String dimension, String screenshot) {
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            TestingOverrides.setEnergyDisabled(player.getUUID(), true);
            TestingOverrides.setCooldownsDisabled(player.getUUID(), true);
            if (!CrystalPowerRegistry.tryActivate(player, crystal)) {
                throw new AssertionError("Crystal activation rejected travel to " + dimension);
            }
        });
        singleplayer.getServer().waitFor(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            return player.level().dimension().identifier().toString().equals(dimension)
                    && BodyProxyManager.hasSession(player, BodyProxyKind.REALM);
        });
		context.waitFor(client -> client.level != null && client.player != null
				&& client.level.dimension().identifier().toString().equals(dimension)
				&& client.gui.screen() == null);
		context.waitTicks(20);
        context.takeScreenshot(screenshot);
        singleplayer.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            if (!BodyProxyManager.recoverToBody(player)) {
                throw new AssertionError("Administrative body recovery rejected from " + dimension);
            }
        });
        singleplayer.getServer().waitFor(server -> server.getPlayerList().getPlayers().getFirst()
                .level().dimension().identifier().toString().equals("minecraft:overworld"));
		context.waitFor(client -> client.level != null
				&& client.level.dimension().identifier().toString().equals("minecraft:overworld")
				&& client.gui.screen() == null);
		context.waitTicks(20);
    }

    private static void captureHudStates(ClientGameTestContext context) {
        setClientState(context, false, 0, 100, List.of());
        context.takeScreenshot("powers-hud-empty-light");
        setClientState(context, false, 50, 100, List.of("powers:flight"));
        context.takeScreenshot("powers-hud-half-active");
        setClientState(context, true, 100, 100, List.of("powers:double_health"));
        context.takeScreenshot("powers-hud-full-darkness");
    }

    private static void captureScreens(ClientGameTestContext context) {
        List<String> rainbowModes = CrystalAbilityCatalog.defaults().get("rainbow_crystal");
        if (rainbowModes.size() != 7) {
            throw new AssertionError("Rainbow selector must expose exactly seven forces");
        }
        capture(context, "powers-rainbow-sevenfold",
                () -> new RainbowConvergenceScreen(rainbowModes, 5), RainbowConvergenceScreen.class);

        setClientState(context, false, 100, 100, List.of());
        capture(context, "powers-rank-maze-light", RankMazeScreen::new, RankMazeScreen.class);
        setClientState(context, true, 100, 100, List.of());
        capture(context, "powers-rank-maze-dark", RankMazeScreen::new, RankMazeScreen.class);

        List<ArtifactActionSnapshot> snapshots = artifactSnapshots();
        List<String> favourites = ArtifactFavouriteRules.defaults(
                ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS), 10,
                "innate/lightning_strike");
        capture(context, "powers-shadow-combat-wheel",
                () -> new ShadowSwordScreen("darkness", "innate/lightning_strike", 10,
                        SizeMorphRules.normalOption(), 100, favourites, snapshots),
                ShadowSwordScreen.class);
        capture(context, "powers-shadow-library",
                () -> ClientAcceptanceScreens.artifactCatalogue("darkness",
                        "innate/lightning_strike", 10, SizeMorphRules.normalOption(), 100,
                        favourites, snapshots), ArtifactCatalogueScreen.class);
    }

    private static void captureRemainingScreens(ClientGameTestContext context) {
        capture(context, "powers-teleport-menu", () -> new TeleportInputScreen(0),
                TeleportInputScreen.class);
        capture(context, "powers-celestial-locator",
                () -> new CelestialLocatorScreen(CelestialSearchMode.ENTITY,
                        UUID.fromString("00000000-0000-0000-0000-000000000001")),
                CelestialLocatorScreen.class);

        var celestial = SpellRegistry.defaults().forTexture("book_grimoire_celestial");
        List<SpellIndexEntry> spells = celestial.spells().stream().map(SpellIndexEntry::from).toList();
        capture(context, "powers-grimoire-index",
                () -> new GrimoireIndexScreen(celestial.key(), 0, spells), GrimoireIndexScreen.class);
        capture(context, "powers-power-option",
                () -> new PowerSelectionScreen(0, PowerRegistry.get("size_shift").ability(),
                        SizeMorphRules.normalOption()), PowerSelectionScreen.class);
        capture(context, "powers-reservoir-transfer",
                () -> new ReservoirTransferScreen(new RelicPackets.OpenReservoirPayload(
                        0, 55, 100, 30, 80, 40, 0)), ReservoirTransferScreen.class);
        capture(context, "powers-arcane-crucible", () -> {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client.player == null) throw new AssertionError("Client player unavailable");
            ArcaneCrucibleMenu menu = new ArcaneCrucibleMenu(91, client.player.getInventory(),
                    BlockPos.ZERO);
            return new ArcaneCrucibleScreen(menu, client.player.getInventory(),
                    Component.translatable("block.powers.arcane_crucible"));
        }, ArcaneCrucibleScreen.class);
    }

    private static void captureCompactScreens(ClientGameTestContext context) {
        context.setScreen(() -> null);
        context.getInput().resizeWindow(854, 480);
        context.waitTick();
        List<String> modes = CrystalAbilityCatalog.defaults().get("rainbow_crystal");
        capture(context, "powers-rainbow-sevenfold-compact",
                () -> new RainbowConvergenceScreen(modes, 0), RainbowConvergenceScreen.class);
        List<ArtifactActionSnapshot> snapshots = artifactSnapshots();
        List<String> favourites = ArtifactFavouriteRules.defaults(
                ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS), 10,
                "innate/lightning_strike");
        capture(context, "powers-shadow-combat-wheel-compact",
                () -> new ShadowSwordScreen("darkness", "innate/lightning_strike", 10,
                        SizeMorphRules.normalOption(), 100, favourites, snapshots),
                ShadowSwordScreen.class);
    }

    private static void capture(ClientGameTestContext context, String name,
            Supplier<Screen> screen, Class<? extends Screen> expected) {
        context.setScreen(screen);
        context.waitForScreen(expected);
        context.waitTick();
        context.takeScreenshot(name);
    }

    private static void setClientState(ClientGameTestContext context, boolean darkness,
            int energy, int capacity, List<String> toggles) {
        context.runOnClient(client -> ClientPowerState.update(new PowerStatePayload(
                List.of("powers:flight", "powers:forcefield", "powers:double_health"), toggles,
                List.of(0, 40, 0), List.of(0, 100, 0), List.of(0, 0, 0), energy, capacity,
                darkness, darkness, false, SizeMorphRules.normalOption(),
                List.of("legacy_0", "legacy_1", "legacy_2", "legacy_3", "legacy_4",
                        "legacy_5", "legacy_6", "legacy_7", "legacy_8", "legacy_9", "legacy_10"),
                "legacy_10", 10)));
        context.waitTick();
    }

    private static List<ArtifactActionSnapshot> artifactSnapshots() {
        return ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS).stream()
                .map(action -> new ArtifactActionSnapshot(action.key(), action.category(),
                        action.energyCost(), 0, 0, action.baseCooldownTicks(), false, false,
                        action.abilityId().equals("size_shift") ? SizeMorphRules.normalOption() : -1))
                .toList();
    }
}
