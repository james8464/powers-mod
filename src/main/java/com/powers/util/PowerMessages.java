package com.powers.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Sends flavor messages with randomized variants so that repeated events
 * never read exactly the same way twice. Each message group lives under a
 * base key with numbered variants: {@code base.1}, {@code base.2}, ...
 * {@code base.count}.
 */
public final class PowerMessages {
	private PowerMessages() {
	}

	/** A randomly chosen variant of a message group. */
	public static Component random(String baseKey, int count) {
		return Component.translatable(baseKey + "." + (ThreadLocalRandom.current().nextInt(count) + 1));
	}

	/** Sends a randomly chosen variant of a message group to a player. */
	public static void send(ServerPlayer player, String baseKey, int count) {
		player.sendSystemMessage(random(baseKey, count));
	}

	/** Sends a randomly chosen variant, formatting the given arguments into it. */
	public static void send(ServerPlayer player, String baseKey, int count, Object... args) {
		player.sendSystemMessage(Component.translatable(
				baseKey + "." + (ThreadLocalRandom.current().nextInt(count) + 1), args));
	}
}
