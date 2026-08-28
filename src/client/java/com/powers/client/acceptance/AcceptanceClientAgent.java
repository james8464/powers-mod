package com.powers.client.acceptance;

import com.powers.PowersMod;
import com.powers.client.screen.CelestialLocatorScreen;
import com.powers.client.VesselControlClient;
import com.powers.network.PowersPackets;
import com.powers.network.CrystalSelectorPackets;
import com.powers.network.GrimoirePackets;
import com.powers.network.ShadowSwordPackets;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Connects and captures one explicitly configured development acceptance client. */
public final class AcceptanceClientAgent {
	private static final int CONNECT_DELAY_TICKS = 40;
	private static final int CAPTURE_DELAY_TICKS = 120;
	private static final AcceptanceClientConfig CONFIG = AcceptanceClientConfig.resolve(
			FabricLoader.getInstance().isDevelopmentEnvironment(), System::getProperty);
	private static int disconnectedTicks;
	private static int connectedTicks;
	private static boolean connecting;
	private static boolean captured;
	private static boolean scriptLoaded;
	private static int nextStep;
	private static List<AcceptanceClientScript.Step> script = List.of();

	private AcceptanceClientAgent() {
	}

	public static void tick(Minecraft client) {
		if (!CONFIG.enabled()) return;
		if (client.player == null || client.getConnection() == null) {
			connectedTicks = 0;
			if (!connecting && ++disconnectedTicks >= CONNECT_DELAY_TICKS) connect(client);
			return;
		}
		disconnectedTicks = 0;
		connecting = false;
		if (++connectedTicks == 20) {
			PowersMod.LOGGER.info("QA client role={} connected as {} to {}", CONFIG.role(),
					client.player.getName().getString(), CONFIG.server());
		}
		loadScript();
		while (nextStep < script.size() && script.get(nextStep).tick() <= connectedTicks) {
			runStep(client, script.get(nextStep++));
		}
		if (!captured && connectedTicks >= CAPTURE_DELAY_TICKS) {
			captured = true;
			Screenshot.grab(client, false);
			PowersMod.LOGGER.info("QA client role={} captured its connected-world evidence",
					CONFIG.role());
		}
	}

	private static void loadScript() {
		if (scriptLoaded) return;
		scriptLoaded = true;
		if (CONFIG.script().isEmpty()) return;
		try {
			script = AcceptanceClientScript.parse(Files.readAllLines(Path.of(CONFIG.script())));
			PowersMod.LOGGER.info("QA client role={} loaded {} scripted steps", CONFIG.role(),
					script.size());
		} catch (IOException | IllegalArgumentException exception) {
			PowersMod.LOGGER.error("QA client role={} rejected acceptance script {}",
					CONFIG.role(), CONFIG.script(), exception);
		}
	}

