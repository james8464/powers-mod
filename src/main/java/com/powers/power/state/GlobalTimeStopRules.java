package com.powers.power.state;

import java.util.UUID;

/** Pure ownership and lifecycle policy for the server-wide tick freeze. */
public final class GlobalTimeStopRules {
	private GlobalTimeStopRules() {
	}

	/** A player power never steals an administrative or already-owned freeze. */
	public static boolean mayStart(boolean alreadyOwned, boolean serverFrozen) {
		return !alreadyOwned && !serverFrozen;
	}

	/** Outside a stop everyone acts; during it only the owning player may act. */
	public static boolean mayAct(UUID owner, UUID actor) {
		return owner == null || owner.equals(actor);
	}

	/** Any broken ownership invariant ends the stop before it can orphan a server. */
	public static boolean shouldRelease(boolean ownerOnline, boolean ownerAlive,
			boolean toggleActive, boolean dampened, boolean serverFrozen) {
		return shouldRelease(ownerOnline, ownerAlive, toggleActive, dampened,
				serverFrozen, false);
	}

	/** External clock writes invalidate power ownership even when the clock stays frozen. */
	public static boolean shouldRelease(boolean ownerOnline, boolean ownerAlive,
			boolean toggleActive, boolean dampened, boolean serverFrozen,
			boolean externallyMutated) {
		return !ownerOnline || !ownerAlive || !toggleActive || dampened
				|| !serverFrozen || externallyMutated;
	}

	/** Never undo the clock state after an administrator or another system takes ownership. */
	public static boolean shouldUnfreezeOnRelease(boolean serverFrozen,
			boolean externallyMutated) {
		return serverFrozen && !externallyMutated;
	}
}
