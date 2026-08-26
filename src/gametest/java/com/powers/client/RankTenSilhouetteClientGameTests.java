package com.powers.client;

import com.google.gson.Gson;
import com.powers.PowersEntities;
import com.powers.client.fx.ClientRankTenSilhouetteManager;
import com.powers.fx.RankTenSilhouetteProfile;
import com.powers.fx.RankTenSilhouetteService;
import com.powers.gametest.VfxGalleryFixture;
import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.testing.TestingOverrides;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Integrated 1280x720 acceptance gallery for every production rank-ten silhouette. */
public final class RankTenSilhouetteClientGameTests implements FabricClientGameTest {
	private static final Gson GSON = new Gson();
	private static final double FAR_DISTANCE = 96.0;
	private static final float CAMERA_PITCH = -5.0F;
	private static final int EXPIRY_WAIT_TICKS = 44;
	private static final String NEAR_BODY_TAG = "powers_vfx005_near_body";
	private static final List<String> POWER_IDS = List.of(
			"size_shift", "time_shift", "flight", "starfall", "void_beam", "fireball",
			"lightning_strike", "thunderclap", "speed_burst", "telekinesis", "energy_beam",
			"super_speed", "breezy_bash", "invisibility", "time_freeze", "forcefield",
			"gravity_displacement", "vessel_possession", "astral_projection", "energy_drain",
			"ice_manipulation", "plant_healing_acceleration", "double_health");
	private static final List<String> ALIGNMENT_VARIANTS = List.of("flight", "forcefield");

	static {
		if (POWER_IDS.size() != 23 || !Set.copyOf(POWER_IDS).equals(RankTenSilhouetteProfile.powerIds())) {
			throw new IllegalStateException("VFX-005 gallery order drifted from the exact catalogue");
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		context.restoreDefaultGameOptions();
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(RankTenSilhouetteClientGameTests::beginManifest);
		AtomicInteger reloadRevision = new AtomicInteger();
		TestWorldSave worldSave;
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			prepare(context, singleplayer);
			captureEmpty(context, "baseline", "size_shift", FAR_DISTANCE,
					false, "all", reloadRevision.get());
			for (String powerId : POWER_IDS) {
				captureEvent(context, singleplayer, "far_normal", powerId, FAR_DISTANCE,
						false, false, "all", reloadRevision.get(), false);
			}
			for (String powerId : POWER_IDS) {
				captureEvent(context, singleplayer, "far_reduced", powerId, FAR_DISTANCE,
						true, false, "minimal", reloadRevision.get(), false);
			}
			for (String powerId : ALIGNMENT_VARIANTS) {
				captureEvent(context, singleplayer, "alignment_variant", powerId, FAR_DISTANCE,
						false, true, "all", reloadRevision.get(), false);
			}
			setNearCasterBody(singleplayer, true);
			context.waitTicks(8);
			captureEvent(context, singleplayer, "near", "flight", 8.0,
					false, false, "all", reloadRevision.get(), false);
			setNearCasterBody(singleplayer, false);
			context.waitTicks(8);
			setWall(singleplayer, true);
			context.waitTicks(8);
			captureEmpty(context, "wall_baseline", "forcefield", FAR_DISTANCE,
					false, "all", reloadRevision.get());
			captureEvent(context, singleplayer, "wall", "forcefield", FAR_DISTANCE,
					false, false, "all", reloadRevision.get(), false);
			setWall(singleplayer, false);
			context.waitTicks(8);
			captureEvent(context, singleplayer, "minimal_particles", "starfall", FAR_DISTANCE,
					false, false, "minimal", reloadRevision.get(), false);
			captureEvent(context, singleplayer, "post_reload", "void_beam", FAR_DISTANCE,
					false, false, "all", reloadRevision.incrementAndGet(), true);
			verifyDimensionBoundary(context, singleplayer);
			captureEvent(context, singleplayer, "post_dimension", "time_freeze", FAR_DISTANCE,
					false, false, "all", reloadRevision.get(), false);
			worldSave = singleplayer.getWorldSave();
		} finally {
			context.runOnClient(client -> {
				if (client.player != null) {
					client.options.screenEffectScale().set(1.0);
					client.options.chatVisibility().set(ChatVisiblity.FULL);
				}
			});
		}
		context.waitFor(client -> client.level == null && client.player == null);
		try (TestSingleplayerContext reconnected = worldSave.open()) {
			prepare(context, reconnected);
			captureEvent(context, reconnected, "post_reconnect", "double_health", FAR_DISTANCE,
					false, false, "all", reloadRevision.get(), false);
		}
	}

