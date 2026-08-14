package com.powers.api.v1;

import net.minecraft.server.level.ServerLevel;
import java.util.Objects;
import java.util.UUID;

/** Authoritative fixed physical presence registered on the server thread. */
public record PhysicalPresence(String actionId, UUID owner, ServerLevel level, double x, double y,
		double z, double radius, long expiresAt, PresenceKind kind) {
	public PhysicalPresence {
		Objects.requireNonNull(actionId, "actionId");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(kind, "kind");
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
				|| !Double.isFinite(radius) || radius < 0 || radius > 128 || expiresAt < 0) {
			throw new IllegalArgumentException("Invalid physical presence");
		}
	}
}
