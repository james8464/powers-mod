package com.powers.magic.runtime;

import com.powers.time.ControlTick;
import com.powers.time.WorldTick;

/** Explicit conversion boundary for world-owned physical magic lifetimes. */
public final class MagicPresenceDeadline {
	private MagicPresenceDeadline() {
	}

	public static WorldTick after(WorldTick now, long durationTicks) {
		return now.plus(Math.max(1L, durationTicks));
	}

	public static WorldTick fromControlRemaining(WorldTick worldNow,
			ControlTick controlNow, ControlTick controlDeadline) {
		return after(worldNow, Math.max(1L, controlNow.remainingUntil(controlDeadline)));
	}
}
