package com.powers.power;

import com.powers.power.crystals.SpaceTimeAbility;
import com.powers.power.state.GlobalTimeStopManager;
import net.minecraft.server.level.ServerPlayer;

/** Shared server-side suppression gate for every player-initiated magical item or cast. */
public final class MagicUseGate {
	public enum Block { NONE, GLOBAL_TIME_STOP, AMETHYST, LOCAL_FREEZE }

	private MagicUseGate() {
	}

	public static boolean passes(ServerPlayer player, boolean punishAmethyst) {
		if (GlobalTimeStopManager.rejectIfStopped(player)) return false;
		AmethystDampening.update(player);
		Block reason = reason(false, AmethystDampening.isDampened(player),
				SpaceTimeAbility.isFrozen(player));
		if (reason == Block.AMETHYST) {
			if (punishAmethyst) AmethystDampening.punish(player);
			return false;
		}
		if (reason == Block.LOCAL_FREEZE) {
			SpaceTimeAbility.reject(player);
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
				|| SpaceTimeAbility.isFrozen(player));
	}

	static Block reason(boolean globalTimeStop, boolean amethyst, boolean localFreeze) {
		if (globalTimeStop) return Block.GLOBAL_TIME_STOP;
		if (amethyst) return Block.AMETHYST;
		return localFreeze ? Block.LOCAL_FREEZE : Block.NONE;
	}
}
