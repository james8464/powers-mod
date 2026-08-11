package com.powers.power;

import com.powers.mind.ParticipantPowerLock;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.GlobalTimeStopManager;
import net.minecraft.server.level.ServerPlayer;
import com.powers.knowledge.MagicAttemptReporter;
import com.powers.knowledge.MagicFailureReason;

/** Shared server-side suppression gate for every player-initiated magical item or cast. */
public final class MagicUseGate {
	public enum Block { NONE, GLOBAL_TIME_STOP, AMETHYST, LOCAL_FREEZE }

	private MagicUseGate() {
	}

	public static boolean passes(ServerPlayer player, boolean punishAmethyst) {
		return passes(player, punishAmethyst, "magic");
	}

	/** Suppression gate with an authoritative action ID for Shadow diagnostics. */
	public static boolean passes(ServerPlayer player, boolean punishAmethyst, String actionId) {
		if (player == null) return false;
		if (ParticipantPowerLock.isLocked(player.getUUID())) {
			MagicAttemptReporter.failure(player, actionId, MagicFailureReason.TIME_LOCKED);
			return false;
		}
		if (GlobalTimeStopManager.rejectIfStopped(player)) {
			MagicAttemptReporter.failure(player, actionId, MagicFailureReason.TIME_LOCKED);
			return false;
		}
		AmethystDampening.update(player);
		Block reason = reason(false, AmethystDampening.isDampened(player),
				EntityFreezeController.isFrozen(player));
		if (reason == Block.AMETHYST) {
			if (punishAmethyst) AmethystDampening.punish(player);
			MagicAttemptReporter.failure(player, actionId, MagicFailureReason.AMETHYST);
			return false;
		}
		if (reason == Block.LOCAL_FREEZE) {
			EntityFreezeController.reject(player);
			MagicAttemptReporter.failure(player, actionId, MagicFailureReason.TIME_LOCKED);
			return false;
		}
		return true;
	}

	/**
	 * Revalidates a cast which remains active after its initial activation. This
	 * deliberately emits no repeated feedback: an amethyst ward or competing
	 * time stop should end the effect once, not spam its owner every tick.
	 */
	public static boolean ongoingAllowed(ServerPlayer player) {
		if (player == null || !player.isAlive() || player.isRemoved()) return false;
		return reason(!GlobalTimeStopManager.mayAct(player),
				AmethystDampening.isDampened(player), timeLocked(player)) == Block.NONE;
	}

	/** True for both the server-wide clock lock and a local crystal freeze. */
	public static boolean timeLocked(ServerPlayer player) {
		return player != null && (!GlobalTimeStopManager.mayAct(player)
				|| EntityFreezeController.isFrozen(player));
	}

	static Block reason(boolean globalTimeStop, boolean amethyst, boolean localFreeze) {
		if (globalTimeStop) return Block.GLOBAL_TIME_STOP;
		if (amethyst) return Block.AMETHYST;
		return localFreeze ? Block.LOCAL_FREEZE : Block.NONE;
	}
}
