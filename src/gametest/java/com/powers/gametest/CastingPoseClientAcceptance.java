package com.powers.gametest;

import com.google.gson.Gson;
import com.powers.PowersEntities;
import com.powers.animation.CastingHand;
import com.powers.animation.CastingPose;
import com.powers.animation.CastingPoseAngles;
import com.powers.animation.CastingPoseEvent;
import com.powers.animation.CastingPoseLocomotion;
import com.powers.animation.CastingPoseService;
import com.powers.animation.CastingStyle;
import com.powers.client.animation.ClientCastingPoseManager;
import com.powers.companion.ShadowCompanionData;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.testing.network.PacketFaultController;
import com.powers.testing.network.PacketFaultProfile;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Exact-build integrated client gallery and lifecycle acceptance for VFX-006. */
public final class CastingPoseClientAcceptance {
	private static final Gson GSON = new Gson();
	private static final String SUBJECT_TAG = "powers_vfx006_subject";
	private static final int DURATION = 40;
	private static final double CAMERA_Y = 100.0;

	private CastingPoseClientAcceptance() {
	}

	public static ReconnectSeed run(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		prepare(context, singleplayer);
		context.runOnClient(CastingPoseClientAcceptance::beginManifest);
		for (boolean reduced : new boolean[]{false, true}) {
			for (CastingStyle style : CastingStyle.values()) {
				for (CastingPose pose : CastingPose.values()) {
					captureGallery(context, singleplayer, style, pose, reduced);
				}
			}
		}
		captureLatency(context, singleplayer, "latency", false);
		captureLatency(context, singleplayer, "late_tracking", true);
		captureLocomotionWalk(context, singleplayer);
		captureCleared(context, singleplayer, "interruption", true);
		captureCleared(context, singleplayer, "expiry", false);
		captureEntityIdReuse(context, singleplayer);
		return seedReconnect(context, singleplayer);
	}

