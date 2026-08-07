package com.powers.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Sends flavor messages with randomized variants so the same event never
 * reads the same way twice. each message group is a base key with numbered
 * variants: {@code base.1}, {@code base.2}, ... {@code base.count}.
 */
public final class PowerMessages {
	private PowerMessages() {
	}

	/** A randomly picked variant from a numbered message group. */
	public static Component random(String baseKey, int count) {
		return Component.translatable(baseKey + "." + (ThreadLocalRandom.current().nextInt(count) + 1));
	}

	/** Sends a random variant of a message group to a player. */
	public static void send(ServerPlayer player, String baseKey, int count) {
		player.sendSystemMessage(random(baseKey, count));
	}

	/** Sends a random variant with the given arguments formatted into it. */
	public static void send(ServerPlayer player, String baseKey, int count, Object... args) {
		player.sendSystemMessage(Component.translatable(
				baseKey + "." + (ThreadLocalRandom.current().nextInt(count) + 1), args));
	}
}
