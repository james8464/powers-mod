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
		return !ownerOnline || !ownerAlive || !toggleActive || dampened || !serverFrozen;
	}
}
