package com.powers.power;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player ability cooldowns keyed by ability id. Every ability
 * declares its cooldown in its constructor; this is where it is enforced.
 */
public final class ActivationCooldowns {
	private static final Map<UUID, Map<String, Long>> READY_AT = new HashMap<>();

	private ActivationCooldowns() {
	}

	/** True when the ability can be used right now. */
	public static boolean isReady(ServerPlayer player, Ability ability) {
		return remainingTicks(player, ability) <= 0;
	}

	/** Ticks left until the ability unlocks, or 0 when ready. */
	public static int remainingTicks(ServerPlayer player, Ability ability) {
		Map<String, Long> cooldowns = READY_AT.get(player.getUUID());
		if (cooldowns == null) {
			return 0;
		}
		long readyAt = cooldowns.getOrDefault(ability.id().toString(), 0L);
		long remaining = readyAt - player.level().getServer().getTickCount();
		return remaining > 0 ? (int) remaining : 0;
	}

	/** Starts the cooldown for a successful activation. */
	public static void start(ServerPlayer player, Ability ability, int ticks) {
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
