package com.powers.gametest;

import com.powers.PowersItems;
import com.powers.PowersSounds;
import com.powers.PowersWeapons;
import com.powers.client.ClientPowerState;
import com.powers.client.ClientSemanticFxMetrics;
import com.powers.client.fx.ClientMagicFx;
import com.powers.client.audio.ClientLayeredAudioMixer;
import com.powers.client.fx.ClientCelestialRuinFx;
import com.powers.client.fx.ClientVisualScarManager;
import com.powers.client.fx.ClientVisualScarRenderer;
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
import com.powers.magic.runtime.MagicRuntime;
import com.powers.network.PowerStatePayload;
import com.powers.network.PowersPlayNetworking;
import com.powers.network.ShadowSwordPackets;
import com.powers.network.RelicPackets;
import com.powers.network.MagicFxPackets;
import com.powers.fx.BeamFxStyle;
import com.powers.fx.PowerFx;
import com.powers.network.CelestialRuinPackets;
import com.powers.fx.FxLodTier;
import com.powers.fx.ClientVisualScarState;
import com.powers.fx.ScarFxProtocolRules;
import com.powers.fx.VisualScarMotifGeometry;
import com.powers.fx.VisualScarRules;
import com.powers.fx.VisualScarService;
import com.powers.mind.BodyProxyKind;
import com.powers.player.ArtifactSelectionState;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.mind.BodyProxyManager;
import com.powers.power.PowerRegistry;
import com.powers.power.abilities.SizeMorphRules;
import com.powers.power.crystals.CrystalAbilityCatalog;
import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.spell.CelestialSearchMode;
import com.powers.spell.SpellIndexEntry;
import com.powers.spell.SpellRegistry;
import com.powers.magic.fx.MagicFxKind;
import com.powers.testing.network.PacketFaultController;
import com.powers.testing.network.PacketFaultDirection;
import com.powers.testing.network.PacketFaultFamily;
import com.powers.testing.network.PacketFaultMetrics;
import com.powers.testing.network.PacketFaultProfile;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
		if (Boolean.getBoolean("powers.vfx006.clientOnly")) {
			TestWorldSave worldSave;
			CastingPoseClientAcceptance.ReconnectSeed reconnect;
			try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
				reconnect = CastingPoseClientAcceptance.run(context, singleplayer);
				worldSave = singleplayer.getWorldSave();
			}
			context.waitFor(client -> client.level == null && client.player == null);
			try (TestSingleplayerContext reopened = worldSave.open()) {
				CastingPoseClientAcceptance.captureReconnect(context, reopened, reconnect);
			}
			return;
		}

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            context.runOnClient(client -> {
                if (client.player == null || client.level == null) {
                    throw new AssertionError("Client player and level must exist after world creation");
                }
            });
			context.takeScreenshot("powers-client-world-smoke");
			verifyPacketFaultClientConvergence(context, singleplayer);
			verifySemanticFxBatching(context, singleplayer);
			verifyDistantSemanticRendering(context, singleplayer);
			verifyDistantEventAudio(context, singleplayer);
			verifyOverlappingRuinRinging(context, singleplayer);
			quiesceVisuals(context);
			visualScarPresentationMatrix(context, singleplayer);
			visualScarResourceReloadContinuity(context);
			visualScarOccludedWall(context, singleplayer);
            verifyCrystalTravel(context, singleplayer);
			quiesceVisuals(context);
            captureHudStates(context);
            captureScreens(context, singleplayer);
            captureRemainingScreens(context);
            captureCompactScreens(context);
            smokeOperatorCommands(singleplayer);
        }
    }

	private static void verifyPacketFaultClientConvergence(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		verifyNamedPacketFaultProfilesOnRealClient(context, singleplayer);
		context.runOnClient(client -> {
			ClientPowerState.reset();
			ClientSemanticFxMetrics.reset();
		});
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			PacketFaultProfile stateProfile = new PacketFaultProfile("client-state", 0xC11E17L,
					EnumSet.of(PacketFaultDirection.CLIENTBOUND),
					EnumSet.of(PacketFaultFamily.MENU_SNAPSHOT, PacketFaultFamily.POWER_STATE),
					3, 0, 0, 2, 64, 40, 16);
			PacketFaultController.configureScoped(server, stateProfile, player);
			PowersPlayNetworking.send(player, new ShadowSwordPackets.OpenMenuPayload(2L,
					"darkness", "innate/fireball", 10, SizeMorphRules.normalOption(), 777,
					List.of("", "", "", "", "", "", "", ""), List.of(), List.of()));
			for (int energy : List.of(555, 666, 777)) {
				PowersPlayNetworking.send(player, new PowerStatePayload(List.of(), List.of(), List.of(),
						List.of(), List.of(), energy, 1_000, false, true, false,
						SizeMorphRules.normalOption(), List.of(), "", 0));
			}
		});
		context.waitFor(client -> client.gui.screen() instanceof ShadowSwordScreen
				&& ClientPowerState.energy() == 777);
		context.runOnClient(client -> client.gui.setScreen(null));
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			var stateMetrics = PacketFaultController.diagnostics(server, player).metrics();
			if (stateMetrics.delayed() < 4L || stateMetrics.reordered() < 1L) {
				throw new AssertionError("Client menu/HUD fault matrix was not exercised: " + stateMetrics);
			}
			PacketFaultProfile fxProfile = new PacketFaultProfile("client-fx", 0xC11E18L,
					EnumSet.of(PacketFaultDirection.CLIENTBOUND), EnumSet.of(PacketFaultFamily.MAGIC_FX),
					3, 500, 0, 2, 128, 40, 32);
			PacketFaultController.configureScoped(server, fxProfile, player);
			for (int index = 0; index < 64; index++) {
				PowersPlayNetworking.send(player, new MagicFxPackets.MagicFxPayload(MagicFxKind.CAST,
						0x9A009000L + index, "qa009", "", player.getX(), player.getY(), player.getZ(),
						0xB36BFF, 0x101018, index, 1, 1));
			}
		});
		context.waitFor(client -> ClientSemanticFxMetrics.snapshot().individualPackets() > 0L);
		singleplayer.getServer().waitFor(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			return PacketFaultController.diagnostics(server, player).queueDepth() == 0;
		});
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			var metrics = PacketFaultController.diagnostics(server, player).metrics();
			if (metrics.dropped() < 1L || metrics.delivered() < 1L || metrics.delayed() < 1L) {
				throw new AssertionError("Real-client FX loss/delay matrix was not exercised: " + metrics);
			}
			PacketFaultController.clearScoped(server, player);
		});
	}

	private static void visualScarPresentationMatrix(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		List<String> captureIds = new ArrayList<>(30);
		List<Block> materials = List.of(Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS,
				Blocks.IRON_BLOCK, Blocks.SAND, Blocks.PACKED_ICE);
		AtomicReference<BlockPos> supportPosition = new AtomicReference<>();
		context.runOnClient(client -> client.player.setXRot(35.0F));
		context.waitFor(client -> Math.abs(client.gameRenderer.mainCamera().xRot() - 35.0F) < 1.0F);
		// Let camera and chunk render state settle before the first evidence frame.
		context.waitTicks(60);
		long generation = 1;
		for (VisualScarRules.Impact impact : VisualScarRules.Impact.values()) {
			for (int material = 0; material < materials.size(); material++) {
				long currentGeneration = generation++;
				int currentMaterial = material;
				String id = "vfx004-scar-matrix-" + impact.name().toLowerCase()
						+ "-" + VisualScarRules.Material.values()[material].name().toLowerCase();
				captureIds.add(id);
				singleplayer.getServer().runOnServer(server -> {
					var player = server.getPlayerList().getPlayers().getFirst();
					BlockPos support = player.blockPosition().south(4).below();
					supportPosition.set(support);
					player.level().setBlockAndUpdate(support, materials.get(currentMaterial).defaultBlockState());
					PowersPlayNetworking.send(player, new MagicFxPackets.ScarFxPayload(
							ScarFxProtocolRules.resetDimension(currentGeneration)));
					if (!VisualScarService.request(player.level(), player, support,
							Direction.UP, impact, id.hashCode())) {
						throw new AssertionError("Production scar service rejected " + id);
					}
				});
				context.waitFor(client -> {
					BlockPos expectedSupport = supportPosition.get();
					if (expectedSupport == null || client.level == null
							|| !client.level.getBlockState(expectedSupport).is(materials.get(currentMaterial))) {
						return false;
					}
					List<ClientVisualScarState.Entry> entries = ClientVisualScarManager.entries();
					return entries.size() == 1
							&& entries.getFirst().position() == expectedSupport.asLong()
							&& entries.getFirst().impact() == impact.ordinal()
							&& entries.getFirst().material() == currentMaterial
							&& entries.getFirst().generation() == currentGeneration;
				});
				context.waitTicks(5);
				context.runOnClient(client -> {
					ClientVisualScarState.Entry entry = new ClientVisualScarState.Entry(
							ScarFxProtocolRules.CREATE_OR_UPDATE, 0, Direction.UP.ordinal(), impact.ordinal(),
							currentMaterial, id.hashCode(), currentGeneration, 1_200, 1_200);
					var mesh = ClientVisualScarRenderer.renderActualMotifMesh(entry, Vec3.ZERO);
					assertMotifTopologyVisible(mesh);
					assertNoKeyOrSwatchSubstitute(mesh);
				});
				context.takeScreenshot(id);
			}
		}
		assertEquals(30, captureIds.size());
	}

	private static void visualScarResourceReloadContinuity(ClientGameTestContext context) {
		List<ClientVisualScarState.Entry> before = context.computeOnClient(
				client -> ClientVisualScarManager.entries());
		if (before.size() != 1) throw new AssertionError("Expected one scar before resource reload");
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		context.runOnClient(client -> reload.set(client.reloadResourcePacks()));
		context.waitFor(client -> reload.get() != null && reload.get().isDone());
		context.computeOnClient(client -> {
			reload.get().join();
			return true;
		});
		context.waitTicks(60);
		context.runOnClient(client -> {
			if (!ClientVisualScarManager.entries().equals(before)) {
				throw new AssertionError("Semantic scar changed across resource reload");
			}
		});
		context.takeScreenshot("vfx004-scar-post-resource-reload");
	}

	private static void visualScarOccludedWall(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		context.takeScreenshot("vfx004-scar-visible-front");
		AtomicReference<BlockPos> wallPosition = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			BlockPos wall = player.blockPosition().south(2);
			wallPosition.set(wall);
			for (int y = 0; y < 3; y++) player.level().setBlockAndUpdate(wall.above(y), Blocks.STONE.defaultBlockState());
		});
		context.waitFor(client -> {
			BlockPos expectedWall = wallPosition.get();
			if (expectedWall == null || client.level == null) return false;
			for (int y = 0; y < 3; y++) {
				if (!client.level.getBlockState(expectedWall.above(y)).is(Blocks.STONE)) return false;
			}
			return true;
		});
		context.waitTicks(5);
		context.takeScreenshot("vfx004-scar-occluded-wall");
		assertOcclusionPipelineConfigured();
	}

	private static void assertMotifTopologyVisible(VisualScarMotifGeometry.Mesh mesh) {
		if (mesh.quads().isEmpty() || mesh.recognitionAnchors() < 1
				|| mesh.topologySignature().isBlank()) {
			throw new AssertionError("Actual motif topology was not visible");
		}
	}

	private static void assertNoKeyOrSwatchSubstitute(VisualScarMotifGeometry.Mesh mesh) {
		if (mesh.vertices().size() < 12 || mesh.recognisableSilhouette() != mesh.motif()) {
			throw new AssertionError("Scar fixture used a key or colour swatch substitute");
		}
	}

	private static void assertOcclusionPipelineConfigured() {
		var pipeline = VisualScarMotifGeometry.pipelineContract();
		if (!pipeline.reverseDepthTest() || pipeline.depthWrite()) {
			throw new AssertionError("Opaque-wall occlusion requires reverse depth test without depth writes");
		}
	}

	private static void assertEquals(int expected, int actual) {
		if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
	}

	private static void verifyNamedPacketFaultProfilesOnRealClient(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		List<String> profiles = List.of("delay150", "delay300", "loss1", "loss5", "duplicate", "reorder");
		for (int profileIndex = 0; profileIndex < profiles.size(); profileIndex++) {
			String profile = profiles.get(profileIndex);
			int finalEnergy = 811 + profileIndex;
			AtomicLong startedAt = new AtomicLong();
			AtomicReference<PacketFaultMetrics> metrics = new AtomicReference<>();
			AtomicBoolean retried = new AtomicBoolean();
			context.runOnClient(client -> ClientPowerState.reset());
			singleplayer.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				PlayerPowers.get(player).forceRestoreEnergy();
				PacketFaultController.configureScoped(server,
						PacketFaultProfile.named(profile, 630_793L), player);
				startedAt.set(server.getTickCount());
				int samples = profile.startsWith("loss") ? 250 : 100;
				for (int sample = 0; sample < samples; sample++) {
					int energy = sample == samples - 1 ? finalEnergy : sample;
					PowersPlayNetworking.send(player, clientPowerState(energy));
				}
			});
			context.waitTicks(10);
			context.runOnClient(client -> retried.set(ClientPowerState.energy() != finalEnergy));
			singleplayer.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				metrics.set(PacketFaultController.diagnostics(server, player).metrics());
				PacketFaultController.clearScoped(server, player);
				if (retried.get()) PowersPlayNetworking.send(player, clientPowerState(finalEnergy));
			});
			context.waitFor(client -> ClientPowerState.energy() == finalEnergy);
			AtomicLong observedBy = new AtomicLong();
			singleplayer.getServer().runOnServer(server -> observedBy.set(
					Math.max(0L, server.getTickCount() - startedAt.get())));
			context.runOnClient(client -> {
				if (ClientPowerState.energy() != finalEnergy) {
					throw new AssertionError(profile + " did not reach the actual client HUD mirror");
				}
			});
			PacketFaultMetrics snapshot = metrics.get();
			if (snapshot == null || snapshot.duplicateSideEffects() != 0L) {
				throw new AssertionError(profile + " did not retain safe transport accounting: " + snapshot);
			}
			if (profile.startsWith("loss") && snapshot.dropped() == 0L) {
				throw new AssertionError(profile + " did not inject loss on the real client path");
			}
			if ("duplicate".equals(profile) && snapshot.duplicated() == 0L) {
				throw new AssertionError("Duplicate profile did not duplicate real client envelopes");
			}
			if ((profile.startsWith("delay") || "reorder".equals(profile))
					&& snapshot.delayed() == 0L) {
				throw new AssertionError(profile + " did not delay real client envelopes");
			}
			if (!profile.startsWith("loss") && retried.get()) {
				throw new AssertionError(profile + " required an unexpected fault-disabled retry");
			}
			System.out.println("QA009_CLIENT_MATRIX profile=" + profile
					+ " observedByTicks=" + observedBy.get() + " retried=" + retried.get()
					+ " finalHudEnergy=" + finalEnergy + " offered=" + snapshot.offered()
					+ " dropped=" + snapshot.dropped() + " duplicated=" + snapshot.duplicated()
					+ " delayed=" + snapshot.delayed() + " reordered=" + snapshot.reordered()
					+ " delivered=" + snapshot.delivered() + " expired=" + snapshot.expired()
					+ " maxQueue=" + snapshot.maximumQueueDepth()
					+ " maxAgeTicks=" + snapshot.maximumAgeTicks()
					+ " duplicateSideEffects=" + snapshot.duplicateSideEffects());
		}
	}

	private static PowerStatePayload clientPowerState(int energy) {
		return new PowerStatePayload(List.of(), List.of(), List.of(), List.of(), List.of(),
				energy, 1_000, false, true, false, SizeMorphRules.normalOption(), List.of(), "", 0);
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
		context.runOnClient(client -> ClientLayeredAudioMixer.resetConnectionEpoch());
		singleplayer.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			PowerFx.sound(player.level(), player.position().add(96.0, 0.0, 0.0),
					PowersSounds.LIGHT_CHORUS, 3.0F, 0.65F);
		});
		context.waitFor(client -> ClientLayeredAudioMixer.metrics().acceptedEvents() == 1);
	}

	private static void quiesceVisuals(ClientGameTestContext context) {
		for (int tick = 0; tick < 35; tick++) {
			context.runOnClient(client -> {
				ClientMagicFx.reset();
				ClientCelestialRuinFx.reset();
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

    private static void captureScreens(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        List<String> rainbowModes = CrystalAbilityCatalog.defaults().get("rainbow_crystal");
        if (rainbowModes.size() != 7) {
            throw new AssertionError("Rainbow selector must expose exactly seven forces");
        }
        capture(context, "powers-rainbow-sevenfold",
				() -> new RainbowConvergenceScreen(0L, rainbowModes, 5), RainbowConvergenceScreen.class);

        setClientState(context, false, 100, 100, List.of());
        capture(context, "powers-rank-maze-light", RankMazeScreen::new, RankMazeScreen.class);
        setClientState(context, true, 100, 100, List.of());
        capture(context, "powers-rank-maze-dark", RankMazeScreen::new, RankMazeScreen.class);

        List<ArtifactActionSnapshot> snapshots = artifactSnapshots();
        List<String> favourites = ArtifactFavouriteRules.defaults(
                ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS), 10,
                "innate/lightning_strike");
		long revision = singleplayer.getServer().computeOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					PowersWeapons.weapon("lycanbane").getDefaultInstance());
			player.addTag(SkillSystem.DARKNESS_TAG);
			PlayerPowers.get(player).setDarknessLevel(player, 10);
			return MagicRuntime.catalogue().snapshot().revision();
		});
        capture(context, "powers-shadow-combat-wheel",
				() -> new ShadowSwordScreen(revision, "darkness", "innate/lightning_strike", 10,
                        SizeMorphRules.normalOption(), 100, favourites, snapshots),
                ShadowSwordScreen.class);
        capture(context, "powers-shadow-library",
                () -> ClientAcceptanceScreens.artifactCatalogue(revision, "darkness",
                        "innate/lightning_strike", 10, SizeMorphRules.normalOption(), 100,
                        favourites, snapshots), ArtifactCatalogueScreen.class);
		verifyVirtualCatalogue(context, singleplayer);
    }

	private static void verifyVirtualCatalogue(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		context.runOnClient(client -> ClientAcceptanceScreens.searchInnate(
				(ArtifactCatalogueScreen) client.gui.screen(), ""));
		ClientAcceptanceScreens.CatalogueProbe before = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (before.widgets() <= 0 || before.widgets() != before.allocations()) {
			throw new AssertionError("Catalogue did not allocate exactly one fixed visible widget pool: " + before);
		}
		context.getInput().scroll(-6.0);
		context.waitTick();
		ClientAcceptanceScreens.CatalogueProbe afterScroll = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (afterScroll.widgets() != before.widgets() || afterScroll.allocations() != before.allocations()) {
			throw new AssertionError("Mouse-wheel scrolling reconstructed catalogue widgets: " + afterScroll);
		}
		if (afterScroll.firstVisibleIndex() <= before.firstVisibleIndex()) {
			throw new AssertionError("Real mouse-wheel input did not advance the virtual window: " + afterScroll);
		}
		singleplayer.getServer().waitFor(server -> {
			List<String> serverFavourites = ArtifactSelectionState.favourites(
					server.getPlayerList().getPlayers().getFirst(), ArtifactAlignment.DARKNESS);
			if (serverFavourites.contains("innate/invisibility")) {
				throw new AssertionError("Acceptance fixture requires Invisibility to begin unbound");
			}
			return true;
		});
		context.runOnClient(client -> ClientAcceptanceScreens.searchDefault(
				(ArtifactCatalogueScreen) client.gui.screen(), "invisibility"));
		context.waitTick();
		ClientAcceptanceScreens.CatalogueProbe searched = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (searched.results() != 1 || searched.firstActionLabel().isBlank()
				|| !searched.noCategoryTabSelected()) {
			throw new AssertionError("Default-surface global search did not isolate unbound Invisibility: " + searched);
		}
		context.runOnClient(client -> ClientAcceptanceScreens.moveDown(
				(ArtifactCatalogueScreen) client.gui.screen()));
		ClientAcceptanceScreens.CatalogueProbe focused = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (!focused.selectedKey().equals(focused.focusedActionKey())
				|| focused.focusedNarrationText().isBlank() || focused.hiddenActionHasFocus()) {
			throw new AssertionError("Keyboard focus/narration did not follow the visible selected action: " + focused);
		}
		context.runOnClient(client -> ClientAcceptanceScreens.searchDefault(
				(ArtifactCatalogueScreen) client.gui.screen(), "no such action"));
		ClientAcceptanceScreens.CatalogueProbe filteredAway = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (filteredAway.hiddenActionHasFocus() || !filteredAway.focusedActionKey().isEmpty()) {
			throw new AssertionError("Filtered pooled button retained stale keyboard focus: " + filteredAway);
		}
		context.runOnClient(client -> ClientAcceptanceScreens.searchDefault(
				(ArtifactCatalogueScreen) client.gui.screen(), "invisibility"));
		context.clickScreenButton(searched.firstActionLabel());
		context.clickScreenButton("1");
		ClientAcceptanceScreens.CatalogueProbe pending = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (!pending.lastBindNonOptimistic()) {
			throw new AssertionError("Client bind event optimistically changed a favourite before acknowledgement");
		}
		context.waitTick();
		singleplayer.getServer().waitFor(server -> ArtifactSelectionState.favourites(
				server.getPlayerList().getPlayers().getFirst(), ArtifactAlignment.DARKNESS)
				.getFirst().equals("innate/invisibility"));
		ClientAcceptanceScreens.CatalogueProbe bound = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (!"innate/invisibility".equals(bound.selectedKey())
				|| !"innate/invisibility".equals(bound.firstFavouriteKey())
				|| bound.widgets() != before.widgets()
				|| bound.allocations() != before.allocations()) {
			throw new AssertionError("Search-result selection/direct binding rebuilt widgets or lost key: " + bound);
		}
		captureCatalogueAtGuiScales(context);
		context.runOnClient(client -> ClientAcceptanceScreens.clearSearch(
				(ArtifactCatalogueScreen) client.gui.screen()));
		context.waitTick();
		ClientAcceptanceScreens.CatalogueProbe cleared = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (!"FAVOURITES".equals(cleared.selectedCategoryTab())) {
			throw new AssertionError("Clearing global search did not restore the chosen category tab: " + cleared);
		}
		verifyTenThousandActionProductionScreen(context);
	}

	private static void captureCatalogueAtGuiScales(ClientGameTestContext context) {
		int previousScale = context.computeOnClient(client -> client.options.guiScale().get());
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeGui();
		});
		context.waitTick();
		context.takeScreenshot("powers-shadow-library-production-1280x720-gui2");
		context.runOnClient(client -> {
			client.options.guiScale().set(3);
			client.resizeGui();
		});
		context.waitTick();
		context.takeScreenshot("powers-shadow-library-production-1280x720-gui3");
		context.runOnClient(client -> {
			client.options.guiScale().set(previousScale);
			client.resizeGui();
		});
		context.waitTick();
	}

	private static void verifyTenThousandActionProductionScreen(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.setScreen(ClientAcceptanceScreens.syntheticCatalogue(10_000)));
		context.waitTick();
		context.runOnClient(client -> ClientAcceptanceScreens.searchInnate(
				(ArtifactCatalogueScreen) client.gui.screen(), ""));
		ClientAcceptanceScreens.CatalogueProbe before = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		context.getInput().scroll(-100.0);
		context.waitTick();
		ClientAcceptanceScreens.CatalogueProbe scrolled = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (scrolled.firstVisibleIndex() <= before.firstVisibleIndex()
				|| scrolled.widgets() != before.widgets() || scrolled.allocations() != before.allocations()) {
			throw new AssertionError("10,000-action production wheel did not scroll without allocation: "
					+ before + " -> " + scrolled);
		}
		context.runOnClient(client -> ClientAcceptanceScreens.searchInnate(
				(ArtifactCatalogueScreen) client.gui.screen(), "synthetic 9999"));
		context.runOnClient(client -> ClientAcceptanceScreens.refreshSynthetic(
				(ArtifactCatalogueScreen) client.gui.screen(), 101L));
		context.waitTick();
		ClientAcceptanceScreens.CatalogueProbe after = context.computeOnClient(client ->
				ClientAcceptanceScreens.catalogueProbe((ArtifactCatalogueScreen) client.gui.screen()));
		if (after.results() != 1 || before.widgets() != before.allocations()
				|| after.widgets() != before.widgets() || after.allocations() != before.allocations()) {
			throw new AssertionError("10,000-action production screen rebuilt its fixed widget pool: "
					+ before + " -> " + after);
		}
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
				() -> new GrimoireIndexScreen(0L, celestial.key(), 0, spells), GrimoireIndexScreen.class);
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
				() -> new RainbowConvergenceScreen(0L, modes, 0), RainbowConvergenceScreen.class);
        List<ArtifactActionSnapshot> snapshots = artifactSnapshots();
        List<String> favourites = ArtifactFavouriteRules.defaults(
                ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS), 10,
                "innate/lightning_strike");
        capture(context, "powers-shadow-combat-wheel-compact",
				() -> new ShadowSwordScreen(0L, "darkness", "innate/lightning_strike", 10,
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
