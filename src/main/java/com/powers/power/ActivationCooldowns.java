package com.powers.power;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player cooldowns, one timer per ability id. Every power declares
 * its own cooldown; this is where the game stops players from spamming
 */
public final class ActivationCooldowns {
	// stores the server tick at which each ability unlocks, per player
	private static final Map<UUID, Map<String, Long>> READY_AT = new HashMap<>();

	private ActivationCooldowns() {
	}

	/** whether the ability is off cooldown and usable right now */
	public static boolean isReady(ServerPlayer player, Ability ability) {
		return remainingTicks(player, ability) <= 0;
	}

	/** ticks left before the ability unlocks, or 0 when it's ready */
	public static int remainingTicks(ServerPlayer player, Ability ability) {
		Map<String, Long> cooldowns = READY_AT.get(player.getUUID());
		if (cooldowns == null) {
			return 0;
		}
		long readyAt = cooldowns.getOrDefault(ability.id().toString(), 0L);
		long remaining = readyAt - player.level().getServer().getTickCount();
		return remaining > 0 ? (int) remaining : 0;
	}

	/** arms the cooldown after a successful activation */
	public static void start(ServerPlayer player, Ability ability, int ticks) {
		// zero means the ability has no cooldown to wait for
		if (ticks <= 0) {
			return;
		}
		READY_AT.computeIfAbsent(player.getUUID(), key -> new HashMap<>())
				.put(ability.id().toString(),
						(long) player.level().getServer().getTickCount() + ticks);
	}

	public static void clear(UUID player) {
		READY_AT.remove(player);
	}

	public static void clearAll() {
		READY_AT.clear();
	}
}
