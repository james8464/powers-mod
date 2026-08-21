package com.powers.testing.network;

import java.util.Objects;
import java.util.UUID;

/** Opaque connection generation; reconnects never inherit scheduled packets. */
public record PacketFaultConnection(UUID owner, long generation) {
	public PacketFaultConnection {
		Objects.requireNonNull(owner, "owner");
		if (generation < 0L) throw new IllegalArgumentException("generation must be non-negative");
	}
}
