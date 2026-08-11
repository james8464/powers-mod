package com.powers.knowledge;

import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Minecraft adapter which stamps attempts and emits one bounded private Shadow hint. */
public final class MagicAttemptReporter {
	private MagicAttemptReporter() {
	}

	public static void failure(ServerPlayer player, String actionId, MagicFailureReason reason) {
		failure(player, actionId, reason, Map.of());
	}

	public static void failure(ServerPlayer player, String actionId, MagicFailureReason reason,
			Map<String, Long> facts) {
		if (player == null) return;
		MagicAttempt attempt = MagicAttempt.failure(actionId, reason,
				player.level().getGameTime(), facts);
		if (MagicAttemptJournal.global().record(player.getUUID(), attempt)) {
			PowerMessages.overlay(player, Component.translatable("shadow.powers.failure_hint"));
		}
	}

	public static void success(ServerPlayer player, String actionId) {
		if (player == null) return;
		MagicAttemptJournal.global().record(player.getUUID(),
				MagicAttempt.success(actionId, player.level().getGameTime()));
	}

	/** Adds a generic failure only when a lower layer did not already report the precise cause. */
	public static void executionFailure(ServerPlayer player, String actionId) {
		if (player == null) return;
		long tick = player.level().getGameTime();
		if (!MagicAttemptJournal.global().hasFailureAt(player.getUUID(), actionId, tick)) {
			failure(player, actionId, MagicFailureReason.EXECUTION_FAILED);
		}
	}
}
