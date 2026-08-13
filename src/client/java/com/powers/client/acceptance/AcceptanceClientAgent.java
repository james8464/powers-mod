package com.powers.client.acceptance;

import com.powers.PowersMod;
import com.powers.network.PowersPackets;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

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
			case RESPAWN -> connection.send(new ServerboundClientCommandPacket(
					ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
			case SCREENSHOT -> Screenshot.grab(client, false);
		}
		PowersMod.LOGGER.info("QA client role={} executed {} [{}] at connected tick {}",
				CONFIG.role(), step.operation(), step.argument(), connectedTicks);
	}

	private static void connect(Minecraft client) {
		connecting = true;
		ServerData data = new ServerData("POWERS QA", CONFIG.server(), ServerData.Type.OTHER);
		ConnectScreen.startConnecting(client.gui.screen() == null ? new TitleScreen(false)
				: client.gui.screen(), client, ServerAddress.parseString(CONFIG.server()), data,
				false, null);
	}
}
