package com.powers.power;

import com.powers.player.PlayerPowers;
import net.minecraft.server.level.ServerPlayer;
import com.powers.testing.TestingOverrides;

/**
 * Per-player cooldowns, one timer per ability id. Every power declares
 * its own cooldown; this is where the game stops players from spamming
 */
public final class ActivationCooldowns {
	private ActivationCooldowns() {
	}

	/** whether the ability is off cooldown and usable right now */
	public static boolean isReady(ServerPlayer player, Ability ability) {
		return remainingTicks(player, ability) <= 0;
	}

	/** Active recovery blocks a cast unless its ability owns a legal reactivation. */
	public static boolean blocks(int remainingTicks, boolean reactivationAllowed) {
		return remainingTicks > 0 && !reactivationAllowed;
	}

	/** ticks left before the ability unlocks, or 0 when it's ready */
	public static int remainingTicks(ServerPlayer player, Ability ability) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		String id = ability.id().toString();
		int remaining = TestingOverrides.cooldownRemaining(DeadlineMath.remainingTicks(
				data.cooldownReadyAt(id), player.level().getGameTime()),
				TestingOverrides.cooldownsDisabled(player.getUUID()));
		if (remaining == 0) {
			data.clearCooldown(id);
		}
		return remaining;
	}

	/** arms the cooldown after a successful activation */
	public static void start(ServerPlayer player, Ability ability, int ticks) {
		// zero means the ability has no cooldown to wait for
		if (ticks <= 0 || TestingOverrides.cooldownsDisabled(player.getUUID())) {
			return;
		}
		PlayerPowers.get(player).setCooldown(ability.id().toString(),
				player.level().getGameTime() + ticks);
	}

	/** Restores the exact deadline captured before a failed transaction. */
	public static void restore(ServerPlayer player, Ability ability, long readyAt) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (readyAt <= 0L) data.clearCooldown(ability.id().toString());
		else data.setCooldown(ability.id().toString(), readyAt);
	}
}
