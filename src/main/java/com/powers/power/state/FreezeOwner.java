package com.powers.power.state;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Stable ownership tokens keep two powers from sharing a player's claim. */
public final class FreezeOwner {
	private FreezeOwner() {
	}

	public static UUID token(String powerId, UUID player) {
		return UUID.nameUUIDFromBytes((powerId + ":" + player).getBytes(StandardCharsets.UTF_8));
	}
}
