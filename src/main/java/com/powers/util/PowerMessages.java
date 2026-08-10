package com.powers.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Routes routine feedback to the actionbar and reserves persistent chat for
 * rare events. Numbered message groups use {@code base.1} through {@code base.count}.
 */
public final class PowerMessages {
	/** Player-facing surfaces supported by server feedback. */
	public enum Delivery {
		OVERLAY,
		CHAT
	}

	private PowerMessages() {
	}

	/** A randomly picked variant from a numbered message group. */
	public static Component random(String baseKey, int count) {
		return Component.translatable(baseKey + "." + (ThreadLocalRandom.current().nextInt(count) + 1));
	}

	/** Sends routine feedback to the concise actionbar overlay. */
	public static void send(ServerPlayer player, String baseKey, int count) {
		dispatch(Delivery.OVERLAY, random(baseKey, count),
				player::sendOverlayMessage, player::sendSystemMessage);
	}

	/** Sends formatted routine feedback to the concise actionbar overlay. */
	public static void send(ServerPlayer player, String baseKey, int count, Object... args) {
		dispatch(Delivery.OVERLAY, variant(baseKey, count, args),
				player::sendOverlayMessage, player::sendSystemMessage);
	}

	/** Sends an exact routine message to the actionbar. */
	public static void overlay(ServerPlayer player, Component message) {
		dispatch(Delivery.OVERLAY, message, player::sendOverlayMessage, player::sendSystemMessage);
	}

	/** Persists a rare or important randomized event in chat history. */
	public static void sendImportant(ServerPlayer player, String baseKey, int count, Object... args) {
		dispatch(Delivery.CHAT, variant(baseKey, count, args),
				player::sendOverlayMessage, player::sendSystemMessage);
	}

	static void dispatch(Delivery delivery, Component message,
			Consumer<Component> overlaySink, Consumer<Component> chatSink) {
		if (delivery == Delivery.CHAT) {
			chatSink.accept(message);
		} else {
			overlaySink.accept(message);
		}
	}

	private static Component variant(String baseKey, int count, Object... args) {
		return Component.translatable(
				baseKey + "." + (ThreadLocalRandom.current().nextInt(count) + 1), args);
	}
}