	public static void captureReconnect(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, ReconnectSeed seed) {
		prepare(context, singleplayer);
		AtomicReference<Subject> subject = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			Entity entity = null;
			for (var level : server.getAllLevels()) {
				entity = level.getEntity(seed.entityUuid());
				if (entity != null) break;
			}
			if (!(entity instanceof LivingEntity living)) {
				throw new AssertionError("Reconnect subject did not persist");
			}
			living.teleportTo(0.5, CAMERA_Y, 6.5);
			subject.set(new Subject(living.getId(), living.getUUID(), seed.entityType()));
		});
		context.waitFor(client -> client.level != null && subject.get() != null
				&& client.level.getEntity(subject.get().entityId()) != null);
		context.waitTicks(4);
		captureInactive(context, "reconnect", seed.event(), subject.get(), subject.get().entityUuid(),
				seed.receiptTick());
	}

	private static void prepare(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		context.waitFor(client -> client.player != null && client.level != null);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			VfxGalleryFixture.stabilize(player);
			player.teleportTo(0.5, CAMERA_Y, 0.5);
			player.setYRot(0.0F);
			player.setXRot(0.0F);
			player.setNoGravity(true);
			player.setInvulnerable(true);
			player.setDeltaMovement(Vec3.ZERO);
			CastingPoseService.clearAll();
		});
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.options.fov().set(70);
			client.options.renderDistance().set(12);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.screenEffectScale().set(1.0);
			client.gui.toastManager().clear();
			client.player.setYRot(0.0F);
			client.player.setXRot(0.0F);
		});
		context.waitTicks(40);
	}

	private static void captureGallery(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, CastingStyle style, CastingPose pose,
			boolean reduced) {
		setReduced(context, reduced);
		Subject subject = spawn(context, singleplayer, style, 6.0, null);
		CastingPoseEvent event = start(singleplayer, subject, pose, style,
				pose == CastingPose.PROJECT ? CastingHand.RIGHT : CastingHand.BOTH, DURATION);
		long receipt = awaitActive(context, subject, event.sequence());
		long now = context.computeOnClient(client -> client.level.getGameTime());
		int wait = (int) Math.max(0L, event.startGameTime() + DURATION / 2 - now);
		if (wait > 0) context.waitTicks(wait);
		captureActive(context, "gallery", subject, event, receipt, reduced);
		discard(context, singleplayer, subject);
	}

	private static void captureLatency(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, String scenario, boolean lateTracking) {
		setReduced(context, false);
		Subject subject = spawn(context, singleplayer, CastingStyle.RADIANT,
				lateTracking ? 180.0 : 6.0, null);
		if (!lateTracking) {
			singleplayer.getServer().runOnServer(server -> PacketFaultController.configureScoped(
					server, PacketFaultProfile.named("delay300", 0xC457006L),
					server.getPlayerList().getPlayers().getFirst()));
		}
		CastingPoseEvent event = start(singleplayer, subject, CastingPose.PROJECT,
				CastingStyle.RADIANT, CastingHand.RIGHT, DURATION);
		if (lateTracking) {
			context.waitTicks(10);
			move(singleplayer, subject, 6.0);
		}
		long receipt = awaitActive(context, subject, event.sequence());
		if (!lateTracking) {
			singleplayer.getServer().runOnServer(server -> PacketFaultController.clearScoped(
					server, server.getPlayerList().getPlayers().getFirst()));
		}
		long now = context.computeOnClient(client -> client.level.getGameTime());
		int wait = (int) Math.max(0L, event.startGameTime() + 10 - now);
		if (wait > 0) context.waitTicks(wait);
		captureActive(context, scenario, subject, event, receipt, false);
		discard(context, singleplayer, subject);
	}

	private static void captureLocomotionWalk(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		setReduced(context, false);
		Subject subject = spawn(context, singleplayer, CastingStyle.RADIANT, 6.0, null);
		CastingPoseEvent event = start(singleplayer, subject, CastingPose.CHANNEL,
				CastingStyle.RADIANT, CastingHand.BOTH, DURATION);
		long receipt = awaitActive(context, subject, event.sequence());
		for (int step = 0; step < 4; step++) {
			singleplayer.getServer().runOnServer(server -> {
				Entity entity = server.overworld().getEntity(subject.entityUuid());
				if (!(entity instanceof LivingEntity living)) {
					throw new AssertionError("Locomotion subject vanished");
				}
				living.move(MoverType.SELF, new Vec3(0.2, 0.0, 0.0));
			});
			context.waitTick();
		}
		long now = context.computeOnClient(client -> client.level.getGameTime());
		int wait = (int) Math.max(0L, event.startGameTime() + 12 - now);
		if (wait > 0) context.waitTicks(wait);
		captureActive(context, "locomotion_walk", subject, event, receipt, false);
		discard(context, singleplayer, subject);
	}

	private static void captureCleared(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, String scenario, boolean interrupt) {
		setReduced(context, false);
		Subject subject = spawn(context, singleplayer, CastingStyle.FIRST_VESSEL, 6.0, null);
		int duration = interrupt ? DURATION : 4;
		CastingPoseEvent event = start(singleplayer, subject, CastingPose.CHANNEL,
				CastingStyle.FIRST_VESSEL, CastingHand.BOTH, duration);
		awaitActive(context, subject, event.sequence());
		if (interrupt) {
			singleplayer.getServer().runOnServer(server -> {
				LivingEntity entity = (LivingEntity) server.overworld().getEntity(subject.entityUuid());
				if (entity == null) throw new AssertionError("Interruption subject vanished");
				CastingPoseService.clear(entity);
			});
			context.waitTicks(3);
		} else {
			context.waitTicks(duration + 2);
		}
		captureInactive(context, scenario, event, subject, subject.entityUuid(), null);
		discard(context, singleplayer, subject);
	}

	private static void captureEntityIdReuse(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		setReduced(context, false);
		Subject original = spawn(context, singleplayer, CastingStyle.RADIANT, 6.0, null);
		CastingPoseEvent event = start(singleplayer, original, CastingPose.PROJECT,
				CastingStyle.RADIANT, CastingHand.RIGHT, DURATION);
		awaitActive(context, original, event.sequence());
		AtomicReference<Subject> replacement = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			Entity before = server.overworld().getEntity(original.entityUuid());
			if (before == null) throw new AssertionError("Reuse source vanished");
			before.discard();
			LivingEntity after = create(server.getPlayerList().getPlayers().getFirst(),
					CastingStyle.RADIANT);
			after.setId(original.entityId());
			stabilize(after, 6.0);
			if (!server.overworld().addFreshEntity(after)) {
				throw new AssertionError("Could not add entity-ID replacement");
			}
			replacement.set(new Subject(after.getId(), after.getUUID(), entityType(CastingStyle.RADIANT)));
		});
		context.waitFor(client -> replacement.get() != null && client.level != null
				&& client.level.getEntity(original.entityId()) != null
				&& client.level.getEntity(original.entityId()).getUUID()
						.equals(replacement.get().entityUuid()));
		context.waitTicks(3);
		captureInactive(context, "entity_id_reuse", event, original,
				replacement.get().entityUuid(), null);
		discard(context, singleplayer, replacement.get());
	}

	private static ReconnectSeed seedReconnect(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		setReduced(context, false);
		Subject subject = spawn(context, singleplayer, CastingStyle.RADIANT, 6.0, null);
		CastingPoseEvent event = start(singleplayer, subject, CastingPose.CHANNEL,
				CastingStyle.RADIANT, CastingHand.BOTH, DURATION);
		long receipt = awaitActive(context, subject, event.sequence());
		return new ReconnectSeed(event, subject.entityType(), subject.entityUuid(), receipt);
	}

	private static Subject spawn(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, CastingStyle style, double distance,
			Integer forcedId) {
		AtomicReference<Subject> result = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
			LivingEntity entity = create(owner, style);
			if (forcedId != null) entity.setId(forcedId);
			stabilize(entity, distance);
			if (!server.overworld().addFreshEntity(entity)) {
				throw new AssertionError("Could not add VFX-006 " + style + " subject");
			}
			result.set(new Subject(entity.getId(), entity.getUUID(), entityType(style)));
		});
		if (distance <= 160.0) {
			context.waitFor(client -> result.get() != null && client.level != null
					&& client.level.getEntity(result.get().entityId()) != null
					&& client.level.getEntity(result.get().entityId()).getUUID()
							.equals(result.get().entityUuid()));
		}
		return result.get();
	}

	private static LivingEntity create(ServerPlayer owner, CastingStyle style) {
		EntityType<? extends LivingEntity> type = switch (style) {
			case SHADOW -> PowersEntities.SHADOW_COMPANION;
			case RADIANT -> PowersEntities.RADIANT_SENTINEL;
			case DARKNESS -> PowersEntities.DARKNESS_CREATURE;
			case HERALD_LIGHT -> PowersEntities.LIGHT_HERALD;
			case HERALD_DARK -> PowersEntities.DARK_HERALD;
			case FIRST_VESSEL -> PowersEntities.FIRST_VESSEL;
		};
		LivingEntity entity = type.create(owner.level(), EntitySpawnReason.COMMAND);
		if (entity == null) throw new AssertionError("Could not create VFX-006 " + style + " subject");
		if (entity instanceof ShadowCompanionEntity shadow) {
			shadow.configure(owner, ShadowCompanionData.defaults().withRevealed(true));
		}
		return entity;
	}

	private static void stabilize(LivingEntity entity, double distance) {
		entity.teleportTo(0.5, CAMERA_Y, 0.5 + distance);
		entity.setYRot(180.0F);
		entity.setXRot(0.0F);
		entity.setNoGravity(true);
		entity.setInvulnerable(true);
		entity.setDeltaMovement(Vec3.ZERO);
		entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLAZE_ROD));
		entity.addTag(SUBJECT_TAG);
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			mob.setNoAi(true);
			mob.setPersistenceRequired();
		}
	}

	private static CastingPoseEvent start(TestSingleplayerContext singleplayer, Subject subject,
			CastingPose pose, CastingStyle style, CastingHand hand, int duration) {
		AtomicReference<CastingPoseEvent> result = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			Entity entity = server.overworld().getEntity(subject.entityUuid());
			if (!(entity instanceof LivingEntity living)) {
				throw new AssertionError("VFX-006 subject vanished before pose start");
			}
			result.set(CastingPoseService.start(living, pose, style, hand, duration)
					.orElseThrow(() -> new AssertionError("VFX-006 pose start was rejected")));
		});
		return result.get();
	}

	private static long awaitActive(ClientGameTestContext context, Subject subject, long sequence) {
		context.waitFor(client -> {
			Entity entity = client.level == null ? null : client.level.getEntity(subject.entityId());
			return entity != null && ClientCastingPoseManager.resolve(entity)
					.map(resolved -> resolved.event().sequence() == sequence).orElse(false);
		});
		return context.computeOnClient(client -> client.level.getGameTime());
	}

	private static void captureActive(ClientGameTestContext context, String scenario,
			Subject subject, CastingPoseEvent event, long receiptTick, boolean reduced) {
		Resolved resolved = context.computeOnClient(client -> {
			LivingEntity entity = (LivingEntity) client.level.getEntity(subject.entityId());
			if ("locomotion_walk".equals(scenario)) {
				entity.walkAnimation.update(0.8F, 1.0F, 1.0F);
			}
			var state = ClientCastingPoseManager.resolve(entity).orElseThrow();
			CastingPoseAngles angles = CastingPoseAngles.resolve(state.event().pose(),
					state.event().style(), state.event().hand(), state.progress(), reduced).scale(
							CastingPoseLocomotion.scale(entity.isFallFlying(),
									entity.isVisuallySwimming(), entity.getSwimAmount(1.0F),
									entity.walkAnimation.speed(1.0F), entity.isPassenger()));
			return new Resolved(client.level.getGameTime(), state.progress(), angles);
		});
		capture(context, scenario, subject, subject.entityUuid(), event, receiptTick,
				resolved.tick(), reduced, true, resolved.progress(), resolved.angles());
	}

	private static void captureInactive(ClientGameTestContext context, String scenario,
			CastingPoseEvent event, Subject subject, UUID resolvedUuid, Long priorReceiptTick) {
		long tick = context.computeOnClient(client -> {
			Entity entity = client.level.getEntity(subject.entityId());
			if (entity != null && ClientCastingPoseManager.resolve(entity).isPresent()) {
				throw new AssertionError(scenario + " retained a stale casting pose");
			}
			return client.level.getGameTime();
		});
		capture(context, scenario, subject, resolvedUuid, event,
				priorReceiptTick == null ? tick : priorReceiptTick, tick,
				false, false, 1.0, CastingPoseAngles.ZERO);
	}

	private static void capture(ClientGameTestContext context, String scenario, Subject subject,
			UUID resolvedUuid, CastingPoseEvent event, long receiptTick, long captureTick,
			boolean reduced, boolean active, double progress, CastingPoseAngles angles) {
		String mode = "gallery".equals(scenario) ? (reduced ? "-reduced" : "-normal") : "";
		String captureId = "vfx006-" + scenario + '-' + event.style().name().toLowerCase()
				+ '-' + event.pose().name().toLowerCase() + mode + '-' + event.sequence();
		Path screenshot = context.takeScreenshot(captureId);
		context.waitTick();
		context.runOnClient(client -> appendManifest(client.gameDirectory.toPath(), new ManifestRow(
				1, implementationSha(), captureId, scenario, subject.entityType(), subject.entityId(),
				event.entityUuid(), resolvedUuid, event.sequence(), event.pose(), event.style(),
				event.hand(), event.startGameTime(), event.durationTicks(), receiptTick, captureTick,
				reduced, active, progress, angles, screenshot.getFileName().toString(), sha256(screenshot))));
	}

	private static void setReduced(ClientGameTestContext context, boolean reduced) {
		context.runOnClient(client -> client.options.screenEffectScale().set(reduced ? 0.0 : 1.0));
		context.waitTick();
	}

	private static void move(TestSingleplayerContext singleplayer, Subject subject, double distance) {
		singleplayer.getServer().runOnServer(server -> {
			Entity entity = server.overworld().getEntity(subject.entityUuid());
			if (entity == null) throw new AssertionError("Late-tracking subject vanished");
			entity.teleportTo(0.5, CAMERA_Y, 0.5 + distance);
		});
	}

	private static void discard(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, Subject subject) {
		singleplayer.getServer().runOnServer(server -> {
			Entity entity = server.overworld().getEntity(subject.entityUuid());
			if (entity != null) entity.discard();
		});
		context.waitFor(client -> client.level == null
				|| client.level.getEntity(subject.entityId()) == null);
	}

	private static String entityType(CastingStyle style) {
		return switch (style) {
			case SHADOW -> "powers:shadow_companion";
			case RADIANT -> "powers:radiant_sentinel";
			case DARKNESS -> "powers:darkness_creature";
			case HERALD_LIGHT -> "powers:light_herald";
			case HERALD_DARK -> "powers:dark_herald";
			case FIRST_VESSEL -> "powers:first_vessel";
		};
	}

	private static String implementationSha() {
		String value = System.getProperty("powers.vfx006.implementationSha", "");
		if (!value.matches("[0-9a-f]{40}") || value.chars().allMatch(character -> character == '0')) {
			throw new IllegalStateException("A non-zero immutable VFX-006 implementation SHA is required");
		}
		return value;
	}

	private static void beginManifest(net.minecraft.client.Minecraft client) {
		try {
			Files.writeString(client.gameDirectory.toPath().resolve("vfx006-manifest.jsonl"), "",
					StandardCharsets.UTF_8);
		} catch (IOException error) {
			throw new IllegalStateException("Could not initialise VFX-006 manifest", error);
		}
	}

	private static void appendManifest(Path gameDirectory, ManifestRow row) {
		try {
			Files.writeString(gameDirectory.resolve("vfx006-manifest.jsonl"), GSON.toJson(row) + "\n",
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException error) {
			throw new IllegalStateException("Could not append VFX-006 manifest", error);
		}
	}

	private static String sha256(Path path) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(Files.readAllBytes(path)));
		} catch (IOException | NoSuchAlgorithmException error) {
			throw new IllegalStateException("Could not digest VFX-006 screenshot", error);
		}
	}

	public record ReconnectSeed(CastingPoseEvent event, String entityType, UUID entityUuid,
			long receiptTick) {
	}

	private record Subject(int entityId, UUID entityUuid, String entityType) {
	}

	private record Resolved(long tick, double progress, CastingPoseAngles angles) {
	}

	private record ManifestRow(int schemaVersion, String implementationSha, String captureId,
			String scenario, String entityType, int entityId, UUID entityUuid, UUID resolvedEntityUuid,
			long sequence, CastingPose pose, CastingStyle style, CastingHand hand,
			long authoritativeStartTick, int durationTicks, long receiptTick, long captureTick,
			boolean reducedMotion, boolean active, double progress, CastingPoseAngles angles,
			String imagePath, String sha256) {
	}
}