	private static void prepare(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		context.waitFor(client -> client.player != null && client.level != null);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			VfxGalleryFixture.stabilize(player);
			RankTenSilhouetteService.clear(server);
			PlayerPowers.get(player).setSkillLevel(player, 10);
			TestingOverrides.setAll(player.getUUID(), true);
			player.removeTag(SkillSystem.DARKNESS_TAG);
			player.teleportTo(0.5, 100.0, 0.5);
			player.setYRot(0.0F);
			player.setXRot(CAMERA_PITCH);
			player.setNoGravity(true);
			player.setInvulnerable(true);
			player.setDeltaMovement(Vec3.ZERO);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.options.fov().set(70);
			client.options.renderDistance().set(12);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.screenEffectScale().set(1.0);
			client.player.setYRot(0.0F);
			client.player.setXRot(CAMERA_PITCH);
		});
		context.waitFor(client -> client.player != null
				&& client.player.position().distanceToSqr(0.5, 100.0, 0.5) < 1.0
				&& ClientRankTenSilhouetteManager.entries().isEmpty());
		// Rank synchronization emits legitimate advancement toasts and first-awakening HUD text.
		// Let those production overlays and the first sky/time synchronization settle before baseline.
		// Client sky/fog interpolation continues after the server's fixed-noon state arrives.
		// Five hundred ticks covers that bounded transition on both initial load and reconnect.
		context.waitTicks(500);
		quiesceUi(context);
	}

	private static void quiesceUi(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.gui.toastManager().clear();
			client.options.chatVisibility().set(ChatVisiblity.HIDDEN);
		});
		context.waitTicks(2);
	}

	private static void captureEvent(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, String category, String powerId, double distance,
			boolean reduced, boolean darkness, String particles, int reloadRevision,
			boolean reloadResources) {
		setClientOptions(context, reduced, particles);
		publishThroughProduction(singleplayer, powerId, distance, darkness);
		int profileId = RankTenSilhouetteProfile.forPower(powerId).orElseThrow().networkId();
		context.waitFor(client -> ClientRankTenSilhouetteManager.entries().size() == 1
				&& ClientRankTenSilhouetteManager.entries().getFirst().wire().profileId() == profileId);
		if (reloadResources) {
			reloadResources(context);
			context.waitFor(client -> ClientRankTenSilhouetteManager.entries().isEmpty());
			publishThroughProduction(singleplayer, powerId, distance, darkness);
			context.waitFor(client -> ClientRankTenSilhouetteManager.entries().size() == 1
					&& ClientRankTenSilhouetteManager.entries().getFirst().wire().profileId() == profileId);
		}
		context.waitTicks(2);
		capture(context, category, powerId, darkness ? "darkness" : "radiant", distance,
				reduced, particles, reloadRevision);
		context.waitTicks(EXPIRY_WAIT_TICKS);
		context.waitFor(client -> ClientRankTenSilhouetteManager.entries().isEmpty());
	}

	private static void publishThroughProduction(TestSingleplayerContext singleplayer,
			String powerId, double distance, boolean darkness) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Vec3 origin = player.position();
			if (darkness) {
				player.addTag(SkillSystem.DARKNESS_TAG);
				PlayerPowers.get(player).setDarknessLevel(player, 10);
			} else {
				player.removeTag(SkillSystem.DARKNESS_TAG);
				PlayerPowers.get(player).setSkillLevel(player, 10);
			}
			player.setPos(origin.x, origin.y, origin.z + distance);
			RankTenSilhouetteService.afterSuccessfulInnateCast(player, powerId);
			player.setPos(origin.x, origin.y, origin.z);
			player.setDeltaMovement(Vec3.ZERO);
			if (darkness) player.removeTag(SkillSystem.DARKNESS_TAG);
		});
	}

	private static void captureEmpty(ClientGameTestContext context, String category,
			String powerId, double distance, boolean reduced, String particles, int reloadRevision) {
		setClientOptions(context, reduced, particles);
		context.waitFor(client -> ClientRankTenSilhouetteManager.entries().isEmpty());
		capture(context, category, powerId, "radiant", distance, reduced, particles, reloadRevision);
	}

	private static void capture(ClientGameTestContext context, String category, String powerId,
			String alignment, double distance, boolean reduced, String particles, int reloadRevision) {
		String captureId = "vfx005-" + category + '-' + powerId;
		Path screenshot = context.takeScreenshot(captureId);
		context.waitTick();
		context.runOnClient(client -> {
			var stamp = ClientRankTenSilhouetteManager.captureHandlerStamp(client);
			long lifecycleEpoch = Math.addExact(Math.multiplyExact(stamp.connectionEpoch(), 1_000_000L),
					stamp.dimensionGeneration());
			ManifestRow row = new ManifestRow(captureId, category, powerId, alignment,
					(int) distance, reduced, particles, reloadRevision, lifecycleEpoch,
					screenshot.getFileName().toString());
			appendManifest(client.gameDirectory.toPath(), row);
		});
	}

	private static void setClientOptions(ClientGameTestContext context,
			boolean reduced, String particles) {
		context.runOnClient(client -> {
			client.options.particles().set("minimal".equals(particles)
					? ParticleStatus.MINIMAL : ParticleStatus.ALL);
			client.options.screenEffectScale().set(reduced ? 0.0 : 1.0);
			client.player.setYRot(0.0F);
			client.player.setXRot(CAMERA_PITCH);
		});
		context.waitTick();
	}

	private static void reloadResources(ClientGameTestContext context) {
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		context.runOnClient(client -> reload.set(client.reloadResourcePacks()));
		context.waitFor(client -> reload.get() != null && reload.get().isDone());
		context.computeOnClient(client -> {
			reload.get().join();
			if (ClientRankTenSilhouetteManager.entries().size() != 1) {
				throw new AssertionError("Resource reload discarded the live semantic silhouette");
			}
			return true;
		});
		// Vanilla completes the reload future before its bounded fade overlay leaves the frame.
		context.waitTicks(60);
	}

	private static void verifyDimensionBoundary(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		publishThroughProduction(singleplayer, "time_freeze", FAR_DISTANCE, false);
		context.waitFor(client -> ClientRankTenSilhouetteManager.entries().size() == 1);
		AtomicReference<Vec3> overworldPosition = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			overworldPosition.set(player.position());
			var nether = server.getLevel(Level.NETHER);
			if (nether == null || !(player.teleport(new TeleportTransition(nether,
					new Vec3(0.5, 100.0, 0.5), Vec3.ZERO, 0.0F, CAMERA_PITCH,
					TeleportTransition.DO_NOTHING)) instanceof ServerPlayer)) {
				throw new AssertionError("Integrated dimension transition to Nether failed");
			}
		});
		context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.NETHER));
		context.waitFor(client -> ClientRankTenSilhouetteManager.entries().isEmpty());
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			var overworld = server.getLevel(Level.OVERWORLD);
			if (overworld == null || !(player.teleport(new TeleportTransition(overworld,
					overworldPosition.get(), Vec3.ZERO, 0.0F, CAMERA_PITCH,
					TeleportTransition.DO_NOTHING)) instanceof ServerPlayer)) {
				throw new AssertionError("Integrated dimension transition back to Overworld failed");
			}
		});
		context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.OVERWORLD));
		context.waitFor(client -> ClientRankTenSilhouetteManager.entries().isEmpty());
		context.waitTicks(8);
		quiesceUi(context);
	}

	private static void setNearCasterBody(TestSingleplayerContext singleplayer, boolean present) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			for (var entity : player.level().getAllEntities()) {
				if (entity.entityTags().contains(NEAR_BODY_TAG)) entity.discard();
			}
			if (!present) return;
			var body = PowersEntities.POWER_TEST_ACTOR.create(player.level(), EntitySpawnReason.COMMAND);
			if (body == null) throw new AssertionError("Could not create near caster body target");
			body.setPos(player.getX(), player.getY(), player.getZ() + 8.0);
			body.setYRot(180.0F);
			body.setXRot(0.0F);
			body.setNoAi(true);
			body.setNoGravity(true);
			body.setInvulnerable(true);
			body.setPersistenceRequired();
			body.setCustomNameVisible(false);
			body.setDeltaMovement(Vec3.ZERO);
			body.addTag(NEAR_BODY_TAG);
			if (!player.level().addFreshEntity(body)) {
				throw new AssertionError("Could not add near caster body target");
			}
		});
	}

	private static void setWall(TestSingleplayerContext singleplayer, boolean present) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			for (int x = -8; x <= 8; x++) for (int y = 94; y <= 108; y++) {
				player.level().setBlockAndUpdate(new BlockPos(x, y, 12), present
						? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState());
			}
		});
	}

	private static void beginManifest(net.minecraft.client.Minecraft client) {
		Path manifest = client.gameDirectory.toPath().resolve("vfx005-manifest.jsonl");
		try {
			Files.createDirectories(manifest.getParent());
			Files.writeString(manifest, "", StandardCharsets.UTF_8);
		} catch (IOException error) {
			throw new IllegalStateException("Could not initialise VFX-005 manifest", error);
		}
	}

	private static void appendManifest(Path gameDirectory, ManifestRow row) {
		try {
			Files.writeString(gameDirectory.resolve("vfx005-manifest.jsonl"),
					GSON.toJson(row) + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException error) {
			throw new IllegalStateException("Could not append VFX-005 manifest row", error);
		}
	}

	private record ManifestRow(String captureId, String category, String powerId,
			String alignment, int distance, boolean reducedMotion, String particles,
			int reloadRevision, long epoch, String imagePath) {
		private ManifestRow {
			if (!POWER_IDS.contains(powerId) || distance < 0 || reloadRevision < 0 || epoch < 0
					|| !List.of("radiant", "darkness").contains(alignment)
					|| !List.of("all", "minimal").contains(particles)
					|| captureId.isBlank() || category.isBlank() || imagePath.isBlank()) {
				throw new IllegalArgumentException("Invalid VFX-005 manifest row");
			}
		}
	}
}
