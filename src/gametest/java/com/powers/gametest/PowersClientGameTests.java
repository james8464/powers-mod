package com.powers.gametest;

import com.powers.PowersItems;
import com.powers.client.ClientPowerState;
import com.powers.client.ClientSemanticFxMetrics;
import com.powers.client.fx.ClientMagicFx;
import com.powers.client.fx.ClientEventAudio;
import com.powers.client.fx.ClientCelestialRuinFx;
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
import com.powers.network.MagicFxPackets;
import com.powers.fx.BeamFxStyle;
import com.powers.fx.PowerFx;
import com.powers.network.EventAudioPackets;
import com.powers.network.CelestialRuinPackets;
import com.powers.fx.FxLodTier;
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
			verifySemanticFxBatching(context, singleplayer);
			verifyDistantSemanticRendering(context, singleplayer);
			verifyDistantEventAudio(context, singleplayer);
			verifyOverlappingRuinRinging(context, singleplayer);
            verifyCrystalTravel(context, singleplayer);
			quiesceVisuals(context);
            captureHudStates(context);
            captureScreens(context);
            captureRemainingScreens(context);
            captureCompactScreens(context);
            smokeOperatorCommands(singleplayer);
        }
    }

	private static void verifySemanticFxBatching(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		long firstEventId = 0xF015_0000L;
		context.runOnClient(client -> ClientSemanticFxMetrics.reset());
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			MagicFxPackets.resetTransportMetrics(server);
			for (int index = 0; index < 10; index++) {
				MagicFxPackets.sendBeam(player, new MagicFxPackets.BeamFxPayload(
						firstEventId + index, BeamFxStyle.values()[index],
						player.getX(), player.getEyeY(), player.getZ(),
						player.getX() + 8.0, player.getEyeY() + index * 0.1, player.getZ(),
						16, 0xB8F4FF));
			}
		});
		singleplayer.getServer().waitFor(server -> {
			var snapshot = MagicFxPackets.transportSnapshot(server);
			return snapshot.immediatePackets() >= 1 && snapshot.batchPackets() >= 1
					&& snapshot.batchedEntries() >= 9;
		});
		context.waitFor(client -> {
			var snapshot = ClientSemanticFxMetrics.snapshot();
			return snapshot.batchPackets() >= 1 && snapshot.batchedEntries() >= 9;
		});
		context.runOnClient(client -> {
			List<Long> controlled = ClientSemanticFxMetrics.snapshot().recentEventIds().stream()
					.filter(id -> id >= firstEventId && id < firstEventId + 10)
					.toList();
			List<Long> expected = java.util.stream.LongStream.range(firstEventId, firstEventId + 10)
					.boxed().toList();
			if (!controlled.equals(expected)) {
				throw new AssertionError("Semantic FX delivery order changed: " + controlled);
			}
		});
	}

	private static void verifyOverlappingRuinRinging(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		context.runOnClient(client -> ClientCelestialRuinFx.reset());
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
					new CelestialRuinPackets.Payload(CelestialRuinPackets.Phase.DETONATE,
							player.getX() + 16.0, player.getY(), player.getZ(), 0, FxLodTier.NEAR));
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
					new CelestialRuinPackets.Payload(CelestialRuinPackets.Phase.DETONATE,
							player.getX() + 1_800.0, player.getY(), player.getZ(), 0, FxLodTier.FAR));
		});
		context.waitFor(client -> ClientCelestialRuinFx.activeRingingCount() == 2);
	}

	private static void verifyDistantSemanticRendering(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		context.runOnClient(client -> {
			client.particleEngine.clearParticles();
			ClientSemanticFxMetrics.reset();
		});
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			PowerFx.rune(player.level(), player.position().add(144.0, 0.0, 0.0),
					8.0, 0xB36BFF, 48, 0.25);
		});
		context.waitFor(client -> !ClientSemanticFxMetrics.snapshot().recentEventIds().isEmpty()
				&& particleCount(client.particleEngine.countParticles()) > 0);

		context.runOnClient(client -> {
			client.particleEngine.clearParticles();
			ClientSemanticFxMetrics.reset();
		});
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			PowerFx.eventRune(player.level(), player.position().add(1_800.0, 0.0, 0.0),
					12.0, 0xFFF2A8, 64, 0.5);
		});
		context.waitFor(client -> !ClientSemanticFxMetrics.snapshot().recentEventIds().isEmpty()
				&& particleCount(client.particleEngine.countParticles()) > 0);
	}

	private static int particleCount(String diagnostic) {
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)(?!.*\\d)")
				.matcher(diagnostic == null ? "" : diagnostic);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
	}

	private static void verifyDistantEventAudio(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		context.runOnClient(client -> ClientEventAudio.resetMetrics());
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			PowerFx.eventSound(player.level(), player.position().add(1_800.0, 0.0, 0.0),
					EventAudioPackets.Cue.LIGHT_HERALD, 3.0F, 0.65F);
		});
		context.waitFor(client -> ClientEventAudio.handledCount() == 1);
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
		String assignedPower = singleplayer.getServer().computeOnServer(server -> {
			var player = server.getPlayerList().getPlayerByName("Player0");
			if (player == null) throw new AssertionError("Client test player is unavailable");
			var assigned = com.powers.player.PlayerPowers.get(player).getSlotIds();
			var allegiance = com.powers.player.SkillSystem.hasDarknessTag(player)
					? com.powers.power.PowerAffinity.DARKNESS
					: com.powers.power.PowerAffinity.RADIANT;
			return PowerRegistry.getAssignable(allegiance).stream()
					.map(power -> power.id().getPath())
					.filter(id -> assigned.stream().noneMatch(saved -> {
						var power = PowerRegistry.get(saved);
						return power != null && power.id().getPath().equals(id);
					}))
					.findFirst()
					.orElseThrow(() -> new AssertionError("No unassigned compatible power exists"));
		});
        for (String command : List.of(
                "powers list",
                "powers slots Player0",
				"powers assign Player0 " + assignedPower + " 0",
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
					|| !com.powers.player.PlayerPowers.get(player).getPower(0).id().getPath()
							.equals(assignedPower)) {
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