	private static void runStep(Minecraft client, AcceptanceClientScript.Step step) {
		var connection = client.getConnection();
		if (connection == null) return;
		switch (step.operation()) {
			case COMMAND -> connection.sendCommand(step.argument());
			case CHAT -> connection.sendChat(step.argument());
			case ACTIVATE -> ClientPlayNetworking.send(new PowersPackets.ActivateAbilityPayload(
					Integer.parseInt(step.argument())));
			case SELECT -> {
				String[] values = step.argument().split(" ");
				ClientPlayNetworking.send(new PowersPackets.SelectAbilityOptionPayload(
						Integer.parseInt(values[0]), Integer.parseInt(values[1])));
			}
			case USE -> {
				if (!VesselControlClient.requestRelease()
						&& client.gameMode != null && client.player != null) {
					client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
				}
			}
			case ATTACK -> attackNamed(client, step.argument());
			case GRIMOIRE -> {
				String[] values = step.argument().split(" ");
				ClientPlayNetworking.send(new GrimoirePackets.SelectSpellPayload(
						com.powers.client.ClientActionRegistry.revision(), values[0], values[1]));
			}
			case CRYSTAL -> ClientPlayNetworking.send(new CrystalSelectorPackets.SelectPayload(
					com.powers.client.ClientActionRegistry.revision(), step.argument()));
			case ARTIFACT -> {
				String[] values = step.argument().split(" ");
				ClientPlayNetworking.send(new ShadowSwordPackets.CommitPayload(
						com.powers.client.ClientActionRegistry.revision(),
						values[0], values[1], Integer.parseInt(values[2])));
			}
			case ARTIFACT_TELEPORT -> {
				String[] values = step.argument().split(" ");
				Identifier dimension = Identifier.parse(values[4]);
				ClientPlayNetworking.send(new ShadowSwordPackets.TeleportPayload(
						com.powers.client.ClientActionRegistry.revision(), values[0],
						com.powers.client.ClientActionRegistry.artifactActionKey(),
						Double.parseDouble(values[1]), Double.parseDouble(values[2]),
						Double.parseDouble(values[3]),
						ResourceKey.create(Registries.DIMENSION, dimension),
						values[5].equals("self") ? "" : values[5]));
			}
			case TELEPORT -> {
				String[] values = step.argument().split(" ");
				Identifier dimension = Identifier.parse(values[4]);
				ClientPlayNetworking.send(new PowersPackets.TeleportRequestPayload(
						Integer.parseInt(values[0]), Double.parseDouble(values[1]),
						Double.parseDouble(values[2]), Double.parseDouble(values[3]),
						ResourceKey.create(Registries.DIMENSION, dimension), "", false));
			}
			case RESPAWN -> connection.send(new ServerboundClientCommandPacket(
					ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
			case CLOSE -> {
				if (client.gui.screen() != null) client.gui.screen().onClose();
			}
			case LOCATOR -> {
				if (!(client.gui.screen() instanceof CelestialLocatorScreen locator)
						|| !locator.submitAcceptanceTarget(step.argument())) {
					PowersMod.LOGGER.error("QA client role={} could not submit locator input [{}]",
							CONFIG.role(), step.argument());
				}
			}
			case KEY -> setAcceptanceKey(client, step.argument());
			case LOOK -> setLook(client, step.argument());
			case SCREENSHOT -> {
				verifyScreenshotState(client, step.argument());
				// Acceptance captures are evidence, not a transcript of earlier system
				// notifications. Clear queued/visible UI noise immediately before pixels
				// are sampled; Screenshot may report its own filename after the capture.
				client.gui.hud.getChat().clearMessages(false);
				client.gui.toastManager().clear();
				Screenshot.grab(client, false);
			}
			case CLEAN -> {
				client.gui.hud.getChat().clearMessages(false);
				client.gui.toastManager().clear();
			}
			case SETTING -> applyReducedMotionSetting(client);
			case AUDIO_EMIT -> LayeredAudioAcceptance.emit(client, step.argument());
			case AUDIO_COMFORT -> LayeredAudioAcceptance.comfort(step.argument());
			case AUDIO_ASSERT -> LayeredAudioAcceptance.assertLast(step.argument());
		}
		PowersMod.LOGGER.info("QA client role={} executed {} [{}] at connected tick {}",
				CONFIG.role(), step.operation(), step.argument(), connectedTicks);
	}

	private static void verifyScreenshotState(Minecraft client, String label) {
		if (label.equals("locator_entity_two_clients")) {
			if (!(client.gui.screen() instanceof CelestialLocatorScreen locator)
					|| !locator.acceptanceVisiblePlayers().contains("VfxObserver")) {
				throw new AssertionError("Two-client locator proof lacks VfxObserver");
			}
			PowersMod.LOGGER.info("QA VFX proof locator visiblePlayers={}",
					locator.acceptanceVisiblePlayers());
		}
		if (label.equals("darkness_advancement_root")) {
			if (!(client.gui.screen() instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen)
					|| client.player == null
					|| client.player.connection.getAdvancements().get(PowersMod.id("darkness_root")) == null
					|| client.player.connection.getAdvancements().get(PowersMod.id("skill_root")) != null) {
				throw new AssertionError("Darkness advancement proof is not the sole loaded POWERS root");
			}
			PowersMod.LOGGER.info("QA VFX proof selectedRoot=powers:darkness_root opposingRootLoaded=false");
		}
	}

	private static void applyReducedMotionSetting(Minecraft client) {
		client.options.particles().set(net.minecraft.server.level.ParticleStatus.MINIMAL);
		client.options.screenEffectScale().set(0.0);
		client.options.save();
		var particles = client.options.particles().get();
		double scale = client.options.screenEffectScale().get();
		if (particles != net.minecraft.server.level.ParticleStatus.MINIMAL || scale != 0.0) {
			throw new IllegalStateException("Reduced-motion options did not resolve exactly");
		}
		PowersMod.LOGGER.info(
				"QA client role={} resolved reduced-motion settings particles={} screenEffectScale={}",
				CONFIG.role(), particles, scale);
	}

	private static void setLook(Minecraft client, String argument) {
		if (client.player == null) return;
		String[] values = argument.split(" ");
		float yaw = Float.parseFloat(values[0]);
		float pitch = Float.parseFloat(values[1]);
		client.player.setYRot(yaw);
		client.player.setXRot(pitch);
		client.player.setYHeadRot(yaw);
		client.player.setYBodyRot(yaw);
		client.player.yRotO = yaw;
		client.player.xRotO = pitch;
	}

	private static void setAcceptanceKey(Minecraft client, String argument) {
		String[] values = argument.split(" ");
		var key = switch (values[0]) {
			case "forward" -> client.options.keyUp;
			case "back" -> client.options.keyDown;
			case "left" -> client.options.keyLeft;
			case "right" -> client.options.keyRight;
			case "jump" -> client.options.keyJump;
			case "sneak" -> client.options.keyShift;
			case "sprint" -> client.options.keySprint;
			case "advancements" -> client.options.keyAdvancements;
			case "rank_maze" -> com.powers.client.PowersClient.rankMazeKey;
			default -> throw new IllegalArgumentException("Unsupported acceptance key");
		};
		AcceptanceKeyInput.apply(key, values[0], values[1].equals("on"));
	}

	private static void attackNamed(Minecraft client, String name) {
		if (client.level == null || client.player == null || client.gameMode == null) return;
		LivingEntity target = null;
		for (var entity : client.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity living
					&& living.getName().getString().equalsIgnoreCase(name)) {
				target = living;
				break;
			}
		}
		if (target != null) {
			client.gameMode.attack(client.player, target);
			client.player.swing(InteractionHand.MAIN_HAND);
		}
	}

	private static void connect(Minecraft client) {
		connecting = true;
		ServerData data = new ServerData("POWERS QA", CONFIG.server(), ServerData.Type.OTHER);
		ConnectScreen.startConnecting(client.gui.screen() == null ? new TitleScreen(false)
				: client.gui.screen(), client, ServerAddress.parseString(CONFIG.server()), data,
				false, null);
	}
}
