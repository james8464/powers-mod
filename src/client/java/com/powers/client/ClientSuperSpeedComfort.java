package com.powers.client;

import com.powers.network.SuperSpeedComfortPackets;

/** Local-only finite mirror used solely by the camera presentation hook. */
public final class ClientSuperSpeedComfort {
	private static int remainingTicks;

	private ClientSuperSpeedComfort() {
	}

	public static void handle(SuperSpeedComfortPackets.Payload payload) {
		remainingTicks = payload.active() ? payload.remainingTicks() : 0;
	}

	public static void tick() {
		if (remainingTicks > 0) remainingTicks--;
	}

	public static boolean active() {
		return remainingTicks > 0;
	}

	public static void clear() {
		remainingTicks = 0;
	}
}
